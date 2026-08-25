package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.NerionHttp;

/**
 * Response from POST /v1/p2p/resolve.
 */
public final class ResolveResult {

    private final String tunnelId;
    private final String type;
    private final String instanceId;
    private final String instanceName;
    private final String ownerDisplayName;
    private final String ownerAvatarUrl;

    public ResolveResult(String tunnelId, String type, String instanceId, String instanceName,
                         String ownerDisplayName, String ownerAvatarUrl) {
        this.tunnelId = tunnelId;
        this.type = type;
        this.instanceId = instanceId;
        this.instanceName = instanceName;
        this.ownerDisplayName = ownerDisplayName;
        this.ownerAvatarUrl = ownerAvatarUrl;
    }

    public String getTunnelId() { return tunnelId; }
    public String getType() { return type; }
    public String getInstanceId() { return instanceId; }
    public String getInstanceName() { return instanceName; }
    public String getOwnerDisplayName() { return ownerDisplayName; }
    public String getOwnerAvatarUrl() { return ownerAvatarUrl; }

    public static ResolveResult fromJson(String json) {
        String tunnelId = NerionHttp.extractString(json, "tunnelId");
        String type = NerionHttp.extractString(json, "type");
        String instanceId = NerionHttp.extractNullableString(json, "instanceId");
        String instanceName = NerionHttp.extractNullableString(json, "instanceName");
        String ownerDisplayName = NerionHttp.extractString(json, "ownerDisplayName");
        String ownerAvatarUrl = NerionHttp.extractNullableString(json, "ownerAvatarUrl");
        return new ResolveResult(tunnelId, type, instanceId, instanceName, ownerDisplayName, ownerAvatarUrl);
    }
}
