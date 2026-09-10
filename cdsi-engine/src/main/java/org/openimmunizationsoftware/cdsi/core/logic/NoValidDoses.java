package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;

/**
 * 8.6 No Valid Doses (Table 8-11). Method names are part of the Role A
 * contract: tests invoke each row reflectively. Gender-match and maximum-age
 * methods are retained as no-ops (SPEC-4.6-0007): Table 8-11 does not define
 * them, and {@code evalTable()} does not call them.
 */
public class NoValidDoses extends LogicStep {

  public NoValidDoses(DataModel dataModel) {
    super(LogicStepType.NO_VALID_DOSES, dataModel);
  }

  private List<PatientSeries> scorablePatientSeriesList() {
    return dataModel.getScorablePatientSeriesList();
  }

  /**
   * Table 8-11 row 1: can start earliest (SELECTB-14) — forecast earliest date.
   */
  private void evaluate_AScorablePatientSeriesCanStartEarliest() {
    PatientSeriesScoring.<Date>scoreExtremum(scorablePatientSeriesList(), patientSeries -> {
      Forecast forecast = patientSeries.getForecast();
      return forecast == null ? null : forecast.getEarliestDate();
    }, Comparator.<Date>reverseOrder(), 1);
  }

  /**
   * Table 8-11 row 2: completable (SELECTB-3 / SELECTB-12). A series with no
   * forecast is not scored by this row at all ({@code NoValidDosesCompletableTest
   * #seriesWithoutForecastIsNotScored}), unlike 8.5 which treats the same shape
   * as "not true".
   */
  private void evaluate_ACandidatePatientSeriesIsCompletable() {
    Date dateOfBirth = PatientSeriesScoring.dateOfBirth(dataModel);
    for (PatientSeries patientSeries : scorablePatientSeriesList()) {
      if (patientSeries.getForecast() == null) {
        continue;
      }
      if (PatientSeriesScoring.isCompletable(patientSeries, dateOfBirth)) {
        patientSeries.addScore(1);
      } else {
        patientSeries.addScore(-1);
      }
    }
  }

  /**
   * Retained for Role A reflection. Table 8-11 defines no gender-match
   * condition (SPEC-4.6-0007); awarding points here would be a fourth row.
   */
  private void evaluate_ACandidatePatientSeriesGenderSpecific() {
    // intentionally empty
  }

  /**
   * Table 8-11 row 3: product patient series is a penalty (−1 / n/a / +1).
   */
  private void evaluate_ACandidatePatientSeriesIsAProductPatientSeries() {
    PatientSeriesScoring.scoreIndependent(scorablePatientSeriesList(),
        patientSeries -> !patientSeries.isProductPatientSeries(), 1);
  }

  /**
   * Retained for Role A reflection. Table 8-11 defines no maximum-age-to-start
   * condition (SPEC-4.6-0007).
   */
  private void evaluate_ACandidatePatientSeriesHasExceededTheMaximumAge() {
    // intentionally empty
  }

  private void evalTable() {
    evaluate_AScorablePatientSeriesCanStartEarliest();
    evaluate_ACandidatePatientSeriesIsCompletable();
    evaluate_ACandidatePatientSeriesIsAProductPatientSeries();
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES);
    evaluateLogicTables();
    evalTable();
    return next();
  }
}
