package me.feeldev.nerionnetwork.internal;

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
    /** {@code POST /v1/p2p/join} reached a session whose host hasn't opened it yet — try again shortly. */
    HOST_NOT_READY("host_not_ready"),
    /** {@code POST /v1/p2p/join} reached a friends-only or private session without its invite code. */
    JOIN_NOT_ALLOWED("join_not_allowed"),
    /** The session's host banned the caller: it can't join that session again. */
    TUNNEL_BANNED("tunnel_banned"),
    /** The player being invited is banned from the caller's session. */
    PLAYER_BANNED("player_banned"),
    /** Too many invites to the same non-friend in the last hour — try again later. */
    INVITE_RATE_LIMITED("invite_rate_limited"),
    /** The caller isn't the host or a current player of that session. */
    NOT_TUNNEL_PARTICIPANT("not_tunnel_participant"),
    /** Can't invite that player: the host, the caller themselves, or a non-friend who isn't a recent player of the session. */
    INVALID_INVITEE("invalid_invitee"),
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
