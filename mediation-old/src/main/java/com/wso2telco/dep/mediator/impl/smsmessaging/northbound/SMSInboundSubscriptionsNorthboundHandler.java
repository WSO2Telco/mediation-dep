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
package com.wso2telco.dep.mediator.impl.smsmessaging.northbound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.DestinationAddresses;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.NorthboundSubscribeRequest;
import com.wso2telco.dep.mediator.impl.smsmessaging.SMSExecutor;
import com.wso2telco.dep.mediator.impl.smsmessaging.SMSHandler;
import com.wso2telco.dep.mediator.internal.ApiUtils;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.SMSMessagingService;
import com.wso2telco.dep.mediator.util.ConfigFileReader;
import com.wso2telco.dep.mediator.util.DataPublisherConstants;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.northbound.ValidateNBSubscription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

public class SMSInboundSubscriptionsNorthboundHandler implements SMSHandler {

	/** The log. */
	private static Log log = LogFactory.getLog(SMSInboundSubscriptionsNorthboundHandler.class);

	/** The Constant API_TYPE. */
	private static final String API_TYPE = "smsmessaging";

	/** The occi. */
	private OriginatingCountryCalculatorIDD occi;

	/** The smsMessagingDAO. */
	private SMSMessagingService smsMessagingService;

	/** The executor. */
	private SMSExecutor executor;

	/** The api utils. */
	private ApiUtils apiUtils;

	/** JSON builder */
	private Gson gson = new GsonBuilder().serializeNulls().create();

	public SMSInboundSubscriptionsNorthboundHandler(SMSExecutor executor) {

		this.executor = executor;
		occi = new OriginatingCountryCalculatorIDD();
		smsMessagingService = new SMSMessagingService();
		apiUtils = new ApiUtils();
	}

	@Override
	public boolean validate(String httpMethod, String requestPath, JSONObject jsonBody, MessageContext context)
			throws Exception {

		if (!httpMethod.equalsIgnoreCase("POST")) {

			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			throw new Exception("Method not allowed");
		}

		context.setProperty(DataPublisherConstants.OPERATION_TYPE, 205);
		IServiceValidate validator;

		validator = new ValidateNBSubscription();
		validator.validateUrl(requestPath);
		validator.validate(jsonBody.toString());

		return true;
	}

	@Override
	public boolean handle(MessageContext context) throws Exception {

		String requestid = UID.getUniqueID(Type.RETRIVSUB.getCode(), context, executor.getApplicationid());

		HashMap<String, String> jwtDetails = apiUtils.getJwtTokenDetails(context);
		JSONObject jsonBody = executor.getJsonBody();
		JSONObject jsondstaddr = jsonBody.getJSONObject("subscription");

		String orgclientcl = "";
		if (!jsondstaddr.isNull("clientCorrelator")) {
			orgclientcl = jsondstaddr.getString("clientCorrelator");
		}

		String serviceProvider = jwtDetails.get("subscriber");
		log.debug("subscriber Name : " + serviceProvider);

		NorthboundSubscribeRequest nbSubsrequst = gson.fromJson(jsonBody.toString(), NorthboundSubscribeRequest.class);
		String origNotiUrl = nbSubsrequst.getSubscription().getCallbackReference().getNotifyURL();

		String origCallbackData = nbSubsrequst.getSubscription().getCallbackReference().getCallbackData();

		String notificationFormat = nbSubsrequst.getSubscription().getNotificationFormat();

		List<OperatorEndpoint> endpoints = occi.getAPIEndpointsByApp(API_TYPE, executor.getSubResourcePath(),
				executor.getValidoperators(context),context);

		Map<String, OperatorEndpoint> operatorMap = new HashMap<String, OperatorEndpoint>();

		for (OperatorEndpoint endpoint : endpoints) {

			operatorMap.put(endpoint.getOperator(), endpoint);

		}

		Integer moSubscriptionId = smsMessagingService.subscriptionEntry(
				nbSubsrequst.getSubscription().getCallbackReference().getNotifyURL(), serviceProvider);
		Map<String, String> mediatorConfMap = ConfigFileReader.getInstance().getMediatorConfigMap();
		String subsEndpoint = mediatorConfMap.get("hubMOSubsGatewayEndpoint") + "/" + moSubscriptionId;

		nbSubsrequst.getSubscription().getCallbackReference().setNotifyURL(subsEndpoint);

		nbSubsrequst.getSubscription().setClientCorrelator(orgclientcl + ":" + requestid);


		log.debug("subscription northbound request body : " + gson.toJson(nbSubsrequst));

		DestinationAddresses[] destinationAddresses = nbSubsrequst.getSubscription().getDestinationAddresses();

		for (DestinationAddresses destinationAddressesObj : destinationAddresses) {

			if (operatorMap.containsKey(destinationAddressesObj.getOperatorCode().trim())) {

				OperatorEndpoint endpoint = operatorMap.get(destinationAddressesObj.getOperatorCode().trim());
				String url = endpoint.getEndpointref().getAddress();

				destinationAddressesObj.setToAddress(url);

				destinationAddressesObj.setAuthorizationHeader("Bearer " + executor.getAccessToken(endpoint.getOperator(), context));

				destinationAddressesObj.setOperatorId(endpoint.getOperatorId());

				log.debug("operator name: " + endpoint.getOperator());

			} else {

				log.error("OperatorEndpoint not found. Operator Not Provisioned: " + destinationAddressesObj.getOperatorCode());

				destinationAddressesObj.setToAddress("Not Provisioned");
			}
		}

		nbSubsrequst.getSubscription().setDestinationAddresses(destinationAddresses);

		String requestStr = gson.toJson(nbSubsrequst);

		JsonUtil.newJsonPayload(((Axis2MessageContext) context).getAxis2MessageContext(), requestStr, true, true);

		HandlerUtils.setHandlerProperty(context,this.getClass().getSimpleName());

		String ResourceUrlPrefix = mediatorConfMap.get("hubGateway");
		context.setProperty("responseResourceURL", ResourceUrlPrefix + executor.getApiContext()+ "/" + executor.getApiVersion() + executor.getSubResourcePath()+ "/" +  moSubscriptionId);

		context.setProperty("subscriptionID", moSubscriptionId);
		context.setProperty("original_notifyUrl", origNotiUrl);
		context.setProperty("original_callbackData", origCallbackData);

		context.setProperty("original_clientCorrelator", orgclientcl);

		context.setProperty("original_notificationFormat", notificationFormat);

		return true;
	}

}
