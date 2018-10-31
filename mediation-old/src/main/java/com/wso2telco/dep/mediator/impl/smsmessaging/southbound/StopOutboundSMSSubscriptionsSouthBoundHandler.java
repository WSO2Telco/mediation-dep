/*******************************************************************************
 * Copyright  (c) 2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 *
 * WSO2.Telco Inc. licences this file to you under  the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.dep.mediator.impl.smsmessaging.southbound;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.dep.mediator.ErrorConstants;
import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.entity.ussd.DeleteOperator;
import com.wso2telco.dep.mediator.entity.ussd.DeleteSubscriptionRequest;
import com.wso2telco.dep.mediator.entity.ussd.DeleteSubscriptionRequestDTO;
import com.wso2telco.dep.mediator.impl.smsmessaging.SMSExecutor;
import com.wso2telco.dep.mediator.impl.smsmessaging.SMSHandler;
import com.wso2telco.dep.mediator.impl.smsmessaging.StopOutboundSMSSubscriptionsHandler;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.service.SMSMessagingService;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.ValidateDNCancelSubscription;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.ValidateDNCancelSubscriptionPlugin;
import com.wso2telco.dep.operatorservice.model.OperatorEndPointDTO;
import com.wso2telco.dep.operatorservice.model.OperatorSubscriptionDTO;
import com.wso2telco.dep.operatorservice.service.OparatorService;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This handler stop subscriptions for delivery notifications on southbound scenario (gateway)
 */
public class StopOutboundSMSSubscriptionsSouthBoundHandler implements SMSHandler {

    /** The log. */
    private static Log log = LogFactory.getLog(StopOutboundSMSSubscriptionsHandler.class);

    /** The Constant API_TYPE. */
    private static final String API_TYPE = "smsmessaging";

    /** The occi. */
    private OriginatingCountryCalculatorIDD occi;

    /** The smsMessagingDAO. */
    private SMSMessagingService smsMessagingService;

    /** The executor. */
    private SMSExecutor executor;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    private List<OperatorEndPointDTO> operatorEndpoints;

    /**
     * Instantiates a new stop outbound sms subscriptions handler.
     *
     * @param executor
     *            the executor
     */
    public StopOutboundSMSSubscriptionsSouthBoundHandler(SMSExecutor executor) {

        this.executor = executor;
        occi = new OriginatingCountryCalculatorIDD();
        smsMessagingService = new SMSMessagingService();

        try {
            operatorEndpoints = new OparatorService().getOperatorEndpoints();
        } catch (BusinessException e) {
            log.warn("Error while retrieving operator endpoints", e);
        }
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
        if (httpMethod.equalsIgnoreCase("DELETE")) {
            String dnSubscriptionId = requestPath.substring(requestPath.lastIndexOf("/") + 1);
            String[] params = { dnSubscriptionId };

            String[] urlElements = requestPath.split("/");
            int elements = urlElements.length;
            if (elements == 5) {
                validator = new ValidateDNCancelSubscriptionPlugin();
                log.debug("Invoke validation - ValidateDNCancelSubscriptionPlugin");
            } else if (elements == 4) {
                validator = new ValidateDNCancelSubscription();
                log.debug("Invoke validation - ValidateDNCancelSubscription");
            } else {
                throw new Exception("requestPath not valid");
            }

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
        if (executor.getHttpMethod().equalsIgnoreCase("DELETE")) {
            return deleteSubscriptions(context);
        }
        return false;
    }

    /**
     * Delete subscriptions.
     *
     * @param context
     *            the context
     * @return true, if successful
     * @throws Exception
     *             the exception
     */
    private boolean deleteSubscriptions(MessageContext context) throws Exception {
        UID.getUniqueID(Type.DELRETSUB.getCode(), context, executor.getApplicationid());

        String requestPath = executor.getSubResourcePath();
        String subId = requestPath.substring(requestPath.lastIndexOf("/") + 1);

        Integer dnSubscriptionId;
        try{
            dnSubscriptionId = Integer.parseInt(subId.replaceFirst("sub", ""));
        }
        catch (NumberFormatException ex){
            throw new CustomException(MSISDNConstants.SVC0002, "", new String[] {ErrorConstants.INVALID_SUBSCRIPTION_ID});
        }

        List<OperatorSubscriptionDTO> domainSubs = (smsMessagingService.outboudSubscriptionQuery(Integer.valueOf(dnSubscriptionId)));

        if (domainSubs != null && !domainSubs.isEmpty()) {

            // If operator list also added as the payload, to be used in HUB
            List<DeleteOperator> deleteOperators = new ArrayList<DeleteOperator>();

            for (OperatorSubscriptionDTO operatorSubscriptionDTO : domainSubs) {
                OperatorEndPointDTO endPointDTO = getValidEndpoints(API_TYPE, operatorSubscriptionDTO.getOperator());
                if (endPointDTO != null)
                    operatorSubscriptionDTO.setOperatorId(endPointDTO.getOperatorid());
                else
                    log.warn("Valid endpoint is empty: " + operatorSubscriptionDTO.getOperator());
            }

            for (OperatorSubscriptionDTO domainSub : domainSubs) {
                deleteOperators.add(new DeleteOperator(
                        domainSub.getOperator(),
                        domainSub.getDomain(),
                        "Bearer " + executor.getAccessToken(domainSub.getOperator(), context),
                        domainSub.getOperatorId())
                );
            }

            DeleteSubscriptionRequest deleteSubscriptionRequest = new DeleteSubscriptionRequest(new DeleteSubscriptionRequestDTO(deleteOperators));

            String payload = gson.toJson(deleteSubscriptionRequest);

            JsonUtil.newJsonPayload(((Axis2MessageContext) context).getAxis2MessageContext(), payload, true, true);

            // First operator is taken into variables to be used in GW
            OperatorSubscriptionDTO sub = domainSubs.get(0);
            HandlerUtils.setHandlerProperty(context, this.getClass().getSimpleName());
            HandlerUtils.setEndpointProperty(context, sub.getDomain());
            HandlerUtils.setAuthorizationHeader(context, executor,
                    new OperatorEndpoint(new EndpointReference(sub.getDomain()), sub.getOperator()));
            context.setProperty("subscriptionId", dnSubscriptionId);
        } else {
            throw new CustomException("POL0001", "",new String[] { "SMS Receipt Subscription Not Found: " + dnSubscriptionId });
        }

        return true;
    }

    /**
     * Gets the valid endpoints.
     *
     * @param api             the api
     * @param validoperator   the validoperator
     * @return the valid endpoints
     */
    private OperatorEndPointDTO getValidEndpoints(String api, final String validoperator) {

        OperatorEndPointDTO validoperendpoint = null;

        for (OperatorEndPointDTO d : operatorEndpoints) {
            if ((d.getApi().equalsIgnoreCase(api)) && (validoperator.equalsIgnoreCase(d.getOperatorcode()) ) ) {
                validoperendpoint = d;
                break;
            }
        }

        return validoperendpoint;
    }
}
