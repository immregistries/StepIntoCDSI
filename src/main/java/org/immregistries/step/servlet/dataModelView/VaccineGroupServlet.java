package org.immregistries.step.servlet.dataModelView;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.immregistries.step.core.data.DataModel;
import org.immregistries.step.domain.Antigen;
import org.immregistries.step.domain.VaccineGroup;

/**
 * Created by Eric on 7/7/16.
 */
public class VaccineGroupServlet extends MainServlet {
  public static final String SERVLET_NAME = "dataModelViewVaccineGroup";
  private static final String PARAM_SEARCH = "search_term";

  public static String makeLink(VaccineGroup vaccineGroup) {
    return "<a href=\"" + SERVLET_NAME + "?" + PARAM_SEARCH + "=" + vaccineGroup.getName()
        + "\" target=\"dataModelView\">" + vaccineGroup.getName() + "</a>";
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

    String search_term = req.getParameter(PARAM_SEARCH);
    if (search_term == null) {
      search_term = "";
    }
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      printHeader(out, "Vaccine Group");

      out.println("    <form action=\"" + SERVLET_NAME + "\">");

      out.println("      <input type=\"text\" name=\"" + PARAM_SEARCH + "\"><br>");
      out.println("      <input type=\"submit\" value=\"Submit\">");

      out.println("    </form>");

      out.println("  <table>");
      out.println("   <tr>");
      out.println("      <th>Name</td>");
      out.println("      <th>Vaccine List</td>");
      out.println("      <th>Vaccine Group Forecast</td>");
      out.println("      <th>Administer Full Vaccine Group</td>");
      out.println("      <th>Antigen List</td>");
      out.println("   </tr>");

      if (search_term.length() >= 1) {
        VaccineGroup vaccineGroup = dataModel.getVaccineGroupMap().get(search_term);
        out.println("   <tr>");
        out.println("      <td>" + makeLink(vaccineGroup) + "</td>");
        out.println("      <td>" + vaccineGroup.getVaccineList() + "</td>");
        out.println("      <td>" + vaccineGroup.getVaccineGroupForecast() + "</td>");
        out.println("      <td>" + vaccineGroup.getAdministerFullVaccineGroup() + "</td>");
        out.println("      <td>" + vaccineGroup.getAntigenList() + "</td>");

        out.println("   </tr>");
      } else {
        for (VaccineGroup vaccineGroup : dataModel.getVaccineGroupMap().values()) {
          out.println("   <tr>");
          out.println("      <td>" + makeLink(vaccineGroup) + "</td>");
          out.println("      <td>" + vaccineGroup.getVaccineList() + "</td>");
          out.println("      <td>" + vaccineGroup.getVaccineGroupForecast() + "</td>");
          out.println("      <td>" + vaccineGroup.getAdministerFullVaccineGroup() + "</td>");
          out.println("      <td>");
          if (vaccineGroup.getAntigenList().size() > 0) {
            out.println("        <ul>");
            for (Antigen antigen : vaccineGroup.getAntigenList()) {
              out.println("          <li>" + AntigenServlet.makeLink(antigen) + "</li>");
            }
            out.println("        </ul>");
          } else {
            out.println("-");
          }
          out.println("      </td>");
          out.println("   </tr>");
        }
      }
      // Do stuff here .....
      out.println("  </table>");

      printFooter(out);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }

  }
}
