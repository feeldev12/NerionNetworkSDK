package me.feeldev.nerionnetwork.models;

import me.feeldev.nerionnetwork.internal.Json;
import me.feeldev.nerionnetwork.internal.JsonObject;

/**
 * One entry from {@code GET /v1/social/requests} or {@code GET /v1/social/requests/sent}
 * (doc §8.16) — both endpoints share this exact wire shape.
 * <p>
 * <b>Naming quirk (doc §8.16):</b> for a request returned by {@code GET /v1/social/requests/sent},
 * the {@code from*} fields actually describe the person the request was sent <i>to</i>, not
 * someone it's "from" — this is the backend's own wire shape, not a bug in this model. Don't be
 * misled by the field names when rendering a sent-requests list.
 */
public final class FriendRequest {

    private final String requestId;
    private final String fromPlayerAccountId;
    private final String fromPlayerName;
    private final String fromAvatarUrl;
    private final String fromPrimarySkinUrl;
    private final Long sentAt;

    public FriendRequest(String requestId, String fromPlayerAccountId, String fromPlayerName,
                          String fromAvatarUrl, String fromPrimarySkinUrl, Long sentAt) {
        this.requestId = requestId;
        this.fromPlayerAccountId = fromPlayerAccountId;
        this.fromPlayerName = fromPlayerName;
        this.fromAvatarUrl = fromAvatarUrl;
        this.fromPrimarySkinUrl = fromPrimarySkinUrl;
        this.sentAt = sentAt;
    }

    /**
     * Pass this to {@link me.feeldev.nerionnetwork.NerionNetworkClient#acceptFriendRequest(String)},
     * {@link me.feeldev.nerionnetwork.NerionNetworkClient#rejectFriendRequest(String)} (for a request
     * from {@code getFriendRequests()}), or
     * {@link me.feeldev.nerionnetwork.NerionNetworkClient#cancelFriendRequest(String)} (for a request
     * from {@code getSentFriendRequests()}).
     */
    public String getRequestId() {
        return requestId;
    }

    /** The other party's account id — the sender for a received request, the recipient for a sent one. */
    public String getFromPlayerAccountId() {
        return fromPlayerAccountId;
    }

    /** The other party's display name — see the class-level naming-quirk note. */
    public String getFromPlayerName() {
        return fromPlayerName;
    }

    public String getFromAvatarUrl() {
        return fromAvatarUrl;
    }

    public String getFromPrimarySkinUrl() {
        return fromPrimarySkinUrl;
    }

    /** Unix timestamp in seconds. Null if never recorded. */
    public Long getSentAt() {
        return sentAt;
    }

    public static FriendRequest fromJson(String json) {
        return fromJson(Json.parseObject(json));
    }

    static FriendRequest fromJson(JsonObject o) {
        return new FriendRequest(
                o.getString("requestId"),
                o.getString("fromPlayerAccountId"),
                o.getString("fromPlayerName"),
                o.getOptionalString("fromAvatarUrl"),
                o.getOptionalString("fromPrimarySkinUrl"),
                o.getOptionalLong("sentAt")
        );
    }
}
