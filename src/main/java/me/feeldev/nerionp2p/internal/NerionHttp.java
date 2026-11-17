package me.feeldev.nerionp2p.internal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class NerionHttp {

    private static final Duration DEFAULT_POST_TIMEOUT = Duration.ofSeconds(15);

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
        return post(path, jsonBody, DEFAULT_POST_TIMEOUT);
    }

    public String post(String path, String jsonBody, Duration timeout) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(timeout)
                .build();
        return send(req);
    }

    public String postNoBody(String path) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(DEFAULT_POST_TIMEOUT)
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
}
