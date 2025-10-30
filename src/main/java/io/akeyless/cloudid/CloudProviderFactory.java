package io.akeyless.cloudid;

import io.akeyless.cloudid.aws.AwsIamCloudIdProvider;
import io.akeyless.cloudid.azure.AzureAdCloudIdProvider;
import io.akeyless.cloudid.gcp.GcpCloudIdProvider;

/**
 * Factory for creating {@link CloudIdProvider} for a given access type.
 */
public final class CloudProviderFactory {
    private CloudProviderFactory() {
    }

    /**
     * Create a CloudIdProvider for the given access type.
     * Supported values: "aws_iam", "azure_ad", "gcp".
     *
     * @param accessType access type string
     * @return provider instance
     */
    public static CloudIdProvider getCloudIdProvider(String accessType) {
        if (accessType == null) {
            throw new IllegalArgumentException("accessType must not be null");
        }
        String normalized = accessType.trim().toLowerCase();
        switch (normalized) {
            case "aws_iam":
                return new AwsIamCloudIdProvider();
            case "azure_ad":
                return new AzureAdCloudIdProvider();
            case "gcp":
                return new GcpCloudIdProvider();
            default:
                throw new IllegalArgumentException("Unsupported accessType: " + accessType);
        }
    }
    public static void main(String[] args) {
        CloudIdProvider provider = getCloudIdProvider("aws_iam");
        try {
            String cloudId = provider.getCloudId();
            System.out.println("Cloud ID: " + cloudId);
        } catch (Exception e) {
            System.err.println("Error retrieving Cloud ID: " + e.getMessage());
        }
        provider = getCloudIdProvider("azure_ad");
        try {
            String cloudId = provider.getCloudId();
            System.out.println("Cloud ID: " + cloudId);
        } catch (Exception e) {
            System.err.println("Error retrieving Cloud ID: " + e.getMessage());
        }
        provider = getCloudIdProvider("gcp");
        try {
            String cloudId = provider.getCloudId();
            System.out.println("Cloud ID: " + cloudId);
        } catch (Exception e) {
            System.err.println("Error retrieving Cloud ID: " + e.getMessage());
        }
    }
}
