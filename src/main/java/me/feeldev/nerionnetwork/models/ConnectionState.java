package me.feeldev.nerionnetwork.models;

/**
 * Connection state as reported by {@code GET /v1/p2p/status}'s {@code state} field (doc §8.12).
 * <p>
 * Note: this is a different, smaller vocabulary than {@link HostResult#getState()}'s raw
 * {@code "STARTING"} value from {@code POST /v1/p2p/host} — the two endpoints don't share a
 * state enum, so {@code HostResult} intentionally keeps its state as a plain String.
 */
public enum ConnectionState {
    IDLE, DETECTING_NAT, SIGNALING, PUNCHING, HANDSHAKING, CONNECTED,
    FALLBACK_PENDING, FALLBACK, ERROR,
    /** A state value this SDK version doesn't recognize yet. Never throws — forward-compatible. */
    UNKNOWN;

    public static ConnectionState fromWire(String value) {
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
