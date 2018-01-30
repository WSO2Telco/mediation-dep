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
package com.wso2telco.dep.mediator.impl.smsmessaging;

import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.internal.ApiUtils;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.mediationrule.OriginatingCountryCalculatorIDD;
import com.wso2telco.dep.mediator.util.FileNames;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.ValidateRetrieveSms;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: Auto-generated Javadoc

/**
 * The Class RetrieveSMSHandler.
 */
public class RetrieveSMSHandler implements SMSHandler {

	/** The log. */
	private static Log log = LogFactory.getLog(RetrieveSMSHandler.class);

	/** The Constant API_TYPE. */
	protected static final String API_TYPE = "smsmessaging";

	/** The occi. */
	protected OriginatingCountryCalculatorIDD occi;

	/** The api util. */
	protected ApiUtils apiUtil;

	/** The executor. */
	protected SMSExecutor executor;

	/**
	 * Instantiates a new retrieve sms handler.
	 *
	 * @param executor
	 *            the executor
	 */
	public RetrieveSMSHandler(SMSExecutor executor) {
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

		String requestid = UID.getUniqueID(Type.SMSRETRIVE.getCode(), context, executor.getApplicationid());

		List<OperatorEndpoint> endpoints = occi.getAPIEndpointsByApp( API_TYPE, executor.getSubResourcePath(), executor.getValidoperators(context));
		String endpoint = endpoints.get(0).getEndpointref().getAddress();

		URL retrieveURL = new URL("http://example.com/smsmessaging/v1" + executor.getSubResourcePath());
		String urlQuery = retrieveURL.getQuery();

		if (urlQuery != null) {
			if (urlQuery.contains("maxBatchSize")) {
				String queryParts[] = urlQuery.split("=");
				if (queryParts.length > 1) {
					if (Integer.parseInt(queryParts[1]) > 100) {
						// batchSize exceeds allowed limit, set it to 100
						endpoint = modifyBatchSize(endpoint, "100");
					}
				}
			}
		} else {
			// no batchSize found, set it to 10
			endpoint = setBatchSize(endpoint, "10");
		}

		FileReader fileReader = new FileReader();
		String file = CarbonUtils.getCarbonConfigDirPath() + File.separator + FileNames.MEDIATOR_CONF_FILE.getFileName();
		Map<String, String> mediatorConfMap = fileReader.readPropertyFile(file);

		// use the first endpoint only
		HandlerUtils.setAuthorizationHeader(context, executor, endpoints.get(0));
		HandlerUtils.setEndpointProperty(context, endpoint);
		HandlerUtils.setHandlerProperty(context, this.getClass().getSimpleName());
		// if specific resource URL prefix for retrieve SMS is set in the mediator-conf.properties file, use that
		String smsRetrieveResourceUrlPrefix = mediatorConfMap.get("smsRetrieveResourceUrlPrefix");
		if (smsRetrieveResourceUrlPrefix != null && !smsRetrieveResourceUrlPrefix.isEmpty()) {
			smsRetrieveResourceUrlPrefix = smsRetrieveResourceUrlPrefix.endsWith("/") ?
					smsRetrieveResourceUrlPrefix.substring(0, smsRetrieveResourceUrlPrefix.length() - 1) :  smsRetrieveResourceUrlPrefix;
			context.setProperty("SMS_RETRIEVE_GATEWAY_RESOURCE_URL_PREFIX", smsRetrieveResourceUrlPrefix);
		} else {
			// if specific resource URL prefix for retrieve SMS is not found, use generic hub ur prefix
			context.setProperty("GATEWAY_RESOURCE_URL_PREFIX", mediatorConfMap.get("hubGateway"));
		}
		context.setProperty("REQUEST_ID", requestid);

		return true;
	}

	/**
	 * modifies the 'batchSize' query parameter
	 *
	 * @param endpoint endpoint url
	 * @param batchSize batch size to set
     * @return endpoint with batchSize query parameter modified
     */
	private static String modifyBatchSize(String endpoint, String batchSize) {

		String regex = "(?i)(?<=maxBatchSize)=([^&#]*)";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(endpoint);

		if (m.find()) {
			return m.replaceAll("=" + batchSize);
		} else {
			return setBatchSize(endpoint, batchSize);
		}
	}

	/**
	 * set the 'batchSize' query parameter
	 *
	 * @param endpoint endpoint url
	 * @param batchSize batch size to set
	 * @return endpoint with batchSize query parameter appended
     */
	private static String setBatchSize (String endpoint, String batchSize) {
		return endpoint + ((endpoint.indexOf("?") == -1 ? "?" : "&") + "maxBatchSize=" + batchSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.wso2telco.mediator.impl.sms.SMSHandler#validate(java.lang.String,
	 * java.lang.String, org.json.JSONObject, org.apache.synapse.MessageContext)
	 */
	@Override
	public boolean validate(String httpMethod, String requestPath, JSONObject jsonBody, MessageContext context) throws Exception {
		if (!httpMethod.equalsIgnoreCase("GET")) {
			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			throw new Exception("Method not allowed");
		}

		IServiceValidate validator;
		String appID = apiUtil.getAppID(context, "retrive_sms");
		String[] params = { appID, "" };
		validator = new ValidateRetrieveSms();
		validator.validateUrl(requestPath);
		validator.validate(params);
		return true;
	}
}
