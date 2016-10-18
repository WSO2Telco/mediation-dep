package lk.dialog.smsc.mife.request.sendsms;

public class OutboundSMSTextMessage {
	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "ClassPojo [message = " + message + "]";
	}
}