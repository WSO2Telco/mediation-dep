
package lk.dialog.smsc.mife.request.subscription.delinfonotify;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class DeliveryInfoNotificationRequest {

    @SerializedName("deliveryInfoNotification")
    @Expose
    private DeliveryInfoNotification deliveryInfoNotification;

    /**
     * 
     * @return
     *     The deliveryInfoNotification
     */
    public DeliveryInfoNotification getDeliveryInfoNotification() {
        return deliveryInfoNotification;
    }

    /**
     * 
     * @param deliveryInfoNotification
     *     The deliveryInfoNotification
     */
    public void setDeliveryInfoNotification(DeliveryInfoNotification deliveryInfoNotification) {
        this.deliveryInfoNotification = deliveryInfoNotification;
    }

    public DeliveryInfoNotificationRequest withDeliveryInfoNotification(DeliveryInfoNotification deliveryInfoNotification) {
        this.deliveryInfoNotification = deliveryInfoNotification;
        return this;
    }

}
