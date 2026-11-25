package com.studentudp.client;

/**
 * Exception representing a client-side validation error, UDP timeout/network failure,
 * or an ERROR response returned by the UDP server.
 */
public class ClientRequestException extends Exception {

    public ClientRequestException(String message) {
        super(message);
    }

    public ClientRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
