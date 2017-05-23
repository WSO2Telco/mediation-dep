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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.entity.smsmessaging.CallbackReference;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.DeliveryReceiptGroupSubscription;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.NorthboundDeliveryReceiptGroupSubscriptionRequest;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.NorthboundDeliveryReceiptSubscriptionRequest;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.SenderAddresses;
import com.wso2telco.dep.mediator.impl.smsmessaging.SMSExecutor;
import com.wso2telco.dep.mediator.impl.smsmessaging.SMSHandler;
import com.wso2telco.dep.mediator.internal.ApiUtils;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.SMSMessagingService;
import com.wso2telco.dep.mediator.util.FileNames;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.ValidateCancelSubscription;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.northbound.ValidateNBOutboundSubscription;
import com.wso2telco.dep.operatorservice.model.OperatorSubscriptionDTO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * The Class NBOutboundSMSSubscriptionsHandler.
 */
public class OutboundSMSSubscriptionsNorthboundHandler implements SMSHandler {

	/** The log. */
	private static Log log = LogFactory.getLog(OutboundSMSSubscriptionsNorthboundHandler.class);

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

	/** The configuration file */
	private String file = CarbonUtils.getCarbonConfigDirPath() + File.separator + FileNames.MEDIATOR_CONF_FILE.getFileName();

	/** The configurations */
	private Map<String, String> mediatorConfMap;

	/** The Gson Builder */
	private Gson gson = new GsonBuilder().serializeNulls().create();

	/**
	 * Instantiates a new NB outbound sms subscriptions handler.
	 *
	 * @param executor
	 *            the executor
	 */
	public OutboundSMSSubscriptionsNorthboundHandler(SMSExecutor executor) {
		this.executor = executor;
		occi = new OriginatingCountryCalculatorIDD();
		smsMessagingService = new SMSMessagingService();
		apiUtils = new ApiUtils();
		mediatorConfMap = new FileReader().readPropertyFile(file);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.wso2telco.mediator.impl.sms.SMSHandler#validate(java.lang.String,
	 * java.lang.String, org.json.JSONObject, org.apache.synapse.MessageContext)
	 */
	@Override
	public boolean validate(String httpMethod, String requestPath, JSONObject jsonBody, MessageContext context)
			throws Exception {
		
		IServiceValidate validator;
		if (httpMethod.equalsIgnoreCase("POST")) {
			validator = new ValidateNBOutboundSubscription();
			validator.validateUrl(requestPath);
			validator.validate(jsonBody.toString());
			return true;
		} else if (httpMethod.equalsIgnoreCase("DELETE")) {
			String dnSubscriptionId = requestPath.substring(requestPath.lastIndexOf("/") + 1);
			String[] params = { dnSubscriptionId };
			validator = new ValidateCancelSubscription();
			validator.validateUrl(requestPath);
			validator.validate(params);
			return true;
		} else {
			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			throw new Exception("Method not allowed");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.wso2telco.mediator.impl.sms.SMSHandler#handle(org.apache.synapse.
	 * MessageContext)
	 */
	@Override
	public boolean handle(MessageContext context) throws Exception {

		if (executor.getHttpMethod().equalsIgnoreCase("POST")) {
			return createSubscriptions(context);
		} /*else if (executor.getHttpMethod().equalsIgnoreCase("DELETE")) {
			return deleteSubscriptions(context);
		}*/

		return false;
	}

	/**
	 * Creates the subscriptions.
	 *
	 * @param context
	 *            the context
	 * @return true, if successful
	 * @throws Exception
	 *             the exception
	 */
	private boolean createSubscriptions(MessageContext context) throws Exception {

		String requestid = UID.getUniqueID(Type.RETRIVSUB.getCode(), context, executor.getApplicationid());

		HashMap<String, String> jwtDetails = apiUtils.getJwtTokenDetails(context);
		JSONObject jsonBody = executor.getJsonBody();
//		JSONObject jsondstaddr = jsonBody.getJSONObject("deliveryReceiptSubscription");

		String serviceProvider = jwtDetails.get("subscriber");
		log.debug("Subscriber Name : " + serviceProvider);
		
		String hubDNSubsGatewayEndpoint = mediatorConfMap.get("hubDNSubsGatewayEndpoint");
		log.debug("Hub DN Notify URL : " + hubDNSubsGatewayEndpoint);
		
		NorthboundDeliveryReceiptSubscriptionRequest nbDeliveryReceiptSubscriptionRequest = gson.fromJson(jsonBody.toString(), NorthboundDeliveryReceiptSubscriptionRequest.class);
		String orgclientcl = nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription().getClientCorrelator();
		String origNotiUrl = nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription().getCallbackReference().getNotifyURL();
		String origCallbackData = nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription().getCallbackReference().getCallbackData();

		List<OperatorEndpoint> endpoints = occi.getAPIEndpointsByApp(API_TYPE, executor.getSubResourcePath(),executor.getValidoperators());

		Map<String, OperatorEndpoint> operatorMap = new HashMap<String, OperatorEndpoint>();

		for (OperatorEndpoint endpoint : endpoints) {

			operatorMap.put(endpoint.getOperator(), endpoint);

		}

		Integer dnSubscriptionId = smsMessagingService.outboundSubscriptionEntry(nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription().getCallbackReference().getNotifyURL(), serviceProvider);
		String subsEndpoint = hubDNSubsGatewayEndpoint + "/" + dnSubscriptionId;

		nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription().getCallbackReference()
				.setNotifyURL(subsEndpoint);
		log.debug("Delivery notification subscription northbound request body : "
				+ gson.toJson(nbDeliveryReceiptSubscriptionRequest));

		SenderAddresses[] senderAddresses = nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription()
		                                                                        .getSenderAddresses();

//		List<OperatorSubscriptionDTO> domainsubs = new ArrayList<OperatorSubscriptionDTO>();
//		SouthboundDeliveryReceiptSubscriptionRequest sbDeliveryReceiptSubscriptionResponse = null;

		for (SenderAddresses senderAddressesObj : senderAddresses) {

			if (operatorMap.containsKey(senderAddressesObj.getOperatorCode())) {

				OperatorEndpoint endpoint = operatorMap.get(senderAddressesObj.getOperatorCode());
				String url = endpoint.getEndpointref().getAddress();
				String southboundURLPart = "/" + senderAddressesObj.getSenderAddress() + "/subscriptions";
				url = url.replace("/subscriptions", southboundURLPart);

				senderAddressesObj.setToAddress(url);

				senderAddressesObj.setAuthorizationHeader("Bearer " + executor.getAccessToken(endpoint.getOperator(), context));

				senderAddressesObj.setOperatorId(endpoint.getOperatorId());

			} else {

				log.error("OperatorEndpoint not found. Operator Not Provisioned: " + senderAddressesObj.getOperatorCode());

				senderAddressesObj.setToAddress("Not Provisioned");
			}
		}

		NorthboundDeliveryReceiptGroupSubscriptionRequest sbDeliveryReceiptSubscriptionRequest = new NorthboundDeliveryReceiptGroupSubscriptionRequest();
		DeliveryReceiptGroupSubscription deliveryReceiptSubscriptionRequest = new DeliveryReceiptGroupSubscription();
		CallbackReference callbackReference = new CallbackReference();

		callbackReference.setCallbackData(nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription().getCallbackReference().getCallbackData());
		callbackReference.setNotifyURL(subsEndpoint);

		deliveryReceiptSubscriptionRequest.setCallbackReference(callbackReference);
		deliveryReceiptSubscriptionRequest.setClientCorrelator(orgclientcl + ":" + requestid);
		deliveryReceiptSubscriptionRequest.setSenderAddresses(senderAddresses);

		sbDeliveryReceiptSubscriptionRequest.setDeliveryReceiptSubscription(deliveryReceiptSubscriptionRequest);

		String requestStr = gson.toJson(sbDeliveryReceiptSubscriptionRequest);

		HandlerUtils.setHandlerProperty(context,this.getClass().getSimpleName());

		String ResourceUrlPrefix = mediatorConfMap.get("hubGateway");
		context.setProperty("responseResourceURL", ResourceUrlPrefix + executor.getResourceUrl() + "/" + dnSubscriptionId);

		context.setProperty("subscriptionID", dnSubscriptionId);
		context.setProperty("original_notifyUrl", origNotiUrl);
		context.setProperty("original_callbackData", origCallbackData);

		JsonUtil.newJsonPayload(((Axis2MessageContext) context).getAxis2MessageContext(), requestStr, true, true);

		return true;
	}

		/*

		for (OperatorEndpoint endpoint : endpoints) {			

			for (int i = 0; i < senderAddresses.length; i++) {
				if (senderAddresses[i].getOperatorCode().equalsIgnoreCase(endpoint.getOperator())) {
					log.debug("Operator name: " + endpoint.getOperator());
					SouthboundDeliveryReceiptSubscriptionRequest sbDeliveryReceiptSubscriptionRequest = new SouthboundDeliveryReceiptSubscriptionRequest();
					DeliveryReceiptSubscription deliveryReceiptSubscriptionRequest = new DeliveryReceiptSubscription();
					CallbackReference callbackReference = new CallbackReference();

					callbackReference.setCallbackData(nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription().getCallbackReference().getCallbackData());
					callbackReference.setNotifyURL(subsEndpoint);
					
					deliveryReceiptSubscriptionRequest.setCallbackReference(callbackReference);
					deliveryReceiptSubscriptionRequest.setClientCorrelator(orgclientcl + ":" + requestid);
					deliveryReceiptSubscriptionRequest.setOperatorCode(senderAddresses[i].getOperatorCode());
					deliveryReceiptSubscriptionRequest.setFilterCriteria(senderAddresses[i].getFilterCriteria());
					
					sbDeliveryReceiptSubscriptionRequest.setDeliveryReceiptSubscription(deliveryReceiptSubscriptionRequest);

					String sbRequestBody = removeResourceURL(gson.toJson(sbDeliveryReceiptSubscriptionRequest));
					log.debug("Delivery notification southbound request body of " + endpoint.getOperator()+ " operator: " + sbRequestBody);

					String url = endpoint.getEndpointref().getAddress();
					String southboundURLPart = "/" + senderAddresses[i].getSenderAddress() + "/subscriptions";
					url = url.replace("/subscriptions", southboundURLPart);
					log.debug("Delivery notification southbound request url of " + endpoint.getOperator()+ " operator: " + url);

					String notifyres = executor.makeRequest(endpoint, url, sbRequestBody, true, context,false);

					log.info("Delivery notification southbound response body of " + endpoint.getOperator() + " operator: " + notifyres
					         + " Request ID: " + UID.getRequestID(context));
					
					if (notifyres == null) {
						senderAddresses[i].setStatus("Failed");
						
					} else {
						// plugin exception handling
						sbDeliveryReceiptSubscriptionResponse = gson.fromJson(notifyres,SouthboundDeliveryReceiptSubscriptionRequest.class);
						if (sbDeliveryReceiptSubscriptionResponse.getDeliveryReceiptSubscription() == null) {
							senderAddresses[i].setStatus("NotCreated");
						} else {
							domainsubs.add(new OperatorSubscriptionDTO(endpoint.getOperator(),sbDeliveryReceiptSubscriptionResponse.getDeliveryReceiptSubscription()
											.getResourceURL()));
							senderAddresses[i].setStatus("Created");
						}
					}
					break;
				}
			}
		}
		// todo: domainsubs contains successful attempts
		smsMessagingService.outboundOperatorsubsEntry(domainsubs, dnSubscriptionId);
		String ResourceUrlPrefix = mediatorConfMap.get("hubGateway");

		SenderAddresses[] responseSenderAddresses = new SenderAddresses[senderAddresses.length];
		int senderAddressesCount = 0;
		int successResultCount = 0;
		for (SenderAddresses sendernAddressesResult : senderAddresses) {
			String deliverySubscriptionStatus = sendernAddressesResult.getStatus();
			if (deliverySubscriptionStatus == null) {
				sendernAddressesResult.setStatus("Failed");
			} else if (deliverySubscriptionStatus.equals("Created")) {
				successResultCount++;
			}
			responseSenderAddresses[senderAddressesCount] = sendernAddressesResult;
			senderAddressesCount++;
		}

		// todo: error if successful attempt count is zero
		if (successResultCount == 0) {
			throw new CustomException("POL0299", "", new String[] { "Error registering subscription" });
		}

		nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription().setSenderAddresses(responseSenderAddresses);
		nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription().setResourceURL(ResourceUrlPrefix + executor.getResourceUrl() + "/" + dnSubscriptionId);
		nbDeliveryReceiptSubscriptionRequest.getDeliveryReceiptSubscription().getCallbackReference().setNotifyURL(origNotiUrl);

		String nbDeliveryReceiptSubscriptionResponseBody = gson.toJson(nbDeliveryReceiptSubscriptionRequest);
		log.info("Delivery notification subscription northbound response body : " + nbDeliveryReceiptSubscriptionResponseBody
		         + " Request ID: " + UID.getRequestID(context));
		executor.removeHeaders(context);
		((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 201);
		executor.setResponse(context, nbDeliveryReceiptSubscriptionResponseBody);


		*/



	/**
	 * Removes the resource url.
	 *
	 * @param sbSubsrequst
	 *            the sb subsrequst
	 * @return the string
	 */
	/*private String removeResourceURL(String sbSubsrequst) {

		String sbDeliveryNotificationrequestString = "";

		try {

			JSONObject objJSONObject = new JSONObject(sbSubsrequst);
			JSONObject objDeliveryNotificationRequest = (JSONObject) objJSONObject.get("deliveryReceiptSubscription");
			objDeliveryNotificationRequest.remove("resourceURL");

			sbDeliveryNotificationrequestString = objDeliveryNotificationRequest.toString();
		} catch (JSONException ex) {

			log.error("Error in removeResourceURL" + ex.getMessage());
			throw new CustomException("POL0299", "", new String[] { "Error registering subscription" });
		}

		return "{\"deliveryReceiptSubscription\":" + sbDeliveryNotificationrequestString + "}";
	}*/
	
	/*private boolean deleteSubscriptions(MessageContext context) throws Exception {
		        String requestPath = executor.getSubResourcePath();
		        String dnSubscriptionId = requestPath.substring(requestPath.lastIndexOf("/") + 1);
		
		        String requestid = UID.getUniqueID(Type.DELRETSUB.getCode(), context, executor.getApplicationid());
		
		        List<OperatorSubscriptionDTO> domainsubs = (smsMessagingService.outboudSubscriptionQuery(Integer.valueOf(dnSubscriptionId)));
		        if (domainsubs.isEmpty()) {
		            throw new CustomException("POL0001", "", new String[]{"SMS Receipt Subscription Not Found: " + dnSubscriptionId});
		        }
		
		        String resStr = "";
		        for (OperatorSubscriptionDTO subs : domainsubs) {
					resStr = executor.makeDeleteRequest(
							new OperatorEndpoint(new EndpointReference(subs.getDomain()), subs.getOperator()), subs.getDomain(),
							null, true, context,false);
				}
		        new SMSMessagingService().outboundSubscriptionDelete(Integer.valueOf(dnSubscriptionId));
		
		       executor.removeHeaders(context);
		        ((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 204);
		
		        return true;
		    }*/
	}
