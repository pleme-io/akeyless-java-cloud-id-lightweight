package io.akeyless.cloudid.aws;

import com.fasterxml.jackson.jr.ob.JSON;
import io.akeyless.cloudid.http.HttpResponse;
import io.akeyless.cloudid.http.HttpTransport;
import io.akeyless.cloudid.http.JdkHttpTransport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    private final String overrideProfileName;
    private final String overrideCredentialsPath;
    private final String overrideConfigPath;
    private final boolean ignoreEnv;

    public AwsCredentialResolver() {
        this(new JdkHttpTransport());
    }

    public AwsCredentialResolver(HttpTransport http) {
        this(http, null, null, null, false);
    }

    public AwsCredentialResolver(HttpTransport http, String overrideProfileName, String overrideCredentialsPath,
                                 String overrideConfigPath, boolean ignoreEnv) {
        this.http = http;
        this.overrideProfileName = overrideProfileName;
        this.overrideCredentialsPath = overrideCredentialsPath;
        this.overrideConfigPath = overrideConfigPath;
        this.ignoreEnv = ignoreEnv;
    }

    public AwsCredentials resolve() throws Exception {
        // 1. Environment Variables (skip if ignoreEnv is true)
        if (!ignoreEnv) {
            String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
            String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
            String sessionToken = System.getenv("AWS_SESSION_TOKEN"); // optional
            if (accessKey != null && accessKey.length() > 0 && secretKey != null && secretKey.length() > 0) {
                return new AwsCredentials(accessKey, secretKey, sessionToken);
            }
        }

        // 1b. Shared credentials/config profile (~/.aws/credentials or ~/.aws/config)
        AwsCredentials profileCreds = loadFromAwsProfile();
        if (profileCreds != null) {
            return profileCreds;
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

    private AwsCredentials loadFromAwsProfile() {
        String profile = (overrideProfileName != null && !overrideProfileName.isEmpty())
                ? overrideProfileName
                : firstNonEmpty(System.getenv("AWS_PROFILE"), System.getenv("AWS_DEFAULT_PROFILE"), "default");
        // Try shared credentials file first
        String credPath = (overrideCredentialsPath != null && !overrideCredentialsPath.isEmpty())
                ? overrideCredentialsPath
                : System.getenv("AWS_SHARED_CREDENTIALS_FILE");
        if (credPath == null || credPath.isEmpty()) {
            String home = System.getProperty("user.home");
            if (home != null && !home.isEmpty()) {
                credPath = home + File.separator + ".aws" + File.separator + "credentials";
            }
        }
        AwsCredentials creds = readProfileFromIni(credPath, profile, /*sectionPrefix*/ null);
        if (creds != null) {
            return creds;
        }
        // Fallback to config file format with [profile <name>] sections
        String cfgPath = (overrideConfigPath != null && !overrideConfigPath.isEmpty())
                ? overrideConfigPath
                : System.getenv("AWS_CONFIG_FILE");
        if (cfgPath == null || cfgPath.isEmpty()) {
            String home = System.getProperty("user.home");
            if (home != null && !home.isEmpty()) {
                cfgPath = home + File.separator + ".aws" + File.separator + "config";
            }
        }
        return readProfileFromIni(cfgPath, profile, /*sectionPrefix*/ "profile ");
    }

    private static AwsCredentials readProfileFromIni(String path, String profile, String sectionPrefix) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        File f = new File(path);
        if (!f.exists() || !f.isFile()) {
            return null;
        }
        String targetSection = profile == null || profile.isEmpty() ? "default" : profile;
        if (sectionPrefix != null) {
            targetSection = sectionPrefix + targetSection;
        }

        String currentSection = null;
        String accessKey = null;
        String secretKey = null;
        String sessionToken = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String raw;
            while ((raw = br.readLine()) != null) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1).trim();
                    continue;
                }
                if (!targetSection.equals(currentSection)) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if ("aws_access_key_id".equals(key)) {
                    accessKey = value;
                } else if ("aws_secret_access_key".equals(key)) {
                    secretKey = value;
                } else if ("aws_session_token".equals(key) || "aws_security_token".equals(key)) {
                    sessionToken = value;
                }
            }
        } catch (Exception ignore) {
            return null;
        }

        if (accessKey != null && accessKey.length() > 0 && secretKey != null && secretKey.length() > 0) {
            return new AwsCredentials(accessKey, secretKey, sessionToken);
        }
        return null;
    }

    private static String firstNonEmpty(String a, String b, String c) {
        if (a != null && !a.isEmpty()) return a;
        if (b != null && !b.isEmpty()) return b;
        return c;
    }
}