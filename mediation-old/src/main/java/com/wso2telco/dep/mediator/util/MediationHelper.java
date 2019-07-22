package com.wso2telco.dep.mediator.util;

import com.wso2telco.core.dbutils.exception.BusinessException;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;
import java.util.Map;

public class MediationHelper {

    private MediationHelper() {}

    private static MediationHelper instance;

    private Log log = LogFactory.getLog(this.getClass());

    public static synchronized MediationHelper getInstance() {
        if (instance == null) {
            instance = new MediationHelper();
        }
        return instance;
    }

    public static final String APP_ID = "APP_ID";

    /**
     * Store application.
     *
     * @param context the context
     * @return the string
     * @throws AxisFault the axis fault
     */
    public String getApplicationId(MessageContext context) throws BusinessException {

        log.debug("Calling getApplicationId");
        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) context).getAxis2MessageContext();

        Object appId = context.getProperty(APP_ID);
        if (appId != null) {
            if (log.isDebugEnabled()) {
                log.debug("Getting Application ID from context : " + appId.toString());
            }
            return appId.toString();
        }

        String applicationid = null;

        Object headers = axis2MessageContext.getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers != null && headers instanceof Map) {
            try {
                Map headersMap = (Map) headers;
                String jwtparam = (String) headersMap.get("x-jwt-assertion");
                if (null == jwtparam) {
                    throw new BusinessException("Can't find 'x-jwt-assertion' header in request");
                }
                String[] jwttoken = jwtparam.split("\\.");
                String jwtbody = new String(Base64.getMimeDecoder().decode(jwttoken[1]));
                JSONObject jwtobj = new JSONObject(jwtbody);
                applicationid = jwtobj.getString("http://wso2.org/claims/applicationid");
                if (log.isDebugEnabled()) {
                    log.debug("Getting Application ID from JWT : " + applicationid);
                }
                context.setProperty(APP_ID, applicationid);

            } catch (JSONException ex) {
                throw new BusinessException("Error retriving application id");
            }
        }
        log.debug("Application ID is : " + applicationid);
        return applicationid;
    }
}
