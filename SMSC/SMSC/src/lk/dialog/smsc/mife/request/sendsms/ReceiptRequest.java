package lk.dialog.smsc.mife.request.sendsms;

public class ReceiptRequest {
	private String notifyURL;

	private String callbackData;

	public String getNotifyURL() {
		return notifyURL;
	}

	public void setNotifyURL(String notifyURL) {
		this.notifyURL = notifyURL;
	}

	public String getCallbackData() {
		return callbackData;
	}

	public void setCallbackData(String callbackData) {
		this.callbackData = callbackData;
	}

	@Override
	public String toString() {
		return "ClassPojo [notifyURL = " + notifyURL + ", callbackData = "
				+ callbackData + "]";
	}
}