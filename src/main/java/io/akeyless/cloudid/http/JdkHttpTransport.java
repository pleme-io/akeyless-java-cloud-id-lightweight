package io.akeyless.cloudid.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setInstanceFollowRedirects(false);

        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
        }

        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }
        }

        int status = conn.getResponseCode();
        InputStream is = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        String responseBody = readFully(is);
        Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>(conn.getHeaderFields());
        conn.disconnect();
        return new HttpResponse(status, responseBody, responseHeaders);
    }

    private static String readFully(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}


