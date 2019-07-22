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

import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.entity.OparatorEndPointSearchDTO;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.USSDService;
import com.wso2telco.dep.mediator.util.APIType;
import com.wso2telco.dep.mediator.util.ConfigFileReader;
import com.wso2telco.dep.mediator.util.DataPublisherConstants;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.mediator.util.ValidationUtils;
import com.wso2telco.dep.oneapi.constant.ussd.USSDKeyConstants;
import com.wso2telco.dep.oneapi.constant.ussd.USSDValueConstants;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.ussd.ValidateUssdSend;
import com.wso2telco.dep.subscriptionvalidator.util.ValidatorUtils;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

// TODO: Auto-generated Javadoc

/**
 * The Class SendUSSDHandler.
 */
public class SendUSSDHandler implements USSDHandler {

	/** The log. */
	private Log log = LogFactory.getLog(SendUSSDHandler.class);

	/** The occi. */
	private OriginatingCountryCalculatorIDD occi;

	/** The executor. */
	private USSDExecutor executor;

	/** The ussdDAO. */
	private USSDService ussdService;

	/**
	 * Instantiates a new send ussd handler.
	 *
	 * @param executor
	 *            the executor
	 */
	public SendUSSDHandler(USSDExecutor executor) {
		occi = new OriginatingCountryCalculatorIDD();
		this.executor = executor;
		ussdService = new USSDService();
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

        String requestid = UID.getUniqueID(Type.SEND_USSD.getCode(), context, executor.getApplicationid());
        JSONObject jsonBody = executor.getJsonBody();

		String address = jsonBody.getJSONObject(USSDKeyConstants.OUT_BOUND_USSD_MESSAGE_REQUEST)
				.getString(USSDKeyConstants.ADDRESS);
        String msisdn = address.substring(5);

		String consumerKey = "";
		String userId = "";
		consumerKey = (String) context.getProperty("CONSUMER_KEY");
		userId = (String) context.getProperty("USER_ID");

		OperatorEndpoint endpoint = null;

		validateUssdAction(jsonBody, context);

		String filteredAddress = address.replace("etel:", "").replace("tel:", "");
		if (!filteredAddress.startsWith("+")) {
			filteredAddress = "+" + filteredAddress;
		}

		if (ValidatorUtils.getValidatorForSubscriptionFromMessageContext(context).validate(context)) {
			OparatorEndPointSearchDTO searchDTO = new OparatorEndPointSearchDTO();
			searchDTO.setApi(APIType.USSD);
			searchDTO.setApiName((String) context.getProperty("API_NAME"));
			searchDTO.setContext(context);
			searchDTO.setIsredirect(false);
			searchDTO.setMSISDN(filteredAddress);
			searchDTO.setOperators(executor.getValidoperators(context));
			searchDTO.setRequestPathURL(executor.getSubResourcePath());

			endpoint = occi.getOperatorEndpoint(searchDTO);
		}
		context.setProperty("operator", endpoint.getOperator());
		context.setProperty("OPERATOR_NAME", endpoint.getOperator());
		context.setProperty("OPERATOR_ID", endpoint.getOperatorId());

		if (!jsonBody.getJSONObject(USSDKeyConstants.OUT_BOUND_USSD_MESSAGE_REQUEST)
				.isNull(USSDKeyConstants.RESPONSE_REQUEST)) {

			JSONObject responseRequest = jsonBody.getJSONObject(USSDKeyConstants.OUT_BOUND_USSD_MESSAGE_REQUEST)
					.getJSONObject(USSDKeyConstants.RESPONSE_REQUEST);
			if (!responseRequest.isNull(USSDKeyConstants.NOTIFY_URL)) {

				String notifyUrl = responseRequest.getString(USSDKeyConstants.NOTIFY_URL).trim();
				if (!notifyUrl.isEmpty()) {
					Integer subscriptionId = ussdService.ussdRequestEntry(notifyUrl, consumerKey, endpoint.getOperator(), userId);
					log.info("created subscriptionId  -  " + subscriptionId + " Request ID: " + UID.getRequestID(context));

					String subsEndpoint = ConfigFileReader.getInstance().getMediatorConfigMap().get("ussdGatewayEndpoint") + subscriptionId;
					log.info("Subsendpoint - " + subsEndpoint + " Request ID: " + UID.getRequestID(context));
					context.setProperty("subsEndPoint", subsEndpoint);
				}
			}
		}
		context.setProperty(MSISDNConstants.USER_MSISDN, msisdn);
		context.setProperty(MSISDNConstants.MSISDN, address);

		String sending_add = endpoint.getEndpointref().getAddress();
		log.info("sending endpoint found: " + sending_add + " Request ID: " + UID.getRequestID(context));

		HandlerUtils.setHandlerProperty(context,this.getClass().getSimpleName());
		HandlerUtils.setEndpointProperty(context,sending_add);
		HandlerUtils.setAuthorizationHeader(context,executor,endpoint);

		return true;
	}

	private void validateUssdAction(JSONObject jsonBody, MessageContext context) throws Exception {
		String ussdAction = jsonBody.getJSONObject(USSDKeyConstants.OUT_BOUND_USSD_MESSAGE_REQUEST)
				.getString(USSDKeyConstants.USSD_ACTION);
		if ( !(ussdAction.equals(USSDValueConstants.MTINIT) || ussdAction.equals(USSDValueConstants.MTCONT)) ){
			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			throw new Exception("Ussd Action Not Allowed!");
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
		context.setProperty(DataPublisherConstants.OPERATION_TYPE, 400);
		if (!httpMethod.equalsIgnoreCase("POST")) {
			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			throw new Exception("Method not allowed");
		}

        IServiceValidate validator = new ValidateUssdSend();
		validator.validateUrl(requestPath);
		validator.validate(jsonBody.toString());
		ValidationUtils.compareMsisdn(requestPath, jsonBody, APIType.USSD);

		return true;
	}
}
