package org.openimmunizationsoftware.cdsi.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.openimmunizationsoftware.cdsi.auth.AuthPageRenderer;
import org.openimmunizationsoftware.cdsi.auth.AuthSessionSupport;
import org.openimmunizationsoftware.cdsi.auth.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String APP_CODE = "step";
    private static final Logger LOG = LoggerFactory.getLogger(LoginServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String code = req.getParameter("code");
        String state = req.getParameter("state");
        String requestedUrl = req.getParameter("requested_url");
        String returnTo = req.getParameter("return_to");
        String codePreview = code == null ? "none" : abbreviateForLog(code, 16);

        LOG.info(
                "Login callback received: uri={} query={} codePresent={} codePreview={} state={} requested_url={} return_to={}",
                req.getRequestURI(),
                req.getQueryString(),
                code != null && !code.trim().isEmpty(),
                codePreview,
                state,
                requestedUrl,
                returnTo);

        ExchangeResult exchangeResult = null;
        if (code != null && !code.trim().isEmpty()) {
            exchangeResult = exchangeCodeWithHub(req, code.trim());
        }

        if (exchangeResult != null && exchangeResult.success && exchangeResult.hasRequiredUserInfo()) {
            SessionUser sessionUser = new SessionUser(
                    exchangeResult.name,
                    exchangeResult.organization,
                    exchangeResult.title,
                    exchangeResult.email);
            req.getSession(true).setAttribute(AuthSessionSupport.SESSION_USER_ATTRIBUTE, sessionUser);
            RedirectDecision redirectDecision = determineRedirectDecision(req, exchangeResult.requestedUrlFromHub);
            LOG.info(
                    "Hub exchange succeeded: hubUserId={} email={} loginReady=true requested_url_from_hub={} chosen_redirect_target={} fallback_reason={}",
                    exchangeResult.hubUserId,
                    exchangeResult.email,
                    exchangeResult.requestedUrlFromHub,
                    redirectDecision.target,
                    redirectDecision.fallbackReason);
            resp.sendRedirect(redirectDecision.target);
            return;
        }

        if (exchangeResult == null) {
            LOG.warn("Login callback missing code parameter; no exchange attempted.");
        } else {
            LOG.warn(
                    "Hub exchange failed: httpStatus={} error={} loginReady={} responseLength={} requested_url_from_hub={} responsePreview={}",
                    exchangeResult.httpStatus,
                    exchangeResult.errorMessage,
                    exchangeResult.hasRequiredUserInfo(),
                    exchangeResult.responseBody == null ? 0 : exchangeResult.responseBody.length(),
                    exchangeResult.requestedUrlFromHub,
                    abbreviateForLog(exchangeResult.responseBody, 1200));
        }

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <title>Step Login Callback</title>");
        out.println("  <style>");
        out.println("    body { font-family: Arial, sans-serif; margin: 24px; }");
        out.println("    h1 { margin-bottom: 10px; }");
        out.println("    h2 { margin-top: 24px; }");
        out.println("    table { border-collapse: collapse; min-width: 500px; }");
        out.println("    th, td { border: 1px solid #d6d6d6; padding: 10px; text-align: left; vertical-align: top; }");
        out.println("    th { width: 120px; background: #f4f4f4; }");
        out.println("    .note { margin-top: 14px; color: #555; }");
        out.println("    .ok { color: #0b6b2c; font-weight: bold; }");
        out.println("    .fail { color: #9a1f1f; font-weight: bold; }");
        out.println("    pre { background: #f8f8f8; border: 1px solid #ddd; padding: 10px; overflow: auto; } ");
        out.println("  </style>");
        out.println("</head>");
        out.println("<body>");

        AuthPageRenderer.renderSignedInHeader(out, req);

        out.println("  <h1>Login Callback Received</h1>");
        out.println("  <p>This page is intentionally unprotected and now tests Hub code exchange.</p>");

        out.println("  <table>");
        out.println("    <tr><th>code</th><td>" + AuthPageRenderer.escapeHtml(code) + "</td></tr>");
        out.println("    <tr><th>state</th><td>" + AuthPageRenderer.escapeHtml(state) + "</td></tr>");
        out.println("  </table>");

        out.println("  <h2>Hub Exchange API Call</h2>");
        out.println("  <table>");
        out.println(
                "    <tr><th>endpoint</th><td>" + AuthPageRenderer.escapeHtml(buildHubExchangeUrl()) + "</td></tr>");
        out.println("    <tr><th>app_code</th><td>" + APP_CODE + "</td></tr>");
        out.println("  </table>");

        if (exchangeResult == null) {
            out.println("  <p class=\"fail\">No code parameter provided. Skipping exchange call.</p>");
        } else {
            out.println("  <p class=\"" + (exchangeResult.success ? "ok" : "fail") + "\">"
                    + (exchangeResult.success ? "Exchange success" : "Exchange failed") + "</p>");
            out.println("  <table>");
            out.println("    <tr><th>http_status</th><td>" + exchangeResult.httpStatus + "</td></tr>");
            out.println("    <tr><th>error</th><td>" + AuthPageRenderer.escapeHtml(exchangeResult.errorMessage)
                    + "</td></tr>");
            out.println("    <tr><th>login_ready</th><td>" + (exchangeResult.hasRequiredUserInfo() ? "yes" : "no")
                    + "</td></tr>");
            out.println("  </table>");

            if (exchangeResult.responseBody != null && !exchangeResult.responseBody.isEmpty()) {
                out.println("  <h2>Hub Response (Raw JSON)</h2>");
                out.println("  <pre>" + AuthPageRenderer.escapeHtml(exchangeResult.responseBody) + "</pre>");

                out.println("  <h2>Parsed Response Fields</h2>");
                out.println("  <table>");
                printParsedField(out, "hub_user_id", exchangeResult.hubUserId);
                printParsedField(out, "email", exchangeResult.email);
                printParsedField(out, "name", exchangeResult.name);
                printParsedField(out, "organization", exchangeResult.organization);
                printParsedField(out, "title", exchangeResult.title);
                printParsedField(out, "issued_at", exchangeResult.issuedAt);
                printParsedField(out, "expires_in_seconds", exchangeResult.expiresInSeconds);
                out.println("  </table>");
            }
        }

        out.println(
                "  <p class=\"note\">Next step will validate state and then create the Step session user from this response.</p>");
        out.println("  <p><a href=\"" + req.getContextPath() + "/home\">Try protected home</a></p>");

        FooterRenderer.render(out, getServletContext());

        out.println("</body>");
        out.println("</html>");
    }

    private void printParsedField(PrintWriter out, String key, String value) {
        out.println("    <tr><th>" + key + "</th><td>" + AuthPageRenderer.escapeHtml(value) + "</td></tr>");
    }

    private ExchangeResult exchangeCodeWithHub(HttpServletRequest req, String code) {
        ExchangeResult result = new ExchangeResult();
        String endpoint = buildHubExchangeUrl();
        result.httpStatus = -1;

        LOG.info("Calling Hub exchange API: endpoint={} app_code={} codeLength={}", endpoint, APP_CODE,
                code == null ? 0 : code.length());

        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            String userIp = req.getRemoteAddr();
            String requestJson = "{\"app_code\":\"" + APP_CODE + "\",\"code\":\""
                    + escapeJson(code) + "\",\"user_ip\":\"" + escapeJson(userIp) + "\"}";
            byte[] body = requestJson.getBytes(StandardCharsets.UTF_8);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int status = connection.getResponseCode();
            result.httpStatus = status;

            InputStream responseStream = (status >= 200 && status < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            result.responseBody = readStream(responseStream);
            result.success = status >= 200 && status < 300;
            result.hubUserId = extractJsonNumber(result.responseBody, "hub_user_id");
            result.email = extractJsonString(result.responseBody, "email");
            result.name = extractJsonString(result.responseBody, "name");
            result.organization = extractJsonString(result.responseBody, "organization");
            result.title = extractJsonString(result.responseBody, "title");
            result.issuedAt = extractJsonString(result.responseBody, "issued_at");
            result.expiresInSeconds = extractJsonNumber(result.responseBody, "expires_in_seconds");
            result.requestedUrlFromHub = extractJsonString(result.responseBody, "requested_url");
            result.returnToFromHub = extractJsonString(result.responseBody, "return_to");
            LOG.info("Hub exchange parsed fields: requested_url={} return_to={} hub_user_id={} email={} login_ready={}",
                    result.requestedUrlFromHub,
                    result.returnToFromHub,
                    result.hubUserId,
                    result.email,
                    result.hasRequiredUserInfo());
            if (!result.success) {
                result.errorMessage = "Hub returned non-success HTTP status";
            } else if (!result.hasRequiredUserInfo()) {
                result.errorMessage = "Hub response is missing one or more required user fields";
                result.success = false;
            }
        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            LOG.warn("Exception during Hub exchange call", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return result;
    }

    private String buildHubExchangeUrl() {
        return AuthSessionSupport.getHubAuthExchangeUrl();
    }

    private RedirectDecision determineRedirectDecision(HttpServletRequest req, String requestedUrlFromHub) {
        String homeTarget = req.getContextPath() + "/home";
        if (requestedUrlFromHub == null || requestedUrlFromHub.trim().isEmpty()) {
            return new RedirectDecision(homeTarget, "Hub exchange response did not include requested_url");
        }

        try {
            URI requestedUri = new URI(requestedUrlFromHub.trim());
            URI configuredStepUri = new URI(SoftwareVersion.STEP_EXTERNAL_URL);

            if (!equalsIgnoreCase(requestedUri.getScheme(), configuredStepUri.getScheme())
                    || !equalsIgnoreCase(requestedUri.getHost(), configuredStepUri.getHost())
                    || getEffectivePort(requestedUri) != getEffectivePort(configuredStepUri)) {
                return new RedirectDecision(homeTarget,
                        "requested_url host, scheme, or port did not match configured step.external.url");
            }

            String configuredPath = normalizePath(configuredStepUri.getPath());
            String requestedPath = normalizePath(requestedUri.getPath());
            if (!requestedPath.equals(configuredPath) && !requestedPath.startsWith(configuredPath + "/")) {
                return new RedirectDecision(homeTarget,
                        "requested_url path was outside configured Step base path");
            }

            StringBuilder redirectTarget = new StringBuilder(requestedPath);
            if (requestedUri.getRawQuery() != null && !requestedUri.getRawQuery().isEmpty()) {
                redirectTarget.append('?').append(requestedUri.getRawQuery());
            }
            if (requestedUri.getRawFragment() != null && !requestedUri.getRawFragment().isEmpty()) {
                redirectTarget.append('#').append(requestedUri.getRawFragment());
            }
            return new RedirectDecision(redirectTarget.toString(), "none");
        } catch (URISyntaxException e) {
            LOG.warn("Invalid requested_url returned from Hub: {}", requestedUrlFromHub, e);
            return new RedirectDecision(homeTarget, "requested_url from Hub was not a valid URI");
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String extractJsonString(String json, String key) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractJsonNumber(String json, String key) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*([0-9]+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String abbreviateForLog(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private int getEffectivePort(URI uri) {
        if (uri.getPort() > -1) {
            return uri.getPort();
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return -1;
        }
        if (scheme.equalsIgnoreCase("https")) {
            return 443;
        }
        if (scheme.equalsIgnoreCase("http")) {
            return 80;
        }
        return -1;
    }

    private boolean equalsIgnoreCase(String first, String second) {
        if (first == null) {
            return second == null;
        }
        return second != null && first.equalsIgnoreCase(second);
    }

    private static class ExchangeResult {
        private boolean success;
        private int httpStatus;
        private String responseBody = "";
        private String errorMessage = "";
        private String hubUserId = "";
        private String email = "";
        private String name = "";
        private String organization = "";
        private String title = "";
        private String issuedAt = "";
        private String expiresInSeconds = "";
        private String requestedUrlFromHub = "";
        private String returnToFromHub = "";

        private boolean hasRequiredUserInfo() {
            return !isBlank(name) && !isBlank(organization) && !isBlank(title) && !isBlank(email);
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }

    private static class RedirectDecision {
        private final String target;
        private final String fallbackReason;

        private RedirectDecision(String target, String fallbackReason) {
            this.target = target;
            this.fallbackReason = fallbackReason;
        }
    }
}
