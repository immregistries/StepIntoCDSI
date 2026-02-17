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
        out.println("  <title>Step Into CDSi</title>");
        out.println("  <style>");
        out.println("    body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }");
        out.println("    h1, h2, h3 { color: #333; }");
        out.println("    h1 { margin-bottom: 20px; }");
        out.println("    h2 { margin-top: 30px; margin-bottom: 15px; }");
        out.println("    h3 { margin-top: 15px; margin-bottom: 10px; }");
        out.println(
                "    img { float: right; margin: 20px 0 20px 20px; border: 1px solid #ddd; padding: 8px; background-color: #f9f9f9; }");
        out.println("    p { margin: 10px 0; }");
        out.println("    a { color: #007bff; text-decoration: none; }");
        out.println("    a:hover { text-decoration: underline; }");
        out.println("    table { border-collapse: collapse; margin: 20px 0; width: 100%; clear: both; }");
        out.println("    th, td { border: 1px solid #ddd; padding: 10px; text-align: left; vertical-align: top; }");
        out.println("    th { background-color: #f2f2f2; font-weight: bold; }");
        out.println("    .muted { color: #666; font-size: 12px; }");
        out.println("    .action-links { font-size: 12px; }");
        out.println("    .action-links a { margin-right: 8px; }");
        out.println("    strong { font-weight: bold; }");
        out.println("  </style>");
        out.println("</head>");
        out.println("<body>");

        out.println("  <h1>Step Into CDSi</h1>");
        out.println("  <img src=\"pm.png\" alt=\"Process Model Diagram\" width=\"900\" />");

        out.println("  <p>");
        out.println(
                "    <strong>Step Into CDSi</strong> is an open-source reference and demonstration tool for understanding how");
        out.println(
                "    immunization clinical decision support (CDS) logic is executed in practice. It enables developers, analysts,");
        out.println(
                "    and public-health stakeholders to explore vaccine evaluation and forecasting behavior in a transparent,");
        out.println("    step-by-step manner rather than relying on opaque &quot;black-box&quot; forecasting engines.");
        out.println("  </p>");

        out.println("  <p>");
        out.println(
                "    This project is <strong>independent of the CDC CDSi initiative</strong>, but it is designed to work with and");
        out.println(
                "    illuminate the publicly available CDSi Logic Specification, Supporting Data, and test materials published by CDC.");
        out.println("    For official CDSi background and resources, see:");
        out.println(
                "    <a href=\"https://www.cdc.gov/iis/cdsi/index.html\">https://www.cdc.gov/iis/cdsi/index.html</a>");
        out.println("  </p>");

        // Example Test Cases section
        out.println("  <h2>Example Test Cases</h2>");
        out.println("  <p class=\"muted\">Select an example to run through different interfaces:</p>");
        out.println("  <table>");
        out.println("    <tr>");
        out.println("      <th>Category</th>");
        out.println("      <th>Example Name</th>");
        out.println("      <th style=\"width: 300px;\">Actions</th>");
        out.println("    </tr>");

        // Get examples from SandboxServletExamples
        java.util.List<SandboxServletExamples.ExampleCategory> examples = SandboxServletExamples
                .getExamples();
        for (SandboxServletExamples.ExampleCategory category : examples) {
            for (SandboxServletExamples.ExampleLink link : category.links) {
                out.println("    <tr>");
                out.println("      <td>" + escapeHtml(category.title) + "</td>");
                out.println("      <td>" + escapeHtml(link.name) + "</td>");
                out.println("      <td class=\"action-links\">");
                out.println("        <a href=\"/step/step" + escapeHtml(link.url) + "\">Step</a>");
                out.println("        <a href=\"/step/forecast" + escapeHtml(link.url) + "\">Forecast</a>");
                out.println("        <a href=\"/step/run" + escapeHtml(link.url) + "\">Run</a>");
                out.println("        <a href=\"/step/sandbox" + escapeHtml(link.url) + "\">FHIR Sandbox</a>");
                out.println("      </td>");
                out.println("    </tr>");
            }
        }

        out.println("  </table>");

        // Application Areas section
        out.println("  <h2>Application Areas</h2>");

        out.println("  <h3>Step</h3>");
        out.println("  <p>");
        out.println(
                "    Walk through CDS logic one step at a time for a specific patient scenario. Each step corresponds to a chapter");
        out.println(
                "    of the CDSi Logic Specification, allowing users to see exactly how intermediate decisions and calculations are produced.");
        out.println("  </p>");

        out.println("  <h3>Supporting Data</h3>");
        out.println("  <p>");
        out.println(
                "    Browse and inspect the CDSi supporting data used by the engine. This enables direct comparison between specification");
        out.println("    inputs and step-level processing behavior.");
        out.println("  </p>");

        out.println("  <h3>Forecast</h3>");
        out.println("  <p>");
        out.println(
                "    Generate a full text forecast similar to outputs produced by production CDS engines. The system evaluates immunization");
        out.println(
                "    history, applies CDSi logic end-to-end, and returns final evaluation and recommendation results.");
        out.println("  </p>");

        out.println("  <h3>FHIR Server</h3>");
        out.println("  <p>");
        out.println(
                "    Expose forecasting functionality through a FHIR endpoint aligned with the Immunization Decision Support (ImmDS) FHIR");
        out.println("    implementation guide:");
        out.println(
                "    <a href=\"https://hl7.org/fhir/us/immds/index.html\">https://hl7.org/fhir/us/immds/index.html</a>");
        out.println("  </p>");

        out.println("  <h3>FHIR Sandbox</h3>");
        out.println("  <p>");
        out.println(
                "    Provide a simple FHIR client for interacting with the FHIR server, supporting testing, validation, and demonstration");
        out.println("    of CDS interoperability workflows.");
        out.println("  </p>");

        // Remaining content sections
        out.println("  <h2>Transparent Immunization Decision Support</h2>");
        out.println("  <p>");
        out.println(
                "    Immunization clinical decision support converts complex vaccination guidance into computable logic that determines");
        out.println(
                "    which vaccines a patient needs and when they should be given. Because national recommendations are published in");
        out.println(
                "    clinical language rather than executable rules, implementations across health information systems often vary.");
        out.println(
                "    Step Into CDSi makes this translation visible by exposing each stage of evaluation and forecasting, allowing users");
        out.println(
                "    to trace how patient history, timing rules, and schedule logic combine to produce final recommendations.");
        out.println("  </p>");

        out.println("  <h2>Designed for Learning, Testing, and Demonstration</h2>");
        out.println("  <p>");
        out.println(
                "    Step Into CDSi is intentionally built as a <strong>reference and demonstration system</strong>, not a production");
        out.println(
                "    clinical service. The application enables step-through inspection of logic, review of supporting data, and generation");
        out.println(
                "    of realistic forecast output comparable to operational CDS engines. As of February 2025, approximately");
        out.println(
                "    <strong>90% of CDSi-defined test cases pass</strong>, though patient condition logic and certain edge scenarios remain");
        out.println(
                "    incomplete. Results are broadly representative of real-world system behavior and useful for education, prototyping,");
        out.println(
                "    and interoperability demonstrations, but the system <strong>must not be used for clinical or public-health decision");
        out.println("    making</strong>.");
        out.println("  </p>");

        out.println("  <h2>Integration with the NIST FITS Testing Platform</h2>");
        out.println("  <p>");
        out.println(
                "    Step Into CDSi works directly with the <strong>Forecasting for Immunization Test Suite (FITS)</strong> maintained by NIST:");
        out.println("    <a href=\"https://fits.nist.gov/fits/\">https://fits.nist.gov/fits/</a>");
        out.println(
                "    CDC publishes official CDSi test cases through FITS, and the platform supports bidirectional testing workflows.");
        out.println(
                "    Step Into CDSi can retrieve and execute FITS test cases locally for validation and debugging, while FITS can also invoke");
        out.println(
                "    Step Into CDSi as an external forecasting engine for comparative testing. These complementary patterns support");
        out.println(
                "    specification verification, regression testing, and interoperability experimentation across CDS implementations.");
        out.println("  </p>");

        out.println("  <h2>Open Source Java Implementation</h2>");
        out.println("  <p>");
        out.println("    The full source code is publicly available on GitHub:");
        out.println(
                "    <a href=\"https://github.com/immregistries/StepIntoCDSI\">https://github.com/immregistries/StepIntoCDSI</a>");
        out.println(
                "    Step Into CDSi is implemented as a <strong>Java web application</strong> designed to run in a standard servlet container");
        out.println(
                "    such as Apache Tomcat and is distributed under the <strong>Apache 2.0 open-source license</strong>. This allows");
        out.println(
                "    implementers, researchers, and standards developers to review the logic, adapt the code, and use the system as a");
        out.println("    foundation for experimentation or education without licensing barriers.");
        out.println("  </p>");

        out.println("  <h2>Access and Security Context</h2>");
        out.println("  <p>");
        out.println(
                "    The demonstration application itself <strong>does not require login or authentication</strong> for general use.");
        out.println(
                "    However, integration with the FITS platform for automated testing requires valid FITS credentials issued through that");
        out.println(
                "    service. This separation preserves ease of demonstration while respecting the access controls associated with official");
        out.println("    test infrastructure.");
        out.println("  </p>");

        out.println("</body>");
        out.println("</html>");
        out.close();
    }

    /**
     * Escape HTML special characters for safe output.
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
