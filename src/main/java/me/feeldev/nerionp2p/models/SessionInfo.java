package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.NerionHttp;

/**
 * Response from POST /v1/p2p/session (create) and GET /v1/p2p/session (get info).
 */
public final class SessionInfo {

    private final String tunnelId;
    private final String p2pSessionId;
    private final String publicHost;
    private final int publicPort;
    private final int relayPort;

    public SessionInfo(String tunnelId, String p2pSessionId, String publicHost, int publicPort, int relayPort) {
        this.tunnelId = tunnelId;
        this.p2pSessionId = p2pSessionId;
        this.publicHost = publicHost;
        this.publicPort = publicPort;
        this.relayPort = relayPort;
    }

    public String getTunnelId() { return tunnelId; }
    public String getP2pSessionId() { return p2pSessionId; }
    public String getPublicHost() { return publicHost; }
    public int getPublicPort() { return publicPort; }
    public int getRelayPort() { return relayPort; }

    public static SessionInfo fromJson(String json) {
        String tunnelId = NerionHttp.extractString(json, "tunnelId");
        String p2pSessionId = NerionHttp.extractNullableString(json, "p2pSessionId");
        String publicHost = NerionHttp.extractString(json, "publicHost");
        int publicPort = NerionHttp.extractInt(json, "publicPort");
        int relayPort = NerionHttp.extractInt(json, "relayPort");
        return new SessionInfo(tunnelId, p2pSessionId, publicHost, publicPort, relayPort);
    }
}
