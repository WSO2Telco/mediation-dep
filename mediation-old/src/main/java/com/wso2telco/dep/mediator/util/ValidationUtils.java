/*******************************************************************************
 * Copyright  (c) 2018, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 *
 *  WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.wso2telco.dep.mediator.util;

import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import com.wso2telco.dep.user.masking.UserMaskHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.json.JSONObject;

/**
 *@author WSO2telco
 * Created on 2018/08/15
 *
 */
public final class ValidationUtils {

    static Log log = LogFactory.getLog(ValidationUtils.class);

    /**
	 * This method extracts userId from payload and resource url and passed to
	 * validate whether they are same
	 */
	public static void compareMsisdn(String resourcePath, JSONObject jsonBody, boolean userAnonymization, MessageContext context) {
		String urlmsisdn = null;
		try {
			urlmsisdn = URLDecoder.decode(resourcePath.substring(1,
					resourcePath.indexOf("transactions") - 1), "UTF-8");
			if (userAnonymization) {
				try {
					urlmsisdn = UserMaskHandler.maskUserId(urlmsisdn, false, (String)context.getProperty("USER_MASKING_SECRET_KEY"));
				} catch (Exception e) {
					log.debug("Error while decoeing user ID");
				}
			}

		} catch (UnsupportedEncodingException e) {
			log.debug("Url MSISDN can not be decoded ");
		}
		// This validation assumes that userID should be with the prefix "tel:+" and back end
		// still does not support with other prefixes for this API.
		// Therefore below line should be modified in future depending on requirements
		String payloadMsisdn = jsonBody.getJSONObject("amountTransaction").getString("endUserId");
		if (userAnonymization) {
			try {
				payloadMsisdn = UserMaskHandler.maskUserId(payloadMsisdn, false, (String)context.getProperty("USER_MASKING_SECRET_KEY"));
			} catch (Exception e) {
				log.debug("Error while decoeing user ID");
			}
		}
		payloadMsisdn = payloadMsisdn.substring(5);
		compaireUserIds(urlmsisdn, payloadMsisdn, "endUserId");
	}


	/**
	 * This method extracts SenderId of sendSMS API from payload and resource url and passed to
	 * validate whether they are same
	 */
	public static void compareSenderId(String resourcePath, JSONObject jsonBody, MessageContext context) {
		String urlmsisdn = null;
		try {
			urlmsisdn = URLDecoder.decode(resourcePath.substring(resourcePath.indexOf("outbound") + 9, resourcePath.indexOf("requests") - 1), "UTF-8");

		} catch (UnsupportedEncodingException e) {
			log.debug("Url MSISDN can not be decoded ");
		}
		compaireUserIds(urlmsisdn, jsonBody.getJSONObject("outboundSMSMessageRequest").getString("senderAddress").substring(5), "senderAddress");
	}

	private static void compaireUserIds(String urlmsisdn, String payloadMsisdn, String inputName) {
		if(urlmsisdn != null){
			urlmsisdn = getMsisdnNumber(urlmsisdn);
		} else {
			log.debug("Not valid msisdn in resourceURL");
			throw new CustomException(MSISDNConstants.SVC0002, "", new String[] {"Not valid msisdn in URL"});
		}

		if(payloadMsisdn.equalsIgnoreCase(urlmsisdn.trim()) ){
			log.debug("msisdn in resourceURL and payload msisdn are same");
		} else {
			log.debug("msisdn in resourceURL and payload msisdn are not same");
			throw new CustomException(MSISDNConstants.SVC0002, "", new String[] { "Two different " + inputName + " provided" });
		}
	}

    /**
     * Returns array of MSISDNs without "tel:+" prefix
     */
	public static String[] getUserMsisdns(String[] msisdns) {
		List<String> userMsisdn = new ArrayList<String>();
		for(String msisdn  : msisdns) {
			userMsisdn.add(getMsisdnNumber(msisdn));
		}
		return userMsisdn.toArray(new String[userMsisdn.size()]);
	}
	
	/**
     * Returns array of MSISDNs without "tel:" prefix
     */
	public static String[] getQueryMsisdns(String[] msisdns) {
		List<String> qurMsisdn = new ArrayList<String>();
		for(String msisdn  : msisdns) {
			qurMsisdn.add(getMsisdnNumberWithPlus(msisdn));
		}
		return qurMsisdn.toArray(new String[qurMsisdn.size()]);
	}
	
	/**
	 * Returns MSISDN number without prefix
	 */
	public static String getMsisdnNumber(String msisdn) {
		if (msisdn.startsWith(MSISDNConstants.ETEL_1)) {
			msisdn = msisdn.substring(6).trim();
    	} else if ((msisdn.startsWith(MSISDNConstants.TEL_1)) || msisdn.startsWith(MSISDNConstants.ETEL_2)) {
    		msisdn = msisdn.substring(5).trim();
        } else if (msisdn.startsWith(MSISDNConstants.TEL_2)|| msisdn.startsWith(MSISDNConstants.ETEL_3)) {
        	msisdn = msisdn.substring(4);
        } else if (msisdn.startsWith(MSISDNConstants.TEL_3)) {
        	msisdn = msisdn.substring(3);
        } else if (msisdn.startsWith(MSISDNConstants.PLUS)) {
        	msisdn = msisdn.substring(1);
        }
		return msisdn;
	}
	
	/**
	 * Returns MSISDN number only with "+" prefix
	 */
	public static String getMsisdnNumberWithPlus(String msisdn) {
		if (msisdn.contains(MSISDNConstants.PLUS)) {
			msisdn = msisdn.substring(msisdn.lastIndexOf(MSISDNConstants.PLUS));
    	}
		return msisdn;
	}
}
