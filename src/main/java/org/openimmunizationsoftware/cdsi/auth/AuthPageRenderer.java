package org.openimmunizationsoftware.cdsi.auth;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

public final class AuthPageRenderer {

    private AuthPageRenderer() {
    }

    public static void renderSignedInHeader(PrintWriter out, HttpServletRequest request) {
        SessionUser user = AuthSessionSupport.getSessionUser(request);
        out.println(
                "<div style=\"padding: 8px 12px; margin-bottom: 12px; background: #f5f7fa; border: 1px solid #d6dde6; text-align: right;\">");
        if (user == null) {
            out.println("No Authenticated");
        } else {
            String accountUrl = request.getContextPath() + "/account";
            out.println("Signed in as <a href=\"" + accountUrl + "\">" + escapeHtml(user.getDisplayName()) + "</a>");
        }
        out.println("</div>");
    }

    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
