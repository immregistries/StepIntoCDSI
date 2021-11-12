package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class ImmunizationHistory {
  private List<VaccineDoseAdministered> vaccineDoseAdministeredList =
      new ArrayList<VaccineDoseAdministered>();
  private PatientHistory medicalHistory = null;
  private List<AdverseEvent> adverseEventList = new ArrayList<AdverseEvent>();

  public List<VaccineDoseAdministered> getVaccineDoseAdministeredList() {
    return vaccineDoseAdministeredList;
  }

  public void setVaccineDoseAdministeredList(
      List<VaccineDoseAdministered> vaccineDoseAdministeredList) {
    this.vaccineDoseAdministeredList = vaccineDoseAdministeredList;
  }

  public PatientHistory getMedicalHistory() {
    return medicalHistory;
  }

  public void setMedicalHistory(PatientHistory medicalHistory) {
    this.medicalHistory = medicalHistory;
  }

  public List<AdverseEvent> getAdverseEventList() {
    return adverseEventList;
  }

  public void setAdverseEventList(List<AdverseEvent> adverseEventList) {
    this.adverseEventList = adverseEventList;
  }
}
