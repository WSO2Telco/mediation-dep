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
package com.wso2telco.dep.mediator.impl;

import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.RequestExecutor;
import com.wso2telco.dep.mediator.internal.ResourceURLUtil;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.util.DataPublisherConstants;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.mediator.util.ValidationUtils;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.impl.location.ValidateLocation;
import com.wso2telco.dep.subscriptionvalidator.util.ValidatorUtils;
import com.wso2telco.dep.mediator.entity.OparatorEndPointSearchDTO;
import com.wso2telco.dep.mediator.util.APIType;

import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

// TODO: Auto-generated Javadoc

/**
 * The Class LocationExecutor.
 */
public class LocationExecutor extends RequestExecutor {

    /** The occi. */
    private OriginatingCountryCalculatorIDD occi;

    ValidateLocation validator;
    /**
     * Instantiates a new location executor.
     */
    public LocationExecutor() {
        occi = new OriginatingCountryCalculatorIDD();
        validator = new ValidateLocation();
    }

    /* (non-Javadoc)
     * @see com.wso2telco.mediator.RequestExecutor#execute(org.apache.synapse.MessageContext)
     */
    @Override
    public boolean execute(MessageContext context) throws CustomException, AxisFault, Exception {

    	String[] addresses = validator.getMsisdns();
    	context.setProperty(MSISDNConstants.MSISDN, addresses[0]);
    	context.setProperty(MSISDNConstants.USER_MSISDN, ValidationUtils.getUserMsisdns(addresses)[0]);
        OperatorEndpoint endpoint = null;
		if (ValidatorUtils.getValidatorForSubscriptionFromMessageContext(context).validate(
				context)) {
            OparatorEndPointSearchDTO searchDTO = new OparatorEndPointSearchDTO();
            searchDTO.setApi(APIType.PAYMENT);
            searchDTO.setApiName((String) context.getProperty("API_NAME"));
            searchDTO.setContext(context);
            searchDTO.setIsredirect(true);
            searchDTO.setMSISDN(ValidationUtils.getQueryMsisdns(addresses)[0]);
            searchDTO.setOperators(getValidoperators(context));
            searchDTO.setRequestPathURL(getSubResourcePath());

            endpoint = occi.getOperatorEndpoint(searchDTO);
		}
		
        String sending_add = endpoint.getEndpointref().getAddress();
		sending_add += getSubResourcePath();
        
        HandlerUtils.setHandlerProperty(context,this.getClass().getSimpleName());
		HandlerUtils.setEndpointProperty(context,sending_add);
		HandlerUtils.setAuthorizationHeader(context,this,endpoint);

        context.setProperty("operator", endpoint.getOperator());

        context.setProperty("OPERATOR_NAME", endpoint.getOperator());
        context.setProperty("OPERATOR_ID", endpoint.getOperatorId());

		((Axis2MessageContext) context).getAxis2MessageContext().setProperty("messageType", "application/json");
		
        return true;
    }

    /* (non-Javadoc)
     * @see com.wso2telco.mediator.RequestExecutor#validateRequest(java.lang.String, java.lang.String, org.json.JSONObject, org.apache.synapse.MessageContext)
     */
    @Override
    public boolean validateRequest(String httpMethod, String requestPath, JSONObject jsonBody, MessageContext context) throws Exception {
        
        context.setProperty(DataPublisherConstants.OPERATION_TYPE, 300);
        
        if (!httpMethod.equalsIgnoreCase("GET")) {
            ((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
            throw new Exception("Method not allowed");
        }
        String[] params = new ResourceURLUtil().getParamPairs(requestPath);
        validator.validateUrl(requestPath);
        validator.validate(params);

        return true;
    }

	
}
