package org.openimmunizationsoftware.cdsi.servlet.dataModelView;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;

public class CvxServlet extends MainServlet {

  public static final String SERVLET_NAME = "dataModelViewCvx";

  private static final String PARAM_SEARCH = "search_term";

  public static String makeLink(VaccineType vaccineType) {
    return "<a href=\"" + SERVLET_NAME + "?" + PARAM_SEARCH + "=" + vaccineType.getCvxCode()
        + "\" target=\"dataModelView\">" + vaccineType.getShortDescription() + "</a>";
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    HttpSession session = req.getSession(true);
    DataModel dataModel = (DataModel) session.getAttribute("dataModel");
    if (dataModel == null) {
      return;
    }

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());

    String search_term = req.getParameter(PARAM_SEARCH);
    if (search_term == null) {
      search_term = "";
    }



    try {
      printHeader(out, "CVX");

      out.println("    <form action=\"" + SERVLET_NAME + "\">");

      out.println("      <input type=\"text\" name=\"" + PARAM_SEARCH + "\"><br>");
      out.println("      <input type=\"submit\" value=\"Submit\">");

      out.println("    </form>");

      out.println("  <div class=\"w3-card w3-cell w3-margin\">");
      out.println("    <header class=\"w3-container w3-khaki\">");
      out.println("      <h2>CVX</h2>");
      out.println("    </header>");
      out.println("    <div class=\"w3-container\">");
      out.println("      <table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
      out.println("        <caption>Vaccine Dose Administered</caption>");
      out.println("        <tr>");
      out.println("          <th>Code</th>");
      out.println("          <th>Short Description</th>");
      out.println("        </tr>");
      for (VaccineDoseAdministered vaccineDoseAdministered : dataModel.getPatient()
          .getReceivesList()) {
        if (search_term.length() >= 1) {
          VaccineType vaccineType = dataModel.getCvxMap().get(search_term);
          out.println("   <tr>");
          out.println("      <td>" + vaccineType.getCvxCode() + "</td>");
          out.println("      <td>" + makeLink(vaccineType) + "</td>");
          out.println("   </tr>");
        } else {
          for (VaccineType vaccineType : dataModel.getCvxMap().values()) {
            out.println("   <tr>");
            out.println("      <td>" + vaccineType.getCvxCode() + "</td>");
            out.println("      <td>" + makeLink(vaccineType) + "</td>");
            out.println("   </tr>");
          }
        }
      }
      out.println("      </table>");
      out.println("    </div>");
      out.println("  </div>");

      printFooter(out);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }
  }
}
