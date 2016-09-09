
package lk.dialog.smsc.mife.response.subscribesms;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Subscription {

    @SerializedName("callbackReference")
    @Expose
    private CallbackReference callbackReference;
    @SerializedName("criteria")
    @Expose
    private String criteria;
    @SerializedName("destinationAddress")
    @Expose
    private String destinationAddress;
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

    /**
     * 
     * @return
     *     The criteria
     */
    public String getCriteria() {
        return criteria;
    }

    /**
     * 
     * @param criteria
     *     The criteria
     */
    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }

    /**
     * 
     * @return
     *     The destinationAddress
     */
    public String getDestinationAddress() {
        return destinationAddress;
    }

    /**
     * 
     * @param destinationAddress
     *     The destinationAddress
     */
    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
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

}
