package org.immregistries.step.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.immregistries.step.SoftwareVersion;
import org.immregistries.step.core.data.DataModel;
import org.immregistries.step.core.data.DataModelLoader;
import org.immregistries.step.core.logic.LogicStepFactory;
import org.immregistries.step.core.logic.LogicStepType;


@SuppressWarnings("serial")
public class HomeServlet extends HttpServlet {

  public static final String PARAM_SHOW = "show";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      {
        doHeader(out, session);
        String show = req.getParameter(PARAM_SHOW);
        out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
        if (show == null) {
          out.println(
              "    <div class=\"w3-panel w3-yellow\"><p class=\"w3-left-align\">This system is for test purposes only. "
                  + "Do not submit production data. This system is not secured for safely holding personally identifiable heath information.  </p></div>");
          out.println("    <p>Demonstrates step-by-step how to generate an immunization forecast and evaluation. </p>");
          out.println("    <h2>Primary Functions Supported</h2>");
          out.println("    <ul class=\"w3-ul w3-hoverable\">");
          out.println("      <li><a href=\"home\">Home</a>: Home Page.</li>");
          out.println("    </ul>");
        }
        out.println("  </div>");
        out.println(
            "  <img src=\"images/ibrahim-rifath-tPkd1GprSWE-unsplash.jpg\" class=\"w3-round\" alt=\"Step Into Immunization Forecasting\" width=\"400\">");
        out.println(
            "Photo by <a href=\"https://unsplash.com/@photoripey?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText\">Ibrahim Rifath</a> on <a href=\"https://unsplash.com/s/photos/step?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText\">Unsplash</a>\r\n" + 
            "  ");  
        doFooter(out, session);
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    out.flush();
    out.close();
  }

  public static void doHeader(PrintWriter out, HttpSession session) {
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>Step Into Immunization Forecasting</title>");
    out.println("    <link rel=\"stylesheet\" href=\"https://www.w3schools.com/w3css/4/w3.css\"/>");
    out.println("  </head>");
    out.println("  <body>");
    out.println("    <header class=\"w3-container w3-light-grey\">");
    out.println("      <div class=\"w3-bar w3-light-grey\">");
    out.println(
        "        <a href=\"home\" class=\"w3-bar-item w3-button w3-green\">Home</a>");
    out.println("        <a href=\"step\" class=\"w3-bar-item w3-button\">Step</a>");
    out.println("        <a href=\"forecast\" class=\"w3-bar-item w3-button\">Forecast</a>");
    out.println("        <a href=\"dataModelView\" class=\"w3-bar-item w3-button\">Model</a>");
    out.println("      </div>");
    out.println("    </header>");
    out.println("    <div class=\"w3-container\">");

  }

  public static void doFooter(PrintWriter out, HttpSession session) {
    out.println("  </div>");
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    out.println("  <div class=\"w3-container w3-green\">");
    out.println("    <p>Step Into Immunization Forecasting v" + SoftwareVersion.VERSION + " - Current Time "
        + sdf.format(System.currentTimeMillis()) + "</p>");
    out.println("  </div>");
    out.println("  </body>");
    out.println("</html>");
  }
  
  protected DataModel readRequest(HttpServletRequest req) throws Exception {
    DataModel dataModel = DataModelLoader.createDataModel();
    dataModel.setRequest(req);
    dataModel.setNextLogicStep(
        LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel));
    return dataModel;
  }

}
