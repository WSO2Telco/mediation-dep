/*******************************************************************************
 * Copyright  (c) 2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 *
 * WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
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

package com.wso2telco.dep.mediator.impl.smsmessaging;

import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.dep.mediator.service.SMSMessagingService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class SendSMSRequestIdPersistMediator extends AbstractMediator {

    private Log log = LogFactory.getLog(this.getClass());

    @Override
    public boolean mediate(MessageContext messageContext) {

        // create JSON object out of 'ADDRESSES' property
        JSONArray addressArray;
        try {
            addressArray = new JSONArray(messageContext.getProperty("ADDRESSES"));
        } catch (JSONException e) {
            log.error("Error in creating json array out of: " + messageContext.getProperty("ADDRESSES"), e);
            return false;
        }

        String requestId = (String) messageContext.getProperty("REQUEST_ID");
        String senderAddress = (String) messageContext.getProperty("SENDER_ADDRESS");
        String requestIdFromOperatorResponse = (String) messageContext.getProperty("SEND_SMS_OPERATOR_REQUEST_ID");

        // validate
        if (!assertNotNull(requestId)) {
            log.error("Validation failure for REQUEST_ID, value: " + requestId);
            return false;
        }
        if (!assertNotNull(senderAddress)) {
            log.error("Validation failure for SENDER_ADDRESS, value: " + senderAddress);
            return false;
        }
        if (!assertNotNull(requestIdFromOperatorResponse)) {
            log.error("Validation failure for SEND_SMS_OPERATOR_REQUEST_ID, value: " + requestIdFromOperatorResponse);
            return false;
        }

        Map<String, String> addressToOperatorRequestIdMap = new HashMap<String, String>(addressArray.length());
        for (int i = 0 ; i < addressArray.length() ; i++) {
            String address;
            try {
                address = addressArray.getString(i);
            } catch (JSONException e) {
                log.error("Error in retrieving address element from JSONArray", e);
                return false;
            }
            addressToOperatorRequestIdMap.put(address, requestIdFromOperatorResponse);
        }

        try {
            new SMSMessagingService().insertSMSRequestIds(requestId, senderAddress, addressToOperatorRequestIdMap);
            addressToOperatorRequestIdMap.clear();
        } catch (BusinessException e) {
            log.error("Error inserting request ids for send SMS operation", e);
            return false;
        }

        return true;
    }

    private static boolean assertNotNull (String aString) {
       return aString != null && !aString.isEmpty();
    }

    public boolean isContentAware() {
        return false;
    }
}
