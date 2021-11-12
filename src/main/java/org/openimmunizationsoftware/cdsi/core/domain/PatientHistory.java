package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatientHistory {
  private ImmunizationHistory immunizationHistory = null;
  private List<PatientObservation> patientObservationList = new ArrayList<PatientObservation>();
  private List<RelevantMedicalObservation> releventMedicalObservationList =
      new ArrayList<RelevantMedicalObservation>();
  private Set<Contraindication> contraindicationSet = new HashSet<Contraindication>();
  
  public List<PatientObservation> getPatientObservationList() {
    return patientObservationList;
  }

  public void setPatientObservationList(List<PatientObservation> patientObservationList) {
    this.patientObservationList = patientObservationList;
  }

  @Deprecated
  public Set<Contraindication> getContraindicationSet() {
    return contraindicationSet;
  }

  public ImmunizationHistory getImmunizationHistory() {
    return immunizationHistory;
  }

  public void setImmunizationHistory(ImmunizationHistory immunizationHistory) {
    this.immunizationHistory = immunizationHistory;
  }

  public List<RelevantMedicalObservation> getReleventMedicalObservationList() {
    return releventMedicalObservationList;
  }

  public void setReleventMedicalObservationList(
      List<RelevantMedicalObservation> releventMedicalObservationList) {
    this.releventMedicalObservationList = releventMedicalObservationList;
  }
}
