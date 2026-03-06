package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.openimmunizationsoftware.cdsi.auth.AuthSessionSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        logoutAndRedirect(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        logoutAndRedirect(req, resp);
    }

    private void logoutAndRedirect(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        AuthSessionSupport.clearSessionUser(req);
        resp.sendRedirect(SoftwareVersion.HUB_EXTERNAL_URL);
    }
}

