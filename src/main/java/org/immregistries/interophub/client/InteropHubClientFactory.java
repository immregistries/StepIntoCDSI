package org.immregistries.interophub.client;

public final class InteropHubClientFactory {

    private InteropHubClientFactory() {
    }

    public static InteropHubClient create(HubClientConfig config) {
        return new HttpInteropHubClient(config);
    }
}
