package me.feeldev.nerionnetwork;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import me.feeldev.nerionnetwork.internal.NetworkApiException;
import me.feeldev.nerionnetwork.models.PingResponse;
import me.feeldev.nerionnetwork.models.SessionParticipant;
import me.feeldev.nerionnetwork.models.SessionParticipants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Players of the session: list, host reports, bans and invites to non-friends. */
class NerionNetworkClientSessionPlayersTest {

    private HttpServer server;
    private final List<String> requests = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private int status = 200;
    private String response = "{}";
    private NerionNetworkClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        client = new NerionNetworkClient(NerionNetworkConfig.builder()
                .credentialFile(new File("unused-credential-file.txt"))
                .build());
        client.connect("http://127.0.0.1:" + server.getAddress().getPort(), "test-token");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
        bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void getSessionParticipants_readsPlayersInAndOut() {
        response = "{\"tunnelId\":\"t-1\",\"role\":\"PLAYER\",\"participants\":["
                + "{\"playerAccountId\":\"p-1\",\"displayName\":\"Ana\",\"avatarUrl\":null,\"minecraftUuid\":\"u-1\","
                + "\"minecraftName\":\"Ana\",\"friend\":false,\"inSession\":false,"
                + "\"firstJoinedAt\":\"2026-11-18T10:00:00Z\",\"leftAt\":\"2026-11-18T10:30:00Z\","
                + "\"banned\":false,\"canInvite\":true},"
                + "{\"playerAccountId\":null,\"displayName\":null,\"minecraftUuid\":\"u-2\",\"minecraftName\":\"Steve\","
                + "\"friend\":false,\"inSession\":true,\"firstJoinedAt\":1795500000.5,\"leftAt\":null,"
                + "\"banned\":false,\"canInvite\":false}]}";

        SessionParticipants result = client.getSessionParticipants();

        assertEquals("GET /v1/p2p/participants", requests.get(0));
        assertEquals("t-1", result.getTunnelId());
        assertFalse(result.isHost());
        SessionParticipant left = result.getParticipants().get(0);
        assertEquals("p-1", left.getPlayerAccountId());
        assertFalse(left.isInSession());
        assertEquals(Instant.parse("2026-11-18T10:30:00Z"), left.getLeftAt());
        assertTrue(left.canInvite());
        SessionParticipant noAccount = result.getParticipants().get(1);
        assertFalse(noAccount.hasNerionAccount());
        assertTrue(noAccount.isInSession());
        assertNull(noAccount.getLeftAt());
        assertEquals(Instant.ofEpochSecond(1795500000L, 500_000_000L), noAccount.getFirstJoinedAt());
    }

    @Test
    void getSessionParticipants_hostRole() {
        response = "{\"tunnelId\":\"t-1\",\"role\":\"HOST\",\"participants\":[]}";

        assertTrue(client.getSessionParticipants().isHost());
    }

    @Test
    void reportPlayerJoinedAndLeft_sendTheEvent() {
        client.reportPlayerJoined("u-1", "Ana");
        client.reportPlayerLeft("u-1", "Ana");

        assertEquals("POST /v1/p2p/players-event", requests.get(0));
        assertEquals("{\"type\":\"JOINED\",\"uuid\":\"u-1\",\"name\":\"Ana\"}", bodies.get(0));
        assertEquals("{\"type\":\"LEFT\",\"uuid\":\"u-1\",\"name\":\"Ana\"}", bodies.get(1));
    }

    @Test
    void setPlayerBanned_byAccountAndByUuid() {
        client.setPlayerBanned("p-1", true);
        client.setPlayerBannedByUuid("u-2", "Steve", false);

        assertEquals("POST /v1/p2p/ban", requests.get(0));
        assertEquals("{\"playerAccountId\":\"p-1\",\"banned\":true}", bodies.get(0));
        assertEquals("{\"uuid\":\"u-2\",\"name\":\"Steve\",\"banned\":false}", bodies.get(1));
        assertThrows(IllegalArgumentException.class, () -> client.setPlayerBanned(" ", true));
    }

    @Test
    void inviteToSession_usesTheInviteRoute() {
        client.inviteToSession("p-1");

        assertEquals("POST /v1/social/invite/p-1", requests.get(0));
    }

    @Test
    void inviteToSession_rateLimitedIsReadable() {
        status = 429;
        response = "{\"error\":{\"code\":\"invite_rate_limited\",\"message\":\"slow down\"}}";

        NetworkApiException e = assertThrows(NetworkApiException.class, () -> client.inviteToSession("p-1"));

        assertTrue(e.isInviteRateLimited());
    }

    @Test
    void newErrorCodes_haveHelpers() {
        assertTrue(new NetworkApiException(403, "{\"error\":{\"code\":\"tunnel_banned\"}}").isTunnelBanned());
        assertTrue(new NetworkApiException(409, "{\"error\":{\"code\":\"player_banned\"}}").isPlayerBanned());
        assertTrue(new NetworkApiException(403, "{\"error\":{\"code\":\"not_tunnel_participant\"}}").isNotTunnelParticipant());
        assertTrue(new NetworkApiException(400, "{\"error\":{\"code\":\"invalid_invitee\"}}").isInvalidInvitee());
    }

    @Test
    void ping_announcesSessionPlayers() {
        response = "{\"apiVersion\":1,\"capabilities\":[\"p2p\",\"p2p.participants\"]}";

        PingResponse ping = client.ping();

        assertTrue(ping.supportsSessionPlayers());
        assertFalse(new PingResponse(1, new String[]{"p2p"}).supportsSessionPlayers());
    }
}
