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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.wso2telco.refund.utils.ApiInfoDao;
import org.apache.log4j.Logger;

import com.wso2telco.refund.utils.AxataDBUtilException;
import com.wso2telco.refund.utils.DbUtils;

import org.json.JSONObject;

/**
 * The Class ReadService.
 * @author Yasith Lokuge
 */
public class ReadService {
	
	
	/** The operator id. */
	private String operatorId = null;
	
	/** The consumer key. */
	private String consumerKey = null;		
	
	/** The Constant logger. */
	final static Logger logger = Logger.getLogger(ReadService.class);
	
	/**
	 * Instantiates a new read service.
	 *
	 * @param id the id
	 * @throws SQLException the SQL exception
	 * @throws AxataDBUtilException the axata db util exception
	 */
	public ReadService(String id) throws SQLException, AxataDBUtilException{
		String sql="SELECT operatorRef,consumerKey,operatorId from SB_API_RESPONSE_SUMMARY where operatorRef='"+id+"'";
		Connection connection = DbUtils.getAxiataDBConnection();
		Statement statement = connection.createStatement();			
		statement.executeQuery(sql);		
		ResultSet resultSet = statement.executeQuery(sql);

		if (!resultSet.next() ) {
			logger.info(" No records found for the given Server Reference Code: " + id);
			throw new AxataDBUtilException(" No records found for the given Server Reference Code: " + id);
		}

		while(resultSet.next()){
			String serverRefCode = resultSet.getString(1);

			if(serverRefCode.equals(id)){
				logger.info("Server Reference Code : " + serverRefCode);
				consumerKey = resultSet.getString(2);
				operatorId = resultSet.getString(3);
				break;
			}
		}
		connection.close();	
	}
	
	/**
	 * Gets the consumer key.
	 *
	 * @return the consumer key
	 * @throws AxataDBUtilException 
	 */
	public String getConsumerKey() throws AxataDBUtilException{	
		
		if (consumerKey == null){
			logger.error("Invalid Original Server Reference Code or Not Found");
			throw new AxataDBUtilException("Invalid Original Server Reference Code or Not Found");
		}
		return consumerKey;		
	}

	/**
	 * Gets the operator id.
	 *
	 * @return the operator id
	 * @throws AxataDBUtilException 
	 */
	public String getOperatorId() throws AxataDBUtilException {	
		
		if (operatorId == null){
			logger.error("Invalid Original Server Reference Code or Not Found");		
			throw new AxataDBUtilException("Invalid Original Server Reference Code or Not Found");
		}
		return operatorId;
	}
}
