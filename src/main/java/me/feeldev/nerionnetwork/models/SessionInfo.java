package me.feeldev.nerionnetwork.models;

import me.feeldev.nerionnetwork.internal.Json;
import me.feeldev.nerionnetwork.internal.JsonObject;

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
        JsonObject o = Json.parseObject(json);
        String tunnelId = o.getString("tunnelId");
        String p2pSessionId = o.getOptionalString("p2pSessionId");
        String publicHost = o.getString("publicHost");
        int publicPort = o.getInt("publicPort");
        Integer relayPort = o.getOptionalInt("relayPort");
        String visibility = o.getOptionalString("visibility");
        return new SessionInfo(tunnelId, p2pSessionId, publicHost, publicPort, relayPort, visibility);
    }
}
