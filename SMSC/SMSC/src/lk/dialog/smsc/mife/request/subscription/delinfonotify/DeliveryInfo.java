
package lk.dialog.smsc.mife.request.subscription.delinfonotify;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class DeliveryInfo {

    @SerializedName("address")
    @Expose
    private String address;
    @SerializedName("deliveryStatus")
    @Expose
    private String deliveryStatus;

    /**
     * 
     * @return
     *     The address
     */
    public String getAddress() {
        return address;
    }

    /**
     * 
     * @param address
     *     The address
     */
    public void setAddress(String address) {
        this.address = address;
    }

    public DeliveryInfo withAddress(String address) {
        this.address = address;
        return this;
    }

    /**
     * 
     * @return
     *     The deliveryStatus
     */
    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    /**
     * 
     * @param deliveryStatus
     *     The deliveryStatus
     */
    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public DeliveryInfo withDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
        return this;
    }

}
