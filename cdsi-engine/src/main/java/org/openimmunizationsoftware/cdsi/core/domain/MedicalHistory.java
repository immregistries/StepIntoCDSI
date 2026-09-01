package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MedicalHistory {
  private ImmunizationHistory immunizationHistory = null;
  private List<PatientObservation> patientObservationList = new ArrayList<PatientObservation>();
  private Set<Contraindication_TO_BE_REMOVED> contraindicationSet = new HashSet<Contraindication_TO_BE_REMOVED>();

  public Set<Contraindication_TO_BE_REMOVED> getContraindicationSet() {
    return contraindicationSet;
  }

  public ImmunizationHistory getImmunizationHistory() {
    return immunizationHistory;
  }

  public void setImmunizationHistory(ImmunizationHistory immunizationHistory) {
    this.immunizationHistory = immunizationHistory;
  }

  public List<PatientObservation> getPatientObservationList() {
    return patientObservationList;
  }

  public void setPatientObservationList(
      List<PatientObservation> patientObservationList) {
    this.patientObservationList = patientObservationList;
  }
}
