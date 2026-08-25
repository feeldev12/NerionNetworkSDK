package me.feeldev.nerionp2p.models;

/**
 * Connection transport mode — {@code P2P} (direct, NAT-punched) or {@code RELAY} (via a relay
 * server). Appears on {@link JoinResult#getMode()} and {@code GET /v1/p2p/status}'s {@code mode}
 * field. Null means idle or not yet connected.
 */
public enum ConnectionMode {
    P2P, RELAY,
    /** A mode value this SDK version doesn't recognize yet. Never throws — forward-compatible. */
    UNKNOWN;

    /** Returns null for a JSON {@code null}, {@link #UNKNOWN} for an unrecognized non-null value. */
    public static ConnectionMode fromWire(String value) {
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
