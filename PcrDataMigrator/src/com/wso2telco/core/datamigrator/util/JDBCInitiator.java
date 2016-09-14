package com.wso2telco.core.datamigrator.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCInitiator {
	
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost/pcr_service";	
	static final String USER = "root";
	static final String PASS = "root";

	public Connection getConnection() {

		Connection conn = null;		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
		} catch (ClassNotFoundException | SQLException e) {			
			e.printStackTrace();
		}			
		return conn;
	}
}