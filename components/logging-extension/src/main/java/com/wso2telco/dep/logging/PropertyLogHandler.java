package com.wso2telco.dep.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

public class PropertyLogHandler extends AbstractMediator {

	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public boolean mediate(MessageContext messageContext) {

		org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
				.getAxis2MessageContext();

		// RegistryEntry payloadLoggingEnabled =
		// messageContext.getConfiguration().getRegistry().getRegistryEntry(PAYLOAD_LOGGING_ENABLED);

		String direction = (String) axis2MessageContext.getProperty(MessageConstants.MESSAGE_TYPE);

		if (direction != null && direction.equalsIgnoreCase(MessageType.REQUEST.getMessageType())) {
			logRequestProperties(messageContext, axis2MessageContext, true);
		} else if (direction != null && direction.equalsIgnoreCase(MessageType.RESPONSE.getMessageType())) {
			logResponseProperties(messageContext, axis2MessageContext, true);
		}

		return true;
	}

	private void logRequestProperties(MessageContext messageContext,
			org.apache.axis2.context.MessageContext axis2MessageContext, boolean isPayloadLoggingEnabled) {

		String requestId = (String) messageContext.getProperty(MessageConstants.REQUEST_ID);

		if (CommonUtil.nullOrTrimmed(requestId) == null) {
			UniqueIDGenerator.generateAndSetUniqueID("MI", messageContext,
					(String) messageContext.getProperty(MessageConstants.APPLICATION_ID));
		}

		String jsonBody = JsonUtil.jsonPayloadToString(axis2MessageContext);
		log.info("[" + dateFormat.format(new Date()) + "] >>>>> API Request id "
				+ messageContext.getProperty(MessageConstants.REQUEST_ID));

		log.debug("                                       >>>>> reqBody :" + jsonBody);

	}

	private void logResponseProperties(MessageContext messageContext,
			org.apache.axis2.context.MessageContext axis2MessageContext, boolean isPayloadLoggingEnabled) {

		String jsonBody = JsonUtil.jsonPayloadToString(axis2MessageContext);

		log.info("[" + dateFormat.format(new Date()) + "] >>>>> API Request id "
				+ messageContext.getProperty(MessageConstants.REQUEST_ID));

		log.debug("                                       >>>>> respBody :" + jsonBody);

	}
}
