package com.wso2telco.core.datamigrator.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.wso2telco.core.datamigrator.dto.RequestDTO;
import com.wso2telco.core.datamigrator.exception.PCRException;
import com.wso2telco.core.datamigrator.util.JDBCInitiator;
import com.wso2telco.core.datamigrator.util.SectorUtil;

public class RequestDAO {

	private static Logger log = Logger.getLogger(SectorUtil.class);
	
	public ArrayList<RequestDTO> getRequests() throws PCRException {

		JDBCInitiator initiator = new JDBCInitiator();
		Connection conn = initiator.getConnection();
		ArrayList<RequestDTO> results = new ArrayList<RequestDTO>();

		try {
			String sql = "SELECT * FROM REQUESTS";
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);

			ResultSetMetaData metadata = resultSet.getMetaData();
			int numberOfColumns = metadata.getColumnCount();
			while (resultSet.next()) {
				int i = 1;
				RequestDTO requestDTO = new RequestDTO();
				while (i <= numberOfColumns) {
					requestDTO.setConsumerKey(resultSet.getString("CONSUMER_KEY"));
					requestDTO.setCallbackurl(resultSet.getString("SECTOR_ID"));
					requestDTO.setMsisdn(resultSet.getString("MSISDN"));
					requestDTO.setApplicationName(resultSet.getString("APP_NAME"));
					i++;
				}
				results.add(requestDTO);
			}
		} catch (SQLException e) {
			log.error("sql syntax error",e);
			throw new PCRException("sql error");
		}
		return results;
	}
}
