package me.feeldev.nerionp2p;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import me.feeldev.nerionp2p.models.Friend;
import me.feeldev.nerionp2p.models.JoinResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Uses a real embedded {@link HttpServer} (JDK-provided, zero new test dependency) instead of a
 * mocking framework — {@link NerionHttp} is a concrete class with no seam to mock, and this
 * exercises the real request/response wire contract per doc §5.3/§4.3.
 */
class NerionP2PClientJoinFriendTest {

    private static final class RecordedRequest {
        final String path;
        final String body;

        RecordedRequest(String path, String body) {
            this.path = path;
            this.body = body;
        }
    }

    private HttpServer server;
    private final List<RecordedRequest> requests = new ArrayList<>();
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private NerionP2PClient client;

    /** Response body served for the next request; set per-test before calling joinFriend. */
    private volatile String nextResponseBody = "{}";

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();

        NerionP2PConfig config = NerionP2PConfig.builder()
                .credentialFile(new File("unused-credential-file.txt"))
                .build();
        client = new NerionP2PClient(config);
        client.connect("http://127.0.0.1:" + server.getAddress().getPort(), "test-token");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new RecordedRequest(exchange.getRequestURI().getPath(), body));

        byte[] responseBytes = nextResponseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private Friend friendWithTarget(String source, String targetId) {
        return new Friend("f-1", "acc-1", "someone", "Someone", "SomeoneMc",
                null, null, true, "instance-1", "My Modpack", 1700000000L, false, "{}",
                source, targetId, "instance-1");
    }

    @Test
    void joinFriend_routesToTunnelJoin_whenSourceIsTunnel() {
        nextResponseBody = "{\"mode\":\"P2P\",\"localBridgePort\":51234,\"publicHost\":\"relay.example.com\",\"publicPort\":25566}";
        Friend friend = friendWithTarget("TUNNEL", "tunnel-42");

        JoinResult result = client.joinFriend(friend);

        assertEquals(1, requestCount.get());
        assertEquals("/v1/p2p/join", requests.get(0).path);
        assertTrue(requests.get(0).body.contains("\"tunnelId\":\"tunnel-42\""));
        assertTrue(result.isP2P());
        assertEquals(51234, result.getLocalBridgePort());
    }

    @Test
    void joinFriend_routesToSocialJoin_whenSourceIsRoom() {
        nextResponseBody = "{\"mode\":\"DIRECT\",\"localBridgePort\":0,\"publicHost\":\"203.0.113.5\",\"publicPort\":25565}";
        Friend friend = friendWithTarget("ROOM", "room-7");

        JoinResult result = client.joinFriend(friend);

        assertEquals(1, requestCount.get());
        assertEquals("/v1/social/join", requests.get(0).path);
        assertTrue(requests.get(0).body.contains("\"source\":\"ROOM\""));
        assertTrue(requests.get(0).body.contains("\"targetId\":\"room-7\""));
        assertTrue(result.isDirect());
        assertEquals("203.0.113.5:25565", result.getConnectAddress());
    }

    @Test
    void joinFriend_throwsAndMakesNoRequest_whenFriendHasNoJoinTarget() {
        Friend friend = new Friend("f-1", "acc-1", "someone", "Someone", "SomeoneMc",
                null, null, true, "instance-1", "My Modpack", 1700000000L, false, "{}");

        assertThrows(IllegalArgumentException.class, () -> client.joinFriend(friend));
        assertEquals(0, requestCount.get());
    }
}
