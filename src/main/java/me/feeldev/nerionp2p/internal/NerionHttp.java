package me.feeldev.nerionp2p.internal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class NerionHttp {

    private final HttpClient client;
    private final String baseUrl;
    private final String token;

    public NerionHttp(String baseUrl, String token) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.token = token;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                // The launcher's local mod-api server (tiny_http, Rust) only speaks HTTP/1.1 — the
                // JDK client's default h2c upgrade attempt against it fails intermittently instead of
                // cleanly falling back, since tiny_http never replies to the Upgrade handshake at all.
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public String get(String path) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        return send(req);
    }

    public String post(String path, String jsonBody) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(15))
                .build();
        return send(req);
    }

    public String postNoBody(String path) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(15))
                .build();
        return send(req);
    }

    private String send(HttpRequest req) {
        try {
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new P2PApiException(res.statusCode(), res.body());
            }
            return res.body();
        } catch (P2PApiException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new P2PNetworkException("HTTP request interrupted: " + req.uri(), e);
        } catch (Exception e) {
            throw new P2PNetworkException("HTTP request failed: " + req.uri(), e);
        }
    }

    public static String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) throw new RuntimeException("Key '" + key + "' not found in: " + json);
        int colon = json.indexOf(':', keyIdx + search.length());
        int startQuote = json.indexOf('"', colon + 1);
        int endQuote = json.indexOf('"', startQuote + 1);
        return json.substring(startQuote + 1, endQuote);
    }

    public static int extractInt(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) throw new RuntimeException("Key '" + key + "' not found in: " + json);
        int colon = json.indexOf(':', keyIdx + search.length());
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        return Integer.parseInt(json.substring(start, end));
    }

    public static String extractNullableString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colon = json.indexOf(':', keyIdx + search.length());
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (json.charAt(start) == 'n') return null;
        int startQuote = json.indexOf('"', start);
        int endQuote = json.indexOf('"', startQuote + 1);
        return json.substring(startQuote + 1, endQuote);
    }

    /** Like {@link #extractInt}, but returns null instead of throwing when the value is a JSON {@code null}. */
    public static Integer extractNullableInt(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colon = json.indexOf(':', keyIdx + search.length());
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (json.startsWith("null", start)) return null;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (end == start) return null;
        return Integer.parseInt(json.substring(start, end));
    }

    public static boolean extractBoolean(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) throw new RuntimeException("Key '" + key + "' not found in: " + json);
        int colon = json.indexOf(':', keyIdx + search.length());
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        return json.startsWith("true", start);
    }

    /** Like {@link #extractNullableInt}, but for {@code long} values (e.g. Unix timestamps past the int32 range). */
    public static Long extractNullableLong(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colon = json.indexOf(':', keyIdx + search.length());
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (json.startsWith("null", start)) return null;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (end == start) return null;
        return Long.parseLong(json.substring(start, end));
    }

    /**
     * Extracts the raw JSON text of a nested object field, e.g. {@code "socialLinks": {"discord": "..."}}.
     * Returns {@code "{}"} if the key is missing. This SDK doesn't attempt to give {@code socialLinks} a
     * fixed shape (the API doesn't document one), so it's exposed as raw JSON text for callers who need it.
     */
    public static String extractRawObject(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return "{}";
        int colon = json.indexOf(':', keyIdx + search.length());
        int braceStart = json.indexOf('{', colon);
        int depth = 0;
        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(braceStart, i + 1);
                }
            }
        }
        throw new RuntimeException("Unbalanced '{' for key '" + key + "' in: " + json);
    }

    /**
     * Extracts the raw JSON text of each object in a flat array field, e.g. {@code "ports": [{...}, {...}]}.
     * Returns an empty list if the key is missing or the array is empty. Objects are assumed to be
     * flat (no nested arrays/objects inside them), which holds for every array this API returns.
     */
    public static java.util.List<String> extractObjectArray(String json, String key) {
        java.util.List<String> result = new java.util.ArrayList<>();
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return result;
        int colon = json.indexOf(':', keyIdx + search.length());
        int bracketStart = json.indexOf('[', colon);
        int depth = 0;
        int bracketEnd = -1;
        for (int i = bracketStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    bracketEnd = i;
                    break;
                }
            }
        }
        if (bracketEnd < 0) throw new RuntimeException("Unbalanced '[' for key '" + key + "' in: " + json);

        String inner = json.substring(bracketStart + 1, bracketEnd).trim();
        int objDepth = 0;
        int objStart = -1;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '{') {
                if (objDepth == 0) objStart = i;
                objDepth++;
            } else if (c == '}') {
                objDepth--;
                if (objDepth == 0) {
                    result.add(inner.substring(objStart, i + 1));
                }
            }
        }
        return result;
    }

    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
