package com.wso2telco.db;

import java.sql.Connection;

public class DBConnectionPool {
  private static final DBConnections connectionPool = new DBConnections();

  public static synchronized Connection getConnection() throws Exception {
    return connectionPool.getConnectionFromPool();
  }
  
  public static synchronized void releaseConnection(Connection connection) {
    connectionPool.releaseConnection(connection);
  }
}
