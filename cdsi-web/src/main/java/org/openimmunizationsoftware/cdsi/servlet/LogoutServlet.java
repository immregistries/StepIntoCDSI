package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;

import org.openimmunizationsoftware.cdsi.auth.AuthSessionSupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.sendRedirect(AuthSessionSupport.getHubHomeUrl());
    }
}
