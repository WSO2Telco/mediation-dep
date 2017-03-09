package com.wso2telco.dep.spend.limit.mediation.mediator;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import java.util.HashMap;


/**
 * Created by azham on 3/3/17.
 */
public class apigeeTokenCache extends AbstractMediator {

   static HashMap<String,token> apigeeTokens = new HashMap<>();

    public boolean mediate(MessageContext messageContext) {

        try {
            String accessToken = messageContext.getProperty("apigeeAccessToken").toString();
            String refreshToken = messageContext.getProperty("apigeeRefreshToken").toString();
            String operator = messageContext.getProperty("operator").toString();

            token t = new token();

            t.setAccessToken(accessToken);
            t.setRefreshToken(refreshToken);
            setTokens(operator,t);

        } catch (Exception e) {
            log.info("An error occurred",e);
        }

        return true;
    }




    private void setTokens(String operator, token token){

       apigeeTokens.put(operator,token);
    }

    private static boolean lookUp (String operator){

        return apigeeTokens.containsKey(operator);

    }

}



 class token {

    private String accessToken;
    private String refreshToken;

    public String getAccessToken(){

        return this.accessToken;
    }

    public String getRefreshToken(){

        return this.refreshToken;
    }

     public void setAccessToken(String accessToken) {
         this.accessToken = accessToken;
     }

     public void setRefreshToken(String refreshToken) {
         this.refreshToken = refreshToken;
     }
 }