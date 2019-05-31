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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.entity.smsmessaging.CallbackReference;
import com.wso2telco.dep.mediator.entity.ussd.ShortCodes;
import com.wso2telco.dep.mediator.entity.ussd.SubscriptionGatewayRequest;
import com.wso2telco.dep.mediator.entity.ussd.SubscriptionGatewayRequestDTO;
import com.wso2telco.dep.mediator.entity.ussd.SubscriptionHubRequest;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.USSDService;
import com.wso2telco.dep.mediator.util.ConfigFileReader;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.ussd.ValidateUssdSubscription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

public class NorthBoundUSSDSubscriptionHandler implements USSDHandler {
	
	private Log log = LogFactory.getLog(NorthBoundUSSDSubscriptionHandler.class);
    private static final String API_TYPE ="ussd";
    private OriginatingCountryCalculatorIDD occi;
    private USSDExecutor executor;
    private USSDService ussdService;

    public NorthBoundUSSDSubscriptionHandler(USSDExecutor ussdExecutor){

        occi = new OriginatingCountryCalculatorIDD();
        this.executor = ussdExecutor;
        ussdService = new USSDService();
    }

    @Override
    public boolean validate(String httpMethod, String requestPath, JSONObject jsonBody, MessageContext context) throws Exception {
        IServiceValidate validator;

        validator = new ValidateUssdSubscription();
        validator.validateUrl(requestPath);
        validator.validate(jsonBody.toString());

        return true;
    }

    @Override
    public boolean handle(MessageContext context) throws Exception {

        String requestid = UID.getUniqueID(Type.MO_USSD.getCode(), context, executor.getApplicationid());
        JSONObject jsonBody = executor.getJsonBody();
        String notifyUrl = jsonBody.getJSONObject("subscription").getJSONObject("callbackReference").getString("notifyURL");
        Gson gson = new GsonBuilder().serializeNulls().create();

        String consumerKey = (String) context.getProperty("CONSUMER_KEY");
        String userId = (String) context.getProperty("USER_ID");

        //Integer subscriptionId = ussdService.ussdRequestEntry(notifyUrl,consumerKey);
        String operatorId="";
        Integer subscriptionId = ussdService.ussdRequestEntry(notifyUrl,consumerKey,operatorId,userId);
        log.info("created subscription Id  -  " + subscriptionId);

        Map<String, String> mediatorConfMap = ConfigFileReader.getInstance().getMediatorConfigMap();
        String subsEndpoint = mediatorConfMap.get("ussdGatewayEndpoint")+subscriptionId;
        log.info("Subsendpoint - " +subsEndpoint);
        context.setProperty("subsEndPoint", subsEndpoint);

        jsonBody.getJSONObject("subscription").getJSONObject("callbackReference").put("notifyURL", subsEndpoint);

        List<OperatorEndpoint> endpoints = occi.getAPIEndpointsByApp(API_TYPE, executor.getSubResourcePath(),
                executor.getValidoperators(context),context);

        SubscriptionHubRequest subscriptionHubRequest = gson.fromJson(jsonBody.toString(),SubscriptionHubRequest.class);
        ShortCodes[] shortCodes = subscriptionHubRequest.getSubscription().getShortCodes();
        String originalClientCorrelator = subscriptionHubRequest.getSubscription().getClientCorrelator();

        SubscriptionGatewayRequest subscriptionGatewayRequest = new SubscriptionGatewayRequest();
        SubscriptionGatewayRequestDTO subscriptionGatewayRequestDTO = new SubscriptionGatewayRequestDTO();
        CallbackReference callbackReference = new CallbackReference();

        Map<String, OperatorEndpoint> operatorMap = new HashMap<String, OperatorEndpoint>();

        for (OperatorEndpoint endpoint : endpoints) {

            operatorMap.put(endpoint.getOperator(), endpoint);

        }

        // request creation
        subscriptionGatewayRequestDTO.setClientCorrelator(jsonBody.getJSONObject("subscription").getString("clientCorrelator"));

        if (jsonBody.getJSONObject("subscription").getJSONObject("callbackReference").has("callbackData")) {
            callbackReference.setCallbackData(jsonBody.getJSONObject("subscription").getJSONObject("callbackReference").getString("callbackData"));
        }

        callbackReference.setNotifyURL(subsEndpoint);
        subscriptionGatewayRequestDTO.setCallbackReference(callbackReference);

        for (ShortCodes shortCodesObj :shortCodes) {

            if (operatorMap.containsKey(shortCodesObj.getOperatorCode())) {

                OperatorEndpoint endpoint = operatorMap.get(shortCodesObj.getOperatorCode());

                shortCodesObj.setToAddress(endpoint.getEndpointref().getAddress());

                ussdService.updateOperatorIdBySubscriptionId(subscriptionId,endpoint.getOperator());

                log.info("sending endpoint found: " + endpoint.getEndpointref().getAddress() + " Request ID: " + UID.getRequestID(context));

                shortCodesObj.setAuthorizationHeader("Bearer " + executor.getAccessToken(endpoint.getOperator(), context));
                //SET OP_ID & OP_CODE
                shortCodesObj.setOperatorId(endpoint.getOperatorId());
                //shortCodesObj.setOperatorCode(endpoint.getOperator());
            } else {
                log.error("OperatorEndpoint not found. Operator Not Provisioned: " + shortCodesObj.getOperatorCode());
                shortCodesObj.setToAddress("Not Provisioned");
            }

        }

        subscriptionGatewayRequestDTO.setShortCodes(shortCodes);
        subscriptionGatewayRequestDTO.setClientCorrelator(originalClientCorrelator + ":" + requestid);

        subscriptionGatewayRequest.setSubscription(subscriptionGatewayRequestDTO);
        String requestStr = gson.toJson(subscriptionGatewayRequest);

        HandlerUtils.setHandlerProperty(context,this.getClass().getSimpleName());
        context.setProperty("responseResourceURL", mediatorConfMap.get("hubGateway")+executor.getApiContext()+ "/" + executor.getApiVersion() + executor.getSubResourcePath()+"/"+subscriptionId);
        context.setProperty("subscriptionID", subscriptionId);
        context.setProperty("original_clientCorrelator", originalClientCorrelator);

        JsonUtil.newJsonPayload(((Axis2MessageContext) context).getAxis2MessageContext(), requestStr, true, true);

        return true;
    }
}
