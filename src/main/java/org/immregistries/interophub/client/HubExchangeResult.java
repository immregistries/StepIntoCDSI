package org.immregistries.interophub.client;

public class HubExchangeResult {

    private final boolean success;
    private final int httpStatus;
    private final String responseBody;
    private final String errorMessage;
    private final HubUserInfo userInfo;
    private final String requestedUrlFromHub;
    private final String returnToFromHub;

    public HubExchangeResult(boolean success, int httpStatus, String responseBody, String errorMessage,
            HubUserInfo userInfo, String requestedUrlFromHub, String returnToFromHub) {
        this.success = success;
        this.httpStatus = httpStatus;
        this.responseBody = trimOrEmpty(responseBody);
        this.errorMessage = trimOrEmpty(errorMessage);
        this.userInfo = userInfo == null
                ? new HubUserInfo("", "", "", "", "", "", "")
                : userInfo;
        this.requestedUrlFromHub = trimOrEmpty(requestedUrlFromHub);
        this.returnToFromHub = trimOrEmpty(returnToFromHub);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public HubUserInfo getUserInfo() {
        return userInfo;
    }

    public String getRequestedUrlFromHub() {
        return requestedUrlFromHub;
    }

    public String getReturnToFromHub() {
        return returnToFromHub;
    }

    public boolean hasRequiredUserInfo() {
        return userInfo.hasRequiredUserInfo();
    }

    private static String trimOrEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
