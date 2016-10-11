/*******************************************************************************
 * Copyright  (c) 2015-2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 * 
 *  WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
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
package com.wso2telco.refund.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

import com.wso2telco.refund.api.RefundService;
import com.wso2telco.refund.exception.RefundException;
import org.apache.commons.lang.StringEscapeUtils;

import com.google.gson.Gson;	
import com.wso2telco.refund.pojo.RefundRequest;
import com.wso2telco.refund.utils.ApiInfoDao;
import com.wso2telco.refund.utils.AxataDBUtilException;
import com.wso2telco.refund.utils.DbUtils;
import org.apache.log4j.Logger;

// TODO: Auto-generated Javadoc
/**
 * The Class CreateService.
 * @author Yasith Lokuge	
 */
public  class CreateService extends ApiInfoDao {

	final static Logger logger = Logger.getLogger(CreateService.class);

	/**
	 * Creates the entry.
	 *
	 * @param jsonBody the json body
	 * @throws SQLException the SQL exception
	 * @throws AxataDBUtilException the axata db util exception
	 */
	public void createEntry(String jsonBody) throws SQLException, AxataDBUtilException, RefundException {


		logger.info("Json Body : "+jsonBody);
		Gson gson = new Gson();
		RefundRequest refundRequest = gson.fromJson(jsonBody, RefundRequest.class);
		String formattedJson = StringEscapeUtils.escapeJava(jsonBody.replaceAll("[\r\n]+","").replace(" ","").replace("\t",""));
		String consumerKey = null;
		String refCode = null;
		String operatorId = null;
		ApiInfoDao  apiInfoDao = new ApiInfoDao();

		if (apiInfoDao.isRefCodeIgnored()) {
			logger.info("Original Ref Code will be Ignored.");
			refCode = "null";
			logger.info("Original Ref Code : "+refCode);
			consumerKey = apiInfoDao.getConsumerKey();
			operatorId = apiInfoDao.getOperatorId();
			logger.info("consumerKey : " + consumerKey);
			logger.info("operatorId : " + operatorId);
		} else {
			refCode = refundRequest.getAmountTransaction().getOriginalServerReferenceCode();
			if(isInvalidRefCode(refCode)){
				logger.error("ref code cannot be empty or null");
				throw new RefundException("ref code cannot be empty or null");
			}
			logger.info("Original Ref Code : "+refCode);
			ReadService readService = new ReadService(refCode);
			consumerKey = readService.getConsumerKey();
			operatorId = readService.getOperatorId();
			logger.info("consumerKey : " + consumerKey);
			logger.info("operatorId : " + operatorId);
		}

		PreparedStatement requestps = null;
		PreparedStatement responseps = null;
		String msisdn = refundRequest.getAmountTransaction().getEndUserId().substring(5);
		
		String requestSql = "INSERT INTO SB_API_REQUEST_SUMMARY (messageRowID,api,api_version,version,apiPublisher,consumerKey,userId,context,request_count,hostName,"
				+ "resourcePath,method,requestId,operatorId,chargeAmount,purchaseCategoryCode,jsonBody,year,month,day,time) VALUES "
				+ "( ? , ? , ? , ? , ? , '"+consumerKey+"' , ? , ? ,"+1+", ? , ? , ? ,'null', ? , ? ,'null','"+formattedJson+"', ? , ? , ? , ? )";
		
		
		String responseSql = "INSERT INTO SB_API_RESPONSE_SUMMARY (messageRowID,api,api_version,version,apiPublisher,consumerKey,userId,context,response_count,hostName,"
				+ "resourcePath,method,requestId,operatorId,chargeAmount,purchaseCategoryCode,jsonBody,year,month,day,time,responseCode,msisdn,operatorRef) VALUES "
				+ "( ? , ? , ? , ? , ? , '"+consumerKey+"' , ? , ? ,"+1+", ? , ? , ? ,'null', ? , ? ,'null','"+formattedJson+"', ? , ? , ? , ? ,'200','"+msisdn+"','"+refCode+"')";
		
		
		if (consumerKey != null && !consumerKey.isEmpty()) {
			
			Connection connection = DbUtils.getAxiataDBConnection();
			requestps = connection.prepareStatement(requestSql);
			responseps = connection.prepareStatement(responseSql);
						
			requestps.setString(1, getMessageRowId());
			requestps.setString(2, getApi());
			requestps.setString(3, getApi_version());
			requestps.setString(4, getVersion());
			requestps.setString(5, getApiPublisher());
			requestps.setString(6, getUsername());
			requestps.setString(7, getContext());
			requestps.setString(8, getHostName());
			requestps.setString(9, getResourcePath());
			requestps.setString(10, getMethod());
			requestps.setString(11, operatorId);
			requestps.setString(12, refundRequest.getAmountTransaction().getPaymentAmount().getChargingInformation().getAmount());
			requestps.setInt(13, Calendar.getInstance().get(Calendar.YEAR) );
			requestps.setInt(14, Calendar.getInstance().get(Calendar.MONTH)+1);
			requestps.setInt(15, Calendar.getInstance().get(Calendar.DATE) );
			requestps.setString(16, getCurrentTime());
			
			responseps.setString(1, getMessageRowId());
			responseps.setString(2, getApi());
			responseps.setString(3, getApi_version());
			responseps.setString(4, getVersion());
			responseps.setString(5, getApiPublisher());
			responseps.setString(6, getUsername());
			responseps.setString(7, getContext());
			responseps.setString(8, getHostName());
			responseps.setString(9, getResourcePath());
			responseps.setString(10, getMethod());
			responseps.setString(11, operatorId);
			responseps.setString(12, refundRequest.getAmountTransaction().getPaymentAmount().getChargingInformation().getAmount());
			responseps.setInt(13, Calendar.getInstance().get(Calendar.YEAR) );
			responseps.setInt(14, Calendar.getInstance().get(Calendar.MONTH)+1);
			responseps.setInt(15, Calendar.getInstance().get(Calendar.DATE) );
			responseps.setString(16, getCurrentTime());						
			
			requestps.executeUpdate();
			responseps.executeUpdate();
			connection.close();			
		}			
	}

	private boolean isInvalidRefCode(String refCode) {
		return (refCode.equals("") || refCode.isEmpty() || refCode == null);
	}
}
