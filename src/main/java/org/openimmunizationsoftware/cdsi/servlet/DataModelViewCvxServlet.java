package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;

public class DataModelViewCvxServlet extends ForecastServlet {
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
      out.println("    <title>CDSi - Data Model View - CVX</title>");
      out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"index.css\">");
      out.println("  </head>");
      out.println("  <body>");
      out.println("  <table>");
      out.println("   <tr>");
      out.println("      <th>Cvx Code</td>");
      out.println("      <th>Short Description</td>");
      out.println("   </tr>");
      dataModel.getCvxMap();
      for(VaccineType vaccineType: dataModel.getCvxMap().values()){
        // I am most likely doing the wrong thing here. :)
        out.println("   <tr>");
        out.println("      <td>" + vaccineType.getCvxCode() + "</td>");
        out.println("      <td>" + vaccineType.getShortDescription() + "</td>");
        out.println("   </tr>");

      }
      // Do stuff here .....
      out.println("  </table>");
      out.println("  </body>");
      out.println("</html>");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }
  }
}
