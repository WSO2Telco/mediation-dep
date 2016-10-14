
package lk.dialog.smsc.mife.response.sendsms;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class ReceiptRequest {

    @SerializedName("notifyURL")
    @Expose
    private Object notifyURL;
    @SerializedName("callbackData")
    @Expose
    private Object callbackData;

    /**
     * 
     * @return
     *     The notifyURL
     */
    public Object getNotifyURL() {
        return notifyURL;
    }

    /**
     * 
     * @param notifyURL
     *     The notifyURL
     */
    public void setNotifyURL(Object notifyURL) {
        this.notifyURL = notifyURL;
    }

    /**
     * 
     * @return
     *     The callbackData
     */
    public Object getCallbackData() {
        return callbackData;
    }

    /**
     * 
     * @param callbackData
     *     The callbackData
     */
    public void setCallbackData(Object callbackData) {
        this.callbackData = callbackData;
    }

}
