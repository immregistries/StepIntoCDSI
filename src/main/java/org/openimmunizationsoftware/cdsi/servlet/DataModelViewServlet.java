package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class DataModelViewServlet extends ForecastServlet {

  private static final String PARAM_VIEW = "view";
  private static final String VIEW_PATIENT = "patient";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    HttpSession session = req.getSession(true);
    DataModel dataModel = (DataModel) session.getAttribute("dataModel");
    if (dataModel == null) {
      return;
    }

    resp.setContentType("text/html");

    // String s= req.getParameter("s");
    // Integer.parseInt(s);
    // s.equals("");
    // out.println(" <a href=\"dataModelView?s=" + s + "\"");

    String view = req.getParameter("view");
    if (view == null) {
      view = VIEW_PATIENT;
    }

    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
      out.println("<html>");
      out.println("  <head>");
      out.println("    <title>CDSi - Data Model View</title>");
      out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"index.css\">");
      out.println("  </head>");
      out.println("  <body>");
      if (view.equals(VIEW_PATIENT)) {
        printViewPatient(dataModel, out);
      }
      out.println("  </body>");
      out.println("</html>");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }
  }

  private void printViewPatient(DataModel dataModel, PrintWriter out) {
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <caption>Patient</caption>");
    out.println("       <th>Patient DOB</th>");
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    out.println("       <td>" + sdf.format(dataModel.getPatient().getDateOfBirth()) + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Gender</th>");
    out.println("       <td>" + dataModel.getPatient().getGender() + "</td>");
    out.println("     </tr>");
    out.println("   </table>");
  }
}
