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
import com.wso2telco.dep.mediator.delegator.CarbonUtilsDelegator;
import com.wso2telco.dep.mediator.delegator.OCCIDelegator;
import com.wso2telco.dep.mediator.delegator.ValidatorUtilsDelegator;
import com.wso2telco.dep.mediator.entity.OparatorEndPointSearchDTO;
import com.wso2telco.dep.mediator.internal.AggregatorValidator;
import com.wso2telco.dep.mediator.internal.ApiUtils;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.service.PaymentService;
import com.wso2telco.dep.mediator.util.*;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.payment.ValidateRefund;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONException;
import org.json.JSONObject;

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

    private OCCIDelegator occi;

    private PaymentService paymentService;

    private PaymentExecutor executor;

    private ApiUtils apiUtils;

    private PaymentUtil paymentUtil;

    private static List<String> validCategories = null;

    private CarbonUtilsDelegator carbonUtilsDelegator;
    private ValidatorUtilsDelegator validatorUtilsDelegator;
    private FileReader fileReader;

    @Deprecated
	public AmountRefundHandler(PaymentExecutor executor) {
        this.executor = executor;
        occi = OCCIDelegator.getInstance();
        paymentService = new PaymentService();
        apiUtils = new ApiUtils();
        paymentUtil = new PaymentUtil();
        fileReader = new FileReader();
        carbonUtilsDelegator = CarbonUtilsDelegator.getInstance();
        validatorUtilsDelegator  = ValidatorUtilsDelegator.getInstance();
	}

    public AmountRefundHandler(OCCIDelegator occi, PaymentService paymentService, PaymentExecutor executor,
                               ApiUtils apiUtils, PaymentUtil paymentUtil, CarbonUtilsDelegator carbonUtilsDelegator,
                               ValidatorUtilsDelegator validatorUtilsDelegator, FileReader fileReader) {
        this.occi = occi;
        this.paymentService = paymentService;
        this.executor = executor;
        this.apiUtils = apiUtils;
        this.paymentUtil = paymentUtil;
        this.carbonUtilsDelegator = carbonUtilsDelegator;
        this.validatorUtilsDelegator = validatorUtilsDelegator;
        this.fileReader = fileReader;
    }

    @Override
	public boolean validate(String httpMethod, String requestPath,
			JSONObject jsonBody, MessageContext context) throws Exception {
		if (!httpMethod.equalsIgnoreCase(HttpPost.METHOD_NAME)) {
			((Axis2MessageContext) context).getAxis2MessageContext()
			                               .setProperty(MediatorConstants.HTTP_SC, HttpStatus.SC_METHOD_NOT_ALLOWED);
			throw new Exception("Method not allowed");
		}
        UserMaskingUtils.setPaymentUserMaskingContextProperties(executor, context, jsonBody);
        ValidationUtils.compareMsisdn(executor.getSubResourcePath(), executor.getJsonBody());
		IServiceValidate validator = new ValidateRefund(executor.isUserAnonymization(), (String) context.getProperty(MSISDNConstants.MSISDN));
		validator.validateUrl(requestPath);
		validator.validate(jsonBody.toString());
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

        String file = carbonUtilsDelegator.getCarbonConfigDirPath() + File.separator + FileNames.MEDIATOR_CONF_FILE.getFileName();
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
            if (validatorUtilsDelegator.getValidatorForSubscriptionFromMessageContext(context).validate(context)) {
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

            sendingAdd = PaymentUtil.decodeSendingAddressIfMasked(executor, context, sendingAdd);
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

            JSONObject paymentAmount = objAmountTransaction.getJSONObject(AttributeConstants.PAYMENT_AMOUNT);

            if (paymentAmount.has(AttributeConstants.CHARGING_META_DATA) &&
                    !paymentAmount.isNull(AttributeConstants.CHARGING_META_DATA)) {

                JSONObject chargingMetaData = paymentAmount.getJSONObject(AttributeConstants.CHARGING_META_DATA);
                boolean isAggregator = PaymentUtil.isAggregator(context);

                if (isAggregator && !chargingMetaData.isNull("onBehalfOf")) {
                    new AggregatorValidator().validateMerchant(
                            Integer.valueOf(executor.getApplicationid()),
                            endpoint.getOperator(), subscriber,
                            chargingMetaData.getString("onBehalfOf"));
                }


                // validate payment categoreis
                List<String> validPayCategories = paymentService.getValidPayCategories();
                paymentUtil.validatePaymentCategory(chargingMetaData, validPayCategories);
            }
        } catch (JSONException e) {
            log.error("Manipulating received JSON Object: " + e);
            throw new CustomException("SVC0001", "", new String[]{"Incorrect JSON Object received"});
        }

        // set information to the message context, to be used in the sequence
        HandlerUtils.setHandlerProperty(context, this.getClass().getSimpleName());
        HandlerUtils.setEndpointProperty(context, sendingAdd);
        context.setProperty("hubGateway", mediatorConfMap.get("hubGateway"));
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