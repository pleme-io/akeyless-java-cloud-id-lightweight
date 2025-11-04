package io.akeyless.cloudid.http;

import java.io.IOException;
import java.util.Map;

public interface HttpTransport {
    HttpResponse get(String url, Map<String, String> headers, int connectTimeoutMs, int readTimeoutMs) throws IOException;

    HttpResponse put(String url, Map<String, String> headers, String body, int connectTimeoutMs, int readTimeoutMs) throws IOException;
}


