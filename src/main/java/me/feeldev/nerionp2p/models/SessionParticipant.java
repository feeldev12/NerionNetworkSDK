package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.JsonObject;

import java.time.Instant;

/**
 * One player of the session the caller hosts or joined, from {@code GET /v1/p2p/participants}.
 * <p>
 * Includes players who already left ({@link #isInSession()} false, {@link #getLeftAt()} set), so a
 * mod can show them and invite them back. {@link #getPlayerAccountId()} is {@code null} for a
 * player who joined without a Nerion account: show them, but they can't be invited or added.
 */
public final class SessionParticipant {

    private final String playerAccountId;
    private final String displayName;
    private final String avatarUrl;
    private final String minecraftUuid;
    private final String minecraftName;
    private final boolean friend;
    private final boolean inSession;
    private final Instant firstJoinedAt;
    private final Instant leftAt;
    private final boolean banned;
    private final boolean canInvite;

    public SessionParticipant(String playerAccountId, String displayName, String avatarUrl,
                              String minecraftUuid, String minecraftName, boolean friend,
                              boolean inSession, Instant firstJoinedAt, Instant leftAt,
                              boolean banned, boolean canInvite) {
        this.playerAccountId = playerAccountId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.minecraftUuid = minecraftUuid;
        this.minecraftName = minecraftName;
        this.friend = friend;
        this.inSession = inSession;
        this.firstJoinedAt = firstJoinedAt;
        this.leftAt = leftAt;
        this.banned = banned;
        this.canInvite = canInvite;
    }

    /** Nerion account id, for {@link me.feeldev.nerionp2p.NerionP2PClient#inviteToSession(String)}; null without an account. */
    public String getPlayerAccountId() {
        return playerAccountId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getMinecraftUuid() {
        return minecraftUuid;
    }

    public String getMinecraftName() {
        return minecraftName;
    }

    /** Whether this player is a friend of the caller. */
    public boolean isFriend() {
        return friend;
    }

    /** True while the player is in the session; false once they left. */
    public boolean isInSession() {
        return inSession;
    }

    public Instant getFirstJoinedAt() {
        return firstJoinedAt;
    }

    /** When the player left, or {@code null} while they are still in the session. */
    public Instant getLeftAt() {
        return leftAt;
    }

    public boolean isBanned() {
        return banned;
    }

    /** Whether the caller can invite this player to the session right now. */
    public boolean canInvite() {
        return canInvite;
    }

    public boolean hasNerionAccount() {
        return playerAccountId != null;
    }

    static SessionParticipant fromJson(JsonObject o) {
        return new SessionParticipant(
                o.getOptionalString("playerAccountId"),
                o.getOptionalString("displayName"),
                o.getOptionalString("avatarUrl"),
                o.getOptionalString("minecraftUuid"),
                o.getOptionalString("minecraftName"),
                o.has("friend") && o.getBoolean("friend"),
                o.has("inSession") && o.getBoolean("inSession"),
                o.getOptionalInstant("firstJoinedAt"),
                o.getOptionalInstant("leftAt"),
                o.has("banned") && o.getBoolean("banned"),
                o.has("canInvite") && o.getBoolean("canInvite"));
    }
}
