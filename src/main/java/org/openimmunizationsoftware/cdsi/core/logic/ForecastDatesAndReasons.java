package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;

public class ForecastDatesAndReasons extends LogicStep {

  public ForecastDatesAndReasons(DataModel dataModel) {
    super(LogicStepType.FORECAST_DATES_AND_REASONS, dataModel);
    // TODO Auto-generated constructor stub
  }

  @Override
  public LogicStep process() throws Exception {
    Forecast forecast = new Forecast();
    forecast.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
    forecast.setTargetDose(dataModel.getTargetDose());
    dataModel.setForecast(forecast);
    dataModel.getPatientSeries().setForecast(forecast);

    setNextLogicStepType(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST);
    return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    // TODO Auto-generated method stub
    printStandard(out);
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    // TODO Auto-generated method stub
    printStandard(out);

  }

  private void printTableAndFigure(PrintWriter out) {
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Section</th>");
    out.println("    <th>Activity</th>");
    out.println("    <th>Goal</th>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>5.1</td>");
    out.println("    <td>Evaluate Dose Conditional Skip</td>");
    out.println(
        "    <td>The goal of this step is to determine if the target dose can be skipped due to a patient‘s age at assessment or immunization history.</td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>5.2</td>");
    out.println("    <td>Determine Evidence of Immunity</td>");
    out.println("    <td>The goal of this step is to determine if the patient has evidence of immunity.");
    out.println("</td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>5.3</td>");
    out.println("    <td>Determine Forecast Need</td>");
    out.println("    <td>The goal of this step is to determine if the patient should");
    out.println("receive another dose.</td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>5.4</td>");
    out.println("    <td>Generate Forecast Dates</td>");
    out.println("    <td>The goal of this step is to generate forecast dates for the");
    out.println("next target dose.</td>");
    out.println("  </tr>");
    out.println("</table>");
    out.println("");
    out.println("<p>The figure below provides an illustration of the forecast dates and reasonsprocess.</p>");
    out.println("");
    out.println("<img src=\"Figure 5.1.png\"/>");

  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + getTitle() + "</h1>");
    out.println(
        "<p>The CDS engine uses a patient's medical and vaccine history to forecast immunization due dates. This chapter identifies specific business rules that are used by a CDS engine to forecast the next  target dose.  The major steps involved in this process are listed in the table below.</p>");
    printTableAndFigure(out);
    TargetDose targetDose = dataModel.getTargetDose();
    if (targetDose == null) {
      out.println("<p>No Target Dose defined</p>");
    } else {
      out.println("<p>Tracked Series Dose: " + targetDose.getTrackedSeriesDose() + "</p>");
    }
  }
}
