package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class TargetDose
{
  private TargetDoseStatus targetDoseStatus = null;
  private SeriesDose trackedSeriesDose = null;
  private VaccineDoseAdministered satisifiedByVaccineDoseAdministered = null;

  public TargetDose() {
    // default
  }
  
  public TargetDose(SeriesDose seriesDose) {
    this.trackedSeriesDose = seriesDose;
  }
  
  public TargetDose(TargetDose targetDose) {
    this.trackedSeriesDose = targetDose.getTrackedSeriesDose();
  }
  
  public VaccineDoseAdministered getSatisifiedByVaccineDoseAdministered() {
    return satisifiedByVaccineDoseAdministered;
  }

  public void setSatisifiedByVaccineDoseAdministered(VaccineDoseAdministered satisifiedByVaccineDoseAdministered) {
    this.satisifiedByVaccineDoseAdministered = satisifiedByVaccineDoseAdministered;
  }


  public TargetDoseStatus getTargetDoseStatus() {
    return targetDoseStatus;
  }

  public void setTargetDoseStatus(TargetDoseStatus targetDoseStatus) {
    this.targetDoseStatus = targetDoseStatus;
  }

  public SeriesDose getTrackedSeriesDose() {
    return trackedSeriesDose;
  }

  public void setTrackedSeriesDose(SeriesDose trackedSeriesDose) {
    this.trackedSeriesDose = trackedSeriesDose;
  }

}
