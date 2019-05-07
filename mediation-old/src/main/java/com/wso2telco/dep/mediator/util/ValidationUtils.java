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
import com.wso2telco.dep.oneapi.constant.payment.PaymentKeyConstants;
import com.wso2telco.dep.oneapi.constant.payment.PaymentResourceConstants;
import com.wso2telco.dep.oneapi.constant.ussd.USSDKeyConstants;
import com.wso2telco.dep.oneapi.constant.ussd.USSDResourceConstants;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 *@author WSO2telco
 * Created on 2018/08/15
 *
 */
public final class ValidationUtils {

	static Log log = LogFactory.getLog(ValidationUtils.class);

	/**
	 * This method extracts userId from payload and resource url and passed to
	 * validate whether they are same. Deprecated due to the api has the only support for {@link APIType#PAYMENT}
	 *
	 * @param resourcePath
	 * @param jsonBody
	 */
	@Deprecated
	public static void compareMsisdn(String resourcePath, JSONObject jsonBody) {
		compareMsisdn(resourcePath, jsonBody, APIType.PAYMENT);
	}

	/**
	 * This method extracts userId from payload and resource url and passed to
	 * validate whether they are same
	 */
	public static void compareMsisdn(String resourcePath, JSONObject jsonBody, APIType apiType) {

		String urlmsisdn = resolveURLMsisdn(resourcePath, apiType);

		String payloadMsisdn = resolvePayloadMsisdn(jsonBody, apiType);

		if (urlmsisdn != null) {
			urlmsisdn = getMsisdnNumber(urlmsisdn);
		} else {
			log.debug("Not valid msisdn in resourceURL");
			throw new CustomException(MSISDNConstants.SVC0002, "", new String[]{"Not valid msisdn in URL"});
		}

		if (payloadMsisdn.equalsIgnoreCase(urlmsisdn.trim())) {
			log.debug("msisdn in resourceURL and payload msisdn are same");
		} else {
			log.debug("msisdn in resourceURL and payload msisdn are not same");
			throw new CustomException(MSISDNConstants.SVC0002, "",
					new String[]{"Two different MSISDNs provided in resourceURL and payload"});
		}
	}

		/**
	 * Resolve msisdn from the url
	 *
	 * @param resourcePath
	 * @param apiType
	 * @return
	 */
	private static String resolveURLMsisdn(String resourcePath, APIType apiType) {

		switch (apiType) {
			case PAYMENT:
				return decodeMsisdnFromPaymentResourcePathh(resourcePath);
			case USSD:
				return decodeMsisdnFromUssdResourcePath(resourcePath);
			default:
				throw new UnsupportedOperationException("Not supported yet.");
		}
	}

	/**
	 * decode msisdn from ussd resource path
	 *
	 * @param resourcePath
	 * @return
	 */
	private static String decodeMsisdnFromUssdResourcePath(String resourcePath) {
		try {
			return URLDecoder.decode((resourcePath.substring(resourcePath.indexOf(USSDResourceConstants.RESOURCE_PATH_OUTBOUND) +
					USSDResourceConstants.RESOURCE_PATH_OUTBOUND.length() + 1)), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.debug("Url MSISDN can not be decoded.");
		}

		return null;
	}

	/**
	 * decode msisdn from payment resource path
	 *
	 * @param resourcePath
	 * @return
	 */
	private static String decodeMsisdnFromPaymentResourcePathh(String resourcePath) {
		try {
			return URLDecoder.decode(resourcePath.substring(1,
					resourcePath.indexOf(PaymentResourceConstants.RESOURCE_PATH_TRANSACTIONS) - 1), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.debug("Url MSISDN can not be decoded.");
		}

		return null;
	}

	/**
	 * This validation assumes that userID should be with the prefix "tel:+" and back end
	 * still does not support with other prefixes for this API.
	 *
	 * @param jsonBody
	 * @param apiType
	 * @return
	 */
    private static String resolvePayloadMsisdn(JSONObject jsonBody, APIType apiType) {
        switch (apiType) {
            case USSD:
                return getMsisdnNumber(jsonBody.getJSONObject(USSDKeyConstants.KEY_OUT_BOUND_USSD_MESSAGE_REQUEST).
                        getString(USSDKeyConstants.KEY_ADDRESS));
            case PAYMENT:
                return getMsisdnNumber(jsonBody.getJSONObject(PaymentKeyConstants.KEY_AMOUNT_TRANSACTION).
                        getString(PaymentKeyConstants.KEY_END_USER_ID));
            default:
                throw new UnsupportedOperationException("The API type not supported yet.");
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
