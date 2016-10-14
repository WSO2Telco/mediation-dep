
package lk.dialog.smsc.mife.request.subscription.delinfonotify;

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

    public DeliveryInfoNotification withCallbackData(String callbackData) {
        this.callbackData = callbackData;
        return this;
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

    public DeliveryInfoNotification withDeliveryInfo(DeliveryInfo deliveryInfo) {
        this.deliveryInfo = deliveryInfo;
        return this;
    }

}
