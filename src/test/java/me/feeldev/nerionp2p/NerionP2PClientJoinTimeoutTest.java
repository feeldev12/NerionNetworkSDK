package me.feeldev.nerionp2p;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import me.feeldev.nerionp2p.internal.P2PNetworkException;
import me.feeldev.nerionp2p.models.JoinResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/** A join answers slower than other calls (the launcher tries P2P first): it must not time out at 15 s. */
class NerionP2PClientJoinTimeoutTest {

    private static final long SLOW_ANSWER_MILLIS = 16_000;

    private HttpServer server;
    private NerionP2PClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
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
        exchange.getRequestBody().readAllBytes();
        try {
            Thread.sleep(SLOW_ANSWER_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        byte[] body = "{\"mode\":\"P2P\",\"localBridgePort\":51234,\"publicHost\":\"relay.example.com\",\"publicPort\":25566}"
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        } catch (IOException ignored) {
            // The client already gave up (the stop() case below).
        }
    }

    @Test
    void joinWaitsLongerThanOtherCalls() {
        assertTrue(NerionP2PClient.JOIN_TIMEOUT.compareTo(Duration.ofSeconds(45)) >= 0);

        JoinResult result = client.joinByTunnelId("tunnel-42");

        assertTrue(result.isP2P());
        assertEquals(51234, result.getLocalBridgePort());
    }

    @Test
    void otherCallsKeepTheShortTimeout() {
        assertThrows(P2PNetworkException.class, () -> client.stop());
    }
}
