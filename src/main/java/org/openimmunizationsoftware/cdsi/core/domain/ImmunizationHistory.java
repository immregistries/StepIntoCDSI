package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class ImmunizationHistory {
  private List<VaccineDoseAdministered> vaccineDoseAdministeredList =
      new ArrayList<VaccineDoseAdministered>();
  private PatientHistory patientHistory = null;
  private List<AdverseEvent> adverseEventList = new ArrayList<AdverseEvent>();

  public List<VaccineDoseAdministered> getVaccineDoseAdministeredList() {
    return vaccineDoseAdministeredList;
  }

  public void setVaccineDoseAdministeredList(
      List<VaccineDoseAdministered> vaccineDoseAdministeredList) {
    this.vaccineDoseAdministeredList = vaccineDoseAdministeredList;
  }

  public PatientHistory getPatientHistory() {
    return patientHistory;
  }

  public void setPatientHistory(PatientHistory medicalHistory) {
    this.patientHistory = medicalHistory;
  }

  public List<AdverseEvent> getAdverseEventList() {
    return adverseEventList;
  }

  public void setAdverseEventList(List<AdverseEvent> adverseEventList) {
    this.adverseEventList = adverseEventList;
  }
}
