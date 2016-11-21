package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class TargetDose
{
  private TargetDoseStatus targetDoseStatus = null;
  private SeriesDose trackedSeriesDose = null;
  private VaccineDoseAdministered satisfiedByVaccineDoseAdministered = null;
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
    System.out.println("88888888888888888888888888888888888888888888888888888888888888888888888888888888888888888");
    System.out.println(satisfiedByVaccineDoseAdministered);
    System.out.println("_________________________________________________________________________________________");
  }


  public TargetDoseStatus getTargetDoseStatus() {
	  System.out.println("00000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
	  System.out.println(targetDoseStatus);
	  System.out.println("00000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
    return targetDoseStatus;
  }

  public void setTargetDoseStatus(TargetDoseStatus targetDoseStatus) {
    this.targetDoseStatus = targetDoseStatus;
    System.out.println("----------------------------------------------------------------------------------------");
    System.out.println(targetDoseStatus);
    System.out.println("_________________________________________________________________________________________");
  }

  public SeriesDose getTrackedSeriesDose() {
    return trackedSeriesDose;
  }

  public void setTrackedSeriesDose(SeriesDose trackedSeriesDose) {
    this.trackedSeriesDose = trackedSeriesDose;
  }

}
