package lk.dialog.smsc.exception;

public class SMSCException extends Exception{
	private static final long serialVersionUID = 1L;

	public SMSCException(String message) {
		super(message);
	}

	public SMSCException(Throwable t) {
		super(t);
	}

	public SMSCException(String message, Throwable t) {
		super(message, t);
	}
}
