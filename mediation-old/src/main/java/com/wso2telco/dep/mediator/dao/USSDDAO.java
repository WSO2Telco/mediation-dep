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
package com.wso2telco.dep.mediator.dao;

import com.wso2telco.core.dbutils.DbUtils;
import com.wso2telco.core.dbutils.util.DataSourceNames;
import com.wso2telco.dep.mediator.util.DatabaseTables;
import com.wso2telco.dep.operatorservice.model.OperatorSubscriptionDTO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class USSDDAO {

	/** The Constant log. */
	private final Log log = LogFactory.getLog(USSDDAO.class);

	/**
	 * Ussd request entry.
	 *
	 * @param notifyURL
	 *            the notifyURL
	 * @return the integer
	 * @throws Exception
	 *             the exception
	 */
	public Integer ussdRequestEntry(String notifyURL , String consumerKey, String operatorId, String userId) throws SQLException, Exception {

		Connection con = null;
		PreparedStatement insert_statement=null;
		PreparedStatement select_statement =null;
		ResultSet insert_result = null;
		Integer selectId = 0;
		Integer newId = 0;

		try {

			con = DbUtils.getDbConnection(DataSourceNames.WSO2TELCO_DEP_DB);
			if (con == null) {

				throw new Exception("Connection not found");
			}

			con.setAutoCommit(false);
            
			
			StringBuilder insertQueryString = new StringBuilder(" INSERT INTO ");
			insertQueryString.append(DatabaseTables.USSD_REQUEST_ENTRY.getTableName());
			insertQueryString.append(" (notifyurl,sp_consumerKey,operatorId,userId) ");
			insertQueryString.append("VALUES ( ? ,? ,? ,? )");

			insert_statement = con.prepareStatement(insertQueryString.toString(), Statement.RETURN_GENERATED_KEYS);

			insert_statement.setString(1, notifyURL);
			insert_statement.setString(2, consumerKey);
			insert_statement.setString(3, operatorId);
			insert_statement.setString(4, userId);
			
			log.debug("sql query in ussdRequestEntry : " + insert_statement);

			insert_statement.executeUpdate();

			insert_result = insert_statement.getGeneratedKeys();

			while (insert_result.next()) {
				newId = insert_result.getInt(1);
			}
			
		} catch (SQLException e) {

			log.error("database operation error in ussdRequestEntry : ", e);
			throw e;
		} catch (Exception e) {

			log.error("error in ussdRequestEntry : ", e);
			throw e;
		} finally {

			DbUtils.closeAllConnections(insert_statement, con, insert_result);
			
		}

		return newId;
	}

	/**
	 * Gets the USSD notify.
	 *
	 * @param subscriptionId
	 *            the subscriptionId
	 * @return the USSD notify
	 * @throws Exception
	 *             the exception
	 */
		public List<String> getUSSDNotifyURL(Integer subscriptionId) throws SQLException, Exception {

		Connection con = DbUtils.getDbConnection(DataSourceNames.WSO2TELCO_DEP_DB);
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<String> ussdSPDetails = null;


		try {

			if (con == null) {

				throw new Exception("Connection not found");
			}

			StringBuilder queryString = new StringBuilder("SELECT notifyurl, sp_consumerKey, operatorId, userId ");
			queryString.append("FROM ");
			queryString.append(DatabaseTables.USSD_REQUEST_ENTRY.getTableName());
			queryString.append(" WHERE ussd_request_did = ?");

			ps = con.prepareStatement(queryString.toString());

			ps.setInt(1, subscriptionId);
			
			log.debug("sql query in getUSSDNotifyURL : " + ps);

			rs = ps.executeQuery();

			if (rs.next()) {

				ussdSPDetails = new ArrayList<String>();

				//notifyurls = rs.getString("notifyurl");
			    ussdSPDetails.add(rs.getString("notifyurl"));
				ussdSPDetails.add(rs.getString("sp_consumerKey"));
				ussdSPDetails.add(rs.getString("operatorId"));
				ussdSPDetails.add(rs.getString("userId"));
				
			}

		} catch (SQLException e) {

			log.error("database operation error in getUSSDNotify : ", e);
			throw e;
		} catch (Exception e) {

			log.error("error in getUSSDNotify : ", e);
			throw e;
		} finally {

			DbUtils.closeAllConnections(ps, con, rs);
		}

		return ussdSPDetails;
	}

	public List<String> getUSSDNotifyURL(Integer subscriptionId, String consumerKey) throws Exception {
		Connection con = DbUtils.getDbConnection(DataSourceNames.WSO2TELCO_DEP_DB);
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<String> ussdSPDetails = null;

		try {
			if (con == null) {
				throw new Exception("Connection not found");
			}
			StringBuilder queryString = new StringBuilder("SELECT notifyurl, sp_consumerKey, operatorId, userId ");
			queryString.append("FROM ");
			queryString.append(DatabaseTables.USSD_REQUEST_ENTRY.getTableName());
			queryString.append(" WHERE ussd_request_did = ? AND sp_consumerKey = ?");

			ps = con.prepareStatement(queryString.toString());
			ps.setInt(1, subscriptionId);
			ps.setString(2, consumerKey);
			log.debug("sql query in getUSSDNotifyURL : " + ps);
			rs = ps.executeQuery();
			if (rs.next()) {
				ussdSPDetails = new ArrayList<String>();
				ussdSPDetails.add(rs.getString("notifyurl"));
				ussdSPDetails.add(rs.getString("sp_consumerKey"));
				ussdSPDetails.add(rs.getString("operatorId"));
				ussdSPDetails.add(rs.getString("userId"));
			}
		} catch (SQLException e) {
			log.error("database operation error in getUSSDNotify : ", e);
			throw e;
		} catch (Exception e) {

			log.error("error in getUSSDNotify : ", e);
			throw e;
		} finally {
			DbUtils.closeAllConnections(ps, con, rs);
		}
		return ussdSPDetails;
	}

	/**
	 * Ussd entry delete.
	 *
	 * @param subscriptionId
	 *            the subscriptionId
	 * @return true, if successful
	 * @throws Exception
	 *             the exception
	 */
	public boolean ussdEntryDelete(Integer subscriptionId) throws SQLException, Exception {

		Connection con = DbUtils.getDbConnection(DataSourceNames.WSO2TELCO_DEP_DB);
		PreparedStatement ps = null;

		try {

			if (con == null) {

				throw new Exception("Connection not found");
			}

			StringBuilder queryString = new StringBuilder("DELETE FROM ")
			.append(DatabaseTables.USSD_REQUEST_ENTRY.getTableName())
			.append(" WHERE ussd_request_did = ?");

			ps = con.prepareStatement(queryString.toString());

			ps.setInt(1, subscriptionId);
			
			log.debug("sql query in ussdEntryDelete : " + ps);

			ps.executeUpdate();
		} catch (SQLException e) {

			log.error("database operation error in ussdEntryDelete : ", e);
			throw e;
		} catch (Exception e) {

			log.error("error in ussdEntryDelete : ", e);
			throw e;
		} finally {

			DbUtils.closeAllConnections(ps, con, null);
		}

		return true;
	}
	
	
	public void moUssdSubscriptionEntry(List<OperatorSubscriptionDTO> domainsubs,Integer moSubscriptionId) throws SQLException, Exception{

		Connection con = DbUtils.getDbConnection(DataSourceNames.WSO2TELCO_DEP_DB);
		PreparedStatement insertStatement = null;
		
		try {
			
			if (con == null) {
				throw new Exception("Connection not found");
			}
			con.setAutoCommit(false);
			
			StringBuilder queryString = new StringBuilder("INSERT INTO ");
			queryString.append(DatabaseTables.MO_USSD_SUBSCRIPTIONS.getTableName());
			queryString.append(" (ussd_request_did, domainurl, operator) ");
			queryString.append("VALUES (?, ?, ?)");
			
			insertStatement = con.prepareStatement(queryString.toString());

			for (OperatorSubscriptionDTO d : domainsubs) {
				
				insertStatement.setInt(1, moSubscriptionId);
				insertStatement.setString(2, d.getDomain());
				insertStatement.setString(3, d.getOperator());
				
				insertStatement.addBatch();
			}
			
			insertStatement.executeBatch();
			con.commit();
			

		} catch (SQLException e) {

			log.error("database operation error in operatorSubsEntry : ", e);
			throw e;
		} catch (Exception e) {

			log.error("error in operatorSubsEntry : ", e);
			throw e;
		} finally {

			DbUtils.closeAllConnections(insertStatement, con, null);
		}
	}
	/**
	 * 
	 * @param moSubscriptionId
	 * @return
	 * @throws SQLException
	 * @throws Exception
	 */
	
	public List<OperatorSubscriptionDTO> moUssdSubscriptionQuery(Integer moSubscriptionId) throws SQLException, Exception {
	
		Connection con = DbUtils.getDbConnection(DataSourceNames.WSO2TELCO_DEP_DB);
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<OperatorSubscriptionDTO> domainsubs = new ArrayList();
		
		try {
			
			if (con == null) {

				throw new Exception("Connection not found");
			}

			StringBuilder queryString = new StringBuilder("SELECT domainurl, operator ");
			queryString.append("FROM ");
			queryString.append(DatabaseTables.MO_USSD_SUBSCRIPTIONS.getTableName());
			queryString.append(" WHERE ussd_request_did = ?");

			ps = con.prepareStatement(queryString.toString());

			ps.setInt(1, moSubscriptionId);
			
			log.debug("sql query in subscriptionQuery : " + ps);

			rs = ps.executeQuery();

			while (rs.next()) {

				domainsubs.add(new OperatorSubscriptionDTO(rs.getString("operator"), rs.getString("domainurl")));
			}
		} catch (SQLException e) {

			log.error("database operation error in moUssdSubscriptionQuery : ", e);
			throw e;
		} catch (Exception e) {

			log.error("error in moUssdSubscriptionQuery : ", e);
			throw e;
		} finally {

			DbUtils.closeAllConnections(ps, con, rs);
		}
		
		return domainsubs;

}
	
	
	public void moUssdSubscriptionDelete(Integer moSubscriptionId) throws SQLException, Exception {

		Connection con = DbUtils.getDbConnection(DataSourceNames.WSO2TELCO_DEP_DB);
		PreparedStatement deleteSubscriptionsStatement = null;
		PreparedStatement deleteOperatorSubscriptionsStatement = null;

		try {

			if (con == null) {

				throw new Exception("Connection not found");
			}

			/**
			 * Set autocommit off to handle the transaction
			 */
			con.setAutoCommit(false);

			StringBuilder deleteSubscriptionsQueryString = new StringBuilder("DELETE FROM ")
			.append(DatabaseTables.USSD_REQUEST_ENTRY.getTableName())
			.append(" WHERE ussd_request_did = ?");

			deleteSubscriptionsStatement = con.prepareStatement(deleteSubscriptionsQueryString.toString());
			deleteSubscriptionsStatement.setInt(1, moSubscriptionId);
			
			log.debug("sql query in outboundSubscriptionDelete : " + deleteSubscriptionsStatement);

			deleteSubscriptionsStatement.executeUpdate();

			StringBuilder deleteOperatorSubscriptionsQueryString = new StringBuilder("DELETE FROM ")
			.append(DatabaseTables.MO_USSD_SUBSCRIPTIONS.getTableName())
			.append(" WHERE ussd_request_did = ?");

			deleteOperatorSubscriptionsStatement = con.prepareStatement(deleteOperatorSubscriptionsQueryString.toString());

			deleteOperatorSubscriptionsStatement.setInt(1, moSubscriptionId);
			
			log.debug("sql query in outboundSubscriptionDelete : " + deleteOperatorSubscriptionsStatement);

			deleteOperatorSubscriptionsStatement.executeUpdate();

			/**
			 * commit the transaction if all success
			 */
			con.commit();
		} catch (SQLException e) {

			/**
			 * rollback if Exception occurs
			 */
			con.rollback();

			log.error("database operation error in moUssdSubscriptionDelete : ", e);
			throw e;
		} catch (Exception e) {

			/**
			 * rollback if Exception occurs
			 */
			con.rollback();

			log.error("Error while deleting subscriptions. ", e);
			throw e;
		} finally {

			DbUtils.closeAllConnections(deleteSubscriptionsStatement, con, null);
			DbUtils.closeAllConnections(deleteOperatorSubscriptionsStatement, null, null);
		}
	}

	public String getOperatorIdByOperator(String operator)  throws SQLException, Exception {
		
			Connection con = DbUtils.getDbConnection(DataSourceNames.WSO2TELCO_DEP_DB);
			PreparedStatement ps = null;
	        ResultSet rs = null;
	        String operatorId="";
	
	        try {
	        	StringBuilder queryString = new StringBuilder(" SELECT id FROM ")
	        	.append(DatabaseTables.OPERATORS.getTableName())
	        	.append(" o ")
	        	.append(" WHERE ")
	        	.append(" operatorname=? ");
	        	
	            ps = con.prepareStatement(queryString.toString());
	            ps.setString(1, operator);
	            rs = ps.executeQuery();
	
	            if (rs.next() && rs.getString("id")!= null ) {
	                operatorId=  rs.getString("id") ;
	            }
	        } catch (Exception e) {
	            DbUtils.handleException("Error while checking OperatorId. ", e);
	        } finally {
	            DbUtils.closeAllConnections(ps, con, rs);
	        }
	        return operatorId;
		
		
	}	

		public void updateOperatorIdBySubscriptionId(Integer subscriptionId, String operatorId)  throws SQLException, Exception {
    		
			Connection con = DbUtils.getDbConnection(DataSourceNames.WSO2TELCO_DEP_DB);
			PreparedStatement ps = null;
			    
			try {
				if (con == null) {
					throw new Exception("Connection not found");
				}
				
				StringBuilder queryString = new StringBuilder(" UPDATE ")
				.append(DatabaseTables.USSD_REQUEST_ENTRY.getTableName())
				.append(" SET ")
				.append(" operatorId= ? ")
				.append(" WHERE ")
				.append(" ussd_request_did = ? ");
				
				ps = con.prepareStatement(queryString.toString());	           
				ps.setString(1, operatorId);
				ps.setInt(2,subscriptionId);
	           
				ps.executeUpdate();
		
			} catch (Exception e) {
				throw e;
			} finally {
				DbUtils.closeAllConnections(ps, con, null);
			}
				
			}
}
