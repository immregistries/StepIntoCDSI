package org.openimmunizationsoftware.cdsi.servlet.dataModelView;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.ClinicalHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;
import org.openimmunizationsoftware.cdsi.servlet.ForecastServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by Eric on 7/7/16.
 */
public class ScheduleServlet extends MainServlet {

  public static final String SERVLET_NAME = "dataModelViewAntigen";

  /*
   * public static String makeLink(Schedule schedule) { return "<a href=\"" +
   * SERVLET_NAME + "?\" target=\"dataModelView\">" "</a>"; }
   */

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    HttpSession session = req.getSession(true);
    DataModel dataModel = (DataModel) session.getAttribute("dataModel");
    if (dataModel == null) {
      return;
    }

    resp.setContentType("text/html");

    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      printHeader(out, "Schedule");
      printSchedule(dataModel, out);
      printFooter(out);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }
  }

  private void printSchedule(DataModel dataModel, PrintWriter out) {
    out.println("    <table>");
    out.println("      <tr>");
    out.println("        <caption>Schedule</caption>");
    out.println("        <th>Schedule Name</th>");
    out.println("        <th>Contraindication List</th>");
    out.println("        <th>Live Virus Conflict List</th>");
    out.println("        <th>Antigen Series List</th>");
    out.println("        <th>Immunity</th>");

    out.println("      </tr>");
    for (Schedule schedule : dataModel.getScheduleList()) {
      printRowSchedule(schedule, out);
    }
    out.println("    </table>");

  }

  private void printRowSchedule(Schedule schedule, PrintWriter out) {
    out.println("      <tr>");
    out.println("        <td>" + schedule.getScheduleName() + "</td>");
    out.println("        <td>" + schedule.getContraindicationList() + "</td>");
    out.println("        <td>" + schedule.getLiveVirusConflictList() + "</td>");
    out.println("        <td>");
    out.println("          <ul>");

    for (AntigenSeries antigenSeries : schedule.getAntigenSeriesList()) {
      out.println("       <li>" + antigenSeries.getSeriesName() + "</li>");
    }
    out.println("          </ul>");

    out.println("        </td>");
    out.println("        <td>");
    if (schedule.getImmunity() != null) {
      if (schedule.getImmunity().getClinicalHistoryList().size() > 0) {
        out.println("          <ul>");

        for (ClinicalHistory clinicalHistory : schedule.getImmunity().getClinicalHistoryList()) {
          out.println("       <li>" + clinicalHistory.getImmunityGuideline() + "</li>");
        }
        out.println("          </ul>");
      }
      out.println("        </td>");
      out.println("      </tr>");
    }
  }

}
