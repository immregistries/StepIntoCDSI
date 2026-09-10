package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class CompletePatientSeries extends LogicStep {

  private List<PatientSeries> patientSeriesList = dataModel.getScorablePatientSeriesList();

  private int numberOfValidDoses(PatientSeries patientSeries) {
    int nbOfValidDoses = 0;

    List<TargetDose> targetDoseList = patientSeries.getTargetDoseList();
    if (targetDoseList == null) {
      alert("ALERT: TargetDoseList is null for patient series; expected to be populated by previous process");
      return nbOfValidDoses;
    }

    log("Counting valid doses for patient series. Total target doses: " + targetDoseList.size());

    for (TargetDose target : targetDoseList) {
      if (target.getTargetDoseStatus() != null) {
        if (target.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {
          nbOfValidDoses++;
          log("  Found satisfied target dose (count: " + nbOfValidDoses + ")");
        }
      }

    }
    log("Total valid doses: " + nbOfValidDoses);
    return nbOfValidDoses;
  }

  public CompletePatientSeries(DataModel dataModel) {
    super(LogicStepType.COMPLETE_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");
    log("CompletePatientSeries initialized. Total patient series to evaluate: " + patientSeriesList.size());
  }

  /***
   * cond1 A candidate patient series has the most valid doses
   */

  private void evaluate_ACandidatePatientSeriesHasTheMostValidDoses() {
    int mostValidDoses = 0;
    log("Starting evaluation: A candidate patient series has the most valid doses");

    // set mostValidDoses to the greatest number of valid doses found in one patient
    // series
    log("Phase 1: Scanning all patient series to find maximum valid dose count");
    int seriesCount = 0;
    for (PatientSeries patientSeries : patientSeriesList) {
      seriesCount++;
      if (patientSeries.getPatientSeriesStatus() != null
          && !patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)) {
        log("  Series " + seriesCount + ": Skipping (status is not COMPLETE: " + patientSeries.getPatientSeriesStatus()
            + ")");
        continue;
      }

      int newValidDoses = numberOfValidDoses(patientSeries);
      log("  Series " + seriesCount + ": Valid dose count = " + newValidDoses);
      if (newValidDoses > mostValidDoses) {
        mostValidDoses = newValidDoses;
        log("    New max valid doses found: " + mostValidDoses);
      }
    }

    log("Phase 1 complete. Maximum valid dose count: " + mostValidDoses);
    log("Phase 2: Counting how many series share the maximum");

    // SELECTB-19 defines "has the most" as >= every other scorable series - a
    // tie (2 or more series at the maximum) scores 0 per Table 8-7's middle
    // column, not +1 for each; only a lone series at the maximum scores +1.
    int countAtMax = 0;
    for (PatientSeries patientSeries : patientSeriesList) {
      if (patientSeries.getPatientSeriesStatus() != null
          && !patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)) {
        continue;
      }
      if (numberOfValidDoses(patientSeries) >= mostValidDoses) {
        countAtMax++;
      }
    }

    log("Phase 2 complete. " + countAtMax + " series share the maximum.");
    log("Phase 3: Scoring every patient series based on valid dose count");

    int scoredSeriesCount = 0;
    for (PatientSeries patientSeries : patientSeriesList) {
      scoredSeriesCount++;
      if (patientSeries.getPatientSeriesStatus() != null
          && !patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)) {
        // Table 8-7 scores complete patient series only - a Not Complete series
        // does not compete for "has the most valid doses" and is not scored by
        // this row at all (not even downward).
        log("  Series " + scoredSeriesCount + ": Score unchanged (status is not COMPLETE)");
        continue;
      }

      if (numberOfValidDoses(patientSeries) < mostValidDoses) {
        patientSeries.descPatientScoreSeries();
        log("  Series " + scoredSeriesCount + ": Score decreased (valid doses less than max)");
      } else if (countAtMax == 1) {
        patientSeries.incPatientScoreSeries();
        log("  Series " + scoredSeriesCount + ": Score increased (lone series with most valid doses)");
      } else {
        log("  Series " + scoredSeriesCount + ": Score unchanged (tied with another series at the maximum)");
      }
    }
    log("Phase 3 complete. Patient series evaluation finished.");

  }

  @Override
  public LogicStep process() throws Exception {
    log("CompletePatientSeries.process() started");
    setNextLogicStepType(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES);
    evaluate_ACandidatePatientSeriesHasTheMostValidDoses();
    log("CompletePatientSeries.process() completed. Moving to next step: "
        + LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES);
    return next();
  }

}
