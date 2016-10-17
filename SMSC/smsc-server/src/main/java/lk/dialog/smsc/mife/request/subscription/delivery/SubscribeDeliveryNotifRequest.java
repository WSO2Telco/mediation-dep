
package lk.dialog.smsc.mife.request.subscription.delivery;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class SubscribeDeliveryNotifRequest {

    @SerializedName("deliveryReceiptSubscription")
    @Expose
    private DeliveryReceiptSubscription deliveryReceiptSubscription;

    /**
     * 
     * @return
     *     The deliveryReceiptSubscription
     */
    public DeliveryReceiptSubscription getDeliveryReceiptSubscription() {
        return deliveryReceiptSubscription;
    }

    /**
     * 
     * @param deliveryReceiptSubscription
     *     The deliveryReceiptSubscription
     */
    public void setDeliveryReceiptSubscription(DeliveryReceiptSubscription deliveryReceiptSubscription) {
        this.deliveryReceiptSubscription = deliveryReceiptSubscription;
    }

}
