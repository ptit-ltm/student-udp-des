package com.studentudp.server;

/**
 * Thrown when DES key validation, encryption, Base64 decoding, or decryption fails.
 */
public class DesCryptoException extends Exception {

    public DesCryptoException(String message) {
        super(message);
    }

    public DesCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
