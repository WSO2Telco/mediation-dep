package lk.dialog.smsc.pool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.dialog.smsc.exception.SMSCException;
import lk.dialog.smsc.services.DeliverSMService;
import lk.dialog.smsc.services.QuerySMService;
import lk.dialog.smsc.services.SendMultiSMSService;
import lk.dialog.smsc.services.SendSMSService;
import lk.dialog.smsc.services.i.IMIFEService;
import lk.dialog.smsc.util.PropertyReader;
import lk.dialog.smsc.util.SMSCSIMProperties;

public class MIFEServicePool {
  
  public static final int TYPE_SEND_SMS = 1;
  public static final int TYPE_SEND_MULTI = 2;
  public static final int TYPE_QUERY_SM = 3;      //Query the delivery status of SMS
  public static final int TYPE_DELIVER_SM = 4;    //Subscribe to delivery notification
  
  private Map<Integer, List<IMIFEService>> mifeServices = new HashMap<Integer, List<IMIFEService>>();
  private static MIFEServicePool MIFE_SERVICE_POOL = null;

  public static synchronized IMIFEService getNextService(int serviceType) throws SMSCException {
    if(MIFE_SERVICE_POOL == null) {
      initServicePool();
    }
    return MIFE_SERVICE_POOL.getNextAvailableService(serviceType);
  }
  
  public static synchronized void releaseService(int serviceType, IMIFEService service) throws SMSCException {
    MIFE_SERVICE_POOL.addServiceBackToList(serviceType, service);
  }
  
  private synchronized void addServiceBackToList(int serviceType, IMIFEService service) throws SMSCException {
    List<IMIFEService> servicesOfType = mifeServices.get(serviceType);
    if(servicesOfType == null) {
      throw new SMSCException("Unrecognized operation type");
    }
    mifeServices.get(serviceType).add(service);
    notifyAll();
  }
  
  private IMIFEService getNextAvailableService(int serviceType) throws SMSCException {
    List<IMIFEService> servicesOfType = mifeServices.get(serviceType);
    if(servicesOfType == null) {
      throw new SMSCException("Unrecognized operation type");
    } else if(servicesOfType.isEmpty()) {//all services of this type are busy. wait until a service is released.
      try {
        wait();
      } catch (InterruptedException e) {
        e.printStackTrace();//TODO handle
        throw new SMSCException(e);
      }
      return getNextService(serviceType);
    } else {
      return servicesOfType.remove(0);
    }
  }
  
  private static void initServicePool() {
    
    MIFE_SERVICE_POOL = new MIFEServicePool();
    
    int numberOfSendSmsServices = Integer.parseInt(PropertyReader.getPropertyValue(SMSCSIMProperties.NUM_SMS_SERVICES));
    int numberOfSendMultiSmsServices = Integer.parseInt(PropertyReader.getPropertyValue(SMSCSIMProperties.NUM_MULTI_SMS_SERVICES));
    int numberOfQuerySmServices = Integer.parseInt(PropertyReader.getPropertyValue(SMSCSIMProperties.NUM_QUERY_SM_SERVICES));
    int numberOfDeliverSmServices = Integer.parseInt(PropertyReader.getPropertyValue(SMSCSIMProperties.NUM_DELIVER_SM_SERVICES));
    
    addServices(SendSMSService.class, MIFEServicePool.TYPE_SEND_SMS, numberOfSendSmsServices);
    addServices(SendMultiSMSService.class, MIFEServicePool.TYPE_SEND_MULTI, numberOfSendMultiSmsServices);
    addServices(QuerySMService.class, MIFEServicePool.TYPE_QUERY_SM, numberOfQuerySmServices);
    addServices(DeliverSMService.class, MIFEServicePool.TYPE_DELIVER_SM, numberOfDeliverSmServices);
    
  }

  private static void addServices(Class<?> cls, int type, int numServices) {
    List<IMIFEService> servicesList = new ArrayList<IMIFEService>();
    IMIFEService aService = (IMIFEService) getNewInstance(cls);
    if(aService != null) {
      servicesList.add(aService);
    }
    MIFE_SERVICE_POOL.mifeServices.put(type, servicesList);
    for(int i = 1; i < numServices; i++) {
      IMIFEService service = (IMIFEService) getNewInstance(cls);
      MIFE_SERVICE_POOL.mifeServices.get(type).add(service);
    }
  }

  private static Object getNewInstance(Class<?> cls) {
    try {
      return cls.getConstructor().newInstance();
    } catch (Exception e) {
      e.printStackTrace();//TODO log needed
      return null;
    }
  }
}
