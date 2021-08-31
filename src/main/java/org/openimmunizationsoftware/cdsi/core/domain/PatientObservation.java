package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;

public class PatientObservation extends Observation {
  private PatientHistory patientHistory = null;
  private Date ObservationDate = null;
  private ActivePatientObservation activePatientObservation = null;

  public PatientHistory getPatientHistory() {
    return patientHistory;
  }

  public void setPatientHistory(PatientHistory patientHistory) {
    this.patientHistory = patientHistory;
  }

  public Date getObservationDate() {
    return ObservationDate;
  }

  public void setObservationDate(Date observationDate) {
    ObservationDate = observationDate;
  }

  public ActivePatientObservation getActivePatientObservation() {
    return activePatientObservation;
  }

  public void setActivePatientObservation(ActivePatientObservation activePatientObservation) {
    this.activePatientObservation = activePatientObservation;
  }
}
