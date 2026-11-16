package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.Json;
import me.feeldev.nerionp2p.internal.JsonObject;

/**
 * Response from GET /v1/net/free-port.
 */
public final class FreePortResponse {

    private final int port;

    public FreePortResponse(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public static FreePortResponse fromJson(String json) {
        JsonObject o = Json.parseObject(json);
        int port = o.getInt("port");
        return new FreePortResponse(port);
    }
}
