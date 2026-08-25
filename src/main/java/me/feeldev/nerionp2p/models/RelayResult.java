package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.NerionHttp;

import java.util.ArrayList;
import java.util.List;

/**
 * Response from {@code POST /v1/p2p/relay} (doc §8.8) — relay-only connection, skipping P2P
 * entirely. Use this when you already know P2P won't work in this environment.
 */
public final class RelayResult {

    private final String publicHost;
    private final int publicPort;
    private final List<RelayExtraPort> extraPorts;

    public RelayResult(String publicHost, int publicPort, List<RelayExtraPort> extraPorts) {
        this.publicHost = publicHost;
        this.publicPort = publicPort;
        this.extraPorts = extraPorts;
    }

    public String getPublicHost() {
        return publicHost;
    }

    public int getPublicPort() {
        return publicPort;
    }

    public List<RelayExtraPort> getExtraPorts() {
        return extraPorts;
    }

    public static RelayResult fromJson(String json) {
        String publicHost = NerionHttp.extractString(json, "publicHost");
        int publicPort = NerionHttp.extractInt(json, "publicPort");
        List<String> objs = NerionHttp.extractObjectArray(json, "extraPorts");
        List<RelayExtraPort> extraPorts = new ArrayList<>(objs.size());
        for (String obj : objs) {
            int localPort = NerionHttp.extractInt(obj, "localPort");
            String protocol = NerionHttp.extractString(obj, "protocol");
            extraPorts.add(new RelayExtraPort(localPort, protocol));
        }
        return new RelayResult(publicHost, publicPort, extraPorts);
    }
}
