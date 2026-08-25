package me.feeldev.nerionp2p.models;

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
        int apiVersion = me.feeldev.nerionp2p.internal.NerionHttp.extractInt(json, "apiVersion");
        // Simple array extraction for capabilities
        String capStr = json.substring(json.indexOf("\"capabilities\""));
        int bracketStart = capStr.indexOf('[');
        int bracketEnd = capStr.indexOf(']');
        String inner = capStr.substring(bracketStart + 1, bracketEnd);
        String[] capabilities = inner.isEmpty() ? new String[0] :
                inner.replace("\"", "").split(",");
        return new PingResponse(apiVersion, capabilities);
    }
}
