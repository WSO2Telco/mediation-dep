package com.wso2telco.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import com.wso2telco.services.util.FileUtil;

public class DBConnections {
  private static List<Connection> connections = new ArrayList<Connection>();
  private static int numConfiguredConnections = 0;
  private static int numCreatedConnections = 0;
  static {
    numConfiguredConnections = Integer.valueOf(FileUtil.getPropertyValue("db.num_connections"));
  }

  Connection getConnectionFromPool() throws Exception {
    if(!connections.isEmpty()) {
      return connections.remove(0);
    } else {
      if(numCreatedConnections < numConfiguredConnections) {
        return createNewConnection();
      } else {
        try {
          wait();//All connections are busy. Wait until one is released.
          return getConnectionFromPool();
        } catch (InterruptedException e) {
          return getConnectionFromPool();
        }
      }
    }
  }

  private static Connection createNewConnection() throws Exception {
    try {
      Class.forName("com.mysql.jdbc.Driver");
      Connection conn = DriverManager.getConnection(
          FileUtil.getPropertyValue("db.connection.url"),
          FileUtil.getPropertyValue("db.connection.username"),
          FileUtil.getPropertyValue("db.connection.password"));
      numCreatedConnections++;
      return conn;
    } catch (Exception e) {
      e.printStackTrace();// TODO have to handle
      throw e;
    }
  }

  synchronized void releaseConnection(Connection connection) {
    connections.add(connection);
    notifyAll();
  }
}
