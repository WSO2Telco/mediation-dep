
package lk.dialog.smsc.mife.request.subscription.delivery;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class CallbackReference {

    @SerializedName("callbackData")
    @Expose
    private String callbackData;
    @SerializedName("notifyURL")
    @Expose
    private String notifyURL;

    /**
     * 
     * @return
     *     The callbackData
     */
    public String getCallbackData() {
        return callbackData;
    }

    /**
     * 
     * @param callbackData
     *     The callbackData
     */
    public void setCallbackData(String callbackData) {
        this.callbackData = callbackData;
    }

    /**
     * 
     * @return
     *     The notifyURL
     */
    public String getNotifyURL() {
        return notifyURL;
    }

    /**
     * 
     * @param notifyURL
     *     The notifyURL
     */
    public void setNotifyURL(String notifyURL) {
        this.notifyURL = notifyURL;
    }

}
