package me.feeldev.nerionnetwork.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JoinResultTest {

    @Test
    void isDirect_true_whenModeIsDirect() {
        JoinResult result = new JoinResult("DIRECT", 0, "203.0.113.5", 25565);

        assertTrue(result.isDirect());
        assertFalse(result.isP2P());
        assertFalse(result.isRelay());
    }

    @Test
    void isDirect_false_whenModeIsP2p() {
        JoinResult result = new JoinResult("P2P", 51234, "203.0.113.5", 25565);

        assertFalse(result.isDirect());
    }

    @Test
    void getConnectAddress_forDirectMode_usesPublicHostAndPort() {
        JoinResult result = new JoinResult("DIRECT", 0, "203.0.113.5", 25565);

        assertEquals("203.0.113.5:25565", result.getConnectAddress());
        assertEquals("203.0.113.5", result.getConnectHost());
        assertEquals(25565, result.getConnectPort());
    }

    @Test
    void fromJson_parsesDirectMode() {
        String json = "{\"mode\":\"DIRECT\",\"localBridgePort\":0,\"publicHost\":\"203.0.113.5\",\"publicPort\":25565}";

        JoinResult result = JoinResult.fromJson(json);

        assertTrue(result.isDirect());
        assertEquals("203.0.113.5:25565", result.getConnectAddress());
    }
}
