package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Patient {
  private Date dateOfBirth = null;
  private String gender = "";
  private String countryOfBirth = "";
  private List<VaccineDoseAdministered> receivesList = new ArrayList<VaccineDoseAdministered>();
  private PatientHistory patientHistory = new PatientHistory();
  private List<EvidenceOfImmunity> hasProofOfList = new ArrayList<EvidenceOfImmunity>();

  public List<EvidenceOfImmunity> getHasProofOfList() {
    return hasProofOfList;
  }

  public PatientHistory getPatientHistory() {
    return patientHistory;
  }

  public void setPatientHistory(PatientHistory patientHistory) {
    this.patientHistory = patientHistory;
  }

  public Date getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getCountryOfBirth() {
    return countryOfBirth;
  }

  public void setCountryOfBirth(String countryOfBirth) {
    this.countryOfBirth = countryOfBirth;
  }

  public List<VaccineDoseAdministered> getReceivesList() {
    return receivesList;
  }

  public void setReceivesList(List<VaccineDoseAdministered> receivesList) {
    this.receivesList = receivesList;
  }
}
