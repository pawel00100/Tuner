package com.tuner.utils.rest_client;

public class RequestException extends Exception {

    public RequestException(Throwable cause) {
        super(cause);
    }

    public RequestException(String message) {
        super(message);
    }
}
