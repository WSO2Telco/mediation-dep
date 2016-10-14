package lk.dialog.smsc.util;


import lk.dialog.smsc.mife.request.sendsms.OutboundSMSMessageRequest;
import lk.dialog.smsc.mife.request.sendsms.OutboundSMSTextMessage;
import lk.dialog.smsc.mife.request.sendsms.ReceiptRequest;

import java.util.HashMap;
import java.util.Map;

import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.SubmitMultiSM;
import com.logica.smpp.pdu.SubmitSM;

public class JsonUtil {
  public static OutboundSMSMessageRequest buildOutboundSMSMessageRequestAsRequest(SubmitSM submitSM, String uniqueRefNumber, boolean isLongMessage,Map<String,String> shortCord) {
	  
	try {
		
	    OutboundSMSMessageRequest sendSMSRequest = new OutboundSMSMessageRequest();
	    sendSMSRequest.setAddress(new String[] { "tel:+" + submitSM.getDestAddr().getAddress() });
	    if(shortCord.get("senderAddress")!=null){
	    sendSMSRequest.setSenderAddress("tel:" + shortCord.get("senderAddress"));
		}else {
	    sendSMSRequest.setSenderAddress("tel:" +submitSM.getSourceAddr().getAddress());
		}
	    
	    if (shortCord.get("senderName")!=null) {
	    	sendSMSRequest.setSenderName(shortCord.get("senderName").toString());
		}
	    OutboundSMSTextMessage outboundSMSTextMessage = new OutboundSMSTextMessage();
	    //Check if payload or short message
	    if( SMSUtil.isShortMessageFieldSet( submitSM ) ){
	    	outboundSMSTextMessage.setMessage(submitSM.getShortMessage("UTF-8"));
	    }else{
	    	outboundSMSTextMessage.setMessage( new String( submitSM.getMessagePayload().getBuffer(), "UTF-8") );
	    }
	    
	    sendSMSRequest.setOutboundSMSTextMessage(outboundSMSTextMessage);
	    sendSMSRequest.setClientCorrelator(String.valueOf(submitSM.getSequenceNumber()));
	    
	    ReceiptRequest receiptRequest = new ReceiptRequest();
	    receiptRequest.setNotifyURL( PropertyReader.getPropertyValue(SMSCSIMProperties.NOTIFY_URL_DELIVERY) );
	    //receiptRequest.setCallbackData(uniqueRefNumber + "," + isLongMessage);
	    receiptRequest.setCallbackData(uniqueRefNumber);
	    sendSMSRequest.setReceiptRequest(receiptRequest);
	    return sendSMSRequest;
	    
	} catch (Exception e) {
		return null;
	}

  }

  // if msg.getSourceAddr().getAddress() is alphanumeric then set it as sender name
  private static String getSenderName(Address addressNode) {
    try {
      String regex = PropertyReader.getPropertyValue(SMSCSIMProperties.SUBMITSM_SENDER_NAME_REGEX);
      String address = addressNode.getAddress();
      if (address != null && address.matches(regex)) {
        return address;
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }

  }
  public static OutboundSMSMessageRequest buildOutboundSMSMessageRequestAsRequest( SubmitMultiSM submitMultiSM ){
	  
	try {
		
		  OutboundSMSMessageRequest sendSMSMultiRequest = new OutboundSMSMessageRequest();
		  
		  //Destination address array
		  short iNumberOfDestinations = submitMultiSM.getNumberOfDests();
		  String[] strDestArray = new String[iNumberOfDestinations];
		  for( int i = 0; i < strDestArray.length; i++ ){
			  strDestArray[i] = "tel:+"+submitMultiSM.getDestAddress(i).getAddress().getAddress();
		  }
		  
		  sendSMSMultiRequest.setAddress( strDestArray );
		  sendSMSMultiRequest.setSenderAddress("tel:" + submitMultiSM.getSourceAddr().getAddress() );
		  sendSMSMultiRequest.setSenderName(getSenderName(submitMultiSM.getSourceAddr()));		
		  OutboundSMSTextMessage outboundSMSTextMessage = new OutboundSMSTextMessage();
		  
		  //Check if payload or short message
		  if( SMSUtil.isShortMessageFieldSet( submitMultiSM ) ){
		    outboundSMSTextMessage.setMessage( submitMultiSM.getShortMessage("UTF-8"));
		  }else{
		    outboundSMSTextMessage.setMessage( new String( submitMultiSM.getMessagePayload().getBuffer(), "UTF-8" ) );
		  }
		  sendSMSMultiRequest.setOutboundSMSTextMessage(outboundSMSTextMessage);
		  sendSMSMultiRequest.setClientCorrelator( String.valueOf(submitMultiSM.getSequenceNumber()) );
		  
		  ReceiptRequest receiptRequest = new ReceiptRequest();
		  receiptRequest.setNotifyURL( PropertyReader.getPropertyValue(SMSCSIMProperties.NOTIFY_URL_DELIVERY) );
		  
		  sendSMSMultiRequest.setReceiptRequest(receiptRequest);
		  
		  return sendSMSMultiRequest;
		  
	} catch (Exception e) {
		
		return null;
		
	}
	  
  }
  
}
