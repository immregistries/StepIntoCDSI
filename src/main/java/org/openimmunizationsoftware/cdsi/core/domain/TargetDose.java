package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class TargetDose
{
  private TargetDoseStatus targetDoseStatus = null;
  private SeriesDose trackedSeriesDose = null;
  private VaccineDoseAdministered satisfiedByVaccineDoseAdministered = null;
  private String statusCause = "";
  
  public TargetDose() {
    // default
  }
  
  public TargetDose(SeriesDose seriesDose) {
    this.trackedSeriesDose = seriesDose;
  }
  
  public TargetDose(TargetDose targetDose) {
    this.trackedSeriesDose = targetDose.getTrackedSeriesDose();
  }
  
  public VaccineDoseAdministered getSatisfiedByVaccineDoseAdministered() {
    return satisfiedByVaccineDoseAdministered;
  }

  public void setSatisfiedByVaccineDoseAdministered(VaccineDoseAdministered satisfiedByVaccineDoseAdministered) {
    this.satisfiedByVaccineDoseAdministered = satisfiedByVaccineDoseAdministered;
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

public String getStatusCause() {
	return statusCause;
}

public void setStatusCause(String statusCause) {
	this.statusCause = statusCause;
}


}
