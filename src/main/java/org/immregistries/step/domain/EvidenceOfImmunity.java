package org.immregistries.step.domain;

public class EvidenceOfImmunity {

  private PatientHistory patientHistory = null;
  private Patient patient = null;

  public Patient getPatient() {
    return patient;
  }

  public void setPatient(Patient patient) {
    this.patient = patient;
  }

  public PatientHistory getPatientHistory() {
    return patientHistory;
  }

  public void setPatientHistory(PatientHistory patientHistory) {
    this.patientHistory = patientHistory;
  }
}
