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

import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.entity.OparatorEndPointSearchDTO;
import com.wso2telco.dep.mediator.internal.AggregatorValidator;
import com.wso2telco.dep.mediator.internal.ApiUtils;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.PaymentService;
import com.wso2telco.dep.mediator.util.APIType;
import com.wso2telco.dep.mediator.util.FileNames;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.mediator.util.ValidationUtils;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.payment.ValidateRefund;
import com.wso2telco.dep.subscriptionvalidator.util.ValidatorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author User
 */
public class AmountRefundHandler implements PaymentHandler {

	private static Log log = LogFactory.getLog(AmountRefundHandler.class);
	private OriginatingCountryCalculatorIDD occi;
	private PaymentExecutor executor;
	private PaymentService dbservice;
	private ApiUtils apiUtils;
	private PaymentUtil paymentUtil;

	public AmountRefundHandler(PaymentExecutor executor) {
		this.executor = executor;
		occi = new OriginatingCountryCalculatorIDD();
		dbservice = new PaymentService();
		apiUtils = new ApiUtils();
		paymentUtil = new PaymentUtil();
	}

	@Override
	public boolean validate(String httpMethod, String requestPath,
			JSONObject jsonBody, MessageContext context) throws Exception {
		if (!httpMethod.equalsIgnoreCase("POST")) {
			((Axis2MessageContext) context).getAxis2MessageContext()
			                               .setProperty("HTTP_SC", 405);
			throw new Exception("Method not allowed");
		}

		IServiceValidate validator = new ValidateRefund();
		validator.validateUrl(requestPath);
		validator.validate(jsonBody.toString());
		ValidationUtils.compareMsisdn(executor.getSubResourcePath(), executor.getJsonBody(), APIType.PAYMENT);
		return true;
	}

	@Override
    public boolean handle(MessageContext context) throws Exception {

        String requestId = UID.getUniqueID(Type.PAYMENT.getCode(), context,
                executor.getApplicationid());

        HashMap<String, String> jwtDetails = apiUtils
                .getJwtTokenDetails(context);
        OperatorEndpoint endpoint = null;
        String clientCorrelator = null;
        String sending_add = null;

        FileReader fileReader = new FileReader();
        String file = CarbonUtils.getCarbonConfigDirPath() + File.separator + FileNames.MEDIATOR_CONF_FILE.getFileName();
        Map<String, String> mediatorConfMap = fileReader.readPropertyFile(file);
        String hub_gateway_id = mediatorConfMap.get("hub_gateway_id");
        if (log.isDebugEnabled()) {
            log.debug("Hub / Gateway Id : " + hub_gateway_id);
        }

        String appId = jwtDetails.get("applicationid");
        if (log.isDebugEnabled()) {
            log.debug("Application Id : " + appId);
        }
        String subscriber = jwtDetails.get("subscriber");
        if (log.isDebugEnabled()) {
            log.debug("Subscriber Name : " + subscriber);
        }

        try {
            JSONObject jsonBody = executor.getJsonBody();

            String endUserId = jsonBody.getJSONObject("amountTransaction").getString("endUserId");
            String msisdn = endUserId.substring(5);
            context.setProperty(MSISDNConstants.USER_MSISDN, msisdn);
            context.setProperty(MSISDNConstants.MSISDN, endUserId);
            // OperatorEndpoint endpoint = null;
            if (ValidatorUtils.getValidatorForSubscriptionFromMessageContext(context).validate(context)) {
                OparatorEndPointSearchDTO searchDTO = new OparatorEndPointSearchDTO();
                searchDTO.setApi(APIType.PAYMENT);
                searchDTO.setApiName((String) context.getProperty("API_NAME"));
                searchDTO.setContext(context);
                searchDTO.setIsredirect(false);
                searchDTO.setMSISDN(endUserId.replace("tel:", ""));
                searchDTO.setOperators(executor.getValidoperators(context));
                searchDTO.setRequestPathURL(executor.getSubResourcePath());
                endpoint = occi.getOperatorEndpoint(searchDTO);
            }

            sending_add = endpoint.getEndpointref().getAddress();
            if (log.isDebugEnabled()) {
                log.debug("sending endpoint found: " + sending_add);
            }


            if (!jsonBody.has("amountTransaction")) {
                throw new CustomException("SVC0001", "", new String[]{"Incorrect JSON Object received"});
            }
            JSONObject objAmountTransaction = jsonBody.getJSONObject("amountTransaction");

            if (!objAmountTransaction.isNull("clientCorrelator")) {

                clientCorrelator = nullOrTrimmed(objAmountTransaction.get("clientCorrelator").toString());
            }
            if (clientCorrelator == null || clientCorrelator.equals("")) {
                if (log.isDebugEnabled()) {
                    log.debug("clientCorrelator not provided by application and hub/plugin generating clientCorrelator on behalf of application");
                }
                String hashString = apiUtils.getHashString(jsonBody.toString());
                if (log.isDebugEnabled()) {
                    log.debug("hashString : " + hashString);
                }
                clientCorrelator = hashString + "-" + requestId + ":" + hub_gateway_id + ":" + appId;

            } else {

                if (log.isDebugEnabled()) {
                    log.debug("clientCorrelator provided by application");
                }
                clientCorrelator = clientCorrelator + ":" + hub_gateway_id + ":" + appId;
            }

            JSONObject paymentAmount = objAmountTransaction.getJSONObject("paymentAmount");

            if (paymentAmount.has("chargingMetaData")) {

                JSONObject chargingMetaData = paymentAmount.getJSONObject("chargingMetaData");
                /* String subscriber = paymentUtil.storeSubscription(context); */
                boolean isAggregator = PaymentUtil.isAggregator(context);

                if (isAggregator) {
                    // JSONObject chargingdmeta =
                    // clientclr.getJSONObject("paymentAmount").getJSONObject("chargingMetaData");
                    if (!chargingMetaData.isNull("onBehalfOf")) {
                        new AggregatorValidator().validateMerchant(
                                Integer.valueOf(executor.getApplicationid()),
                                endpoint.getOperator(), subscriber,
                                chargingMetaData.getString("onBehalfOf"));
                    }
                }


                // validate payment categoreis
                List<String> validPayCategories = dbservice.getValidPayCategories();
                paymentUtil.validatePaymentCategory(chargingMetaData, validPayCategories);
            }
        } catch (JSONException e) {
            log.error("Manipulating received JSON Object: " + e);
            throw new CustomException("SVC0001", "", new String[]{"Incorrect JSON Object received"});
        }

        // set information to the message context, to be used in the sequence
        HandlerUtils.setHandlerProperty(context, this.getClass().getSimpleName());
        HandlerUtils.setEndpointProperty(context, sending_add);
        HandlerUtils.setGatewayHost(context);
        HandlerUtils.setAuthorizationHeader(context, executor, endpoint);
        context.setProperty("requestResourceUrl", executor.getResourceUrl());
        context.setProperty("requestID", requestId);
        context.setProperty("clientCorrelator", clientCorrelator);
        context.setProperty("operator", endpoint.getOperator());
        context.setProperty("OPERATOR_NAME", endpoint.getOperator());
        context.setProperty("OPERATOR_ID", endpoint.getOperatorId());

        return true;
    }

	private String makeRefundResponse(String responseStr, String requestid,
			String clientCorrelator) {

		String jsonResponse = null;

		try {

			FileReader fileReader = new FileReader();
	       	String file = CarbonUtils.getCarbonConfigDirPath() + File.separator + FileNames.MEDIATOR_CONF_FILE.getFileName();
			Map<String, String> mediatorConfMap = fileReader.readPropertyFile(file);
			String ResourceUrlPrefix = mediatorConfMap.get("hubGateway");

			JSONObject jsonObj = new JSONObject(responseStr);
			JSONObject objAmountTransaction = jsonObj.getJSONObject("amountTransaction");

			objAmountTransaction.put("clientCorrelator", clientCorrelator);
			objAmountTransaction.put("resourceURL", ResourceUrlPrefix
					+ executor.getResourceUrl() + "/" + requestid);
			jsonResponse = jsonObj.toString();
		} catch (Exception e) {

			log.error("Error in formatting amount refund response : "+ e.getMessage());
			throw new CustomException("SVC1000", "", new String[] { null });
		}

		if (log.isDebugEnabled())
			log.debug("Formatted amount refund response : " + jsonResponse);
		return jsonResponse;

	}

	/**
	 * + * Ensure the input value is either a null value or a trimmed string +
	 */
	public static String nullOrTrimmed(String s) {
		String rv = null;
		if (s != null && s.trim().length() > 0) {
			rv = s.trim();
		}
		return rv;
	}

}