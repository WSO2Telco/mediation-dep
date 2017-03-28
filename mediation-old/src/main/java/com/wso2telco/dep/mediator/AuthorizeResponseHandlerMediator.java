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

package com.wso2telco.dep.mediator;

import com.wso2telco.dep.mediator.publisher.PublishFactory;
import com.wso2telco.dep.mediator.util.DataPublisherConstants;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;

public class AuthorizeResponseHandlerMediator extends AbstractMediator {


    /* (non-Javadoc)
     * @see org.apache.synapse.Mediator#mediate(org.apache.synapse.MessageContext)
     */
    public boolean mediate(MessageContext context) {

        String requestStr = JsonUtil.jsonPayloadToString(((Axis2MessageContext) context)
                .getAxis2MessageContext());
       publishResponseData(requestStr, context);

        return true;
    }


    /**
     * Publish response data.
     *

     * @param retStr
     *            the ret str
     * @param messageContext
     *            the message context
     */
    private void publishResponseData(String retStr, MessageContext messageContext) {
        // set properties for response data publisher

        messageContext.setProperty(DataPublisherConstants.MSISDN,
                messageContext.getProperty(MSISDNConstants.USER_MSISDN));

        boolean isPaymentReq = false;
        String paymentType=null;
        JSONObject paymentRes = null;

        if (retStr != null && !retStr.isEmpty()) {
            // get serverReferenceCode property for payment API response

            // get exception property for exception response
            JSONObject exception = null;
            JSONObject response = null;
            try {
                //JSONObject response = new JSONObject(retStr);
                response  = new JSONObject(retStr);
                paymentRes = response.optJSONObject("amountTransaction");
                messageContext.setProperty(DataPublisherConstants.CHARGE_AMOUNT,paymentRes.optJSONObject("paymentAmount").optJSONObject("chargingInformation").opt("amount"));
                
                if (paymentRes != null) {
                    if (paymentRes.has("serverReferenceCode")) {
                        messageContext.setProperty(DataPublisherConstants.OPERATOR_REF,
                                paymentRes.optString("serverReferenceCode"));
                    } else if (paymentRes.has("originalServerReferenceCode")) {
                        messageContext.setProperty(DataPublisherConstants.OPERATOR_REF,
                                paymentRes.optString("originalServerReferenceCode"));
                    }
                    isPaymentReq = true;
                }

                exception = response.optJSONObject("requestError");
                if (exception != null) {
                    JSONObject exception_body = exception.optJSONObject("serviceException");
                    if (exception_body == null) {
                        exception_body = exception.optJSONObject("policyException");
                    }

                    if (exception_body != null) {
                        log.info("exception id: " + exception_body.optString("messageId"));
                        log.info("exception message: " + exception_body.optString("text"));
                        messageContext.setProperty(DataPublisherConstants.EXCEPTION_ID,
                                exception_body.optString("messageId"));
                        messageContext.setProperty(DataPublisherConstants.EXCEPTION_MESSAGE,
                                exception_body.optString("text"));
                        messageContext.setProperty(DataPublisherConstants.RESPONSE, "1");
                    }
                }
            } catch (JSONException e) {
                log.error("Error in converting response to json. " + e.getMessage(), e);
            }
        }

        if ( isPaymentReq ) {
            log.debug("Publish to CEP");
            try {
                PublishFactory.getPublishable(paymentRes).publish(messageContext, paymentRes);
            } catch (Exception e) {
                log.error("ERROR occured at data publishing to CEP ", e);
            }
        }

    }


}
