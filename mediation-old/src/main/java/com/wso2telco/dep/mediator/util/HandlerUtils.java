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
package com.wso2telco.dep.mediator.util;

import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.RequestExecutor;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.Map;

/**
 * Utility class to set custom properties inside custom handlers
 */
public class HandlerUtils {

    /**
     * Set the custom property "HANDLER"
     * @param messageContext synapse message context
     * @param className custom handler class name
     */
    public static void setHandlerProperty(MessageContext messageContext, String className) {
        messageContext.setProperty("HANDLER", className);
    }

    /**
     * Set the ENDPOINT property
     * @param messageContext synapse message context
     * @param sendingAddress the endpoint to be invoked
     */
    public static void setEndpointProperty(MessageContext messageContext, String sendingAddress) {
        messageContext.setProperty("ENDPOINT", sendingAddress);
    }

    /**
     * Set Authorization header
     * @param messageContext synapse message context
     * @param executor custom executor instance
     * @param operatorEndpoint
     * @throws Exception operator endpoint
     */
    public static void setAuthorizationHeader(MessageContext messageContext, RequestExecutor executor, OperatorEndpoint
            operatorEndpoint) throws Exception {
        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();
        Object headers = axis2MessageContext
                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers != null && headers instanceof Map) {
            Map headersMap = (Map) headers;
            try {
                headersMap.put("Authorization", "Bearer " + executor.getAccessToken(operatorEndpoint
                        .getOperator(), messageContext));
            } catch (Exception e) {
                throw new Exception("Exception while setting Authorization header", e);
            }
        }
    }

    /**
     * Set the gateway host to the message context.
     *
     * @param messageContext the message context for which the gateway host is set
     */
    public static void setGatewayHost(MessageContext messageContext){
        FileReader fileReader = new FileReader();
        String file = CarbonUtils.getCarbonConfigDirPath() + File.separator + FileNames.MEDIATOR_CONF_FILE.getFileName();
        Map<String, String> mediatorConfMap = fileReader.readPropertyFile(file);
        messageContext.setProperty("hubGateway", mediatorConfMap.get("hubGateway"));
    }
}
