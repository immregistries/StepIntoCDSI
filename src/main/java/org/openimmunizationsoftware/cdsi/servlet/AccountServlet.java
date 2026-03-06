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

public class AccountServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        SessionUser user = AuthSessionSupport.getSessionUser(req);
        if (user == null) {
            AuthSessionSupport.redirectToHubLogin(req, resp);
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <title>Step Account</title>");
        out.println("  <style>");
        out.println("    body { font-family: Arial, sans-serif; margin: 24px; }");
        out.println("    h1 { margin-bottom: 18px; }");
        out.println("    table { border-collapse: collapse; min-width: 420px; }");
        out.println("    th, td { border: 1px solid #d8d8d8; padding: 10px; text-align: left; }");
        out.println("    th { width: 170px; background-color: #f4f4f4; }");
        out.println("    .actions { margin-top: 20px; }");
        out.println("    .actions a, .actions button { margin-right: 12px; }");
        out.println("    button { padding: 8px 14px; cursor: pointer; }");
        out.println("  </style>");
        out.println("</head>");
        out.println("<body>");

        AuthPageRenderer.renderSignedInHeader(out, req);

        out.println("  <h1>Account Details</h1>");
        out.println("  <table>");
        out.println("    <tr><th>Display Name</th><td>" + AuthPageRenderer.escapeHtml(user.getDisplayName())
                + "</td></tr>");
        out.println("    <tr><th>Organization</th><td>" + AuthPageRenderer.escapeHtml(user.getOrganization())
                + "</td></tr>");
        out.println("    <tr><th>Title</th><td>" + AuthPageRenderer.escapeHtml(user.getTitle()) + "</td></tr>");
        out.println("    <tr><th>Email</th><td>" + AuthPageRenderer.escapeHtml(user.getEmail()) + "</td></tr>");
        out.println("  </table>");

        out.println("  <div class=\"actions\">");
        out.println("    <a href=\"" + AuthSessionSupport.getHubHomeUrl() + "\">Return To Hub</a>");
        out.println("    <form method=\"post\" action=\"" + req.getContextPath()
                + "/logout\" style=\"display:inline;\">");
        out.println("      <button type=\"submit\">Logout Of Step</button>");
        out.println("    </form>");
        out.println("  </div>");

        FooterRenderer.render(out, getServletContext());

        out.println("</body>");
        out.println("</html>");
    }
}
