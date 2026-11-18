package me.feeldev.nerionnetwork.models;

import me.feeldev.nerionnetwork.internal.Json;
import me.feeldev.nerionnetwork.internal.JsonObject;

/**
 * Response from GET /v1/ping.
 */
public final class PingResponse {

    private final int apiVersion;
    private final String[] capabilities;

    public PingResponse(int apiVersion, String[] capabilities) {
        this.apiVersion = apiVersion;
        this.capabilities = capabilities;
    }

    public int getApiVersion() {
        return apiVersion;
    }

    public String[] getCapabilities() {
        return capabilities;
    }

    public boolean hasCapability(String capability) {
        for (String c : capabilities) {
            if (c.equals(capability)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the launcher has the session-players routes (participants, players-event, ban). */
    public boolean supportsSessionPlayers() {
        return hasCapability("p2p.participants");
    }

    public static PingResponse fromJson(String json) {
        JsonObject o = Json.parseObject(json);
        return new PingResponse(o.getInt("apiVersion"), o.getStringList("capabilities").toArray(new String[0]));
    }
}
