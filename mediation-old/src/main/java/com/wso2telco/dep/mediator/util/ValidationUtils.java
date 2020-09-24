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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	 * valid msisdn values as follow
	 * 7755, tel:7755, tel:+7755, tel%3A%2B7755, tel%3A7755
	 */
		public static void compareMsisdn(String resourcePath, JSONObject jsonBody) {
		String urlmsisdn = null;
		String payloadMsisdn = null;
		String msisdnVal = null;

		if (resourcePath.contains("transactions")) {
			msisdnVal = resourcePath.substring(1, resourcePath.indexOf("transactions") - 1);

			urlmsisdn = validateMsisdn(msisdnVal);

			payloadMsisdn = validateMsisdn(jsonBody.getJSONObject("amountTransaction").
					getString("endUserId"));

		} else if (resourcePath.contains("outbound")) {
			if (resourcePath.contains("requests")) {
				//use regex matcher
				msisdnVal = resourcePath.substring(
						resourcePath.indexOf("outbound") + 9,resourcePath.indexOf("requests") - 1);

				urlmsisdn = validateMsisdn(msisdnVal);

				payloadMsisdn = validateMsisdn(jsonBody.getJSONObject("outboundSMSMessageRequest").
						getString("senderAddress"));
			} else {
				msisdnVal = resourcePath.substring(
						resourcePath.indexOf("outbound") + 9 );

				urlmsisdn = validateMsisdn(msisdnVal);

				payloadMsisdn = validateMsisdn(jsonBody.getJSONObject("outboundUSSDMessageRequest").
						getString("address"));
			}
		}

		if(urlmsisdn == null){
			log.debug("Not valid msisdn in resourceURL");
			throw new CustomException(MSISDNConstants.SVC0004, "Invalid id found in requested URL %1",
					new String[] {msisdnVal});
        }

        if(payloadMsisdn.equalsIgnoreCase(urlmsisdn.trim()) ){
            log.debug("msisdn in resourceURL and payload msisdn are same");
        } else {
            log.debug("msisdn in resourceURL and payload msisdn are not same");
            throw new CustomException(MSISDNConstants.SVC0002, "", new String[] { "Two different endUserId provided" });
        }
	}

	public static String validateMsisdn(String msisdnVal) {
		String urlmsisdn = null;
		boolean msisdnMatcher = msisdnVal.matches("(?:tel:|tel:\\+|tel%3A%2B|tel%3A)\\d+");
		if (msisdnMatcher) {
			urlmsisdn = msisdnVal.split("(?:tel:\\+|tel:|tel%3A%2B|tel%3A)")[1];
		} else if (msisdnVal.matches("^\\d+")) {
			urlmsisdn = msisdnVal;
		}
		return urlmsisdn;
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
