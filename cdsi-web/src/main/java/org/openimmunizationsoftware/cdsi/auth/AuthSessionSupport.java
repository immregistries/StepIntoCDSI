package org.openimmunizationsoftware.cdsi.auth;

import java.io.IOException;

import org.immregistries.interophub.client.HubClientConfig;
import org.immregistries.interophub.client.InteropHubClient;
import org.immregistries.interophub.client.InteropHubClientFactory;
import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public final class AuthSessionSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AuthSessionSupport.class);
    private static final String APP_CODE = "step";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 8000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 12000;
    private static final InteropHubClient HUB_CLIENT = InteropHubClientFactory.create(
            new HubClientConfig(
                    SoftwareVersion.STEP_EXTERNAL_URL,
                    SoftwareVersion.HUB_EXTERNAL_URL,
                    APP_CODE,
                    DEFAULT_CONNECT_TIMEOUT_MS,
                    DEFAULT_READ_TIMEOUT_MS));

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
        String redirectUrl = HUB_CLIENT.buildLoginUrl(requestedUrl);

        LOG.info("Preparing Hub login redirect: sessionId={} requestedUrl={} redirectUrl={}",
                getSessionId(request), requestedUrl, redirectUrl);

        return redirectUrl;
    }

    public static String getHubHomeUrl() {
        return HUB_CLIENT.getHubHomeUrl();
    }

    public static String getHubAuthExchangeUrl() {
        return HUB_CLIENT.getHubAuthExchangeUrl();
    }

    public static InteropHubClient getInteropHubClient() {
        return HUB_CLIENT;
    }

    public static void redirectToHubLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String redirectUrl = getHubLoginUrl(request);
        LOG.info("Redirecting to Hub login: sessionId={} redirectUrl={}", getSessionId(request), redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    public static String getCurrentUrl(HttpServletRequest request) {
        String basePath = SoftwareVersion.STEP_EXTERNAL_URL;
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }

        String query = request.getQueryString();
        String result = basePath + requestPath;
        if (query != null && !query.isEmpty()) {
            result = result + "?" + query;
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? "none" : session.getId();
    }
}
