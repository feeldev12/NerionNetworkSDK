package me.feeldev.nerionp2p.models;

/**
 * Status of the independent relay-sidecar connection, from {@code GET /v1/p2p/status}'s
 * {@code relayStatus} field. This is a SEPARATE state machine from {@link ConnectionState} — a
 * host keeps a relay connection alive as a fallback safety net even while {@code state ==
 * CONNECTED} via P2P, so don't assume this mirrors {@link StatusResponse#getState()}.
 */
public enum RelayStatus {
    IDLE, CONNECTING, ONLINE, STOPPED, ERROR,
    /** A status value this SDK version doesn't recognize yet. Never throws — forward-compatible. */
    UNKNOWN;

    public static RelayStatus fromWire(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
