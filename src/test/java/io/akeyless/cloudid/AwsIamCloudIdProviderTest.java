package io.akeyless.cloudid;

import com.fasterxml.jackson.jr.ob.JSON;
import io.akeyless.cloudid.aws.AwsCredentialResolver;
import io.akeyless.cloudid.aws.AwsIamCloudIdProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AwsIamCloudIdProviderTest {

    @Test
    @SuppressWarnings("unchecked")
    public void buildsBase64EncodedStsRequestBundle() throws Exception {
        AwsCredentialResolver.AwsCredentials creds = new AwsCredentialResolver.AwsCredentials(
                "AKIAEXAMPLE", "SECRETKEYEXAMPLE", "SESSIONTOKEN");
        AwsCredentialResolver resolver = new AwsCredentialResolver() {
            @Override
            public AwsCredentials resolve() {
                return creds;
            }
        };

        AwsIamCloudIdProvider provider = new AwsIamCloudIdProvider(resolver);
        String cloudId = provider.getCloudId();
        assertNotNull(cloudId);

        String json = new String(Base64.getDecoder().decode(cloudId), StandardCharsets.UTF_8);
        Map<String, Object> root = (Map<String, Object>) JSON.std.mapFrom(json);

        assertEquals("POST", root.get("sts_request_method"));
        String url = new String(Base64.getDecoder().decode((String) root.get("sts_request_url")), StandardCharsets.UTF_8);
        assertEquals("https://sts.amazonaws.com/", url);
        String body = new String(Base64.getDecoder().decode((String) root.get("sts_request_body")), StandardCharsets.UTF_8);
        assertEquals("Action=GetCallerIdentity&Version=2011-06-15", body);
        String headersJson = new String(Base64.getDecoder().decode((String) root.get("sts_request_headers")), StandardCharsets.UTF_8);
        Map<String, Object> headers = (Map<String, Object>) JSON.std.mapFrom(headersJson);
        // Basic sanity checks
        assertNotNull(headers.get("Content-Type"));
        assertNotNull(headers.get("Host"));
        assertNotNull(headers.get("X-Amz-Date"));
        assertNotNull(headers.get("X-Amz-Security-Token"));
    }
}


