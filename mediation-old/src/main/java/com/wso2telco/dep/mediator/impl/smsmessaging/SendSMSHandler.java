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
package com.wso2telco.dep.mediator.impl.smsmessaging;

import com.google.common.base.CharMatcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.ResponseHandler;
import com.wso2telco.dep.mediator.entity.OparatorEndPointSearchDTO;
import com.wso2telco.dep.mediator.entity.smsmessaging.SendSMSRequest;
import com.wso2telco.dep.mediator.entity.smsmessaging.SendSMSResponse;
import com.wso2telco.dep.mediator.impl.AbstractHandler;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.SMSMessagingService;
import com.wso2telco.dep.mediator.util.DataPublisherConstants;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.mediator.util.ValidationUtils;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.ValidateSendSms;
import com.wso2telco.dep.operatorservice.model.OperatorApplicationDTO;
import com.wso2telco.dep.subscriptionvalidator.exceptions.ValidatorException;
import com.wso2telco.dep.user.masking.UserMaskHandler;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// TODO: Auto-generated Javadoc

/**
 * The Class SendSMSHandler.
 */
public class SendSMSHandler extends AbstractHandler{

	/** The log. */
	private Log log = LogFactory.getLog(SendSMSHandler.class);

	/** The Constant API_TYPE. */
	private static final String API_TYPE = "smsmessaging";

	/** The occi. */
	private OriginatingCountryCalculatorIDD occi;

	/** The response handler. */
	private ResponseHandler responseHandler;

	/** The executor. */
	private SMSExecutor executor;

	/** The smsMessagingDAO. */
	private SMSMessagingService smsMessagingService;

	/**
	 * Instantiates a new send sms handler.
	 *
	 * @param executor
	 *            the executor
	 * @throws ValidatorException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public SendSMSHandler(SMSExecutor executor) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ValidatorException{
		super();
		this.executor = executor;
		occi = new OriginatingCountryCalculatorIDD();
		responseHandler = new ResponseHandler();
		smsMessagingService = new SMSMessagingService();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.wso2telco.mediator.impl.sms.SMSHandler#handle(org.apache.synapse.
	 * MessageContext)
	 */
	@Override
	public boolean handle(MessageContext context) throws CustomException, AxisFault, Exception {
		
		String requestid = UID.getUniqueID(Type.SMSSEND.getCode(), context, executor.getApplicationid());
		
		// append request id to client correlator
		JSONObject jsonBody = executor.getJsonBody();
		//JSONObject clientclr = jsonBody.getJSONObject("outboundSMSMessageRequest");
		//clientclr.put("clientCorrelator", clientclr.getString("clientCorrelator") + ":" + requestid);

		Gson gson = new GsonBuilder().serializeNulls().create();
		SendSMSRequest subsrequest = gson.fromJson(jsonBody.toString(), SendSMSRequest.class);
		String senderAddress = subsrequest.getOutboundSMSMessageRequest().getSenderAddress();

		// ========================UNICODE PATCH
		byte[] preUtf8 = subsrequest.getOutboundSMSMessageRequest().getOutboundTextMessage().getMessage().getBytes("UTF-8");
		String utf8String = new String(preUtf8, "UTF-8");
		subsrequest.getOutboundSMSMessageRequest().getOutboundTextMessage().setMessage(utf8String);
		// ========================UNICODE PATCH


		if (!subscriptionValidate(context)) {
			throw new CustomException("SVC0001", "", new String[] { "Subscription Validation Unsuccessful" });
		}
		int smsCount = getSMSMessageCount(subsrequest.getOutboundSMSMessageRequest().getOutboundTextMessage().getMessage());
		context.setProperty(DataPublisherConstants.RESPONSE, String.valueOf(smsCount));
		
		if (!jsonBody.getJSONObject("outboundSMSMessageRequest").isNull("address")) {

            JSONArray addressArray = jsonBody.getJSONObject("outboundSMSMessageRequest").getJSONArray("address");
            
            String firstAddress = addressArray.getString(0);
			if (executor.isUserAnonymization() && UserMaskHandler.isMaskedUserId(firstAddress)) {
				firstAddress = UserMaskHandler.transcryptUserId(firstAddress, false, (String)context.getProperty("USER_MASKING_SECRET_KEY"));
			}
            
            if(firstAddress.contains("tel:+")){
        		
            	firstAddress = firstAddress.replace("tel:+", "");
        	} else if(firstAddress.contains("tel:")){
        		
        		firstAddress = firstAddress.replace("tel:", "");
        	}else if(firstAddress.contains("+")){
        		
        		firstAddress = firstAddress.replace("+", "");
        	}
            
            String firstAddressPrefix = firstAddress.substring(0, 2);          
            
    		for (int a = 0; a < addressArray.length(); a++) {
            	
            	String address = addressArray.getString(a);
				if (executor.isUserAnonymization() && UserMaskHandler.isMaskedUserId(address)) {
					address = UserMaskHandler.transcryptUserId(address, false, (String)context.getProperty("USER_MASKING_SECRET_KEY"));
				}
            	
            	if(address.contains("tel:+")){
            		
            		address = address.replace("tel:+", "");
            	} else if(address.contains("tel:")){
            		
            		address = address.replace("tel:", "");
            	}else if(address.contains("+")){
            		
            		address = address.replace("+", "");
            	}
            	
            	String prefix = address.substring(0,2);
            	
            	if(!firstAddressPrefix.equals(prefix)){
            		
            		throw new CustomException("SVC0001", "", new String[] { "Cross operator addresses not supported" });
            	}
            }
        }

        // Get first address to find operator endpoint
		String firstAddress = jsonBody.getJSONObject("outboundSMSMessageRequest").getJSONArray("address").getString(0);
		// Get resolved MSISDN if request used User Anonymization
		if (executor.isUserAnonymization() && UserMaskHandler.isMaskedUserId(firstAddress)) {
			context.setProperty(MSISDNConstants.MASKED_MSISDN_SUFFIX, HandlerUtils.getMSISDNSuffix(firstAddress));
			firstAddress = UserMaskHandler.transcryptUserId(firstAddress, false, (String)context.getProperty("USER_MASKING_SECRET_KEY"));
		}

		// taking the operator endpoint with first address, since this is for same o,perator
		OperatorEndpoint operatorEndpoint = getEndpoint(firstAddress, context, API_TYPE);

		String sending_add = operatorEndpoint.getEndpointref().getAddress();
		log.info("sending endpoint found: " + sending_add + " Request ID: " + UID.getRequestID(context));
		//-----URL DECODE
		if (sending_add.contains("outbound") && sending_add.contains("requests") && sending_add.contains("tel:+")) {
			sending_add.replace("tel:+", "tel:+");
		}else{
			sending_add = java.net.URLDecoder.decode(sending_add, "UTF-8");
		}
		//-----URL DECODE

		// set auth header, endpoint and handler to message context
		HandlerUtils.setAuthorizationHeader(context, executor, operatorEndpoint);
		HandlerUtils.setEndpointProperty(context, sending_add);
		HandlerUtils.setHandlerProperty(context, this.getClass().getSimpleName());


		// read sendSMSResourceURL from mediatorConfMap and set to message context
		String sendSmsResourceUrlPrefix = getProperty("sendSMSResourceURL");
		if (sendSmsResourceUrlPrefix != null && !sendSmsResourceUrlPrefix.isEmpty()) {
			sendSmsResourceUrlPrefix = sendSmsResourceUrlPrefix.endsWith("/") ?
					sendSmsResourceUrlPrefix.substring(0, sendSmsResourceUrlPrefix.length() - 1) :  sendSmsResourceUrlPrefix;
			sendSmsResourceUrlPrefix = sendSmsResourceUrlPrefix + "/" + senderAddress;

		} else {
			// sendSMSResourceURL not found in mediatorConfMap
			sendSmsResourceUrlPrefix = (String) context.getProperty("REST_URL_PREFIX") + executor.getApiContext()+ "/" + executor.getApiVersion() + executor.getSubResourcePath();
			sendSmsResourceUrlPrefix = sendSmsResourceUrlPrefix.substring(0, sendSmsResourceUrlPrefix.indexOf("/requests"));

		}

		// encode the sender address
		String encodedSenderAddress = null;
		try {
			encodedSenderAddress = URLEncoder.encode(senderAddress, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
			encodedSenderAddress = senderAddress;
		}

		context.setProperty("SEND_SMS_RESOURCE_URL_PREFIX", sendSmsResourceUrlPrefix);
		context.setProperty("REQUEST_ID", requestid);
		context.setProperty("SENDER_ADDRESS", senderAddress);
		context.setProperty("ENCODED_SENDER_ADDRESS", encodedSenderAddress);
		// create an array out of address element and set it to message context
		JSONArray addressElement = jsonBody.getJSONObject("outboundSMSMessageRequest").getJSONArray("address");
		String[] addresses = new String[addressElement.length()];
		String addressList = "";
		String addressSuffixList = "";
		String maskedAddressList = "";
		String maskedAddressSuffixList = "";
		Map<String, String> maskedMsisdnMap = new HashMap<>();
		Map<String, String> maskedMsisdnSuffixMap = new HashMap<>();

		for (int i = 0 ; i < addressElement.length() ; i ++) {
			addresses[i] = addressElement.getString(i);
			String resolvedAddress = addressElement.getString(i);
			String resolvedAddressSuffix = HandlerUtils.getMSISDNSuffix(resolvedAddress);
			if (executor.isUserAnonymization()) {
				maskedAddressList += (addresses[i] + ",");
				// Address List may contains unmasked user Ids too for build address list
				if (UserMaskHandler.isMaskedUserId(addressElement.getString(i))) {
					resolvedAddress = UserMaskHandler.transcryptUserId(addressElement.getString(i), false,
							(String)context.getProperty(MSISDNConstants.USER_MASKING_SECRET_KEY));
					resolvedAddressSuffix = HandlerUtils.getMSISDNSuffix(resolvedAddress);
				}
				String maskedAddressSuffix = HandlerUtils.getMSISDNSuffix(addresses[i]);
				maskedAddressSuffixList += maskedAddressSuffix + ",";
				// Update map<masked-msisdn, actual msisdn>
				maskedMsisdnMap.put(addresses[i], resolvedAddress);
				maskedMsisdnSuffixMap.put(maskedAddressSuffix, resolvedAddressSuffix);
				addresses[i] = resolvedAddress;
			}
			addressList += (addresses[i] + ",");
			addressSuffixList +=  resolvedAddressSuffix + ",";
		}
		context.setProperty(MSISDNConstants.ADDRESSES, addresses);
		context.setProperty(MSISDNConstants.MSISDN_LIST, addressList.substring(0, addressList.length() - 1));
		context.setProperty(MSISDNConstants.MSISDN_SUFFIX_LIST, addressSuffixList.substring(0, addressSuffixList.length() - 1));

		context.setProperty(MSISDNConstants.MSISDN_LIST, addressList.substring(0, addressList.length() - 1));
		if (executor.isUserAnonymization()) {
			context.setProperty(MSISDNConstants.MASKED_MSISDN_LIST, maskedAddressList.substring(0, maskedAddressList.length() - 1));
			context.setProperty(MSISDNConstants.MASKED_MSISDN_MAP, maskedMsisdnMap);

			context.setProperty(MSISDNConstants.MASKED_MSISDN_SUFFIX_LIST, maskedAddressSuffixList.substring(0, maskedAddressSuffixList.length() - 1));
			context.setProperty(MSISDNConstants.MASKED_MSISDN_SUFFIX_MAP, maskedMsisdnSuffixMap);
		}

		context.setProperty("RESPONSE_DELIVERY_INFO_RESOURCE_URL", getProperty("hubGateway")+ executor.getApiContext()+ "/" + executor.getApiVersion() + executor.getSubResourcePath()+ "/" +requestid + "/deliveryInfos");

		context.setProperty("OPERATOR_NAME", operatorEndpoint.getOperator());
		context.setProperty("OPERATOR_ID", operatorEndpoint.getOperatorId());

//		Map<String, SendSMSResponse> smsResponses = smssendmulti(context, subsrequest,
//				jsonBody.getJSONObject("outboundSMSMessageRequest").getJSONArray("address"), API_TYPE,
//				executor.getValidoperators());
//		if (Util.isAllNull(smsResponses.values())) {
//			throw new CustomException("POL0257", "Message not delivered %1", new String[] {
//					"Request failed. Errors " + "occurred while sending the request for all the destinations." });
//		}
//		// NB publish
//		executor.removeHeaders(context);
//		String resPayload = responseHandler.makeSmsSendResponse(context, jsonBody.toString(), smsResponses, requestid);
//		storeRequestIDs(requestid, senderAddress, smsResponses);
//		executor.setResponse(context, resPayload);


		return true;
	}
	
/*TODO
 * Below method should throw a dedicated exception instead of using a generic exception
*/
	private OperatorEndpoint getEndpoint (String address, MessageContext messageContext, String apiType) throws Exception {
		OparatorEndPointSearchDTO searchDTO = new OparatorEndPointSearchDTO();
		searchDTO.setApiName(apiType);
		searchDTO.setContext(messageContext);
		searchDTO.setIsredirect(false);
		searchDTO.setMSISDN(address);
		searchDTO.setOperators(executor.getValidoperators(messageContext));
		searchDTO.setRequestPathURL(executor.getSubResourcePath());
		return occi.getOperatorEndpoint(searchDTO);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.wso2telco.mediator.impl.sms.SMSHandler#validate(java.lang.String,
	 * java.lang.String, org.json.JSONObject, org.apache.synapse.MessageContext)
	 */
	@Override
	public boolean validate(String httpMethod, String requestPath, JSONObject jsonBody, MessageContext context) throws Exception {
		if (!httpMethod.equalsIgnoreCase("POST")) {
			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			throw new Exception("Method not allowed");
		}

		context.setProperty(DataPublisherConstants.OPERATION_TYPE, 200);

		IServiceValidate validator = new ValidateSendSms(executor.isUserAnonymization(), (String)context.getProperty("USER_MASKING_SECRET_KEY"));
		validator.validateUrl(requestPath);
		validator.validate(jsonBody.toString());
		ValidationUtils.compareSenderId(executor.getSubResourcePath(), executor.getJsonBody(), context);
		String senderName = jsonBody.getJSONObject("outboundSMSMessageRequest").optString("senderName");

		if (senderName.equals("") || senderName == null || senderName.length() < 10) {
			context.setProperty(DataPublisherConstants.MERCHANT_ID, "");
			context.setProperty(DataPublisherConstants.CATEGORY, "");
			context.setProperty(DataPublisherConstants.SUB_CATEGORY, "");
		} else {
			if (senderName.substring(0, 3).equals("000")) {
				context.setProperty(DataPublisherConstants.MERCHANT_ID, "");
			} else {
				context.setProperty(DataPublisherConstants.MERCHANT_ID, senderName.substring(0, 3));
			}

			if (senderName.substring(3, 6).equals("000")) {
				context.setProperty(DataPublisherConstants.CATEGORY, "");
			} else {
				context.setProperty(DataPublisherConstants.CATEGORY, senderName.substring(3, 6));
			}

			if (senderName.substring(6, 9).equals("000")) {
				context.setProperty(DataPublisherConstants.SUB_CATEGORY, "");
			} else {
				context.setProperty(DataPublisherConstants.SUB_CATEGORY, senderName.substring(6, 9));
			}
		}
		return true;
	}

	/**
	 * Smssendmulti.
	 *
	 * @param smsmc
	 *            the smsmc
	 * @param sendreq
	 *            the sendreq
	 * @param listaddr
	 *            the listaddr
	 * @param apitype
	 *            the apitype
	 * @param operators
	 *            the operators
	 * @return the map
	 * @throws Exception
	 *             the exception
	 */
	
	/*TODO
	 * Below method should throw a dedicated exception instead of using a generic exception
	*/
	private Map<String, SendSMSResponse> smssendmulti(MessageContext smsmc, SendSMSRequest sendreq, JSONArray listaddr, String apitype, List<OperatorApplicationDTO> operators) throws Exception {

		OperatorEndpoint endpoint = null;
		String jsonStr;
		String address;
		Map<String, SendSMSResponse> smsResponses = new HashMap<String, SendSMSResponse>();
		for (int i = 0; i < listaddr.length(); i++) {

			SendSMSResponse sendSMSResponse = null;
			address = listaddr.getString(i);

            log.info("id : " + address + " Request ID: " + UID.getRequestID(smsmc));
			smsmc.setProperty(MSISDNConstants.USER_MSISDN, address.substring(5));
			OparatorEndPointSearchDTO searchDTO = new OparatorEndPointSearchDTO();
			searchDTO.setApiName(apitype);
			searchDTO.setContext(smsmc);
			searchDTO.setIsredirect(false);
			searchDTO.setMSISDN(address);
			searchDTO.setOperators(operators);
			searchDTO.setRequestPathURL(executor.getSubResourcePath());

			endpoint = occi.getOperatorEndpoint(searchDTO);

			List<String> sendAdr = new ArrayList<String>();
			sendAdr.add(address);
			sendreq.getOutboundSMSMessageRequest().setAddress(sendAdr);
			jsonStr = new Gson().toJson(sendreq);
			String sending_add = endpoint.getEndpointref().getAddress();
			log.info("sending endpoint found: " + sending_add + " Request ID: " + UID.getRequestID(smsmc));
			  //-----URL DECODE
            if (sending_add.contains("outbound") && sending_add.contains("requests") && sending_add.contains("tel:+")) {
            	sending_add.replace("tel:+", "tel:+");
			}else{
				sending_add = java.net.URLDecoder.decode(sending_add, "UTF-8");
			}
            //-----URL DECODE
			String responseStr = executor.makeRequest(endpoint, sending_add, jsonStr, true, smsmc,false);
			sendSMSResponse = parseJsonResponse(responseStr);

			smsResponses.put(address, sendSMSResponse);
		}

		return smsResponses;
	}

	/**
	 * Parses the json response.
	 *
	 * @param responseString
	 *            the response string
	 * @return the send sms response
	 */
	private SendSMSResponse parseJsonResponse(String responseString) {
		Gson gson = new GsonBuilder().create();
		SendSMSResponse smsResponse;
		try {
			smsResponse = gson.fromJson(responseString, SendSMSResponse.class);
			if (smsResponse.getOutboundSMSMessageRequest() == null) {
				return null;
			}
		} catch (JsonSyntaxException e) {
			log.error(e.getMessage(), e);
			return null;
		}
		return smsResponse;
	}

	/**
	 * Store request i ds.
	 *
	 * @param requestID
	 *            the request id
	 * @param senderAddress
	 *            the sender address
	 * @param smsResponses
	 *            the sms responses
	 * @throws Exception
	 */
	/*private void storeRequestIDs(String requestID, String senderAddress, Map<String, SendSMSResponse> smsResponses)
			throws Exception {

		Map<String, String> reqIdMap = new HashMap<String, String>(smsResponses.size());

		for (Map.Entry<String, SendSMSResponse> entry : smsResponses.entrySet()) {

			SendSMSResponse smsResponse = entry.getValue();
			String pluginReqId = null;

			if (smsResponse != null) {

				String resourceURL = smsResponse.getOutboundSMSMessageRequest().getResourceURL().trim();
				String[] segments = resourceURL.split("/");
				pluginReqId = segments[segments.length - 1];
			}

			reqIdMap.put(entry.getKey(), pluginReqId);
		}

		smsMessagingService.insertSMSRequestIds(requestID, senderAddress, reqIdMap);
	}*/

	/**
	 * Gets the SMS message count.
	 *
	 * @param textMessage
	 *            the text message
	 * @return the SMS message count
	 */
	private int getSMSMessageCount(String textMessage) {
		
		final int asciiMessageLength=160;
		final int utf8MessageLength=70;

		int smsCount = 0;
		try {
			int count = textMessage.length();
			log.debug("Character count of text message : " + count);
			if (count > 0) {
				if (CharMatcher.ASCII.matchesAllOf(textMessage)) {
					log.debug("ASCII MESSAGE");
					int tempSMSCount = count / asciiMessageLength;
					int tempRem = count % asciiMessageLength;

					if (tempRem > 0) {
						tempSMSCount++;
					}
					smsCount = tempSMSCount;

				} else {
					log.debug("UTF8 MESSAGE");
					int tempSMSCount = count / utf8MessageLength;
					int tempRem = count % utf8MessageLength;

					if (tempRem > 0) {
						tempSMSCount++;
					}
					smsCount = tempSMSCount;
				}
			}
		} catch (Exception e) {
			log.error("error in getSMSMessageCharacterCount : " + e.getMessage());
			return 0;
		}
		log.debug("SMS count : " + smsCount);
		return smsCount;

	}
}
