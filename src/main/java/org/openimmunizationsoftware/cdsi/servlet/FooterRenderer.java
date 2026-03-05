package org.openimmunizationsoftware.cdsi.servlet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import jakarta.servlet.ServletContext;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;

public final class FooterRenderer {

    private static final String FOOTER_TEMPLATE_PATH = "/WEB-INF/footer.html";
    private static final String VERSION_TOKEN = "${software.version}";
    private static volatile String cachedTemplate;

    private FooterRenderer() {
    }

    public static void render(PrintWriter out, ServletContext servletContext) {
        String template = getTemplate(servletContext);
        String footer = template.replace(VERSION_TOKEN, escapeHtml(SoftwareVersion.VERSION));
        out.println(footer);
    }

    private static String getTemplate(ServletContext servletContext) {
        if (cachedTemplate != null) {
            return cachedTemplate;
        }
        synchronized (FooterRenderer.class) {
            if (cachedTemplate != null) {
                return cachedTemplate;
            }
            cachedTemplate = loadTemplate(servletContext);
            return cachedTemplate;
        }
    }

    private static String loadTemplate(ServletContext servletContext) {
        if (servletContext != null) {
            try (InputStream is = servletContext.getResourceAsStream(FOOTER_TEMPLATE_PATH)) {
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        return reader.lines().collect(Collectors.joining("\n"));
                    }
                }
            } catch (Exception e) {
            }
        }

        return "<footer style=\"margin-top:40px; padding-top:20px; border-top:1px solid #ccc; font-size:0.9em;\">"
                + "<p><strong>American Immunization Registry Association (AIRA)</strong> supports collaboration, standards, and shared solutions that strengthen immunization information systems and improve the use of vaccination data to protect public health. "
                + "<a href=\"https://www.immregistries.org/\">https://www.immregistries.org/</a></p>"
                + "<p style=\"margin-top:20px;\">Step Into CDSi version " + escapeHtml(SoftwareVersion.VERSION)
                + " &dash; &copy; Copyright 2026, American Immunization Registry Association. All rights reserved. "
                + "<a href=\"https://aira.memberclicks.net/assets/docs/Organizational_Docs/AIRA%20Privacy%20Policy%20-%20Final%202024_.pdf\">Privacy Policy</a> "
                + "<a href=\"https://repository.immregistries.org/files/AIRA-Terms_of_Use_2024.pdf\">Terms of Use</a></p>"
                + "</footer>";
    }

    private static String escapeHtml(String text) {
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
