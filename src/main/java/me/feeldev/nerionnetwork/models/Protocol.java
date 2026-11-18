package me.feeldev.nerionnetwork.models;

/** Transport protocol for an {@link ExtraPort}. */
public enum Protocol {
    TCP("tcp"), UDP("udp");

    private final String wire;

    Protocol(String wire) {
        this.wire = wire;
    }

    public String wireValue() {
        return wire;
    }
}
