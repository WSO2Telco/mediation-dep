
package lk.dialog.smsc.mife.response.subscribesms;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class SMSReceiveRequest {

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

}
