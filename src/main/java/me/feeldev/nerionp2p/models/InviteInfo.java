package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.NerionHttp;

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
        String url = NerionHttp.extractString(json, "url");
        String token = NerionHttp.extractString(json, "token");
        int maxUses = NerionHttp.extractInt(json, "maxUses");
        int usesCount = NerionHttp.extractInt(json, "usesCount");
        String expiresAt = NerionHttp.extractString(json, "expiresAt");
        return new InviteInfo(url, token, maxUses, usesCount, expiresAt);
    }
}
