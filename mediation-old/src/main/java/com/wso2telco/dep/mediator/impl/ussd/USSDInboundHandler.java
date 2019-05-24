/*
 *
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package com.wso2telco.dep.mediator.impl.ussd;

import java.util.List;

import com.wso2telco.dep.mediator.MediatorConstants;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.USSDService;
import com.wso2telco.dep.mediator.util.ConfigFileReader;
import com.wso2telco.dep.mediator.util.DataPublisherConstants;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.ussd.ValidateReceiveUssd;
import com.wso2telco.dep.subscriptionvalidator.util.ValidatorUtils;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

// TODO: Auto-generated Javadoc

/**
 * The Class USSDInboundHandler.
 */
public class USSDInboundHandler implements USSDHandler {
	
	public static final String MTCONT = "mtcont";
	public static final String MTFIN = "mtfin";
	public static final String MOINIT = "moinit";
	public static final String MOCONT = "mocont";
	public static final String MOFIN = "mofin";

	/** The log. */
	private Log log = LogFactory.getLog(USSDInboundHandler.class);

	/** The Constant API_TYPE. */
	private static final String API_TYPE = "ussd";

	/** The ussdDAO. */
	private USSDService ussdService;

	/** The executor. */
	private USSDExecutor executor;

	/** The occi. */
	private OriginatingCountryCalculatorIDD occi;

	/**
	 * Instantiates a new USSD inbound handler.
	 *
	 * @param executor
	 *            the executor
	 */
	public USSDInboundHandler(USSDExecutor executor) {

		this.executor = executor;
		ussdService = new USSDService();
		occi = new OriginatingCountryCalculatorIDD();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.wso2telco.mediator.impl.ussd.USSDHandler#handle(org.apache.synapse.
	 * MessageContext)
	 */
	@Override
	public boolean handle(MessageContext context) throws CustomException, AxisFault, Exception {

		String requestid = UID.getUniqueID(Type.RETRIEVE_USSD.getCode(), context, executor.getApplicationid());
		String requestPath = executor.getSubResourcePath();
		String subscriptionId = requestPath.substring(requestPath.lastIndexOf("/") + 1);

		// remove non numeric chars
		subscriptionId = subscriptionId.replaceAll("[^\\d.]", "");
		log.debug("subscriptionId - " + subscriptionId);
		
		List<String> ussdSPDetails = ussdService.getUSSDNotify(Integer.valueOf(subscriptionId));

		if (ussdSPDetails.isEmpty()) {
			throw new CustomException("SVC0002", "", new String[] { "Invalid SubscriptionID" });
		}
		String notifyUrl = ussdSPDetails.get(0);
		String requestRouterUrl = ConfigFileReader.getInstance().getMediatorConfigMap().get("requestRouterUrl");
		if (requestRouterUrl != null) {
			notifyUrl = requestRouterUrl + notifyUrl;
		}
		context.setProperty("spEndpoint", notifyUrl);

		JSONObject jsonBody = executor.getJsonBody();
		
		validateUssdAction(jsonBody, context);
		
		//jsonBody.getJSONObject("inboundUSSDMessageRequest").getJSONObject("responseRequest").put("notifyURL", ussdSPDetails.get(0));
		
		String address = jsonBody.getJSONObject("inboundUSSDMessageRequest").getString("address");
		String msisdn = address.substring(5);
		context.setProperty(MediatorConstants.USER_MSISDN, msisdn);
		context.setProperty(MediatorConstants.MSISDN, address);
		context.setProperty(DataPublisherConstants.MSISDN, msisdn);
        context.setProperty(DataPublisherConstants.SP_CONSUMER_KEY, ussdSPDetails.get(1));
        context.setProperty(DataPublisherConstants.SP_OPERATOR_ID, ussdSPDetails.get(2));
        context.setProperty(DataPublisherConstants.SP_USER_ID, ussdSPDetails.get(3));


        log.debug("01 SP_CONSUMER_KEY found - " + ussdSPDetails.get(1) + " Request ID: " + UID.getRequestID(context));
        log.info("01 SP_OPERATOR_ID found - " + ussdSPDetails.get(2) + " Request ID: " + UID.getRequestID(context));
        log.info("01 SP_USER_ID found - " + ussdSPDetails.get(3) + " Request ID: " + UID.getRequestID(context));

        OperatorEndpoint operatorendpoint = new OperatorEndpoint(new EndpointReference(notifyUrl), null);
        String sending_add = operatorendpoint.getEndpointref().getAddress();
        
        //==============SET OPERATOR ID & OPERATOR NAME
        OperatorEndpoint endpoint = null;
        
		String filteredAddress = address.replace("etel:", "").replace("tel:", "");
		if (!filteredAddress.startsWith("+")) {
			filteredAddress = "+" + filteredAddress;
		}
		
		if (ValidatorUtils.getValidatorForSubscriptionFromMessageContext(context).validate(context)) {
			endpoint = occi.getAPIEndpointsByMSISDN(filteredAddress, API_TYPE,
					executor.getSubResourcePath(), false, executor.getValidoperators(context));
		}
		context.setProperty("operator", operatorendpoint.getOperator());
		context.setProperty("OPERATOR_NAME", operatorendpoint.getOperator());
		context.setProperty("OPERATOR_ID", operatorendpoint.getOperatorId());        
		//==============SET OPERATOR ID & OPERATOR NAME
		
        HandlerUtils.setHandlerProperty(context, this.getClass().getSimpleName());
        HandlerUtils.setEndpointProperty(context, sending_add);
        HandlerUtils.setAuthorizationHeader(context, executor, operatorendpoint);

		return true;
	}

	
	private void validateUssdAction(JSONObject jsonBody, MessageContext context) throws Exception {
		String ussdAction = jsonBody.getJSONObject("inboundUSSDMessageRequest").getString("ussdAction");
		if ( !( ussdAction.equals(MTCONT) || ussdAction.equals(MTFIN) || ussdAction.equals(MOINIT) || ussdAction.equals(MOCONT) || ussdAction.equals(MOFIN))){
			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			throw new CustomException("SVC0002", "Invalid input value for message part %1",
			        new String[]{"Invalid ussdAction"});
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.wso2telco.mediator.impl.ussd.USSDHandler#validate(java.lang.String,
	 * java.lang.String, org.json.JSONObject, org.apache.synapse.MessageContext)
	 */
	@Override
	public boolean validate(String httpMethod, String requestPath, JSONObject jsonBody, MessageContext context)
			throws Exception {

		context.setProperty(DataPublisherConstants.OPERATION_TYPE, 407);

		IServiceValidate validator;

		validator = new ValidateReceiveUssd();
		validator.validateUrl(requestPath);
		validator.validate(jsonBody.toString());

		return true;
	}
}
