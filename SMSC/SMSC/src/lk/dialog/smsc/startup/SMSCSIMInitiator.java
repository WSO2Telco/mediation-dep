package lk.dialog.smsc.startup;

import com.logica.smscsim.Simulator;


public class SMSCSIMInitiator {
  public static void init(Simulator simulator) {
    new Thread(new SubscriptionChecker()).start();
    MessageBroker.start(simulator);
  }
}