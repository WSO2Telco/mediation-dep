package com.wso2telco.dep.mediator;

import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.dep.mediator.internal.UID;
import com.wso2telco.dep.operatorservice.model.OperatorApplicationDTO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;


/**
 * This mediator clears operator details cached in RequestExecutor.
 */
public class CacheClearingMediator extends AbstractMediator {

    private static Log log = LogFactory.getLog(CacheClearingMediator.class);

    @Override
    public boolean mediate(MessageContext messageContext) {
        Object propertyObject = messageContext.getProperty(HandlerMediator.REQUEST_EXECUTOR);

        if (propertyObject != null) {
            RequestExecutor requestExecutor = (RequestExecutor) propertyObject;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Executing cache clear mediator for Request ID: " + UID.getRequestID(messageContext));
                }
                requestExecutor.clearOperatorDetailsFromCache(messageContext);
            } catch (BusinessException ex) {
                log.error("Error occurred while clearing token.", ex);
            }

        }
        return true;
    }
}
