package org.openimmunizationsoftware.cdsi.servlet;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by Eric on 7/7/16.
 */
public class DataModelViewVaccineGroupServlet extends ForecastServlet{
    /*public static String makeLink(VaccineGroup vaccineGroup) {
        return ;
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
            out.println("    <title>CDSi - Data Model View - Vaccine Group</title>");
            out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"index.css\">");
            out.println("  </head>");
            out.println("  <body>");
            out.println("");




            out.println("  </body>");
            out.println("</html>");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }





    }
    private void printVaccineGroup(DataModel dataModel, PrintWriter out) {
        out.println("    <table>");
        out.println("      <tr>");
        out.println("        <caption>Vaccine Group</caption>");
        out.println("        <th>Name</th>");
        out.println("        <th>Vaccine List</th>");
        out.println("        <th>Vaccine Group Forecast</th>");
        out.println("        <th>Administer Full Vaccine Group</th>");
        out.println("        <th>Antigen List</th>");

        out.println("      </tr>");
    }
}
