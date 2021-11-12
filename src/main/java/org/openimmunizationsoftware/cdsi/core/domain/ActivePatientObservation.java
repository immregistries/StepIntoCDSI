package org.openimmunizationsoftware.cdsi.core.domain;

public class ActivePatientObservation {
  public PatientObservation patientObservation = null;

  public PatientObservation getPatientObservation() {
    return patientObservation;
  }

  public void setPatientObservation(PatientObservation patientObservation) {
    this.patientObservation = patientObservation;
  }
}
