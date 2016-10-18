
package com.wso2telco.rest.json.request.sm;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class InboundSMSMessageNotification {

    @SerializedName("callbackData")
    @Expose
    private String callbackData;
    @SerializedName("inboundSMSMessage")
    @Expose
    private InboundSMSMessage inboundSMSMessage;

    /**
     * 
     * @return
     *     The callbackData
     */
    public String getCallbackData() {
        return callbackData;
    }

    /**
     * 
     * @param callbackData
     *     The callbackData
     */
    public void setCallbackData(String callbackData) {
        this.callbackData = callbackData;
    }

    /**
     * 
     * @return
     *     The inboundSMSMessage
     */
    public InboundSMSMessage getInboundSMSMessage() {
        return inboundSMSMessage;
    }

    /**
     * 
     * @param inboundSMSMessage
     *     The inboundSMSMessage
     */
    public void setInboundSMSMessage(InboundSMSMessage inboundSMSMessage) {
        this.inboundSMSMessage = inboundSMSMessage;
    }

}
