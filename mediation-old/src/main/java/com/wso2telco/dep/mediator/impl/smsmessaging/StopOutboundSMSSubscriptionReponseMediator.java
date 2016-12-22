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

import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.dep.mediator.service.SMSMessagingService;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * This class mediator is responsible for deleting outbound subscriptions
 */
public class StopOutboundSMSSubscriptionReponseMediator extends AbstractMediator {

    /**
     * The smsMessagingDAO.
     */
    private SMSMessagingService smsMessagingService;

    @Override
    public boolean mediate(MessageContext messageContext) {

        smsMessagingService = new SMSMessagingService();
        //synapse property
        Integer dnSubscriptionId = (Integer) messageContext.getProperty("SUBSCRIPTION_ID");
        try {
            smsMessagingService.outboundSubscriptionDelete(Integer.valueOf(dnSubscriptionId));
        } catch (BusinessException e) {
            log.error("Error while deleting the subscription id " + dnSubscriptionId, e);
        }
        return true;
    }
}
