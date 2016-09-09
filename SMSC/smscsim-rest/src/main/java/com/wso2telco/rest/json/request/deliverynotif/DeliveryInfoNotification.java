
package com.wso2telco.rest.json.request.deliverynotif;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class DeliveryInfoNotification {

    @SerializedName("callbackData")
    @Expose
    private String callbackData;
    @SerializedName("deliveryInfo")
    @Expose
    private DeliveryInfo deliveryInfo;

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
     *     The deliveryInfo
     */
    public DeliveryInfo getDeliveryInfo() {
        return deliveryInfo;
    }

    /**
     * 
     * @param deliveryInfo
     *     The deliveryInfo
     */
    public void setDeliveryInfo(DeliveryInfo deliveryInfo) {
        this.deliveryInfo = deliveryInfo;
    }

}
