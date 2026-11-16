package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.Json;
import me.feeldev.nerionp2p.internal.JsonObject;

/**
 * Response from POST /v1/p2p/host.
 */
public final class HostResult {

    private final String tunnelId;
    private final String publicHost;
    private final int publicPort;
    private final int relayPort;
    private final String state;

    public HostResult(String tunnelId, String publicHost, int publicPort, int relayPort, String state) {
        this.tunnelId = tunnelId;
        this.publicHost = publicHost;
        this.publicPort = publicPort;
        this.relayPort = relayPort;
        this.state = state;
    }

    public String getTunnelId() { return tunnelId; }
    public String getPublicHost() { return publicHost; }
    public int getPublicPort() { return publicPort; }
    public int getRelayPort() { return relayPort; }
    public String getState() { return state; }

    public static HostResult fromJson(String json) {
        JsonObject o = Json.parseObject(json);
        String tunnelId = o.getString("tunnelId");
        String publicHost = o.getString("publicHost");
        int publicPort = o.getInt("publicPort");
        int relayPort = o.getInt("relayPort");
        String state = o.getString("state");
        return new HostResult(tunnelId, publicHost, publicPort, relayPort, state);
    }
}
