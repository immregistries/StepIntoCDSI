package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.HashMap;
import java.util.Map;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class TargetDose {
  private TargetDoseStatus targetDoseStatus = TargetDoseStatus.NOT_SATISFIED;
  private SeriesDose trackedSeriesDose = null;
  private VaccineDoseAdministered satisfiedByVaccineDoseAdministered = null;
  private String statusCause = "";
  private Map<VaccineDoseAdministered, Evaluation> evaluationToVaccineDoseAdministeredMap = new HashMap<VaccineDoseAdministered, Evaluation>();

  public Evaluation getEvaluation() {
    if(satisfiedByVaccineDoseAdministered != null) {
      return evaluationToVaccineDoseAdministeredMap.get(satisfiedByVaccineDoseAdministered);
    }
    return null;
  }

  public Map<VaccineDoseAdministered, Evaluation> getEvaluationToVaccineDoseAdministeredMap() {
    return evaluationToVaccineDoseAdministeredMap;
  }

  public void setEvaluation(VaccineDoseAdministered vda, Evaluation evaluation) {
    evaluationToVaccineDoseAdministeredMap.put(vda,evaluation);
  }

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

  public void setSatisfiedByVaccineDoseAdministered(
      VaccineDoseAdministered satisfiedByVaccineDoseAdministered) {
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

  @Override
  public String toString() {
    if (trackedSeriesDose != null) {
      return trackedSeriesDose.toString();
    }
    // TODO Auto-generated method stub
    return super.toString();
  }

}
