package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

/**
 * 8.5 In-process Patient Series (Table 8-9). Method names are part of the
 * Role A contract: tests invoke each row reflectively.
 */
public class InProcessPatientSeries extends LogicStep {

  public InProcessPatientSeries(DataModel dataModel) {
    super(LogicStepType.IN_PROCESS_PATIENT_SERIES, dataModel);
    setConditionTableName("Table 8-9");
  }

  private List<PatientSeries> scorablePatientSeriesList() {
    return dataModel.getScorablePatientSeriesList();
  }

  /**
   * Table 8-9 is applied to in-process patient series only (SELECTB-16). A
   * scorable sibling with no satisfied target dose is outside this table and
   * keeps the score 8.1 left it holding.
   */
  private List<PatientSeries> inProcessScorablePatientSeriesList() {
    List<PatientSeries> inProcess = new ArrayList<PatientSeries>();
    for (PatientSeries patientSeries : scorablePatientSeriesList()) {
      if (isInProcess(patientSeries)) {
        inProcess.add(patientSeries);
      }
    }
    return inProcess;
  }

  private static boolean isInProcess(PatientSeries patientSeries) {
    if (!PatientSeriesStatus.NOT_COMPLETE.equals(patientSeries.getPatientSeriesStatus())) {
      return false;
    }
    if (patientSeries.getTargetDoseList() == null) {
      return false;
    }
    for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
      if (targetDose.getTargetDoseStatus() == TargetDoseStatus.SATISFIED) {
        return true;
      }
    }
    return false;
  }

  /**
   * Table 8-9 row 1: product patient series AND all administered doses Valid.
   * Remaining unevaluated target doses do not fail SELECTB-2.
   */
  private void evaluate_ACandidatePatientSeriesIsAProductPatientSeriesAndHasAllValidDoses() {
    PatientSeriesScoring.scoreIndependent(inProcessScorablePatientSeriesList(),
        patientSeries -> patientSeries.isProductPatientSeries() && patientSeries.hasAllValidAdministeredDoses(), 2);
  }

  /**
   * Table 8-9 row 2: completable (SELECTB-3 / SELECTB-12).
   */
  private void evaluate_ACandidatePatientSeriesIsCompletable() {
    Date dateOfBirth = PatientSeriesScoring.dateOfBirth(dataModel);
    PatientSeriesScoring.scoreIndependent(inProcessScorablePatientSeriesList(),
        patientSeries -> PatientSeriesScoring.isCompletable(patientSeries, dateOfBirth), 3);
  }

  /**
   * Table 8-9 row 3: most valid doses (SELECTB-19).
   */
  private void evaluate_ACandidatePatientSeriesHasTheMostValidDoses() {
    PatientSeriesScoring.scoreExtremum(inProcessScorablePatientSeriesList(), PatientSeries::getValidDoseCount,
        Comparator.naturalOrder(), 2);
  }

  /**
   * Table 8-9 row 4: closest to completion is fewest not-satisfied target doses
   * (SELECTB-5), not most valid doses.
   */
  private void evaluate_ACandidatePatientSeriesIsClosestToCompletion() {
    PatientSeriesScoring.scoreExtremum(inProcessScorablePatientSeriesList(), PatientSeries::getNotSatisfiedDoseCount,
        Comparator.reverseOrder(), 2);
  }

  /**
   * Table 8-9 row 5: can finish earliest. SELECTB-11 only compares completable
   * series; a non-completable series is "not true" (-1) and does not compete.
   * The date compared is SELECTB-12's forecast finish date, not latestDate.
   */
  private void evaluate_ACandidatePatientSeriesCanFinishEarliest() {
    Date dateOfBirth = PatientSeriesScoring.dateOfBirth(dataModel);
    PatientSeriesScoring.<Date>scoreExtremum(inProcessScorablePatientSeriesList(), patientSeries -> {
      if (!PatientSeriesScoring.isCompletable(patientSeries, dateOfBirth)) {
        return null;
      }
      return PatientSeriesScoring.forecastFinishDate(patientSeries);
    }, Comparator.<Date>reverseOrder(), 1);
  }

  private void evaluateTable() {
    evaluate_ACandidatePatientSeriesIsAProductPatientSeriesAndHasAllValidDoses();
    evaluate_ACandidatePatientSeriesIsCompletable();
    evaluate_ACandidatePatientSeriesHasTheMostValidDoses();
    evaluate_ACandidatePatientSeriesIsClosestToCompletion();
    evaluate_ACandidatePatientSeriesCanFinishEarliest();
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES);
    evaluateTable();
    return next();
  }
}
