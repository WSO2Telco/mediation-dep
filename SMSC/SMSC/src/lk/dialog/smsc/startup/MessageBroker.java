package lk.dialog.smsc.startup;

import java.net.URI;
import java.net.URISyntaxException;

import lk.dialog.smsc.util.PropertyReader;
import lk.dialog.smsc.util.SMSCSIMProperties;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logica.smscsim.Simulator;

public class MessageBroker {
	
	static Logger logger = LoggerFactory.getLogger( MessageBroker.class );
		
  public static void start(Simulator simulator) {
    BrokerService broker = new BrokerService();
    TransportConnector connector = new TransportConnector();
    try {
      String brokerURI = PropertyReader.getPropertyValue(SMSCSIMProperties.MESSAGE_BROKER_URI);
      connector.setUri(new URI(brokerURI));
      broker.addConnector(connector);
      broker.start();
      
      logger.info("Connecting to message queue at -> " + brokerURI);
      System.out.println("Connecting to message queue at -> " + brokerURI);
      MessageReceiver.startNewListener(simulator);
      
    } catch (URISyntaxException e) {
      logger.info("MessageBroker::start", e );
      e.printStackTrace();
    } catch (Exception e) {
      logger.info("MessageBroker::start", e );
      e.printStackTrace();
    }
  }
}
