package io.akeyless.cloudid;

import io.akeyless.cloudid.azure.AzureAdCloudIdProvider;
import io.akeyless.cloudid.http.HttpResponse;
import io.akeyless.cloudid.http.HttpTransport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AzureAdCloudIdProviderTest {

    @Test
    public void returnsAccessToken() throws Exception {
        HttpTransport http = Mockito.mock(HttpTransport.class);
        String url = "http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&resource=https%3A%2F%2Fmanagement.azure.com%2F";
        String body = "{\"access_token\":\"abc123\",\"token_type\":\"Bearer\"}";
        Mockito.when(http.get(Mockito.eq(url), Mockito.anyMap(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(new HttpResponse(200, body, Collections.<String, java.util.List<String>>emptyMap()));

        AzureAdCloudIdProvider provider = new AzureAdCloudIdProvider(http);
        String cloudId = provider.getCloudId();
        assertEquals("abc123", cloudId);
    }
}


