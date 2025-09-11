package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.servlet.ForecastServlet;

public class End extends LogicStep {

  public End(DataModel dataModel) {
    super(LogicStepType.END, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    return null;
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println(
        "<p>End printing forecast stuff</p>");

    VaccineGroup vaccineGroup = dataModel.getVaccineGroup();
    out.println("<h2>" + vaccineGroup.getName() + "</h2>");

    PatientSeries p = dataModel.getBestPatientSeriesList().size() == 0 ? null
        : dataModel.getBestPatientSeriesList().get(0);
    if (dataModel.getBestPatientSeriesList() == null) {
      out.println("<p>Best Patient Series List is null!</p>");
    } else {
      out.println("<p>Best Patient Series List size = " + dataModel.getBestPatientSeriesList().size() + "</p>");
    }
    out.println("<p>Forecast List size = " + dataModel.getForecastList().size() + "</p>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Antigen</th>");
    out.println("    <th>Target Dose</th>");
    out.println("    <th>VGF Status</th>");
    out.println("    <th>Earliest Date</th>");
    out.println("    <th>Adjusted Recommended Date</th>");
    out.println("    <th>Adjusted Past Due Date</th>");
    out.println("    <th>Latest Date</th>");
    out.println("    <th>Unadjusted Recommended Date</th>");
    out.println("    <th>Unadjusted Past Due Date</th>");
    out.println("    <th>Forecast Reason</th>");
    out.println("  </tr>");

    for (Forecast forecast : dataModel.getForecastList()) {
      for (Antigen antigen : dataModel.getAntigenSelectedList()) {
        if (forecast.getAntigen().equals(antigen)) {
          out.println("  <tr>");
          out.println("    <td>" + forecast.getAntigen().getName() + "</td>");
          out.println("    <td>" + forecast.getTargetDose() + "</td>");
          out.println("    <td>" + (forecast.getVaccineGroupForecast() == null ? "null"
            : forecast.getVaccineGroupForecast().getVaccineGroupStatus()) + "</td>");
          out.println("    <td>" + n(forecast.getEarliestDate()) + "</td>");
          out.println("    <td>" + n(forecast.getAdjustedRecommendedDate()) + "</td>");
          out.println("    <td>" + n(forecast.getAdjustedPastDueDate()) + "</td>");
          out.println("    <td>" + n(forecast.getLatestDate()) + "</td>");
          out.println("    <td>" + n(forecast.getUnadjustedRecommendedDate()) + "</td>");
          out.println("    <td>" + n(forecast.getUnadjustedPastDueDate()) + "</td>");
          out.println("    <td>" + forecast.getForecastReason() + "</td>");
          out.println("  </tr>");
        }
      }
    }
    out.println("</table>");

    List<VaccineGroupForecast> vgfl = dataModel.getVaccineGroupForecastList();
    out.println("<p>Vaccine Group Forecast List size = " + vgfl.size() + "</p>");
    if (vgfl.size() > 0) {
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>Target Dose</th>");
      out.println("    <th>Patient Series Status</th>");
      out.println("  </tr>");
      for (VaccineGroupForecast vgf : vgfl) {
        out.println("  <tr>");
        out.println("    <td>" + vgf.getAntigen().getName() + "</td>");
        out.println("    <td>" + vgf.getTargetDose() + "</td>");
        out.println("    <td>" + vgf.getPatientSeriesStatus() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
    out.println("<pre>");
    ForecastServlet.printText(dataModel, out);
    out.println("</pre>");
    out.println("<h2>Printing Standard</h2>");
    printStandard(out);
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
  }

  private void printStandard(PrintWriter out) {
    printConditionAttributesTable(out);
    printLogicTables(out);
  }
}
