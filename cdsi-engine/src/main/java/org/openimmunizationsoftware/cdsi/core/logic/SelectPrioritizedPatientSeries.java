package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

/**
 * 8.7 Select Prioritized Patient Series (Table 8-13, SELECTBEST-1/2). An empty
 * series preference is "no preference" however the String was built — not a
 * reference comparison against the interned {@code ""}.
 */
public class SelectPrioritizedPatientSeries extends LogicStep {

  private PatientSeries prioritizedPatientSeries = null;

  public SelectPrioritizedPatientSeries(DataModel dataModel) {
    super(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES, dataModel);
    setConditionTableName("Table 8-13");
  }

  public List<PatientSeries> getPatientSeriesList() {
    return dataModel.getScorablePatientSeriesList();
  }

  public PatientSeries getPrioritizedPatientSeries() {
    return prioritizedPatientSeries;
  }

  private void selectPrioritizedPatientSeries() {
    List<PatientSeries> patientSeriesList = dataModel.getScorablePatientSeriesList();
    if (patientSeriesList == null || patientSeriesList.isEmpty()) {
      prioritizedPatientSeries = null;
      return;
    }
    prioritizedPatientSeries = patientSeriesList.get(0);
    for (PatientSeries patientSeries : patientSeriesList) {
      int score = patientSeries.getScorePatientSeries();
      int bestScore = prioritizedPatientSeries.getScorePatientSeries();
      if (score > bestScore) {
        prioritizedPatientSeries = patientSeries;
      } else if (score == bestScore) {
        Integer currentPreference = seriesPreferenceRank(patientSeries);
        Integer bestPreference = seriesPreferenceRank(prioritizedPatientSeries);
        if (currentPreference != null && bestPreference != null && currentPreference < bestPreference) {
          prioritizedPatientSeries = patientSeries;
        }
      }
    }
  }

  /**
   * SELECTBEST-2: a declared series preference is a trim-able integer. Null,
   * blank, and whitespace-only values (including a non-interned empty string
   * from {@code "  ".trim()}) are no preference.
   */
  private static Integer seriesPreferenceRank(PatientSeries patientSeries) {
    if (patientSeries.getTrackedAntigenSeries() == null
        || patientSeries.getTrackedAntigenSeries().getSelectPatientSeries() == null) {
      return null;
    }
    SelectPatientSeries selectPatientSeries = patientSeries.getTrackedAntigenSeries().getSelectPatientSeries();
    String seriesPreference = selectPatientSeries.getSeriesPreference();
    if (seriesPreference == null) {
      return null;
    }
    String trimmed = seriesPreference.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    try {
      return Integer.valueOf(trimmed);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  public LogicStep process() throws Exception {
    selectPrioritizedPatientSeries();
    if (prioritizedPatientSeries != null) {
      dataModel.getPrioritizedPatientSeriesList().add(prioritizedPatientSeries);
    }
    setNextLogicStepType(LogicStepType.SELECT_NEXT_SERIES_GROUP);
    return next();
  }

  public int numberOfValidDoses(PatientSeries patientSeries) {
    int nbOfValidDoses = 0;
    if (patientSeries.getTargetDoseList() == null) {
      return nbOfValidDoses;
    }
    for (TargetDose target : patientSeries.getTargetDoseList()) {
      if (TargetDoseStatus.SATISFIED.equals(target.getTargetDoseStatus())) {
        nbOfValidDoses++;
      }
    }
    return nbOfValidDoses;
  }
}
