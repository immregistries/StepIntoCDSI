package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class TargetDose {
  private TargetDoseStatus targetDoseStatus = TargetDoseStatus.NOT_SATISFIED;
  private SeriesDose trackedSeriesDose = null;
  private VaccineDoseAdministered satisfiedByVaccineDoseAdministered = null;
  private String statusCause = "";
  List<Evaluation> evaluationList = new ArrayList<>();

  public Evaluation getEvaluation() {
    if (evaluationList.size() > 0) {
      return evaluationList.get(evaluationList.size() - 1);
    }
    return null;
  }

  public List<Evaluation> getEvaluationList() {
    return evaluationList;
  }

  public void setEvaluation(Evaluation evaluation) {
    evaluationList.add(evaluation);
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
