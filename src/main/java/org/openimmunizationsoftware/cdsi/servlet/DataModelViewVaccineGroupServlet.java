package org.openimmunizationsoftware.cdsi.servlet;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

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
            out.println("  Hello world!");




            out.println("  </body>");
            out.println("</html>");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }





    }
}
