package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class MedicalHistory
{
  private ImmunizationHistory immunizationHistory = null;
  private List<RelevantMedicalObservation> releventMedicalObservationList = new ArrayList<RelevantMedicalObservation>();

  public ImmunizationHistory getImmunizationHistory() {
    return immunizationHistory;
  }

  public void setImmunizationHistory(ImmunizationHistory immunizationHistory) {
    this.immunizationHistory = immunizationHistory;
  }

  public List<RelevantMedicalObservation> getReleventMedicalObservationList() {
    return releventMedicalObservationList;
  }

  public void setReleventMedicalObservationList(List<RelevantMedicalObservation> releventMedicalObservationList) {
    this.releventMedicalObservationList = releventMedicalObservationList;
  }
}
