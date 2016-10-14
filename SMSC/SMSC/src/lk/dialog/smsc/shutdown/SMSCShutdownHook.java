/*
 * shirhan@inovaitsys.com
 * Note : Shutdown hook to capture the exit strategy of the simulator
 * */
package lk.dialog.smsc.shutdown;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logica.smscsim.Simulator;

public class SMSCShutdownHook {

		static Logger logger = LoggerFactory.getLogger( SMSCShutdownHook.class );
	
		 public void attachShutDownHook( final Simulator objSMSC ){

			  Runtime.getRuntime().addShutdownHook( new Thread() {
				   @Override
				   public void run() {
					   
					   try {
						objSMSC.exit();
					} catch (IOException e) {
						logger.error("SMSCShutdownHook::attachShutDownHook", e );
						e.printStackTrace();
						//System.out.println("Exception :: attachShutDownHook"+ e.getMessage());
					}
					   System.out.println("Exiting gracefully!");
					   logger.info("Exiting gracefully!");
				   }
			  });

			  System.out.println("Shut Down Hook Attached.");
			  logger.info("Shut Down Hook Attached.");
		 }
		 
}
