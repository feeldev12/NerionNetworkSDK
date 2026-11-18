package me.feeldev.nerionnetwork;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import me.feeldev.nerionnetwork.internal.NetworkApiException;
import me.feeldev.nerionnetwork.models.JoinResult;
import me.feeldev.nerionnetwork.models.Visibility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rooms with players who aren't friends: UNLISTED sessions, invite links, waiting for the host. */
class NerionNetworkClientRoomsTest {

    private static final String NOT_READY =
            "{\"error\":{\"code\":\"host_not_ready\",\"message\":\"not yet\"}}";
    private static final String JOINED =
            "{\"mode\":\"RELAY\",\"localBridgePort\":0,\"publicHost\":\"relay.example.com\",\"publicPort\":25566}";

    private HttpServer server;
    private final List<String> bodies = new ArrayList<>();
    /** Status and body for each next request; the last one repeats. */
    private final Deque<Object[]> responses = new ArrayDeque<>();
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
        bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        Object[] response = responses.size() > 1 ? responses.poll() : responses.peek();
        byte[] bytes = ((String) response[1]).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders((Integer) response[0], bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void createSession_sendsUnlisted() {
        responses.add(new Object[]{200,
                "{\"tunnelId\":\"t-1\",\"publicHost\":\"relay.example.com\",\"publicPort\":25566}"});

        client.createSession(Visibility.UNLISTED);

        assertTrue(bodies.get(0).contains("\"visibility\":\"UNLISTED\""));
    }

    @Test
    void joinByInviteCode_acceptsTheWholeLink() {
        responses.add(new Object[]{200, JOINED});

        client.joinByInviteCode("https://nerionmc.com/join/tunnel/abc123/");

        assertTrue(bodies.get(0).contains("\"inviteCode\":\"abc123\""));
    }

    @Test
    void inviteToken_keepsAPlainToken() {
        assertEquals("abc123", NerionNetworkClient.inviteToken(" abc123 "));
        assertEquals("abc123", NerionNetworkClient.inviteToken("https://nerionmc.com/join/tunnel/abc123?ref=x"));
    }

    @Test
    void joinByTunnelIdWhenReady_retriesUntilTheHostOpens() {
        responses.add(new Object[]{409, NOT_READY});
        responses.add(new Object[]{409, NOT_READY});
        responses.add(new Object[]{200, JOINED});

        JoinResult result = client.joinByTunnelIdWhenReady("t-1", Duration.ofSeconds(10));

        assertEquals(3, bodies.size());
        assertEquals("relay.example.com", result.getConnectHost());
    }

    @Test
    void joinByTunnelIdWhenReady_givesUpAfterTheTimeout() {
        responses.add(new Object[]{409, NOT_READY});

        NetworkApiException e = assertThrows(NetworkApiException.class,
                () -> client.joinByTunnelIdWhenReady("t-1", Duration.ofMillis(500)));

        assertTrue(e.isHostNotReady());
    }

    @Test
    void joinByTunnelIdWhenReady_doesNotRetryOtherErrors() {
        responses.add(new Object[]{403,
                "{\"error\":{\"code\":\"join_not_allowed\",\"message\":\"friends only\"}}"});

        NetworkApiException e = assertThrows(NetworkApiException.class,
                () -> client.joinByTunnelIdWhenReady("t-1", Duration.ofSeconds(10)));

        assertTrue(e.isJoinNotAllowed());
        assertEquals(1, bodies.size());
    }
}
