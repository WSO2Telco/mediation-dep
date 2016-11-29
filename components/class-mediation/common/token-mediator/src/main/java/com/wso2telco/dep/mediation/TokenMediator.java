package com.wso2telco.dep.mediation;

import com.wso2telco.core.dbutils.fileutils.FileReader;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.operatorservice.model.OperatorApplicationDTO;
import com.wso2telco.dep.operatorservice.service.OparatorService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.JSONObject;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Call the operator token service (Token Pool Service/ Default Token Service) and retrieves a Token
 */
public class TokenMediator extends AbstractMediator {

    protected Log log = LogFactory.getLog(this.getClass());

    @Override
    public boolean mediate(MessageContext messageContext) {

        // find the property operator from message context
        String operator = (String) messageContext.getProperty("OPERATOR");
        if (operator == null || operator.isEmpty()) {
            // cannot proceed
            log.error("Unable to find operator from message context");
            return false;
        }

        String token = "";
        FileReader fileReader = new FileReader();
        String file = CarbonUtils.getCarbonConfigDirPath() + File.separator
                + "mediator-conf.properties";
        Map<String, String> mediatorConfMap = fileReader.readPropertyFile(file);

        String tokenPoolService = mediatorConfMap.get("tokenpoolservice");
        String resourceURL = mediatorConfMap.get("tokenpoolResourceURL");
        log.info("tokenPoolService Enabled: " + tokenPoolService + "with tokenPoolService URL: " + resourceURL);

        if (tokenPoolService != null && resourceURL != null && tokenPoolService.equals("true")) {
            log.info("Token Pool Service getPoolAccessToken() Flow ");
            try {
                token = getPoolAccessToken(operator, resourceURL);
            } catch (Exception e) {
                log.error(e);
                return false;
            }
        } else {
            log.info("Axiata Mediator getDefaultAccessToken() Flow ");
            try {
                token = getDefaultAccessToken(operator, messageContext);
            } catch (Exception e) {
                log.error(e);
                return false;
            }
        }

        // set the token as a transport header
        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();
        Object headers = axis2MessageContext
                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers != null && headers instanceof Map) {
            Map headersMap = (Map) headers;
            headersMap.put("Authorization", "Bearer " + token);
        }

        return true;
    }

    /**
     * Retrieves the token from the token pool service.
     *
     * @param operator which is operator in Token Pool service
     * @return access token
     * @throws Exception if an error occurs while retrieving the access token
     */
    private String getPoolAccessToken(String operator, String resourceURL) throws Exception {
        StringBuffer result = new StringBuffer();
        HttpURLConnection poolConnection = null;
        URL requestUrl;

        try {
            requestUrl = new URL(resourceURL + URLEncoder.encode(operator, "UTF-8"));
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
     * @return access token
     * @throws Exception if any error occurs while retrieving
     */
    private String getDefaultAccessToken(String operator, MessageContext messageContext) throws
            Exception {


        List<OperatorApplicationDTO> validoperators = null;
        OperatorApplicationDTO op = null;
        String token = null;

        if (operator == null) {
            return token;
        }

        String applicationid = (String) messageContext.getProperty("APPLICATION_ID");
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
        log.info("Token time : " + op.getTokentime() + " Request ID: " + getRequestID(messageContext));
        log.info("Token validity : " + op.getTokenvalidity() + " Request ID: " + getRequestID(messageContext));

        long timeexpires = (long) (op.getTokentime() + (op.getTokenvalidity() * 1000));

        log.info("Expire time : " + timeexpires + " Request ID: " + getRequestID(messageContext));

        long currtime = new Date().getTime();

        log.info("Current time : " + currtime + " Request ID: " + getRequestID(messageContext));

        if (timeexpires > currtime) {
            token = op.getToken();
            log.info("Token of " + op.getOperatorname() + " operator is active"
                    + " Request ID: " + getRequestID(messageContext));
        } else {

            log.info("Regenerating the token of " + op.getOperatorname() + " operator"
                    + " Request ID: " + getRequestID(messageContext));
            String Strtoken = makeTokenrequest(op.getTokenurl(),
                    "grant_type=refresh_token&refresh_token=" + op.getRefreshtoken(),
                    ("" + op.getTokenauth()),
                    messageContext);
            if (Strtoken != null && Strtoken.length() > 0) {
                log.info("Token regeneration response of " + op.getOperatorname() + " operator : " + Strtoken
                        + " Request ID: " + getRequestID(messageContext));

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

    /**
     * Make tokenrequest.
     *
     * @param tokenurl      the tokenurl
     * @param urlParameters the url parameters
     * @param authheader    the authheader
     * @return the string
     */
    protected String makeTokenrequest(String tokenurl, String urlParameters, String authheader, MessageContext messageContext) {

        String retStr = "";

        URL neturl;
        HttpURLConnection connection = null;

        log.info("url : " + tokenurl + " | urlParameters : " + urlParameters + " | authheader : " + authheader
                + " Request ID: " + getRequestID(messageContext));

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


    public static String getRequestID(MessageContext messageContext) {
        String requestId = "";
        if (messageContext != null) {
            requestId = (String) messageContext.getProperty("com.wso2telco.prop.requestId");
        }
        return requestId;
    }
}

