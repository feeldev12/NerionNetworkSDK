package me.feeldev.nerionnetwork.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small, strict JSON reader and writer (RFC 8259) for the launcher's mod API.
 *
 * <p>The SDK has no dependencies on purpose: it runs inside mods and plugins that bring their own
 * Gson or Jackson, and a bundled copy of either could clash with theirs.
 *
 * <p>Parsed values map to Java as: object → {@code Map<String, Object>}, array → {@code List<Object>},
 * string → {@link String}, integer → {@link Long}, other numbers → {@link Double}, {@code true}/{@code false}
 * → {@link Boolean}, {@code null} → {@code null}.
 */
public final class Json {

    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    /** Parses a JSON document whose top level must be an object. */
    public static JsonObject parseObject(String text) {
        if (text == null) {
            throw new JsonException("Expected a JSON object, got nothing");
        }
        Json reader = new Json(text);
        reader.skipWhitespace();
        Map<String, Object> object = reader.readObject();
        reader.skipWhitespace();
        if (reader.pos != text.length()) {
            throw reader.error("Unexpected trailing content");
        }
        return new JsonObject(object);
    }

    /** {@code value} as a JSON string literal, quotes included. */
    public static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    /** Writes a parsed value (see the class comment for the types) back as compact JSON. */
    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String s) {
            out.append(quote(s));
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(quote(String.valueOf(entry.getKey()))).append(':');
                write(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) out.append(',');
                write(list.get(i), out);
            }
            out.append(']');
        } else {
            out.append(value);
        }
    }

    // ── reader ──────────────────────────────────────────────────────────────────

    private Object readValue() {
        skipWhitespace();
        if (pos >= text.length()) {
            throw error("Unexpected end of input");
        }
        char c = text.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    yield readNumber();
                }
                throw error("Unexpected character '" + c + "'");
            }
        };
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> object = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return Collections.unmodifiableMap(object);
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("Expected a string key");
            }
            String key = readString();
            skipWhitespace();
            expect(':');
            object.put(key, readValue());
            skipWhitespace();
            char next = next();
            if (next == '}') {
                return Collections.unmodifiableMap(object);
            }
            if (next != ',') {
                throw error("Expected ',' or '}' in object");
            }
        }
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> array = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return Collections.unmodifiableList(array);
        }
        while (true) {
            array.add(readValue());
            skipWhitespace();
            char next = next();
            if (next == ']') {
                return Collections.unmodifiableList(array);
            }
            if (next != ',') {
                throw error("Expected ',' or ']' in array");
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') {
                return out.toString();
            }
            if (c < 0x20) {
                throw error("Unescaped control character in string");
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char escaped = next();
            switch (escaped) {
                case '"', '\\', '/' -> out.append(escaped);
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> out.append(readUnicodeEscape());
                default -> throw error("Invalid escape '\\" + escaped + "'");
            }
        }
    }

    private char readUnicodeEscape() {
        if (pos + 4 > text.length()) {
            throw error("Truncated \\u escape");
        }
        try {
            char c = (char) Integer.parseInt(text.substring(pos, pos + 4), 16);
            pos += 4;
            return c;
        } catch (NumberFormatException e) {
            throw error("Invalid \\u escape");
        }
    }

    private Object readNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < text.length() && isNumberChar(text.charAt(pos))) {
            pos++;
        }
        String number = text.substring(start, pos);
        try {
            boolean integral = number.indexOf('.') < 0 && number.indexOf('e') < 0 && number.indexOf('E') < 0;
            return integral ? (Object) Long.parseLong(number) : (Object) Double.parseDouble(number);
        } catch (NumberFormatException e) {
            throw error("Invalid number '" + number + "'");
        }
    }

    private static boolean isNumberChar(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
    }

    private Object readLiteral(String literal, Object value) {
        if (!text.startsWith(literal, pos)) {
            throw error("Invalid literal");
        }
        pos += literal.length();
        return value;
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                return;
            }
            pos++;
        }
    }

    private char peek() {
        if (pos >= text.length()) {
            throw error("Unexpected end of input");
        }
        return text.charAt(pos);
    }

    private char next() {
        char c = peek();
        pos++;
        return c;
    }

    private void expect(char expected) {
        if (next() != expected) {
            pos--;
            throw error("Expected '" + expected + "'");
        }
    }

    private JsonException error(String message) {
        return new JsonException(message + " at position " + pos);
    }
}
