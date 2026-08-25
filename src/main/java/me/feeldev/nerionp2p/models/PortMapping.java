package me.feeldev.nerionp2p.models;

/**
 * A port mapping entry (localPort → publicPort).
 */
public final class PortMapping {

    private final int localPort;
    private final int publicPort;

    public PortMapping(int localPort, int publicPort) {
        this.localPort = localPort;
        this.publicPort = publicPort;
    }

    public int getLocalPort() { return localPort; }
    public int getPublicPort() { return publicPort; }
}
