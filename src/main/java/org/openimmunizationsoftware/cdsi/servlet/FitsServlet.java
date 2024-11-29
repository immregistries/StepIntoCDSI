package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupStatus;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepFactory;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;
import org.openimmunizationsoftware.cdsi.servlet.fits.FitsManager;
import org.openimmunizationsoftware.cdsi.servlet.fits.TestCaseRegistered;

import gov.nist.healthcare.cds.domain.TestPlan;
import gov.nist.healthcare.cds.enumeration.Gender;
import gov.nist.healthcare.cds.enumeration.SerieStatus;

public class FitsServlet extends ForecastServlet {

    private static FitsManager fitsManager = null;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

    private static String[][] equivalentCvx = new String[][] { { "85", "52" }, { "107", "112" },
            { "45", "08", "189", "43" }, { "163", "164", "162" }, { "137", "165" }, { "48", "17" }, { "07", "03" },
            { "109", "152", "133" }, { "188", "187" }, { "108", "147" } };

    private static boolean isSameVaccineCvx(String cvx1, String cvx2) {
        if (cvx1 == null || cvx2 == null) {
            return false;
        }
        if (cvx1.equals(cvx2)) {
            return true;
        }
        for (String[] cvxs : equivalentCvx) {
            for (String cvx : cvxs) {
                if (cvx.equals(cvx1)) {
                    for (String cvx3 : cvxs) {
                        if (cvx3.equals(cvx2)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(true);
        String action = req.getParameter("action");
        Exception exception = null;
        String testPlanIdSelected = req.getParameter("testPlanId");
        String groupNameSelected = req.getParameter("groupName");

        if (action != null) {
            if (action.equals("Refresh")) {
                try {
                    fitsManager = new FitsManager();
                    fitsManager.setUsername(req.getParameter("username"));
                    fitsManager.setUrl(req.getParameter("url"));
                    fitsManager.setPassword(req.getParameter("password"));
                    fitsManager.init();
                } catch (Exception e) {
                    exception = e;
                    e.printStackTrace();
                }
            } else if (action.equals("Run") && fitsManager != null) {
                // get map of TestCaseRegistered for testPlanIdSelected and groupNameSelected
                Map<String, TestCaseRegistered> testCaseMap = fitsManager.getGroupTestCaseMap(testPlanIdSelected)
                        .get(groupNameSelected);
                if (testCaseMap != null) {
                    logToOut("Running forecaster for " + testPlanIdSelected + " - " + groupNameSelected);
                    for (TestCaseRegistered testCaseRegistered : testCaseMap.values()) {
                        try {
                            String link = createLink(testCaseRegistered);
                            String linkStep = "step" + link;
                            String linkForecast = "forecast" + link;
                            logToOut(" - Running " + testCaseRegistered.getTestCase().getUid());
                            logToOut("   - " + linkStep);
                            logToOut("   - " + linkForecast);
                            DataModel dataModel = DataModelLoader.createDataModel();
                            // setup data model
                            dataModel.setTestCaseRegistered(testCaseRegistered);
                            LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel);
                            dataModel.setNextLogicStep(
                                    LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel));
                            process(dataModel);
                            List<VaccineGroupForecast> vaccineGroupForecastList = dataModel
                                    .getVaccineGroupForecastList();
                            if (vaccineGroupForecastList != null) {
                                for (TestCaseRegistered.Forecast forecast : testCaseRegistered.getForecastList()) {
                                    for (VaccineGroupForecast vgf : vaccineGroupForecastList) {
                                        String expCvx = forecast.getVaccineCvxExp();
                                        String actCvx = vgf.getAntigen().getCvxForForecast();
                                        logToOut("     - Forecasing for: " + vgf.getAntigen().getName() + " (" + actCvx
                                                + ") " + vgf.getVaccineGroupStatus());
                                        if (isSameVaccineCvx(expCvx, actCvx)) {
                                            VaccineGroupStatus vaccineGroupStatus = vgf.getVaccineGroupStatus();
                                            SerieStatus serieStatus = vaccineGroupStatus.getSerieStatus();
                                            forecast.setSerieStatusAct(serieStatus);
                                            logToOut(
                                                    "       - This is the one we are looking for, setting series status to "
                                                            + serieStatus);
                                            if (vgf.getVaccineGroupStatus() == VaccineGroupStatus.NOT_COMPLETE) {
                                                forecast.setEarliestAct(vgf.getEarliestDate());
                                                forecast.setRecommendedAct(vgf.getAdjustedRecommendedDate());
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            testCaseRegistered.setException(e);
                        }
                    }
                }
            }
        }

        String urlString = setupUrlString(req);
        String usernameString = setupUsernameString(req);

        resp.setContentType("text/html");
        PrintWriter out = new PrintWriter(resp.getOutputStream());
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
        out.println("<html>");
        out.println("  <head>");
        out.println("    <title>CDSi - FITS</title>");
        out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"index.css\">");
        out.println("  </head>");
        out.println("  <body>");

        if (exception != null) {
            out.println("    <h1>Exception</h1>");
            out.println("    <p>" + exception.getMessage() + "</p>");
            out.println("<pre>");
            exception.printStackTrace(out);
            out.println("</pre>");
        }

        // username, url, and password collection fields
        if (fitsManager != null) {
            out.println("    <h1>Connected to FITS</h1>");
            out.println("  <form action=\"fits\" method=\"GET\" id=\"fitsFormSelect\">");
            // drop down to show selected test plan and be able to change
            out.println("    <label for=\"testPlanId\">Test Plan:</label>");
            out.println("    <select id=\"testPlanId\" name=\"testPlanId\">");
            for (TestPlan testPlan : fitsManager.getTestPlanList()) {
                out.println("      <option value=\"" + testPlan.getId() + "\""
                        + ((testPlanIdSelected != null && testPlan.getId().equals(testPlanIdSelected)) ? " selected"
                                : "")
                        + ">" + testPlan.getName() + "</option>");
            }
            out.println("    </select>");
            out.println("    <label for=\"groupName\">Group Name:</label>");
            out.println("    <select id=\"groupName\" name=\"groupName\">");
            for (String groupName : fitsManager.getGroupNames()) {
                out.println("      <option value=\"" + groupName + "\""
                        + ((groupNameSelected != null && groupName.equals(groupNameSelected)) ? " selected" : "")
                        + ">" + groupName + "</option>");
            }
            out.println("    </select>");
            // submit option saying "select"
            out.println("    <input type=\"Submit\" name=\"action\" value=\"Select\">");
            out.println("    <input type=\"Submit\" name=\"action\" value=\"Run\">");
            out.println("  </form>");

            for (TestPlan testPlan : fitsManager.getTestPlanList()) {
                if (testPlanIdSelected == null || !testPlan.getId().equals(testPlanIdSelected)) {
                    continue;
                }
                out.println("<h2>" + testPlan.getName() + "</h2>");
                Map<String, Map<String, TestCaseRegistered>> groupTestCaseMap = fitsManager
                        .getGroupTestCaseMap(testPlan.getId());
                for (String groupName : groupTestCaseMap.keySet()) {
                    if (groupNameSelected == null || !groupName.equals(groupNameSelected)) {
                        continue;
                    }
                    out.println("    <h3>" + groupName + "</h3>");
                    out.println("    <table>");
                    out.println("      <tr>");
                    out.println("        <th>Test Case</th>");
                    out.println("        <th>Title</th>");
                    out.println("        <th>Action</th>");
                    out.println("        <th>Vaccine</th>");
                    out.println("        <th>Exp<br/>Status</th>");
                    out.println("        <th>Exp<br/>Earliest</th>");
                    out.println("        <th>Exp<br/>Recomend</th>");
                    out.println("        <th>Act<br/>Status</th>");
                    out.println("        <th>Act<br/>Earliest</th>");
                    out.println("        <th>Act<br/>Recomend</th>");
                    out.println("      </tr>");
                    for (TestCaseRegistered testCaseRegistered : groupTestCaseMap.get(groupName).values()) {
                        String link = createLink(testCaseRegistered);
                        String linkStep = "step" + link;
                        String linkForecast = "forecast" + link;
                        String groupNameUrlEncoded = groupName.replaceAll(" ", "%20");
                        String uidUrlEncoded = testCaseRegistered.getTestCase().getUid().replaceAll(" ", "%20");
                        String linkRun = "forecast";
                        linkRun += link + "&action=Run";
                        linkRun += "&testPlanId=" + testPlan.getId();
                        linkRun += "&groupName=" + groupNameUrlEncoded;
                        linkRun += "&uid=" + uidUrlEncoded;
                        int numberOfForecasts = testCaseRegistered.getForecastList().size();
                        String rowspan = numberOfForecasts > 1 ? (" rowspan=\"" + numberOfForecasts + "\"") : "";
                        out.println("      <tr>");
                        out.println(
                                "        <td" + rowspan + ">" + testCaseRegistered.getTestCase().getUid() + "</td>");
                        out.println(
                                "        <td" + rowspan + ">" + testCaseRegistered.getTestCase().getName() + "</td>");
                        out.println("        <td" + rowspan + ">");
                        // can't take actions if test case registered is has problems
                        if (testCaseRegistered.isGood()) {
                            out.println("          <a href=\"" + linkStep + "\" target=\"_blank\">Step</a> | ");
                            out.println("          <a href=\"" + linkForecast + "\" target=\"_blank\">Forecast</a> | ");
                            out.println("          <a href=\"" + linkRun + "\" target=\"_blank\">Run</a> ");
                        } else {
                            // print out problem reason, no link
                            out.println("          " + testCaseRegistered.getProblemReason());
                        }
                        out.println("        </td>");
                        if (numberOfForecasts == 0) {
                            out.println("<td colspan=\"4\">No Forecast Expectations</td>");
                        } else {

                            for (TestCaseRegistered.Forecast f : testCaseRegistered.getForecastList()) {
                                if (f != testCaseRegistered.getForecastList().get(0)) {
                                    out.println("      <tr>");
                                }
                                out.println("        <td>" + f.getVaccineCvxExp() + "</td>");
                                out.println("        <td>" + f.getSerieStatusExp() + "</td>");
                                out.println("        <td>" + format(f.getEarliestExp()) + "</td>");
                                out.println("        <td>" + format(f.getRecommendedExp()) + "</td>");
                                if (testCaseRegistered.getException() != null) {
                                    out.println("        <td colspan=\"3\">");
                                    out.println(testCaseRegistered.getException().getMessage());
                                    out.println("<pre>");
                                    testCaseRegistered.getException().printStackTrace(out);
                                    out.println("</pre>");
                                    out.println("        </td>");
                                } else {
                                    String ts = "";
                                    // if forecast.getSerieStatusExp is not null and forecast.getSerieStatusAct is
                                    // not null and they are not equal we need to color the td red
                                    if (f.getSerieStatusExp() != null && f.getSerieStatusAct() != null) {
                                        if (f.getSerieStatusExp().equals(f.getSerieStatusAct())) {
                                            ts = " class=\"pass\"";
                                        } else {
                                            ts = " class=\"fail\"";
                                        }
                                    }
                                    out.println("        <td" + ts + ">" + f.getSerieStatusAct() + "</td>");
                                    ts = "";
                                    if (f.getEarliestExp() != null && f.getEarliestAct() != null) {
                                        if (format(f.getEarliestExp()).equals(format(f.getEarliestAct()))) {
                                            ts = " class=\"pass\"";
                                        } else {
                                            ts = " class=\"fail\"";
                                        }
                                    }
                                    out.println("        <td" + ts + ">" + format(f.getEarliestAct()) + "</td>");
                                    ts = "";
                                    if (f.getRecommendedExp() != null && f.getRecommendedAct() != null) {
                                        if (format(f.getRecommendedExp()).equals(format(f.getRecommendedAct()))) {
                                            ts = " class=\"pass\"";
                                        } else {
                                            ts = " class=\"fail\"";
                                        }
                                    }
                                    out.println("        <td" + ts + ">" + format(f.getRecommendedAct()) + "</td>");
                                }
                                out.println("      </tr>");
                            }
                        }
                        out.println("      </tr>");
                    }
                    out.println("    </table>");
                }
            }
            out.println("</ol>");
        }
        out.println("  <form action=\"fits\" method=\"GET\" id=\"fitsForm\">");
        out.println("    <h3>Enter FITS Credentials</h3>");
        out.println("    <label for=\"username\">Username:</label>");
        out.println(
                "    <input type=\"text\" id=\"username\" name=\"username\" value=\"" + usernameString + "\"required>");
        out.println("    <label for=\"url\">URL:</label>");
        out.println("    <input type=\"text\" id=\"url\" name=\"url\" value=\"" + urlString + "\" required>");
        out.println("    <label for=\"password\">Password:</label>");
        out.println("    <input type=\"password\" id=\"password\" name=\"password\" required>");
        out.println("    <input type=\"Submit\" name=\"action\" value=\"Refresh\">");
        out.println("  </form>");

        out.println("  </body>");
        out.println("</html>");
        out.close();
    }

    private boolean enableLoggingToOut = false;

    private void logToOut(String log) {
        if (enableLoggingToOut) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println(sdf.format(new Date()) + " " + log);
        }
    }

    private void process(DataModel dataModel) throws Exception {
        int count = 0;
        while (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END) {
            LogicStep nextLogicStep = dataModel.getLogicStep().process();
            dataModel.setNextLogicStep(nextLogicStep);
            count++;
            if (count > 100000) {
                System.err.println(
                        "Appear to be caught in a loop at this step: " + dataModel.getLogicStep().getTitle());
                // too many steps!
                if (count > 100100) {
                    throw new RuntimeException(
                            "Logic steps seem to be caught in a loop, unable to get results");
                }
            }
        }
    }

    private String format(Date date) {
        if (date == null) {
            return "-";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }

    private String setupUsernameString(HttpServletRequest req) {
        String usernameString = req.getParameter("username");
        if (usernameString == null) {
            usernameString = "nbunker";
        }
        return usernameString;
    }

    private String setupUrlString(HttpServletRequest req) {
        String urlString = req.getParameter("url");
        if (urlString == null) {
            urlString = "https://fits.nist.gov/";
        }
        return urlString;
    }

    private String createLink(TestCaseRegistered testCaseRegistered) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String link = "?";
        if (testCaseRegistered.getEvalDate() != null) {
            // http://localhost:8080/step/step?evalDate=20160630&scheduleName=default&resultFormat=text&patientDob=20150630&patientSex=F&vaccineDate1=20150826&vaccineCvx1=47&vaccineMvx1=&vaccineDate2=20151027&vaccineCvx2=47&vaccineMvx2=&vaccineDate3=20151229&vaccineCvx3=47&vaccineMvx3=
            link += "evalDate=" + sdf.format(testCaseRegistered.getEvalDate());
            link += "&";
            link += "resultFormat=text";
            if (testCaseRegistered.getBirthDate() != null) {
                link += "&";
                link += "patientDob=" + sdf.format(testCaseRegistered.getBirthDate());
            }
            link += "&";
            String sex = "F";
            if (testCaseRegistered.getTestCase().getPatient().getGender() != null) {
                sex = testCaseRegistered.getTestCase().getPatient().getGender() == Gender.F ? "F" : "M";
            }
            link += "patientSex=" + sex;
            int position = 1;
            for (TestCaseRegistered.Vaccination vaccination : testCaseRegistered.getVaccinationList()) {
                link += "&";
                link += "vaccineDate" + position;
                link += "=";
                link += sdf.format(vaccination.getVaccineDate());
                link += "&";
                link += "vaccineCvx" + position;
                link += "=";
                link += vaccination.getVaccineCvx();
                link += "&";
                link += "vaccineMvx" + position;
                link += "=";
                link += vaccination.getVaccineMvx();
                position++;
            }
        }
        return link;
    }

}
