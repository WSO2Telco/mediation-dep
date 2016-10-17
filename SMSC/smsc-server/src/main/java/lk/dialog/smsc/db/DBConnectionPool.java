package lk.dialog.smsc.db;

import java.sql.Connection;

public class DBConnectionPool {
  private static final DBConnections connectionPool = new DBConnections();

  public static synchronized Connection getConnection() throws Exception {
	System.out.println("Gettging DB connection from pool.");
    return connectionPool.getConnectionFromPool();
  }
  
  public static synchronized void releaseConnection(Connection connection) {
	System.out.println("Releasing DB connection to pool.");
    connectionPool.releaseConnection(connection);
  }
}
