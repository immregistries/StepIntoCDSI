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
            SessionUser sessionUser = (SessionUser) value;
            if (isBlank(sessionUser.getDisplayName()) || isBlank(sessionUser.getEmail())) {
                session.removeAttribute(SESSION_USER_ATTRIBUTE);
                return null;
            }
            return sessionUser;
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
        String hubHomeUrl = getHubHomeUrl();
        String separator = hubHomeUrl.contains("?") ? "&" : "?";
        String returnTo = buildLoginReturnUrl();

        return hubHomeUrl + separator
                + "app_code=step"
                + "&return_to=" + encode(returnTo)
                + "&state=" + encode(UUID.randomUUID().toString())
                + "&requested_url=" + encode(requestedUrl);
    }

    public static String getHubHomeUrl() {
        return appendPathToHubBase("home");
    }

    public static String getHubAuthExchangeUrl() {
        return appendPathToHubBase("api/auth/exchange");
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

    private static String buildLoginReturnUrl() {
        String base = SoftwareVersion.STEP_EXTERNAL_URL;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/login";
    }

    private static String appendPathToHubBase(String path) {
        String base = SoftwareVersion.HUB_EXTERNAL_URL;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String p = path == null ? "" : path;
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.isEmpty()) {
            return base;
        }
        return base + "/" + p;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
