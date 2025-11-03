package io.akeyless.cloudid.gcp;

import io.akeyless.cloudid.CloudIdProvider;
import io.akeyless.cloudid.http.HttpResponse;
import io.akeyless.cloudid.http.HttpTransport;
import io.akeyless.cloudid.http.JdkHttpTransport;
import io.akeyless.cloudid.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class GcpCloudIdProvider implements CloudIdProvider {
    private static final String IDENTITY_URL =
            "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity?audience=https%3A%2F%2Fapi.akeyless.io&format=full";


    private final HttpTransport http;

    public GcpCloudIdProvider() {
        this(new JdkHttpTransport());
    }

    public GcpCloudIdProvider(HttpTransport http) {
        this.http = http;
    }

    @Override
    public String getCloudId() throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Metadata-Flavor", "Google");
        HttpResponse res = http.get(IDENTITY_URL, headers, Utils.CONNECT_TIMEOUT_MS, Utils.READ_TIMEOUT_MS);
        if (res.getStatusCode() / 100 != 2) {
            throw new IOException("Failed to get GCP identity token: HTTP " + res.getStatusCode());
        }
        String token = res.getBody();
        if (token == null || token.isEmpty()) {
            throw new IOException("GCP identity token response is empty");
        }
        return token;
    }
}