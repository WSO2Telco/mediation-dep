/*
 *
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package com.wso2telco.dep.mediator.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.wso2telco.dep.mediator.ICallresponse;
import com.wso2telco.dep.mediator.RequestExecutor;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.operatorservice.model.OperatorApplicationDTO;
import com.wso2telco.dep.operatorservice.service.OparatorService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.json.JSONObject;

/**
 * Util class to get operator access token
 */
public class OperatorAccessToken {
    private static List<OperatorApplicationDTO> validoperators = null;
    private static Log log = LogFactory.getLog(RequestExecutor.class);

    public static String getAccessToken(String operator, MessageContext messageContext) throws Exception {
        String response = "";
        Map<String, String> mediatorConfMap =  ConfigFileReader.getInstance().getMediatorConfigMap();
        String tokenPoolService = mediatorConfMap.get("tokenpoolservice");
        String resourceURL = mediatorConfMap.get("tokenpoolResourceURL");
        log.info("tokenPoolService Enabled: " + tokenPoolService + "with tokenPoolService URL: " + resourceURL);

        if (tokenPoolService != null && resourceURL != null && tokenPoolService.equals("true")) {
            log.info("Token Pool Service getPoolAccessToken() Flow ");
            response = getPoolAccessToken(operator, resourceURL);
        } else {
            log.info("Hub Mediator getDefaultAccessToken() Flow ");
            response = getDefaultAccessToken(operator, messageContext);
        }
        return response;
    }

    private static String getPoolAccessToken(String owner_id, String resourceURL) throws Exception {
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

    protected static String getDefaultAccessToken(String operator, MessageContext messageContext) throws Exception {
        OperatorApplicationDTO op = null;
        String token = null;

        if (operator == null) {
            return token;
        }

        //String applicationid = getApplicationid();
        String applicationid = (String) messageContext.getProperty("APPLICATION_ID_X");

        if (applicationid == null) {
            throw new CustomException("SVC0001", "", new String[]{"Requested service is not provisioned"});
        }
        OparatorService operatorService = new OparatorService();
        validoperators = operatorService.getApplicationOperators(Integer.valueOf(applicationid));

        if (validoperators.isEmpty()) {
            throw new CustomException("SVC0001", "", new String[]{"Requested service is not provisioned"});
        }

        for (OperatorApplicationDTO d : validoperators) {
            if (d.getOperatorname() != null && d.getOperatorname().contains(operator)) {
                op = d;
                break;
            }
        }
        //
        log.info("Token time : " + op.getTokentime() + " Request ID: " + UID.getRequestID(messageContext));
        log.info("Token validity : " + op.getTokenvalidity() + " Request ID: " + UID.getRequestID(messageContext));

        long timeexpires = (long) (op.getTokentime() + (op.getTokenvalidity() * 1000));

        log.info("Expire time : " + timeexpires + " Request ID: " + UID.getRequestID(messageContext));

        long currtime = new Date().getTime();

        log.info("Current time : " + currtime + " Request ID: " + UID.getRequestID(messageContext));

        if (timeexpires > currtime) {
            token = op.getToken();
            log.info("Token of " + op.getOperatorname() + " operator is active"
                    + " Request ID: " + UID.getRequestID(messageContext));
        } else {

            log.info("Regenerating the token of " + op.getOperatorname() + " operator"
                    + " Request ID: " + UID.getRequestID(messageContext));
            String Strtoken = makeTokenrequest(op.getTokenurl(),
                    "grant_type=refresh_token&refresh_token=" + op.getRefreshtoken(),
                    ("" + op.getTokenauth()),
                    messageContext);
            if (Strtoken != null && Strtoken.length() > 0) {
                log.info("Token regeneration response of " + op.getOperatorname() + " operator : " + Strtoken
                        + " Request ID: " + UID.getRequestID(messageContext));

                JSONObject jsontoken = new JSONObject(Strtoken);
                token = jsontoken.getString("access_token");
                operatorService.updateOperatorToken(op.getOperatorid(),
                        jsontoken.getString("refresh_token"),
                        Long.parseLong(jsontoken.getString("expires_in")),
                        new Date().getTime(), token);

            } else {
                log.error("Token regeneration response of " + op.getOperatorname() + " operator is invalid.");
            }
        }
        //something here

        return token;
    }

    protected static String makeTokenrequest(String tokenurl, String urlParameters, String authheader, MessageContext messageContext) {

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
}
