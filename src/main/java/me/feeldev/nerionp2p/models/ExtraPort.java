package me.feeldev.nerionp2p.models;

/**
 * An additional TCP/UDP port to tunnel alongside the main port, e.g. a Dynmap web port. Used in
 * {@code POST /v1/p2p/host} and {@code POST /v1/p2p/relay} request bodies (doc §8.7, §8.8).
 */
public final class ExtraPort {

    private final int port;
    private final Protocol protocol;

    public ExtraPort(int port, Protocol protocol) {
        this.port = port;
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public String toJson() {
        return "{\"port\":" + port + ",\"protocol\":\"" + protocol.wireValue() + "\"}";
    }
}
