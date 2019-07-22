/*
 *
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package com.wso2telco.dep.mediator;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.dep.mediator.entity.smsmessaging.southbound.InboundSMSMessage;
import com.wso2telco.dep.mediator.entity.smsmessaging.southbound.InboundSMSMessageList;
import com.wso2telco.dep.mediator.entity.smsmessaging.southbound.SouthboundRetrieveResponse;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.mediator.unmarshaler.GroupDTO;
import com.wso2telco.dep.mediator.unmarshaler.GroupEventUnmarshaller;
import com.wso2telco.dep.mediator.unmarshaler.OparatorNotinListException;
import com.wso2telco.dep.mediator.util.ConfigFileReader;
import com.wso2telco.dep.mediator.util.DataPublisherConstants;
import com.wso2telco.dep.mediator.util.MediationHelper;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.exceptions.RequestError;
import com.wso2telco.dep.oneapivalidation.exceptions.ResponseError;
import com.wso2telco.dep.operatorservice.exception.OperatorServiceException;
import com.wso2telco.dep.operatorservice.model.Operator;
import com.wso2telco.dep.operatorservice.model.OperatorAppSearchDTO;
import com.wso2telco.dep.operatorservice.model.OperatorApplicationDTO;
import com.wso2telco.dep.operatorservice.service.OparatorService;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONException;
import org.json.JSONObject;

// TODO: Auto-generated Javadoc

/**
 * The Class RequestExecutor.
 */
public abstract class RequestExecutor {

	/** The log. */
	private static Log log = LogFactory.getLog(RequestExecutor.class);

	/** The validoperators. */
	private static List<OperatorApplicationDTO> validoperators = null;

	// Operator name and Details.
	private static Map<String,Operator> operatorMap = null;

	/** The http method. */
	private String httpMethod;

	/** The json body. */
	private JSONObject jsonBody;

	/** The sub resource path. */
	private String subResourcePath;
	// others methods to be implementd
	/** The str err. */
	// <TO-DO>
	private String strErr;

	/**
	 * Gets the str err.
	 *
	 * @return the str err
	 */
	public String getStrErr() {
		return strErr;
	}

	/**
	 * Sets the str err.
	 *
	 * @param strErr
	 *            the new str err
	 */
	public void setStrErr(String strErr) {
		this.strErr = strErr;
	}

	/** The resource url. */
	protected String resourceUrl;

	/**
	 * Gets the resource url.
	 *
	 * @return the resource url
	 */
	public String getResourceUrl() {
		return resourceUrl;
	}

	/**
	 * Sets the resource url.
	 *
	 * @param resourceUrl
	 *            the new resource url
	 */
	public void setResourceUrl(String resourceUrl) {
		this.resourceUrl = resourceUrl;
	}

	/** The applicationid. */
	String applicationid;

	/**
	 * Gets the applicationid.
	 *
	 * @return the applicationid
	 */
	public String getApplicationid() {
		return applicationid;
	}

	/**
	 * Sets the applicationid.
	 *
	 * @param applicationid
	 *            the new applicationid
	 */
	public void setApplicationid(String applicationid) {
		this.applicationid = applicationid;
	}

	/**
	 * The api version
	 */
	String apiVersion;

	/**
	 * Gets the apiversion.
	 *
	 * @return apiVersion
	 */
	public String getApiVersion() {
		return apiVersion;
	}

	/**
	 * Set the apiVersion
	 *
	 * @param apiVersion
	 */
	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	/**
	 * The api context
	 */
	String apiContext;

	/**
	 * Gets the apiContext
	 *
	 * @return
	 */
	public String getApiContext() {
		return apiContext;
	}

	/**
	 * Set the  apiContext
	 *
	 * @param apiContext
	 */
	public void setApiContext(String apiContext) {
		this.apiContext = apiContext;
	}

	/**
	 * Gets the validoperators.
	 *
	 * @return the validoperators
	 */
	public List<OperatorApplicationDTO> getValidoperators(MessageContext context) throws BusinessException {
		OparatorService operatorService = new OparatorService();
		if(validoperators==null || validoperators.isEmpty()){
			validoperators = operatorService.loadActiveApplicationOperators();
		}

		if(context!=null){
			
			final String ApiName =  (String) context.getProperty("API_NAME");
			final int appId = Integer.valueOf(MediationHelper.getInstance().getApplicationId(context));
		OperatorApplicationDTO dto=new OperatorApplicationDTO();
		dto.setApplicationid(appId);
		dto.setApiName(ApiName );
		/**
 		* check if the given  subscription already loaded
 		*/
		if (!validoperators.contains(dto)) {

         /**
         * clear the cached operators and load from the db
         */
			OperatorAppSearchDTO searchDTO = new OperatorAppSearchDTO();
			searchDTO.setApiName(ApiName);
			searchDTO.setApplicationId(appId);
			final List<OperatorApplicationDTO> latestSubscriptions = operatorService.loadActiveApplicationOperators(searchDTO);
			if(!latestSubscriptions.isEmpty()) {
				validoperators.addAll(latestSubscriptions);
			}

		}
		if (!validoperators.contains(dto)) {
			throw new CustomException("SVC0001", "", new String[] { "Requested service is not provisioned" });
		}
		}
		return validoperators;
	}

	public OperatorApplicationDTO  checkAndReturnValiOparatorsIfExsista(MessageContext context) throws BusinessException{
		final String ApiName =  (String) context.getProperty("API_NAME");
		final int appId = Integer.valueOf(MediationHelper.getInstance().getApplicationId(context));
		getValidoperators(context);
		OperatorApplicationDTO dto=new OperatorApplicationDTO();
		dto.setApplicationid(appId);
		dto.setApiName(ApiName );


		return validoperators.get(validoperators.indexOf(dto));
	}
	/**
	 * Gets the http method.
	 *
	 * @return the http method
	 */
	public String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Gets the json body.
	 *
	 * @return the json body
	 */
	public JSONObject getJsonBody() {
		return jsonBody;
	}

	/**
	 * Gets the sub resource path.
	 *
	 * @return the sub resource path
	 */
	public String getSubResourcePath() {
		return subResourcePath;
	}

	/**
	 * Initialize.
	 *
	 * @param context
	 *            the context
	 * @return true, if successful
	 * @throws Exception
	 *             the exception
	 */
	public boolean initialize(MessageContext context) throws Exception {
		String applicationid = getApplicationid();
		String apiName = (String) context.getProperty("API_NAME");
		apiVersion = (String) context.getProperty("VERSION");
		apiContext = (String) context.getProperty("REST_API_CONTEXT");

		OparatorService operatorService = new OparatorService();
		if (applicationid == null) {
			throw new CustomException("SVC0001", "", new String[] { "Requested service is not provisioned" });
		}

		getValidoperators(context);
    	subResourcePath = (String) context.getProperty("REST_SUB_REQUEST_PATH");
		resourceUrl = (String) context.getProperty("REST_FULL_REQUEST_PATH");
		httpMethod = (String) context.getProperty("HTTP_METHOD");


		/*String jsonPayloadToString = JsonUtil
				.jsonPayloadToString(((Axis2MessageContext) context).getAxis2MessageContext());
		log.debug("DEBUG LOGS FOR LBS 13 : jsonPayloadToString = " + jsonPayloadToString);
		jsonBody = new JSONObject(jsonPayloadToString);
*/
		try {

/*			org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) context)
					.getAxis2MessageContext();
			RelayUtils.buildMessage(a2mc);
			String jsonPayloadToString = JsonUtil.jsonPayloadToString(a2mc);*/


			String jsonPayloadToString = JsonUtil.jsonPayloadToString(((Axis2MessageContext) context)
					.getAxis2MessageContext());

			jsonBody = new JSONObject(jsonPayloadToString);


		} catch (JSONException e) {
			log.error(e.getMessage());
			// ((Axis2MessageContext) context).setAxis2MessageContext("");
			// JsonUtil.newJsonPayload(((Axis2MessageContext)
			// context).getAxis2MessageContext(), "{}", true, true);
			// JsonUtil.removeJsonPayload(((Axis2MessageContext)
			// context).getAxis2MessageContext());

			// Axis2MessageContext axctx = ((Axis2MessageContext) context);
			// AxisMessage axmsg =
			// axctx.getAxis2MessageContext().getAxisMessage();
			//
			// axctx.getAxis2MessageContext().setAxisMessage(new AxisMessage());
			throw new CustomException(
					"SVC0001",
					"Json Error",
					new String[] { "Request is missing required URI components" });
		}

		return true;
	}

	/**
	 * Execute.
	 *
	 * @param context
	 *            the context
	 * @return true, if successful
	 * @throws CustomException
	 *             the custom exception
	 * @throws AxisFault
	 *             the axis fault
	 * @throws Exception
	 *             the exception
	 */
	public abstract boolean execute(MessageContext context) throws CustomException, AxisFault, Exception;

	/**
	 * Validate request.
	 *
	 * @param httpMethod
	 *            the http method
	 * @param requestPath
	 *            the request path
	 * @param jsonBody
	 *            the json body
	 * @param context
	 *            the context
	 * @return true, if successful
	 * @throws Exception
	 *             the exception
	 */
	public abstract boolean validateRequest(String httpMethod, String requestPath, JSONObject jsonBody,
			MessageContext context) throws Exception;

	/**
	 * Sets the response.
	 *
	 * @param mc
	 *            the mc
	 * @param responseStr
	 *            the response str
	 * @throws AxisFault
	 *             the axis fault
	 */
	public void setResponse(MessageContext mc, String responseStr) throws AxisFault {
		JsonUtil.newJsonPayload(((Axis2MessageContext) mc).getAxis2MessageContext(), responseStr, true, true);
	}

	/**
	 * Removes the headers.
	 *
	 * @param context
	 *            the context
	 */
	public void removeHeaders(MessageContext context) {
		Object headers = ((Axis2MessageContext) context).getAxis2MessageContext()
		                                                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
		if (headers != null && headers instanceof Map) {
			Map headersMap = (Map) headers;
			headersMap.clear();
		}
	}

	/**
	 * Handle plugin exception.
	 *
	 * @param errResp
	 *            the err resp
	 * @throws CustomException
	 *             the custom exception
	 */
	public void handlePluginException(String errResp) throws CustomException {
		Gson gson = new GsonBuilder().serializeNulls().create();
		String messagid = null;
		String variables = null;

		if (errResp.contains("requestError")) {
			errResp = errResp.replace("[", "").replace("]", "");
		}

		ResponseError responseError = gson.fromJson(errResp, ResponseError.class);
		if (responseError == null) {
			return;
		}
		RequestError reqerror = responseError.getRequestError();
		if (reqerror == null) {
			return;
		}

		if (reqerror.getPolicyException() != null) {
			messagid = reqerror.getPolicyException().getMessageId();
			variables = reqerror.getPolicyException().getVariables();
		} else if (reqerror.getServiceException() != null) {
			messagid = reqerror.getServiceException().getMessageId();
			variables = reqerror.getServiceException().getVariables();
		} else {
			return;
		}
		throw new CustomException(messagid, "", new String[] { variables });
	}

	/**
	 * Make request.
	 *
	 * @param operatorendpoint
	 *            the operatorendpoint
	 * @param url
	 *            the url
	 * @param requestStr
	 *            the request str
	 * @param auth
	 *            the auth
	 * @param messageContext
	 *            the message context
	 * @return the string
	 */
	public String makeRequest(OperatorEndpoint operatorendpoint, String url, String requestStr, boolean auth, MessageContext messageContext , boolean inclueHeaders) {


		 //MO Callback
        boolean isMoCallBack=false;
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(requestStr);
		} catch (JSONException error) {
			error.printStackTrace();
		}
		Iterator<String> keys = jsonObject.keys();
		if( keys.hasNext() ){
		   String key = (String)keys.next();
		   if (key.equals("inboundSMSMessageNotification")||key.equals("deliveryInfoNotification")) {
		   isMoCallBack=true;
	   }
		}

		//Not needed anymore since mediation of the Payment API happens through synapse
//        try {// check for charge operation. if true append ESB url
//            JSONObject jsonObj = new JSONObject(requestStr);
//            String transactionOperationStatus = jsonObj.getJSONObject("amountTransaction")
//                    .getString("transactionOperationStatus");
//            String status = "Charged";
//            if (status.equals(transactionOperationStatus)) {
//                url = modifyEndpoint(url, operatorendpoint.getOperator(), messageContext);
//            }
//        } catch (JSONException exception) {
//            log.error("Error parsing json object from request.", exception);
//        }

		ICallresponse icallresponse = null;
		String retStr = "";
		int statusCode = 0;

		URL neturl;
		HttpURLConnection connection = null;

		try {
			// String Authtoken = AccessToken;
			// //FileUtil.getApplicationProperty("wow.api.bearer.token");

			// String encodeurl = URLEncoder.encode(url, "UTF-8");
			neturl = new URL(url);
			connection = (HttpURLConnection) neturl.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
			//connection.setRequestProperty("charset", "utf-8");
			if (auth) {
				connection.setRequestProperty("Authorization", "Bearer " + getAccessToken(operatorendpoint.getOperator(), messageContext));

				// Add JWT token header
				org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
						.getAxis2MessageContext();
				Object headers = axis2MessageContext
						.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

				if (headers != null && headers instanceof Map) {
					Map headersMap = (Map) headers;
					String jwtparam = (String) headersMap.get("x-jwt-assertion");
					if (jwtparam != null) {
						connection.setRequestProperty("x-jwt-assertion", jwtparam);
					}
				}
			}


			if (inclueHeaders) {
				org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
						.getAxis2MessageContext();
				Object headers = axis2MessageContext
						.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
				if (headers != null && headers instanceof Map) {
					Map headersMap = (Map) headers;
					Iterator it = headersMap.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry entry = (Map.Entry) it.next();
						connection.setRequestProperty((String) entry.getKey(),
								(String) entry.getValue()); // avoids a
															// ConcurrentModificationException
					}
				}
			}

			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			 log.info("Southbound Request URL: " + connection.getRequestMethod() + " " + connection.getURL()
	            		+ " Request ID: " + UID.getRequestID(messageContext));
	            if (log.isDebugEnabled()) {
	                log.debug("Southbound Request Headers: " + connection.getRequestProperties());
	            }
	            log.info("Southbound Request Body: " + requestStr
	            		+ " Request ID: " + UID.getRequestID(messageContext));

			//========================UNICODE PATCH=========================================
			BufferedOutputStream wr = new BufferedOutputStream(connection.getOutputStream());
			wr.write(requestStr.getBytes("UTF-8"));

			wr.flush();
			wr.close();
			//========================UNICODE PATCH=========================================

           /*DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
             wr.writeBytes(requestStr);
            wr.flush();
            wr.close();*/
			statusCode = connection.getResponseCode();
			if ((statusCode != 200) && (statusCode != 201) && (statusCode != 400) && (statusCode != 401)) {
				throw new RuntimeException("Failed : HTTP error code : " + statusCode);
			}

			InputStream is = null;
			if ((statusCode == 200) || (statusCode == 201)) {
				is = connection.getInputStream();
			} else {
				is = connection.getErrorStream();
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String output;
			while ((output = br.readLine()) != null) {
				retStr += output;
			}
			br.close();

		   log.info("Southbound Response Status: " + statusCode + " " + connection.getResponseMessage()
            		+ " Request ID: " + UID.getRequestID(messageContext));
            if (log.isDebugEnabled()) {
                log.debug("Southbound Response Headers: " + connection.getHeaderFields());
            }
            log.info("Southbound Response Body: " + retStr
            		+ " Request ID: " + UID.getRequestID(messageContext));


		} catch (Exception e) {
			log.error("[WSRequestService ], makerequest, " + e.getMessage(), e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}

			log.debug("Mo OR DN CallBack : " + isMoCallBack +" requestStr : " + requestStr+" retStr : " + retStr);

			messageContext.setProperty(DataPublisherConstants.RESPONSE_CODE, Integer.toString(statusCode));
            messageContext.setProperty(DataPublisherConstants.MSISDN, messageContext.getProperty(MSISDNConstants.USER_MSISDN));
		/*	TODO:This need to consider when publishing request data
			  if (isMoCallBack) {
              	publishResponseData(statusCode, requestStr, messageContext);
  			}else {
  				publishResponseData(statusCode, retStr, messageContext);
  			}*/
		}
		return retStr;
	}

	/**
	 * Make tokenrequest.
	 *
	 * @param tokenurl
	 *            the tokenurl
	 * @param urlParameters
	 *            the url parameters
	 * @param authheader
	 *            the authheader
	 * @return the string
	 */
	protected String makeTokenrequest(String tokenurl, String urlParameters, String authheader, MessageContext messageContext) {

		ICallresponse icallresponse = null;
		String retStr = "";

		URL neturl;
		HttpURLConnection connection = null;

		log.info("url : " + tokenurl + " | urlParameters : " + urlParameters + " | authheader : " + authheader
			 + " Request ID: " + UID.getRequestID(messageContext));

		if ((tokenurl != null && tokenurl.length() > 0) && (urlParameters != null && urlParameters.length() > 0)
				&& (authheader != null && authheader.length() > 0)) {
			try {

				byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
				int postDataLength = postData.length;
				URL url = new URL(tokenurl);

				connection = (HttpURLConnection) url.openConnection();
				connection.setDoOutput(true);
				connection.setInstanceFollowRedirects(false);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Authorization", authheader);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
				connection.setUseCaches(false);

				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.write(postData);
				wr.flush();
				wr.close();

				if ((connection.getResponseCode() != 200) && (connection.getResponseCode() != 201)
						&& (connection.getResponseCode() != 400) && (connection.getResponseCode() != 401)) {
					log.info("connection.getResponseMessage() : " + connection.getResponseMessage());
					throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());
				}

				InputStream is = null;
				if ((connection.getResponseCode() == 200) || (connection.getResponseCode() == 201)) {
					is = connection.getInputStream();
				} else {
					is = connection.getErrorStream();
				}

				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String output;
				while ((output = br.readLine()) != null) {
					retStr += output;
				}
				br.close();
			} catch (Exception e) {
				log.error("[WSRequestService ], makerequest, " + e.getMessage(), e);
				return null;
			} finally {

				if (connection != null) {
					connection.disconnect();
				}
			}
		} else {
			log.error("Token refresh details are invalid.");
		}

		return retStr;
	}

	/**
	 * Make get request.
	 *
	 * @param operatorendpoint
	 *            the operatorendpoint
	 * @param url
	 *            the url
	 * @param requestStr
	 *            the request str
	 * @param auth
	 *            the auth
	 * @param messageContext
	 *            the message context
	 * @return the string
	 */
	public String makeGetRequest(OperatorEndpoint operatorendpoint, String url, String requestStr, boolean auth,
	                             MessageContext messageContext, boolean inclueHeaders) {

		int statusCode = 0;
		String retStr = "";
		URL neturl;
		HttpURLConnection connection = null;

		try {

			// String Authtoken = AccessToken;
			// //FileUtil.getApplicationProperty("wow.api.bearer.token");
			// DefaultHttpClient httpClient = new DefaultHttpClient();
			String encurl = (requestStr != null) ? url + requestStr : url;
			neturl = new URL(encurl);
			connection = (HttpURLConnection) neturl.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");

			if (auth) {
				connection.setRequestProperty("Authorization", "Bearer " + getAccessToken(operatorendpoint.getOperator(), messageContext));

				// Add JWT token header
				org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
						.getAxis2MessageContext();
				Object headers = axis2MessageContext
						.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
				if (headers != null && headers instanceof Map) {
					Map headersMap = (Map) headers;
					String jwtparam = (String) headersMap.get("x-jwt-assertion");
					if (jwtparam != null) {
						connection.setRequestProperty("x-jwt-assertion", jwtparam);
					}
				}
			}

			if (inclueHeaders) {
				org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
						.getAxis2MessageContext();
				Object headers = axis2MessageContext
						.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
				if (headers != null && headers instanceof Map) {
					Map headersMap = (Map) headers;
					Iterator it = headersMap.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry entry = (Map.Entry) it.next();
						connection.setRequestProperty((String) entry.getKey(),
								(String) entry.getValue()); // avoids a
															// ConcurrentModificationException
					}
				}
			}


			connection.setUseCaches(false);

			log.info("Southbound Request URL: " + connection.getRequestMethod() + " " + connection.getURL()
            		+ " Request ID: " + UID.getRequestID(messageContext));
            if (log.isDebugEnabled()) {
                log.debug("Southbound Request Headers: " + connection.getRequestProperties());
            }
            log.info("Southbound Request Body: " + requestStr
            		+ " Request ID: " + UID.getRequestID(messageContext));


			statusCode = connection.getResponseCode();
			if ((statusCode != 200) && (statusCode != 201) && (statusCode != 400) && (statusCode != 401)) {
				throw new RuntimeException("Failed : HTTP error code : " + statusCode);
			}

			InputStream is = null;
			if ((statusCode == 200) || (statusCode == 201)) {
				is = connection.getInputStream();
			} else {
				is = connection.getErrorStream();
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String output;
			while ((output = br.readLine()) != null) {
				retStr += output;
			}
			br.close();

		   log.info("Southbound Response Status: " + statusCode + " " + connection.getResponseMessage()
            		+ " Request ID: " + UID.getRequestID(messageContext));
            if (log.isDebugEnabled()) {
                log.debug("Southbound Response Headers: " + connection.getHeaderFields());
            }
            log.info("Southbound Response Body: " + retStr
	            		+ " Request ID: " + UID.getRequestID(messageContext));
		} catch (Exception e) {
			log.error("[WSRequestService ], makerequest, " + e.getMessage(), e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return retStr;
	}

	/**
	 * Make delete request.
	 *
	 * @param operatorendpoint
	 *            the operatorendpoint
	 * @param url
	 *            the url
	 * @param requestStr
	 *            the request str
	 * @param auth
	 *            the auth
	 * @param messageContext
	 *            the message context
	 * @return the string
	 */
	public String makeDeleteRequest(OperatorEndpoint operatorendpoint, String url, String requestStr, boolean auth,
	                                MessageContext messageContext , boolean inclueHeaders) {

		int statusCode = 0;
		String retStr = "";
		URL neturl;
		HttpURLConnection connection = null;

		try {

			// String Authtoken = AccessToken;
			// //FileUtil.getApplicationProperty("wow.api.bearer.token");
			// DefaultHttpClient httpClient = new DefaultHttpClient();

			String encurl = (requestStr != null) ? url + requestStr : url;
			neturl = new URL(encurl);
			connection = (HttpURLConnection) neturl.openConnection();
			connection.setRequestMethod("DELETE");
			connection.setRequestProperty("Content-Type", "application/json");

			if (auth) {
				connection.setRequestProperty("Authorization", "Bearer " + getAccessToken(operatorendpoint.getOperator(), messageContext));
				// Add JWT token header
				org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
						.getAxis2MessageContext();
				Object headers = axis2MessageContext
						.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
				if (headers != null && headers instanceof Map) {
					Map headersMap = (Map) headers;
					String jwtparam = (String) headersMap.get("x-jwt-assertion");
					if (jwtparam != null) {
						connection.setRequestProperty("x-jwt-assertion", jwtparam);
					}
				}
			}


			if (inclueHeaders) {
				org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
						.getAxis2MessageContext();
				Object headers = axis2MessageContext
						.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
				if (headers != null && headers instanceof Map) {
					Map headersMap = (Map) headers;
					Iterator it = headersMap.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry entry = (Map.Entry) it.next();
						connection.setRequestProperty((String) entry.getKey(),
								(String) entry.getValue()); // avoids a
															// ConcurrentModificationException
					}
				}
			}

			connection.setUseCaches(false);

			log.info("Southbound Request URL: " + connection.getRequestMethod() + " " + connection.getURL()
            		+ " Request ID: " + UID.getRequestID(messageContext));
            if (log.isDebugEnabled()) {
                log.debug("Southbound Request Headers: " + connection.getRequestProperties());
            }
            log.info("Southbound Request Body: " + requestStr
            		+ " Request ID: " + UID.getRequestID(messageContext));

			if (requestStr != null) {
				connection.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(requestStr);
				wr.flush();
				wr.close();
			}

			statusCode = connection.getResponseCode();
			log.info("response code: " + statusCode);

			if ((statusCode != 200) && (statusCode != 201) && (statusCode != 400) && (statusCode != 401)
					&& (statusCode != 204)) {
				throw new RuntimeException("Failed : HTTP error code : " + statusCode);
			}

			InputStream is = null;
			if ((statusCode == 200) || (statusCode == 201) || (statusCode == 204)) {
				is = connection.getInputStream();
			} else {
				is = connection.getErrorStream();
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String output;
			while ((output = br.readLine()) != null) {
				retStr += output;
			}
			br.close();

		   log.info("Southbound Response Status: " + statusCode + " " + connection.getResponseMessage()
            		+ " Request ID: " + UID.getRequestID(messageContext));
            if (log.isDebugEnabled()) {
                log.debug("Southbound Response Headers: " + connection.getHeaderFields());
            }
            log.info("Southbound Response Body: " + retStr
            		+ " Request ID: " + UID.getRequestID(messageContext));


		} catch (Exception e) {
			log.error("[WSRequestService ], makerequest, " + e.getMessage(), e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return retStr;
	}

	/**
	 * Make north bound request.
	 *
	 * @param operatorendpoint
	 *            the operatorendpoint
	 * @param url
	 *            the url
	 * @param requestStr
	 *            the request str
	 * @param auth
	 *            the auth
	 * @param messageContext
	 *            the message context
	 * @param inclueHeaders
	 *            the inclue headers
	 * @return the int
	 */
	public int makeNorthBoundRequest(OperatorEndpoint operatorendpoint, String url, String requestStr, boolean auth,
	                                 MessageContext messageContext, boolean inclueHeaders) {


		ICallresponse icallresponse = null;
		String retStr = "";
		int statusCode = 0;

		URL neturl;
		HttpURLConnection connection = null;

		try {

			neturl = new URL(url);
			connection = (HttpURLConnection) neturl.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Accept-Charset", "UTF-8");// ADDED
			if (auth) {
				connection.setRequestProperty("Authorization", "Bearer " + getAccessToken(operatorendpoint.getOperator(), messageContext));

				// Add JWT token header
				org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
						.getAxis2MessageContext();
				Object headers = axis2MessageContext
						.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
				if (headers != null && headers instanceof Map) {
					Map headersMap = (Map) headers;
					String jwtparam = (String) headersMap.get("x-jwt-assertion");
					if (jwtparam != null) {
						connection.setRequestProperty("x-jwt-assertion", jwtparam);
					}
				}
			}

			if (inclueHeaders) {
				org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
						.getAxis2MessageContext();
				Object headers = axis2MessageContext
						.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
				if (headers != null && headers instanceof Map) {
					Map headersMap = (Map) headers;
					Iterator it = headersMap.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry entry = (Map.Entry) it.next();
						connection.setRequestProperty((String) entry.getKey(), (String) entry.getValue()); // avoids
																											// a
																											// ConcurrentModificationException
					}
				}
			}
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			 log.info("Northbound Request URL: " + connection.getRequestMethod() + " " + connection.getURL()
	            		+ " Request ID: " + UID.getRequestID(messageContext));
	            if (log.isDebugEnabled()) {
	                log.debug("Northbound Request Headers: " + connection.getRequestProperties());
	            }
	            log.info("Northbound Request Body: " + requestStr
	            		+ " Request ID: " + UID.getRequestID(messageContext));

			// ========================UNICODE
			// PATCH=========================================
			BufferedOutputStream wr = new BufferedOutputStream(connection.getOutputStream());
			wr.write(requestStr.getBytes("UTF-8"));
			wr.flush();
			wr.close();
			// ========================UNICODE
			// PATCH=========================================

			statusCode = connection.getResponseCode();

			log.info("Northbound Response Status: " + statusCode + " " + connection.getResponseMessage()
            		+ " Request ID: " + UID.getRequestID(messageContext));
            if (log.isDebugEnabled()) {
                log.debug("Northbound Response Headers: " + connection.getHeaderFields());
            }
            log.info("Northbound Response Body: " + retStr
            		+ " Request ID: " + UID.getRequestID(messageContext));

			if (statusCode != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + statusCode);
			}

		} catch (Exception e) {
			log.error("[WSRequestService ], makerequest, " + e.getMessage(), e);
			return 0;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return statusCode;
	}


	public String makeRetrieveSMSGetRequest(OperatorEndpoint operatorendpoint, String url, String requestStr, boolean auth,
	                                        MessageContext messageContext, boolean inclueHeaders) {

		Gson gson = new GsonBuilder().serializeNulls().create();
		int statusCode = 0;
		String retStr = "";
		URL neturl;
		HttpURLConnection connection = null;

		try {

			// String Authtoken = AccessToken;
			// //FileUtil.getApplicationProperty("wow.api.bearer.token");
			// DefaultHttpClient httpClient = new DefaultHttpClient();
			String encurl = (requestStr != null) ? url + requestStr : url;
			neturl = new URL(encurl);
			connection = (HttpURLConnection) neturl.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");

			if (auth) {
				connection.setRequestProperty("Authorization", "Bearer " + getAccessToken(operatorendpoint.getOperator(), messageContext));

				// Add JWT token header
				org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
						.getAxis2MessageContext();
				Object headers = axis2MessageContext
						.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
				if (headers != null && headers instanceof Map) {
					Map headersMap = (Map) headers;
					String jwtparam = (String) headersMap
							.get("x-jwt-assertion");
					if (jwtparam != null) {
						connection.setRequestProperty("x-jwt-assertion",
								jwtparam);
					}
				}
			}

			if (inclueHeaders) {
				org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
						.getAxis2MessageContext();
				Object headers = axis2MessageContext
						.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
				if (headers != null && headers instanceof Map) {
					Map headersMap = (Map) headers;
					Iterator it = headersMap.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry entry = (Map.Entry) it.next();
						connection.setRequestProperty((String) entry.getKey(),
								(String) entry.getValue()); // avoids a
															// ConcurrentModificationException
					}
				}
			}

			connection.setUseCaches(false);

			log.info("Southbound Request URL: " + connection.getRequestMethod() + " " + connection.getURL()
            		+ " Request ID: " + UID.getRequestID(messageContext));
            if (log.isDebugEnabled()) {
                log.debug("Southbound Request Headers: " + connection.getRequestProperties());
            }
            log.info("Southbound Request Body: " + requestStr
            		+ " Request ID: " + UID.getRequestID(messageContext));

			statusCode = connection.getResponseCode();
			if ((statusCode != 200) && (statusCode != 201) && (statusCode != 400) && (statusCode != 401)) {
				throw new RuntimeException("Failed : HTTP error code : " + statusCode);
			}

			InputStream is = null;
			if ((statusCode == 200) || (statusCode == 201)) {
				is = connection.getInputStream();
			} else {
				is = connection.getErrorStream();
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String output;
			while ((output = br.readLine()) != null) {
				retStr += output;
			}
			br.close();

			   log.info("Southbound Response Status: " + statusCode + " " + connection.getResponseMessage()
	            		+ " Request ID: " + UID.getRequestID(messageContext));
	            if (log.isDebugEnabled()) {
	                log.debug("Southbound Response Headers: " + connection.getHeaderFields());
	            }
	            log.info("Southbound Response Body: " + retStr
	            		+ " Request ID: " + UID.getRequestID(messageContext));

			SouthboundRetrieveResponse sbRetrieveResponse = gson.fromJson(retStr,SouthboundRetrieveResponse.class);
            if (sbRetrieveResponse!= null && sbRetrieveResponse.getInboundSMSMessageList() != null) {

				if (sbRetrieveResponse.getInboundSMSMessageList().getInboundSMSMessage()!=null && sbRetrieveResponse.getInboundSMSMessageList().getInboundSMSMessage().length!=0) {
           		InboundSMSMessage[] inboundSMSMessageResponses = sbRetrieveResponse.getInboundSMSMessageList().getInboundSMSMessage();
                    messageContext.setProperty(DataPublisherConstants.RESPONSE, String.valueOf(inboundSMSMessageResponses.length));
        		}else{
        			InboundSMSMessage[] inboundSMSMessageResponses = new InboundSMSMessage[0];
        			InboundSMSMessageList inboundSMSMessageList=new InboundSMSMessageList();
        			inboundSMSMessageList.setInboundSMSMessage(inboundSMSMessageResponses);
        			inboundSMSMessageList.setNumberOfMessagesInThisBatch("0");
        			inboundSMSMessageList.setResourceURL("Not Available");
        			inboundSMSMessageList.setTotalNumberOfPendingMessages("0");
        			sbRetrieveResponse.setInboundSMSMessageList(inboundSMSMessageList);

				InboundSMSMessage[] inboundSMSMessageResponsesN = sbRetrieveResponse.getInboundSMSMessageList().getInboundSMSMessage();
				messageContext.setProperty(DataPublisherConstants.RESPONSE,String.valueOf(inboundSMSMessageResponsesN.length));
        		}
			}else {
				messageContext.setProperty(DataPublisherConstants.RESPONSE,String.valueOf(0));
			}
		} catch (Exception e) {
			log.error("[WSRequestService ], makerequest, " + e.getMessage(), e);
			messageContext.setProperty(DataPublisherConstants.RESPONSE,String.valueOf(0));
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return retStr;
	}




	private void publishWalletPaymentData(int statusCode, String retStr, MessageContext messageContext) {
		        //set properties for response data publisher
		        messageContext.setProperty(DataPublisherConstants.RESPONSE_CODE, Integer.toString(statusCode));
		        messageContext.setProperty(DataPublisherConstants.MSISDN, messageContext.getProperty(MSISDNConstants.USER_MSISDN));


		    }


		    /**
		     *
		     * @param url
		     * @param requestStr
		     * @param messageContext
		     * @return
		     */
		    public String makeWalletRequest(OperatorEndpoint operatorendpoint, String url, String requestStr, boolean auth,
		                                    MessageContext messageContext, boolean inclueHeaders) {


		        String retStr = "";
		        int statusCode = 0;

		        URL neturl;
		        HttpURLConnection connection = null;

		        try {

		            neturl = new URL(url);
		            connection = (HttpURLConnection) neturl.openConnection();
		           connection.setRequestMethod("POST");
		            connection.setRequestProperty("Content-Type", "application/json");
		            connection.setRequestProperty("Accept", "application/json");
		            connection.setRequestProperty("Accept-Charset", "UTF-8");//ADDED
		            if (auth) {
		            	connection.setRequestProperty("Authorization", "Bearer " + getAccessToken(operatorendpoint.getOperator(), messageContext));

		                //Add JWT token header
		                org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
		                        .getAxis2MessageContext();
		               Object headers = axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
		                if (headers != null && headers instanceof Map) {
		                    Map headersMap = (Map) headers;
		                    String jwtparam = (String) headersMap.get("x-jwt-assertion");
		                    if (jwtparam != null) {
		                        connection.setRequestProperty("x-jwt-assertion", jwtparam);
		                    }
		                }
		            }

		            if (inclueHeaders) {
		                org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
		                        .getAxis2MessageContext();
		                Object headers = axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
		                if (headers != null && headers instanceof Map) {
		                    Map headersMap = (Map) headers;
		                    Iterator it = headersMap.entrySet().iterator();
		                    while (it.hasNext()) {
		                        Map.Entry entry = (Map.Entry) it.next();
		                        connection.setRequestProperty((String) entry.getKey(), (String) entry.getValue()); // avoids a ConcurrentModificationException
		                    }
		                }
		            }
		            connection.setUseCaches(false);
		            connection.setDoInput(true);
		            connection.setDoOutput(true);

		            log.info("Southbound Request URL: " + connection.getRequestMethod() + " " + connection.getURL()
		            		+ " Request ID: " + UID.getRequestID(messageContext));
		            if (log.isDebugEnabled()) {
		                log.debug("Southbound Request Headers: " + connection.getRequestProperties());
		            }
		            log.info("Southbound Request Body: " + requestStr
		            		+ " Request ID: " + UID.getRequestID(messageContext));

		            //========================UNICODE PATCH=========================================
		            BufferedOutputStream wr = new BufferedOutputStream(connection.getOutputStream());
		            wr.write(requestStr.getBytes("UTF-8"));
		            wr.flush();
		            wr.close();
		            //========================UNICODE PATCH=========================================


		            statusCode = connection.getResponseCode();
		            if ((statusCode != 200) && (statusCode != 201) && (statusCode != 400) && (statusCode != 401)) {
		                throw new RuntimeException("Failed : HTTP error code : " + statusCode);
		            }

		           InputStream is = null;
		            if ((statusCode == 200) || (statusCode == 201)) {
		                is = connection.getInputStream();
		            } else {
		                is = connection.getErrorStream();
		            }

		            BufferedReader br = new BufferedReader(new InputStreamReader(is));
		            String output;
		            while ((output = br.readLine()) != null) {
		                retStr += output;
		            }
		            br.close();
		            log.info("Southbound Response Status: " + statusCode + " " + connection.getResponseMessage()
		            		+ " Request ID: " + UID.getRequestID(messageContext));
		            if (log.isDebugEnabled()) {
		                log.debug("Southbound Response Headers: " + connection.getHeaderFields());
		            }
		            log.info("Southbound Response Body: " + retStr
		            		+ " Request ID: " + UID.getRequestID(messageContext));
		        } catch (Exception e) {
		            log.error("[WSRequestService ], makerequest, " + e.getMessage(), e);
		            return null;
		        } finally {
		            if (connection != null) {
		                connection.disconnect();
		            }
		            messageContext.setProperty(DataPublisherConstants.RESPONSE_CODE, Integer.toString(statusCode));
		            messageContext.setProperty(DataPublisherConstants.MSISDN, messageContext.getProperty(
				            MSISDNConstants.USER_MSISDN));
		            publishWalletPaymentData(statusCode, retStr, messageContext);
		        }

		        return retStr;
		    }

		    /**
		         *
		         * @param url
		         * @param requestStr
		         * @param messageContext
		         * @return
		        */
        public String makeCreditRequest(OperatorEndpoint operatorendpoint, String url, String requestStr, boolean auth,
                                        MessageContext messageContext, boolean inclueHeaders) {


            String retStr = "";
            int statusCode = 0;

            URL neturl;
            HttpURLConnection connection = null;

            try {

                neturl = new URL(url);
                connection = (HttpURLConnection) neturl.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Accept-Charset", "UTF-8");//ADDED
                if (auth) {
                	connection.setRequestProperty("Authorization", "Bearer " + getAccessToken(operatorendpoint.getOperator(),messageContext));
                    //Add JWT token header
                    org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                            .getAxis2MessageContext();
                    Object headers = axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                    if (headers != null && headers instanceof Map) {
                        Map headersMap = (Map) headers;
                        String jwtparam = (String) headersMap.get("x-jwt-assertion");
                        if (jwtparam != null) {
                            connection.setRequestProperty("x-jwt-assertion", jwtparam);
                        }
                    }
                }

                if (inclueHeaders) {
                    org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                            .getAxis2MessageContext();
                    Object headers = axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                    if (headers != null && headers instanceof Map) {
                        Map headersMap = (Map) headers;
                        Iterator it = headersMap.entrySet().iterator();
                       while (it.hasNext()) {
                            Map.Entry entry = (Map.Entry) it.next();
                            connection.setRequestProperty((String) entry.getKey(), (String) entry.getValue()); // avoids a ConcurrentModificationException
                        }
                    }
                }
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);


                log.debug("Southbound Request URL: " + connection.getRequestMethod() + " " + connection.getURL());
                log.debug("Southbound Request Headers: " + connection.getRequestProperties());
                log.debug("Southbound Request Body: " + requestStr);


               //UNICODE
                BufferedOutputStream wr = new BufferedOutputStream(connection.getOutputStream());
                wr.write(requestStr.getBytes("UTF-8"));
                wr.flush();
                wr.close();


                statusCode = connection.getResponseCode();
                if ((statusCode != 200) && (statusCode != 201) && (statusCode != 400) && (statusCode != 401)) {
                    throw new RuntimeException("Failed : HTTP error code : " + statusCode);
                }

                InputStream is = null;
                if ((statusCode == 200) || (statusCode == 201)) {
                    is = connection.getInputStream();
                } else {
                   is = connection.getErrorStream();
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String output;
                while ((output = br.readLine()) != null) {
                    retStr += output;
                }
                br.close();


                log.debug("Southbound Response Status: " + statusCode + " " + connection.getResponseMessage());
                log.debug("Southbound Response Headers: " + connection.getHeaderFields());
                log.debug("Southbound Response Body: " + retStr);

            } catch (Exception e) {
                log.error("[CreditRequestService ], makerequest, " + e.getMessage(), e);
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                messageContext.setProperty(DataPublisherConstants.RESPONSE_CODE, Integer.toString(statusCode));
                messageContext.setProperty(DataPublisherConstants.MSISDN, messageContext.getProperty(MSISDNConstants.USER_MSISDN));
                publishWalletPaymentData(statusCode, retStr, messageContext);
            }

            return retStr;
        }

    /**
     * Retrieves the access token using either token pool service or the default flow is done
     *
     * @param operator the name of the operator
     * @return the string representation of the access token
     * @throws Exception
     */
	public String getAccessToken(String operator, MessageContext messageContext) throws Exception {
		String response = "";
		if (log.isDebugEnabled()) {
			log.debug("Fetching token for Operator : " + operator);
		}
		if (operator == null) {
			return response;
		}
		Map<String, String> mediatorConfMap = ConfigFileReader.getInstance().getMediatorConfigMap();

		String tokenPoolService = mediatorConfMap.get("tokenpoolservice");
		String resourceURL = mediatorConfMap.get("tokenpoolResourceURL");
		if (log.isDebugEnabled()) {
			log.debug("tokenPoolService Enabled : " + tokenPoolService + " with tokenPoolService URL : " + resourceURL);
		}
		if (tokenPoolService != null && resourceURL != null && tokenPoolService.equals("true")) {
			if (log.isDebugEnabled()) {
				log.debug("Token Pool Service getPoolAccessToken() Flow ");
			}

			response = getPoolAccessToken(operator, resourceURL);
		} else {
			log.debug("Hub Mediator getDefaultAccessToken() Flow ");
			response = getDefaultAccessToken(operator, messageContext);
		}
		return response;
	}

    /**
     * Retrieves the token from the token pool service.
     *
     * @param owner_id which is operator in Token Pool service
     * @return access token
     * @throws Exception
     */
    private String getPoolAccessToken(String owner_id, String resourceURL) throws Exception {
        StringBuffer result = new StringBuffer();
        HttpURLConnection poolConnection = null;
        URL requestUrl;

        try {

            requestUrl = new URL(resourceURL + URLEncoder.encode(owner_id, "UTF-8"));
            poolConnection = (HttpURLConnection) requestUrl.openConnection();
            poolConnection.setDoOutput(true);
            poolConnection.setInstanceFollowRedirects(false);
            poolConnection.setRequestMethod("GET");
            poolConnection.setRequestProperty("Accept", "application/json");
            poolConnection.setUseCaches(false);

            InputStream input = null;
            if (poolConnection.getResponseCode() == 200) {
                input = poolConnection.getInputStream();
            } else {
                input = poolConnection.getErrorStream();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            String output;
            while ((output = br.readLine()) != null) {
                result.append(output);
            }
            br.close();
        } catch (Exception e) {
            log.error("[TokenPoolRequestService ], getPoolAccessToken, " + e.getMessage());
            return null;
        } finally {
            if (poolConnection != null) {
                poolConnection.disconnect();
            }
        }
        return result.toString();
    }


	/**
	 * Retieves the access token using the default flow.
	 *
	 * @param operator the name of the operator
	 * @return
	 * @throws Exception
	 */
	protected String getDefaultAccessToken(String operator, MessageContext messageContext) throws Exception {

		if (operator == null) {
			throw new CustomException("", "", new String[]{"Requested Operator is not provisioned"});
		}

		Operator operatorDetail = getOperatorByName(operator);
		messageContext.setProperty("OPERATOR",operator);

		String token = null;

		String applicationid = getApplicationid();
		if (applicationid == null) {
			throw new CustomException("SVC0001", "", new String[]{"Requested service is not provisioned"});
		}

		//
		if (log.isDebugEnabled()) {
			log.debug("Token time : " + operatorDetail.getTokentime() + " Request ID: " + UID.getRequestID(messageContext));
			log.debug("Token validity : " + operatorDetail.getTokenvalidity() + " Request ID: " + UID.getRequestID(messageContext));
		}

		long timeexpires = (long) (operatorDetail.getTokentime() + (operatorDetail.getTokenvalidity() * 1000));

		if (log.isDebugEnabled()) {
			log.debug("Expire time : " + timeexpires + " Request ID: " + UID.getRequestID(messageContext));
		}
		long currtime = new Date().getTime();

		if (log.isDebugEnabled()) {
			log.debug("Current time : " + currtime + " Request ID: " + UID.getRequestID(messageContext));
		}

		if (timeexpires > currtime) {
			token = operatorDetail.getToken();
			if (log.isDebugEnabled()) {
				log.debug("Token of " + operatorDetail.getOperatorName() + " operator is active"
						+ " Request ID: " + UID.getRequestID(messageContext));
			}

		} else {

			if (log.isDebugEnabled()) {
				log.debug("Regenerating the token of " + operatorDetail.getOperatorName() + " operator"
						+ " Request ID: " + UID.getRequestID(messageContext));
			}

			String Strtoken = makeTokenrequest(operatorDetail.getTokenurl(),
					"grant_type=refresh_token&refresh_token=" + operatorDetail.getRefreshtoken(),
					("" + operatorDetail.getTokenauth()),
					messageContext);
			if (Strtoken != null && Strtoken.length() > 0) {
				if (log.isDebugEnabled()) {
					log.debug("Token regeneration response of " + operatorDetail.getOperatorName() + " operator : " + Strtoken
							+ " Request ID: " + UID.getRequestID(messageContext));
				}
				JSONObject jsontoken = new JSONObject(Strtoken);
				// Removing operator upon an error, so that refreshed credentials will be
				// loaded with the next request
				if (jsontoken.has("error")) {
					operatorMap.remove(operator);
					if (log.isDebugEnabled()) {
						log.debug("Removing operator detail record with Operator : " + operatorDetail.getOperatorName());
					}
					return operatorDetail.getToken();
				}

				token = jsontoken.getString("access_token");

				String refresh_token = jsontoken.getString("refresh_token");
				long expiryTime = Long.parseLong(String.valueOf(jsontoken.get("expires_in")));
				long current_time = new Date().getTime();

				if (log.isDebugEnabled()) {
					log.debug("Updating DB with renewed token - refresh_token : " + refresh_token + ", expiryTime : "
							+ expiryTime + ", token : " + token + ",current_time : " + current_time);
				}
				OparatorService operatorService = new OparatorService();
				operatorService.updateOperatorToken(operatorDetail.getOperatorId(),
						refresh_token,
						expiryTime,
						current_time, token);

				operatorDetail.setRefreshtoken(refresh_token);
				operatorDetail.setToken(token);
				operatorDetail.setTokenvalidity(expiryTime);
				operatorDetail.setTokentime(current_time);


			} else {
				log.error("Token regeneration response of " + operatorDetail.getOperatorName() + " operator is invalid.");
			}
		}

		return token;
	}

	public void clearOperatorDetailsFromCache(MessageContext context) throws BusinessException {

		Object operator = context.getProperty("OPERATOR");

		if(operator != null)
		{
			log.debug("Clearing Cache for Operator : " + operator.toString());
			operatorMap.remove(operator.toString());
		}
	}

	private Operator getOperatorByName(String operator) throws OperatorServiceException {

		if (operatorMap == null || operatorMap.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("Operator Map is null, populating ...");
			}
			operatorMap = new ConcurrentHashMap<>();
			OparatorService operatorService = new OparatorService();
			List<Operator> operators = operatorService.getOperatorsWithTokenDetails();
			for (Operator opr : operators) {
				if (log.isDebugEnabled()) {
					log.debug("Adding operator to Map : " + opr);
				}
				operatorMap.put(opr.getOperatorName(), opr);
			}
			return operatorMap.get(operator);

		}

		if (!operatorMap.containsKey(operator)) {
			if (log.isDebugEnabled()) {
				log.debug("Operator " + operator + "is not cached locally. Calling OperatorService.");
			}
			OparatorService operatorService = new OparatorService();
			Operator opr = operatorService.getOperatorWithTokenDetails(operator);

			if (log.isDebugEnabled() && opr != null) {
				log.debug("Operator details fetched for " + opr);
			}
			operatorMap.put(opr.getOperatorName(), opr);
			return opr;
		}

		return operatorMap.get(operator);
	}

    private String modifyEndpoint(String sendingAdd, String operator, MessageContext context){

	    String esbEndpoint = ConfigFileReader.getInstance().getMediatorConfigMap().get("esbEndpoint");
        String isUserInfoEnabled = "false";
        String operatorEndpoint = sendingAdd;
        String consumerKey = "";

        consumerKey = (String) context.getProperty("CONSUMER_KEY");

        GroupEventUnmarshaller unmarshaller = GroupEventUnmarshaller.getInstance();
        try {
            GroupDTO groupDTO = unmarshaller.getGroupDTO(operator, consumerKey);
            isUserInfoEnabled = groupDTO.getUserInfoEnabled() + "";
        } catch (OparatorNotinListException e) {
            e.printStackTrace();
        }

        String isUserinfoEnabledQueryParam = "isUserInfoEnabled=" + isUserInfoEnabled;
        String operatorEndpointQueryParam = "oprendpoint=" + operatorEndpoint;
        String operatorQueryParam = "operator=" + operator;
        String consumerKeyQueryParam = "consumerKey=" + consumerKey;

        String modifiedEndpoint =
                esbEndpoint + "?" + operatorEndpointQueryParam + "&" + operatorQueryParam + "&" + consumerKeyQueryParam
                + "&" + isUserinfoEnabledQueryParam;

        return modifiedEndpoint;
    }

}
