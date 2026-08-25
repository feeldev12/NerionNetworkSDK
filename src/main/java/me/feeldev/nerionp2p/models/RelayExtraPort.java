package me.feeldev.nerionp2p.models;

/**
 * One extra port mapping as echoed back by {@code POST /v1/p2p/relay} (doc §8.8) —
 * {@code {localPort, protocol}}. Distinct shape from {@link PortMapping}, which is the
 * {@code GET /v1/p2p/ports} (doc §8.9) response shape ({@code {localPort, publicPort}}).
 */
public final class RelayExtraPort {

    private final int localPort;
    private final String protocol;

    public RelayExtraPort(int localPort, String protocol) {
        this.localPort = localPort;
        this.protocol = protocol;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getProtocol() {
        return protocol;
    }
}
