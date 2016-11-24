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

package com.wso2telco.dep.mediator.impl.payment;

import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.ResponseHandler;
import com.wso2telco.dep.mediator.entity.OparatorEndPointSearchDTO;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.PaymentService;
import com.wso2telco.dep.mediator.util.APIType;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.payment.ValidateQueryPaymentStatus;
import com.wso2telco.dep.subscriptionvalidator.util.ValidatorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

import java.util.Map;

public class QueryPaymentStatusHandler implements PaymentHandler {

	private Log log = LogFactory.getLog(AmountChargeHandler.class);

	private static final String API_TYPE = "payment";
	private OriginatingCountryCalculatorIDD occi;
	private ResponseHandler responseHandler;
	private PaymentExecutor executor;
	private PaymentService dbservice;

	public QueryPaymentStatusHandler(PaymentExecutor executor) {
		this.executor = executor;
		occi = new OriginatingCountryCalculatorIDD();
		responseHandler = new ResponseHandler();
		dbservice = new PaymentService();
	}

	@Override
	public boolean handle(MessageContext context) throws Exception {

		String[] params = executor.getSubResourcePath().split("/");
		context.setProperty(MSISDNConstants.USER_MSISDN, params[1].substring(5));
		OperatorEndpoint endpoint = null;
		if (ValidatorUtils.getValidatorForSubscription(context).validate(
				context)) {
			OparatorEndPointSearchDTO searchDTO = new OparatorEndPointSearchDTO();
			searchDTO.setApi(APIType.PAYMENT);
			searchDTO.setContext(context);
			searchDTO.setIsredirect(true);
			searchDTO.setMSISDN(params[1]);
			searchDTO.setOperators(executor.getValidoperators());
			searchDTO.setRequestPathURL(executor.getSubResourcePath());

			endpoint = occi.getOperatorEndpoint(searchDTO);
			/*
			 * occi.getAPIEndpointsByMSISDN(params[1].replace("tel:", ""),
			 * API_TYPE, executor.getSubResourcePath(), true,
			 * executor.getValidoperators());
			 */}

		String sending_add = endpoint.getEndpointref().getAddress();

		// set information to the message context, to be used in the sequence
		context.setProperty("HANDLER", this.getClass().getSimpleName());
		context.setProperty("ENDPOINT", sending_add);
		// set Authorization header
		org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) context)
				.getAxis2MessageContext();
		Object headers = axis2MessageContext
				.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
		if (headers != null && headers instanceof Map) {
			Map headersMap = (Map) headers;
			headersMap.put("Authorization", "Bearer " + executor.getAccessToken(endpoint
					.getOperator(), context));
		}

//		String responseStr = executor.makeGetRequest(endpoint, sending_add,	executor.getSubResourcePath(), true, context, false);
//
//		executor.removeHeaders(context);
//
//		if (responseStr == null || responseStr.equals("") || responseStr.isEmpty()) {
//			throw new CustomException("SVC1000", "", new String[] { null });
//		} else {
//			executor.handlePluginException(responseStr);
//		}
//
//		// set response re-applied
//		executor.setResponse(context, responseStr);
//		((Axis2MessageContext) context).getAxis2MessageContext().setProperty("messageType", "application/json");
//		((Axis2MessageContext) context).getAxis2MessageContext().setProperty("ContentType", "application/json");

		return true;
	}

	@Override
	public boolean validate(String httpMethod, String requestPath, JSONObject jsonBody, MessageContext context) throws Exception {
		if (!httpMethod.equalsIgnoreCase("GET")) {
			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);

			throw new Exception("Method not allowed");
		}

		String[] params = executor.getSubResourcePath().split("/");
		IServiceValidate validator = new ValidateQueryPaymentStatus();
		validator.validateUrl(requestPath);
		validator.validate(params);

		return true;
	}

}
