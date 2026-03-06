package org.openimmunizationsoftware.cdsi.auth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class AuthSessionSupport {

    public static final String SESSION_USER_ATTRIBUTE = "stepAuthenticatedUser";

    private AuthSessionSupport() {
    }

    public static SessionUser getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER_ATTRIBUTE);
        if (value instanceof SessionUser) {
            return (SessionUser) value;
        }
        return null;
    }

    public static void clearSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_USER_ATTRIBUTE);
        }
    }

    public static String getHubLoginUrl(HttpServletRequest request) {
        String requestedUrl = getCurrentUrl(request);
        String separator = SoftwareVersion.HUB_EXTERNAL_URL.contains("?") ? "&" : "?";

        return SoftwareVersion.HUB_EXTERNAL_URL + separator
                + "app_code=step"
                + "&return_to=" + encode(SoftwareVersion.STEP_EXTERNAL_URL)
                + "&state=" + encode(UUID.randomUUID().toString())
                + "&requested_url=" + encode(requestedUrl);
    }

    public static void redirectToHubLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(getHubLoginUrl(request));
    }

    public static String getCurrentUrl(HttpServletRequest request) {
        StringBuffer requestUrl = request.getRequestURL();
        String query = request.getQueryString();
        if (query == null || query.isEmpty()) {
            return requestUrl.toString();
        }
        return requestUrl.toString() + "?" + query;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

