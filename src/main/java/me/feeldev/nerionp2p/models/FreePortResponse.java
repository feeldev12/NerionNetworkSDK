package me.feeldev.nerionp2p.models;

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
        int port = me.feeldev.nerionp2p.internal.NerionHttp.extractInt(json, "port");
        return new FreePortResponse(port);
    }
}
