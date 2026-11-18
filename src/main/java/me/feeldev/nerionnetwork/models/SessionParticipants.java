package me.feeldev.nerionnetwork.models;

import me.feeldev.nerionnetwork.internal.Json;
import me.feeldev.nerionnetwork.internal.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response from {@code GET /v1/p2p/participants}: the players of the session the caller hosts
 * ({@link #isHost()}) or joined, including those who left.
 */
public final class SessionParticipants {

    private final String tunnelId;
    private final boolean host;
    private final List<SessionParticipant> participants;

    public SessionParticipants(String tunnelId, boolean host, List<SessionParticipant> participants) {
        this.tunnelId = tunnelId;
        this.host = host;
        this.participants = Collections.unmodifiableList(new ArrayList<>(participants));
    }

    public String getTunnelId() {
        return tunnelId;
    }

    /** True when the caller hosts the session; false when it joined it as a player. */
    public boolean isHost() {
        return host;
    }

    public List<SessionParticipant> getParticipants() {
        return participants;
    }

    public static SessionParticipants fromJson(String json) {
        JsonObject o = Json.parseObject(json);
        List<SessionParticipant> list = new ArrayList<>();
        for (JsonObject p : o.getObjectList("participants")) {
            list.add(SessionParticipant.fromJson(p));
        }
        return new SessionParticipants(o.getOptionalString("tunnelId"), "HOST".equals(o.getOptionalString("role")), list);
    }
}
