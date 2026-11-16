package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.Json;
import me.feeldev.nerionp2p.internal.JsonObject;

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

    public static PingResponse fromJson(String json) {
        JsonObject o = Json.parseObject(json);
        return new PingResponse(o.getInt("apiVersion"), o.getStringList("capabilities").toArray(new String[0]));
    }
}
