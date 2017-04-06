package org.openimmunizationsoftware.cdsi.core.domain;

public class RelevantMedicalObservation {
  private MedicalHistory medicalHistory = null;

  public MedicalHistory getMedicalHistory() {
    return medicalHistory;
  }

  public void setMedicalHistory(MedicalHistory medicalHistory) {
    this.medicalHistory = medicalHistory;
  }
}
