package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.List;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

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
