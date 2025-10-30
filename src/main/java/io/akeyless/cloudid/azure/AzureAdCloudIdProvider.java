package io.akeyless.cloudid.azure;

import io.akeyless.cloudid.CloudIdProvider;
import io.akeyless.cloudid.http.HttpResponse;
import io.akeyless.cloudid.http.HttpTransport;
import io.akeyless.cloudid.http.JdkHttpTransport;
import io.akeyless.cloudid.util.JsonExtractors;
import io.akeyless.cloudid.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class AzureAdCloudIdProvider implements CloudIdProvider {
    private static final String TOKEN_URL =
            "http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&resource=https%3A%2F%2Fmanagement.azure.com%2F";

    private final HttpTransport http;

    public AzureAdCloudIdProvider() {
        this(new JdkHttpTransport());
    }

    public AzureAdCloudIdProvider(HttpTransport http) {
        this.http = http;
    }

    @Override
    public String getCloudId() throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Metadata", "true");
        HttpResponse res = http.get(TOKEN_URL, headers, Utils.CONNECT_TIMEOUT_MS, Utils.READ_TIMEOUT_MS);
        if (res.getStatusCode() / 100 != 2) {
            throw new IOException("Failed to get Azure MI token: HTTP " + res.getStatusCode() + ": " + res.getBody());
        }
        String token = JsonExtractors.extractStringField(res.getBody(), "access_token");
        if (token == null || token.isEmpty()) {
            throw new IOException("Azure MI token response missing access_token");
        }
        return token;
    }
}