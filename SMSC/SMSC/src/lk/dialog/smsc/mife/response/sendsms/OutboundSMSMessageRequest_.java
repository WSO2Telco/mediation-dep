
package lk.dialog.smsc.mife.response.sendsms;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class OutboundSMSMessageRequest_ {

    @SerializedName("senderAddress")
    @Expose
    private String senderAddress;
    @SerializedName("clientCorrelator")
    @Expose
    private String clientCorrelator;
    @SerializedName("senderName")
    @Expose
    private Object senderName;
    @SerializedName("receiptRequest")
    @Expose
    private ReceiptRequest receiptRequest;
    @SerializedName("outboundSMSTextMessage")
    @Expose
    private OutboundSMSTextMessage outboundSMSTextMessage;
    @SerializedName("deliveryInfoList")
    @Expose
    private DeliveryInfoList deliveryInfoList;
    @SerializedName("address")
    @Expose
    private List<String> address = new ArrayList<String>();
    @SerializedName("resourceURL")
    @Expose
    private String resourceURL;

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
     *     The senderName
     */
    public Object getSenderName() {
        return senderName;
    }

    /**
     * 
     * @param senderName
     *     The senderName
     */
    public void setSenderName(Object senderName) {
        this.senderName = senderName;
    }

    /**
     * 
     * @return
     *     The receiptRequest
     */
    public ReceiptRequest getReceiptRequest() {
        return receiptRequest;
    }

    /**
     * 
     * @param receiptRequest
     *     The receiptRequest
     */
    public void setReceiptRequest(ReceiptRequest receiptRequest) {
        this.receiptRequest = receiptRequest;
    }

    /**
     * 
     * @return
     *     The outboundSMSTextMessage
     */
    public OutboundSMSTextMessage getOutboundSMSTextMessage() {
        return outboundSMSTextMessage;
    }

    /**
     * 
     * @param outboundSMSTextMessage
     *     The outboundSMSTextMessage
     */
    public void setOutboundSMSTextMessage(OutboundSMSTextMessage outboundSMSTextMessage) {
        this.outboundSMSTextMessage = outboundSMSTextMessage;
    }

    /**
     * 
     * @return
     *     The deliveryInfoList
     */
    public DeliveryInfoList getDeliveryInfoList() {
        return deliveryInfoList;
    }

    /**
     * 
     * @param deliveryInfoList
     *     The deliveryInfoList
     */
    public void setDeliveryInfoList(DeliveryInfoList deliveryInfoList) {
        this.deliveryInfoList = deliveryInfoList;
    }

    /**
     * 
     * @return
     *     The address
     */
    public List<String> getAddress() {
        return address;
    }

    /**
     * 
     * @param address
     *     The address
     */
    public void setAddress(List<String> address) {
        this.address = address;
    }

    /**
     * 
     * @return
     *     The resourceURL
     */
    public String getResourceURL() {
        return resourceURL;
    }

    /**
     * 
     * @param resourceURL
     *     The resourceURL
     */
    public void setResourceURL(String resourceURL) {
        this.resourceURL = resourceURL;
    }

}
