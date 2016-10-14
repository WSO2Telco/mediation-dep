package lk.dialog.smsc.db;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.dialog.smsc.mife.request.sendsms.MifeSMSRequest;
import lk.dialog.smsc.mife.response.sendsms.OutboundSMSMessageRequest;
import lk.dialog.smsc.util.PDU;

import com.google.gson.Gson;
import com.logica.smpp.pdu.QuerySM;
import com.logica.smpp.pdu.QuerySMResp;
import com.logica.smpp.pdu.SubmitMultiSM;
import com.logica.smpp.pdu.SubmitMultiSMResp;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.pdu.SubmitSMResp;
import com.logica.smpp.pdu.ValueNotSetException;
import com.logica.smscsim.SimulatorPDUProcessor;

public class DBLogger {
	
	static Logger logger = LoggerFactory.getLogger( DBLogger.class );
	
  //logging the request details sent from outside to smscsim
  public void logSMSRequstFromSME(SubmitSM objSubmitSM, boolean partialSMS) throws Exception  {
    String msgBody = null;
    try {
        byte[] msgPayLoadArr = objSubmitSM.getMessagePayload().getBuffer();
        msgBody = new String(msgPayLoadArr);
      } catch (Exception e) {
        msgBody = objSubmitSM.getShortMessage("UTF-8");
      }
    msgBody=msgBody.replace("'", "");
    String address = objSubmitSM.getSourceAddr().getAddress();
    String refNo = String.valueOf(objSubmitSM.getSequenceNumber());
    boolean needDelivReport = isDeviceryReportRequired(objSubmitSM);
    short sarSegmantSeqNo = 0;
    short sarRefNo = 0;
    short sarTotalSegmants = 0;
    if(partialSMS) {
      sarSegmantSeqNo = objSubmitSM.getSarSegmentSeqnum();
      sarRefNo = objSubmitSM.getSarMsgRefNum();
      sarTotalSegmants = objSubmitSM.getSarTotalSegments();
    }
    logRequstFromSME(partialSMS, address, refNo, msgBody, needDelivReport, sarSegmantSeqNo, sarRefNo, sarTotalSegmants, PDU.SUBMIT_SM);
  }
  
  private void logRequstFromSME(boolean partialSMS, String address, String refNo, String body, boolean needDelivReport, short sarSegmantSeqNo
      , short sarRefNo, short sarTotalSegmants, PDU pdu) throws Exception  {
    Connection connection = DBConnectionPool.getConnection();
    String sql = "INSERT INTO smse_req_resp_log "
        + "(sender_addr, ref_num, body, type_request, sme_id, need_delivery_report, sar_seq_num, sar_ref_num, sar_total_segmants, pdu, logged_at) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
    PreparedStatement stmt = connection.prepareStatement(sql);
    stmt.setString(1,  address);
    stmt.setString(2, refNo);
    stmt.setString(3, body);
    stmt.setInt(4, 1);
    stmt.setInt(5, 1);
    stmt.setBoolean(6, needDelivReport);
    stmt.setInt(7, sarSegmantSeqNo);
    stmt.setInt(8, sarRefNo);
    stmt.setInt(9, sarTotalSegmants);
    stmt.setInt(10, pdu.value());
    logger.info("logRequstFromSME : "+stmt.toString());
    stmt.executeUpdate();
    DBConnectionPool.releaseConnection(connection);
  }

  private boolean isDeviceryReportRequired(SubmitSM objSubmitSM) {
    try {
      return objSubmitSM.getAlertOnMsgDelivery();
    } catch (Exception e) {
    	//logger.error( "DBLogger::isDeviceryReportRequired ", e );
        return true;
    }
  }
  
  private boolean isDeviceryReportRequired(SubmitMultiSM multisubmitRequest) {
    try {
      return multisubmitRequest.getAlertOnMsgDelivery();
    } catch (Exception e) {
    	//logger.error( "DBLogger::isDeviceryReportRequired [SUBMIT_MULTI]", e );
      return true;
    }
  }

  //logging the request details sent from smscsim to mife
  public void logSMSRequstToHub(SubmitSM objSubmitSM, String msgBody) throws Exception {
    msgBody = msgBody.replace("'", "");
    Connection connection = DBConnectionPool.getConnection();
    String sql = "INSERT INTO hub_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu, logged_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
    PreparedStatement stmt = connection.prepareStatement(sql);
    stmt.setString(1, objSubmitSM.getSourceAddr().getAddress());
    stmt.setString(2, "" + objSubmitSM.getSequenceNumber());
    stmt.setString(3, msgBody);
    stmt.setInt(4, 1);
    stmt.setInt(5, 1);
    stmt.setInt(6, PDU.SUBMIT_SM.value());
    logger.info("logSMSRequstToHub : "+stmt.toString());
    stmt.executeUpdate();
    DBConnectionPool.releaseConnection(connection);
  }

  //logging the response details sent from mife to smscsim
  public void logSMSResponseFromHub(OutboundSMSMessageRequest objObdSMSReq) throws Exception {
	if (objObdSMSReq!=null && objObdSMSReq.getOutboundSMSMessageRequest()!=null && objObdSMSReq.getOutboundSMSMessageRequest().getAddress()!=null) {
	    String destAdd = objObdSMSReq.getOutboundSMSMessageRequest().getAddress().toString();
	    destAdd = destAdd.replace("[tel:", "");
	    destAdd = destAdd.replace("]", "");
	    String sendAddr = objObdSMSReq.getOutboundSMSMessageRequest().getSenderAddress().replace("tel:", "");

	    Connection connection = DBConnectionPool.getConnection();
	    String sql = "INSERT INTO hub_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu, logged_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
	    PreparedStatement stmt = connection.prepareStatement(sql);
	    stmt.setString(1, sendAddr);
	    stmt.setString(2, String.valueOf(objObdSMSReq.getOutboundSMSMessageRequest().getClientCorrelator()));
	    //stmt.setString(3, objObdSMSReq.getOutboundSMSMessageRequest().getOutboundSMSTextMessage().getMessage().replace("'", ""));
	    stmt.setString(3, new Gson().toJson(objObdSMSReq) );
	    stmt.setInt(4, 0);
	    stmt.setInt(5, 1);
	    stmt.setInt(6, PDU.SUBMIT_SM.value());
	    logger.info("logSMSResponseFromHub : "+stmt.toString());
	    stmt.executeUpdate();
	    DBConnectionPool.releaseConnection(connection);
	}  
  }

  // logging the response details sent from smscsim to outside
  public void logSMSResponseToSME(/*SubmitSM*/SubmitSMResp submitSM, boolean isLongMessage)throws Exception {
    String msgBody = null;
    try {
      byte[] msgPayLoadArr = submitSM.getBody().getBuffer();
      msgBody = new String(msgPayLoadArr);
    } catch (Exception e) {
      msgBody = submitSM.getMessageId();
    }
    msgBody = msgBody.replace("'", "");
    String sendAddr = ((SubmitSM)submitSM.getOriginalRequest()).getSourceAddr().getAddress();

    Connection connection = DBConnectionPool.getConnection();
    //String sql = "INSERT INTO smse_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu, logged_at,sms_id) VALUES (?, ?, ?, ?, ?, ?, NOW(),?)";//EDITED
    String sql="UPDATE smse_req_resp_log SET sms_id=?,sar_seq_num=?,sar_ref_num=?,sar_total_segmants=?,type_request=0 WHERE ref_num=?";
    PreparedStatement stmt = connection.prepareStatement(sql);
    //stmt.setString(1, sendAddr);
    //stmt.setString(2, String.valueOf(submitSM.getSequenceNumber()));
    //stmt.setString(3, msgBody);
    //stmt.setInt(4, 0);
    //stmt.setInt(5, 1);
    //stmt.setInt(6, PDU.SUBMIT_SM.value());
    stmt.setString(1,submitSM.getMessageId());//TODO
    if (isLongMessage) {
    	stmt.setString(2, String.valueOf(((SubmitSM)submitSM.getOriginalRequest()).getSarMsgRefNum()));
        stmt.setString(3, String.valueOf(((SubmitSM)submitSM.getOriginalRequest()).getSarTotalSegments()));
        stmt.setString(4, String.valueOf(((SubmitSM)submitSM.getOriginalRequest()).getSarSegmentSeqnum()));
	}else{
		stmt.setString(2, "0");
        stmt.setString(3, "0");
        stmt.setString(4, "0");
	}
    stmt.setString(5, String.valueOf(submitSM.getSequenceNumber()));
    logger.info("logSMSResponseToSME : "+stmt.toString());
    stmt.executeUpdate();
    DBConnectionPool.releaseConnection(connection);
/*    String msgBody = null;
    try {
      byte[] msgPayLoadArr = submitSM.getMessagePayload().getBuffer();
      msgBody = new String(msgPayLoadArr);
    } catch (Exception e) {
      msgBody = submitSM.getShortMessage();
    }
    msgBody = msgBody.replace("'", "");
    String sendAddr = submitSM.getSourceAddr().getAddress();

    Connection connection = DBConnectionPool.getConnection();
    String sql = "INSERT INTO smse_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu) VALUES (?, ?, ?, ?, ?, ?)";
    PreparedStatement stmt = connection.prepareStatement(sql);
    stmt.setString(1, sendAddr);
    stmt.setString(2, String.valueOf(submitSM.getSequenceNumber()));
    stmt.setString(3, msgBody);
    stmt.setInt(4, 0);
    stmt.setInt(5, 1);
    stmt.setInt(6, PDU.SUBMIT_SM.value());
    stmt.executeUpdate();
    DBConnectionPool.releaseConnection(connection);*/
  }

  public void LogMultiSMSRequestFromSME(SubmitMultiSM multisubmitRequest, boolean partialSMS) throws Exception {
    String msgBody = null;
    try {
      byte[] msgPayLoadArr = multisubmitRequest.getMessagePayload().getBuffer();
      msgBody = new String(msgPayLoadArr);
    } catch (Exception e) {
      msgBody = multisubmitRequest.getShortMessage("UTF-8");
    }
    msgBody = msgBody.replace("'", "");
    String address = multisubmitRequest.getSourceAddr().getAddress();
    String refNo = String.valueOf(multisubmitRequest.getSequenceNumber());
    boolean needDelivReport = isDeviceryReportRequired(multisubmitRequest);
    short sarSegmantSeqNo = 0;
    short sarRefNo = 0;
    short sarTotalSegmants = 0;
    if(partialSMS) {
      sarSegmantSeqNo = multisubmitRequest.getSarSegmentSeqnum();
      sarRefNo = multisubmitRequest.getSarMsgRefNum();
      sarTotalSegmants = multisubmitRequest.getSarTotalSegments();
    }
    logRequstFromSME(partialSMS, address, refNo, msgBody, needDelivReport, sarSegmantSeqNo, sarRefNo, sarTotalSegmants, PDU.SUBMIT_SM_MULTI);
  }

  public void LogMultiSMSRequestToHub(MifeSMSRequest multisubmitRequest, String msgBody) throws Exception {
    msgBody = msgBody.replace("'", "");
    Connection connection = DBConnectionPool.getConnection();
    String sql = "INSERT INTO hub_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu, logged_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
    PreparedStatement stmt = connection.prepareStatement(sql);
    stmt.setString(1, multisubmitRequest.getOutboundSMSMessageRequest().getSenderAddress());
    stmt.setString(2, multisubmitRequest.getOutboundSMSMessageRequest().getClientCorrelator());
    stmt.setString(3, msgBody);
    stmt.setInt(4, 1);
    stmt.setInt(5, 1);
    stmt.setInt(6, PDU.SUBMIT_SM_MULTI.value());
    logger.info("LogMultiSMSRequestToHub : "+stmt.toString());
    stmt.executeUpdate();
    DBConnectionPool.releaseConnection(connection);
  }
  
  public void logMultiSMSResponseFromHub( OutboundSMSMessageRequest objObdSMSReq)throws Exception{
    String destAdd = objObdSMSReq.getOutboundSMSMessageRequest()
        .getAddress().toString();
    destAdd = destAdd.replace("[tel:", "");
    destAdd = destAdd.replace("]", "");
    String sendAddr = objObdSMSReq.getOutboundSMSMessageRequest()
        .getSenderAddress().replace("tel:", "");

    Connection connection = DBConnectionPool.getConnection();
    String sql = "INSERT INTO hub_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu, logged_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
    PreparedStatement stmt = connection.prepareStatement(sql);
    stmt.setString(1, sendAddr);
    stmt.setString(2, String.valueOf(objObdSMSReq.getOutboundSMSMessageRequest().getClientCorrelator()));
    //stmt.setString(3, objObdSMSReq.getOutboundSMSMessageRequest().getOutboundSMSTextMessage().getMessage().replace("'", ""));
    stmt.setString( 3, new Gson().toJson(objObdSMSReq) );
    stmt.setInt(4, 0);
    stmt.setInt(5, 1);
    stmt.setInt(6, PDU.SUBMIT_SM_MULTI.value());
    stmt.executeUpdate();
    logger.info("logMultiSMSResponseFromHub : "+stmt.toString());
    DBConnectionPool.releaseConnection(connection);
  }

  public void logMultiSMSResponseToSME( SubmitMultiSMResp submitMultiResponse ) throws Exception{
	  
	  String sendAddr = ((SubmitMultiSM)submitMultiResponse.getOriginalRequest()).getSourceAddr().getAddress();
	  String msgBody = null;
	  if( ((SubmitMultiSM)submitMultiResponse.getOriginalRequest()).getShortMessage() != null ){
		  msgBody = ((SubmitMultiSM)submitMultiResponse.getOriginalRequest()).getShortMessage("UTF-8");
	  }else{
		  msgBody = new String( ((SubmitMultiSM)submitMultiResponse.getOriginalRequest()).getMessagePayload().getBuffer() );
	  }
	  
	  Connection connection = DBConnectionPool.getConnection();
	  //String sql = "INSERT INTO smse_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu, logged_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
	  String sql="UPDATE smse_req_resp_log SET sms_id=?,type_request=0 WHERE ref_num=?"; 
	  	PreparedStatement stmt = connection.prepareStatement(sql);
	    /*stmt.setString( 1, sendAddr );
	    stmt.setString( 2, String.valueOf( submitMultiResponse.getSequenceNumber() ) );
	    stmt.setString( 3, msgBody );
	    stmt.setInt(4, 0);
	    stmt.setInt(5, 1);
	    stmt.setInt(6, PDU.SUBMIT_SM.value());*/
	    stmt.setString(1,submitMultiResponse.getMessageId());//EDITED
	    stmt.setString(2, String.valueOf(submitMultiResponse.getSequenceNumber()));//EDITED
	    
	    logger.info("logMultiSMSResponseToSME : "+stmt.toString());
	    stmt.executeUpdate();
	  DBConnectionPool.releaseConnection(connection);
  }

  public void logQuerySMRequestFromSME(QuerySM queryRequest)throws Exception{
    Connection connection = DBConnectionPool.getConnection();
    String sql = "INSERT INTO smse_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu, logged_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
    PreparedStatement stmt = connection.prepareStatement(sql);
    stmt.setString(1, queryRequest.getSourceAddr().getAddress());
    stmt.setString(2, "" + queryRequest.getSequenceNumber());
    stmt.setString(3, queryRequest.getMessageId());
    stmt.setInt(4, 1);
    stmt.setInt(5, 1);
    stmt.setInt(6, PDU.QUERY_SM.value());
    logger.info("logQuerySMRequestFromSME : "+stmt.toString());
    stmt.executeUpdate();
    DBConnectionPool.releaseConnection(connection);
  }
  
  public void logQuerySMRequestToHub(QuerySM queryRequest, String msgBody)throws Exception{
    Connection connection = DBConnectionPool.getConnection();
    String sql = "INSERT INTO hub_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu, logged_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
    PreparedStatement stmt = connection.prepareStatement(sql);
    stmt.setString(1, queryRequest.getSourceAddr().getAddress());
    stmt.setString(2, String.valueOf(queryRequest.getSequenceNumber()));
    stmt.setString(3, msgBody);
    stmt.setInt(4, 1);
    stmt.setInt(5, 1);
    stmt.setInt(6, PDU.QUERY_SM.value());
    logger.info("logQuerySMRequestToHub : "+stmt.toString());
    stmt.executeUpdate();
    DBConnectionPool.releaseConnection(connection);
  }
  
  public void logQuerySMResponseFromHub( int iSequenceNumber, String strResponseJson, String strSender )throws Exception{
	  /*
	  String destAdd = null;
	  if( (objObdSMSReq.getOutboundSMSMessageRequest().getAddress() != null) && (objObdSMSReq.getOutboundSMSMessageRequest().getAddress().size() != 0) ){
		  destAdd = objObdSMSReq.getOutboundSMSMessageRequest().getAddress().toString();
		  destAdd = destAdd.replace("[tel:", "");
		  destAdd = destAdd.replace("]", "");
	  }
	 
	String sendAddr = null;
	if( (objObdSMSReq.getOutboundSMSMessageRequest().getSenderAddress() != null ) && (objObdSMSReq.getOutboundSMSMessageRequest().getSenderAddress().length() > 0) ){
		sendAddr = objObdSMSReq.getOutboundSMSMessageRequest().getSenderAddress().replace("tel:", "");
	}
	 */
	  
    Connection connection = DBConnectionPool.getConnection();
    String sql = "INSERT INTO hub_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu, logged_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
    PreparedStatement stmt = connection.prepareStatement(sql);
    stmt.setString( 1, strSender );
    //stmt.setString( 2, String.valueOf( objObdSMSReq.getOutboundSMSMessageRequest().getClientCorrelator() ) );
    stmt.setString( 2, String.valueOf( iSequenceNumber ) );
    //stmt.setString( 3, objObdSMSReq.getOutboundSMSMessageRequest().getOutboundSMSTextMessage().getMessage().replace("'", ""));
    stmt.setString( 3, strResponseJson );
    stmt.setInt(4, 0);
    stmt.setInt(5, 1);
    stmt.setInt(6, PDU.QUERY_SM.value());
    logger.info("logQuerySMResponseFromHub : "+stmt.toString());
    stmt.executeUpdate();
    DBConnectionPool.releaseConnection(connection);
  }
  
  public void logQuerySMResponseToSME( int iSequenceNumber, QuerySMResp queryResponse, String strSenderAddress )throws Exception{
    Connection connection = DBConnectionPool.getConnection();
    String sql = "INSERT INTO smse_req_resp_log (sender_addr, ref_num, body, type_request, sme_id, pdu, logged_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
    PreparedStatement stmt = connection.prepareStatement(sql);
    stmt.setString(1, strSenderAddress );
    stmt.setString(2, String.valueOf(queryResponse.getSequenceNumber()));
    stmt.setString(3, queryResponse.getMessageId());
    stmt.setInt(4, 0);
    stmt.setInt(5, 1);
    stmt.setInt(6, PDU.QUERY_SM.value());
    logger.info("logQuerySMResponseToSME : "+stmt.toString());
    stmt.executeUpdate();
    DBConnectionPool.releaseConnection(connection);
  }

public void logSMSRequstFromSME(SubmitSM objSubmitSM, boolean partialSMS, String uniqueRefNumber,String systemId) throws Exception {
    String msgBody = null;
    try {
        byte[] msgPayLoadArr = objSubmitSM.getMessagePayload().getBuffer();
        msgBody = new String(msgPayLoadArr);
      } catch (Exception e) {
        msgBody = objSubmitSM.getShortMessage("UTF-8");
      }
    msgBody=msgBody.replace("'", "");
    String address = objSubmitSM.getSourceAddr().getAddress();
    String recieveAddress=objSubmitSM.getDestAddr().getAddress();
    String refNo = String.valueOf(objSubmitSM.getSequenceNumber());
    boolean needDelivReport = isDeviceryReportRequired(objSubmitSM);
    short sarSegmantSeqNo = 0;
    short sarRefNo = 0;
    short sarTotalSegmants = 0;
    if(partialSMS) {
      sarSegmantSeqNo = objSubmitSM.getSarSegmentSeqnum();
      sarRefNo = objSubmitSM.getSarMsgRefNum();
      sarTotalSegmants = objSubmitSM.getSarTotalSegments();
    }
    logRequstFromSME(partialSMS, address, refNo, msgBody, needDelivReport, sarSegmantSeqNo, sarRefNo, sarTotalSegmants, PDU.SUBMIT_SM,uniqueRefNumber,recieveAddress,systemId);
  }

	private void logRequstFromSME(boolean partialSMS, String address, String refNo, String body, boolean needDelivReport, short sarSegmantSeqNo
	      , short sarRefNo, short sarTotalSegmants, PDU pdu, String uniqueRefNumber,String recieveAddress,String systemId) throws Exception  {
	    Connection connection = DBConnectionPool.getConnection();
	    String sql = "INSERT INTO smse_req_resp_log "
	        + "(sender_addr, ref_num, body, type_request, sme_id, need_delivery_report, sar_seq_num, sar_ref_num, sar_total_segmants, pdu, logged_at,callback,address) "
	        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(),?,?)";
	    PreparedStatement stmt = connection.prepareStatement(sql);
	    stmt.setString(1,  address);
	    stmt.setString(2, refNo);
	    stmt.setString(3, body);
	    stmt.setInt(4, 1);
	    stmt.setInt(5, Integer.valueOf(systemId));
	    stmt.setBoolean(6, needDelivReport);
	    stmt.setInt(7, sarSegmantSeqNo);
	    stmt.setInt(8, sarRefNo);
	    stmt.setInt(9, sarTotalSegmants);
	    stmt.setInt(10, pdu.value());
	    stmt.setString(11, uniqueRefNumber);
	    stmt.setString(12, recieveAddress.replace("tel", "").replace(":", "").replace("+", ""));
	    logger.info("logRequstFromSME : "+stmt.toString());
	    stmt.executeUpdate();
	    DBConnectionPool.releaseConnection(connection);
	  }
  /*
   * public Statement createConnection() { Statement stmt = null; try {
   * Class.forName("com.mysql.jdbc.Driver"); conn =
   * DriverManager.getConnection(
   * DbPropertyReader.getPropertyValue("connection.url"),
   * DbPropertyReader.getPropertyValue("connection.username"),
   * DbPropertyReader.getPropertyValue("connection.password"));
   * 
   * stmt = conn.createStatement(); } catch (Exception e) {
   * e.printStackTrace();// TODO have to handle } return stmt; }
   */

	public String getUserIdFromName(String systemId) {
		    String clientId = "";
		    try {
		      Connection connection = DBConnectionPool.getConnection();
		      String sql = "SELECT id FROM sme where code= ?";
		      PreparedStatement preparedStatement = connection.prepareStatement(sql);
		      preparedStatement.setString(1, systemId);
		      ResultSet rs = preparedStatement.executeQuery();
		      while (rs.next()) {
		        clientId = rs.getString("id");
		      }
		      preparedStatement.close();
		      DBConnectionPool.releaseConnection(connection);
		    } catch (Exception e) {
		    	logger.error("Error in getUserIdFromName : " , e );
		    	e.printStackTrace();
		    }
		    return clientId;
		  }
	
	public String getNameFromId(int id) {
	    String code = "";
	    try {
	      Connection connection = DBConnectionPool.getConnection();
	      String sql = "SELECT code FROM sme where id= ?";
	      PreparedStatement preparedStatement = connection.prepareStatement(sql);
	      preparedStatement.setInt(1, id);
	      ResultSet rs = preparedStatement.executeQuery();
	      while (rs.next()) {
	    	  code = rs.getString("code");
	      }
	      preparedStatement.close();
	      DBConnectionPool.releaseConnection(connection);
	    } catch (Exception e) {
	    	logger.error("Error in getNameFromId : " , e );
	    	e.printStackTrace();
	    }
	    return code;
	  }

}
