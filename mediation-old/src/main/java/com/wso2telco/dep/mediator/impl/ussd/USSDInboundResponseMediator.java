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
package com.wso2telco.dep.mediator.impl.ussd;

import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.service.USSDService;
import com.wso2telco.dep.mediator.util.FileNames;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.mediator.util.OperatorAccessToken;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.JSONObject;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * A class mediator to handle response from SP in USSD inbound flow
 */
public class USSDInboundResponseMediator extends AbstractMediator {

    private USSDService ussdService;

    @Override
    public boolean mediate(MessageContext messageContext) {
        FileReader fileReader = new FileReader();
        String file = CarbonUtils.getCarbonConfigDirPath() + File.separator
                + FileNames.MEDIATOR_CONF_FILE.getFileName();
        Map<String, String> mediatorConfMap = fileReader.readPropertyFile(file);
        String requestPath = (String) messageContext.getProperty("REST_SUB_REQUEST_PATH");
        String subscriptionId = requestPath.substring(requestPath.lastIndexOf("/") + 1);
        ussdService = new USSDService();
        try {
            List<String> ussdSPDetails = ussdService.getUSSDNotify(Integer.valueOf(subscriptionId));
            String jsonPayloadToString = JsonUtil
                    .jsonPayloadToString(((Axis2MessageContext) messageContext)
                            .getAxis2MessageContext());
            JSONObject jsonBody = new JSONObject(jsonPayloadToString);
            if (jsonBody == null) {
                throw new CustomException("POL0299", "", new String[]{"Error invoking Endpoint"});
            }
            String action = jsonBody.getJSONObject("outboundUSSDMessageRequest").getString("ussdAction");
            String subsEndpoint = mediatorConfMap.get("ussdGatewayEndpoint") + subscriptionId;
            messageContext.setProperty("subsEndPoint", subsEndpoint);

            if (action.equalsIgnoreCase("mtcont")) {
                log.info("Subsendpoint - " + subsEndpoint + " Request ID: " + UID.getRequestID(messageContext));
            }

            if (action.equalsIgnoreCase("mtfin")) {
                log.info("Subsendpoint - " + subsEndpoint + " Request ID: " + UID.getRequestID(messageContext));
                boolean deleted = ussdService.ussdEntryDelete(Integer.valueOf(subscriptionId));
                log.info("Entry deleted " + deleted + " Request ID: " + UID.getRequestID(messageContext));

            }

            if (action.equalsIgnoreCase("mocont")) {
                log.info("Subsendpoint - " + subsEndpoint + " Request ID: " + UID.getRequestID(messageContext));
            }

            if (action.equalsIgnoreCase("mofin")) {
                log.info("Subsendpoint - " + subsEndpoint + " Request ID: " + UID.getRequestID(messageContext));
            }

            OperatorEndpoint endpoint = new OperatorEndpoint(new EndpointReference(ussdSPDetails.get(0)), null);
            String sending_add = endpoint.getEndpointref().getAddress();
            log.info("sending_add=" + sending_add);
            HandlerUtils.setEndpointProperty(messageContext, sending_add);
            Object headers = ((Axis2MessageContext) messageContext).getAxis2MessageContext()
                    .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers != null && headers instanceof Map) {
                Map headersMap = (Map) headers;
                headersMap.put("Authorization", "Bearer " + OperatorAccessToken.getAccessToken(endpoint
                        .getOperator(), messageContext));
            }
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty("HTTP_SC", 201);
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty("messageType", "application/json");
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty("ContentType", "application/json");
            //executor.setResponse(context,jsonBody.toString());
            /*String transformedJson = jsonBody.toString();
            JsonUtil.newJsonPayload(
                    ((Axis2MessageContext) messageContext).getAxis2MessageContext(),
                    transformedJson, true, true);*/

            //System.out.println("Transformed body:\n" + transformedJson);

        } catch (Exception e) {
            log.error("Exception while handling ussd inbound response", e);
        }
        //JSONObject jsonBody = executor.getJsonBody();
        return true;
    }
}
