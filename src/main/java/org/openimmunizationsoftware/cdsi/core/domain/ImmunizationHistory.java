package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class ImmunizationHistory {
  private List<VaccineDoseAdministered> vaccineDoseAdministeredList =
      new ArrayList<VaccineDoseAdministered>();
  private MedicalHistory medicalHistory = null;
  private List<AdverseReaction> adverseReactionList = new ArrayList<AdverseReaction>();

  public List<VaccineDoseAdministered> getVaccineDoseAdministeredList() {
    return vaccineDoseAdministeredList;
  }

  public void setVaccineDoseAdministeredList(
      List<VaccineDoseAdministered> vaccineDoseAdministeredList) {
    this.vaccineDoseAdministeredList = vaccineDoseAdministeredList;
  }

  public MedicalHistory getMedicalHistory() {
    return medicalHistory;
  }

  public void setMedicalHistory(MedicalHistory medicalHistory) {
    this.medicalHistory = medicalHistory;
  }

  public List<AdverseReaction> getAdverseReactionList() {
    return adverseReactionList;
  }

  public void setAdverseReactionList(List<AdverseReaction> adverseReactionList) {
    this.adverseReactionList = adverseReactionList;
  }
}
