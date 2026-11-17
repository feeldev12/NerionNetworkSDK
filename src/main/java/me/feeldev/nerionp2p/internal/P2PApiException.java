package me.feeldev.nerionp2p.internal;

/**
 * Thrown when the launcher's mod API returns a non-2xx HTTP response.
 * The {@link #getErrorCode()} field contains the machine-readable error code from the JSON
 * envelope (doc §6) — key your handling off that, never off {@link #getApiMessage()}, which is a
 * free-form human string that may change wording between launcher versions without notice.
 */
public final class P2PApiException extends RuntimeException {

    private final int httpStatus;
    private final ErrorCode errorCode;
    private final String apiMessage;

    public P2PApiException(int httpStatus, String responseBody) {
        super("HTTP " + httpStatus + ": " + responseBody);
        this.httpStatus = httpStatus;
        JsonObject error = errorObject(responseBody);
        this.errorCode = ErrorCode.fromWire(field(error, "code"));
        this.apiMessage = field(error, "message");
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** Machine-readable error code. Prefer this over {@link #getApiMessage()} for control flow. */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** Legacy string accessor — prefer {@link #getErrorCode()}. */
    public String getCode() {
        return errorCode.wireValue();
    }

    /** Free-form human-readable message from the launcher. For logging only, not control flow. */
    public String getApiMessage() {
        return apiMessage;
    }

    public boolean isInvalidToken() {
        return errorCode == ErrorCode.INVALID_TOKEN;
    }

    public boolean isMissingScope() {
        return errorCode == ErrorCode.MISSING_SCOPE;
    }

    public boolean isP2pSessionActive() {
        return errorCode == ErrorCode.P2P_SESSION_ACTIVE;
    }

    public boolean isTunnelAlreadyActive() {
        return errorCode == ErrorCode.TUNNEL_ALREADY_ACTIVE;
    }

    public boolean isNoActiveSession() {
        return errorCode == ErrorCode.NO_ACTIVE_SESSION;
    }

    public boolean isInvalidPort() {
        return errorCode == ErrorCode.INVALID_PORT;
    }

    public boolean isInvalidTarget() {
        return errorCode == ErrorCode.INVALID_TARGET;
    }

    public boolean isBackendError() {
        return errorCode == ErrorCode.BACKEND_ERROR;
    }

    public boolean isAuthUnavailable() {
        return errorCode == ErrorCode.AUTH_UNAVAILABLE;
    }

    public boolean isNotFound() {
        return errorCode == ErrorCode.NOT_FOUND;
    }

    /** True when {@code inviteFriendToSession} was called with no active play session (doc §8.15). */
    public boolean isNotPlayingInstance() {
        return errorCode == ErrorCode.NOT_PLAYING_INSTANCE;
    }

    /** True when the session's host hasn't opened it yet: wait and join again (doc §8.10). */
    public boolean isHostNotReady() {
        return errorCode == ErrorCode.HOST_NOT_READY;
    }

    /** True when the session is only for the host's friends and no invite code was given (doc §8.10). */
    public boolean isJoinNotAllowed() {
        return errorCode == ErrorCode.JOIN_NOT_ALLOWED;
    }

    /** The {@code "error"} object of the envelope; null when the body isn't one (e.g. a proxy's HTML page). */
    private static JsonObject errorObject(String body) {
        try {
            return Json.parseObject(body).getOptionalObject("error");
        } catch (JsonException e) {
            return null;
        }
    }

    private static String field(JsonObject error, String key) {
        try {
            return error == null ? null : error.getOptionalString(key);
        } catch (JsonException e) {
            return null;
        }
    }
}
