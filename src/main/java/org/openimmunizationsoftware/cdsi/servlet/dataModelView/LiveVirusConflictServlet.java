package org.openimmunizationsoftware.cdsi.servlet.dataModelView;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.servlet.ForecastServlet;

public class LiveVirusConflictServlet extends MainServlet {

  public static final String SERVLET_NAME = "dataModelViewLiveVirusConflict";

  private static final String PARAM_SEARCH = "search_term";

  public static String makeLink(VaccineType vaccineType) {
    return ""; // "<a href=\"" + SERVLET_NAME + "?" + PARAM_SEARCH + "=" +
               // vaccineType.getCvxCode() + "\" target=\"dataModelView\">" +
               // vaccineType.getShortDescription() + "</a>";
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    HttpSession session = req.getSession(true);
    DataModel dataModel = (DataModel) session.getAttribute("dataModel");
    if (dataModel == null) {
      return;
    }

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());

    String search_term = req.getParameter(PARAM_SEARCH);
    if (search_term == null) {
      search_term = "";
    }

    try {
      printHeader(out, "Live Virus Conflict");

      printFooter(out);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }
  }
}
