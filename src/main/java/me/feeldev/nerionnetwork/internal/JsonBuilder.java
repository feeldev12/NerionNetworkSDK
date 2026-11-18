package me.feeldev.nerionnetwork.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal builder for the small, flat JSON request bodies this SDK sends.
 * Not a general-purpose JSON library — just enough for this API's request shapes.
 * String fields are omitted entirely when null, matching the API's "optional fields
 * may be omitted, not just set to null" convention.
 */
public final class JsonBuilder {

    private final List<String> fields = new ArrayList<>();

    public JsonBuilder put(String key, String value) {
        if (value != null) {
            fields.add(Json.quote(key) + ":" + Json.quote(value));
        }
        return this;
    }

    public JsonBuilder put(String key, int value) {
        fields.add(Json.quote(key) + ":" + value);
        return this;
    }

    public JsonBuilder put(String key, boolean value) {
        fields.add(Json.quote(key) + ":" + value);
        return this;
    }

    /** Embeds a pre-built JSON fragment (object/array) verbatim under this key. Omitted when null. */
    public JsonBuilder putRaw(String key, String rawJson) {
        if (rawJson != null) {
            fields.add(Json.quote(key) + ":" + rawJson);
        }
        return this;
    }

    public String build() {
        return "{" + String.join(",", fields) + "}";
    }

    public static String arrayOfObjects(List<String> objectJsonFragments) {
        return "[" + String.join(",", objectJsonFragments) + "]";
    }
}
