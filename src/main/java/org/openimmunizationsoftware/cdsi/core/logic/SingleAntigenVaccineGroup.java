package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class SingleAntigenVaccineGroup extends LogicStep {

  // private ConditionAttribute<Date> caDateAdministered = null;

  public SingleAntigenVaccineGroup(DataModel dataModel) {
    super(LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP, dataModel);
    setConditionTableName("Table ");

    // caDateAdministered = new ConditionAttribute<Date>("Vaccine dose
    // administered", "Date
    // Administered");

    // caTriggerAgeDate.setAssumedValue(FUTURE);

    // conditionAttributesList.add(caDateAdministered);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    List<VaccineGroupForecast> vaccineGroupForecastList = new ArrayList<VaccineGroupForecast>();

    VaccineGroup vaccineGroup = dataModel.getVaccineGroup();
    VaccineGroupForecast vgf = new VaccineGroupForecast();
    vgf.setVaccineGroup(vaccineGroup);
    PatientSeries p = dataModel.getBestPatientSeriesList().size() == 0 ? null
        : dataModel.getBestPatientSeriesList().get(0);
    for (Forecast forecast : dataModel.getForecastList()) {
      if (forecast.getAntigen().equals(vaccineGroup.getAntigenList().get(0))) {
        // System.out.println("--> antigen = " + forecast.getAntigen());
        // dataModel.getVaccineGroupForecastList().size()
        // System.out.println("--> getBestPatientSeriesList.size() = " +
        // dataModel.getBestPatientSeriesList().size());
        // for (PatientSeries ps : dataModel.getBestPatientSeriesList()) {
        // System.out.println("--> getPatientSeriesStatus = " +
        // ps.getPatientSeriesStatus() + " for "
        // + ps.getTrackedAntigenSeries().getSeriesName() + " for "
        // + ps.getTrackedAntigenSeries().getTargetDisease());
        // }
        log("<p>    forecast antigen equals first antigen in vaccine group, creating VGF</p>");
        // Règle en plus
        vgf.setAntigen(forecast.getAntigen());
        vgf.setTargetDose(p == null ? null : p.getForecast().getTargetDose());
        // SINGLEANTVG-1 The vaccine group status for a single antigen vaccine group
        // must be the
        // patient series status of the best patient series.
        vgf.setVaccineGroupStatus(p == null ? null : p.getPatientSeriesStatus());
        vgf.setPatientSeriesStatus(p == null ? null : p.getPatientSeriesStatus());

        // SINGLEANTVG-2 The vaccine group forecast earliest date for a single antigen
        // vaccine group
        // must be the best patient series forecast earliest date.
        vgf.setEarliestDate(forecast.getEarliestDate());
        // SINGLEANTVG-3 The vaccine group forecast adjusted recommended date for a
        // single antigen
        // vaccine group must be the best patient series forecast adjusted recommended
        // date.
        vgf.setAdjustedRecommendedDate(forecast.getAdjustedRecommendedDate());

        // SINGLEANTVG-4 The vaccine group forecast adjusted past due date for a single
        // antigen
        // vaccine group must be the best patient series forecast adjusted past due
        // date.
        vgf.setAdjustedPastDueDate(forecast.getAdjustedPastDueDate());
        // SINGLEANTVG-5 The vaccine group forecast latest date for a single antigen
        // vaccine group
        // must be the best patient series forecast latest date.
        vgf.setLatestDate(forecast.getLatestDate());
        // SINGLEANTVG-6 The vaccine group forecast unadjusted recommended date for a
        // single antigen
        // vaccine group must be the best patient series forecast unadjusted recommended
        // date.
        vgf.setUnadjustedRecommendedDate(forecast.getUnadjustedRecommendedDate());
        // SINGLEANTVG-7 The vaccine group forecast unadjusted past due date for a
        // single antigen
        // vaccine group must be the best patient series forecast unadjusted past due
        // date.
        vgf.setUnadjustedPastDueDate(forecast.getUnadjustedPastDueDate());
        // SINGLEANTVG-8 The vaccine group forecast reason for a single antigen vaccine
        // group must
        // be set the best patient series forecast reason.
        vgf.setForecastReason(forecast.getForecastReason());
        // SINGLEANTVG-9 The vaccine group forecast antigens needed for a single antigen
        // vaccine
        // group must be the best patient series target disease.
        // vgf.setAntigensNeededList(forecast.getAntigen());
        // SINGLEANTVG-10 The vaccine group forecast recommended vaccines for a single
        // antigen
        // vaccine group must be the best patient series forecast recommended vaccines.
        //
        dataModel.getVaccineGroupForecastList().add(vgf);
      }
    }

    setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
    return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  public PatientSeries getBestPatientSeries() {
    if (dataModel.getPatientSeriesList().size() > 0) {
      PatientSeries bestPatientSeries = dataModel.getPatientSeriesList().get(0);
      for (PatientSeries ps : dataModel.getPatientSeriesList()) {
        if (ps.getScorePatientSeries() > bestPatientSeries.getScorePatientSeries())
          bestPatientSeries = ps;
      }
      return bestPatientSeries;
    } else {
      return null;
    }
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + getTitle() + "</h1>");
    out.println(
        "<p>The forecasting rules which need to be applied to a single antigen vaccine group are listed in the table below</p>");

    List<VaccineGroupForecast> vaccineGroupForecastList = new ArrayList<VaccineGroupForecast>();
    VaccineGroup vaccineGroup = dataModel.getVaccineGroup();
    out.println("<h2>" + vaccineGroup.getName() + "</h2>");
    VaccineGroupForecast vgf = new VaccineGroupForecast();

    PatientSeries p = dataModel.getBestPatientSeriesList().size() == 0 ? null
        : dataModel.getBestPatientSeriesList().get(0);
    if (dataModel.getBestPatientSeriesList() == null) {
      out.println("<p>Best Patient Series List is null!</p>");
    } else {
      out.println("<p>Best Patient Series List size = " + dataModel.getBestPatientSeriesList().size() + "</p>");
    }
    out.println("<p>Forecast List size = " + dataModel.getForecastList().size() + " for list "
        + dataModel.getForecastList() + "</p>");
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
      out.println("  <tr>");
      out.println("    <td>" + forecast.getAntigen().getName() + "</td>");
      out.println("    <td>" + (p == null ? null : p.getForecast().getTargetDose()) + "</td>");
      out.println("    <td>" + (p == null ? null : p.getPatientSeriesStatus()) + "</td>");
      out.println("    <td>" + n(forecast.getEarliestDate()) + "</td>");
      out.println("    <td>" + n(forecast.getAdjustedRecommendedDate()) + "</td>");
      out.println("    <td>" + n(forecast.getAdjustedPastDueDate()) + "</td>");
      out.println("    <td>" + n(forecast.getLatestDate()) + "</td>");
      out.println("    <td>" + n(forecast.getUnadjustedRecommendedDate()) + "</td>");
      out.println("    <td>" + n(forecast.getUnadjustedPastDueDate()) + "</td>");
      out.println("    <td>" + forecast.getForecastReason() + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");

    setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);

  }

  private class LT extends LogicTable {
    public LT() {
      super(0, 0, "Table ?-?");

      // setLogicCondition(0, new LogicCondition("date administered > lot expiration
      // date?") {
      // @Override
      // public LogicResult evaluateInternal() {
      // if (caDateAdministered.getFinalValue() == null ||
      // caTriggerAgeDate.getFinalValue() == null)
      // {
      // return LogicResult.NO;
      // }
      // if
      // (caDateAdministered.getFinalValue().before(caTriggerAgeDate.getFinalValue()))
      // {
      // return LogicResult.YES;
      // }
      // return LogicResult.NO;
      // }
      // });

      // setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO,
      // LogicResult.ANY);

      // setLogicOutcome(0, new LogicOutcome() {
      // @Override
      // public void perform() {
      // log("No. The target dose cannot be skipped. ");
      // log("Setting next step: 4.3 Substitute Target Dose");
      // setNextLogicStep(LogicStep.SUBSTITUTE_TARGET_DOSE_FOR_EVALUATION);
      // }
      // });
      //
    }
  }

}
