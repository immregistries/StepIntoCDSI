package org.openimmunizationsoftware.cdsi.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openimmunizationsoftware.cdsi.auth.AuthPageRenderer;
import org.openimmunizationsoftware.cdsi.auth.AuthSessionSupport;
import org.openimmunizationsoftware.cdsi.auth.SessionUser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String APP_CODE = "step";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String code = req.getParameter("code");
        String state = req.getParameter("state");
        ExchangeResult exchangeResult = null;
        if (code != null && !code.trim().isEmpty()) {
            exchangeResult = exchangeCodeWithHub(code.trim());
        }

        if (exchangeResult != null && exchangeResult.success && exchangeResult.hasRequiredUserInfo()) {
            SessionUser sessionUser = new SessionUser(
                    exchangeResult.name,
                    exchangeResult.organization,
                    exchangeResult.title,
                    exchangeResult.email);
            req.getSession(true).setAttribute(AuthSessionSupport.SESSION_USER_ATTRIBUTE, sessionUser);
            resp.sendRedirect(req.getContextPath() + "/home");
            return;
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

    private ExchangeResult exchangeCodeWithHub(String code) {
        ExchangeResult result = new ExchangeResult();
        String endpoint = buildHubExchangeUrl();
        result.httpStatus = -1;

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

            String requestJson = "{\"app_code\":\"" + APP_CODE + "\",\"code\":\""
                    + escapeJson(code) + "\"}";
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
            if (!result.success) {
                result.errorMessage = "Hub returned non-success HTTP status";
            } else if (!result.hasRequiredUserInfo()) {
                result.errorMessage = "Hub response is missing one or more required user fields";
                result.success = false;
            }
        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
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

        private boolean hasRequiredUserInfo() {
            return !isBlank(name) && !isBlank(organization) && !isBlank(title) && !isBlank(email);
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
