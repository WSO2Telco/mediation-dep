package lk.dialog.smsc.mife.request.sendsms;

public class OutboundSMSMessageRequest {
	private String senderAddress;

	private String senderName;

	private String clientCorrelator;

	private OutboundSMSTextMessage outboundSMSTextMessage;

	private String[] address;

	private ReceiptRequest receiptRequest;

	public String getSenderAddress() {
		return senderAddress;
	}

	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getClientCorrelator() {
		return clientCorrelator;
	}

	public void setClientCorrelator(String clientCorrelator) {
		this.clientCorrelator = clientCorrelator;
	}

	public OutboundSMSTextMessage getOutboundSMSTextMessage() {
		return outboundSMSTextMessage;
	}

	public void setOutboundSMSTextMessage(
			OutboundSMSTextMessage outboundSMSTextMessage) {
		this.outboundSMSTextMessage = outboundSMSTextMessage;
	}

	public String[] getAddress() {
		return address;
	}

	public void setAddress(String[] address) {
		this.address = address;
	}

	public ReceiptRequest getReceiptRequest() {
		return receiptRequest;
	}

	public void setReceiptRequest(ReceiptRequest receiptRequest) {
		this.receiptRequest = receiptRequest;
	}

	@Override
	public String toString() {
		return "ClassPojo [senderAddress = " + senderAddress
				+ ", senderName = " + senderName + ", clientCorrelator = "
				+ clientCorrelator + ", outboundSMSTextMessage = "
				+ outboundSMSTextMessage + ", address = " + address
				+ ", receiptRequest = " + receiptRequest + "]";
	}
}