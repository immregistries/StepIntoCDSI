package org.immregistries.interophub.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpInteropHubClient implements InteropHubClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpInteropHubClient.class);

    private final HubClientConfig config;

    public HttpInteropHubClient(HubClientConfig config) {
        this.config = config;
    }

    @Override
    public String buildLoginUrl(String requestedUrl) {
        String hubHomeUrl = getHubHomeUrl();
        String separator = hubHomeUrl.contains("?") ? "&" : "?";
        String state = UUID.randomUUID().toString();
        String returnTo = buildLoginReturnUrl();

        return hubHomeUrl + separator
                + "app_code=" + encode(config.getAppCode())
                + "&return_to=" + encode(returnTo)
                + "&state=" + encode(state)
                + "&requested_url=" + encode(valueOrEmpty(requestedUrl));
    }

    @Override
    public String getHubHomeUrl() {
        return appendPath(config.getHubExternalUrl(), "home");
    }

    @Override
    public String getHubAuthExchangeUrl() {
        return appendPath(config.getHubExternalUrl(), "api/auth/exchange");
    }

    @Override
    public HubExchangeResult exchangeCode(String code, String userIp) {
        String endpoint = getHubAuthExchangeUrl();
        HttpURLConnection connection = null;

        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(config.getConnectTimeoutMs());
            connection.setReadTimeout(config.getReadTimeoutMs());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            String requestJson = "{\"app_code\":\"" + escapeJson(config.getAppCode())
                    + "\",\"code\":\"" + escapeJson(code)
                    + "\",\"user_ip\":\"" + escapeJson(userIp) + "\"}";

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestJson.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            InputStream responseStream = (status >= 200 && status < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = readStream(responseStream);

            HubUserInfo userInfo = new HubUserInfo(
                    extractJsonNumber(responseBody, "hub_user_id"),
                    extractJsonString(responseBody, "email"),
                    extractJsonString(responseBody, "name"),
                    extractJsonString(responseBody, "organization"),
                    extractJsonString(responseBody, "title"),
                    extractJsonString(responseBody, "issued_at"),
                    extractJsonNumber(responseBody, "expires_in_seconds"));

            String requestedUrlFromHub = extractJsonString(responseBody, "requested_url");
            String returnToFromHub = extractJsonString(responseBody, "return_to");

            boolean success = status >= 200 && status < 300;
            String errorMessage = "";
            if (!success) {
                errorMessage = "Hub returned non-success HTTP status";
            } else if (!userInfo.hasRequiredUserInfo()) {
                errorMessage = "Hub response is missing one or more required user fields";
                success = false;
            }

            LOG.info("Hub exchange parsed fields: requested_url={} return_to={} hub_user_id={} email={} login_ready={}",
                    requestedUrlFromHub,
                    returnToFromHub,
                    userInfo.getHubUserId(),
                    userInfo.getEmail(),
                    userInfo.hasRequiredUserInfo());

            return new HubExchangeResult(
                    success,
                    status,
                    responseBody,
                    errorMessage,
                    userInfo,
                    requestedUrlFromHub,
                    returnToFromHub);
        } catch (Exception e) {
            LOG.warn("Exception during Hub exchange call", e);
            return new HubExchangeResult(
                    false,
                    -1,
                    "",
                    e.getClass().getSimpleName() + ": " + e.getMessage(),
                    new HubUserInfo("", "", "", "", "", "", ""),
                    "",
                    "");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public HubRedirectDecision determineRedirectDecision(String requestedUrlFromHub, String fallbackTarget) {
        String fallback = isBlank(fallbackTarget) ? "/home" : fallbackTarget;
        if (isBlank(requestedUrlFromHub)) {
            return new HubRedirectDecision(fallback, "Hub exchange response did not include requested_url");
        }

        try {
            URI requestedUri = new URI(requestedUrlFromHub.trim());
            URI configuredStepUri = new URI(config.getStepExternalUrl());

            if (!equalsIgnoreCase(requestedUri.getScheme(), configuredStepUri.getScheme())
                    || !equalsIgnoreCase(requestedUri.getHost(), configuredStepUri.getHost())
                    || getEffectivePort(requestedUri) != getEffectivePort(configuredStepUri)) {
                return new HubRedirectDecision(
                        fallback,
                        "requested_url host, scheme, or port did not match configured step external URL");
            }

            String configuredPath = normalizePath(configuredStepUri.getPath());
            String requestedPath = normalizePath(requestedUri.getPath());
            if (!requestedPath.equals(configuredPath) && !requestedPath.startsWith(configuredPath + "/")) {
                return new HubRedirectDecision(
                        fallback,
                        "requested_url path was outside configured Step base path");
            }

            StringBuilder redirectTarget = new StringBuilder(requestedPath);
            if (!isBlank(requestedUri.getRawQuery())) {
                redirectTarget.append('?').append(requestedUri.getRawQuery());
            }
            if (!isBlank(requestedUri.getRawFragment())) {
                redirectTarget.append('#').append(requestedUri.getRawFragment());
            }
            return new HubRedirectDecision(redirectTarget.toString(), "none");
        } catch (URISyntaxException e) {
            LOG.warn("Invalid requested_url returned from Hub: {}", requestedUrlFromHub, e);
            return new HubRedirectDecision(fallback, "requested_url from Hub was not a valid URI");
        }
    }

    private String buildLoginReturnUrl() {
        String base = trimTrailingSlash(config.getStepExternalUrl());
        return base + "/login";
    }

    private String appendPath(String baseUrl, String path) {
        String base = trimTrailingSlash(baseUrl);
        String p = valueOrEmpty(path);
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.isEmpty()) {
            return base;
        }
        return base + "/" + p;
    }

    private String trimTrailingSlash(String value) {
        String v = valueOrEmpty(value);
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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

    private String normalizePath(String path) {
        if (isBlank(path)) {
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
}
