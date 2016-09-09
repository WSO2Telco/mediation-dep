/*
 * Copyright (c) 1996-2001
 * Logica Mobile Networks Limited
 * All rights reserved.
 *
 * This software is distributed under Logica Open Source License Version 1.0
 * ("Licence Agreement"). You shall use it and distribute only in accordance
 * with the terms of the License Agreement.
 *
 */
package com.logica.smscsim;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ie.omk.smpp.message.tlv.Tag;
import lk.dialog.smsc.db.DBLogger;
import lk.dialog.smsc.mife.request.sendsms.MifeSMSRequest;
import lk.dialog.smsc.services.i.IMIFEService;
import lk.dialog.smsc.util.PropertyReader;
import lk.dialog.smsc.util.SMSCSIMProperties;
import lk.dialog.smsc.util.SMSUtil;
import lk.dialog.smsc.mife.response.sendsms.OutboundSMSMessageRequest;
import lk.dialog.smsc.pool.MIFEServicePool;
import lk.dialog.smsc.util.JsonUtil;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.logica.smpp.Data;
import com.logica.smpp.SmppObject;
import com.logica.smpp.debug.Debug;
import com.logica.smpp.debug.Event;
import com.logica.smpp.debug.FileLog;
import com.logica.smpp.pdu.BindRequest;
import com.logica.smpp.pdu.BindResponse;
import com.logica.smpp.pdu.CancelSM;
import com.logica.smpp.pdu.DataSMResp;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.DeliverSMResp;
import com.logica.smpp.pdu.QuerySM;
import com.logica.smpp.pdu.QuerySMResp;
import com.logica.smpp.pdu.ReplaceSM;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.Response;
import com.logica.smpp.pdu.SubmitMultiSM;
import com.logica.smpp.pdu.SubmitMultiSMResp;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.pdu.SubmitSMResp;
import com.logica.smpp.pdu.WrongLengthOfStringException;
import com.logica.smpp.util.ByteBuffer;
import com.logica.smscsim.util.Record;
import com.logica.smscsim.util.Table;
import com.wso2telco.refresh.Refresher;

/**
 * Class <code>SimulatorPDUProcessor</code> gets the <code>Request</code>
 * from the client and creates the proper
 * <code>Response</code> and sends it. At the beginning it authenticates
 * the client using information in the bind request and list of users provided
 * during construction of the processor. It also stores messages
 * sent from client and allows cancellation and replacement of the messages.
 *
 * @author Logica Mobile Networks SMPP Open Source Team
 * @version 1.0, 20 Sep 2001
 * @see PDUProcessor
 * @see SimulatorPDUProcessorFactory
 * @see SMSCSession
 * @see ShortMessageStore
 * @see Table
 */

/*
  20-09-01 ticp@logica.com added reference to the DeliveryInfoSender to support
                           automatic sending of delivery info PDUs
*/

public class SimulatorPDUProcessor extends PDUProcessor
{
	
	static Logger logger = LoggerFactory.getLogger( SimulatorPDUProcessor.class );
	
  /*
   * Map that holds partials of long SMSs
   * */
  private Map<Short,List<Request>> mapLongSMSPartials = new HashMap<Short, List<Request>>();

    /**
     * The session this processor uses for sending of PDUs.
     */
    private SMSCSession session = null;

    /**
     * The container for received messages.
     */
    private ShortMessageStore messageStore = null;

    /**
     * The thread which sends delivery information for messages
     * which require delivery information.
     */
    private DeliveryInfoSender deliveryInfoSender = null;

    /**
     * The table with system id's and passwords for authenticating
     * of the bounding ESMEs.
     */
    private Table users = null;

    /**
     * Indicates if the bound has passed.
     */
    private boolean bound = false;

    /**
     * The system id of the bounded ESME.
     */
    private String systemId = null;

    /**
     * If the information about processing has to be printed
     * to the standard output.
     */
    private boolean displayInfo = false;

    /**
     * The message id assigned by simulator to submitted messages.
     */
    private static int intMessageId = 20000000;

    /**
     * System id of this simulator sent to the ESME in bind response.
     */
    private static final String SYSTEM_ID = "Smsc Simulator";

    /**
     * The name of attribute which contains the system id of ESME.
     */
    private static final String SYSTEM_ID_ATTR = "name";

    /**
     * The name of attribute which conatins password of ESME.
     */
    private static final String PASSWORD_ATTR = "password";

    private Debug debug = SmppObject.getDebug();
    private Event event = SmppObject.getEvent();

    /**
     * Constructs the PDU processor with given session,
     * message store for storing of the messages and a table of
     * users for authentication.
     * @param session the sessin this PDU processor works for
     * @param messageStore the store for messages received from the client
     * @param users the list of users used for authenticating of the client
     */   
    public SimulatorPDUProcessor(SMSCSession session,
        ShortMessageStore messageStore, Table users) {
        this.session = session;
        this.messageStore = messageStore;
        this.users = users;
    }

    /**
     * Depending on the <code>commandId</code>
     * of the <code>request</code> creates the proper response.
     * The first request must be a <code>BindRequest</code> with the correct
     * parameters.
     * @param request the request from client
     */
    private static int j = 0;

    public void clientRequest(Request request)
    {
      DBLogger dbLogger=new DBLogger();
      /*
      long i = Thread.currentThread().getId();
      System.out.println("RECEIVED-" + i);
      if(j == 0) {
        j = 1;
        try {
          System.out.println("SLEEPING-" + i);
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      }
      System.out.println("LEFT-" + i);
      if(true) {
        return;
      }
      */
      
      
        debug.write("SimulatorPDUProcessor.clientRequest() " + request.debugString());
        logger.info("SimulatorPDUProcessor.clientRequest() " + request.debugString());
        
        Response response;
        int commandStatus;
        int commandId = request.getCommandId();
        try {
        	
            display("client request: "+request.debugString());
            logger.info( "Client request: "+request.debugString() );
            
            if (!bound) { // the first PDU must be bound request
                if (commandId == Data.BIND_TRANSMITTER ||
                    commandId == Data.BIND_RECEIVER ||
                    commandId == Data.BIND_TRANSCEIVER) {
                    commandStatus = checkIdentity((BindRequest)request);
                    if (commandStatus == 0) { // authenticated
                        // firstly generate proper bind response
                        BindResponse bindResponse =
                            (BindResponse)request.getResponse();
                        bindResponse.setSystemId(SYSTEM_ID);
                        // and send it to the client via serverResponse
                        serverResponse(bindResponse);
                        // success => bound
                        bound = true;
                    } else { // system id not authenticated
                        // get the response
                        response = request.getResponse();
                        // set it the error command status
                        response.setCommandStatus(commandStatus);
                        // and send it to the client via serverResponse
                        serverResponse(response);
                        // bind failed, stopping the session
                        session.stop();
                    }
                } else {
                    // the request isn't a bound req and this is wrong: if not
                    // bound, then the server expects bound PDU
                    if (request.canResponse()) {
                        // get the response
                        response = request.getResponse();
                        response.setCommandStatus(Data.ESME_RINVBNDSTS);
                        // and send it to the client via serverResponse
                        serverResponse(response);
                    } else {
                        // cannot respond to a request which doesn't have
                        // a response :-(
                    }
                    // bind failed, stopping the session
                    session.stop();
                }
            } else { // already bound, can receive other PDUs
                if (request.canResponse()) {
                  
                    response = request.getResponse();
                    String strURLParam = null;
                    String strAccessToken = null;
                    String strRequestJson = null;
                    String strResponseJson = null;
                    int iResponseStatus = 0;
                    IMIFEService nextService = null;
                    
                    switch(commandId) { // for selected PDUs do extra steps
                    case Data.SUBMIT_SM:
                      
                      logger.info("SUBMIT_SM");
                      SubmitSM objSubmitRequest = (SubmitSM)request;
                      boolean processImmediately = true;
                      boolean isLongMessage = SMSUtil.isPartialAvailable( objSubmitRequest );
                      if( !SMSUtil.isPayloadFieldSet( objSubmitRequest ) && !SMSUtil.isShortMessageFieldSet( objSubmitRequest ) ){
                    	  logger.info("Neither Short MSG or Payload were set!!");
                    	  System.out.println("ERROR :: Neither Short MSG or Payload were set!!");
                        
                      }else{
                        
                          //Note : Check if the SMS is a long SMS, then populate the map until all partials
                          //arrive. Then concatenate and process.
                    	  SubmitSM objLastPartial = null;
                        String uniqueRefNumber = null;
                        
                        StringBuilder builder =new StringBuilder();
                		builder.append("CB");
                		builder.append(new Date().getTime());
                		Random random=new Random();
                		builder.append(random.nextInt());
                		String cbUniqueRefNumber=builder.toString();
                		
                        logger.info("Is long SMS message : "+isLongMessage);
                        
                          if(isLongMessage) {
                            uniqueRefNumber = String.valueOf(objSubmitRequest.getSarMsgRefNum());
                        	  
                            dbLogger.logSMSRequstFromSME(objSubmitRequest, true,cbUniqueRefNumber);
                            int iTotalNumberOfPartials = objSubmitRequest.getSarTotalSegments();
                            logger.info("Total number of partials in long SMS : "+iTotalNumberOfPartials);
                            
                              if( iTotalNumberOfPartials > 1 ){

                                short shMessageSeqNumber = objSubmitRequest.getSarMsgRefNum();

                                //Checking for partials
                                List<Request> lstPUD = mapLongSMSPartials.get( shMessageSeqNumber );

                                if( lstPUD == null ){
                                  lstPUD = new ArrayList<Request>(iTotalNumberOfPartials);
                                  lstPUD.add( objSubmitRequest.getSarSegmentSeqnum() - 1, request );
                                  mapLongSMSPartials.put( shMessageSeqNumber, lstPUD );
                                  
                                }else{
                                  lstPUD.add( ((SubmitSM)request).getSarSegmentSeqnum() - 1, request );
                                  
                                }

                                //Check if all partials have arriaved
                                if( iTotalNumberOfPartials == lstPUD.size() ){

                                  SubmitSM tmpSubmitSM = null;
                                  String strConcatMsg = "";
                                  for( int i = 0; i< lstPUD.size(); i++ ){
                                    
                                    tmpSubmitSM = (SubmitSM)lstPUD.get(i);
                                    
                                    if( SMSUtil.isShortMessageFieldSet( tmpSubmitSM ) ){
                                        strConcatMsg += tmpSubmitSM.getShortMessage();
                                        
                                    }else if( SMSUtil.isPayloadFieldSet( tmpSubmitSM ) ){
                                      //Get from payload
                                      strConcatMsg += SMSUtil.getMessageFromPayload( tmpSubmitSM );
                                      
                                    }

                                  }
                                  
                                  //Using last partial as anchor
                                  objLastPartial = (SubmitSM)lstPUD.get( lstPUD.size()-1 );
                                  if( SMSUtil.isShortMessageFieldSet( objSubmitRequest ) ){
                                	  objLastPartial.setShortMessage( strConcatMsg );
                                	  
                                  }else{
                                	  objLastPartial.setMessagePayload( new ByteBuffer( strConcatMsg.getBytes() ) );
                                	  
                                  }
                                  
                                  mapLongSMSPartials.remove( shMessageSeqNumber );
                                  processImmediately = true;
                                }else{
                                  processImmediately = false;
                                }
                              }
                          } else {
                            dbLogger.logSMSRequstFromSME(objSubmitRequest, false,cbUniqueRefNumber );
                            uniqueRefNumber = String.valueOf(objSubmitRequest.getSequenceNumber());
                          }
                          
                          if( processImmediately ){

                            //logging to db of request to smscsim from external
//                            dbLogger.logSMSRequstToSmscsim(objSubmitRequest, false, (short)1);
                        	  
                        	  String outboundURI = PropertyReader.getPropertyValue(SMSCSIMProperties.OUTBOUND_URI);
                        	  strURLParam = MessageFormat.format(outboundURI, objSubmitRequest.getSourceAddr().getAddress());
                              //strURLParam = "/outbound/tel:+"+objSubmitRequest.getSourceAddr().getAddress()+"/requests";
                              
                              objSubmitRequest = ( objLastPartial != null )? objLastPartial : objSubmitRequest;
                              //lk.dialog.smsc.mife.request.sendsms.OutboundSMSMessageRequest sendSMSRequest = JsonUtil.buildOutboundSMSMessageRequestAsRequest(objSubmitRequest, uniqueRefNumber, isLongMessage);
                              lk.dialog.smsc.mife.request.sendsms.OutboundSMSMessageRequest sendSMSRequest = JsonUtil.buildOutboundSMSMessageRequestAsRequest(objSubmitRequest, cbUniqueRefNumber, isLongMessage);

                              MifeSMSRequest mifeSmsRequest = new MifeSMSRequest();
                              mifeSmsRequest.setOutboundSMSMessageRequest(sendSMSRequest);

                              strAccessToken = Refresher.getToken();
                              strRequestJson = new Gson().toJson(mifeSmsRequest);
                              
                              //logging resquest to mife from smscsim
                             dbLogger.logSMSRequstToHub(objSubmitRequest, strRequestJson);
                              
                             nextService = MIFEServicePool.getNextService(MIFEServicePool.TYPE_SEND_SMS);
                             logger.info("HHHHHHHHHHHHH         strURLParam : "+strURLParam);
                             logger.info("HHHHHHHHHHHHH         strRequestJson : "+strRequestJson);
                             
                             HttpResponse objSubmitResponse = nextService.handleRequest( strURLParam, strRequestJson, strAccessToken );
                             MIFEServicePool.releaseService(MIFEServicePool.TYPE_SEND_SMS, nextService);
                              
//                              iResponseStatus = objSubmitResponse.getStatusLine().getStatusCode();
                              
                              strResponseJson = EntityUtils.toString( objSubmitResponse.getEntity() );
                              //System.out.println( strResponseJson );
                              logger.info("HHHHHHHHHHHHH         strResponseJson : "+strResponseJson); 
                              OutboundSMSMessageRequest sendSMSResponse = new Gson().fromJson( strResponseJson, OutboundSMSMessageRequest.class );
                              //logging response from mife to smscsim
                            dbLogger.logSMSResponseFromHub(sendSMSResponse);

                          }
                        
                      }

                      SubmitSMResp submitResponse = (SubmitSMResp)response;
                      submitResponse.setMessageId( assignMessageId() );
                      
                    
                      //log from smscsim to external on delivery notice
                      dbLogger.logSMSResponseToSME(submitResponse,isLongMessage);

                      display("putting message into message store");
                      messageStore.submit((SubmitSM)request,
                                          submitResponse.getMessageId(),systemId);
                      byte registeredDelivery =
                          (byte)(((SubmitSM)request).getRegisteredDelivery() &
                          Data.SM_SMSC_RECEIPT_MASK);
                      
                      //=====================SMPP PATCH=====================DN Subscription
                      /*if (registeredDelivery == Data.SM_SMSC_RECEIPT_REQUESTED) {
                          deliveryInfoSender.submit(this,(SubmitSM)request,
                                                    submitResponse.getMessageId());
                          //log from smscsim to external on delivery notice
                          //dbLogger.logSMSResponseToSME((SubmitSM)request);
                      }*/
                      //=====================SMPP PATCH=====================DN Subscription
                      break;

                    case Data.SUBMIT_MULTI:
                      
                      logger.info("SUBMIT_MULTI");
                      SubmitMultiSM multisubmitRequest =(SubmitMultiSM)request;
                      boolean partialMessage = SMSUtil.isPartialAvailable( multisubmitRequest );
                        //logging multi sms from external to smscsim
                        dbLogger.LogMultiSMSRequestFromSME(multisubmitRequest, partialMessage);
                      boolean processMultiImmediately = true;
                      
                        if( !SMSUtil.isPayloadFieldSet( multisubmitRequest ) && !SMSUtil.isShortMessageFieldSet( multisubmitRequest ) ){
                          //Error, neither short msg or payload fields are set
                          logger.error("ERROR MULTI SM:: Neither Short MSG or Payload were set!!");
                          System.out.println("ERROR MULTI SM:: Neither Short MSG or Payload were set!!");
                          
                        }else{
                      
                           //Note : Check if the SMS is a long SMS, then populate the map until all partials
                           //arrive. Then concatenate and process.
                           SubmitMultiSM objLastPartial = null;
                          
                            if(partialMessage){
                              
                              int iTotalNumberOfPartials = multisubmitRequest.getSarTotalSegments();
                                if( iTotalNumberOfPartials > 1 ){

                                  short shMessageSeqNumber = multisubmitRequest.getSarMsgRefNum();
                                  
                                  //Checking for partials
                                  List<Request> lstPUD = mapLongSMSPartials.get( shMessageSeqNumber );
                                  if( lstPUD == null ){
                                    lstPUD = new ArrayList<Request>();
                                    lstPUD.add( ((SubmitMultiSM)request).getSarSegmentSeqnum() - 1, request );
                                    mapLongSMSPartials.put( shMessageSeqNumber, lstPUD );
                                    
                                  }else{
                                    lstPUD.add( ((SubmitMultiSM)request).getSarSegmentSeqnum() - 1, request );
                                    
                                  }

                                  //Check if all partials have arriaved
                                  if( iTotalNumberOfPartials == lstPUD.size() ){

                                    SubmitMultiSM tmpSubmitMultiSM = null;
                                    String strConcatMsg = "";
                                    
                                    for( int i = 0; i< lstPUD.size(); i++ ){
                                      
                                      tmpSubmitMultiSM = (SubmitMultiSM)lstPUD.get(i);
                                      
                                      if( SMSUtil.isShortMessageFieldSet( tmpSubmitMultiSM ) ){
                                          strConcatMsg += tmpSubmitMultiSM.getShortMessage();
                                      }else if( SMSUtil.isPayloadFieldSet( tmpSubmitMultiSM ) ){
                                        //Get from payload
                                        strConcatMsg += SMSUtil.getMessageFromPayload( tmpSubmitMultiSM );
                                      }

                                    }
                                    
                                    //Using last partial as anchor
                                    objLastPartial = (SubmitMultiSM)lstPUD.get( lstPUD.size()-1 );
                                    if( SMSUtil.isShortMessageFieldSet( multisubmitRequest ) ){
                                  	  objLastPartial.setShortMessage( strConcatMsg );
                                    }else{
                                  	  objLastPartial.setMessagePayload( new ByteBuffer( strConcatMsg.getBytes() ) );
                                    }
                                    
                                    mapLongSMSPartials.remove( shMessageSeqNumber );
                                    processMultiImmediately = true;
                                    
                                  }else{
                                    processMultiImmediately = false;
                                    
                                  }

                                }
                              
                            }
                          
                            if( processMultiImmediately ){
                            	String outboundURI = PropertyReader.getPropertyValue(SMSCSIMProperties.OUTBOUND_URI);
                          	  	strURLParam = MessageFormat.format(outboundURI, multisubmitRequest.getSourceAddr().getAddress());
                                //strURLParam = "/outbound/tel:+"+multisubmitRequest.getSourceAddr().getAddress()+"/requests";
                                
                                multisubmitRequest = ( objLastPartial != null )? objLastPartial : multisubmitRequest;
                                lk.dialog.smsc.mife.request.sendsms.OutboundSMSMessageRequest sendSMSMultiRequest = JsonUtil.buildOutboundSMSMessageRequestAsRequest( multisubmitRequest );
                                
                                MifeSMSRequest mifeMultiSmsRequest = new MifeSMSRequest();
                                mifeMultiSmsRequest.setOutboundSMSMessageRequest(sendSMSMultiRequest);
                                //System.out.println(new Gson().toJson(mifeMultiSmsRequest));

                                strAccessToken = Refresher.getToken();
                                strRequestJson = new Gson().toJson(mifeMultiSmsRequest);
                                
                                //logging multi sms request to mife from smscsim
                                dbLogger.LogMultiSMSRequestToHub( mifeMultiSmsRequest, strRequestJson );
                                
                                nextService = MIFEServicePool.getNextService(MIFEServicePool.TYPE_SEND_MULTI);
                                HttpResponse objMultiResponse = nextService.handleRequest( strURLParam, strRequestJson, strAccessToken );
                                MIFEServicePool.releaseService( MIFEServicePool.TYPE_SEND_MULTI, nextService );

                                iResponseStatus = objMultiResponse.getStatusLine().getStatusCode();
                                
                                strResponseJson = EntityUtils.toString( objMultiResponse.getEntity() );
                                System.out.println( strResponseJson );
                                
                                OutboundSMSMessageRequest sendSMSResponse = new Gson().fromJson( strResponseJson, OutboundSMSMessageRequest.class );
                               
                                //log for response from mife to smscsim
                                dbLogger.logMultiSMSResponseFromHub(sendSMSResponse);
                                
                            }
                            
                            SubmitMultiSMResp submitMultiResponse = (SubmitMultiSMResp)response;
                            submitMultiResponse.setMessageId(assignMessageId());

                            //Note :: There are getters in the response where unsuccessful number and SME's are set
                            //but no setters available.
                            //submitMultiResponse.setExtraOptional( (short)Tag.SAR_TOTAL_SEGMENTS.intValue(), new ByteBuffer() );
                            //submitMultiResponse.setExtraOptional( (short)Tag.SAR_SEGMENT_SEQNUM.intValue(), new ByteBuffer() );
                            //submitMultiResponse.setExtraOptional( (short)Tag.SAR_MSG_REF_NUM.intValue(),   new ByteBuffer() );
                            
                            //log response from smscsim to external
                            dbLogger.logMultiSMSResponseToSME( submitMultiResponse );
                            
                        }
                        
                        break;

                    case Data.DELIVER_SM:

                      DeliverSM deliverRequest = (DeliverSM)request;
                      
                        strURLParam = "/outbound/subscriptions";
                        strAccessToken = Refresher.getToken();
                        strRequestJson = "{\"deliveryReceiptSubscription\":{\"callbackReference\":{\"callbackData\":\"12345\",\"notifyURL\":\"http://localhost:8080/DemoService/wso2/telco/demo/notifymo\"},\"clientCorrelator\":\"12345\",\"senderAddresses\":[{\"senderAddress\":\"7555\",\"operatorCode\":\"DIALOG\",\"filterCriteria\":\"123456\"}]}}";
                        IMIFEService nextService2 = MIFEServicePool.getNextService(MIFEServicePool.TYPE_DELIVER_SM);
                        HttpResponse objDeliverSMResponse = nextService2.handleRequest( strURLParam, strRequestJson, strAccessToken );

                        iResponseStatus = objDeliverSMResponse.getStatusLine().getStatusCode();
                        MIFEServicePool.releaseService(MIFEServicePool.TYPE_DELIVER_SM, nextService2);
                        strResponseJson = EntityUtils.toString( objDeliverSMResponse.getEntity() );
                        System.out.println( strResponseJson );

                        DeliverSMResp deliverResponse = (DeliverSMResp)response;
                        deliverResponse.setMessageId(assignMessageId());
                        break;

                    case Data.DATA_SM:

                        DataSMResp dataResponse = (DataSMResp)response;
                        dataResponse.setMessageId(assignMessageId());
                        break;

                    case Data.QUERY_SM:
                    	
                    	logger.info("QUERY_SM");
                        QuerySM queryRequest = (QuerySM)request;
                        
                        //log of querysm request from smscsim to mife 
                        dbLogger.logQuerySMRequestFromSME(queryRequest);                                                     
                        
                        String strSenderddress = queryRequest.getSourceAddr().getAddress();
                        String strRequestId = queryRequest.getMessageId();
                        int iSequenceNumber = queryRequest.getSequenceNumber();
                        
                        String outboundURIDeliveryInfos = PropertyReader.getPropertyValue(SMSCSIMProperties.OUTBOUND_URL_DELIVERY_INFOS);
                  	  	strURLParam = MessageFormat.format(outboundURIDeliveryInfos, strSenderddress, strRequestId);
                        //strURLParam = "/outbound/tel:+"+strSenderddress+"/requests/"+strRequestId+"/deliveryInfos";
                        strAccessToken = Refresher.getToken();
                      
                        dbLogger.logQuerySMRequestToHub(queryRequest, "Source Address : "+strSenderddress+" Message Id : "+strRequestId);
                        nextService = MIFEServicePool.getNextService(MIFEServicePool.TYPE_QUERY_SM);
                        HttpResponse objQuerySMResponse = nextService.handleRequest(strURLParam, null, strAccessToken);
                        MIFEServicePool.releaseService( MIFEServicePool.TYPE_QUERY_SM, nextService );
            
                        iResponseStatus = objQuerySMResponse.getStatusLine().getStatusCode();

                        strResponseJson = EntityUtils.toString( objQuerySMResponse.getEntity() );
                        System.out.println( strResponseJson );
                        
                        //OutboundSMSMessageRequest sendSMSResponse = new Gson().fromJson( strResponseJson, OutboundSMSMessageRequest.class );
                        
                        //log querysm response from mife to smscsim
                        dbLogger.logQuerySMResponseFromHub( iSequenceNumber, strResponseJson, strSenderddress );
                        
                        QuerySMResp queryResponse = (QuerySMResp)response;
                        display("querying message in message store");
                        queryResponse.setMessageId(queryRequest.getMessageId());

                        if( iResponseStatus == 200 ){
                          queryResponse.setMessageState( Data.DFLT_MSG_STATE );
                          queryResponse.setErrorCode( (byte)Data.DFLT_ERR );
                        }else{
                          queryResponse.setMessageState( Data.DFLT_MSG_STATE );
                          queryResponse.setErrorCode( (byte)Data.ESME_RQUERYFAIL );
                        }
                        
                        //log for querysm response from smscsim to external
                        dbLogger.logQuerySMResponseToSME( iSequenceNumber, queryResponse, strSenderddress );
                        break;

                    case Data.CANCEL_SM:
                        CancelSM cancelRequest = (CancelSM)request;
                        display("cancelling message in message store");
                        messageStore.cancel(cancelRequest.getMessageId());
                        break;

                    case Data.REPLACE_SM:
                        ReplaceSM replaceRequest = (ReplaceSM)request;
                        display("replacing message in message store");
                        messageStore.replace(replaceRequest.getMessageId(),
                                             replaceRequest.getShortMessage());
                        break;

                    case Data.UNBIND:
                        // do nothing, just respond and after sending
                        // the response stop the session
                        break;
                    }
                    // send the prepared response
                    serverResponse(response);
                    if (commandId == Data.UNBIND) {
                        // unbind causes stopping of the session
                        session.stop();
                    }
                } else {
                    // can't respond => nothing to do :-)
                }
            }
        } catch (WrongLengthOfStringException e) {
        	logger.error("SimulatorPDUProcessor::clientRequest ", e );
            event.write(e, "");
            
        } catch ( Exception e ){
          logger.error("SimulatorPDUProcessor::clientRequest ", e );
          e.printStackTrace();
          event.write(e, "");
          
        }
        
    }


    /**
     * Processes the response received from the client.
     * @param response the response from client
     */
    public void clientResponse(Response response)
    {
        debug.write("SimulatorPDUProcessor.clientResponse() " +
                    response.debugString());
        display("client response: "+response.debugString());
    }
    
    /**
     * Sends a request to a client. For example, it can be used to send
     * delivery info to the client.
     * @param request the request to be sent to the client
     */
    public void serverRequest(Request request)
    {
        debug.write("SimulatorPDUProcessor.serverRequest() " +
                    request.debugString());
        display("server request: "+request.debugString());
        session.send(request);
    }
    
    /**
     * Send the response created by <code>clientRequest</code> to the client.
     * @param response the response to send to client
     */
    public void serverResponse(Response response)
    {
        debug.write("SimulatorPDUProcessor.serverResponse() " + response.debugString());
        logger.info("SimulatorPDUProcessor.serverResponse() " + response.debugString());
        
        display("server response: "+response.debugString());
        session.send(response);
    }
    
    /**
     * Checks if the bind request contains valid system id and password.
     * For this uses the table of users provided in the constructor of the
     * <code>SimulatorPDUProcessor</code>. If the authentication fails,
     * i.e. if either the user isn't found or the password is incorrect,
     * the function returns proper status code.
     * @param request the bind request as received from the client
     * @return status code of the authentication; ESME_ROK if authentication
     *         passed
     */
    private int checkIdentity(BindRequest request)
    {   
        int commandStatus = Data.ESME_ROK;
        Record user = users.find(SYSTEM_ID_ATTR,request.getSystemId());
        if (user != null) {
            String password = user.getValue(PASSWORD_ATTR);
            if (password != null) {
                if (!request.getPassword().equals(password)) {
                    commandStatus = Data.ESME_RINVPASWD;
                    debug.write("system id "+request.getSystemId()+
                                " not authenticated. Invalid password.");
                    display("not authenticated "+request.getSystemId()+
                            " -- invalid password");
                } else {
                    systemId = request.getSystemId();
                    debug.write("system id " + systemId + " authenticated");
                    display("authenticated "+systemId);
                }
            } else {
                commandStatus = Data.ESME_RINVPASWD;
                debug.write("system id " + systemId + " not authenticated. "+
                            "Password attribute not found in users file");
                display("not authenticated "+systemId+
                        " -- no password for user.");
            }
        } else {
            commandStatus = Data.ESME_RINVSYSID;
            debug.write("system id " + request.getSystemId() +
                        " not authenticated -- not found");
            display("not authenticated "+request.getSystemId()+
                    " -- user not found");
        }
        return commandStatus;
    }
    
    /**
     * Creates a unique message_id for each sms sent by a client to the smsc.
     * @return unique message id
     */
    private String assignMessageId()
    {
    	String messageId = "";
        intMessageId++;
        messageId += intMessageId;
        return messageId;
        
        //------------------Null Byte PATCH------------------//
		
		/**
		String newStr="";
		byte[] mid = messageId.getBytes();
        mid = java.util.Arrays.copyOf(mid, mid.length+1);
        newStr=new String(mid);
        */
        
        /**
        try {
			byte byteArr2='\u0000';
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(messageId.getBytes());
			outputStream.write(byteArr2);
			byte c[] = outputStream.toByteArray();
			newStr=new String(c);    			
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
        //------------------Null Byte PATCH------------------//
    }

    /**
     * Returns the session this PDU processor works for.
     * @return the session of this PDU processor
     */
    public SMSCSession getSession() { return session; }

    /**
     * Returns the system id of the client for whose is this PDU processor
     * processing PDUs.
     * @return system id of client
     */
    public String getSystemId() { return systemId; }

    /**
     * Sets if the info about processing has to be printed on
     * the standard output.
     */
    public void setDisplayInfo(boolean on) { displayInfo = on; }

    /**
     * Returns status of printing of processing info on the standard output.
     */
    public boolean getDisplayInfo() { return displayInfo; }

    /**
     * Sets the delivery info sender object which is used to generate and send
     * delivery pdus for messages which require the delivery info as the outcome
     * of their sending.
     */
    public void setDeliveryInfoSender(DeliveryInfoSender deliveryInfoSender)
    {
        this.deliveryInfoSender = deliveryInfoSender;
    }

    private void display(String info)
    {
        if (getDisplayInfo()) {
            String sysId = getSystemId();
            if (sysId == null) {
                sysId = "";
            }
            System.out.println(FileLog.getLineTimeStamp() + " ["+sysId+"] "+info);
        }
    }
    
}
