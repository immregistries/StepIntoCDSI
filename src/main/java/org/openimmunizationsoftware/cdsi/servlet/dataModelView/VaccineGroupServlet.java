package org.openimmunizationsoftware.cdsi.servlet.dataModelView;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.servlet.ForecastServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by Eric on 7/7/16.
 */
public class VaccineGroupServlet extends MainServlet {
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
      printHeader(out, "Vaccine Group");
      // TODO: Lee, do something here
      printFooter(out);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }

  }
}
