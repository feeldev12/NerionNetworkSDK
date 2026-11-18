package me.feeldev.nerionnetwork.models;

/** Role in the current session, from {@code GET /v1/p2p/status}'s {@code role} field. Null means idle. */
public enum ConnectionRole {
    HOST, JOINER,
    /** A role value this SDK version doesn't recognize yet. Never throws — forward-compatible. */
    UNKNOWN;

    /** Returns null for a JSON {@code null} (idle), {@link #UNKNOWN} for an unrecognized non-null value. */
    public static ConnectionRole fromWire(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
