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
package com.wso2telco.dep.mediator.impl.smsmessaging.northbound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.InboundSMSMessage;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.InboundSMSMessageList;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.NorthboundRetrieveRequest;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.NorthboundRetrieveResponse;
import com.wso2telco.dep.mediator.entity.smsmessaging.northbound.Registrations;
import com.wso2telco.dep.mediator.impl.smsmessaging.SMSExecutor;
import com.wso2telco.dep.mediator.impl.smsmessaging.SMSHandler;
import com.wso2telco.dep.mediator.internal.APICall;
import com.wso2telco.dep.mediator.internal.ApiUtils;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.util.FileNames;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.northbound.ValidateNBRetrieveSms;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


// TODO: Auto-generated Javadoc

/**
 * The Class NBRetrieveSMSHandler.
 */
public class RetrieveSMSNorthboundHandler implements SMSHandler {

	/** The log. */
private static Log log = LogFactory
		.getLog(RetrieveSMSNorthboundHandler.class);

/** The Constant API_TYPE. */
private static final String API_TYPE = "smsmessaging";

/** The occi. */
private OriginatingCountryCalculatorIDD occi;

/** The api util. */
private ApiUtils apiUtil;

/** The executor. */
private SMSExecutor executor;

/**
 * Instantiates a new NB retrieve sms handler.
 *
 * @param executor
 *            the executor
 */
public RetrieveSMSNorthboundHandler(SMSExecutor executor) {
	this.executor = executor;
	occi = new OriginatingCountryCalculatorIDD();
	apiUtil = new ApiUtils();
}

/*
 * (non-Javadoc)
 * 
 * @see
 * com.wso2telco.mediator.impl.sms.SMSHandler#handle(org.apache.synapse.
 * MessageContext)
 */
@Override
public boolean handle(MessageContext context) throws CustomException, AxisFault, Exception {

	SOAPBody body = context.getEnvelope().getBody();
	Gson gson = new GsonBuilder().serializeNulls().create();

	String reqType = "retrive_sms";
	String requestid = UID.getUniqueID(Type.SMSRETRIVE.getCode(), context,
	                                   executor.getApplicationid());

	int batchSize = 100;

	JSONObject jsonBody = executor.getJsonBody();
	NorthboundRetrieveRequest nbRetrieveRequest = gson.fromJson(
			jsonBody.toString(), NorthboundRetrieveRequest.class);
	log.info("-------------------------------------- Retrieve messages sent to your Web application --------------------------------------"
	         + " Request ID: " + UID.getRequestID(context));
	log.info("Retrieve northbound request body : " + gson.toJson(nbRetrieveRequest)
	         + " Request ID: " + UID.getRequestID(context));


	List<OperatorEndpoint> endpoints = occi.getAPIEndpointsByApp(API_TYPE,
			executor.getSubResourcePath(), executor.getValidoperators());

	List<OperatorEndpoint> validEndpoints = new ArrayList<OperatorEndpoint>();
	Registrations[] registrations = nbRetrieveRequest
			.getInboundSMSMessages().getRegistrations();

	if (nbRetrieveRequest.getInboundSMSMessages().getMaxBatchSize() != null) {
		String requestBodyBatchSize = nbRetrieveRequest
				.getInboundSMSMessages().getMaxBatchSize();

		if (!requestBodyBatchSize.equals("")) {
			if (Integer.parseInt(requestBodyBatchSize) < 100) {
				batchSize = Integer.parseInt(requestBodyBatchSize);
			}
		}
	}

     int perOpCoLimit = batchSize / (endpoints.size());
     String getRequestURL = null;
     String criteria= null;
     String operatorCode=null;

	for (OperatorEndpoint operatorEndpoint : endpoints) {

		for (int i = 0; i < registrations.length; i++) {
			if (registrations[i].getOperatorCode().equalsIgnoreCase(operatorEndpoint.getOperator())) {
				validEndpoints.add(operatorEndpoint);

					if (registrations[i].getCriteria() != null) {
						criteria = registrations[i].getCriteria();
					}
                String url = operatorEndpoint.getEndpointref().getAddress();

					if (criteria == null || criteria.equals("")) {
						operatorCode = registrations[i].getOperatorCode();
						log.info("Operator RetrieveSMSHandler"+operatorCode);
						getRequestURL = "/"+ registrations[i].getRegistrationID()+ "/messages?maxBatchSize=" + batchSize;
						url = url.replace("/messages", getRequestURL);
						log.info("Invoke RetrieveSMSHandler of plugin");
					} else {
						operatorCode = registrations[i].getOperatorCode();
						log.info("Operator RetrieveSMSHandler"+operatorCode);
						getRequestURL = "/"+ registrations[i].getRegistrationID()+ "/" + criteria+ "/messages?maxBatchSize=" + batchSize;
						url = url.replace("/messages", getRequestURL);
						log.info("Invoke SBRetrieveSMSHandler of plugin");
					}

                registrations[i].setToAddress(url);
                registrations[i].setAuthorizationHeader("Bearer " + executor.getAccessToken(operatorEndpoint.getOperator(), context));
                registrations[i].setBatchSize(perOpCoLimit);
                registrations[i].setOperatorId(operatorEndpoint.getOperatorId());
				break;
			}
		}
	}

    String requestStr = gson.toJson(nbRetrieveRequest);
    HandlerUtils.setHandlerProperty(context, this.getClass().getSimpleName());
    JsonUtil.newJsonPayload(((Axis2MessageContext) context).getAxis2MessageContext(), requestStr, true, true);

	return true;
}

/*
 * (non-Javadoc)
 * 
 * @see
 * com.wso2telco.mediator.impl.sms.SMSHandler#validate(java.lang.String,
 * java.lang.String, org.json.JSONObject, org.apache.synapse.MessageContext)
 */
@Override
public boolean validate(String httpMethod, String requestPath,
		JSONObject jsonBody, MessageContext context) throws Exception {
	if (!httpMethod.equalsIgnoreCase("POST")) {
		((Axis2MessageContext) context).getAxis2MessageContext()
		                               .setProperty("HTTP_SC", 405);
		throw new Exception("Method not allowed");
	}

	if (httpMethod.equalsIgnoreCase("POST")) {
		IServiceValidate validator;
		validator = new ValidateNBRetrieveSms();
		validator.validateUrl(requestPath);
		validator.validate(jsonBody.toString());
	}

	return true;
}
}
