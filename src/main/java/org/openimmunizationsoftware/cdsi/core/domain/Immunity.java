package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class Immunity {
  private ClinicalGuidelineObservation clinicalGuidelineObservation = null;
  private Schedule schedule = null;
  private List<BirthDateImmunity> birthDateImmunityList = new ArrayList<BirthDateImmunity>();
  private List<ClinicalHistoryImmunity> clinicalHistoryList = new ArrayList<ClinicalHistoryImmunity>();
  private List<Antigen> antigenList = new ArrayList<Antigen>();
  
  public List<Antigen> getAntigenList() {
    return antigenList;
  }

  public ClinicalGuidelineObservation getClinicalGuidelineObservation() {
    return clinicalGuidelineObservation;
  }

  public void setClinicalGuidelineObservation(
      ClinicalGuidelineObservation clinicalGuidelineObservation) {
    this.clinicalGuidelineObservation = clinicalGuidelineObservation;
  }

  public Schedule getSchedule() {
    return schedule;
  }

  public void setSchedule(Schedule schedule) {
    this.schedule = schedule;
  }

  public List<ClinicalHistoryImmunity> getClinicalHistoryList() {
    return clinicalHistoryList;
  }

  public List<BirthDateImmunity> getBirthDateImmunityList() {
    return birthDateImmunityList;
  }
}
