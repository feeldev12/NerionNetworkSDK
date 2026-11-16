package me.feeldev.nerionp2p.internal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void readsEscapesAndUnicode() {
        JsonObject o = Json.parseObject("{\"name\":\"say \\\"hi\\\"\\n\\u00f1and\\\\\"}");

        assertEquals("say \"hi\"\nñand\\", o.getString("name"));
    }

    @Test
    void keyInsideAStringOrANestedObjectIsNotMistakenForTheField() {
        JsonObject o = Json.parseObject(
                "{\"note\":\"\\\"online\\\":true\",\"links\":{\"online\":true},\"online\":false}");

        assertFalse(o.getBoolean("online"));
    }

    @Test
    void numbersNullsAndOptionalGetters() {
        JsonObject o = Json.parseObject("{\"port\":25565,\"at\":1731772800000,\"relay\":null,\"neg\":-3}");

        assertEquals(25565, o.getInt("port"));
        assertEquals(1731772800000L, o.getOptionalLong("at"));
        assertNull(o.getOptionalInt("relay"));
        assertNull(o.getOptionalString("missing"));
        assertEquals(-3, o.getInt("neg"));
    }

    @Test
    void requiredGetterThrowsOnMissingKey() {
        JsonObject o = Json.parseObject("{}");

        assertThrows(JsonException.class, () -> o.getString("tunnelId"));
        assertThrows(JsonException.class, () -> o.getInt("port"));
    }

    @Test
    void wrongTypeThrows() {
        JsonObject o = Json.parseObject("{\"port\":\"25565\"}");

        assertThrows(JsonException.class, () -> o.getInt("port"));
    }

    @Test
    void arraysOfObjectsAndStrings() {
        JsonObject o = Json.parseObject("{\"ports\":[{\"localPort\":1,\"inner\":{\"x\":[1,2]}},{\"localPort\":2}],\"caps\":[\"p2p\",\"social\"]}");

        List<JsonObject> ports = o.getObjectList("ports");
        assertEquals(2, ports.size());
        assertEquals(2, ports.get(1).getInt("localPort"));
        assertEquals(List.of("p2p", "social"), o.getStringList("caps"));
        assertTrue(o.getObjectList("missing").isEmpty());
    }

    @Test
    void rawObjectRoundTrips() {
        JsonObject o = Json.parseObject("{\"socialLinks\": {\"discord\": \"a\\\"b\", \"n\": 1}}");

        assertEquals("{\"discord\":\"a\\\"b\",\"n\":1}", o.getRawObject("socialLinks"));
        assertEquals("{}", o.getRawObject("missing"));
    }

    @Test
    void rejectsMalformedInput() {
        assertThrows(JsonException.class, () -> Json.parseObject("{\"a\":1"));
        assertThrows(JsonException.class, () -> Json.parseObject("{\"a\":1} trailing"));
        assertThrows(JsonException.class, () -> Json.parseObject("[1,2]"));
        assertThrows(JsonException.class, () -> Json.parseObject("<html>502</html>"));
    }

    @Test
    void quoteEscapesControlCharacters() {
        assertEquals("\"a\\\"b\\\\c\\nd\\u0001\"", Json.quote("a\"b\\c\nd\u0001"));
    }

    @Test
    void builderWritesValidJson() {
        String body = new JsonBuilder().put("name", "line\n\"quoted\"").put("port", 25565).build();

        JsonObject o = Json.parseObject(body);
        assertEquals("line\n\"quoted\"", o.getString("name"));
        assertEquals(25565, o.getInt("port"));
    }

    @Test
    void apiExceptionReadsTheErrorEnvelope() {
        P2PApiException e = new P2PApiException(401,
                "{\"error\":{\"code\":\"invalid_token\",\"message\":\"missing or unrecognized bearer token\"}}");

        assertEquals(ErrorCode.INVALID_TOKEN, e.getErrorCode());
        assertEquals("missing or unrecognized bearer token", e.getApiMessage());
    }

    @Test
    void apiExceptionToleratesANonJsonBody() {
        P2PApiException e = new P2PApiException(502, "<html>Bad gateway</html>");

        assertEquals(ErrorCode.UNKNOWN, e.getErrorCode());
        assertNull(e.getApiMessage());
    }
}
