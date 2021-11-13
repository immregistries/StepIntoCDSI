package org.immregistries.step.servlet.dataModelView;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.immregistries.step.core.data.DataModel;
import org.immregistries.step.domain.LiveVirusConflict;
import org.immregistries.step.domain.VaccineType;

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
      printHeader(out, session, MiniMenuItem.LIVE_VIRUS_CONFLICT);
      out.println("   <table>");
      out.println("     <tr>");
      out.println("       <caption>Live Virus Conflict</caption>");
      out.println("       <th>Schedule</th>");
      out.println("       <th>Previous Vaccine Type</th>");
      out.println("       <th>Current Vaccine Type</th>");
      out.println("       <th>Conflict Begin Interval</th>");
      out.println("       <th>Minimal Conflict End Interval</th>");
      out.println("       <th>Conflict End Interval</th>");
      out.println("     </tr>");
      for (LiveVirusConflict liveVirusConflict : dataModel.getLiveVirusConflictList()) {
        printRowLiveVirusConflict(liveVirusConflict, out);
      }
      out.println("   </table>");
      printFooter(out, session);
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
