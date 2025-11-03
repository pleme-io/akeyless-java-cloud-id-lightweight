package io.akeyless.cloudid;

import io.akeyless.cloudid.gcp.GcpCloudIdProvider;
import io.akeyless.cloudid.http.HttpResponse;
import io.akeyless.cloudid.http.HttpTransport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GcpCloudIdProviderTest {

    @Test
    public void returnsIdentityToken() throws Exception {
        HttpTransport http = Mockito.mock(HttpTransport.class);
        Mockito.when(http.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(new HttpResponse(200, "jwt-token", Collections.<String, java.util.List<String>>emptyMap()));

        GcpCloudIdProvider provider = new GcpCloudIdProvider(http);
        String cloudId = provider.getCloudId();
        String expected = Base64.getEncoder().encodeToString("jwt-token".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, cloudId);
    }
}


