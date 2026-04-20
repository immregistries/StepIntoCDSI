package org.immregistries.interophub.client;

public class HubClientConfig {

    private final String stepExternalUrl;
    private final String hubExternalUrl;
    private final String appCode;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public HubClientConfig(String stepExternalUrl, String hubExternalUrl, String appCode,
            int connectTimeoutMs, int readTimeoutMs) {
        this.stepExternalUrl = trimOrEmpty(stepExternalUrl);
        this.hubExternalUrl = trimOrEmpty(hubExternalUrl);
        this.appCode = trimOrEmpty(appCode);
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getStepExternalUrl() {
        return stepExternalUrl;
    }

    public String getHubExternalUrl() {
        return hubExternalUrl;
    }

    public String getAppCode() {
        return appCode;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    private static String trimOrEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
