
package lk.dialog.smsc.mife.request.subscription.mosms;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class SubscriptionRequest {

    @SerializedName("subscription")
    @Expose
    private Subscription subscription;

    /**
     * 
     * @return
     *     The subscription
     */
    public Subscription getSubscription() {
        return subscription;
    }

    /**
     * 
     * @param subscription
     *     The subscription
     */
    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public SubscriptionRequest withSubscription(Subscription subscription) {
        this.subscription = subscription;
        return this;
    }

}
