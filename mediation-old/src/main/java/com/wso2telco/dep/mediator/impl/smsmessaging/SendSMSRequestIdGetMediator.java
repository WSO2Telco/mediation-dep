package com.wso2telco.dep.mediator.impl.smsmessaging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.dep.mediator.service.SMSMessagingService;

public class SendSMSRequestIdGetMediator extends AbstractMediator {
	
    private Log log = LogFactory.getLog(this.getClass());


	@Override
	public boolean mediate(MessageContext messageContext) {
		
		String requestId = null;
        String senderAddress = (String) messageContext.getProperty("SENDER_ADDRESS");
        String requestIdFromOperatorResponse = (String) messageContext.getProperty("SEND_SMS_OPERATOR_REQUEST_ID");

        // validate
        if (!assertNotNull(senderAddress)) {
            log.error("Validation failure for SENDER_ADDRESS, value: " + senderAddress);
            return false;
        }
        if (!assertNotNull(requestIdFromOperatorResponse)) {
            log.error("Validation failure for SEND_SMS_OPERATOR_REQUEST_ID, value: " + requestIdFromOperatorResponse);
            return false;
        }

        try {
        	requestId = new SMSMessagingService().getSMSRequestId(senderAddress, requestIdFromOperatorResponse);
        	messageContext.setProperty("PERSISTED_REQUEST_ID", requestId);
        } catch (BusinessException e) {
            log.error("Error inserting request ids for send SMS operation", e);
            return false;
        }

        return true;
    }

    private static boolean assertNotNull (String aString) {
       return aString != null && !aString.isEmpty();
    }
}
