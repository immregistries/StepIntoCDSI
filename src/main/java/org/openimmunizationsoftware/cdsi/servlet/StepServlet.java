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

public class StepServlet extends ForecastServlet {
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

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
        out.println(
            "  <a href=\"step\"><img src=\"Logo Large.png\" height=\"120\" align=\"left\"/></a>");
        out.println("  <a href=\"dataModelView\" target=\"dataModelView\">View Data Model</a>");
        out.println("<br clear=\"all\"/>");
        if (dataModel.getLogicStepPrevious() == null) {
          out.println("<h1>CDSi Demonstration System</h1> ");
          if (req.getParameter(LogicStep.PARAM_EVAL_DATE) == null) {
            out.println(
                "<p>This application will take the user step-by-step through the CDSi logic. </p>");
            out.println("<h2>Acknowledgements</h2>");
            out.println("<ul>");
            out.println("  <li>Nathan Bunker</li>");
            out.println("  <li>Jordan Coleman</li>");
            out.println("  <li>Eric Ostrom</li>");
            out.println("  <li>Lee Grygla</li>");
            out.println("</ul>");
            out.println("<h2>Demonstration Links</h2>");
            out.println("<ul>");
            out.println(
                "  <li><a href=\"step?evalDate=20160630&scheduleName=default&resultFormat=text&patientDob=20150630&patientSex=F&vaccineDate1=20150826&vaccineCvx1=47&vaccineMvx1=&vaccineDate2=20151027&vaccineCvx2=47&vaccineMvx2=&vaccineDate3=20151229&vaccineCvx3=47&vaccineMvx3=\">Hib Example</a></li>");
            out.println(
                "  <li><a href=\"step?evalDate=20140515&scheduleName=default&resultFormat=text&patientDob=20051215&patientSex=M&vaccineDate1=20060213&vaccineCvx1=10&vaccineMvx1=&vaccineDate2=20060214&vaccineCvx2=100&vaccineMvx2=&vaccineDate3=20060420&vaccineCvx3=10&vaccineMvx3=&vaccineDate4=20060420&vaccineCvx4=20&vaccineMvx4=&vaccineDate5=20060420&vaccineCvx5=17&vaccineMvx5=&vaccineDate6=20060616&vaccineCvx6=17&vaccineMvx6=&vaccineDate7=20060616&vaccineCvx7=10&vaccineMvx7=&vaccineDate8=20060616&vaccineCvx8=08&vaccineMvx8=&vaccineDate9=20060616&vaccineCvx9=20&vaccineMvx9=&vaccineDate10=20060616&vaccineCvx10=100&vaccineMvx10=&vaccineDate11=20060929&vaccineCvx11=08&vaccineMvx11=&vaccineDate12=20060929&vaccineCvx12=100&vaccineMvx12=&vaccineDate13=20061213&vaccineCvx13=20&vaccineMvx13=&vaccineDate14=20061215&vaccineCvx14=08&vaccineMvx14=&vaccineDate15=20061215&vaccineCvx15=85&vaccineMvx15=&vaccineDate16=20061215&vaccineCvx16=03&vaccineMvx16=&vaccineDate17=20061215&vaccineCvx17=21&vaccineMvx17=&vaccineDate18=20071105&vaccineCvx18=17&vaccineMvx18=&vaccineDate19=20071105&vaccineCvx19=20&vaccineMvx19=&vaccineDate20=20071105&vaccineCvx20=10&vaccineMvx20=&vaccineDate21=20080110&vaccineCvx21=85&vaccineMvx21=&vaccineDate22=20080110&vaccineCvx22=100&vaccineMvx22=&vaccineDate23=20140515&vaccineCvx23=21&vaccineMvx23=&vaccineDate24=20140515&vaccineCvx24=139&vaccineMvx24=&vaccineDate25=20140515&vaccineCvx25=10&vaccineMvx25=&vaccineDate26=20140515&vaccineCvx26=03&vaccineMvx26=\">Complete Record</a></li>");
            out.println("</ul>");
          }
        } else {
          dataModel.getLogicStepPrevious().printPost(out);
          dataModel.getLogicStepPrevious().printLog(out);
        }
      } catch (Exception e) {
        e.printStackTrace();
        out.println("<pre>");
        e.printStackTrace(out);
        out.println("</pre>");
      }
    }
    out.println("      </td>");
    out.println("      <td width=\"1010\" valign=\"top\" class=\"mainTable\">");
    if (dataModel.getLogicStepPrevious() == null || dataModel.getLogicStep() == null) {
      out.println(
          "        <img src=\"i.png\" width=\"1000\" onclick=\"document.getElementById('stepForm').submit();\">");
    } else {
      out.println(
          "        <img src=\"" + dataModel.getLogicStepPrevious().getLogicStepType().getName()
              + "-" + dataModel.getLogicStep().getLogicStepType().getName()
              + ".png\" width=\"1000\" onclick=\"document.getElementById('stepForm').submit();\">");
    }
    if (dataModel.getLogicStepPrevious() != null
        || req.getParameter(LogicStep.PARAM_EVAL_DATE) != null) {
      out.println("        <br/><input type=\"submit\" name=\"submit\" value=\"Next Step\"/>");
      out.println("        <select name=\"jumpTo\">");
      for (LogicStepType logicStepType : LogicStep.STEPS) {
        String display = logicStepType.getDisplay();
        if (logicStepType.isIndent()) {
          display = " + " + display;
        }
        if (dataModel.getLogicStep().getLogicStepType() == logicStepType) {
          out.println("          <option value=\"" + logicStepType.getName() + "\" selected>"
              + display + "</option>");
        } else {
          out.println("          <option value=\"" + logicStepType.getName() + "\">" + display
              + "</option>");
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
