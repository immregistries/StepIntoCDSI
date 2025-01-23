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
import org.openimmunizationsoftware.cdsi.servlet.maps.Color;
import org.openimmunizationsoftware.cdsi.servlet.maps.MapEntityMaker;
import org.openimmunizationsoftware.cdsi.servlet.maps.MapPlace;

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
        populationMap.put("AL", 5025369);
        populationMap.put("AK", 733395);
        populationMap.put("AZ", 7158110);
        populationMap.put("AR", 3011553);
        populationMap.put("CA", 39555674);
        populationMap.put("CO", 5775324);
        populationMap.put("CT", 3607701);
        populationMap.put("DE", 989955);
        populationMap.put("DC", 689545);
        populationMap.put("FL", 21538192);
        populationMap.put("GA", 10713755);
        populationMap.put("HI", 1455252);
        populationMap.put("ID", 1839140);
        populationMap.put("IL", 12821814);
        populationMap.put("IN", 6786587);
        populationMap.put("IA", 3190546);
        populationMap.put("KS", 2937745);
        populationMap.put("KY", 4506302);
        populationMap.put("LA", 4657874);
        populationMap.put("ME", 1363196);
        populationMap.put("MD", 6181629);
        populationMap.put("MA", 7033132);
        populationMap.put("MI", 10079338);
        populationMap.put("MN", 5706692);
        populationMap.put("MS", 2961278);
        populationMap.put("MO", 6154854);
        populationMap.put("MT", 1084216);
        populationMap.put("NE", 1961996);
        populationMap.put("NV", 3105595);
        populationMap.put("NH", 1377546);
        populationMap.put("NJ", 9289014);
        populationMap.put("NM", 2117555);
        populationMap.put("NY", 20203772);
        populationMap.put("NC", 10441499);
        populationMap.put("ND", 779046);
        populationMap.put("OH", 11799453);
        populationMap.put("OK", 3959405);
        populationMap.put("OR", 4237224);
        populationMap.put("PA", 13002909);
        populationMap.put("RI", 1097354);
        populationMap.put("SC", 5118252);
        populationMap.put("SD", 886729);
        populationMap.put("TN", 6912347);
        populationMap.put("TX", 29149458);
        populationMap.put("UT", 3271608);
        populationMap.put("VT", 643082);
        populationMap.put("VA", 8631388);
        populationMap.put("WA", 7707586);
        populationMap.put("WV", 1793736);
        populationMap.put("WI", 5894170);
        populationMap.put("WY", 576844);
        populationMap.put("PR", 3285874);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html");
        System.out.println("--> calling doGet");

        PrintWriter out = new PrintWriter(resp.getOutputStream());
        try {
            System.out.println("--> printing header");
            printHeader(out, "Patient");
            try {
                MapEntityMaker mapEntityMaker = new MapEntityMaker();
                for (String testParticipant : populationMap.keySet()) {
                    int population = populationMap.get(testParticipant);
                    MapPlace mapPlace = new MapPlace(testParticipant);
                    mapPlace.setFillerColor(Color.MAP_SELECTED);
                    mapEntityMaker.addMapPlace(mapPlace);
                }
                mapEntityMaker.setMapTitle("Hello World");
                mapEntityMaker.setStatusTitle("Population");
                mapEntityMaker.printMapWithKey(out);
            } catch (Exception e) {
                e.printStackTrace(out);
            }
            System.out.println("--> printing footer");
            printFooter(out);
        } catch (Exception e) {
            System.out.println("--> exception!");
            e.printStackTrace();
        } finally {
            out.close();
            System.out.println("--> finished doGet");
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
