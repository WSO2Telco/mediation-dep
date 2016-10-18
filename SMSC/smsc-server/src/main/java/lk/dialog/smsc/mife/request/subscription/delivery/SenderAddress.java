
package lk.dialog.smsc.mife.request.subscription.delivery;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class SenderAddress {

    @SerializedName("senderAddress")
    @Expose
    private String senderAddress;
    @SerializedName("operatorCode")
    @Expose
    private String operatorCode;
    @SerializedName("filterCriteria")
    @Expose
    private String filterCriteria;

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

    /**
     * 
     * @return
     *     The filterCriteria
     */
    public String getFilterCriteria() {
        return filterCriteria;
    }

    /**
     * 
     * @param filterCriteria
     *     The filterCriteria
     */
    public void setFilterCriteria(String filterCriteria) {
        this.filterCriteria = filterCriteria;
    }

}
