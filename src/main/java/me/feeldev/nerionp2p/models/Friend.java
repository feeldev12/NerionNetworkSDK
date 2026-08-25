package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.NerionHttp;

/**
 * One entry from {@code GET /v1/social/friends} (doc §8.14).
 * <p>
 * {@link #getCurrentInstanceId()}/{@link #getCurrentInstanceName()} are only populated when the
 * friend is {@link #isOnline()} and actively playing something — both null otherwise.
 */
public final class Friend {

    private final String friendshipId;
    private final String playerAccountId;
    private final String username;
    private final String playerName;
    private final String mcUsername;
    private final String avatarUrl;
    private final String primarySkinUrl;
    private final boolean online;
    private final String currentInstanceId;
    private final String currentInstanceName;
    private final Long lastActiveAt;
    private final boolean favorite;
    private final String socialLinksJson;

    public Friend(String friendshipId, String playerAccountId, String username, String playerName,
                  String mcUsername, String avatarUrl, String primarySkinUrl, boolean online,
                  String currentInstanceId, String currentInstanceName, Long lastActiveAt,
                  boolean favorite, String socialLinksJson) {
        this.friendshipId = friendshipId;
        this.playerAccountId = playerAccountId;
        this.username = username;
        this.playerName = playerName;
        this.mcUsername = mcUsername;
        this.avatarUrl = avatarUrl;
        this.primarySkinUrl = primarySkinUrl;
        this.online = online;
        this.currentInstanceId = currentInstanceId;
        this.currentInstanceName = currentInstanceName;
        this.lastActiveAt = lastActiveAt;
        this.favorite = favorite;
        this.socialLinksJson = socialLinksJson;
    }

    public String getFriendshipId() {
        return friendshipId;
    }

    /**
     * Stable account identifier — pass this to
     * {@link me.feeldev.nerionp2p.NerionP2PClient#inviteFriendToSession(String)}.
     */
    public String getPlayerAccountId() {
        return playerAccountId;
    }

    public String getUsername() {
        return username;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMcUsername() {
        return mcUsername;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPrimarySkinUrl() {
        return primarySkinUrl;
    }

    public boolean isOnline() {
        return online;
    }

    /** Null unless {@link #isOnline()} and the friend is actively playing something. */
    public String getCurrentInstanceId() {
        return currentInstanceId;
    }

    /** Null unless {@link #isOnline()} and the friend is actively playing something. */
    public String getCurrentInstanceName() {
        return currentInstanceName;
    }

    /** Unix timestamp in seconds. Null if never recorded. */
    public Long getLastActiveAt() {
        return lastActiveAt;
    }

    public boolean isFavorite() {
        return favorite;
    }

    /** Raw JSON text of the {@code socialLinks} object — the API doesn't document a fixed shape for it. */
    public String getSocialLinksJson() {
        return socialLinksJson;
    }

    public static Friend fromJson(String json) {
        return new Friend(
                NerionHttp.extractString(json, "friendshipId"),
                NerionHttp.extractString(json, "playerAccountId"),
                NerionHttp.extractString(json, "username"),
                NerionHttp.extractString(json, "playerName"),
                NerionHttp.extractString(json, "mcUsername"),
                NerionHttp.extractNullableString(json, "avatarUrl"),
                NerionHttp.extractNullableString(json, "primarySkinUrl"),
                NerionHttp.extractBoolean(json, "online"),
                NerionHttp.extractNullableString(json, "currentInstanceId"),
                NerionHttp.extractNullableString(json, "currentInstanceName"),
                NerionHttp.extractNullableLong(json, "lastActiveAt"),
                NerionHttp.extractBoolean(json, "favorite"),
                NerionHttp.extractRawObject(json, "socialLinks")
        );
    }
}
