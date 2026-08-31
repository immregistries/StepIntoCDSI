package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class SelectPrioritizedPatientSeries extends LogicStep {

  private List<PatientSeries> patientSeriesList = dataModel.getSelectedPatientSeriesList();

  public SelectPrioritizedPatientSeries(DataModel dataModel) {
    super(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");

  }

  private PatientSeries prioritizedPatientSeries = null;

  public List<PatientSeries> getPatientSeriesList() {
    return patientSeriesList;
  }

  public PatientSeries getPrioritizedPatientSeries() {
    return prioritizedPatientSeries;
  }

  private void selectPrioritizedPatientSeries() {
    if (patientSeriesList.size() > 0) {
      prioritizedPatientSeries = patientSeriesList.get(0);
    }
    for (PatientSeries patientSeries : patientSeriesList) {
      if (patientSeries.getScorePatientSeries() == prioritizedPatientSeries.getScorePatientSeries()) {
        String currentSeriesPreference = patientSeries.getTrackedAntigenSeries().getSelectPatientSeries()
            .getSeriesPreference();
        String newSeriesPreference = prioritizedPatientSeries.getTrackedAntigenSeries().getSelectPatientSeries()
            .getSeriesPreference();
        if (currentSeriesPreference != "" && newSeriesPreference != "") {
          if (Integer.parseInt(currentSeriesPreference) < Integer.parseInt(newSeriesPreference)) {
            prioritizedPatientSeries = patientSeries;
          }
        }
      }

      if (patientSeries.getScorePatientSeries() > prioritizedPatientSeries.getScorePatientSeries()) {
        prioritizedPatientSeries = patientSeries;
      }
    }

  }

  @Override
  public LogicStep process() throws Exception {
    selectPrioritizedPatientSeries();
    if (prioritizedPatientSeries != null) {
      dataModel.getPrioritizedPatientSeriesList().add(prioritizedPatientSeries);
    }
    setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
    return next();
  }

  public int numberOfValidDoses(PatientSeries patientSeries) {
    int nbOfValidDoses = 0;
    for (TargetDose target : patientSeries.getTargetDoseList()) {
      if (target.getTargetDoseStatus() != null) {
        if (target.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {
          nbOfValidDoses++;
        }
      }

    }
    return nbOfValidDoses;
  }

}
