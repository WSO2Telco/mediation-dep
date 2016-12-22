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
package com.wso2telco.dep.mediator.impl.smsmessaging.southbound;


import com.wso2telco.dep.mediator.OperatorEndpoint;
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
import com.wso2telco.dep.operatorservice.model.OperatorSubscriptionDTO;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

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
        String requestPath = executor.getSubResourcePath();
        String subid = requestPath.substring(requestPath.lastIndexOf("/") + 1);

        String requestid = UID.getUniqueID(Type.DELRETSUB.getCode(), context, executor.getApplicationid());
        Integer dnSubscriptionId = Integer.parseInt(subid.replaceFirst("sub", ""));
        List<OperatorSubscriptionDTO> domainsubs = (smsMessagingService
                .outboudSubscriptionQuery(Integer.valueOf(dnSubscriptionId)));
        if (domainsubs.isEmpty()) {

            throw new CustomException("POL0001", "",new String[] { "SMS Receipt Subscription Not Found: " + dnSubscriptionId });
        }
        OperatorSubscriptionDTO sub = domainsubs.get(0);
        if (domainsubs.size() > 1) {
            log.warn("Multiple operators found for sbscription. Picking first endpoint: " + sub.getDomain()
                    + " for operator: " + sub.getOperator() + " to send delete request.");
        }
        HandlerUtils.setHandlerProperty(context, this.getClass().getSimpleName());
        HandlerUtils.setEndpointProperty(context, sub.getDomain());
        HandlerUtils.setAuthorizationHeader(context, executor,
                new OperatorEndpoint(new EndpointReference(sub.getDomain()), sub.getOperator()));

        context.setProperty("dnSubscriptionId",dnSubscriptionId);

        return true;
    }
}
