package me.feeldev.nerionnetwork.internal;

/**
 * Wraps IOException / InterruptedException from HTTP calls into an unchecked exception.
 */
public final class NetworkException extends RuntimeException {

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkException(String message) {
        super(message);
    }
}
