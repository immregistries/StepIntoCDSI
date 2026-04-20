package org.immregistries.interophub.client;

public interface InteropHubClient {

    String buildLoginUrl(String requestedUrl);

    String getHubHomeUrl();

    String getHubAuthExchangeUrl();

    HubExchangeResult exchangeCode(String code, String userIp);

    HubRedirectDecision determineRedirectDecision(String requestedUrlFromHub, String fallbackTarget);
}
