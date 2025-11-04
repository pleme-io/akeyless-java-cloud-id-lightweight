package io.akeyless.cloudid.aws;

import com.fasterxml.jackson.jr.ob.JSON;
import io.akeyless.cloudid.http.HttpResponse;
import io.akeyless.cloudid.http.HttpTransport;
import io.akeyless.cloudid.http.JdkHttpTransport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AwsCredentialResolver {

    public static class AwsCredentials {
        public final String accessKeyId;
        public final String secretAccessKey;
        public final String sessionToken;

        public AwsCredentials(String accessKeyId, String secretAccessKey, String sessionToken) {
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
            this.sessionToken = sessionToken;
        }
    }

    private final HttpTransport http;

    public AwsCredentialResolver() {
        this(new JdkHttpTransport());
    }

    public AwsCredentialResolver(HttpTransport http) {
        this.http = http;
    }

    public AwsCredentials resolve() throws Exception {
        // 1. Environment Variables
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String sessionToken = System.getenv("AWS_SESSION_TOKEN"); // optional

        if (accessKey != null && accessKey.length() > 0 && secretKey != null && secretKey.length() > 0) {
            return new AwsCredentials(accessKey, secretKey, sessionToken);
        }

        // 2. ECS Container Credentials (via AWS_CONTAINER_CREDENTIALS_RELATIVE_URI)
        String relativeUri = System.getenv("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI");
        if (relativeUri != null) {
            String ecsUrl = "http://169.254.170.2" + relativeUri;
            return fetchCredentialsFromMetadataService(ecsUrl);
        }

        // 3. EC2 Instance Metadata (IMDSv2)
        String token = fetchImdsV2Token();
        String roleName = httpGet("http://169.254.169.254/latest/meta-data/iam/security-credentials/", token);
        String credsJson = httpGet("http://169.254.169.254/latest/meta-data/iam/security-credentials/" + roleName, token);

        // Parse JSON using jackson-jr
        @SuppressWarnings("unchecked")
        Map<String, Object> json = (Map<String, Object>) JSON.std.mapFrom(credsJson);

        return new AwsCredentials(
                (String) json.get("AccessKeyId"), (String) json.get("SecretAccessKey"), (String) json.get("Token"));
    }

    private String fetchImdsV2Token() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-aws-ec2-metadata-token-ttl-seconds", "21600");
        HttpResponse res = http.put("http://169.254.169.254/latest/api/token", headers, "", HttpTransport.CONNECT_TIMEOUT_MS, HttpTransport.READ_TIMEOUT_MS);
        if (res.getStatusCode() != 200) {
            throw new RuntimeException("Failed to fetch IMDSv2 token");
        }
        return res.getBody();
    }

    private String httpGet(String url, String imdsToken) throws Exception {
        Map<String, String> headers = imdsToken == null ? Collections.<String, String>emptyMap() : singletonHeader("X-aws-ec2-metadata-token", imdsToken);
        HttpResponse res = http.get(url, headers, HttpTransport.CONNECT_TIMEOUT_MS, HttpTransport.READ_TIMEOUT_MS);
        if (res.getStatusCode() != 200) {
            throw new RuntimeException("Failed to fetch metadata from " + url);
        }
        return res.getBody();
    }

    private AwsCredentials fetchCredentialsFromMetadataService(String urlStr) throws Exception {
        HttpResponse res = http.get(urlStr, Collections.<String, String>emptyMap(), HttpTransport.CONNECT_TIMEOUT_MS, HttpTransport.READ_TIMEOUT_MS);
        if (res.getStatusCode() != 200) {
            throw new RuntimeException("Failed to fetch ECS credentials from " + urlStr);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> json = (Map<String, Object>) JSON.std.mapFrom(res.getBody());
        return new AwsCredentials(
                (String) json.get("AccessKeyId"), (String) json.get("SecretAccessKey"), (String) json.get("Token"));
    }

    private static Map<String, String> singletonHeader(String k, String v) {
        Map<String, String> m = new HashMap<String, String>();
        m.put(k, v);
        return m;
    }
}