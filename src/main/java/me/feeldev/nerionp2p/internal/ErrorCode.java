package me.feeldev.nerionp2p.internal;

/**
 * Machine-readable error codes from the API's error envelope ({@code error.code}, doc §6).
 * Always key error handling off this, never off the free-form {@code error.message} —
 * the message text may change wording between launcher versions without notice.
 */
public enum ErrorCode {
    INVALID_PORT("invalid_port"),
    INVALID_TARGET("invalid_target"),
    INVALID_TOKEN("invalid_token"),
    MISSING_SCOPE("missing_scope"),
    NO_ACTIVE_SESSION("no_active_session"),
    P2P_SESSION_ACTIVE("p2p_session_active"),
    TUNNEL_ALREADY_ACTIVE("tunnel_already_active"),
    BACKEND_ERROR("backend_error"),
    AUTH_UNAVAILABLE("auth_unavailable"),
    NOT_FOUND("not_found"),
    /** Doc §8.15 — thrown by {@code inviteFriendToSession} when the caller has no active play session. */
    NOT_PLAYING_INSTANCE("not_playing_instance"),
    /** A code this SDK version doesn't recognize yet. Never throws — forward-compatible. */
    UNKNOWN("unknown");

    private final String wire;

    ErrorCode(String wire) {
        this.wire = wire;
    }

    public String wireValue() {
        return wire;
    }

    public static ErrorCode fromWire(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (ErrorCode c : values()) {
            if (c.wire.equals(value)) {
                return c;
            }
        }
        return UNKNOWN;
    }
}
