package me.feeldev.nerionp2p.internal;

/**
 * Wraps IOException / InterruptedException from HTTP calls into an unchecked exception.
 */
public final class P2PNetworkException extends RuntimeException {

    public P2PNetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public P2PNetworkException(String message) {
        super(message);
    }
}
