package org.immregistries.interophub.client;

public class HubUserInfo {

    private final String hubUserId;
    private final String email;
    private final String name;
    private final String organization;
    private final String title;
    private final String issuedAt;
    private final String expiresInSeconds;

    public HubUserInfo(String hubUserId, String email, String name, String organization,
            String title, String issuedAt, String expiresInSeconds) {
        this.hubUserId = trimOrEmpty(hubUserId);
        this.email = trimOrEmpty(email);
        this.name = trimOrEmpty(name);
        this.organization = trimOrEmpty(organization);
        this.title = trimOrEmpty(title);
        this.issuedAt = trimOrEmpty(issuedAt);
        this.expiresInSeconds = trimOrEmpty(expiresInSeconds);
    }

    public String getHubUserId() {
        return hubUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getOrganization() {
        return organization;
    }

    public String getTitle() {
        return title;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public String getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public boolean hasRequiredUserInfo() {
        return !isBlank(name) && !isBlank(organization) && !isBlank(title) && !isBlank(email);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimOrEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
