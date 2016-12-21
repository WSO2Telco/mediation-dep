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
package com.wso2telco.dep.mediator.impl.smsmessaging.southbound;

import com.wso2telco.dep.mediator.impl.smsmessaging.RetrieveSMSHandler;
import com.wso2telco.dep.mediator.impl.smsmessaging.SMSExecutor;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.southbound.ValidateSBRetrieveSms;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

/**
 * The Class SBRetrieveSMSHandler.
 */
public class RetrieveSMSSouthboundHandler extends RetrieveSMSHandler {

	/**
	 * Instantiates a new SB retrieve sms handler.
	 *
	 * @param executor
	 *            the executor
	 */
	public RetrieveSMSSouthboundHandler(SMSExecutor executor) {
		super(executor);
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
		if (!httpMethod.equalsIgnoreCase("GET")) {
			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			throw new Exception("Method not allowed");
		}

		if (httpMethod.equalsIgnoreCase("GET")) {
			IServiceValidate validator;
			String urlParts = apiUtil.getAppID(context, "retrive_sms");
			String appID = "";
			String criteria = "";
			String[] param = urlParts.split("/");
			if (param.length == 2) {
				appID = param[0];
				criteria = param[1];
			}

			String[] params = { appID, criteria, "" };
			validator = new ValidateSBRetrieveSms();
			validator.validateUrl(requestPath);
			validator.validate(params);
		}

		return true;
	}
}
