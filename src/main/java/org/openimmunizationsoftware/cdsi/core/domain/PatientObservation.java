package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;

public class PatientObservation extends Observation {
  private Observation observation = null;
  private PatientHistory patientHistory = null;
  private Date ObservationDate = null;
  private ClinicalGuidelineObservation clinicalGuidelineObservation = null;

  public ClinicalGuidelineObservation getClinicalGuidelineObservation() {
    return clinicalGuidelineObservation;
  }

  public void setClinicalGuidelineObservation(
      ClinicalGuidelineObservation clinicalGuidelineObservation) {
    this.clinicalGuidelineObservation = clinicalGuidelineObservation;
  }

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

  public Observation getObservation() {
    return observation;
  }

  public void setObservation(Observation observation) {
    this.observation = observation;
  }
}
