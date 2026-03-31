package org.openimmunizationsoftware.cdsi.auth;

import java.io.IOException;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthenticationFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No filter-specific initialization required.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!SoftwareVersion.AUTH_ENABLED) {
            chain.doFilter(request, response);
            return;
        }

        if (isPublicPath(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        if (AuthSessionSupport.getSessionUser(httpRequest) == null) {
            LOG.info("Unauthenticated request intercepted: method={} uri={} query={} fullUrl={}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    httpRequest.getQueryString(),
                    AuthSessionSupport.getCurrentUrl(httpRequest));
            AuthSessionSupport.redirectToHubLogin(httpRequest, httpResponse);
            return;
        }

        // Do not allow protected pages to be reused from browser cache after logout.
        httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setDateHeader("Expires", 0);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No filter-specific resources to cleanup.
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        String path = uri;
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            path = uri.substring(contextPath.length());
        }

        if (path == null || path.isEmpty()) {
            path = "/";
        }

        return path.equals("/login")
                || path.equals("/temp-auth")
                || path.equals("/logout")
                || path.equals("/forecast")
                || path.startsWith("/fhir/")
                || path.equals("/fhir");
    }
}
