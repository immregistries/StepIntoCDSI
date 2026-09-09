package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel;

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
    logicTable.setLogicStepSink(this.getLogicStepSink());
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    VaccineGroup vaccineGroup = dataModel.getVaccineGroup();
    if (vaccineGroup == null || vaccineGroup.getAntigenList() == null || vaccineGroup.getAntigenList().isEmpty()
        || vaccineGroup.getAntigenList().get(0) == null) {
      alert(LogLevel.CONTROL,
          "ALERT.MISSING: Vaccine group or primary antigen missing in SINGLE_ANTIGEN_VACCINE_GROUP; " +
              "vaccineGroup=" + (vaccineGroup != null ? vaccineGroup.getName() : "null") +
              ". Skipping vaccine group forecast generation for this group.");
      setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
      return next();
    }

    log(LogLevel.CONTROL, "SINGLEANTVG: Starting single antigen vaccine group processing; " +
        "vaccineGroup=" + (vaccineGroup != null ? vaccineGroup.getName() : "null") +
        ", bestPatientSeriesListSize=" + dataModel.getBestPatientSeriesList().size());

    // SPEC-4.6-0022: 9.2's own spec text and SINGLEANTVG-1 assume exactly one
    // best patient series will match this antigen ("the patient series status
    // of the patient series forecast"), but SPEC-4.6-0020's series-group loop
    // means an antigen can legitimately have more than one - e.g. RSV's
    // general, unrestricted "RSV 1-dose series" (group 1) and its "RSV 75
    // years+" alternate (group 3) are not declared equivalentSeriesGroups to
    // each other (only to group 2), so 8.8 cannot collapse them into one. When
    // that happens, prefer whichever matching series represents the most
    // favorable outcome for the antigen (Complete/Immune beats an actionable
    // Not Complete path beats a dead-end Contraindicated/Aged Out/Not
    // Recommended one) rather than the previous behavior, which mutated one
    // shared VaccineGroupForecast once per match and silently kept only
    // whichever match happened to be evaluated last.
    int matchCount = 0;
    PatientSeries chosen = null;
    Date earliestOfAllContained = null;
    for (PatientSeries p : dataModel.getBestPatientSeriesList()) {
      Forecast forecast = p.getForecast();
      String seriesName = p.getTrackedAntigenSeries() != null ? p.getTrackedAntigenSeries().getSeriesName() : "null";
      log(LogLevel.STATE, "SINGLEANTVG: Evaluating patient series; " +
          "seriesName=" + seriesName +
          ", patientSeriesStatus=" + p.getPatientSeriesStatus() +
          ", forecastIsNull=" + (forecast == null));

      if (forecast != null && forecast.getAntigen() != null
          && forecast.getAntigen().equals(vaccineGroup.getAntigenList().get(0))) {
        matchCount++;
        log(LogLevel.REASONING, "SINGLEANTVG: Forecast antigen matches vaccine group antigen; " +
            "antigen=" + forecast.getAntigen().getName() +
            ", targetDose=" + p.getForecast().getTargetDose() +
            ", patientSeriesStatus=" + p.getPatientSeriesStatus());
        if (chosen == null || isMoreFavorable(p, chosen)) {
          chosen = p;
        }
        // SINGLEANTVG-2 (verbatim): "the earliest date ... must be the earliest
        // date of ALL the patient series forecasts contained in the vaccine group
        // forecast" - an aggregate minimum over every match, independent of which
        // one SPEC-4.6-0022's status precedence chose.
        if (forecast.getEarliestDate() != null
            && (earliestOfAllContained == null || forecast.getEarliestDate().before(earliestOfAllContained))) {
          earliestOfAllContained = forecast.getEarliestDate();
        }
      }
    }

    if (chosen != null) {
      Forecast forecast = chosen.getForecast();
      String seriesName = chosen.getTrackedAntigenSeries() != null ? chosen.getTrackedAntigenSeries().getSeriesName()
          : "null";

      VaccineGroupForecast vgf = new VaccineGroupForecast();
      vgf.setVaccineGroup(vaccineGroup);

      // Règle en plus
      vgf.setAntigen(forecast.getAntigen());
      vgf.setTargetDose(forecast.getTargetDose());

      // SINGLEANTVG-1 The vaccine group status for a single antigen vaccine group
      // must be the patient series status of the best patient series.
      PatientSeriesStatus pss = chosen.getPatientSeriesStatus();
      if (pss == null) {
        String targetDoseNum = "null";
        if (chosen.getTargetDoseList() != null && chosen.getTargetDoseList().size() > 0) {
          TargetDose td = chosen.getTargetDoseList().get(0);
          if (td.getTrackedSeriesDose() != null) {
            targetDoseNum = td.getTrackedSeriesDose().getDoseNumber();
          }
        }
        alert(LogLevel.CONTROL, "ALERT.MISSING: PatientSeriesStatus is null in best patient series; " +
            "context: step=SINGLE_ANTIGEN_VACCINE_GROUP, series=" + seriesName +
            ", targetDose=" + targetDoseNum +
            "; fallback: will default to NOT_COMPLETE; " +
            "impact: vaccine group status may be incorrect");
      }
      vgf.setVaccineGroupStatus(pss);
      vgf.setPatientSeriesStatus(pss);

      // SINGLEANTVG-2 The vaccine group forecast earliest date for a single antigen
      // vaccine group must be the earliest date of ALL contained patient series
      // forecasts - not necessarily the chosen (most favorable-status) one.
      vgf.setEarliestDate(earliestOfAllContained);
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
      log(LogLevel.REASONING, "SINGLEANTVG: Adding vaccine group forecast; " +
          "antigen=" + vgf.getAntigen().getName() +
          ", chosenSeries=" + seriesName +
          ", status=" + vgf.getVaccineGroupStatus() +
          ", earliestDate=" + vgf.getEarliestDate() +
          ", recommendedDate=" + vgf.getAdjustedRecommendedDate());
      dataModel.getVaccineGroupForecastList().add(vgf);
    }

    log(LogLevel.STATE, "SINGLEANTVG: Completed processing; " +
        "matchedSeries=" + matchCount +
        ", vaccineGroupForecastListSize=" + dataModel.getVaccineGroupForecastList().size());

    if (matchCount == 0) {
      alert(LogLevel.CONTROL, "ALERT.SPECGAP: No matching forecast found for vaccine group; " +
          "context: step=SINGLE_ANTIGEN_VACCINE_GROUP, vaccineGroup=" +
          (vaccineGroup != null ? vaccineGroup.getName() : "null") +
          ", bestPatientSeriesCount=" + dataModel.getBestPatientSeriesList().size() +
          "; impact: vaccine group forecast list may be incomplete");
    }

    setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
    return next();
  }

  /**
   * SPEC-4.6-0022: when more than one best patient series matches this
   * antigen, ranks Complete/Immune above Not Complete above the remaining
   * dead-end statuses (Contraindicated, Aged Out, Not Recommended) - "did the
   * patient satisfy this antigen through any of its alternative pathways" is
   * a materially different question than Table 9-3's multi-antigen "did the
   * patient satisfy every antigen in this bundle," so that table's
   * worst-status-wins precedence is deliberately not reused here. Ties keep
   * whichever series was reached first, i.e. Chapter 8's own series-group
   * processing order (the antigen's default, unrestricted series group before
   * any age/risk-restricted alternate).
   */
  private static boolean isMoreFavorable(PatientSeries candidate, PatientSeries currentBest) {
    return favorabilityRank(candidate.getPatientSeriesStatus()) < favorabilityRank(currentBest.getPatientSeriesStatus());
  }

  private static int favorabilityRank(PatientSeriesStatus status) {
    if (status == PatientSeriesStatus.COMPLETE || status == PatientSeriesStatus.IMMUNE) {
      return 0;
    }
    if (status == PatientSeriesStatus.NOT_COMPLETE) {
      return 1;
    }
    return 2;
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
