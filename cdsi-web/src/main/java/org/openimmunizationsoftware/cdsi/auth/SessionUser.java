package org.openimmunizationsoftware.cdsi.auth;

import java.io.Serializable;

public class SessionUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String displayName;
    private final String organization;
    private final String title;
    private final String email;

    public SessionUser(String displayName, String organization, String title, String email) {
        this.displayName = safeTrim(displayName);
        this.organization = safeTrim(organization);
        this.title = safeTrim(title);
        this.email = safeTrim(email);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getOrganization() {
        return organization;
    }

    public String getTitle() {
        return title;
    }

    public String getEmail() {
        return email;
    }

    private static String safeTrim(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
