package com.wso2telco.dep.spend.limit.mediation.mediator;

import com.wso2telco.redis.RedisClient;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * This mediator is used to insert a key-value pair in Redis with a ttl.
 */
public class PackageTypeCacheMediator extends AbstractMediator {

    // Default ttl value is set to 6 hours for all operators
    private static final int DEFAULT_TTL = 21600;



    public boolean mediate(MessageContext messageContext) {

        try {
            RedisClient.setKey(messageContext.getProperty("uri.var.userAddress").toString(), messageContext.getProperty("userpackagetype").toString(), getValidTtl(messageContext.getProperty("ttl").toString()));

        } catch (Exception ex) {
            log.error("Error while setting key:" + ex.getMessage());
        }

        return true;

    }

    /**
     * This method checks the validity of ttl request parameter and assign default value if invalid.
     *
     * @param ttlStr
     * @return valid ttl int
     */
    private int getValidTtl(String ttlStr) {

        // If "uri.var.ttl" is valid assign ttl
        if (null != ttlStr && !ttlStr.isEmpty()) {

            try {
                int ttl = Integer.parseInt(ttlStr);

                if (ttl > 0)
                    return ttl;

            } catch (NumberFormatException ex) {
                log.error("ttl set to default. ttl parameter " + ttlStr + " is invalid. " + ex.getMessage());
            }
        }

        return DEFAULT_TTL;
    }
}
