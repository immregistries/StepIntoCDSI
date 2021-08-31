package org.openimmunizationsoftware.cdsi.core.domain;

public class RelevantMedicalObservation {
  private PatientHistory medicalHistory = null;

  public PatientHistory getMedicalHistory() {
    return medicalHistory;
  }

  public void setMedicalHistory(PatientHistory medicalHistory) {
    this.medicalHistory = medicalHistory;
  }
}
