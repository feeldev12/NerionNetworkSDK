package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.Json;
import me.feeldev.nerionp2p.internal.JsonObject;

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
        JsonObject o = Json.parseObject(json);
        List<RelayExtraPort> extraPorts = new ArrayList<>();
        for (JsonObject port : o.getObjectList("extraPorts")) {
            extraPorts.add(new RelayExtraPort(port.getInt("localPort"), port.getString("protocol")));
        }
        return new RelayResult(o.getString("publicHost"), o.getInt("publicPort"), extraPorts);
    }
}
