package me.feeldev.nerionp2p.internal;

/** A launcher response that isn't the JSON this SDK expects (malformed, or a field of the wrong type). */
public final class JsonException extends RuntimeException {

    public JsonException(String message) {
        super(message);
    }
}
