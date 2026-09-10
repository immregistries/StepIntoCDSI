package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

/**
 * 8.1 Pre-filter Patient Series (Table 8-2: SELECTB-24, SELECTSCORE-2).
 */
public class PreFilterPatientSeries extends LogicStep {

  public PreFilterPatientSeries(DataModel dataModel) {
    super(LogicStepType.PRE_FILTER_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    List<PatientSeries> inScope = seriesInScope();
    log("8.1 scope antigen="
        + (dataModel.getAntigen() == null ? "null" : dataModel.getAntigen().getName())
        + " seriesGroup=" + dataModel.getCurrentSeriesGroup()
        + " selected=" + dataModel.getSelectedPatientSeriesList().size()
        + " inScope=" + inScope.size());

    List<PatientSeries> candidatePatientSeriesList = new ArrayList<PatientSeries>();
    log("Adding all non-contraindicated schedules");
    for (PatientSeries patientSeries : inScope) {
      // SELECTB-24 is a negative test: not Contraindicated. A null status is not
      // Contraindicated. Null-safe equals so a series Chapter 7 never statused
      // is still a candidate (SPEC-4.6-0025).
      if (!PatientSeriesStatus.CONTRAINDICATED.equals(patientSeries.getPatientSeriesStatus())) {
        log(" - Adding " + seriesNameOf(patientSeries));
        candidatePatientSeriesList.add(patientSeries);
      }
    }
    if (candidatePatientSeriesList.isEmpty()) {
      log("No schedules added, adding all contraindicated schedules");
      for (PatientSeries patientSeries : inScope) {
        if (PatientSeriesStatus.CONTRAINDICATED.equals(patientSeries.getPatientSeriesStatus())) {
          log(" - Adding " + seriesNameOf(patientSeries));
          candidatePatientSeriesList.add(patientSeries);
        }
      }
    }
    log("Number of candidate patient series: " + candidatePatientSeriesList.size());

    List<PatientSeries> scorablePatientSeriesList = new ArrayList<PatientSeries>();
    dataModel.setScorablePatientSeriesList(scorablePatientSeriesList);

    String highestRiskPriority = null;
    for (PatientSeries patientSeries : candidatePatientSeriesList) {
      AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
      if (antigenSeries == null || antigenSeries.getSeriesType() != SeriesType.RISK
          || antigenSeries.getSelectPatientSeries() == null) {
        continue;
      }
      String seriesPriority = antigenSeries.getSelectPatientSeries().getSeriesPriority();
      if (seriesPriority != null
          && (highestRiskPriority == null || highestRiskPriority.compareTo(seriesPriority) > 0)) {
        highestRiskPriority = seriesPriority;
      }
    }
    log("Highest risk priority: " + highestRiskPriority);

    boolean groupHasAValidDose = false;
    boolean groupHasADefaultSeries = false;
    for (PatientSeries patientSeries : candidatePatientSeriesList) {
      if (hasAValidDose(patientSeries)) {
        groupHasAValidDose = true;
      }
      if (isDefaultStandard(patientSeries)) {
        groupHasADefaultSeries = true;
      }
    }

    boolean addedDefault = false;
    log("Looking to add relevant patient series from candidate list");
    for (PatientSeries patientSeries : candidatePatientSeriesList) {
      boolean add = isScorable(patientSeries, highestRiskPriority, groupHasAValidDose, groupHasADefaultSeries);
      String logString = "[" + seriesTypeOf(patientSeries) + "] " + seriesNameOf(patientSeries);
      if (add) {
        patientSeries.resetScore();
        scorablePatientSeriesList.add(patientSeries);
        if (isDefaultStandard(patientSeries)) {
          addedDefault = true;
        }
        log(" + " + logString);
      } else {
        log(" - " + logString);
      }
    }

    log("Added default: " + addedDefault);
    log("Group has a valid dose: " + groupHasAValidDose);
    // SELECTSCORE-2 never says how a group's default series becomes scorable.
    // The implementation keeps the historical fallback: if nothing qualified
    // and the group has no valid dose, add the default Standard series.
    // Recorded as SPECIFICATION_AMBIGUITY rather than treated as a defect.
    if (!addedDefault && !groupHasAValidDose) {
      log("Need to add default series");
      for (PatientSeries patientSeries : candidatePatientSeriesList) {
        if (isDefaultStandard(patientSeries)) {
          patientSeries.resetScore();
          scorablePatientSeriesList.add(patientSeries);
          log(" + [" + seriesTypeOf(patientSeries) + "] " + seriesNameOf(patientSeries));
        }
      }
    }
    log("Final number of relevant patient series: " + scorablePatientSeriesList.size());

    setNextLogicStepType(LogicStepType.IDENTIFY_ONE_PRIORITIZED_PATIENT_SERIES);
    return next();
  }

  /**
   * Defensive copy of selectedPatientSeriesList restricted to the antigen (and
   * series group, when 4.5 has set one) currently being processed. Copilot's
   * commit-2 traces claimed a COVID series was visible during HIB scoring; even
   * if that was the global stepper rather than this list, 8.1 must not score a
   * foreign antigen.
   */
  private List<PatientSeries> seriesInScope() {
    List<PatientSeries> inScope = new ArrayList<PatientSeries>();
    for (PatientSeries patientSeries : dataModel.getSelectedPatientSeriesList()) {
      if (patientSeries == null || patientSeries.getTrackedAntigenSeries() == null) {
        continue;
      }
      if (dataModel.getAntigen() != null
          && !PatientSeriesScoring.belongsToCurrentAntigen(patientSeries, dataModel.getAntigen())) {
        log("ALERT.SCOPE: dropping foreign antigen series from 8.1 input: " + seriesNameOf(patientSeries)
            + " antigen=" + patientSeries.getTrackedAntigenSeries().getTargetDisease());
        continue;
      }
      if (dataModel.getCurrentSeriesGroup() != null) {
        String seriesGroup = SelectBestPatientSeries.seriesGroupOf(patientSeries.getTrackedAntigenSeries());
        if (!dataModel.getCurrentSeriesGroup().equals(seriesGroup)) {
          log("ALERT.SCOPE: dropping foreign series group from 8.1 input: " + seriesNameOf(patientSeries)
              + " group=" + seriesGroup);
          continue;
        }
      }
      inScope.add(patientSeries);
    }
    return inScope;
  }

  private boolean isScorable(PatientSeries patientSeries, String highestRiskPriority, boolean groupHasAValidDose,
      boolean groupHasADefaultSeries) {
    AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
    if (antigenSeries == null || antigenSeries.getSeriesType() == null) {
      return false;
    }
    switch (antigenSeries.getSeriesType()) {
      case RISK:
        String seriesPriority = antigenSeries.getSelectPatientSeries() == null ? null
            : antigenSeries.getSelectPatientSeries().getSeriesPriority();
        return highestRiskPriority == null || highestRiskPriority.equals(seriesPriority);
      case STANDARD:
        if (hasAValidDose(patientSeries) && administeredBeforeMaximumAgeToStart(patientSeries)) {
          return true;
        }
        // SELECTSCORE-2 third bullet: Standard candidate, 0 valid doses across
        // the whole group, and no default series for the group.
        if (!groupHasAValidDose && !groupHasADefaultSeries) {
          return true;
        }
        return false;
      case EVALUATION_ONLY:
        return PatientSeriesStatus.COMPLETE.equals(patientSeries.getPatientSeriesStatus());
      default:
        return false;
    }
  }

  private boolean hasAValidDose(PatientSeries patientSeries) {
    if (patientSeries.getTargetDoseList() == null) {
      return false;
    }
    for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
      if (targetDose.getTargetDoseStatus() != TargetDoseStatus.SATISFIED) {
        continue;
      }
      Evaluation evaluation = targetDose.getEvaluation();
      if (evaluation != null && evaluation.getEvaluationStatus() == EvaluationStatus.VALID) {
        return true;
      }
    }
    return false;
  }

  private boolean administeredBeforeMaximumAgeToStart(PatientSeries patientSeries) {
    SelectPatientSeries selectPatientSeries = patientSeries.getTrackedAntigenSeries() == null ? null
        : patientSeries.getTrackedAntigenSeries().getSelectPatientSeries();
    if (selectPatientSeries == null) {
      return true;
    }
    TimePeriod maxAgeToStart = selectPatientSeries.getMaxAgeToStart();
    if (maxAgeToStart == null || !maxAgeToStart.isValued()) {
      return true;
    }
    Date dateOfBirth = PatientSeriesScoring.dateOfBirth(dataModel);
    if (dateOfBirth == null) {
      return true;
    }
    Date maximumAgeToStartDate = maxAgeToStart.getDateFrom(dateOfBirth);
    Date earliestValid = patientSeries.getEarliestValidAdministeredDate();
    if (earliestValid == null || maximumAgeToStartDate == null) {
      return true;
    }
    return earliestValid.before(maximumAgeToStartDate);
  }

  private static boolean isDefaultStandard(PatientSeries patientSeries) {
    AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
    return antigenSeries != null && antigenSeries.getSeriesType() == SeriesType.STANDARD
        && antigenSeries.getSelectPatientSeries() != null
        && antigenSeries.getSelectPatientSeries().getDefaultSeries() == YesNo.YES;
  }

  private static String seriesNameOf(PatientSeries patientSeries) {
    if (patientSeries.getTrackedAntigenSeries() == null) {
      return "(no series)";
    }
    return patientSeries.getTrackedAntigenSeries().getSeriesName();
  }

  private static SeriesType seriesTypeOf(PatientSeries patientSeries) {
    if (patientSeries.getTrackedAntigenSeries() == null) {
      return null;
    }
    return patientSeries.getTrackedAntigenSeries().getSeriesType();
  }
}
