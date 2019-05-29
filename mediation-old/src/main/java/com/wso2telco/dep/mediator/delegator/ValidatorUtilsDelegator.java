package com.wso2telco.dep.mediator.delegator;

/**
 * Copyright (c) 2019, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 * <p>
 * WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.wso2telco.dep.subscriptionvalidator.exceptions.ValidatorException;
import com.wso2telco.dep.subscriptionvalidator.services.MifeValidator;
import com.wso2telco.dep.subscriptionvalidator.util.ValidatorUtils;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.apimgt.api.APIManagementException;

/**
 * Delegator class for {@link com.wso2telco.dep.subscriptionvalidator.util.ValidatorUtils}
 */
public class ValidatorUtilsDelegator {

    /**
     * @TODO This class need to remove after releasing the solution for static block initiation
     */

    private static final ValidatorUtilsDelegator INSTANCE = new ValidatorUtilsDelegator();

    private ValidatorUtilsDelegator() {
    }

    /**
     * @param mc
     * @return
     * @throws APIManagementException
     * @throws ValidatorException
     */
    public MifeValidator getValidatorForSubscription(MessageContext mc) throws APIManagementException, ValidatorException {
        return ValidatorUtils.getValidatorForSubscription(mc);
    }

    /**
     * @param applicationId
     * @param apiId
     * @return
     * @throws ValidatorException
     */
    public MifeValidator getValidatorForSubscription(int applicationId, int apiId) throws ValidatorException {
        return ValidatorUtils.getValidatorForSubscription(applicationId, apiId);
    }

    /**
     * @param mc
     * @return
     * @throws ValidatorException
     */
    public MifeValidator getValidatorForSubscriptionFromMessageContext(MessageContext mc) throws ValidatorException {
        return ValidatorUtils.getValidatorForSubscriptionFromMessageContext(mc);
    }

    public static ValidatorUtilsDelegator getInstance() {
        return INSTANCE;
    }


}
