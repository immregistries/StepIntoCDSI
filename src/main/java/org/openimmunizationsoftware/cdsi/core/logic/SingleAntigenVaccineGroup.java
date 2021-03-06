package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
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

    // caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date
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
    PatientSeries p = dataModel.getBestPatientSeriesList().get(0);
    for (Forecast forecast : dataModel.getForecastList()) {
      if (forecast.getAntigen().equals(vaccineGroup.getAntigenList().get(0))) {

        // Règle en plus
        vgf.setTargetDose(p.getForecast().getTargetDose());
        // SINGLEANTVG-1 The vaccine group status for a single antigen vaccine group must be the
        // patient series status of the best patient series.

        vgf.setVaccineGroupStatus(p.getPatientSeriesStatus());

        // SINGLEANTVG-2 The vaccine group forecast earliest date for a single antigen vaccine group
        // must be the best patient series forecast earliest date.
        vgf.setEarliestDate(forecast.getEarliestDate());
        // SINGLEANTVG-3 The vaccine group forecast adjusted recommended date for a single antigen
        // vaccine group must be the best patient series forecast adjusted recommended date.
        vgf.setAdjustedRecommendedDate(forecast.getAdjustedRecommendedDate());

        // SINGLEANTVG-4 The vaccine group forecast adjusted past due date for a single antigen
        // vaccine group must be the best patient series forecast adjusted past due date.
        vgf.setAdjustedPastDueDate(forecast.getAdjustedPastDueDate());
        // SINGLEANTVG-5 The vaccine group forecast latest date for a single antigen vaccine group
        // must be the best patient series forecast latest date.
        vgf.setLatestDate(forecast.getLatestDate());
        // SINGLEANTVG-6 The vaccine group forecast unadjusted recommended date for a single antigen
        // vaccine group must be the best patient series forecast unadjusted recommended date.
        vgf.setUnadjustedRecommendedDate(forecast.getUnadjustedRecommendedDate());
        // SINGLEANTVG-7 The vaccine group forecast unadjusted past due date for a single antigen
        // vaccine group must be the best patient series forecast unadjusted past due date.
        vgf.setUnadjustedPastDueDate(forecast.getUnadjustedPastDueDate());
        // SINGLEANTVG-8 The vaccine group forecast reason for a single antigen vaccine group must
        // be set the best patient series forecast reason.
        vgf.setForecastReason(forecast.getForecastReason());
        // SINGLEANTVG-9 The vaccine group forecast antigens needed for a single antigen vaccine
        // group must be the best patient series target disease.
        // vgf.setAntigensNeededList(forecast.getAntigen());
        // SINGLEANTVG-10 The vaccine group forecast recommended vaccines for a single antigen
        // vaccine group must be the best patient series forecast recommended vaccines.
        //
        vgf.setAntigen(forecast.getAntigen());
      }
    }
    dataModel.getVaccineGroupForcastList().add(vgf);

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

    // printConditionAttributesTable(out);
    // printLogicTables(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(0, 0, "Table ?-?");

      // setLogicCondition(0, new LogicCondition("date administered > lot expiration date?") {
      // @Override
      // public LogicResult evaluateInternal() {
      // if (caDateAdministered.getFinalValue() == null || caTriggerAgeDate.getFinalValue() == null)
      // {
      // return LogicResult.NO;
      // }
      // if (caDateAdministered.getFinalValue().before(caTriggerAgeDate.getFinalValue())) {
      // return LogicResult.YES;
      // }
      // return LogicResult.NO;
      // }
      // });

      // setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.ANY);

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
