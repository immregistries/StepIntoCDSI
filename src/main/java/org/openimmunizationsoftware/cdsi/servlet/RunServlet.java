package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel;

/**
 * Human-friendly servlet for execution with log level controls.
 * Renders an HTML page with log level controls and output display.
 */
public class RunServlet extends ForecastServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            StepServlet.registerRequest(req);

            resp.setContentType("text/html");
            PrintWriter out = new PrintWriter(resp.getOutputStream());

            // Parse logging settings
            boolean loggingEnabled = req.getParameter("log") != null;
            Map<LogicStepType, LogLevel> stepLevelMap = null;
            LogLevel defaultLevel = LogLevel.CONTROL;

            if (loggingEnabled) {
                stepLevelMap = buildStepLevelMap(req);
                String globalLevelParam = req.getParameter("logLevel");
                if (globalLevelParam != null) {
                    try {
                        defaultLevel = LogLevel.valueOf(globalLevelParam.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        defaultLevel = LogLevel.CONTROL;
                    }
                }
            }

            // Generate output if requested
            String outputText = "";
            if ("true".equals(req.getParameter("execute"))) {
                DataModel dataModel = readRequest(req);
                StringBuilder logBuffer = loggingEnabled ? new StringBuilder() : null;
                process(dataModel, new PrintWriter(new java.io.StringWriter()), logBuffer, stepLevelMap);
                outputText = generateTextOutput(dataModel, logBuffer, req);
            }

            // Render HTML page
            renderHtmlPage(out, req, loggingEnabled, defaultLevel, stepLevelMap, outputText);

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    private void renderHtmlPage(PrintWriter out, HttpServletRequest req,
            boolean loggingEnabled, LogLevel defaultLevel,
            Map<LogicStepType, LogLevel> stepLevelMap, String outputText) {

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <title>CDSi Run - Execution + Log Controls</title>");
        out.println("  <style>");
        out.println("    body { font-family: Arial, sans-serif; margin: 20px; }");
        out.println("    h1 { color: #333; }");
        out.println(
                "    img { float: right; margin: 20px 0 20px 20px; border: 1px solid #ddd; padding: 8px; background-color: #f9f9f9; }");
        out.println("    table { border-collapse: collapse; margin: 20px 0; }");
        out.println("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("    th { background-color: #f2f2f2; font-weight: bold; }");
        out.println("    .step-name { font-size: 11px; max-width: 300px; }");
        out.println("    .buttons { margin: 20px 0; }");
        out.println(
                "    .buttons button { padding: 10px 20px; margin-right: 10px; font-size: 14px; cursor: pointer; }");
        out.println(
                "    .output-container { border: 1px solid #ccc; padding: 12px; max-height: 600px; overflow: auto; background-color: #f9f9f9; margin-top: 20px; }");
        out.println("    .copy-button { margin-bottom: 10px; padding: 8px 16px; cursor: pointer; }");
        out.println(
                "    pre { margin: 0; white-space: pre-wrap; word-wrap: break-word; font-family: 'Courier New', monospace; font-size: 12px; }");
        out.println(
                "    .controls { margin: 20px 0; padding: 15px; background-color: #f5f5f5; border: 1px solid #ddd; }");
        out.println("    label { margin-right: 10px; }");
        out.println("  </style>");
        out.println("  <script>");
        out.println("    function copyRunOutput() {");
        out.println("      var text = document.getElementById('runOutput').textContent;");
        out.println("      navigator.clipboard.writeText(text).then(function() {");
        out.println("        alert('Output copied to clipboard');");
        out.println("      }, function(err) {");
        out.println("        alert('Failed to copy: ' + err);");
        out.println("      });");
        out.println("    }");
        out.println("  </script>");
        out.println("</head>");
        out.println("<body>");
        out.println("  <h1>CDSi Run - Execution + Log Controls</h1>");
        out.println(
                "  <img src=\"pm.png\" alt=\"Process Model Diagram\" width=\"900\" style=\"cursor: pointer;\" onclick=\"alert('This diagram shows the flow and relationship of all CDSi logic steps');\" />");
        out.println("  <form method=\"get\" action=\"run\">");

        // Hidden inputs for all non-log parameters
        renderHiddenInputs(out, req);

        // Global logging controls
        out.println("  <div class=\"controls\">");
        out.println("    <h3>Global Logging Controls</h3>");
        out.println("    <label><input type=\"checkbox\" name=\"log\" value=\"true\" "
                + (loggingEnabled ? "checked" : "") + "> Enable Logging</label>");
        out.println("    <label style=\"margin-left: 20px;\">Default Level: ");
        out.println("      <select name=\"logLevel\">");
        for (LogLevel level : LogLevel.values()) {
            String selected = (level == defaultLevel) ? " selected" : "";
            out.println("        <option value=\"" + level.name() + "\"" + selected + ">"
                    + level.name() + "</option>");
        }
        out.println("      </select>");
        out.println("    </label>");
        out.println("  </div>");

        // Per-step log level table
        out.println("  <h3>Per-Step Log Levels</h3>");
        out.println("  <table>");
        out.println("    <tr>");
        out.println("      <th>Logic Step</th>");
        out.println("      <th>CONTROL</th>");
        out.println("      <th>STATE</th>");
        out.println("      <th>REASONING</th>");
        out.println("      <th>TRACE</th>");
        out.println("      <th>DUMP</th>");
        out.println("    </tr>");

        for (LogicStepType stepType : LogicStep.STEPS) {
            if (stepType == LogicStepType.END)
                continue; // Skip END step

            LogLevel currentLevel = (stepLevelMap != null)
                    ? stepLevelMap.getOrDefault(stepType, defaultLevel)
                    : defaultLevel;
            String paramName = "log" + stepType.name();

            out.println("    <tr>");
            out.println("      <td class=\"step-name\">" + stepType.getChapter() + " " + stepType.getName() + "</td>");

            for (LogLevel level : LogLevel.values()) {
                String checked = (level == currentLevel) ? " checked" : "";
                out.println("      <td><input type=\"radio\" name=\"" + paramName
                        + "\" value=\"" + level.name() + "\"" + checked + "></td>");
            }

            out.println("    </tr>");
        }

        out.println("  </table>");

        // Action buttons
        out.println("  <div class=\"buttons\">");
        out.println("    <button type=\"submit\" name=\"execute\" value=\"true\">Run</button>");
        out.println("    <button type=\"submit\" formaction=\"forecast\">Forecast</button>");
        out.println("    <button type=\"submit\" formaction=\"step\">Step</button>");
        out.println("  </div>");
        out.println("  </form>");

        // Output display
        if (!outputText.isEmpty()) {
            out.println("  <br style=\"clear: both;\" />");
            out.println("  <div class=\"output-container\">");
            out.println(
                    "    <button type=\"button\" class=\"copy-button\" onclick=\"copyRunOutput()\">Copy Output</button>");
            out.println("    <pre id=\"runOutput\">" + escapeHtml(outputText) + "</pre>");
            out.println("  </div>");
        }

        out.println("</body>");
        out.println("</html>");
    }

    private void renderHiddenInputs(PrintWriter out, HttpServletRequest req) {
        @SuppressWarnings("unchecked")
        Map<String, String[]> paramMap = req.getParameterMap();

        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String name = entry.getKey();

            // Skip log control parameters and execute flag
            if (name.equals("log") || name.equals("logLevel") || name.equals("execute")) {
                continue;
            }

            // Skip per-step log parameters
            if (name.startsWith("log") && name.length() > 3) {
                boolean isStepParam = false;
                for (LogicStepType stepType : LogicStep.STEPS) {
                    if (name.equals("log" + stepType.name())) {
                        isStepParam = true;
                        break;
                    }
                }
                if (isStepParam)
                    continue;
            }

            // Add hidden input for this parameter
            String[] values = entry.getValue();
            for (String value : values) {
                out.println("    <input type=\"hidden\" name=\"" + escapeHtml(name)
                        + "\" value=\"" + escapeHtml(value) + "\">");
            }
        }
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
