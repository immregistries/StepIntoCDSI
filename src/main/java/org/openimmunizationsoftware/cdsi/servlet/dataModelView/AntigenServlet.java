package org.openimmunizationsoftware.cdsi.servlet.dataModelView;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.BirthDateImmunity;
import org.openimmunizationsoftware.cdsi.core.domain.ClinicalHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Exclusion;
import org.openimmunizationsoftware.cdsi.core.domain.Indication;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication;

public class AntigenServlet extends MainServlet {

  public static final String SERVLET_NAME = "dataModelViewAntigen";

  private static final String PARAM_SEARCH = "search_term";

  public static String makeLink(Antigen antigen) {
    return "<a href=\"" + SERVLET_NAME + "?" + PARAM_SEARCH + "=" + antigen.getName()
        + "\" target=\"dataModelView\">" + antigen.getName() + "</a>";
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    HttpSession session = req.getSession(true);
    DataModel dataModel = (DataModel) session.getAttribute("dataModel");
    if (dataModel == null) {
      return;
    }

    resp.setContentType("text/html");

    String search_term = req.getParameter(PARAM_SEARCH);
    if (search_term == null) {
      search_term = "";
    }

    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      printHeader(out, "Antigen");
      out.println("    <form action=\"" + SERVLET_NAME + "\">");

      out.println("      <input type=\"text\" name=\"" + PARAM_SEARCH + "\"><br>");
      out.println("      <input type=\"submit\" value=\"Submit\">");

      out.println("    </form>");
      printAntigen(dataModel, search_term, out);
      printFooter(out);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }
  }

  private void printAntigen(DataModel dataModel, String searchParam, PrintWriter out) {


    out.println("  <div class=\"w3-card w3-cell w3-margin\">");
    out.println("    <header class=\"w3-container w3-khaki\">");
    out.println("      <h2>Antigen</h2>");
    out.println("    </header>");
    out.println("    <div class=\"w3-container\">");
    out.println("      <table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
    out.println("        <caption>Vaccine Dose Administered</caption>");
    out.println("        <tr>");
    out.println("        <th>Name</th>");
    out.println("        <th>Vaccine Group</th>");
    out.println("        <th>Cvx List</th>");
    out.println("        <th>Immunity List</th>");
    out.println("        <th>Contraindication List</th>");
    out.println("        <th>Indication List</th>");
    out.println("        </tr>");
    if (searchParam.equals("")) {
      for (Antigen antigen : dataModel.getAntigenMap().values()) {
        printRowAntigen(dataModel, antigen, out);
      }

    } else {
      printRowAntigen(dataModel, dataModel.getAntigenMap().get(searchParam), out);
    }
    out.println("      </table>");
    out.println("    </div>");
    out.println("  </div>");

  }

  private void printRowAntigen(DataModel dataModel, Antigen antigen, PrintWriter out) {
    out.println("      <tr>");
    out.println("        <td>" + makeLink(antigen) + "</td>");
    out.println("        <td><a href=\"dataModelViewVaccineGroup?search_term="
        + antigen.getVaccineGroup() + "\">" + antigen.getVaccineGroup() + "</td>");
    printCvxList(antigen, out);
    printImmunityList(dataModel, antigen, out);
    printContraindicationList(dataModel, antigen, out);
    printIndicationList(dataModel, antigen, out);
    out.println("      </tr>");

  }

  private void printCvxList(Antigen antigen, PrintWriter out) {
    out.println("        <td>");
    out.println("         <table>");
    for (VaccineType cvx : antigen.getCvxList()) {
      out.println("      <tr>");
      out.println("         <td>" + CvxServlet.makeLink(cvx) + "</td>");
      out.println("      </tr>");
    }
    out.println("         </table>");
    out.println("        </td>");
  }

  private void printImmunityList(DataModel dataModel, Antigen antigen, PrintWriter out) {
    out.println("        <td>");
    out.println("         <table>");
    for (Schedule s : dataModel.getScheduleList()) {
      if(s.getScheduleName().equals(antigen.getName())) {
        for (ClinicalHistory ch : s.getImmunity().getClinicalHistoryList()) {
          out.println("      <tr>");
          out.println("        <td>Code: '" + ch.getImmunityGuidelineCode() + "' Title: '" + ch.getImmunityGuidelineTitle() + "'</td>");
          out.println("      </tr>");
        }
        for (BirthDateImmunity b : s.getImmunity().getBirthDateImmunityList()) {
          for (Exclusion e : b.getExclusionList()) {
            out.println("      <tr>");
            out.println("        <td>ExCode: '" + e.getExclusionCode() + "' ExTitle: '" + e.getExclusionTitle() + "'</td>");
            out.println("      </tr>");
          }
        }
      }
    }
    out.println("         </table>");
    out.println("        </td>");
  }

  private void printContraindicationList(DataModel dataModel, Antigen antigen, PrintWriter out) {
    out.println("        <td>");
    out.println("         <table>");
    for (Schedule s : dataModel.getScheduleList()) {
      if(s.getScheduleName().equals(antigen.getName())) {
        for (Contraindication ci : s.getContraindicationList()) {
          out.println("      <tr>");
          out.println("        <td>Code: '" + ci.getObservationCode() + "' Title: '" + ci.getObservationTitle() + "'</td>");
          out.println("      </tr>");
        }
      }
    }
    out.println("         </table>");
    out.println("        </td>");
  }

  private void printIndicationList(DataModel dataModel, Antigen antigen, PrintWriter out) {
    out.println("        <td>");
    out.println("         <table>");
    for (Schedule s : dataModel.getScheduleList()) {
      if(s.getScheduleName().equals(antigen.getName())) {
        for (AntigenSeries as : s.getAntigenSeriesList()) {
          for(Indication in : as.getIndicationList()) {
            out.println("      <tr>");
            out.println("        <td>Code: '" + in.getObservationCode().getCode() + "' Text: '" + in.getObservationCode().getText() + "'</td>");
            out.println("      </tr>");
          }
        }
      }
    }
    out.println("         </table>");
    out.println("        </td>");
  }

}
