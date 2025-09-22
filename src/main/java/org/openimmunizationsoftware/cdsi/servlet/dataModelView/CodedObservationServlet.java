package org.openimmunizationsoftware.cdsi.servlet.dataModelView;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class CodedObservationServlet extends MainServlet {

  public static final String SERVLET_NAME = "dataModelViewCodedObservation";

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
      printHeader(out, "Coded Observations");
      printViewCodedObservation(dataModel, out);
      printFooter(out);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }
  }

  private void printViewCodedObservation(DataModel dataModel, PrintWriter out) {
    out.println("  <div class=\"w3-card w3-cell w3-margin\">");
    out.println("    <header class=\"w3-container w3-khaki\">");
    out.println("      <h2>Coded Observations</h2>");
    out.println("    </header>");
    out.println("    <div class=\"w3-container\">");
    out.println("      <table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
    out.println("        <tr>");
    out.println("          <th>Code</th>");
    out.println("          <th>Title</th>");
    out.println("          <th>Indication Description</th>");
    out.println("          <th>Contraindication Description</th>");
    out.println("          <th>Clarifying Text</th>");
    out.println("          <th>SNOMED (Code)</th>");
    out.println("          <th>CVX (Code)</th>");
    out.println("          <th>PHIN VS (Code)</th>");
    out.println("        </tr>");
    out.println("      </table>");
    out.println("    </div>");
    out.println("  </div>");
  }

  private void printRowCodedObservation(PrintWriter out) {
    out.println("     <tr>");
    out.println("       <td>" +"</td>");

    out.println("     </tr>");
  }

  
}
