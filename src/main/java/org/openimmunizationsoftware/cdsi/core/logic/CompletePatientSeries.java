package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class CompletePatientSeries extends LogicStep {

  private List<PatientSeries> patientSeriesList = dataModel.getSelectedPatientSeriesList();

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
    log("Phase 2: Scoring patient series based on valid dose count");

    // get number of patientSeries with the most valid dose
    int scoredSeriesCount = 0;
    for (PatientSeries patientSeries : patientSeriesList) {
      scoredSeriesCount++;
      if (patientSeries.getPatientSeriesStatus() != null
          && !patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)) {
        patientSeries.descPatientScoreSeries();
        log("  Series " + scoredSeriesCount + ": Score decreased (status is not COMPLETE)");
        continue;
      }

      if (numberOfValidDoses(patientSeries) < mostValidDoses) {
        patientSeries.descPatientScoreSeries();
        log("  Series " + scoredSeriesCount + ": Score decreased (valid doses less than max)");
        continue;
      }
      patientSeries.incPatientScoreSeries();
      log("  Series " + scoredSeriesCount + ": Score increased (has most valid doses) - SELECTED");
      break;
    }
    log("Phase 2 complete. Patient series evaluation finished.");

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

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
  }

  private void printStandard(PrintWriter out) {
    out.println(
        "<p>Complete  patient  series  provides  the  decision  table  for  determining  the  "
            + "number  of  points  to  assign  to  a complete patient series based on a specified condition. </p>");
    printTable(out);
    printBestPatientSeries(out);
  }

  private void printTable(PrintWriter out) {
    out.println("");
    out.println("<table BORDER=\"1\"> ");
    out.println("  <tr> ");
    out.println(" <th> Conditions </th> ");
    out.println(" <th> If this condition is true for the candidate patient series </th> ");
    out.println(" <th>If this condition is true for two or more candidate patient series </th> ");
    out.println(" <th>If this condition is not true for the candidate patient serie </th> ");
    out.println("  </tr> ");
    out.println("  <tr> ");
    out.println(" <td >A candidate patient series has the most valid doses.</th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> 0</td> ");
    out.println(" <td align=\"center\"> -1 </td> ");
    out.println("  </tr> ");
    out.println("</table>");

  }

}
