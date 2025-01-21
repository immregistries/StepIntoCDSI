package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class ClearServlet extends HttpServlet {

    private class ClearEntry {
        Date month;
        String iisName;
        int countUpdate;
        int countQuery;
    }

    private static Map<String, Map<Date, ClearEntry>> clearIisMap = new HashMap<String, Map<Date, ClearEntry>>();

    static {

    }

    private static Map<String, Integer> populationMap = new HashMap<String, Integer>();

    static {
        populationMap.put("AL", 50);
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
        try {
            printHeader(out, "Patient");

            printFooter(out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    private void printViewPatient(DataModel dataModel, PrintWriter out) {
        // class="w3-table w3-bordered w3-striped w3-border test w3-hoverable"
        out.println("  <div class=\"w3-card w3-cell w3-margin\">");
        out.println("    <header class=\"w3-container w3-khaki\">");
        out.println("      <h2>Patient</h2>");
        out.println("    </header>");
        out.println("    <div class=\"w3-container\">");
        out.println("      <table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable w3-margin\">");
        out.println("        <caption>Demographics</caption>");
        out.println("        <tr>");
        out.println("          <th>Patient DOB</th>");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        out.println("          <td>" + sdf.format(dataModel.getPatient().getDateOfBirth()) + "</td>");
        out.println("        </tr>");

        out.println("        <tr>");
        out.println("          <th>Gender</th>");
        out.println("          <td>" + dataModel.getPatient().getGender() + "</td>");
        out.println("        </tr>");

        out.println("        <tr>");
        out.println("          <th>Country of Birth</th>");
        out.println("          <td>" + dataModel.getPatient().getCountryOfBirth() + "</td>");
        out.println("        </tr>");
        out.println("      </table>");
        out.println("    </div>");
        out.println("  </div>");

    }

    protected void printHeader(PrintWriter out, String section) {
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
        out.println("<html>");
        out.println("  <head>");
        out.println("    <title>CLEAR - Community Led Exchange and Aggregate Reporting</title>");
        out.println("    <link rel=\"stylesheet\" href=\"https://www.w3schools.com/w3css/4/w3.css\"/>");
        out.println("  </head>");
        out.println("  <body>");

        out.println("    <header class=\"w3-container w3-light-grey\">");
        out.println("      <div class=\"w3-bar w3-light-grey\">");
        out.println("        <a href=\"dataModelView\" class=\"w3-bar-item w3-button\">Main</a> ");
        out.println("        <a href=\"dataModelViewAntigen\" class=\"w3-bar-item w3-button\">Antigen</a> ");
        out.println("        <a href=\"dataModelViewCvx\" class=\"w3-bar-item w3-button\">CVX</a> ");
        out.println(
                "        <a href=\"dataModelViewLiveVirusConflict\" class=\"w3-bar-item w3-button\">Live Virus Conflict</a> ");
        out.println("        <a href=\"dataModelViewPatient\" class=\"w3-bar-item w3-button\">Patient</a> ");
        out.println("        <a href=\"dataModelViewSchedule\" class=\"w3-bar-item w3-button\">Schedule</a> ");
        out.println("        <a href=\"dataModelViewVaccineGroup\" class=\"w3-bar-item w3-button\">Vaccine Group</a> ");
        out.println("      </div>");
        out.println("    </header>");
        out.println("    <div class=\"w3-container\">");
    }

    protected void printFooter(PrintWriter out) {
        out.println("  </div>");
        out.println("  <div class=\"w3-container w3-green\">");
        out.println("      <p>Step Into CDSi " + SoftwareVersion.VERSION + " - ");
        out.println(
                "      <a href=\"https://aira.memberclicks.net/assets/docs/Organizational_Docs/AIRA%20Privacy%20Policy%20-%20Final%202024_.pdf\" class=\"underline\">AIRA Privacy Policy</a> - ");
        out.println(
                "      <a href=\"https://aira.memberclicks.net/assets/docs/Organizational_Docs/AIRA%20Terms%20of%20Use%20-%20Final%202024_.pdf\" class=\"underline\">AIRA Terms and Conditions of Use</a></p>");
        out.println("    </div>");
        out.println("  </body>");
        out.println("</html>");
    }

}
