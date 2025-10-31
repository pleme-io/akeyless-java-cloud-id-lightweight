# akeyless-java-cloud-id (lightweight)

A lightweight Java 17 library that retrieves CloudId for authenticating with Akeyless using cloud authorization providers (AWS IAM, Azure AD Managed Identity, GCP Workload Identity). It mirrors the structure of the upstream project and uses only JDK HttpURLConnection for HTTP. JSON handling uses Jackson (bundled as a dependency).

- Reference structure: [akeyless-java-cloud-id](https://github.com/akeylesslabs/akeyless-java-cloud-id/)

## Requirements

- Java 17+
- Maven or Gradle

## Installation

If using the Akeyless Artifactory, add this repository to your Maven settings or POM:

```xml
<repository>
  <id>akeyless-java</id>
  <url>https://akeyless.jfrog.io/artifactory/akeyless-java</url>
  <snapshots><enabled>false</enabled></snapshots>
</repository>
```

Then add the dependency:

```xml
<dependency>
  <groupId>io.akeyless</groupId>
  <artifactId>cloudid-lightweight</artifactId>
  <version>Specify the CloudId package version here</version>
</dependency>
```

To use the Akeyless Java SDK in tandem:

```xml
<dependency>
  <groupId>io.akeyless</groupId>
  <artifactId>akeyless-java</artifactId>
  <version><!-- Specify SDK version --></version>
</dependency>
```

## Usage

```java
import io.akeyless.cloudid.CloudIdProvider;
import io.akeyless.cloudid.CloudProviderFactory;

String accessType = "aws_iam"; // or "azure_ad" or "gcp"
CloudIdProvider provider = CloudProviderFactory.getCloudIdProvider(accessType);
String cloudId = provider.getCloudId();
```

With Akeyless SDK (example):

```java
import io.akeyless.client.ApiClient;
import io.akeyless.client.Configuration;
import io.akeyless.client.api.V2Api;
import io.akeyless.client.model.Auth;
import io.akeyless.client.model.AuthOutput;

ApiClient client = Configuration.getDefaultApiClient();
client.setBasePath("https://api.akeyless.io");
V2Api api = new V2Api(client);

Auth auth = new Auth();
auth.accessId("<Your access id>");
auth.accessType(accessType);
auth.cloudId(cloudId);
AuthOutput out = api.auth(auth);
```

## Providers

- aws_iam: Builds a SigV4-signed STS GetCallerIdentity request bundle (base64-encoded JSON containing method, URL, body and headers). Credentials are resolved from environment variables, then ECS container credentials, then EC2 IMDSv2.
- azure_ad: Retrieves Managed Identity access_token from Azure Instance Metadata Service (raw token string).
- gcp: Retrieves identity token from GCP metadata server (raw JWT string).

Notes for AWS:
- Environment variables must include both `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` (and `AWS_SESSION_TOKEN` for temporary creds). If missing, the resolver falls back to ECS/IMDS.

## Testing

The project uses JUnit 5 and Mockito; HTTP calls are abstracted via a small `HttpTransport` interface to allow mocking.

## License

Apache-2.0. See `LICENSE`.

---

References:
- [akeyless-java-cloud-id](https://github.com/akeylesslabs/akeyless-java-cloud-id/)


## Author

support@akeyless.io