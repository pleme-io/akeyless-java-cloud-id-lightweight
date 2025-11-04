package io.akeyless.cloudid.http;

import java.io.IOException;
import java.util.Map;

public interface HttpTransport {
    public static final int CONNECT_TIMEOUT_MS = 2000;
    public static final int READ_TIMEOUT_MS = 3000;

    HttpResponse get(String url, Map<String, String> headers, int connectTimeoutMs, int readTimeoutMs) throws IOException;
    HttpResponse put(String url, Map<String, String> headers, String body, int connectTimeoutMs, int readTimeoutMs) throws IOException;
}


