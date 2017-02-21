package com.wso2telco.dep.spend.limit.mediation.mediator;

import com.wso2telco.redis.RedisClient;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * This mediator is used to retrieve a value of a key in Redis.
 */
public class PackageTypeLookupMediator extends AbstractMediator {

    public boolean mediate(MessageContext messageContext) {

        try {

            // Assign the fetched value to a messageContext property
            messageContext.setProperty("userpackagetype", RedisClient.getKey(messageContext.getProperty("uri.var.userAddress").toString()));

        } catch (Exception ex) {
            log.error("Error while retrieving key:" + ex.getMessage());
        }

        return true;
    }

}
