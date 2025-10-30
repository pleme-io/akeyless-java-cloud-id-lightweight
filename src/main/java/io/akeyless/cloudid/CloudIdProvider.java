package io.akeyless.cloudid;

import java.io.IOException;

/**
 * Provides a CloudId string for the runtime cloud environment.
 */
public interface CloudIdProvider {
    /**
     * Retrieve the CloudId string for the current runtime environment.
     *
     * @return cloud id string
     * @throws IOException if a network or parsing error occurs
     * @throws Exception 
     */
    String getCloudId() throws IOException, Exception;
}


