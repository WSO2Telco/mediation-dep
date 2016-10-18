/*
 * Note : This class was added for the sake of obtaining SMS messages without the BS.
 * 
 * History
 * 1.0.0		2016-01-19		shirhan@inovaitsys.com		Initial version
 * */
package lk.dialog.smsc.util;

import java.io.UnsupportedEncodingException;

import com.logica.smpp.pdu.PDUException;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.SubmitMultiSM;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.pdu.ValueNotSetException;
import com.logica.smpp.util.ByteBuffer;
import com.logica.smpp.util.NotEnoughDataInByteBufferException;
import com.logica.smpp.util.TerminatingZeroNotFoundException;

public class SMSUtil {
	
	//Submit SM message :: Test method
	public static String getSMSMessage( Request objRequest ) throws UnsupportedEncodingException, NotEnoughDataInByteBufferException, TerminatingZeroNotFoundException, PDUException{
		
		SubmitSM obj  = new SubmitSM();
		obj.setBody( objRequest.getBody() );
		String message = obj.getShortMessage();

		return message;
	}
	
	//Long SMS related methods
	/*
	 * Note :: Check if the short_message field is set.
	 * */
	public static boolean isShortMessageFieldSet( Request objRequest ){
		
		int iMsgLength = 0;
		
		if( objRequest.getClass().getName() == SubmitSM.class.getName() ){
			iMsgLength = ((SubmitSM)objRequest).getSmLength();
		}else if( objRequest.getClass().getName() == SubmitMultiSM.class.getName() ){
			iMsgLength = ((SubmitMultiSM)objRequest).getSmLength();
		}
		
		if( iMsgLength == 0 ){
			return false;
		}else{
			return true;
		}
	}
	
	/*
	 * Note :: Check if payload_field is set
	 * */
	public static boolean isPayloadFieldSet( Request objRequest ){
		
		int iMsgLength = 0;
		
		if( objRequest.getClass().getName() == SubmitSM.class.getName() ){
			iMsgLength = ((SubmitSM)objRequest).getSmLength();
		}else if( objRequest.getClass().getName() == SubmitMultiSM.class.getName() ){
			iMsgLength = ((SubmitMultiSM)objRequest).getSmLength();
		}
		
		if( iMsgLength == 0 ){
			return true;
		}else{
			return false;
		}
	}
	
	/*
	 * Note : Check if the message is a partial. 3 Fields should be set if the message is
	 * splitted into many partials. Checking all three
	 * */
	public static boolean isPartialAvailable( Request objRequest ){
		try {
			if( objRequest.getClass().getName() == SubmitSM.class.getName() ){
				
				((SubmitSM)objRequest).getSarMsgRefNum();
				((SubmitSM)objRequest).getSarTotalSegments();
				((SubmitSM)objRequest).getSarSegmentSeqnum();
			}else if( objRequest.getClass().getName() == SubmitMultiSM.class.getName() ){
				((SubmitMultiSM)objRequest).getSarMsgRefNum();
				((SubmitMultiSM)objRequest).getSarTotalSegments();
				((SubmitMultiSM)objRequest).getSarSegmentSeqnum();
			}
			return true;
	    } catch ( ValueNotSetException e ) {
	    	return false;
	    } catch (Exception e ) {
	      return false;
	    }
	}
	
	/*
	 * Note :: Get message from payload.
	 * */
	public static String getMessageFromPayload( Request objRequest ){
		
		ByteBuffer tmpBuffer = null;
		
		try {
			
			if( objRequest.getClass().getName() == SubmitSM.class.getName() ){
				tmpBuffer = ((SubmitSM)objRequest).getMessagePayload();
			}else if( objRequest.getClass().getName() == SubmitMultiSM.class.getName() ){
				tmpBuffer = ((SubmitMultiSM)objRequest).getMessagePayload();
			}
			
			byte[] shortMsgBytes = tmpBuffer.getBuffer();
			System.out.println("Payload [Before strip]"+ new String(shortMsgBytes));
			return new String(shortMsgBytes);
			
	    } catch ( Exception e ) {
	    	return null;
	    	
	    }

	}

}
