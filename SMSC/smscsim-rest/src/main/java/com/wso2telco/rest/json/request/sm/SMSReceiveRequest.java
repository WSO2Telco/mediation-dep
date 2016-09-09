
package com.wso2telco.rest.json.request.sm;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class SMSReceiveRequest {

    @SerializedName("inboundSMSMessageNotification")
    @Expose
    private InboundSMSMessageNotification inboundSMSMessageNotification;

    /**
     * 
     * @return
     *     The inboundSMSMessageNotification
     */
    public InboundSMSMessageNotification getInboundSMSMessageNotification() {
        return inboundSMSMessageNotification;
    }

    /**
     * 
     * @param inboundSMSMessageNotification
     *     The inboundSMSMessageNotification
     */
    public void setInboundSMSMessageNotification(InboundSMSMessageNotification inboundSMSMessageNotification) {
        this.inboundSMSMessageNotification = inboundSMSMessageNotification;
    }

}
