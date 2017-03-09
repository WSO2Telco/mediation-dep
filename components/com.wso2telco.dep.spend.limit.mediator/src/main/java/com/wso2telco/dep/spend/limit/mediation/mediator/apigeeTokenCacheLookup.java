package com.wso2telco.dep.spend.limit.mediation.mediator;


import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import java.util.HashMap;

import static com.wso2telco.dep.spend.limit.mediation.mediator.apigeeTokenCache.apigeeTokens;


/**
 * Created by azham on 3/8/17.
 */
public class apigeeTokenCacheLookup extends AbstractMediator {



    public boolean mediate(MessageContext messageContext) {

        try {

            String operator = messageContext.getProperty("operator").toString();

            if (lookUp(operator)){
                token token = apigeeTokens.get(operator);
                messageContext.setProperty("apigeeAccessToken",token.getAccessToken());
                messageContext.setProperty("apigeeRefreshToken",token.getRefreshToken());
            }

            messageContext.setProperty("test1",Integer.toString(apigeeTokens.size()));

        } catch (Exception e) {
            log.info("An error occurred.");
        }

        return true;
    }

    private static boolean lookUp (String operator){

        return apigeeTokens.containsKey(operator);

    }

}
