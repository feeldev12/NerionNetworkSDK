package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.Json;
import me.feeldev.nerionp2p.internal.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Response from {@code GET /v1/p2p/ports} (doc §8.9) — current relay extra-port mappings. */
public final class PortsResponse {

    private final List<PortMapping> ports;

    public PortsResponse(List<PortMapping> ports) {
        this.ports = ports;
    }

    public List<PortMapping> getPorts() {
        return ports;
    }

    public static PortsResponse fromJson(String json) {
        List<PortMapping> mappings = new ArrayList<>();
        for (JsonObject port : Json.parseObject(json).getObjectList("ports")) {
            mappings.add(new PortMapping(port.getInt("localPort"), port.getInt("publicPort")));
        }
        return new PortsResponse(mappings);
    }
}
