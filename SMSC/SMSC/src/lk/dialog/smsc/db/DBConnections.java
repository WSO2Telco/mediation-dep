package lk.dialog.smsc.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import lk.dialog.smsc.util.DbPropertyReader;
import lk.dialog.smsc.util.PropertyReader;
import lk.dialog.smsc.util.SMSCSIMProperties;

public class DBConnections {
  private static List<Connection> connections = new ArrayList<Connection>();
  private static int numConfiguredConnections = 0;
  private static int numCreatedConnections = 0;
  static {
    numConfiguredConnections = Integer.valueOf(PropertyReader.getPropertyValue(SMSCSIMProperties.NUM_DB_CONNECTIONS));
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
          DbPropertyReader.getPropertyValue(SMSCSIMProperties.CON_URL_STRING),
          DbPropertyReader.getPropertyValue(SMSCSIMProperties.CON_USERNAME),
          DbPropertyReader.getPropertyValue(SMSCSIMProperties.CON_PASSWORD));
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
