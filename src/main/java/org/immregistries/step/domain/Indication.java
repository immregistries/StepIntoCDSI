package org.immregistries.step.domain;

import java.util.ArrayList;
import java.util.List;

public class Indication {
  private String indicationTextDescription = "";
  private Age indicationBeginAge = null;
  private Age indicationEndAge = null;
  private ClinicalGuidelineObservation clinicalGuidelineObservation = null;
  private List<AntigenSeries> antigenSeriesList = new ArrayList<AntigenSeries>();
  private AdministrativeGuidance administrativeGuidance = null;

  public AdministrativeGuidance getAdministrativeGuidance() {
    return administrativeGuidance;
  }

  public void setAdministrativeGuidance(AdministrativeGuidance administrativeGuidance) {
    this.administrativeGuidance = administrativeGuidance;
  }

  public List<AntigenSeries> getAntigenSeriesList() {
    return antigenSeriesList;
  }

  public String getIndicationTextDescription() {
    return indicationTextDescription;
  }

  public void setIndicationTextDescription(String indicationTextDescription) {
    this.indicationTextDescription = indicationTextDescription;
  }

  public Age getIndicationBeginAge() {
    return indicationBeginAge;
  }

  public void setIndicationBeginAge(Age indicationBeginAge) {
    this.indicationBeginAge = indicationBeginAge;
  }

  public Age getIndicationEndAge() {
    return indicationEndAge;
  }

  public void setIndicationEndAge(Age indicationEndAge) {
    this.indicationEndAge = indicationEndAge;
  }

  public ClinicalGuidelineObservation getClinicalGuidelineObservation() {
    return clinicalGuidelineObservation;
  }

  public void setClinicalGuidelineObservation(
      ClinicalGuidelineObservation clinicalGuidelineObservation) {
    this.clinicalGuidelineObservation = clinicalGuidelineObservation;
  }
}
