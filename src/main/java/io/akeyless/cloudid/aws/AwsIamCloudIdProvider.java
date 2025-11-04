package io.akeyless.cloudid.aws;

import io.akeyless.cloudid.CloudIdProvider;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.jr.ob.JSON;
import java.net.URI;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AwsIamCloudIdProvider implements CloudIdProvider {
    private static final String SERVICE = "sts";
    private static final String REGION = "us-east-1";
    private static final String ENDPOINT = "https://sts.amazonaws.com/";

    private final AwsCredentialResolver credentialResolver;

    public AwsIamCloudIdProvider() {
        this(new AwsCredentialResolver());
    }

    public AwsIamCloudIdProvider(AwsCredentialResolver credentialResolver) {
        this.credentialResolver = credentialResolver;
    }

    @Override
    public String getCloudId() throws Exception {
        AwsCredentialResolver.AwsCredentials creds = credentialResolver.resolve();

        if (creds.accessKeyId == null || creds.secretAccessKey == null) {
            throw new IllegalStateException("Missing AWS credentials");
        }

        String body = "Action=GetCallerIdentity&Version=2011-06-15";
        byte[] bodyBytes = body.getBytes(UTF_8);

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        String host = URI.create(ENDPOINT).getHost();
        String contentSha256 = toHex(hash(bodyBytes));

        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded; charset=utf-8"));
        headers.put("Host", Collections.singletonList(host));
        headers.put("X-Amz-Date", Collections.singletonList(amzDate));
        if (creds.sessionToken != null) {
            headers.put("X-Amz-Security-Token", Collections.singletonList(creds.sessionToken));
        }

        // Step 1: Create canonical request
        StringBuilder canonicalHeaders = new StringBuilder();
        List<String> signedHeadersList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerValue = entry.getValue().get(0); // take first value from List<String>
            canonicalHeaders
                    .append(entry.getKey().toLowerCase())
                    .append(":")
                    .append(headerValue.trim())
                    .append("\n");
            signedHeadersList.add(entry.getKey().toLowerCase());
        }

        String signedHeaders = String.join(";", signedHeadersList);
        String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + contentSha256;

        // Step 2: Create string to sign
        String algorithm = "AWS4-HMAC-SHA256";
        String credentialScope = dateStamp + "/" + REGION + "/" + SERVICE + "/aws4_request";
        String stringToSign = algorithm + "\n" + amzDate
                + "\n" + credentialScope
                + "\n" + toHex(hash(canonicalRequest.getBytes(UTF_8)));

        // Step 3: Calculate the signature
        byte[] signingKey = getSignatureKey(creds.secretAccessKey, dateStamp, REGION, SERVICE);
        byte[] signature = hmacSHA256(signingKey, stringToSign);
        String signatureHex = toHex(signature);

        // Step 4: Build Authorization header
        String authorizationHeader = algorithm + " " + "Credential="
                + creds.accessKeyId + "/" + credentialScope + ", " + "SignedHeaders="
                + signedHeaders + ", " + "Signature="
                + signatureHex;

        headers.put("Authorization", Collections.singletonList(authorizationHeader));

        // Now build the final output (like Go version)
        String headersJson = JSON.std.asString(headers);

        Map<String, String> awsData = new LinkedHashMap<>();
        awsData.put("sts_request_method", "POST");
        awsData.put("sts_request_url", Base64.getEncoder().encodeToString(ENDPOINT.getBytes(UTF_8)));
        awsData.put("sts_request_body", Base64.getEncoder().encodeToString(bodyBytes));
        awsData.put("sts_request_headers", Base64.getEncoder().encodeToString(headersJson.getBytes(UTF_8)));

        String awsDataJson = JSON.std.asString(awsData);
        return Base64.getEncoder().encodeToString(awsDataJson.getBytes(UTF_8));
    }

    // --- Helper functions ---

    private static byte[] hmacSHA256(byte[] key, String data) throws Exception {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes(UTF_8));
    }

    private static byte[] getSignatureKey(String secretKey, String date, String region, String service)
            throws Exception {
        byte[] kSecret = ("AWS4" + secretKey).getBytes(UTF_8);
        byte[] kDate = hmacSHA256(kSecret, date);
        byte[] kRegion = hmacSHA256(kDate, region);
        byte[] kService = hmacSHA256(kRegion, service);
        return hmacSHA256(kService, "aws4_request");
    }

    private static byte[] hash(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(data);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}


