package com.wso2telco.dep.mediator.util;

import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class has created temporarily
 * Created on 8/15/18.
 */
public final class ValidationUtils {

    static Log log = LogFactory.getLog(ValidationUtils.class);

    public static void compareMsisdns(String urlmsisdn, String payloadMsisdn){
        if(urlmsisdn != null){
            if ((urlmsisdn.startsWith(MSISDNConstants.TEL_1)) || urlmsisdn.startsWith(MSISDNConstants.ETEL_1)) {
                urlmsisdn = urlmsisdn.substring(5);
            } else if (urlmsisdn.startsWith(MSISDNConstants.TEL_2)|| urlmsisdn.startsWith(MSISDNConstants.ETEL_2)) {
                urlmsisdn = urlmsisdn.substring(4);
            } else if (urlmsisdn.startsWith(MSISDNConstants.TEL_3) || urlmsisdn.startsWith(MSISDNConstants.ETEL_3)) {
                urlmsisdn = urlmsisdn.substring(3);
            } else if (urlmsisdn.startsWith(MSISDNConstants.PLUS)) {
                urlmsisdn = urlmsisdn.substring(1);
            }
        } else {
           log.debug("Not valid msisdn in resourceURL");
        }

        if(payloadMsisdn.equalsIgnoreCase(urlmsisdn) ){
            log.debug("msisdn in resourceURL and payload msisdn are same");
        } else {
            log.debug("msisdn in resourceURL and payload msisdn are not same");
            throw new CustomException(MSISDNConstants.SVC0002, "", new String[] { "Two different endUserId provided" });
        }
    }
}
