package com.wso2telco.dep.mediator.util;

import com.wso2telco.dep.mediator.MSISDNConstants;
import com.wso2telco.dep.mediator.impl.payment.PaymentExecutor;
import com.wso2telco.dep.user.masking.UserMaskHandler;
import com.wso2telco.dep.user.masking.configuration.UserMaskingConfiguration;

import com.wso2telco.dep.user.masking.exceptions.UserMaskingException;
import org.apache.synapse.MessageContext;
import org.json.JSONObject;

public class UserMaskingUtils {

    public static String initializeUserMaskingProperties(PaymentExecutor executor, MessageContext context,
                                                         JSONObject payload) throws UserMaskingException {

        String endUserId = payload.getJSONObject("amountTransaction").getString("endUserId");
        if (executor.isUserAnonymization()) {
            context.setProperty(MSISDNConstants.MASKED_MSISDN, endUserId);
            context.setProperty(MSISDNConstants.MASKED_MSISDN_SUFFIX, ValidationUtils.getMsisdnNumber(endUserId));
            endUserId = UserMaskHandler.transcryptUserId(endUserId, false,
                    UserMaskingConfiguration.getInstance().getSecretKey());
            context.setProperty("MASKED_RESOURCE", context.getProperty("RESOURCE"));
        }
        String msisdn = ValidationUtils.getMsisdnNumber(endUserId);
        context.setProperty(MSISDNConstants.USER_MSISDN, msisdn);
        context.setProperty(MSISDNConstants.MSISDN, endUserId);
        return endUserId;
    }
}
