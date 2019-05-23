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
import com.wso2telco.dep.mediator.MediatorConstants;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.entity.OparatorEndPointSearchDTO;
import com.wso2telco.dep.mediator.internal.AggregatorValidator;
import com.wso2telco.dep.mediator.internal.ApiUtils;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.PaymentService;
import com.wso2telco.dep.mediator.util.*;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.payment.ValidateRefund;
import com.wso2telco.dep.subscriptionvalidator.util.ValidatorUtils;
import com.wso2telco.dep.user.masking.configuration.UserMaskingConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
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
		if (!httpMethod.equalsIgnoreCase(HttpPost.METHOD_NAME)) {
			((Axis2MessageContext) context).getAxis2MessageContext()
			                               .setProperty(MediatorConstants.HTTP_SC, HttpStatus.SC_METHOD_NOT_ALLOWED);
			throw new Exception("Method not allowed");
		}

		IServiceValidate validator = new ValidateRefund(executor.isUserAnonymization(), UserMaskingConfiguration.getInstance().getSecretKey());
		validator.validateUrl(requestPath);
		validator.validate(jsonBody.toString());
        ValidationUtils.compareMsisdn(executor.getSubResourcePath(), executor.getJsonBody());
		UserMaskingUtils.setPaymentUserMaskingContextProperties(executor, context, jsonBody);
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
        String sendingAdd = null;

        FileReader fileReader = new FileReader();
        String file = CarbonUtils.getCarbonConfigDirPath() + File.separator + FileNames.MEDIATOR_CONF_FILE.getFileName();
        Map<String, String> mediatorConfMap = fileReader.readPropertyFile(file);
        String hubGatewayId = mediatorConfMap.get("hub_gateway_id");
        if (log.isDebugEnabled()) {
            log.debug("Hub / Gateway Id : " + hubGatewayId);
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
            String endUserId = (String) context.getProperty(MSISDNConstants.MSISDN);
            if (ValidatorUtils.getValidatorForSubscriptionFromMessageContext(context).validate(context)) {
                    OparatorEndPointSearchDTO searchDTO = new OparatorEndPointSearchDTO();
                    searchDTO.setApi(APIType.PAYMENT);
                    searchDTO.setApiName((String) context.getProperty("API_NAME"));
                    searchDTO.setContext(context);
                    searchDTO.setIsredirect(false);
                    searchDTO.setMSISDN(endUserId.replace("tel:", ""));
                    searchDTO.setOperators(executor.getValidoperators(context));
                    searchDTO.setRequestPathURL(executor.getSubResourcePath());
                    searchDTO.setLoggingMsisdn((String)context.getProperty("MASKED_MSISDN"));
                    endpoint = occi.getOperatorEndpoint(searchDTO);
            }

            sendingAdd = endpoint.getEndpointref().getAddress();
            if (log.isDebugEnabled()) {
                log.debug("sending endpoint found: " + sendingAdd);
            }
          
            sending_add = PaymentUtil.decodeSendingAddressIfMasked(executor, sending_add);
            if (!jsonBody.has(AttributeConstants.AMOUNT_TRANSACTION)) {
                throw new CustomException("SVC0001", "", new String[]{"Incorrect JSON Object received"});
            }
            JSONObject objAmountTransaction = jsonBody.getJSONObject(AttributeConstants.AMOUNT_TRANSACTION);

            if (!objAmountTransaction.isNull(AttributeConstants.CLIENT_CORRELATOR)) {

                clientCorrelator = nullOrTrimmed(objAmountTransaction.get(AttributeConstants.CLIENT_CORRELATOR).toString());
            }
            if (clientCorrelator == null || clientCorrelator.equals("")) {
                if (log.isDebugEnabled()) {
                    log.debug("clientCorrelator not provided by application and hub/plugin generating clientCorrelator on behalf of application");
                }
                String hashString = apiUtils.getHashString(jsonBody.toString());
                if (log.isDebugEnabled()) {
                    log.debug("hashString : " + hashString);
                }
                clientCorrelator = hashString + "-" + requestId + ":" + hubGatewayId + ":" + appId;

            } else {

                if (log.isDebugEnabled()) {
                    log.debug("clientCorrelator provided by application");
                }
                clientCorrelator = clientCorrelator + ":" + hubGatewayId + ":" + appId;
            }

            JSONObject paymentAmount = objAmountTransaction.getJSONObject("paymentAmount");

            if (paymentAmount.has("chargingMetaData")) {

                JSONObject chargingMetaData = paymentAmount.getJSONObject("chargingMetaData");
                boolean isAggregator = PaymentUtil.isAggregator(context);

                if (isAggregator && !chargingMetaData.isNull("onBehalfOf")) {
                    new AggregatorValidator().validateMerchant(
                            Integer.valueOf(executor.getApplicationid()),
                            endpoint.getOperator(), subscriber,
                            chargingMetaData.getString("onBehalfOf"));
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
        HandlerUtils.setEndpointProperty(context, sendingAdd);
        HandlerUtils.setGatewayHost(context);
        HandlerUtils.setAuthorizationHeader(context, executor, endpoint);
        context.setProperty("requestResourceUrl", executor.getResourceUrl());
        context.setProperty("requestID", requestId);
        context.setProperty(AttributeConstants.CLIENT_CORRELATOR, clientCorrelator);
        context.setProperty("operator", endpoint.getOperator());
        context.setProperty("OPERATOR_NAME", endpoint.getOperator());
        context.setProperty("OPERATOR_ID", endpoint.getOperatorId());

        return true;
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