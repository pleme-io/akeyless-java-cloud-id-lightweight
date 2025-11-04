package io.akeyless.cloudid.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class JdkHttpTransport implements HttpTransport {
    @Override
    public HttpResponse get(String url, Map<String, String> headers, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        return execute("GET", url, headers, null, connectTimeoutMs, readTimeoutMs);
    }

    @Override
    public HttpResponse put(String url, Map<String, String> headers, String body, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        return execute("PUT", url, headers, body, connectTimeoutMs, readTimeoutMs);
    }

    private HttpResponse execute(String method, String urlString, Map<String, String> headers, String body,
                                 int connectTimeoutMs, int readTimeoutMs) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .timeout(Duration.ofMillis(readTimeoutMs));

        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                builder.header(e.getKey(), e.getValue());
            }
        }

        if ("PUT".equalsIgnoreCase(method)) {
            builder.method("PUT", java.net.http.HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }

        try {
            java.net.http.HttpResponse<String> response = client.send(builder.build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            Map<String, List<String>> responseHeaders = response.headers().map();
            String responseBody = response.body();
            return new HttpResponse(status, responseBody, responseHeaders);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}