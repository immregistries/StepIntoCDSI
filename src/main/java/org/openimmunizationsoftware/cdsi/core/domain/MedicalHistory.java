package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class MedicalHistory
{
  private List<ImmunizationHistory> immunizationHistoryList = new ArrayList<ImmunizationHistory>();
  private List<RelevantMedicalObservation> releventMedicalObservationList = new ArrayList<RelevantMedicalObservation>();

  public List<ImmunizationHistory> getImmunizationHistoryList() {
    return immunizationHistoryList;
  }

  public void setImmunizationHistoryList(List<ImmunizationHistory> immunizationHistoryList) {
    this.immunizationHistoryList = immunizationHistoryList;
  }

  public List<RelevantMedicalObservation> getReleventMedicalObservationList() {
    return releventMedicalObservationList;
  }

  public void setReleventMedicalObservationList(List<RelevantMedicalObservation> releventMedicalObservationList) {
    this.releventMedicalObservationList = releventMedicalObservationList;
  }
}
