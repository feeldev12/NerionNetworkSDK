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
    private final Integer relayPort;
    private final String visibility;

    public SessionInfo(String tunnelId, String p2pSessionId, String publicHost, int publicPort, Integer relayPort, String visibility) {
        this.tunnelId = tunnelId;
        this.p2pSessionId = p2pSessionId;
        this.publicHost = publicHost;
        this.publicPort = publicPort;
        this.relayPort = relayPort;
        this.visibility = visibility;
    }

    public String getTunnelId() { return tunnelId; }
    public String getP2pSessionId() { return p2pSessionId; }
    public String getPublicHost() { return publicHost; }
    public int getPublicPort() { return publicPort; }

    /** Public relay port, or null when this session isn't (currently) using relay fallback. */
    public Integer getRelayPort() { return relayPort; }

    /** Current visibility of this tunnel, or null if the launcher is too old to report it. */
    public String getVisibility() { return visibility; }

    public static SessionInfo fromJson(String json) {
        String tunnelId = NerionHttp.extractString(json, "tunnelId");
        String p2pSessionId = NerionHttp.extractNullableString(json, "p2pSessionId");
        String publicHost = NerionHttp.extractString(json, "publicHost");
        int publicPort = NerionHttp.extractInt(json, "publicPort");
        Integer relayPort = NerionHttp.extractNullableInt(json, "relayPort");
        String visibility = NerionHttp.extractNullableString(json, "visibility");
        return new SessionInfo(tunnelId, p2pSessionId, publicHost, publicPort, relayPort, visibility);
    }
}
