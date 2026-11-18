package me.feeldev.nerionnetwork.models;

import me.feeldev.nerionnetwork.internal.Json;
import me.feeldev.nerionnetwork.internal.JsonObject;

/**
 * Response from {@code GET /v1/p2p/status} (doc §8.12) — poll-safe, no side effects, safe to call
 * repeatedly. Both the hosting flow (§9) and joining flow (§10) require polling this every ~2s to
 * find out when the connection actually comes up.
 */
public final class StatusResponse {

    private final ConnectionState state;
    private final ConnectionRole role;
    private final ConnectionMode mode;
    private final String tunnelId;
    private final Integer localBridgePort;
    private final Integer lanPort;
    private final RelayStatus relayStatus;

    public StatusResponse(ConnectionState state, ConnectionRole role, ConnectionMode mode, String tunnelId,
                           Integer localBridgePort, Integer lanPort, RelayStatus relayStatus) {
        this.state = state;
        this.role = role;
        this.mode = mode;
        this.tunnelId = tunnelId;
        this.localBridgePort = localBridgePort;
        this.lanPort = lanPort;
        this.relayStatus = relayStatus;
    }

    public ConnectionState getState() {
        return state;
    }

    /** Null while idle (nobody hosting or joining on this session). */
    public ConnectionRole getRole() {
        return role;
    }

    /** Null while idle or not yet connected. */
    public ConnectionMode getMode() {
        return mode;
    }

    public String getTunnelId() {
        return tunnelId;
    }

    /** Null unless currently connected via P2P. */
    public Integer getLocalBridgePort() {
        return localBridgePort;
    }

    /** Only populated for role HOST — the local Minecraft port being forwarded. Always null for JOINER. */
    public Integer getLanPort() {
        return lanPort;
    }

    public RelayStatus getRelayStatus() {
        return relayStatus;
    }

    public boolean isConnected() {
        return state == ConnectionState.CONNECTED;
    }

    public boolean isError() {
        return state == ConnectionState.ERROR;
    }

    public static StatusResponse fromJson(String json) {
        JsonObject o = Json.parseObject(json);
        ConnectionState state = ConnectionState.fromWire(o.getOptionalString("state"));
        ConnectionRole role = ConnectionRole.fromWire(o.getOptionalString("role"));
        ConnectionMode mode = ConnectionMode.fromWire(o.getOptionalString("mode"));
        String tunnelId = o.getOptionalString("tunnelId");
        Integer localBridgePort = o.getOptionalInt("localBridgePort");
        Integer lanPort = o.getOptionalInt("lanPort");
        RelayStatus relayStatus = RelayStatus.fromWire(o.getOptionalString("relayStatus"));
        return new StatusResponse(state, role, mode, tunnelId, localBridgePort, lanPort, relayStatus);
    }
}
