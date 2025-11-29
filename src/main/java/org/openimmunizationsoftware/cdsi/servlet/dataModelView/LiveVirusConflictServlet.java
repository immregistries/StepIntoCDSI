package org.openimmunizationsoftware.cdsi.servlet.dataModelView;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;

public class LiveVirusConflictServlet extends MainServlet {

  public static final String SERVLET_NAME = "dataModelViewLiveVirusConflict";

  private static final String PARAM_SEARCH = "search_term";

  public static String makeLink(VaccineType vaccineType) {
    return ""; // "<a href=\"" + SERVLET_NAME + "?" + PARAM_SEARCH + "=" +
               // vaccineType.getCvxCode() + "\" target=\"dataModelView\">" +
               // vaccineType.getShortDescription() + "</a>";
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
      printHeader(out, "Live Virus Conflict");

      out.println("  <div class=\"w3-card w3-cell w3-margin\">");
      out.println("    <header class=\"w3-container w3-khaki\">");
      out.println("      <h2>Live Virus Conflict</h2>");
      out.println("    </header>");
      out.println("    <div class=\"w3-container\">");
      out.println("      <table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
      out.println("        <tr>");
      out.println("       <th>Schedule</th>");
      out.println("       <th>Previous Vaccine Type</th>");
      out.println("       <th>Current Vaccine Type</th>");
      out.println("       <th>Conflict Begin Interval</th>");
      out.println("       <th>Minimal Conflict End Interval</th>");
      out.println("       <th>Conflict End Interval</th>");
      out.println("        </tr>");
      for (LiveVirusConflict liveVirusConflict : dataModel.getLiveVirusConflictList()) {
        printRowLiveVirusConflict(liveVirusConflict, out);
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


  private void printRowLiveVirusConflict(LiveVirusConflict liveVirusConflict, PrintWriter out) {
    out.println("     <tr>");
    out.println("       <td>" + n(liveVirusConflict.getSchedule()) + "</td>");
    out.println(
        "       <td>" + CvxServlet.makeLink(liveVirusConflict.getPreviousVaccineType()) + "</td>");
    out.println(
        "       <td>" + CvxServlet.makeLink(liveVirusConflict.getCurrentVaccineType()) + "</td>");
    out.println("       <td>" + liveVirusConflict.getConflictBeginInterval() + "</td>");
    out.println("       <td>" + liveVirusConflict.getMinimalConflictEndInterval() + "</td>");
    out.println("       <td>" + liveVirusConflict.getConflictEndInterval() + "</td>");
    out.println("     </tr>");
  }
}
