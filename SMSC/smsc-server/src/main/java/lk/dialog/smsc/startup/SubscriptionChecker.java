package lk.dialog.smsc.startup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lk.dialog.smsc.db.DBConnectionPool;
import lk.dialog.smsc.mife.request.subscription.delivery.CallbackReference;
import lk.dialog.smsc.mife.request.subscription.delivery.DeliveryReceiptSubscription;
import lk.dialog.smsc.mife.request.subscription.delivery.SenderAddress;
import lk.dialog.smsc.mife.request.subscription.delivery.SubscribeDeliveryNotifRequest;
import lk.dialog.smsc.mife.request.subscription.mosms.DestinationAddress;
import lk.dialog.smsc.mife.request.subscription.mosms.Subscription;
import lk.dialog.smsc.mife.request.subscription.mosms.SubscriptionRequest;
import lk.dialog.smsc.util.CommonUtils;
import lk.dialog.smsc.util.PropertyReader;
import lk.dialog.smsc.util.RESTClient;
import lk.dialog.smsc.util.SMSCSIMProperties;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.logica.smscsim.Simulator;
import com.wso2telco.refresh.Refresher;

class SubscriptionChecker implements Runnable {

  static Logger logger = LoggerFactory.getLogger( SubscriptionChecker.class );
	
  private static String COL_SUBSCRIBED_TO_DELIVERY_NOTIF = "subscribed_to_delivery_notif";
  private static String COL_SUBSCRIBED_TO_MO_SMS = "subscribed_to_mo_sms";

  @Override
  public void run() {
    boolean alreadySubscribedToDeliveryNotif = checkAlreadySubscribed(COL_SUBSCRIBED_TO_DELIVERY_NOTIF);
    boolean alreadySubscribedToMOSms = checkAlreadySubscribed(COL_SUBSCRIBED_TO_MO_SMS);
    if (!alreadySubscribedToDeliveryNotif) {
      String endPoint = PropertyReader.getPropertyValue(SMSCSIMProperties.API_SUBSCRIBE_TO_DELIVERY_NOTIF);
      String jsonBody = buildSubscribeDevileryNotifJsonBody();//TODO - add correct json body
      logger.info("DN Subscription JSON Body ::"+jsonBody);
      subscribe(endPoint, jsonBody, COL_SUBSCRIBED_TO_DELIVERY_NOTIF);
    }
    if (!alreadySubscribedToMOSms) {
      String endPoint = PropertyReader.getPropertyValue(SMSCSIMProperties.API_SUBSCRIBE_TO_MO_SMS);
      String jsonBody = buildSubscribeSMSJsonBody();//TODO - add correct json body
      logger.info("MO Subscription JSON Body ::"+jsonBody);
      subscribe(endPoint, jsonBody, COL_SUBSCRIBED_TO_MO_SMS);
    }
  }

  private String buildSubscribeDevileryNotifJsonBody() {
    SubscribeDeliveryNotifRequest req = new SubscribeDeliveryNotifRequest();
    DeliveryReceiptSubscription drSubs = new DeliveryReceiptSubscription();
    CallbackReference cbakRef = new CallbackReference();
    cbakRef.setNotifyURL(PropertyReader.getPropertyValue(SMSCSIMProperties.CALLBACK_SUBSCRIBE_DELIVERY_NOTIF));
    drSubs.setCallbackReference(cbakRef);
    drSubs.setClientCorrelator(String.valueOf(CommonUtils.getUniqueNumber()));
    drSubs.setSenderAddresses(getSenderAddresses());
    req.setDeliveryReceiptSubscription(drSubs);
    return new Gson().toJson(req);
  }

  private String buildSubscribeSMSJsonBody() {
    SubscriptionRequest req = new SubscriptionRequest();
    Subscription subs = new Subscription();
    subs.setNotificationFormat("JSON");
    lk.dialog.smsc.mife.request.subscription.mosms.CallbackReference cbackRef = new lk.dialog.smsc.mife.request.subscription.mosms.CallbackReference();
    cbackRef.setNotifyURL( PropertyReader.getPropertyValue(SMSCSIMProperties.CALLBACK_SUBSCRIBE_SMS) );
    cbackRef.setCallbackData( PropertyReader.getPropertyValue(SMSCSIMProperties.SUBSCRIPTION_MO_CALLBACKDATA) );
    subs.setCallbackReference(cbackRef);
    subs.setClientCorrelator(String.valueOf(CommonUtils.getUniqueNumber()));
    subs.setDestinationAddresses( getDestinationAddresses() );
    req.setSubscription(subs);
    return new Gson().toJson(req);
  }
  
  private List<DestinationAddress> getDestinationAddresses(){
    String[] operatorCodes = getOperatorCodes();
    List<DestinationAddress> destinationAddresses = new ArrayList<DestinationAddress>();
      for (String operatorCode : operatorCodes) {
        DestinationAddress destAddress = new DestinationAddress();
        destAddress.setOperatorCode(operatorCode);
        destAddress.setDestinationAddress( PropertyReader.getPropertyValue(SMSCSIMProperties.DESTINATION_ADDRESS) );
        destAddress.setCriteria( PropertyReader.getPropertyValue(SMSCSIMProperties.SUBSCRIPTION_MO_CRITERIA) );
        destinationAddresses.add(destAddress);
      }
      return destinationAddresses;
  }

  private List<SenderAddress> getSenderAddresses() {
    String[] operatorCodes = getOperatorCodes();
    List<SenderAddress> senderAddresses = new ArrayList<SenderAddress>();
    for (String operatorCode : operatorCodes) {
      SenderAddress senderAddress = new SenderAddress();
      senderAddress.setOperatorCode(operatorCode);
      senderAddress.setSenderAddress(PropertyReader.getPropertyValue(SMSCSIMProperties.SENDER_ADDRESS));
      senderAddress.setFilterCriteria( PropertyReader.getPropertyValue(SMSCSIMProperties.SUBSCRIPTION_DN_CRITERIA) );
      senderAddresses.add(senderAddress);
    }
    return senderAddresses;
  }

  private String[] getOperatorCodes() {
    try {
      String strOperators = PropertyReader.getPropertyValue(SMSCSIMProperties.OPERATORS_LIST);
      return strOperators.split("\\s*,\\s*");
    } catch (Exception e) {
      return new String[]{};
    }
  }

  private void subscribe(String endPoint, String jsonBody, String columnToUpdate) {
    RESTClient mifeRestClient = new RESTClient();
    HttpResponse objResponse = mifeRestClient.post(endPoint, jsonBody, Refresher.getToken());
    try {

      String strTmp = EntityUtils.toString( objResponse.getEntity() );
      logger.info("SubscriptionChecker::subscribe -> Response "+strTmp );
      //System.out.println("SUBSCRIBE RESP>" + strTmp );

      int statusCode = objResponse.getStatusLine().getStatusCode();
      if(statusCode == 200) {
        updateDatabase(columnToUpdate);
      }
    } catch (Exception e) {
    	logger.error("SubscriptionChecker::subscribe" , e );
        e.printStackTrace();
    }
  }

  private void updateDatabase(String columnToUpdate){
	try {
		Connection connection = DBConnectionPool.getConnection();
//	    String sql = "INSERT INTO sme (" + columnToUpdate + ") VALUES (?) where id = ?";
	    String sql = "UPDATE sme SET " + columnToUpdate + " = ? where id = ?";
	    PreparedStatement stmt = connection.prepareStatement(sql);
	    stmt.setInt(1, 1);
	    stmt.setInt(2, 1);
	    System.out.println(sql);
	    stmt.executeUpdate();
	    DBConnectionPool.releaseConnection(connection);
	    
	} catch (Exception e) {
		logger.error("SubscriptionChecker::updateDatabase" , e );
		e.printStackTrace();
		//System.out.println( "DB Connection Exception :: updateDatabase "+ e.getMessage() );
	}
  }

  private boolean checkAlreadySubscribed(String columnName) {
    boolean alreadySubscribed = false;
    try {
      Connection connection = DBConnectionPool.getConnection();
      String sql = "select " + columnName + " from sme where id = ?";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setInt(1, 1);
      ResultSet rs = preparedStatement.executeQuery();
      while (rs.next()) {
        alreadySubscribed = (Boolean) rs.getObject(columnName);
      }
      preparedStatement.close();
      DBConnectionPool.releaseConnection(connection);
    } catch (Exception e) {
    	logger.error("SubscriptionChecker::checkAlreadySubscribed" , e );
    	e.printStackTrace();
    	//System.out.println( "DB Connection Exception :: checkAlreadySubscribed "+ e.getMessage() );
    }
    return alreadySubscribed;
  }
}