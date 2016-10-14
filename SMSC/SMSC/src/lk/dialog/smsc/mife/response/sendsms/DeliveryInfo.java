
package lk.dialog.smsc.mife.response.sendsms;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class DeliveryInfo {

    @SerializedName("deliveryStatus")
    @Expose
    private String deliveryStatus;
    @SerializedName("address")
    @Expose
    private String address;

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

}
