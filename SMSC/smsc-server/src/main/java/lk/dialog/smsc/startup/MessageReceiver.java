package lk.dialog.smsc.startup;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import lk.dialog.smsc.db.DBConnectionPool;
import lk.dialog.smsc.db.DBLogger;
import lk.dialog.smsc.util.PropertyReader;
import lk.dialog.smsc.util.SMSCSIMProperties;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logica.smscsim.Simulator;

public class MessageReceiver implements Runnable {
	
	static Logger logger = LoggerFactory.getLogger( MessageReceiver.class );
	
  private static ActiveMQConnectionFactory connectionFactory;
  private static Connection connection;
  private static String brokerURI = PropertyReader.getPropertyValue(SMSCSIMProperties.MESSAGE_BROKER_URI);
  private Simulator simulator;
  //If load test failed add a connection pool. Even though connection can be reused, it would be difficult for a connection to 
  //handle large number of connection simultaneously.
  static {
    connectionFactory = new ActiveMQConnectionFactory(brokerURI);
    try {
      connection = connectionFactory.createConnection();
      connection.start();
    } catch (JMSException e) {
      e.printStackTrace();
      logger.error("MessageReceiver ActiveMQConnectionFactory connection error ", e);
    }
  }

  public MessageReceiver(Simulator simulator) {
    this.simulator = simulator;
  }

public static void startNewListener(Simulator simulator) throws Exception {
    new Thread(new MessageReceiver(simulator)).start();
  }

  @Override
  public void run() {
    Session session = null;
    MessageConsumer consumer = null;
    try {
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Destination destination = session.createQueue("TEST.FOO");
      consumer = session.createConsumer(destination);

      MessageListener listner = new MessageListener() {
        public void onMessage(Message message) {
          try {
            if (message instanceof TextMessage) {
              TextMessage textMessage = (TextMessage) message;
              String requestBody = textMessage.getText();
              logger.info("Received > " + requestBody);
              //System.out.println("Received>: " + requestBody);
              
              String[] requestParams = requestBody.split(",");
//            List<String> requestParams = Arrays.asList( requestBody.split(",") );
              
              /*===============================================================================================================
              String s=requestParams[0];
              s = s.substring(s.indexOf("id:") + 3);
      		  s = s.substring(0, s.indexOf("sub")).trim();
              String refNumber = s;			//For DN it will be the row id of delivery_notif table
              //boolean isLongMessage = Boolean.valueOf(requestParams[2]);//EDITED
              String strMsgBody = "";
              ===============================================================================================================*/
              
              //===============================================================================================================
				String sqlQuery = "";
              	String messageType = requestParams[0];
              	String refNumber = requestParams[1];			//For DN it will be the row id of delivery_notif table
              	//NOT USED//boolean isLongMessage = Boolean.valueOf(requestParams[2]);
              	String strMsgBody = requestParams[2];
              	
              	String sme = "N";
              	if (requestParams.length>3) {
              		sme=requestParams[3];
				}
              //===============================================================================================================

              
              boolean needToSendDNOrSMSToClient = false;
              
              //Checking if the message is Deliver Notification or MO
              java.sql.Connection dbCon = DBConnectionPool.getConnection();
              
              if( messageType.equalsIgnoreCase("DELIVERY_NOTIF") ){
            	  
            	  //2016-03-11 :: This check is impossible since the callbackData field isn't as expected. Commenting these lines and 
            	  //			  sending notifications defaultly.
            	  //Check if delivery notification is necessary
            	  /*
            	  if(isLongMessage) {
            	    sqlQuery = "SELECT need_delivery_report FROM smse_req_resp_log WHERE type_request = 1 AND sar_ref_num = ? LIMIT 1";
            	  } else {
            	    sqlQuery = "SELECT need_delivery_report FROM smse_req_resp_log WHERE type_request = 1 AND ref_num = ? LIMIT 1";
            	  }
            	  
            	  needToSendDNOrSMSToClient = false;
            	  
                  PreparedStatement prepStat = dbCon.prepareStatement( sqlQuery );	
                  prepStat.setInt( 1, Integer.parseInt(refNumber) );
                  ResultSet objRes = prepStat.executeQuery();
                  while (objRes.next()) {
                    needToSendDNOrSMSToClient = objRes.getBoolean("need_delivery_report");
                  }
				  */
            	  needToSendDNOrSMSToClient = true;
            	  
                  if(needToSendDNOrSMSToClient){
                    //sqlQuery = "SELECT * FROM delivery_notif WHERE ref_num = ?";
                    sqlQuery = "SELECT * FROM delivery_notif WHERE id = ?";
                  } else {
                    //Do nothing release the DB connection
                    DBConnectionPool.releaseConnection(dbCon);
                  }

              } else if( messageType.equalsIgnoreCase("MO_SMS")) {
            	  needToSendDNOrSMSToClient = true;
            	  sqlQuery = "SELECT * FROM mo_sms WHERE ref_num = ?";
              }
              
              if(needToSendDNOrSMSToClient){
            	  String strMsgBodyTemp="";
            	  PreparedStatement prepStatement = dbCon.prepareStatement( sqlQuery );	
            	  prepStatement.setString(1, refNumber);
            	  ResultSet objResults = prepStatement.executeQuery();
            	  while (objResults.next()) {
            	    strMsgBodyTemp = objResults.getString("body");
                }
            	  DBConnectionPool.releaseConnection(dbCon);

            	  //Note ::Delivery send delivery report or SMS - DELIVER_SM
            	  DBLogger dbLogger=new DBLogger();
            	  String smeUser="";
            	  if (messageType.equalsIgnoreCase("DELIVERY_NOTIF") && !sme.equalsIgnoreCase("N")) {
            		  smeUser=dbLogger.getNameFromId(Integer.valueOf(sme));  
            	  }
            	  
            	  //SELECT code FROM sme s where id=1;
            	  simulator.sendQuedMessage( messageType, strMsgBodyTemp, refNumber, strMsgBody,smeUser );
              }

            } else {
              System.out.println("Received>: " + message);
            }
          } catch (JMSException e) {
        	logger.error("MessageReceiver [JMSException]", e);
            System.out.println("Caught:" + e);
            e.printStackTrace();
          } catch (Exception ex){
        	 logger.error("MessageReceiver ", ex );
        	 System.out.println("Caught:" + ex);
        	 ex.printStackTrace();
          }
        }
      };
      consumer.setMessageListener(listner);
    } catch (Exception e) {
    	logger.error("MessageReceiver::run", e);
      e.printStackTrace();
      if (consumer != null) {
        try {
          consumer.close();
        } catch (JMSException e1) {
        	logger.error("MessageReceiver::run", e1);
          e1.printStackTrace();
        }
      }
      if (session != null) {
        try {
          session.close();
        } catch (JMSException e1) {
        	logger.error("MessageReceiver::run", e1);
          e1.printStackTrace();
        }
      }
    }
  }
}
