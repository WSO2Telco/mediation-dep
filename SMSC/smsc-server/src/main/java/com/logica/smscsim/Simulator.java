/*
 * Copyright (c) 1996-2001
 * Logica Mobile Networks Limited
 * All rights reserved.
 *
 * This software is distributed under Logica Open Source License Version 1.0
 * ("Licence Agreement"). You shall use it and distribute only in accordance
 * with the terms of the License Agreement.
 *
 */
package com.logica.smscsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;

import com.logica.smpp.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.dialog.smsc.db.DBConnectionPool;
import lk.dialog.smsc.mife.request.subscription.inboundmo.InboundSMSMessage;
import lk.dialog.smsc.mife.request.subscription.inboundmo.InboundSMSMessageNotificationRequest;
import lk.dialog.smsc.shutdown.SMSCShutdownHook;
import lk.dialog.smsc.startup.SMSCSIMInitiator;
import lk.dialog.smsc.util.PropertyReader;
import lk.dialog.smsc.util.SMSCSIMProperties;

import com.google.gson.Gson;
import com.logica.smpp.SmppObject;
import com.logica.smpp.debug.Debug;
import com.logica.smpp.debug.Event;
import com.logica.smpp.debug.FileDebug;
import com.logica.smpp.debug.FileEvent;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.WrongLengthOfStringException;
import com.logica.smscsim.util.BasicTableParser;
import com.logica.smscsim.util.Table;

/**
 * Class <code>Simulator</code> is an application class behaving as a real
 * SMSC with SMPP interface.
 * Clients (ESMEs) can bind to it, send requests to which this application
 * generates responses. It also allows to send message to the bound client.
 * It's primary use is for developers creating their SMPP applications to lessen
 * the use of real SMSC. Should any extra functionality is required,
 * the developers can add it to this application. Multiple clients are supported.
 * Transmitter/receiver/transciever bound modes are supported. The bounding clients
 * are authenticated using text file with user definitions.
 * <p>
 * This simulator application uses <code>SimulatorPDUProcessor</code> to process
 * the PDUs received from the clients.
 * <p>
 * To run this application using <b>smpp.jar</b> and <b>smscsim.jar</b> library files execute
 * the following command:
 * <p>
 * <code>java -cp smpp.jar:smscsim.jar com.logica.smscsim.Simulator</code>
 * <p>
 * If your libraries are stored in other that default directory, use the
 * directory name in the <code>-cp</code> argument.
 * 
 * @author Logica Mobile Networks SMPP Open Source Team
 * @version 1.1, 26 Sep 2001
 * @see SimulatorPDUProcessor
 * @see SimulatorPDUProcessorFactory
 * @see SMSCListener
 * @see SMSCSession
 * @see BasicTableParser
 */

/*
  20-09-01 ticp@logica.com added support for sending of delivery info
  26-09-01 ticp@logica.com debug now in a group
*/

public class Simulator
{
	
	static Logger logger = LoggerFactory.getLogger( Simulator.class );
	
    static final String copyright =
        "Copyright (c) 1996-2001 Logica Mobile Networks Limited\n"+
        "This product includes software developed by Logica by whom copyright\n"+
        "and know-how are retained, all rights reserved.\n";

    static {
        //System.out.println(copyright);
        try {
        		Class.forName("lk.dialog.smsc.startup.SMSCSIMInitiator");
        } catch (ClassNotFoundException e) {
        		logger.error( "lk.dialog.smsc.startup.SMSCSIMInitiator class not found", e );
        		e.printStackTrace();
        }
    }

    /**
     * Name of file with user (client) authentication information.
     */
    static String usersFileName = "users.txt";
    static {
      try {
        usersFileName =  new File("./conf/users.txt").getCanonicalPath();
      } catch (IOException e) {
    	  logger.error( "Error occured reading users.txt", e );
    	  e.printStackTrace();
      }
    }
    /**
     * Directory for creating of debug and event files.
     */
    static final String dbgDir = "./";

    /**
     * The debug object.
     */
    static Debug debug = new FileDebug(dbgDir,"sim.dbg");

    /**
     * The event object.
     */
    static Event event = new FileEvent(dbgDir,"sim.evt");

    public static final int DSIM = 16;
    public static final int DSIMD = 17;
    public static final int DSIMD2 = 18;

    static BufferedReader keyboard =
        new BufferedReader(new InputStreamReader(System.in));

    boolean keepRunning = true;
    private SMSCListener smscListener = null;
    private SimulatorPDUProcessorFactory factory = null;
    private PDUProcessorGroup processors = null;
    private ShortMessageStore messageStore = null;
    private DeliveryInfoSender deliveryInfoSender = null;
    private Table users = null;
    private boolean displayInfo = true;

    private Simulator()
    {
    }

    
    /**
     * The main function of the application displays menu with available
     * options.
     */
    public static void main(String args[]) throws IOException
    {
        SmppObject.setDebug(debug);
        SmppObject.setEvent(event);
        debug.activate();
        event.activate();
        debug.deactivate(SmppObject.DRXTXD2);
        debug.deactivate(SmppObject.DPDUD);
        debug.deactivate(SmppObject.DCOMD);
        debug.deactivate(DSIMD2);
        Simulator menu = new Simulator();
        menu.start();
        SMSCSIMInitiator.init( menu ); //Message reciever
        SMSCShutdownHook objShutdownHook = new SMSCShutdownHook();
        objShutdownHook.attachShutDownHook( menu );
        //menu.menu();
    }
    
    /**
     * Displays menu with available simulator options such as starting and
     * stopping listener, listing all currently connected clients,
     * sending of a message to a client, listing all received messages
     * and reloading of user (client) definition file.
     */
    protected void menu() throws IOException
    {
        debug.write("Simulator started");

        keepRunning = true;
        String option = "1";
        int optionInt;
        
        while (keepRunning) {
        	/*
            System.out.println();
            System.out.println("- 1 Start SMSC");
            System.out.println("- 2 Stop SMSC");
            System.out.println("- 3 list clients");
            System.out.println("- 4 send message");
            System.out.println("- 5 send bulk messages"); 
            System.out.println("- 6 list messages");
            System.out.println("- 7 list messages count");
            System.out.println("- 8 save messages");
            System.out.println("- 9 reload users file");
            System.out.println("- 10 log to screen "+
			       (displayInfo ? "off":"on"));
            System.out.println("- 0 Exit");
            System.out.print("> ");
            optionInt = -1;
            */
            try {
                option = keyboard.readLine();
                optionInt = Integer.parseInt(option);
            } catch (Exception e) {
                debug.write("exception reading keyboard " + e);
                optionInt = -1;
            }
            switch (optionInt) {
            /*
            case 1:
                start();
                SMSCSIMInitiator.init(this); //Message reciever
                break;
            case 2:
                stop();
                break;
            case 3:
                listClients();
                break;
            case 4:
                sendMessage();
                break;
            case 5:
                sendBulkMessage();
                break;
            case 6:
                messageList();
                break;
            case 7:
                listMessageCount();
                break;
            case 8:
                saveMessages();
                break;
            case 9:
                reloadUsers();
                break;
            case 10:
                logToScreen();
                break;
            */
            case 0:
                exit();
                break;
            case -1:
                // default option if entering an option went wrong
                break;
            default:
                System.out.println("Invalid option. Choose between 0 and 2.");
                break;
            }
        }

        System.out.println("Exiting SMSC.");
        debug.write("SMSC exited.");
    }

    /**
     * Permits a user to choose the port where to listen on and then creates and
     * starts new instance of <code>SMSCListener</code>.
     * An instance of the <code>SimulatorPDUProcessor</code> is created 
     * and this instance is passed to the <code>SMSCListener</code> which is started
     * just after.
     */
    protected void start() throws IOException
    {
        if (smscListener == null) {
        	/*
            System.out.print("Enter port number> ");
            int port = Integer.parseInt(keyboard.readLine());
            */
        	int port = Integer.parseInt( PropertyReader.getPropertyValue("smsc_port") );
        	System.out.println("Starting at port "+port);
        	logger.info("Starting SMSC at port "+port);
            System.out.print("Starting listener...");
            
            smscListener = new SMSCListener(port,true);
            processors = new PDUProcessorGroup();
            messageStore = new ShortMessageStore();
            deliveryInfoSender = new DeliveryInfoSender();
            deliveryInfoSender.start();
            users = new Table(usersFileName);
            factory = new SimulatorPDUProcessorFactory( processors, messageStore, deliveryInfoSender, users );
            factory.setDisplayInfo(displayInfo);
            smscListener.setPDUProcessorFactory(factory);
            smscListener.start();
            System.out.println("Started.");
            logger.info("Started.");
            
        } else {
            System.out.println("Listener is already running.");
            logger.info("Listener is already running.");
        }
    }
    
    /**
     * Stops all the currently active sessions and then stops the listener.
     */
    protected void stop() throws IOException
    {
        if (smscListener != null) {
        	
        	logger.info("Stopping listener...");
            System.out.println("Stopping listener...");
            
            synchronized (processors) {
            	
                int procCount = processors.count();
                SimulatorPDUProcessor proc;
                SMSCSession session;
                
                for(int i=0; i<procCount; i++) {
                	
                    proc = (SimulatorPDUProcessor)processors.get(i);
                    session = proc.getSession();
                    
                    logger.info("Stopping session "+i+": "+proc.getSystemId() + " ...");
                    System.out.print("Stopping session "+i+": "+proc.getSystemId() + " ...");
                    
                    session.stop();
                    logger.info("Stopped.");
                    System.out.println("Stopped.");
                    
                }
            }
            
            smscListener.stop();
            smscListener = null;
            
            if (deliveryInfoSender!=null) {
                deliveryInfoSender.stop();
            }
            
            logger.info("Stopped.");
            System.out.println("Stopped.");
            
        }
    }

    /**
     * Stops all the currently active sessions, stops the listener
     * and the exits the application.
     */
    //protected void exit() throws IOException
    public void exit() throws IOException
    {
        stop();
        keepRunning = false;
    }
    
    /**
     * Prints all messages currently present in the message store
     * on the standard output.
     */
    protected void messageList()
    {
        if (smscListener != null) {
            messageStore.print();
        } else {
            System.out.println("You must start listener first.");
        }
    }
    
    protected void listMessageCount()
    {
        if (smscListener != null) {
            System.out.println("Message Count: "+messageStore.getMessageCount());;
        } else {
            System.out.println("You must start listener first.");
        }
    }
    
    protected void saveMessages()
    {
        if (smscListener != null) {
            messageStore.writeToFile();
        } else {
            System.out.println("You must start listener first.");
        }
    }
    /**
     * Reloads the user (client) definition file used for authentication of
     * bounding ESMEs. Useful when the user setting is changed or added
     * and restart of the simulator is not possible.
     */
    protected void reloadUsers()
    {
        if (smscListener != null) {
            try {
                if (users != null) {
                    users.reload();
                } else {
                    users = new Table(usersFileName);
                }
                System.out.println("Users file reloaded.");
            } catch (FileNotFoundException e) {
                event.write(e,"reading users file " + usersFileName);
            } catch (IOException e) {
                event.write(e,"reading users file " + usersFileName);
            }
        } else {
            System.out.println("You must start listener first.");
        }
    }

    /**
     * Changes the log to screen status. If logging to screen,
     * an information about received and sent PDUs as well as about
     * connection attempts is printed to standard output.
     */
    protected void logToScreen()
    {
        if (smscListener != null) {
            synchronized (processors) {
		displayInfo = !displayInfo;
                int procCount = processors.count();
                SimulatorPDUProcessor proc;
                for(int i=0; i<procCount; i++) {
                    proc = (SimulatorPDUProcessor)processors.get(i);
                    proc.setDisplayInfo(displayInfo);
                }
            }
            factory.setDisplayInfo(displayInfo);
        }
    }

    /**
     * Prints all currently connected clients on the standard output.
     */
    protected void listClients()
    {
        if (smscListener != null) {
            synchronized (processors) {
                int procCount = processors.count();
                if (procCount > 0) {
                    SimulatorPDUProcessor proc;
                    for(int i=0; i<procCount; i++) {
                        proc = (SimulatorPDUProcessor)processors.get(i);
                        System.out.print(proc.getSystemId());
                        if (!proc.isActive()) {
                            System.out.println(" (inactive)");
                        } else {
                            System.out.println();
                        }
                    }
                } else {
                    System.out.println("No client connected.");
                }
            }
        } else {
            System.out.println("You must start listener first.");
        }
    }
    
    /**
     * Permits data to be sent to a specific client.
     * With the id of the client set by the user, the method <code>sendMessage</code> 
     * gets back the specific reference to the client's <code>PDUProcessor</code>.
     * With this reference you are able to send data to the client.
     */
    protected void sendMessage() throws IOException
    {
        if (smscListener != null) {
            int procCount = processors.count();
            if (procCount > 0) {
                String client;
                SimulatorPDUProcessor proc;
                listClients();
                if (procCount > 1) {
                    System.out.print("Type name of the destination> ");
                    client = keyboard.readLine();
                } else {
                    proc = (SimulatorPDUProcessor)processors.get(0);
                    client = proc.getSystemId();
                }
                for(int i=0; i<procCount; i++) {
                    proc = (SimulatorPDUProcessor)processors.get(i);
                    if (proc.getSystemId().equals(client)) {
                        if (proc.isActive()) {
                            System.out.print("Type the message> ");
                            String message = keyboard.readLine();
                            DeliverSM request = new DeliverSM();
                            try {
                                request.setShortMessage(message);
                                //request. TODO :: Use this code to send SMS for Deliver SM and SMS - deliver_sm
                                proc.serverRequest(request);
                                System.out.println("Message sent.");
                            } catch (WrongLengthOfStringException e) {
                                System.out.println("Message sending failed");
                                event.write(e, "");
                            }
                        } else {
                            System.out.println("This session is inactive.");
                        }
                    }
                }
            } else {
                System.out.println("No client connected.");
            }
        } else {
            System.out.println("You must start listener first.");
        }
    }
    /**
     * 
     * @throws IOException
     */
    protected void sendBulkMessage() throws IOException
    {
        if (smscListener != null) {
            int procCount = processors.count();
            if (procCount > 0) {
                String client;
                SimulatorPDUProcessor proc;
                listClients();
                if (procCount > 1) {
                    System.out.print("Type name of the destination> ");
                    client = keyboard.readLine();
                } else {
                    proc = (SimulatorPDUProcessor)processors.get(0);
                    client = proc.getSystemId();
                }
                for(int i=0; i<procCount; i++) {
                    proc = (SimulatorPDUProcessor)processors.get(i);
                    if (proc.getSystemId().equals(client)) {
                        if (proc.isActive()) {
                            System.out.print("Type the message> ");
                            String message = keyboard.readLine();
                            
                            int count = -1;                            
                            while(count==-1) {
                                try{
                                    System.out.print("Iterations> ");
                                    String iterations = keyboard.readLine();
                                    count = Integer.parseInt(iterations);
                                }
                                catch (Exception e) {
                                    System.out.print("Invalid number of iterations.");                                    
                                }
                            }
                            
                            int throughput = -1;                            
                            while(throughput==-1) {
                                try{
                                    System.out.print("throughput (in mps)> ");
                                    String strThroughput = keyboard.readLine();
                                    throughput = Integer.parseInt(strThroughput);
                                }
                                catch (Exception e) {
                                    System.out.print("Invalid throughput.");                                    
                                }
                            }
                            
                            DeliverSM request = new DeliverSM();
                            try {                               
                                int t = 0;
                                for(int x=0;x<count;x++) {
                                    long st = System.currentTimeMillis();
                                    t++;
                                    request.setShortMessage(message+":"+x);
                                    proc.serverRequest(request);
                                    if(t==throughput) {
                                        try {
                                            long et = System.currentTimeMillis();
                                            if( (1000-(et-st)) > 0) {
                                                Thread.sleep((1000-(et-st)));
                                            }
                                            t=0;
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                System.out.println("Messages sent.");
                            } catch (WrongLengthOfStringException e) {
                                System.out.println("Message sending failed");
                                event.write(e, "");
                            }
                        } else {
                            System.out.println("This session is inactive.");
                        }
                    }
                }
            } else {
                System.out.println("No client connected.");
            }
        } else {
            System.out.println("You must start listener first.");
        }
    }
    
    /*
     * Note :: Sending Delivery notification and MO messages
     * */
    public void sendQuedMessage( String strMsgType, String requestParams, String refNumber, String requestBody ,String sme) throws IOException
    {
        if (smscListener != null) {
            int procCount = processors.count();
            if (procCount > 0) {
            	
                String client;
                SimulatorPDUProcessor proc;
                listClients();
                
                if (procCount > 1) {
                    client = sme;
                } else {
                    proc = (SimulatorPDUProcessor)processors.get(0);
                    client = proc.getSystemId();
                }
                
                for(int i=0; i<procCount; i++) {
                    proc = (SimulatorPDUProcessor)processors.get(i);
                    if (proc.getSystemId().equals(client)) {
                        if (proc.isActive()) {

                            try {

                            	DeliverSM request = new DeliverSM();
                            	Gson gson = new Gson();
                            	
                            	String strSourceAddress, strDestinationAddress, strShortMsg = null;
                            	
                            	//String refNumber = null;
                            	boolean isDeliveryNotif = strMsgType.equalsIgnoreCase("DELIVERY_NOTIF");
                            	if(isDeliveryNotif){
                            		
                            		//DeliveryInfoNotificationRequest objDeliveryNotification = gson.fromJson( requestParams, DeliveryInfoNotificationRequest.class );
                            		//DeliveryInfoNotification objDeliNote = objDeliveryNotification.getDeliveryInfoNotification();
                            		
                            		//refNumber = objDeliNote.getCallbackData().split(",")[1];
                                	request.setDestAddr(PropertyReader.getPropertyValue(SMSCSIMProperties.DESTINATION_ADDRESS));
                                	//request.setShortMessage( "Message recipient :"+objDeliNote.getDeliveryInfo().getAddress()+"\n"+objDeliNote.getDeliveryInfo().getDeliveryStatus() );
                                	request.setShortMessage( requestBody );
                                	request.setEsmClass(Data.SM_SME_ACK_DELIVERY_REQUESTED);
                                	//logger.info("Simulator::sendQuedMessage [DELIVERY_NOTIF]-> "+  "Message recipient :"+objDeliNote.getDeliveryInfo().getAddress()+"\n"+objDeliNote.getDeliveryInfo().getDeliveryStatus() );

                                }else if( strMsgType.equalsIgnoreCase("MO_SMS") ){

                                	InboundSMSMessageNotificationRequest objInboundSMS = gson.fromJson( requestParams, InboundSMSMessageNotificationRequest.class ); 
                                	InboundSMSMessage objSMSMsg = objInboundSMS.getInboundSMSMessageNotification().getInboundSMSMessage();
                                	refNumber = objSMSMsg.getMessageId();
                                	strShortMsg = objSMSMsg.getMessage();
                                	strSourceAddress = objSMSMsg.getSenderAddress();
                                	strDestinationAddress = objSMSMsg.getDestinationAddress();
                                	
                                	logger.info("Simulator::sendQuedMessage [MO_SMS]-> "+"Source : "+request.getSourceAddr().getAddress()+" Dest : "+request.getDestAddr().getAddress() );
                                	System.out.println("Source : "+request.getSourceAddr().getAddress()+" Dest : "+request.getDestAddr().getAddress() );
                                	
                                	request.setSourceAddr( strSourceAddress );
                                	request.setDestAddr( strDestinationAddress );
                                	request.setShortMessage( strShortMsg );
                                }

                                proc.serverRequest(request);
                                updateMoSMSTable(refNumber, isDeliveryNotif);
                                
                                logger.info("Simulator::sendQuedMessage - Message sent.");
                                System.out.println("Message sent.");
                                
                            } catch (WrongLengthOfStringException e) {
                            	logger.error("Simulator::sendQuedMessage - Message sending failed.", e);
                                System.out.println("Message sending failed");
                                event.write(e, "");
                            }
                            
                        } else {
                        	logger.info("Simulator::sendQuedMessage - This session is inactive.");
                            System.out.println("This session is inactive.");
                        }
                    }
                }
                
            } else {
            	logger.info("Simulator::sendQuedMessage - No client connected.");
                System.out.println("No client connected.");
            }
            
        } else {
        	logger.info("Simulator::sendQuedMessage - You must start listener first.");
            System.out.println("You must start listener first.");
        }
    }

    
    private void updateMoSMSTable(String refNumber, boolean isDeliveryNotif) {
      try {
        Connection connection = DBConnectionPool.getConnection();
        String tableName = isDeliveryNotif ? "delivery_notif" : "mo_sms";
        String sql = null;
        
        if( isDeliveryNotif ){
        	 sql = "UPDATE " + tableName + " SET delivered = 1 WHERE id = ?";
        }else{
        	 sql = "UPDATE " + tableName + " SET delivered = 1 WHERE ref_num = ?";
        }
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, refNumber);
        stmt.executeUpdate();
        DBConnectionPool.releaseConnection(connection);
        
      } catch (Exception e) {
    	  
    	  logger.error("Simulator::updateMoSMSTable", e);
          e.printStackTrace();
          
      }
    }

}
