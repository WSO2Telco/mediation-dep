package com.wso2telco.refund.exception;

/**
 * Created by yasith on 9/22/16.
 */

public class RefundException extends Exception  {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RefundException(String message) {
        super(message);
    }

    public RefundException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
