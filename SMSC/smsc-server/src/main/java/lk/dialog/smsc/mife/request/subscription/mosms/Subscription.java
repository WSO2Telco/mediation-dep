
package lk.dialog.smsc.mife.request.subscription.mosms;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Subscription {

    @SerializedName("callbackReference")
    @Expose
    private CallbackReference callbackReference;
    @SerializedName("destinationAddresses")
    @Expose
    private List<DestinationAddress> destinationAddresses = new ArrayList<DestinationAddress>();
    @SerializedName("notificationFormat")
    @Expose
    private String notificationFormat;
    @SerializedName("clientCorrelator")
    @Expose
    private String clientCorrelator;

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

    public Subscription withCallbackReference(CallbackReference callbackReference) {
        this.callbackReference = callbackReference;
        return this;
    }

    /**
     * 
     * @return
     *     The destinationAddresses
     */
    public List<DestinationAddress> getDestinationAddresses() {
        return destinationAddresses;
    }

    /**
     * 
     * @param destinationAddresses
     *     The destinationAddresses
     */
    public void setDestinationAddresses(List<DestinationAddress> destinationAddresses) {
        this.destinationAddresses = destinationAddresses;
    }

    public Subscription withDestinationAddresses(List<DestinationAddress> destinationAddresses) {
        this.destinationAddresses = destinationAddresses;
        return this;
    }

    /**
     * 
     * @return
     *     The notificationFormat
     */
    public String getNotificationFormat() {
        return notificationFormat;
    }

    /**
     * 
     * @param notificationFormat
     *     The notificationFormat
     */
    public void setNotificationFormat(String notificationFormat) {
        this.notificationFormat = notificationFormat;
    }

    public Subscription withNotificationFormat(String notificationFormat) {
        this.notificationFormat = notificationFormat;
        return this;
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

    public Subscription withClientCorrelator(String clientCorrelator) {
        this.clientCorrelator = clientCorrelator;
        return this;
    }

}
