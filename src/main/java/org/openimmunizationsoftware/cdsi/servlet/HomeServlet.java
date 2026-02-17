package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HomeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html");
        PrintWriter out = new PrintWriter(resp.getOutputStream());

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <title>CDSi Home</title>");
        out.println("  <style>");
        out.println("    body { font-family: Arial, sans-serif; margin: 20px; }");
        out.println("    h1, h2 { color: #333; }");
        out.println(
                "    img { float: right; margin: 20px 0 20px 20px; border: 1px solid #ddd; padding: 8px; background-color: #f9f9f9; }");
        out.println(
                "    .summary { margin: 20px 0; padding: 15px; background-color: #f5f5f5; border: 1px solid #ddd; }");
        out.println("    table { border-collapse: collapse; margin: 20px 0; width: 100%; }");
        out.println("    th, td { border: 1px solid #ddd; padding: 10px; text-align: left; vertical-align: top; }");
        out.println("    th { background-color: #f2f2f2; font-weight: bold; }");
        out.println("    .link-title { font-weight: bold; }");
        out.println("    .muted { color: #666; font-size: 12px; }");
        out.println("  </style>");
        out.println("</head>");
        out.println("<body>");

        out.println("  <h1>CDSi Home</h1>");
        out.println("  <img src=\"pm.png\" alt=\"Process Model Diagram\" width=\"900\" />");

        out.println("  <div class=\"summary\">");
        out.println(
                "    <p>Welcome to Step Into CDSi. This home page will be expanded over time with documentation, references, and system information.</p>");
        out.println("    <p>Use the links below to access key application pages.</p>");
        out.println("  </div>");

        out.println("  <h2>Application Links</h2>");
        out.println("  <table>");
        out.println("    <tr><th>Page</th><th>Description</th></tr>");
        out.println(
                "    <tr><td><a class=\"link-title\" href=\"supportingData\">Supporting Data</a></td><td>Browse available supporting data versions, release notes, and antigen XML/Excel files.</td></tr>");
        out.println(
                "    <tr><td><a class=\"link-title\" href=\"run\">Run</a></td><td>Execute forecasts with logging controls.</td></tr>");
        out.println(
                "    <tr><td><a class=\"link-title\" href=\"forecast\">Forecast</a></td><td>Generate standard forecast output.</td></tr>");
        out.println(
                "    <tr><td><a class=\"link-title\" href=\"step\">Step</a></td><td>Step-through CDSi logic flow.</td></tr>");
        out.println(
                "    <tr><td><a class=\"link-title\" href=\"sandbox\">Sandbox</a></td><td>FHIR test harness and interactive examples.</td></tr>");
        out.println("  </table>");

        out.println(
                "  <p class=\"muted\">Planned: expanded app overview, implementation notes, and supporting documentation index.</p>");

        out.println("</body>");
        out.println("</html>");
        out.close();
    }
}
