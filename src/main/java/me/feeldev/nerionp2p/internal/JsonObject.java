package me.feeldev.nerionp2p.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Typed, read-only view of a parsed JSON object (see {@link Json#parseObject(String)}).
 *
 * <p>The required getters ({@link #getString}, {@link #getInt}, {@link #getBoolean}) throw
 * {@link JsonException} when the key is missing; the {@code Optional} ones return {@code null}.
 * A JSON {@code null} reads as {@code null} (or {@code false} for {@link #getBoolean}).
 */
public final class JsonObject {

    private final Map<String, Object> values;

    JsonObject(Map<String, Object> values) {
        this.values = values;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public String getString(String key) {
        Object value = require(key);
        return value == null ? null : as(key, value, String.class);
    }

    public String getOptionalString(String key) {
        Object value = values.get(key);
        return value == null ? null : as(key, value, String.class);
    }

    public int getInt(String key) {
        Object value = require(key);
        if (value == null) {
            throw new JsonException("'" + key + "' is null, expected a number");
        }
        return Math.toIntExact(as(key, value, Long.class));
    }

    public Integer getOptionalInt(String key) {
        Long value = getOptionalLong(key);
        return value == null ? null : Math.toIntExact(value);
    }

    public Long getOptionalLong(String key) {
        Object value = values.get(key);
        return value == null ? null : as(key, value, Long.class);
    }

    public boolean getBoolean(String key) {
        Object value = require(key);
        return value != null && as(key, value, Boolean.class);
    }

    /**
     * A timestamp as an ISO-8601 string ({@code "2026-11-18T10:15:30Z"}) or epoch seconds, possibly
     * fractional; {@code null} when the key is missing or null.
     */
    public java.time.Instant getOptionalInstant(String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            try {
                return java.time.Instant.parse(text);
            } catch (java.time.format.DateTimeParseException e) {
                throw new JsonException("'" + key + "' is not an ISO-8601 instant");
            }
        }
        if (value instanceof Long seconds) {
            return java.time.Instant.ofEpochSecond(seconds);
        }
        if (value instanceof Double seconds) {
            long whole = (long) Math.floor(seconds);
            return java.time.Instant.ofEpochSecond(whole, Math.round((seconds - whole) * 1_000_000_000L));
        }
        throw new JsonException("'" + key + "' is not a timestamp");
    }

    /** The objects of an array field; empty when the key is missing or null. */
    public List<JsonObject> getObjectList(String key) {
        List<JsonObject> result = new ArrayList<>();
        for (Object item : list(key)) {
            result.add(new JsonObject(asMap(key, item)));
        }
        return result;
    }

    /** The strings of an array field; empty when the key is missing or null. */
    public List<String> getStringList(String key) {
        List<String> result = new ArrayList<>();
        for (Object item : list(key)) {
            result.add(as(key, item, String.class));
        }
        return result;
    }

    /** A nested object as JSON text, {@code "{}"} when the key is missing or null. */
    public String getRawObject(String key) {
        Object value = values.get(key);
        return value == null ? "{}" : Json.write(asMap(key, value));
    }

    /** A nested object, or {@code null} when the key is missing or null. */
    public JsonObject getOptionalObject(String key) {
        Object value = values.get(key);
        return value == null ? null : new JsonObject(asMap(key, value));
    }

    private Object require(String key) {
        if (!values.containsKey(key)) {
            throw new JsonException("Missing key '" + key + "'");
        }
        return values.get(key);
    }

    private List<?> list(String key) {
        Object value = values.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list;
        }
        throw new JsonException("'" + key + "' is not an array");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(String key, Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new JsonException("'" + key + "' is not an object");
    }

    private static <T> T as(String key, Object value, Class<T> type) {
        if (!type.isInstance(value)) {
            throw new JsonException("'" + key + "' is not a " + type.getSimpleName().toLowerCase());
        }
        return type.cast(value);
    }
}
