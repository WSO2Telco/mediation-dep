/*
 *
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package com.wso2telco.dep.mediator.impl.smsmessaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.ResponseHandler;
import com.wso2telco.dep.mediator.entity.OparatorEndPointSearchDTO;
import com.wso2telco.dep.mediator.entity.smsmessaging.QuerySMSStatusResponse;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.internal.Util;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.SMSMessagingService;
import com.wso2telco.dep.mediator.util.APIType;
import com.wso2telco.dep.mediator.util.DataPublisherConstants;
import com.wso2telco.dep.mediator.util.FileNames;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.ValidateDeliveryStatus;
import com.wso2telco.dep.subscriptionvalidator.util.ValidatorUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: Auto-generated Javadoc

/**
 * The Class QuerySMSStatusHandler.
 */
public class QuerySMSStatusHandler implements SMSHandler {

	/** The log. */
	private Log log = LogFactory.getLog(QuerySMSStatusHandler.class);

	/** The Constant API_TYPE. */
	private static final String API_TYPE = "sms";

	/** The occi. */
	private OriginatingCountryCalculatorIDD occi;

	/** The executor. */
	private SMSExecutor executor;

	/** The smsMessagingDAO. */
	private SMSMessagingService smsMessagingService;

	/** The response handler. */
	private ResponseHandler responseHandler;

	/** The sender address. */
	private String senderAddress = null;

	/** The request id. */
	private String requestId = null;

	/**
	 * Instantiates a new query sms status handler.
	 *
	 * @param executor
	 *            the executor
	 */
	public QuerySMSStatusHandler(SMSExecutor executor) {

		occi = new OriginatingCountryCalculatorIDD();
		this.executor = executor;
		smsMessagingService = new SMSMessagingService();
		responseHandler = new ResponseHandler();
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
		context.setProperty(DataPublisherConstants.OPERATION_TYPE, 202);
		if (!httpMethod.equalsIgnoreCase("GET")) {
			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			throw new Exception("Method not allowed");
		}
		IServiceValidate validator = new ValidateDeliveryStatus();
		validator.validateUrl(requestPath);

		loadRequestParams();
		validator.validate(new String[] { senderAddress, requestId });

		return true;
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

		String encodedSenderAddress = URLEncoder.encode(senderAddress, "UTF-8");
		Map<String, String> requestIdMap = smsMessagingService.getSMSRequestIds(requestId, senderAddress);
		if (requestIdMap.keySet().isEmpty()) {
            throw new CustomException("SVC0001", "", new String[]{"Could not complete querying SMS statuses"});
		}else {
			sendStatusQueries(context, requestIdMap, encodedSenderAddress);
		}
		String resourceURL = getSendSMSResourceUrlFromFile(context,encodedSenderAddress) + "/requests/" + requestId +
				"/deliveryInfos";
		context.setProperty("QUERY_SMS_DELIVERY_STATUS_RESOURCE_URL",resourceURL);

		return true;
	}


	/**
	 * Set the end point to send the delivery status query request
	 * @param context synapse message context
	 * @param requestIdMap requestidmap
	 * @param encodedSenderAddress url encoded sende adress ex: tel:+7788 -> tel%3A%2B7788
	 * @throws Exception
	 */
	private void sendStatusQueries(MessageContext context,Map<String, String> requestIdMap, String encodedSenderAddress)
			throws Exception {

		String resourcePathPrefix = "/outbound/" + encodedSenderAddress + "/requests/";
		Map<String, QuerySMSStatusResponse> statusResponses = new HashMap<String, QuerySMSStatusResponse>();
		for (Map.Entry<String, String> entry : requestIdMap.entrySet()) {
			String address = entry.getKey();
			String reqId = entry.getValue();
			if (reqId != null) {
				context.setProperty(MSISDNConstants.USER_MSISDN, address.substring(5));
				context.setProperty(MSISDNConstants.MSISDN, address);
				OperatorEndpoint endpoint = null;
				String resourcePath = resourcePathPrefix + reqId + "/deliveryInfos";
				if(ValidatorUtils.getValidatorForSubscriptionFromMessageContext(context).validate(context)){
					OparatorEndPointSearchDTO searchDTO = new OparatorEndPointSearchDTO();
					searchDTO.setApi(APIType.SMS);
					searchDTO.setContext(context);
					searchDTO.setIsredirect(true);
					searchDTO.setMSISDN(address);
					searchDTO.setOperators(executor.getValidoperators(context));
					searchDTO.setRequestPathURL(resourcePath);

					endpoint = occi.getOperatorEndpoint(searchDTO);
					/*
					 * occi.getAPIEndpointsByMSISDN(address.replace("tel:", ""),
					 * API_TYPE, resourcePath, true,
					 * executor.getValidoperators());
					 */
				}
				String sending_add = endpoint.getEndpointref().getAddress();
				sending_add = sending_add + resourcePath;
				log.info("sending endpoint found: " + sending_add + " Request ID: " + UID.getRequestID(context));
				HandlerUtils.setHandlerProperty(context,this.getClass().getSimpleName());
				HandlerUtils.setEndpointProperty(context,sending_add);
				HandlerUtils.setAuthorizationHeader(context,executor,endpoint);

				context.setProperty("OPERATOR_NAME", endpoint.getOperator());
				context.setProperty("OPERATOR_ID", endpoint.getOperatorId());

			}
		}
	}

	/**
	 * read the send sms repsorce url from configuration file
	 * @param mc synapse message context
	 * @param senderAddress url encoded sender adress
	 * @return send sms resource url
	 */
	private String getSendSMSResourceUrlFromFile(MessageContext mc, String senderAddress){
		FileReader fileReader = new FileReader();
		String file = CarbonUtils.getCarbonConfigDirPath() + File.separator
				+ FileNames.MEDIATOR_CONF_FILE.getFileName();

		Map<String, String> mediatorConfMap = fileReader.readPropertyFile(file);

		String resourceURL = mediatorConfMap.get("sendSMSResourceURL");
		if (resourceURL != null && !resourceURL.isEmpty()) {
			resourceURL = resourceURL.substring(1, resourceURL.length() - 1) + senderAddress;
		} else {
			resourceURL = (String) mc.getProperty("REST_URL_PREFIX") + mc.getProperty("REST_FULL_REQUEST_PATH");
			resourceURL = resourceURL.substring(0, resourceURL.indexOf("/requests"));
		}
		return resourceURL;

	}


	/**
	 * Load request params.
	 *
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 */
	private void loadRequestParams() throws UnsupportedEncodingException {
		String reqPath = URLDecoder.decode(executor.getSubResourcePath().replace("+", "%2B"), "UTF-8");
		Pattern pattern = Pattern.compile("outbound/(.+?)/requests/(.+?)/");
		Matcher matcher = pattern.matcher(reqPath);
		while (matcher.find()) {
			senderAddress = matcher.group(1);
			requestId = matcher.group(2);
		}
	}
}
