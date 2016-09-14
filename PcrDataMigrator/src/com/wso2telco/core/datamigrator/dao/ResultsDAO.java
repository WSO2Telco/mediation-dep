package com.wso2telco.core.datamigrator.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.wso2telco.core.datamigrator.dto.ResultDTO;
import com.wso2telco.core.datamigrator.exception.PCRException;
import com.wso2telco.core.datamigrator.util.JDBCInitiator;
import com.wso2telco.core.datamigrator.util.SectorUtil;

public class ResultsDAO {

	private static Logger log = Logger.getLogger(SectorUtil.class);

	public ArrayList<ResultDTO> getResults() throws PCRException {

		JDBCInitiator initiator = new JDBCInitiator();
		Connection conn = initiator.getConnection();
		ArrayList<ResultDTO> results = new ArrayList<ResultDTO>();

		try {
			String sql = "SELECT * FROM RESULTS";
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);

			ResultSetMetaData metadata = resultSet.getMetaData();
			int numberOfColumns = metadata.getColumnCount();
			while (resultSet.next()) {
				int i = 1;
				ResultDTO resultDTO = new ResultDTO();
				while (i <= numberOfColumns) {
					resultDTO.setApplicationId(resultSet.getString("APPLICATION_ID"));
					resultDTO.setCallbackUrl(resultSet.getString("CALLBACK_URL"));
					resultDTO.setConsumerKey(resultSet.getString("CONSUMER_KEY"));
					i++;
				}
				results.add(resultDTO);
			}
		} catch (SQLException e) {
			log.error("sql error",e);
			throw new PCRException("sql error");
		}
		return results;
	}
}
