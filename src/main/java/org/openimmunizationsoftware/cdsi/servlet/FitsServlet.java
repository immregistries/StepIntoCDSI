package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.servlet.fits.FitsManager;
import org.openimmunizationsoftware.cdsi.servlet.fits.TestCaseRegistered;

import gov.nist.healthcare.cds.enumeration.Gender;

public class FitsServlet extends ForecastServlet {

    private static FitsManager fitsManager = null;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(true);
        String action = req.getParameter("action");
        Exception exception = null;

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
            }
        }

        resp.setContentType("text/html");

        String urlString = req.getParameter("url");
        if (urlString == null) {
            urlString = "https://fits.nist.gov/";
        }

        String usernameString = req.getParameter("username");
        if (usernameString == null) {
            usernameString = "nbunker";
        }

        PrintWriter out = new PrintWriter(resp.getOutputStream());
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
        out.println("<html>");
        out.println("  <head>");
        out.println("    <title>CDSi - FITS</title>");
        out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"index.css\">");
        out.println("  </head>");
        out.println("  <body>");
        out.println("  <form action=\"fits\" method=\"GET\" id=\"fitsForm\">");
        // username, url, and password collection fields
        if (fitsManager != null) {
            out.println("    <h1>Connected to FITS</h1>");
            for (String groupName : fitsManager.getGroupNames()) {
                out.println("    <h3>" + groupName + "</h3>");
                out.println("    <table>");
                out.println("      <tr>");
                out.println("        <th>Test Case</th>");
                out.println("        <th>Title</th>");
                out.println("        <th>Action</th>");
                out.println("      </tr>");
                for (TestCaseRegistered testCaseRegistered : fitsManager.getTestCaseListGood(groupName)) {
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
                    String linkStep = "step" + link;
                    String linkForecast = "forecast" + link;
                    out.println("      <tr>");
                    out.println("        <td>" + testCaseRegistered.getTestCase().getUid() + "</td>");
                    out.println("        <td>" + testCaseRegistered.getTestCase().getName() + "</td>");
                    out.println("        <td><a href=\"" + linkStep + "\" target=\"_blank\">Step</a> | <a href=\""
                            + linkForecast
                            + "\">Forecast</a></td>");
                    out.println("      </tr>");
                }
                out.println("    </table>");
            }
        }
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

        if (exception != null) {
            out.println("    <h1>Exception</h1>");
            out.println("    <p>" + exception.getMessage() + "</p>");
            out.println("<pre>");
            exception.printStackTrace(out);
            out.println("</pre>");
        }

        out.println("  </body>");
        out.println("</html>");
        out.close();
    }

}
