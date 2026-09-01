package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import org.immregistries.interophub.client.HubExchangeResult;
import org.immregistries.interophub.client.HubRedirectDecision;
import org.immregistries.interophub.client.HubUserInfo;
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
        String endpoint = AuthSessionSupport.getHubAuthExchangeUrl();
        result.httpStatus = -1;

        LOG.info("Calling Hub exchange API: endpoint={} app_code={} codeLength={}", endpoint, APP_CODE,
                code == null ? 0 : code.length());

        HubExchangeResult hubResult = AuthSessionSupport.getInteropHubClient().exchangeCode(code, req.getRemoteAddr());
        HubUserInfo user = hubResult.getUserInfo();

        result.success = hubResult.isSuccess();
        result.httpStatus = hubResult.getHttpStatus();
        result.responseBody = hubResult.getResponseBody();
        result.errorMessage = hubResult.getErrorMessage();
        result.hubUserId = user.getHubUserId();
        result.email = user.getEmail();
        result.name = user.getName();
        result.organization = user.getOrganization();
        result.title = user.getTitle();
        result.issuedAt = user.getIssuedAt();
        result.expiresInSeconds = user.getExpiresInSeconds();
        result.requestedUrlFromHub = hubResult.getRequestedUrlFromHub();

        return result;
    }

    private String buildHubExchangeUrl() {
        return AuthSessionSupport.getHubAuthExchangeUrl();
    }

    private RedirectDecision determineRedirectDecision(HttpServletRequest req, String requestedUrlFromHub) {
        String homeTarget = req.getContextPath() + "/home";
        HubRedirectDecision decision = AuthSessionSupport.getInteropHubClient()
                .determineRedirectDecision(requestedUrlFromHub, homeTarget);
        return new RedirectDecision(decision.getTarget(), decision.getFallbackReason());
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
