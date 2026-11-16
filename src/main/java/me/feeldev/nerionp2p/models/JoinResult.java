package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.Json;
import me.feeldev.nerionp2p.internal.JsonObject;

/**
 * Response from POST /v1/p2p/join, or from {@link me.feeldev.nerionp2p.NerionP2PClient#joinFriend}
 * when the friend's join target is a ROOM (mode {@code "DIRECT"}).
 * <p>
 * IMPORTANT: branch on {@link #getMode()} — if mode is "RELAY" or "DIRECT",
 * {@link #getLocalBridgePort()} is 0 and you must connect to
 * {@link #getPublicHost()}:{@link #getPublicPort()} instead. {@link #getConnectAddress()} already
 * handles this for you — prefer it over reading {@code localBridgePort} directly.
 */
public final class JoinResult {

    private final String mode;
    private final int localBridgePort;
    private final String publicHost;
    private final int publicPort;

    public JoinResult(String mode, int localBridgePort, String publicHost, int publicPort) {
        this.mode = mode;
        this.localBridgePort = localBridgePort;
        this.publicHost = publicHost;
        this.publicPort = publicPort;
    }

    /** "P2P" or "RELAY". */
    public String getMode() { return mode; }

    /**
     * Local TCP port to connect Minecraft to. Only valid when mode is "P2P".
     * When mode is "RELAY", this is 0 — use publicHost:publicPort instead.
     */
    public int getLocalBridgePort() { return localBridgePort; }

    /** Relay server hostname. Always populated on both P2P and RELAY modes. */
    public String getPublicHost() { return publicHost; }

    /** Relay server port. Always populated on both P2P and RELAY modes. */
    public int getPublicPort() { return publicPort; }

    public boolean isP2P() {
        return "P2P".equals(mode);
    }

    public boolean isRelay() {
        return "RELAY".equals(mode);
    }

    /**
     * True for a direct join to a friend's ROOM via {@link me.feeldev.nerionp2p.NerionP2PClient#joinFriend}.
     * Like {@link #isRelay()}, {@link #getLocalBridgePort()} is 0 in this mode — use
     * {@link #getConnectAddress()}/{@link #getConnectHost()}/{@link #getConnectPort()} instead.
     */
    public boolean isDirect() {
        return "DIRECT".equals(mode);
    }

    /**
     * Returns the address to connect Minecraft to, handling the relay-fallback trap.
     * For P2P: returns "127.0.0.1:{localBridgePort}"
     * For RELAY: returns "{publicHost}:{publicPort}"
     */
    public String getConnectAddress() {
        if (isP2P()) {
            return "127.0.0.1:" + localBridgePort;
        }
        return publicHost + ":" + publicPort;
    }

    public String getConnectHost() {
        return isP2P() ? "127.0.0.1" : publicHost;
    }

    public int getConnectPort() {
        return isP2P() ? localBridgePort : publicPort;
    }

    public static JoinResult fromJson(String json) {
        JsonObject o = Json.parseObject(json);
        String mode = o.getString("mode");
        int localBridgePort = o.getInt("localBridgePort");
        String publicHost = o.getString("publicHost");
        int publicPort = o.getInt("publicPort");
        return new JoinResult(mode, localBridgePort, publicHost, publicPort);
    }
}
