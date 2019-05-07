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
import com.wso2telco.dep.mediator.util.*;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.payment.ValidatePaymentCharge;
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
 * @author WSO2telco
 */
public class AmountChargeHandler implements PaymentHandler {

	private Log log = LogFactory.getLog(AmountChargeHandler.class);

	private OriginatingCountryCalculatorIDD occi;

	private PaymentService paymentService;

	private PaymentExecutor executor;

	private ApiUtils apiUtils;

	private PaymentUtil paymentUtil;

	private static List<String> validCategories = null;

	public AmountChargeHandler(PaymentExecutor executor) {
		this.executor = executor;
		occi = new OriginatingCountryCalculatorIDD();
		paymentService = new PaymentService();
		apiUtils = new ApiUtils();
		paymentUtil = new PaymentUtil();
	}

	public boolean handle(MessageContext context) throws Exception {

		String requestId = UID.getUniqueID(Type.PAYMENT.getCode(), context, executor.getApplicationid());

		HashMap<String, String> jwtDetails = apiUtils.getJwtTokenDetails(context);
        OperatorEndpoint endpoint = null;
        String clientCorrelator = null;
        String sending_add = null;

		String requestResourceURL = executor.getResourceUrl();

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

        JSONObject jsonBody = executor.getJsonBody();
        try {

            String endUserId = jsonBody.getJSONObject("amountTransaction").getString("endUserId");
            String msisdn = endUserId.substring(5);
            //Double chargeamount = Double.parseDouble(jsonBody.getJSONObject("amountTransaction").getJSONObject("paymentAmount").getJSONObject("chargingInformation").getString("amount"));

            context.setProperty(MSISDNConstants.USER_MSISDN, msisdn);
            context.setProperty(MSISDNConstants.MSISDN, endUserId);

            if (ValidatorUtils.getValidatorForSubscriptionFromMessageContext(context).validate(context)) {
                OparatorEndPointSearchDTO searchDTO = new OparatorEndPointSearchDTO();
                searchDTO.setApi(APIType.PAYMENT);
                searchDTO.setApiName((String) context.getProperty("API_NAME"));
                searchDTO.setContext(context);
                searchDTO.setIsredirect(false);
                searchDTO.setMSISDN(endUserId);
                searchDTO.setOperators(executor.getValidoperators(context));
                searchDTO.setRequestPathURL(executor.getSubResourcePath());
                endpoint = occi.getOperatorEndpoint(searchDTO);

            }

            sending_add = endpoint.getEndpointref().getAddress();
            if (log.isDebugEnabled()) {
                log.info("sending endpoint found: " + sending_add + " Request ID: " + UID.getRequestID(context));
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

            if (objAmountTransaction.has("chargingMetaData")) {

                JSONObject chargingMeta = objAmountTransaction.getJSONObject("paymentAmount").getJSONObject("chargingMetaData");

                boolean isAggregator = paymentUtil.isAggregator(context);

                if (isAggregator) {
                    //JSONObject chargingMeta = objAmountTransaction.getJSONObject("paymentAmount").getJSONObject("chargingMetaData");
                    if (!chargingMeta.isNull("onBehalfOf")) {
                        new AggregatorValidator().validateMerchant(
                                Integer.valueOf(executor.getApplicationid()),
                                endpoint.getOperator(), subscriber,
                                chargingMeta.getString("onBehalfOf"));
                    }
                }

                if ((!chargingMeta.isNull("purchaseCategoryCode"))
                        && (!chargingMeta.getString("purchaseCategoryCode").isEmpty())) {

                    if (validCategories == null || validCategories.isEmpty() || (!validCategories.contains(chargingMeta.getString("purchaseCategoryCode")))) {
                        validCategories = paymentService.getValidPayCategories();
                    }

                }

                //validatePaymentCategory(chargingMeta, validCategories);
                paymentUtil.validatePaymentCategory(chargingMeta, validCategories);
            }
        }catch (JSONException e){
            log.error("Manipulating received JSON Object: " + e);
            throw new CustomException("SVC0001", "", new String[]{"Incorrect JSON Object received"});
        }


		// set information to the message context, to be used in the sequence
        HandlerUtils.setHandlerProperty(context, this.getClass().getSimpleName());
        HandlerUtils.setEndpointProperty(context, sending_add);
        HandlerUtils.setGatewayHost(context);
        HandlerUtils.setAuthorizationHeader(context, executor, endpoint);
        context.setProperty("operator", endpoint.getOperator());
        context.setProperty("requestResourceUrl", requestResourceURL);
        context.setProperty("requestID", requestId);
        context.setProperty("clientCorrelator", clientCorrelator);
		context.setProperty("OPERATOR_NAME", endpoint.getOperator());
		context.setProperty("OPERATOR_ID", endpoint.getOperatorId());

        return true;

	}

	@Override
	public boolean validate(String httpMethod, String requestPath, JSONObject jsonBody, MessageContext context) throws Exception {
		IServiceValidate validator;
		String validatorClass = (String) context.getProperty("validatorClass");

		if (!httpMethod.equalsIgnoreCase("POST")) {
			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			log.error("Method not allowed");
			throw new Exception("Method not allowed");
		}

		if(validatorClass != null){
			Class clazz = Class.forName(validatorClass);
			validator = (IServiceValidate) clazz.newInstance();
		}else{
			validator = new ValidatePaymentCharge();
		}

		validator.validateUrl(requestPath);
		validator.validate(jsonBody.toString());
		ValidationUtils.compareMsisdn(executor.getSubResourcePath(), executor.getJsonBody(), APIType.PAYMENT);
		return true;
	}

	/**
	 * Check spend limit.
	 *
	 * @param msisdn
	 *            the msisdn
	 * @param operator
	 *            the operator
	 * @param mc
	 *            the mc
	 * @return true, if successful
	 * @throws AxataDBUtilException
	 *             the axata db util exception
	 */
	/*private boolean checkSpendLimit(String msisdn, String operator, MessageContext context) throws AxataDBUtilException {
		AuthenticationContext authContext = APISecurityUtils.getAuthenticationContext(context);
		String consumerKey = "";
		if (authContext != null) {
			consumerKey = authContext.getConsumerKey();
		}

		SpendLimitHandler spendLimitHandler = new SpendLimitHandler();
		if (spendLimitHandler.isMSISDNSpendLimitExceeded(msisdn)) {
			throw new CustomException("POL1001", "The %1 charging limit for this user has been exceeded", new String[] { "daily" });
		} else if (spendLimitHandler.isApplicationSpendLimitExceeded(consumerKey)) {
			throw new CustomException("POL1001","The %1 charging limit for this application has been exceeded",new String[] { "daily" });
		} else if (spendLimitHandler.isOperatorSpendLimitExceeded(operator)) {
			throw new CustomException("POL1001","The %1 charging limit for this operator has been exceeded",new String[] { "daily" });
		}
		return true;
	}*/

	/**
	 * Store subscription.
	 *
	 * @param context
	 *            the context
	 * @return the string
	 * @throws AxisFault
	 *             the axis fault
	 */
	/*private String storeSubscription(MessageContext context) throws AxisFault {
		String subscription = null;

		org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) context).getAxis2MessageContext();
		Object headers = axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
		if (headers != null && headers instanceof Map) {
			try {
				Map headersMap = (Map) headers;
				String jwtparam = (String) headersMap.get("x-jwt-assertion");
				String[] jwttoken = jwtparam.split("\\.");
				String jwtbody = Base64Coder.decodeString(jwttoken[1]);
				JSONObject jwtobj = new JSONObject(jwtbody);
				subscription = jwtobj.getString("http://wso2.org/claims/subscriber");

			} catch (JSONException ex) {
				throw new AxisFault("Error retriving application id");
			}
		}

		return subscription;
	}*/

	/**
	 * Checks if is aggregator.
	 *
	 * @param context
	 *            the context
	 * @return true, if is aggregator
	 * @throws AxisFault
	 *             the axis fault
	 */
	/*private boolean isAggregator(MessageContext context) throws AxisFault {
		boolean aggregator = false;

		try {
			org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) context).getAxis2MessageContext();
			Object headers = axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
			if (headers != null && headers instanceof Map) {
				Map headersMap = (Map) headers;
				String jwtparam = (String) headersMap.get("x-jwt-assertion");
				String[] jwttoken = jwtparam.split("\\.");
				String jwtbody = Base64Coder.decodeString(jwttoken[1]);
				JSONObject jwtobj = new JSONObject(jwtbody);
				String claimaggr = jwtobj.getString("http://wso2.org/claims/role");
				if (claimaggr != null) {
					String[] allowedRoles = claimaggr.split(",");
					for (int i = 0; i < allowedRoles.length; i++) {
						if (allowedRoles[i].contains(MSISDNConstants.AGGRIGATOR_ROLE)) {
							aggregator = true;
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			log.info("Error retrive aggregator");
		}

		return aggregator;
	}*/

	/**
	 * Validate payment category.
	 *
	 * @param chargingMeta
	 *            the chargingMeta
	 * @param lstCategories
	 *            the lst categories
	 * @throws JSONException
	 *             the JSON exception
	 */
	/*private void validatePaymentCategory(JSONObject chargingMeta, List<String> lstCategories) throws JSONException {
		boolean isvalid = false;
		String chargeCategory = "";
		if ((!chargingMeta.isNull("purchaseCategoryCode"))	&& (!chargingMeta.getString("purchaseCategoryCode").isEmpty())) {

			chargeCategory = chargingMeta.getString("purchaseCategoryCode");
			for (String d : lstCategories) {
				if (d.equalsIgnoreCase(chargeCategory)) {
					isvalid = true;
					break;
				}
			}
		} else {
			isvalid = true;
		}

		if (!isvalid) {
			throw new CustomException("POL0001","A policy error occurred. Error code is %1",new String[] { "Invalid " + "purchaseCategoryCode : "+ chargeCategory });
		}
	}
*/
	/**
	 * Str_piece.
	 *
	 * @param str
	 *            the str
	 * @param separator
	 *            the separator
	 * @param index
	 *            the index
	 * @return the string
	 */
	private String strPiece(String str, char separator, int index) {
		String strResult = "";
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == separator) {
				count++;
				if (count == index) {
					break;
				}
			} else {
				if (count == index - 1) {
					strResult += str.charAt(i);
				}
			}
		}
		return strResult;
	}

	public static String nullOrTrimmed(String s) {
		String rv = null;
		if (s != null && s.trim().length() > 0) {
			rv = s.trim();
		}
		return rv;
	}


}
