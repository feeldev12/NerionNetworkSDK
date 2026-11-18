package me.feeldev.nerionnetwork.models;

import me.feeldev.nerionnetwork.internal.Json;
import me.feeldev.nerionnetwork.internal.JsonObject;

/**
 * Response from GET /v1/p2p/invite.
 */
public final class InviteInfo {

    private final String url;
    private final String token;
    private final int maxUses;
    private final int usesCount;
    private final String expiresAt;

    public InviteInfo(String url, String token, int maxUses, int usesCount, String expiresAt) {
        this.url = url;
        this.token = token;
        this.maxUses = maxUses;
        this.usesCount = usesCount;
        this.expiresAt = expiresAt;
    }

    public String getUrl() { return url; }
    public String getToken() { return token; }
    public int getMaxUses() { return maxUses; }
    public int getUsesCount() { return usesCount; }
    public String getExpiresAt() { return expiresAt; }

    public static InviteInfo fromJson(String json) {
        JsonObject o = Json.parseObject(json);
        String url = o.getString("url");
        String token = o.getString("token");
        int maxUses = o.getInt("maxUses");
        int usesCount = o.getInt("usesCount");
        String expiresAt = o.getString("expiresAt");
        return new InviteInfo(url, token, maxUses, usesCount, expiresAt);
    }
}
