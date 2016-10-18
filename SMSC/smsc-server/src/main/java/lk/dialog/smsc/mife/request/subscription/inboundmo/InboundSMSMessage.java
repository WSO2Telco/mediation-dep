
package lk.dialog.smsc.mife.request.subscription.inboundmo;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class InboundSMSMessage {

    @SerializedName("dateTime")
    @Expose
    private String dateTime;
    @SerializedName("destinationAddress")
    @Expose
    private String destinationAddress;
    @SerializedName("messageId")
    @Expose
    private String messageId;
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("senderAddress")
    @Expose
    private String senderAddress;

    /**
     * 
     * @return
     *     The dateTime
     */
    public String getDateTime() {
        return dateTime;
    }

    /**
     * 
     * @param dateTime
     *     The dateTime
     */
    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public InboundSMSMessage withDateTime(String dateTime) {
        this.dateTime = dateTime;
        return this;
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

    public InboundSMSMessage withDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
        return this;
    }

    /**
     * 
     * @return
     *     The messageId
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 
     * @param messageId
     *     The messageId
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public InboundSMSMessage withMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    /**
     * 
     * @return
     *     The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * 
     * @param message
     *     The message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    public InboundSMSMessage withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * 
     * @return
     *     The senderAddress
     */
    public String getSenderAddress() {
        return senderAddress;
    }

    /**
     * 
     * @param senderAddress
     *     The senderAddress
     */
    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public InboundSMSMessage withSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
        return this;
    }

}
