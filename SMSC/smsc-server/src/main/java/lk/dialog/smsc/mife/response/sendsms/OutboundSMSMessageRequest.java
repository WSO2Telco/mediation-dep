
package lk.dialog.smsc.mife.response.sendsms;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class OutboundSMSMessageRequest {

    @SerializedName("outboundSMSMessageRequest")
    @Expose
    private OutboundSMSMessageRequest_ outboundSMSMessageRequest;

    /**
     * 
     * @return
     *     The outboundSMSMessageRequest
     */
    public OutboundSMSMessageRequest_ getOutboundSMSMessageRequest() {
        return outboundSMSMessageRequest;
    }

    /**
     * 
     * @param outboundSMSMessageRequest
     *     The outboundSMSMessageRequest
     */
    public void setOutboundSMSMessageRequest(OutboundSMSMessageRequest_ outboundSMSMessageRequest) {
        this.outboundSMSMessageRequest = outboundSMSMessageRequest;
    }

}
