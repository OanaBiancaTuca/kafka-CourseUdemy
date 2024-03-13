package com.appsdeveloperblog.ws.emailnotification.error;

public class RetryableException extends RuntimeException {



    //custom error message
    public RetryableException(String message) {
        super(message);
    }

    public RetryableException(Throwable cause) {
        super(cause);
    }
}
