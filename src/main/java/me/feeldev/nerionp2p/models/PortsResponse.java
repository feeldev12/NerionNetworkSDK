package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.NerionHttp;

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
        List<String> objs = NerionHttp.extractObjectArray(json, "ports");
        List<PortMapping> mappings = new ArrayList<>(objs.size());
        for (String obj : objs) {
            int localPort = NerionHttp.extractInt(obj, "localPort");
            int publicPort = NerionHttp.extractInt(obj, "publicPort");
            mappings.add(new PortMapping(localPort, publicPort));
        }
        return new PortsResponse(mappings);
    }
}
