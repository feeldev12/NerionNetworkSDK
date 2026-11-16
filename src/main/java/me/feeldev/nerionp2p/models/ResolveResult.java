package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.Json;
import me.feeldev.nerionp2p.internal.JsonObject;

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
        JsonObject o = Json.parseObject(json);
        String tunnelId = o.getString("tunnelId");
        String type = o.getString("type");
        String instanceId = o.getOptionalString("instanceId");
        String instanceName = o.getOptionalString("instanceName");
        String ownerDisplayName = o.getString("ownerDisplayName");
        String ownerAvatarUrl = o.getOptionalString("ownerAvatarUrl");
        return new ResolveResult(tunnelId, type, instanceId, instanceName, ownerDisplayName, ownerAvatarUrl);
    }
}
