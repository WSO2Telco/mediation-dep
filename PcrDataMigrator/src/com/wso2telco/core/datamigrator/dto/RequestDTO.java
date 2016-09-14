package com.wso2telco.core.datamigrator.dto;

// TODO: Auto-generated Javadoc
/**
 * The Class RequestDTO.
 */
public class RequestDTO {

	/** The msisdn. */
	private String msisdn;
	
	/** The application name. */
	private String applicationName;

	/** The consumer key. */
	private String consumerKey;
	
	/** The callbackurl. */
	private String callbackurl;
	
	/**
	 * Gets the msisdn.
	 *
	 * @return the msisdn
	 */
	public String getMsisdn() {
		return msisdn;
	}
	
	/**
	 * Sets the msisdn.
	 *
	 * @param msisdn the new msisdn
	 */
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	
	/**
	 * Gets the consumer key.
	 *
	 * @return the consumer key
	 */
	public String getConsumerKey() {
		return consumerKey;
	}
	
	/**
	 * Sets the consumer key.
	 *
	 * @param consumerKey the new consumer key
	 */
	public void setConsumerKey(String consumerKey) {
		this.consumerKey = consumerKey;
	}
	
	/**
	 * Gets the callbackurl.
	 *
	 * @return the callbackurl
	 */
	public String getCallbackurl() {
		return callbackurl;
	}
	
	/**
	 * Sets the callbackurl.
	 *
	 * @param callbackurl the new callbackurl
	 */
	public void setCallbackurl(String callbackurl) {
		this.callbackurl = callbackurl;
	}
	
	/**
	 * Gets the application name.
	 *
	 * @return the application name
	 */
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * Sets the application name.
	 *
	 * @param applicationName the new application name
	 */
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
}
