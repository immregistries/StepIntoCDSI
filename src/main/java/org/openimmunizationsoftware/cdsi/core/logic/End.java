package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;

public class End extends LogicStep {

  public End(DataModel dataModel) {
    super(LogicStepType.END, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println("<h1> " + getTitle() + "</h1>");
    out.println(
        "<p>End printing forecast stuff</p>");

    List<VaccineGroupForecast> vaccineGroupForecastList = new ArrayList<VaccineGroupForecast>();
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
    out.println("    <th>Patient Series Status</th>");
    out.println("    <th>Earliest Date</th>");
    out.println("    <th>Adjusted Recommended Date</th>");
    out.println("    <th>Adjusted Past Due Date</th>");
    out.println("    <th>Latest Date</th>");
    out.println("    <th>Unadjusted Recommended Date</th>");
    out.println("    <th>Unadjusted Past Due Date</th>");
    out.println("    <th>Forecast Reason</th>");
    out.println("  </tr>");

    for (Forecast forecast : dataModel.getForecastList()) {
      for (Antigen antigen : dataModel.getAntigenList()) {
        VaccineGroupForecast vgf = new VaccineGroupForecast();
      
        if (forecast.getAntigen().equals(antigen)) {
          out.println("  <tr>");
          out.println("    <td>" + forecast.getAntigen().getName() + "</td>");
          vgf.setTargetDose(p == null ? null : p.getForecast().getTargetDose());
          out.println("    <td>" + (p == null ? null : p.getForecast().getTargetDose()) + "</td>");
          vgf.setVaccineGroupStatus(p == null ? null : p.getPatientSeriesStatus());
          out.println("    <td>" + (p == null ? null : p.getPatientSeriesStatus()) + "</td>");
          vgf.setEarliestDate(forecast.getEarliestDate());
          out.println("    <td>" + n(forecast.getEarliestDate()) + "</td>");
          vgf.setAdjustedRecommendedDate(forecast.getAdjustedRecommendedDate());
          out.println("    <td>" + n(forecast.getAdjustedRecommendedDate()) + "</td>");
          vgf.setAdjustedPastDueDate(forecast.getAdjustedPastDueDate());
          out.println("    <td>" + n(forecast.getAdjustedPastDueDate()) + "</td>");
          vgf.setLatestDate(forecast.getLatestDate());
          out.println("    <td>" + n(forecast.getLatestDate()) + "</td>");
          vgf.setUnadjustedRecommendedDate(forecast.getUnadjustedRecommendedDate());
          out.println("    <td>" + n(forecast.getUnadjustedRecommendedDate()) + "</td>");
          vgf.setUnadjustedPastDueDate(forecast.getUnadjustedPastDueDate());
          out.println("    <td>" + n(forecast.getUnadjustedPastDueDate()) + "</td>");
          vgf.setForecastReason(forecast.getForecastReason());
          out.println("    <td>" + forecast.getForecastReason() + "</td>");
          vgf.setAntigen(forecast.getAntigen());
          out.println("  </tr>");
          dataModel.getVaccineGroupForecastList().add(vgf);
        }
        
      }
    }
    out.println("</table>");
    out.println("<h2>Printing Standard</h2>");
    printStandard(out);

  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);

  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + getTitle() + "</h1>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

}
