package org.openimmunizationsoftware.cdsi.servlet;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by Eric on 7/7/16.
 */
public class DataModelViewScheduleServlet extends ForecastServlet {


    public static final String SERVLET_NAME = "dataModelViewAntigen";

/*    public static String makeLink(Antigen antigen)
    {
        return "<a href=\"" + SERVLET_NAME + "?\" target=\"dataModelView\">"  "</a>";
    }*/

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
            out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
            out.println("<html>");
            out.println("  <head>");
            out.println("    <title>CDSi - Data Model View - Antigen</title>");
            out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"index.css\">");
            out.println("  </head>");
            out.println("  <body>");
            printSchedule(dataModel, out);
            out.println("  </body>");
            out.println("</html>");
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
        out.println("        <th>Immunity List</th>");

        out.println("      </tr>");
        for (Schedule schedule: dataModel.getScheduleList()) {
                printRowSchedule(schedule, out);
            }
        out.println("    </table>");


    }

    private void printRowSchedule(Schedule schedule, PrintWriter out) {
        out.println("      <tr>");
        out.println("        <td>" + schedule.getScheduleName() + "</td>");
        out.println("        <td>" + schedule.getContraindicationList() + "</td>");
        out.println("        <td>" + schedule.getLiveVirusConflictList() + "</td>");
        out.println("        <td>" + schedule.getAntigenSeriesList() + "</td>");
        out.println("        <td>" + schedule.getImmunity() + "</td>");
        out.println("      </tr>");

    }
}
