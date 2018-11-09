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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.mediator.ErrorConstants;
import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.mediator.OperatorEndpoint;
import com.wso2telco.dep.mediator.entity.ussd.DeleteOperator;
import com.wso2telco.dep.mediator.entity.ussd.DeleteSubscriptionRequest;
import com.wso2telco.dep.mediator.entity.ussd.DeleteSubscriptionRequestDTO;
import com.wso2telco.dep.mediator.internal.Type;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.service.SMSMessagingService;
import com.wso2telco.dep.mediator.util.DataPublisherConstants;
import com.wso2telco.dep.mediator.util.FileNames;
import com.wso2telco.dep.mediator.util.HandlerUtils;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.service.IServiceValidate;
import com.wso2telco.dep.oneapivalidation.service.impl.smsmessaging.ValidateCancelSubscription;
import com.wso2telco.dep.operatorservice.model.OperatorEndPointDTO;
import com.wso2telco.dep.operatorservice.model.OperatorSubscriptionDTO;
import com.wso2telco.dep.operatorservice.service.OparatorService;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StopInboundSMSSubscriptionsHandler implements SMSHandler {

	/** The log. */
	private static Log log = LogFactory.getLog(StopInboundSMSSubscriptionsHandler.class);

	/** The Constant API_TYPE. */
	private static final String API_TYPE = "smsmessaging";

	/** The smsMessagingDAO. */
	private SMSMessagingService smsMessagingService;

	/** The executor. */
	private SMSExecutor executor;

	/** Configuration file */
	private String file = CarbonUtils.getCarbonConfigDirPath() + File.separator + FileNames.MEDIATOR_CONF_FILE.getFileName();

	/** Configuration Map */
	private Map<String, String> mediatorConfMap;

	private Gson gson = new GsonBuilder().serializeNulls().create();

	private List<OperatorEndPointDTO> operatorEndpoints;

	public StopInboundSMSSubscriptionsHandler(SMSExecutor executor) {

		this.executor = executor;
		smsMessagingService = new SMSMessagingService();
		mediatorConfMap = new FileReader().readPropertyFile(file);

		try {
			operatorEndpoints = new OparatorService().getOperatorEndpoints();
		} catch (BusinessException e) {
			log.warn("Error while retrieving operator endpoints", e);
		}
	}

	/**
	 * Instantiates a new retrieve sms subscriptions handler.
	 *
	 */

	@Override
	public boolean validate(String httpMethod, String requestPath, JSONObject jsonBody, MessageContext context)
			throws Exception {

		if (!httpMethod.equalsIgnoreCase("DELETE")) {

			((Axis2MessageContext) context).getAxis2MessageContext().setProperty("HTTP_SC", 405);
			throw new Exception("Method not allowed");
		}

		context.setProperty(DataPublisherConstants.OPERATION_TYPE, 205);
		IServiceValidate validator;

		String moSubscriptionId = requestPath.substring(requestPath.lastIndexOf("/") + 1);
		String[] params = { moSubscriptionId };
		validator = new ValidateCancelSubscription();
		validator.validateUrl(requestPath);
		validator.validate(params);

		return true;
	}

	@Override
	public boolean handle(MessageContext context) throws Exception {
		UID.getUniqueID(Type.DELRETSUB.getCode(), context, executor.getApplicationid());

		String requestPath = executor.getSubResourcePath();
		String subId = requestPath.substring(requestPath.lastIndexOf("/") + 1);

		int moSubscriptionId;
		try{
			moSubscriptionId = Integer.parseInt(subId.replaceFirst("sub", ""));
		}
		catch (NumberFormatException ex){
			throw new CustomException(MSISDNConstants.SVC0002, "", new String[] {ErrorConstants.INVALID_SUBSCRIPTION_ID});
		}

		List<OperatorSubscriptionDTO> domainSubs = (smsMessagingService.subscriptionQuery(Integer.valueOf(moSubscriptionId)));

		for (OperatorSubscriptionDTO operatorSubscriptionDTO : domainSubs) {
			OperatorEndPointDTO endPointDTO = getValidEndpoints(API_TYPE, operatorSubscriptionDTO.getOperator());
			if (endPointDTO != null)
				operatorSubscriptionDTO.setOperatorId(endPointDTO.getOperatorid());
			else
				log.warn("Valid endpoint is empty: " + operatorSubscriptionDTO.getOperator());
		}

		if (domainSubs != null && !domainSubs.isEmpty()) {

			List<DeleteOperator> deleteOperators = new ArrayList<DeleteOperator>();

			for (OperatorSubscriptionDTO domainSub : domainSubs) {
				deleteOperators.add(new DeleteOperator(
						domainSub.getOperator(),
						domainSub.getDomain(),
						"Bearer " + executor.getAccessToken(domainSub.getOperator(), context),
						domainSub.getOperatorId())
				);
			}

			DeleteSubscriptionRequest deleteSubscriptionRequest = new DeleteSubscriptionRequest(new DeleteSubscriptionRequestDTO(deleteOperators));

			String payload = gson.toJson(deleteSubscriptionRequest);

			JsonUtil.newJsonPayload(((Axis2MessageContext) context).getAxis2MessageContext(), payload, true, true);

			//pick the first record since this is gateway
			OperatorSubscriptionDTO sub = domainSubs.get(0);

			HandlerUtils.setHandlerProperty(context, this.getClass().getSimpleName());
			HandlerUtils.setEndpointProperty(context, sub.getDomain());
			HandlerUtils.setAuthorizationHeader(context, executor,
					new OperatorEndpoint(new EndpointReference(sub.getDomain()), sub.getOperator()));
			context.setProperty("subscriptionId", moSubscriptionId);
			context.setProperty("responseResourceURL", mediatorConfMap.get("hubGateway") + executor.getApiContext()+ "/" + executor.getApiVersion() + executor.getSubResourcePath());
		} else {
			throw new CustomException("POL0001", "", new String[] { "SMS Receipt Subscription Not Found: " + moSubscriptionId });
		}

		return true;
	}

	/**
	 * Gets the valid endpoints.
	 *
	 * @param api             the api
	 * @param validoperator   the validoperator
	 * @return the valid endpoints
	 */
	private OperatorEndPointDTO getValidEndpoints(String api, final String validoperator) {

		OperatorEndPointDTO validoperendpoint = null;

		for (OperatorEndPointDTO d : operatorEndpoints) {
			if ((d.getApi().equalsIgnoreCase(api)) && (validoperator.equalsIgnoreCase(d.getOperatorcode()) ) ) {
				validoperendpoint = d;
				break;
			}
		}

		return validoperendpoint;
	}
}
