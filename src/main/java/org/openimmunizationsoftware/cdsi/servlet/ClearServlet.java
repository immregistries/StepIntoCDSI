package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.servlet.maps.ClearEntry;
import org.openimmunizationsoftware.cdsi.servlet.maps.Color;
import org.openimmunizationsoftware.cdsi.servlet.maps.MapEntityMaker;
import org.openimmunizationsoftware.cdsi.servlet.maps.MapPlace;

public class ClearServlet extends HttpServlet {

    String userIisName = "AZ";
    Calendar viewMonth = Calendar.getInstance();
    private static Map<String, Map<String, ClearEntry>> clearIisMap = new HashMap<String, Map<String, ClearEntry>>();

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
        populationMap.put("NYC", 8804190);
        populationMap.put("Phil", 1526006);
        populationMap.put("American Samoa", 49710);
        populationMap.put("Guam", 168801);
        populationMap.put("Marshall Islands", 42418);
        populationMap.put("Micronesia", 113373);
        populationMap.put("N. Mariana Islands (CNMI)", 43854);
        populationMap.put("Palau", 21779);
        populationMap.put("Puerto Rico", 3238164);
        populationMap.put("Virgin Islands", 87146);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
                
        SimpleDateFormat sdfMonthYear = new SimpleDateFormat("MMMM YYYY");
        resp.setContentType("text/html");
        System.out.println("--> calling doGet");

        PrintWriter out = new PrintWriter(resp.getOutputStream());
        try {
            System.out.println("--> printing header");
            printHeader(out);

            for(String user : populationMap.keySet()) {
                ClearEntry newEntry = new ClearEntry();
                Random rand = new Random();
                int userPopulation = populationMap.get(user);
                newEntry.setCountUpdate((int)Math.round(rand.nextFloat()*(userPopulation/2.0)+(userPopulation/2.0)));
                newEntry.setCountQuery((int)Math.round(rand.nextFloat()*(userPopulation/2.0)+(userPopulation/2.0)));
                Map<String, ClearEntry> clearEntryDateMap = clearIisMap.get(user) == null ? new HashMap<String, ClearEntry>() : clearIisMap.get(user);
                clearIisMap.put(user,clearEntryDateMap);
                clearEntryDateMap.put(sdfMonthYear.format(viewMonth.getTime()),newEntry);
            }

            {
                Calendar tmpCalendar = Calendar.getInstance();
                tmpCalendar.add(Calendar.YEAR, -2);
                for(int i = 0; i < 25; i++) {
                    SimpleDateFormat sdfRowName = new SimpleDateFormat("MMMMYYYY");
                    tmpCalendar.add(Calendar.MONTH, 1);

                    String rowName = sdfRowName.format(tmpCalendar.getTime());
                    String updateCountString = req.getParameter(rowName + "-Updates");
                    String queryCountString = req.getParameter(rowName + "-Queries");

                    if(updateCountString == null || updateCountString == "") {
                        continue;
                    }
                    if(queryCountString == null || queryCountString == "") {
                        continue;
                    }
                    int updateCount = Integer.parseInt(updateCountString);
                    int queryCount = Integer.parseInt(queryCountString);

                    ClearEntry newEntry = new ClearEntry();
                    newEntry.setCountUpdate(updateCount);
                    newEntry.setCountQuery(queryCount);
                    Map<String, ClearEntry> clearEntryDateMap = clearIisMap.get(userIisName) == null ? new HashMap<String, ClearEntry>() : clearIisMap.get(userIisName);
                    clearIisMap.put(userIisName,clearEntryDateMap);
                    clearEntryDateMap.put(sdfMonthYear.format(tmpCalendar.getTime()),newEntry);
                }
            }
            
            out.println("<h1> " + userIisName + " IIS</h3>");
            out.println("<form>");
            out.println("   <table class=\"w3-table w3-striped\">");
            out.println("      <tr>");
            out.println("          <th>Month</th>");
            out.println("          <th>Updates</th>");
            out.println("          <th>Queries</th>");
            out.println("      </tr>");
            {
                Calendar tmpCalendar = Calendar.getInstance();
                tmpCalendar.add(Calendar.YEAR, -2);
                for(int i = 0; i < 25; i++) {
                    SimpleDateFormat sdfRowName = new SimpleDateFormat("MMMMYYYY");
                    String rowName = sdfRowName.format(tmpCalendar.getTime());
                    out.println("      <tr>");
                    out.println("           <td>" + sdfMonthYear.format(tmpCalendar.getTime()) + "</td>");
                    out.println("           <td><input type=\"number\" name=\"" + rowName + "-Updates" + "\"></td>");
                    out.println("           <td><input type=\"number\" name=\"" + rowName + "-Queries" + "\"></td>");
                    out.println("      </tr>");
                    tmpCalendar.add(Calendar.MONTH, 1);
                }
            }
            out.println("   </table>");
            out.println("   <input class=\"w3-button\" type=\"submit\" value=\"Submit\">");
            out.println("</form>");

            //get highest and lowest test participant numbers
            int highestUpdateCount = -1;
            int lowestUpdateCount = -1;
            
            for(String user : clearIisMap.keySet()) {
                if(clearIisMap.get(user) == null) {
                    continue;
                }
                for (ClearEntry clearEntry : clearIisMap.get(user).values()) {
                    if(clearEntry == null) {
                        continue;
                    }
                    int updateCount = clearEntry.getCountUpdate();
                    if(updateCount > highestUpdateCount || highestUpdateCount == -1) {
                        highestUpdateCount = updateCount;
                    }
                    if(updateCount < lowestUpdateCount || lowestUpdateCount == -1) {
                        lowestUpdateCount = updateCount;
                    }
                }
            }

            int lowerBorder = (highestUpdateCount - lowestUpdateCount)/3;
            int upperBorder = lowerBorder*2;

            if(highestUpdateCount == -1 || lowestUpdateCount == -1) {
                upperBorder = 0;
                lowerBorder = 0;
            }
            out.println("<p> highest update count: " + highestUpdateCount + "</p>");
            out.println("<p> lowest update count: " + lowestUpdateCount + "</p>");

            try {
                MapEntityMaker mapEntityMaker = new MapEntityMaker();
                for (String testParticipant : clearIisMap.keySet()) {
                    ClearEntry ce = clearIisMap.get(testParticipant).get(sdfMonthYear.format(viewMonth.getTime()));
                    if(ce == null) {
                        out.println("<p>ce is null!</p>");
                        continue;
                    }
                    int displayCount = ce.getCountUpdate();
                    
                    MapPlace mapPlace = new MapPlace(testParticipant);

                    mapPlace.setFillerColor(Color.DEFAULT);
                    if(displayCount < lowerBorder) {
                        mapPlace.setFillerColor(Color.MAP_LOWER);
                    } else if(displayCount > upperBorder) {
                        mapPlace.setFillerColor(Color.MAP_UPPER);
                    } else {
                        mapPlace.setFillerColor(Color.MAP_CENTER);
                    }
                    
                    mapEntityMaker.addMapPlace(mapPlace);
                }
                mapEntityMaker.setMapTitle("Map");
                mapEntityMaker.setStatusTitle("Population");
                mapEntityMaker.printMapWithKey(out);
            } catch (Exception e) {
                e.printStackTrace(out);
            }

            
            out.println("<input id=\"updatesRadio\" class=\"w3-button\" type=\"radio\" name=\"display_type\">");
            out.println("<label for=\"updatesRadio\">Updates</label>");
            out.println("<input id=\"queriesRadio\" class=\"w3-button\" type=\"radio\" name=\"display_type\">");
            out.println("<label for=\"queriesRadio\">Queries</label>");

            out.println("<div class=\"w3-cell-row\">");
            out.println("   <div class=\"w3-cell\">");
            out.println("       <input class=\"w3-button\" type=\"button\" value=\"<-\">");
            out.println("   </div>");
            out.println("   <div class=\"w3-cell\">");
            out.println("       <p>" + sdfMonthYear.format(viewMonth.getTime()) + "</p>");
            out.println("   </div>");
            out.println("   <div class=\"w3-cell\">");
            out.println("       <input class=\"w3-button\" type=\"button\" value=\"->\">");
            out.println("   </div>");
            out.println("</div>");

            out.println("   <table class=\"w3-table w3-striped\">");
            out.println("      <tr>");
            out.println("          <th>User</th>");
            out.println("          <th>Updates</th>");
            out.println("          <th>Queries</th>");
            out.println("      </tr>");
            for (String testParticipant : populationMap.keySet()) {
                int updateCount = 0;
                int queryCount = 0;
                
                Map<String, ClearEntry> userEntry = clearIisMap.get(testParticipant);
                if(userEntry != null) {
                    ClearEntry monthEntry = userEntry.get(sdfMonthYear.format(viewMonth.getTime()));
                    if(monthEntry != null) {
                        updateCount = monthEntry.getCountUpdate();
                        queryCount = monthEntry.getCountQuery();
                    }
                }
                out.println("      <tr>");
                out.println("           <td>" + testParticipant + "</td>");
                out.println("           <td><p>" + updateCount + "</p></td>");
                out.println("           <td><p>" + queryCount + "</p></td>");
                out.println("      </tr>");
            }
            out.println("   </table>");

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

    protected void printHeader(PrintWriter out) {
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
        out.println("<html>");
        out.println("  <head>");
        out.println("    <title>CLEAR - Community Led Exchange and Aggregate Reporting</title>");
        out.println("    <link rel=\"stylesheet\" href=\"https://www.w3schools.com/w3css/4/w3.css\"/>");
        out.println("  </head>");
        out.println("  <body>");

        out.println("    <header class=\"w3-container w3-light-grey\">");
        out.println("      <div class=\"w3-bar w3-light-grey\">");
        out.println("        <h1>CLEAR - Community Led Exchange and Aggregate Reporting</h1> ");
        out.println("        <a href=\"\" class=\"w3-bar-item w3-button\">Main</a> ");
        out.println("        <a href=\"clear/map\" class=\"w3-bar-item w3-button\">Map</a> ");
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
