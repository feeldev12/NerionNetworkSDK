package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.NerionHttp;

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
        String tunnelId = NerionHttp.extractString(json, "tunnelId");
        String publicHost = NerionHttp.extractString(json, "publicHost");
        int publicPort = NerionHttp.extractInt(json, "publicPort");
        int relayPort = NerionHttp.extractInt(json, "relayPort");
        String state = NerionHttp.extractString(json, "state");
        return new HostResult(tunnelId, publicHost, publicPort, relayPort, state);
    }
}
