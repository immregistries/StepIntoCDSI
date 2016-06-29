package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;

public class StepServlet extends ForecastServlet
{
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    DataModel dataModel = null;
    String action = req.getParameter("action");
    if (action != null && action.equals("next")) {
      dataModel = (DataModel) session.getAttribute("dataModel");
    }

    Exception exception = null;

    if (dataModel == null) {
      try {
        dataModel = readRequest(req);
        session.setAttribute("dataModel", dataModel);
      } catch (Exception e) {
        e.printStackTrace();
        throw new ServletException(e);
      }
    } else {
      try {
        String submit = req.getParameter("submit");
        if (submit != null && submit.equals("Jump")) {
          String jumpTo = req.getParameter("jumpTo");
          while (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END
              && !dataModel.getLogicStep().getLogicStepType().getName().equals(jumpTo)) {
            dataModel.setNextLogicStep(dataModel.getLogicStep().process());
          }
        }
        dataModel.setNextLogicStep(dataModel.getLogicStep().process());
      } catch (Exception e) {
        e.printStackTrace();
        exception = e;
      }
    }

    resp.setContentType("text/html");

    PrintWriter out = new PrintWriter(resp.getOutputStream());
    out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>CDSi - " + dataModel.getLogicStep().getTitle() + "</title>");
    out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"index.css\">");
    out.println("  </head>");
    out.println("  <body>");
    out.println("  <form action=\"step\" method=\"POST\" id=\"stepForm\">");
    out.println("  <table border=\"0\" width=\"1710\" class=\"mainTable\">");
    out.println("    <tr class=\"mainTable\">");
    out.println("      <td width=\"700\" valign=\"top\" class=\"mainTable\">");
    if (exception != null) {
      out.println("<pre>");
      exception.printStackTrace(out);
      out.println("</pre>");
    } else {
      try {
        if (dataModel.getLogicStepPrevious() == null) {
          out.println("<h1>CDSi by Open Immunization Software</h1>");
          out.println("<p>This forecaster steps the user through the CDSi framework.</p>");
        } else {
          dataModel.getLogicStepPrevious().printPost(out);
        }
      } catch (Exception e) {
        e.printStackTrace();
        out.println("<pre>");
        e.printStackTrace(out);
        out.println("</pre>");
      }
    }
    out.println("        <hr/>");
    out.println("      </td>");
    out.println("      <td width=\"1010\" valign=\"top\" class=\"mainTable\">");
    if (dataModel.getLogicStepPrevious() == null || dataModel.getLogicStep() == null) {
      out.println(
          "        <img src=\"i.png\" width=\"1000\" onclick=\"document.getElementById('stepForm').submit();\">");
    } else {
      out.println("        <img src=\"" + dataModel.getLogicStepPrevious().getLogicStepType().getName() + "-"
          + dataModel.getLogicStep().getLogicStepType().getName()
          + ".png\" width=\"1000\" onclick=\"document.getElementById('stepForm').submit();\">");
    }
    out.println("        <br/><input type=\"submit\" name=\"submit\" value=\"Next Step\"/>");
    out.println("        <select name=\"jumpTo\">");
    for (LogicStepType logicStepType : LogicStep.STEPS) {
      String display = logicStepType.getDisplay();
      if (logicStepType.isIndent()) {
        display = " + " + display;
      }
      if (dataModel.getLogicStep().getLogicStepType() == logicStepType) {
        out.println("          <option value=\"" + logicStepType.getName() + "\" selected>" + display + "</option>");
      } else {
        out.println("          <option value=\"" + logicStepType.getName() + "\">" + display + "</option>");
      }
    }
    out.println("        </select>");
    out.println("        <input type=\"submit\" name=\"submit\" value=\"Jump\"/>");
    out.println("        <input type=\"hidden\" name=\"action\" value=\"next\"/>");

    try {
      if (dataModel.getLogicStep() != null) {
        dataModel.getLogicStep().printPre(out);
      }
    } catch (Exception e) {
      e.printStackTrace();
      out.println("<pre>");
      e.printStackTrace(out);
      out.println("</pre>");
    }
    out.println("      </td>");
    out.println("    </tr>");
    out.println("  </table>");
    out.println("  </form>");
    out.println("  </body>");
    out.println("</html>");
    out.close();
  }

}
