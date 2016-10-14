
package lk.dialog.smsc.mife.request.subscription.mosms;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class DestinationAddress {

    @SerializedName("destinationAddress")
    @Expose
    private String destinationAddress;
    @SerializedName("operatorCode")
    @Expose
    private String operatorCode;
    @SerializedName("criteria")
    @Expose
    private String criteria;

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

    public DestinationAddress withDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
        return this;
    }

    /**
     * 
     * @return
     *     The operatorCode
     */
    public String getOperatorCode() {
        return operatorCode;
    }

    /**
     * 
     * @param operatorCode
     *     The operatorCode
     */
    public void setOperatorCode(String operatorCode) {
        this.operatorCode = operatorCode;
    }

    public DestinationAddress withOperatorCode(String operatorCode) {
        this.operatorCode = operatorCode;
        return this;
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

    public DestinationAddress withCriteria(String criteria) {
        this.criteria = criteria;
        return this;
    }

}
