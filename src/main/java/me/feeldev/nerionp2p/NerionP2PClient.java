package me.feeldev.nerionp2p;

import me.feeldev.nerionp2p.internal.CredentialFileWatcher;
import me.feeldev.nerionp2p.internal.Json;
import me.feeldev.nerionp2p.internal.JsonBuilder;
import me.feeldev.nerionp2p.internal.NerionHttp;
import me.feeldev.nerionp2p.internal.P2PApiException;
import me.feeldev.nerionp2p.internal.P2PNetworkException;
import me.feeldev.nerionp2p.models.ConnectionMode;
import me.feeldev.nerionp2p.models.ConnectionState;
import me.feeldev.nerionp2p.models.ExtraPort;
import me.feeldev.nerionp2p.models.FreePortResponse;
import me.feeldev.nerionp2p.models.Friend;
import me.feeldev.nerionp2p.models.FriendRequest;
import me.feeldev.nerionp2p.models.FriendRequestsResponse;
import me.feeldev.nerionp2p.models.FriendsResponse;
import me.feeldev.nerionp2p.models.HostMode;
import me.feeldev.nerionp2p.models.HostResult;
import me.feeldev.nerionp2p.models.InviteInfo;
import me.feeldev.nerionp2p.models.JoinResult;
import me.feeldev.nerionp2p.models.PingResponse;
import me.feeldev.nerionp2p.models.PortsResponse;
import me.feeldev.nerionp2p.models.RelayResult;
import me.feeldev.nerionp2p.models.ResolveResult;
import me.feeldev.nerionp2p.models.SessionInfo;
import me.feeldev.nerionp2p.models.StatusResponse;
import me.feeldev.nerionp2p.models.Visibility;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Client for the Nerion Launcher's P2P mod API (see {@code docs/mod-api/api-reference.md} in the
 * app-launcher repo for the full contract this class implements).
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>{@link #connect()} — waits for launcher credentials, returns immediately (async), calls
 *       {@code onConnected} once found; or {@link #connect(String, String)} for explicit control.</li>
 *   <li>Use the mutating methods (host, join, stop, etc.) once connected.</li>
 *   <li>{@link #disconnect()} — stops credential polling.</li>
 * </ol>
 * <p>
 * All methods are blocking/synchronous and throw {@link me.feeldev.nerionp2p.internal.P2PApiException}
 * on a non-2xx response (check {@code getErrorCode()}, never the message text) or
 * {@link P2PNetworkException} on a transport-level failure.
 */
public final class NerionP2PClient {

    private static final Logger LOG = Logger.getLogger("NerionP2PSDK");

    private final NerionP2PConfig config;
    /** Set by the credential watcher once the launcher's credential file appears; null until then. */
    private volatile NerionHttp http;
    private CredentialFileWatcher watcher;

    /** A client with the default {@link NerionP2PConfig}: it finds the launcher's credentials by itself. */
    public NerionP2PClient() {
        this(NerionP2PConfig.builder().build());
    }

    public NerionP2PClient(NerionP2PConfig config) {
        this.config = config;
    }

    // ======================== CONNECTION ========================

    /**
     * Connect using the config's credential file (by default the one the launcher writes for this
     * game, see {@link NerionP2PConfig#defaultCredentialFile}). Non-blocking — polls in the background until found.
     */
    public void connect() {
        connect(config.getCredentialFile());
    }

    /** Connect using an explicit credential file path. Non-blocking. */
    public void connect(File credentialFile) {
        LOG.info("[NerionP2P] Waiting for credentials from " + credentialFile + " ...");
        watcher = new CredentialFileWatcher(credentialFile, config.getPollIntervalMs(), LOG,
                this::onCredentials);
        watcher.start();
    }

    /** Connect using an explicit base URL and bearer token — skips credential file polling entirely. */
    public void connect(String baseUrl, String token) {
        onCredentials(baseUrl, token);
    }

    /** Disconnect and stop credential polling (does not call the API — see {@link #stop()} for that). */
    public void disconnect() {
        if (watcher != null) {
            watcher.stop();
        }
    }

    /** True once credentials have been resolved and requests can be made. */
    public boolean isConnected() {
        return http != null;
    }

    // ======================== HEALTH (doc §8.1, §8.2) ========================

    /** {@code GET /v1/ping} — unauthenticated discovery, safe to call before credentials resolve if you already have a base URL. */
    public PingResponse ping() {
        return PingResponse.fromJson(http().get("/v1/ping"));
    }

    /** {@code GET /v1/net/free-port} — asks the launcher for a free local TCP port, scanning from the default floor (49152). Scope {@code net.port}. */
    public FreePortResponse getFreePort() {
        return FreePortResponse.fromJson(http().get("/v1/net/free-port"));
    }

    /** {@code GET /v1/net/free-port?start=} — same, scanning from a custom floor. Scope {@code net.port}. */
    public FreePortResponse getFreePort(int start) {
        return FreePortResponse.fromJson(http().get("/v1/net/free-port?start=" + start));
    }

    // ======================== SESSION (doc §8.3, §8.4) ========================

    /**
     * {@code POST /v1/p2p/session} — creates a tunnel WITHOUT starting P2P/relay yet, using the
     * backend's default visibility. Pair with {@link #host} or {@link #startRelay} afterward, or
     * just call {@link #host} directly — it creates a tunnel implicitly if you haven't. Scope {@code p2p.session}.
     */
    public SessionInfo createSession() {
        return createSession((String) null);
    }

    /** Same as {@link #createSession()}, with an explicit {@link Visibility}. Scope {@code p2p.session}. */
    public SessionInfo createSession(Visibility visibility) {
        return createSession(visibility.name());
    }

    /** Same as {@link #createSession(Visibility)}, with the visibility by name (e.g. {@code "UNLISTED"}). */
    public SessionInfo createSession(String visibility) {
        JsonBuilder body = new JsonBuilder().put("visibility", visibility);
        return SessionInfo.fromJson(http().post("/v1/p2p/session", body.build()));
    }

    /**
     * {@code GET /v1/p2p/session} — join info for MY OWN active tunnel. Requires an active tunnel
     * (from {@link #createSession} or {@link #host}) — throws {@code no_active_session} (404) otherwise.
     * Scope {@code p2p.session}.
     */
    public SessionInfo getSession() {
        return SessionInfo.fromJson(http().get("/v1/p2p/session"));
    }

    /**
     * {@code POST /v1/p2p/visibility} — changes MY OWN active tunnel's visibility, including a
     * session the launcher UI started. Same active-tunnel precondition as {@link #getInvite()}.
     * Scope {@code p2p.session}.
     */
    public void setVisibility(Visibility visibility) {
        setVisibility(visibility.name());
    }

    /** Same as {@link #setVisibility(Visibility)}, with the visibility by name. */
    public void setVisibility(String visibility) {
        JsonBuilder body = new JsonBuilder().put("visibility", visibility);
        http().post("/v1/p2p/visibility", body.build());
    }

    // ======================== DISCOVERY (doc §8.5, §8.6) ========================

    /**
     * {@code GET /v1/p2p/invite} — invite link for MY OWN active tunnel. Works right after
     * {@link #createSession}, before {@link #host}. Same precondition as {@link #getSession()}.
     * The link has no use limit and doesn't expire unless the host sets one in the launcher.
     * Scope {@code p2p.session}.
     */
    public InviteInfo getInvite() {
        return InviteInfo.fromJson(http().get("/v1/p2p/invite"));
    }

    /**
     * {@code POST /v1/p2p/resolve} — looks up what an invite code points to before joining it
     * (e.g. to show a confirmation dialog with the owner's name). Scope {@code p2p.join}.
     */
    public ResolveResult resolveInvite(String inviteCode) {
        JsonBuilder body = new JsonBuilder().put("inviteCode", inviteToken(inviteCode));
        return ResolveResult.fromJson(http().post("/v1/p2p/resolve", body.build()));
    }

    // ======================== HOSTING (doc §8.7, §8.8, §8.9, §8.13) ========================

    /** {@code POST /v1/p2p/host} with just the required {@code lanPort}, default mode ({@code STANDBY}). Scope {@code p2p.host}. */
    public HostResult host(int lanPort) {
        return host(lanPort, null, null, null);
    }

    /** Same as {@link #host(int)}, with an explicit {@link HostMode}. Scope {@code p2p.host}. */
    public HostResult host(int lanPort, HostMode mode) {
        return host(lanPort, mode, null, null);
    }

    /**
     * {@code POST /v1/p2p/host} — the main "open my world for others" call (doc §8.7).
     * <p>
     * Conflict: if this launcher process is already hosting or joining another session, throws
     * {@code p2p_session_active} (409) — call {@link #stop()} first. The response is {@code 202}
     * (async) — poll {@link #status()} to see the connection actually come up.
     *
     * @param lanPort    required, nonzero — the local port your Minecraft server is listening on.
     * @param mode       optional; {@code STANDBY} (default) or {@code SINGLE}.
     * @param extraPorts optional additional ports to tunnel alongside {@code lanPort}.
     * @param tunnelId   optional — reuse a tunnel from {@link #createSession}; must be your own session's tunnel.
     */
    public HostResult host(int lanPort, HostMode mode, List<ExtraPort> extraPorts, String tunnelId) {
        if (lanPort <= 0) {
            throw new IllegalArgumentException("lanPort must be > 0");
        }
        JsonBuilder body = new JsonBuilder().put("lanPort", lanPort);
        if (mode != null) {
            body.put("mode", mode.name());
        }
        if (extraPorts != null && !extraPorts.isEmpty()) {
            body.putRaw("extraPorts", extraPortsToJson(extraPorts));
        }
        body.put("tunnelId", tunnelId);
        return HostResult.fromJson(http().post("/v1/p2p/host", body.build()));
    }

    /** {@code POST /v1/p2p/relay} with just the required {@code localPort} — no extra ports. Scope {@code p2p.host}. */
    public RelayResult startRelay(int localPort) {
        return startRelay(localPort, null);
    }

    /**
     * {@code POST /v1/p2p/relay} — relay-only, skips P2P entirely; use when you already know P2P
     * won't work in this environment. Requires an active tunnel — call {@link #createSession()}
     * first, or use {@link #host} instead if you want the launcher to create the tunnel for you.
     * Scope {@code p2p.host}.
     */
    public RelayResult startRelay(int localPort, List<ExtraPort> extraPorts) {
        if (localPort <= 0) {
            throw new IllegalArgumentException("localPort must be > 0");
        }
        JsonBuilder body = new JsonBuilder().put("localPort", localPort);
        if (extraPorts != null && !extraPorts.isEmpty()) {
            body.putRaw("extraPorts", extraPortsToJson(extraPorts));
        }
        return RelayResult.fromJson(http().post("/v1/p2p/relay", body.build()));
    }

    /** {@code GET /v1/p2p/ports} — current relay extra-port mappings. Scope {@code p2p.host}. */
    public PortsResponse getPorts() {
        return PortsResponse.fromJson(http().get("/v1/p2p/ports"));
    }

    /**
     * {@code POST /v1/p2p/stop} — full teardown: kills the standby loop, all joiner bridges, and
     * the relay sidecar. Idempotent — safe to call even if nothing was running. Scope {@code p2p.host}.
     */
    public void stop() {
        http().postNoBody("/v1/p2p/stop");
    }

    // ======================== JOINING (doc §8.10, §8.11) ========================

    /**
     * {@code POST /v1/p2p/join} using an invite code — join SOMEONE ELSE'S session. See
     * {@link JoinResult#getConnectAddress()} for the relay-fallback trap (doc §7): always branch
     * on {@code mode}, never assume {@code localBridgePort} is populated. Scope {@code p2p.join}.
     * <p>
     * The launcher already retries internally for ~2s before falling back to relay — you don't
     * need your own retry loop around this call.
     * <p>
     * Takes the invite's {@link InviteInfo#getToken() token} or its whole {@link InviteInfo#getUrl() URL}.
     * The code works for anyone, friend or not. Each call counts as one use of the invite, even
     * when connecting fails afterwards.
     */
    public JoinResult joinByInviteCode(String inviteCode) {
        JsonBuilder body = new JsonBuilder().put("inviteCode", inviteToken(inviteCode));
        return JoinResult.fromJson(http().post("/v1/p2p/join", body.build()));
    }

    /**
     * {@code POST /v1/p2p/join} by tunnel ID, without a share code. Scope {@code p2p.join}.
     * <p>
     * Anyone can join a {@link Visibility#PUBLIC} or {@link Visibility#UNLISTED} session this way;
     * a {@code FRIENDS_ONLY} one only takes the host's friends ({@code join_not_allowed} otherwise).
     * Throws {@code host_not_ready} while the host hasn't called {@link #host} yet — see
     * {@link #joinByTunnelIdWhenReady}.
     */
    public JoinResult joinByTunnelId(String tunnelId) {
        return joinByTunnelId(tunnelId, null);
    }

    /**
     * {@link #joinByTunnelId(String)}, waiting up to {@code timeout} for the host to open the
     * session. For rooms where everyone gets the tunnel ID at once and the host's world takes a
     * moment to start. Other errors are thrown right away.
     */
    public JoinResult joinByTunnelIdWhenReady(String tunnelId, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                return joinByTunnelId(tunnelId);
            } catch (P2PApiException e) {
                if (!e.isHostNotReady() || System.currentTimeMillis() + HOST_READY_POLL.toMillis() > deadline) {
                    throw e;
                }
            }
            try {
                Thread.sleep(HOST_READY_POLL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new P2PNetworkException("Interrupted while waiting for the host of " + tunnelId, e);
            }
        }
    }

    /** {@code POST /v1/p2p/join} by tunnel ID with an optional share code. Scope {@code p2p.join}. */
    public JoinResult joinByTunnelId(String tunnelId, String shareCode) {
        JsonBuilder body = new JsonBuilder().put("tunnelId", tunnelId).put("shareCode", shareCode);
        return JoinResult.fromJson(http().post("/v1/p2p/join", body.build()));
    }

    /**
     * {@code POST /v1/p2p/join/stop} — tears down ONLY your own joiner-side bridge; doesn't affect
     * anyone else's hosting session. Idempotent — safe to call even if you were never connected.
     * Scope {@code p2p.join}.
     */
    public void stopJoining() {
        http().postNoBody("/v1/p2p/join/stop");
    }

    // ======================== STATUS (doc §8.12) ========================

    /**
     * {@code GET /v1/p2p/status} — poll-safe, no side effects. Both the hosting and joining flows
     * require polling this every ~2s to find out when the connection actually comes up. Scope {@code p2p.status}.
     */
    public StatusResponse status() {
        return StatusResponse.fromJson(http().get("/v1/p2p/status"));
    }

    /**
     * {@code POST /v1/p2p/peer-mode} — how a player the host's game server sees connecting from
     * {@code 127.0.0.1:port} arrived: {@link ConnectionMode#P2P} over the launcher's bridge,
     * otherwise {@link ConnectionMode#RELAY} (the relay client connects from loopback too). Only
     * meaningful for loopback connections; a player from another address is on the LAN. Scope
     * {@code p2p.status}. Needs a launcher with the route — an older one answers 404.
     */
    public ConnectionMode peerMode(int port) {
        String body = new JsonBuilder().put("port", port).build();
        String response = http().post("/v1/p2p/peer-mode", body);
        return ConnectionMode.fromWire(Json.parseObject(response).getOptionalString("mode"));
    }

    /**
     * Blocks, polling {@link #status()} at {@code pollInterval}, until the state is one of
     * {@code targetStates} or {@code timeout} elapses. A thin convenience wrapper around the
     * manual poll loop from doc §9/§10 — synchronous, matching the rest of this SDK's style.
     * <p>
     * Typical use: {@code awaitState(Duration.ofSeconds(15), Duration.ofSeconds(2), ConnectionState.CONNECTED, ConnectionState.FALLBACK, ConnectionState.ERROR)}.
     *
     * @throws P2PNetworkException if {@code timeout} elapses before any target state is reached, or the wait is interrupted.
     */
    public StatusResponse awaitState(Duration timeout, Duration pollInterval, ConnectionState... targetStates) {
        Set<ConnectionState> targets = new HashSet<>(Arrays.asList(targetStates));
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        StatusResponse last = status();
        while (!targets.contains(last.getState())) {
            if (System.currentTimeMillis() >= deadline) {
                throw new P2PNetworkException("Timed out waiting for state " + targets + "; last state was " + last.getState());
            }
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new P2PNetworkException("Interrupted while waiting for state " + targets, e);
            }
            last = status();
        }
        return last;
    }

    // ======================== SOCIAL (doc §8.14, §8.15) ========================

    /**
     * {@code GET /v1/social/friends} — the caller's friends list, online and offline alike (filter/sort
     * client-side as needed). Same shape whether the caller is a client mod or a server plugin — resolved
     * off the token's identity alone, like every other scoped route. Scope {@code social.friends.read}.
     */
    public FriendsResponse getFriends() {
        return FriendsResponse.fromJson(http().get("/v1/social/friends"));
    }

    /**
     * Joins a friend's currently joinable live session (doc §4.3), branching on
     * {@link Friend#getJoinTargetSource()}:
     * <ul>
     *   <li>{@code "TUNNEL"} — delegates to {@link #joinByTunnelId(String)}, unchanged, the proven path.</li>
     *   <li>{@code "ROOM"} — {@code POST /v1/social/join}, scope {@code p2p.join} (reused, not a new scope).</li>
     * </ul>
     * Always re-check {@link Friend#isJoinable()} before calling this — it throws
     * {@link IllegalArgumentException} immediately, without making any request, if the friend has
     * no valid join target.
     *
     * @throws IllegalArgumentException if {@code friend.isJoinable()} is false.
     */
    public JoinResult joinFriend(Friend friend) {
        String source = friend.getJoinTargetSource();
        if ("TUNNEL".equals(source)) {
            return joinByTunnelId(friend.getJoinTargetId());
        }
        if ("ROOM".equals(source)) {
            JsonBuilder body = new JsonBuilder()
                    .put("source", "ROOM")
                    .put("targetId", friend.getJoinTargetId());
            return JoinResult.fromJson(http().post("/v1/social/join", body.build()));
        }
        throw new IllegalArgumentException(
                "friend has no join target — check Friend#isJoinable() before calling joinFriend()");
    }

    /**
     * {@code POST /v1/social/invite/{friendPlayerAccountId}} — invites the given friend to join the
     * CALLER's currently active play session. No request body — the launcher resolves both "who is
     * calling" (the bearer token) and "which session they're inviting the friend to" (the caller's own
     * active play session, tracked server-side) without either being supplied explicitly, the same
     * "never send your own instanceId" shape every {@code /v1/p2p/*} route already follows.
     * <p>
     * Throws {@link me.feeldev.nerionp2p.internal.P2PApiException} with
     * {@link me.feeldev.nerionp2p.internal.P2PApiException#isNotPlayingInstance()} true if the caller
     * has no active play session right now — you must actually be playing an instance before you can
     * invite someone to it. This route does not confirm the invite was accepted, only that it was sent.
     * Scope {@code social.friends.invite}.
     *
     * @param friendPlayerAccountId {@link Friend#getPlayerAccountId()} of the friend to invite.
     */
    public void inviteFriendToSession(String friendPlayerAccountId) {
        http().postNoBody("/v1/social/invite/" + encodePathSegment(friendPlayerAccountId));
    }

    // ======================== SOCIAL — FRIEND REQUESTS (doc §8.16-§8.20) ========================

    /**
     * {@code GET /v1/social/requests} — friend requests the caller has RECEIVED. Scope
     * {@code social.friends.read}.
     */
    public FriendRequestsResponse getFriendRequests() {
        return FriendRequestsResponse.fromJson(http().get("/v1/social/requests"));
    }

    /**
     * {@code GET /v1/social/requests/sent} — friend requests the caller has SENT. See
     * {@link FriendRequest}'s class javadoc for a field-naming quirk specific to this endpoint's
     * response shape. Scope {@code social.friends.read}.
     */
    public FriendRequestsResponse getSentFriendRequests() {
        return FriendRequestsResponse.fromJson(http().get("/v1/social/requests/sent"));
    }

    /**
     * {@code POST /v1/social/requests} — sends a friend request to the given player. Scope
     * {@code social.friends.write}.
     *
     * @throws me.feeldev.nerionp2p.internal.P2PApiException with {@code isInvalidTarget()} true if
     *         {@code playerAccountId} is missing/malformed, or {@code isBackendError()} true for
     *         anything the backend rejects (already friends, already pending, unknown account —
     *         none of these get a dedicated code, per doc §8.17).
     */
    public void sendFriendRequest(String playerAccountId) {
        JsonBuilder body = new JsonBuilder().put("playerAccountId", playerAccountId);
        http().post("/v1/social/requests", body.build());
    }

    /**
     * {@code POST /v1/social/requests/{requestId}/accept} — accepts a request the caller
     * RECEIVED. {@code requestId} comes from {@link #getFriendRequests()}. Scope
     * {@code social.friends.write}.
     */
    public void acceptFriendRequest(String requestId) {
        http().postNoBody("/v1/social/requests/" + encodePathSegment(requestId) + "/accept");
    }

    /**
     * {@code POST /v1/social/requests/{requestId}/reject} — rejects a request the caller
     * RECEIVED. Same {@code requestId} source as {@link #acceptFriendRequest(String)}. Scope
     * {@code social.friends.write}.
     */
    public void rejectFriendRequest(String requestId) {
        http().postNoBody("/v1/social/requests/" + encodePathSegment(requestId) + "/reject");
    }

    /**
     * {@code POST /v1/social/requests/{requestId}/cancel} — cancels a request the caller SENT
     * (not one they received — see {@link #rejectFriendRequest(String)} for that). {@code requestId}
     * comes from {@link #getSentFriendRequests()}. Scope {@code social.friends.write}.
     */
    public void cancelFriendRequest(String requestId) {
        http().postNoBody("/v1/social/requests/" + encodePathSegment(requestId) + "/cancel");
    }

    // ======================== SOCIAL — FRIENDS MANAGEMENT (doc §8.21, §8.22) ========================

    /**
     * {@code POST /v1/social/friends/{friendPlayerAccountId}/remove} — removes an existing
     * friend. {@code friendPlayerAccountId} is {@link Friend#getPlayerAccountId()}. Scope
     * {@code social.friends.write}.
     */
    public void removeFriend(String friendPlayerAccountId) {
        http().postNoBody("/v1/social/friends/" + encodePathSegment(friendPlayerAccountId) + "/remove");
    }

    /**
     * {@code POST /v1/social/friends/{friendshipId}/favorite} — toggles favorite status on an
     * existing friendship (favorite &#8594; not-favorite, or the reverse — this route doesn't take
     * a target state). {@code friendshipId} is {@link Friend#getFriendshipId()} — <b>not</b> the
     * same value as {@link Friend#getPlayerAccountId()} used by {@link #removeFriend(String)},
     * don't mix the two up. Scope {@code social.friends.write}.
     */
    public void toggleFavorite(String friendshipId) {
        http().postNoBody("/v1/social/friends/" + encodePathSegment(friendshipId) + "/favorite");
    }

    // ======================== PRIVATE ========================

    /**
     * The launcher API client, or a clear failure before the credential file showed up — every call
     * used to die with a bare NullPointerException that callers reported as a generic failure.
     */
    private NerionHttp http() {
        NerionHttp current = http;
        if (current == null) {
            throw new P2PNetworkException("Not connected to the Nerion launcher yet (no launcher API credentials).");
        }
        return current;
    }

    private void onCredentials(String url, String tok) {
        this.http = new NerionHttp(url, tok);
        LOG.info("[NerionP2P] Connected to " + url);
        if (config.getOnConnected() != null) {
            config.getOnConnected().run();
        }
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String extraPortsToJson(List<ExtraPort> extraPorts) {
        List<String> fragments = new ArrayList<>(extraPorts.size());
        for (ExtraPort p : extraPorts) {
            fragments.add(p.toJson());
        }
        return JsonBuilder.arrayOfObjects(fragments);
    }

    private static final Duration HOST_READY_POLL = Duration.ofSeconds(1);

    /** An invite's token, from the token itself or the whole link ({@code https://…/join/tunnel/<token>}). */
    static String inviteToken(String inviteCodeOrUrl) {
        if (inviteCodeOrUrl == null) {
            return null;
        }
        String trimmed = inviteCodeOrUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        int query = trimmed.indexOf('?');
        if (query >= 0) {
            trimmed = trimmed.substring(0, query);
        }
        return trimmed.substring(trimmed.lastIndexOf('/') + 1);
    }
}
