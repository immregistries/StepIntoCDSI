package org.immregistries.interophub.client;

public class HubRedirectDecision {

    private final String target;
    private final String fallbackReason;

    public HubRedirectDecision(String target, String fallbackReason) {
        this.target = target == null ? "" : target;
        this.fallbackReason = fallbackReason == null ? "" : fallbackReason;
    }

    public String getTarget() {
        return target;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }
}
