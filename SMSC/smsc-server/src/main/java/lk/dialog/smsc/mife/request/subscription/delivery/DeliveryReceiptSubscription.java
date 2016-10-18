
package lk.dialog.smsc.mife.request.subscription.delivery;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class DeliveryReceiptSubscription {

    @SerializedName("callbackReference")
    @Expose
    private CallbackReference callbackReference;
    @SerializedName("clientCorrelator")
    @Expose
    private String clientCorrelator;
    @SerializedName("senderAddresses")
    @Expose
    private List<SenderAddress> senderAddresses = new ArrayList<SenderAddress>();

    /**
     * 
     * @return
     *     The callbackReference
     */
    public CallbackReference getCallbackReference() {
        return callbackReference;
    }

    /**
     * 
     * @param callbackReference
     *     The callbackReference
     */
    public void setCallbackReference(CallbackReference callbackReference) {
        this.callbackReference = callbackReference;
    }

    /**
     * 
     * @return
     *     The clientCorrelator
     */
    public String getClientCorrelator() {
        return clientCorrelator;
    }

    /**
     * 
     * @param clientCorrelator
     *     The clientCorrelator
     */
    public void setClientCorrelator(String clientCorrelator) {
        this.clientCorrelator = clientCorrelator;
    }

    /**
     * 
     * @return
     *     The senderAddresses
     */
    public List<SenderAddress> getSenderAddresses() {
        return senderAddresses;
    }

    /**
     * 
     * @param senderAddresses
     *     The senderAddresses
     */
    public void setSenderAddresses(List<SenderAddress> senderAddresses) {
        this.senderAddresses = senderAddresses;
    }

}
