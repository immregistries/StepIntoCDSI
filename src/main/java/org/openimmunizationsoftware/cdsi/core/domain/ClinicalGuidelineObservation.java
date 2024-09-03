package org.openimmunizationsoftware.cdsi.core.domain;

public class ClinicalGuidelineObservation extends Observation {
  private String observationCode = "";
  private String observationTitle = "";

  public String getObservationCode() {
      return observationCode;
  }

  public String getObservationTitle() {
      return observationTitle;
  }

  public void setObservationCode(String observationCode) {
      this.observationCode = observationCode;
  }

  public void setObservationTitle(String observationTitle) {
      this.observationTitle = observationTitle;
  }
}
