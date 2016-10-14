package com.wso2telco.services.util;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;

public class MessageSender {
  private static ActiveMQConnectionFactory connectionFactory;
  private static Connection connection;
  private static String brokerURI = FileUtil.getPropertyValue("message_broker_uri");
  //If load test failed add a connection pool. Even though connection can be reused, it would be difficult for a connection to 
  //handle large number of connection simultaneously.
  static {
    connectionFactory = new ActiveMQConnectionFactory(brokerURI);
    try {
      connection = connectionFactory.createConnection();
      connection.start();
    } catch (JMSException e) {
      e.printStackTrace();
      //TODO handle, could not create/start the connection
    }
  }

  public static void sendNotificationToSMSC(String messageToDeliver) {
    try {
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Destination destination = session.createQueue("TEST.FOO");
      MessageProducer producer = session.createProducer(destination);
      producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      TextMessage message = session.createTextMessage(messageToDeliver);
      System.out.println("Message Sent >" + messageToDeliver);
      producer.send(message);
      session.close();
      //        connection.close();
    } catch (Exception e) {
      e.printStackTrace();//TODO handle
    }
  }
}
