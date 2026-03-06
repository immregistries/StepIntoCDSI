package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.auth.AuthPageRenderer;
import org.openimmunizationsoftware.cdsi.auth.AuthSessionSupport;
import org.openimmunizationsoftware.cdsi.auth.SessionUser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Temporary, non-authenticated setup page for local testing.
 * Remove this servlet before production deployment.
 */
public class TemporaryAuthSetupServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_DISPLAY_NAME = "Test User";
    private static final String DEFAULT_ORGANIZATION = "AIRA";
    private static final String DEFAULT_TITLE = "Analyst";
    private static final String DEFAULT_EMAIL = "test.user@example.org";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        renderPage(req, resp, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String displayName = valueOrDefault(req.getParameter("displayName"), DEFAULT_DISPLAY_NAME);
        String organization = valueOrDefault(req.getParameter("organization"), DEFAULT_ORGANIZATION);
        String title = valueOrDefault(req.getParameter("title"), DEFAULT_TITLE);
        String email = valueOrDefault(req.getParameter("email"), DEFAULT_EMAIL);
        String returnPath = valueOrDefault(req.getParameter("returnPath"), req.getContextPath() + "/home");

        SessionUser user = new SessionUser(displayName, organization, title, email);
        HttpSession session = req.getSession(true);
        session.setAttribute(AuthSessionSupport.SESSION_USER_ATTRIBUTE, user);

        if (returnPath.startsWith("http://") || returnPath.startsWith("https://")) {
            resp.sendRedirect(returnPath);
            return;
        }

        if (!returnPath.startsWith("/")) {
            returnPath = "/" + returnPath;
        }

        if (!returnPath.startsWith(req.getContextPath() + "/")) {
            if (returnPath.equals("/")) {
                returnPath = req.getContextPath() + "/home";
            } else {
                returnPath = req.getContextPath() + returnPath;
            }
        }

        resp.sendRedirect(returnPath);
    }

    private void renderPage(HttpServletRequest req, HttpServletResponse resp, String message)
            throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String contextPath = req.getContextPath();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <title>Temporary Step Auth Setup</title>");
        out.println("  <style>");
        out.println("    body { font-family: Arial, sans-serif; margin: 24px; max-width: 760px; }");
        out.println("    h1 { margin-bottom: 8px; }");
        out.println(
                "    .warning { background: #fff3cd; border: 1px solid #ffe69c; padding: 12px; margin-bottom: 16px; }");
        out.println("    .row { margin-bottom: 10px; }");
        out.println("    label { display: inline-block; width: 140px; font-weight: bold; }");
        out.println("    input[type='text'] { width: 420px; padding: 6px; }");
        out.println("    button { padding: 9px 14px; cursor: pointer; margin-right: 8px; }");
        out.println("    .muted { color: #666; font-size: 13px; }");
        out.println("  </style>");
        out.println("</head>");
        out.println("<body>");

        out.println("  <h1>Temporary Auth Setup</h1>");
        out.println("  <div class=\"warning\">");
        out.println("    This page bypasses normal authentication and is intended for temporary local testing only.");
        out.println("    Remove it before production use.");
        out.println("  </div>");

        if (message != null && !message.isEmpty()) {
            out.println("  <p>" + AuthPageRenderer.escapeHtml(message) + "</p>");
        }

        out.println("  <form method=\"post\" action=\"" + contextPath + "/temp-auth\">");
        out.println(
                "    <div class=\"row\"><label for=\"displayName\">Display Name</label><input id=\"displayName\" name=\"displayName\" type=\"text\" value=\""
                        + DEFAULT_DISPLAY_NAME + "\"></div>");
        out.println(
                "    <div class=\"row\"><label for=\"organization\">Organization</label><input id=\"organization\" name=\"organization\" type=\"text\" value=\""
                        + DEFAULT_ORGANIZATION + "\"></div>");
        out.println(
                "    <div class=\"row\"><label for=\"title\">Title</label><input id=\"title\" name=\"title\" type=\"text\" value=\""
                        + DEFAULT_TITLE + "\"></div>");
        out.println(
                "    <div class=\"row\"><label for=\"email\">Email</label><input id=\"email\" name=\"email\" type=\"text\" value=\""
                        + DEFAULT_EMAIL + "\"></div>");
        out.println(
                "    <div class=\"row\"><label for=\"returnPath\">Return URL</label><input id=\"returnPath\" name=\"returnPath\" type=\"text\" value=\""
                        + contextPath + "/home\"></div>");
        out.println("    <div class=\"row\">");
        out.println("      <button type=\"submit\">Set Session User And Continue</button>");
        out.println("      <a href=\"" + contextPath + "/home\">Go To Home</a>");
        out.println("    </div>");
        out.println("  </form>");

        out.println(
                "  <p class=\"muted\">Shortcut: open this page and click the button without changing anything.</p>");
        out.println("</body>");
        out.println("</html>");
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}

