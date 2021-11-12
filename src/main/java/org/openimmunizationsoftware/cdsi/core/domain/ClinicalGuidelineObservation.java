package org.openimmunizationsoftware.cdsi.core.domain;

public class ClinicalGuidelineObservation {
  private String observationCode = "";
  private String observationTitle = "";
  private PatientObservation patientObservation = null;
  private Contraindication contraindication = null;
  private Immunity immunity = null;
  private Indication indication = null;

  public String getObservationCode() {
    return observationCode;
  }

  public void setObservationCode(String observationCode) {
    this.observationCode = observationCode;
  }

  public String getObservationTitle() {
    return observationTitle;
  }

  public void setObservationTitle(String observationTitle) {
    this.observationTitle = observationTitle;
  }

  public Contraindication getContraindication() {
    return contraindication;
  }

  public void setContraindication(Contraindication contraindication) {
    this.contraindication = contraindication;
  }

  public Immunity getImmunity() {
    return immunity;
  }

  public void setImmunity(Immunity immunity) {
    this.immunity = immunity;
  }

  public Indication getIndication() {
    return indication;
  }

  public void setIndication(Indication indication) {
    this.indication = indication;
  }

  public PatientObservation getPatientObservation() {
    return patientObservation;
  }

  public void setPatientObservation(PatientObservation patientObservation) {
    this.patientObservation = patientObservation;
  }
}
