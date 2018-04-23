package com.wso2telco.dep.mediator.util;

import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.dep.mediator.internal.Base64Coder;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class MediationHelper {

    private MediationHelper() {}

    private static MediationHelper instance;

    public static synchronized MediationHelper getInstance() {
        if (instance == null) {
            instance = new MediationHelper();
        }
        return instance;
    }

    /**
     * Store application.
     *
     * @param context the context
     * @return the string
     * @throws AxisFault the axis fault
     */
    public String getApplicationId(MessageContext context) throws BusinessException {
        String applicationid = null;

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) context).getAxis2MessageContext();
        Object headers = axis2MessageContext.getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers != null && headers instanceof Map) {
            try {
                Map headersMap = (Map) headers;
                String jwtparam = (String)headersMap.get("x-jwt-assertion");
                if (null == jwtparam) {
                    throw new BusinessException("Can't find 'x-jwt-assertion' header in request");
                }
                String[] jwttoken = jwtparam.split("\\.");
                String jwtbody = Base64Coder.decodeString(jwttoken[1]);
                JSONObject jwtobj = new JSONObject(jwtbody);
                applicationid = jwtobj.getString("http://wso2.org/claims/applicationid");

            } catch (JSONException ex) {
                throw new BusinessException("Error retriving application id");
            }
        }
        return applicationid;
    }
}
