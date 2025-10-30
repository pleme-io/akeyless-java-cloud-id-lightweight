package io.akeyless.cloudid;

import io.akeyless.cloudid.gcp.GcpCloudIdProvider;
import io.akeyless.cloudid.http.HttpResponse;
import io.akeyless.cloudid.http.HttpTransport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GcpCloudIdProviderTest {

    @Test
    public void returnsIdentityToken() throws Exception {
        HttpTransport http = Mockito.mock(HttpTransport.class);
        String url = "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity?audience=https%3A%2F%2Fapi.akeyless.io&format=full";
        Mockito.when(http.get(Mockito.eq(url), Mockito.anyMap(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(new HttpResponse(200, "jwt-token", Collections.<String, java.util.List<String>>emptyMap()));

        GcpCloudIdProvider provider = new GcpCloudIdProvider(http);
        String cloudId = provider.getCloudId();
        assertEquals("jwt-token", cloudId);
    }
}


