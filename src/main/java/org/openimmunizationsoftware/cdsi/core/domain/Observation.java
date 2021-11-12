package org.openimmunizationsoftware.cdsi.core.domain;

public class Observation {
  private PatientObservation patientObservation = null;
  private ClinicalGuidelineObservation clinicalGuidelineObservation = null;

  public ClinicalGuidelineObservation getClinicalGuidelineObservation() {
    return clinicalGuidelineObservation;
  }

  public void setClinicalGuidelineObservation(
      ClinicalGuidelineObservation clinicalGuidelineObservation) {
    this.clinicalGuidelineObservation = clinicalGuidelineObservation;
  }

  public PatientObservation getPatientObservation() {
    return patientObservation;
  }

  public void setPatientObservation(PatientObservation patientObservation) {
    this.patientObservation = patientObservation;
  }
}
