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
package com.wso2telco.dep.mediator.entity;

import com.wso2telco.dep.mediator.util.APIType;
import com.wso2telco.dep.operatorservice.model.OperatorApplicationDTO;
import org.apache.synapse.MessageContext;

import java.io.Serializable;
import java.util.List;

public class OparatorEndPointSearchDTO implements Serializable {
	
		/**
		 * 
		 */
		private static final long serialVersionUID = -2314179270730291269L;
	
		private APIType api;
		private String apiName;
		private String requestPathURL;
		private boolean isredirect;
		private List<OperatorApplicationDTO> operators;
		private MessageContext context;
		private String MSISDN ;
		
		
		public String getApiName() {
			return apiName;
		}
		
		public void setApiName(String apiName) {
			this.apiName = apiName;
		}
	
		public String getMSISDN() {
			return MSISDN;
		}
	
		public void setMSISDN(String mSISDN) {
			MSISDN = mSISDN;
		}
	
		public MessageContext getContext() {
			return context;
		}
	
		public void setContext(MessageContext context) {
			this.context = context;
		}
	
	
		public APIType getApiType() {
			return api;
		}
	
		public void setApi(APIType api) {
			this.api = api;
		}
	
		public String getRequestPathURL() {
			return requestPathURL;
		}
	
		public void setRequestPathURL(String requestPathURL) {
		this.requestPathURL = requestPathURL;
		}
		public boolean isIsredirect() {
			return isredirect;
		}
	
		public void setIsredirect(boolean isredirect) {
			this.isredirect = isredirect;
		}
	
		public List<OperatorApplicationDTO> getOperators() {
			return operators;
		}
	
		public void setOperators(List<OperatorApplicationDTO> operators) {
			this.operators = operators;
		}
	
	}

