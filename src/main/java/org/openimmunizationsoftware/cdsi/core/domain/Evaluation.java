package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;

public class Evaluation {
  private EvaluationStatus evaluationStatus = null;
  private EvaluationReason evaluationReason = null;
  private VaccineDoseAdministered vaccineDoseAdministered = null;
  private Antigen antigen = null;
  private TargetDose targetDose = null;

  public VaccineDoseAdministered getVaccineDoseAdministered() {
    return vaccineDoseAdministered;
  }

  public void setVaccineDoseAdministered(VaccineDoseAdministered vaccineDoseAdministered) {
    this.vaccineDoseAdministered = vaccineDoseAdministered;
  }

  public Antigen getAntigen() {
    return antigen;
  }

  public void setAntigen(Antigen antigen) {
    this.antigen = antigen;
  }

  public EvaluationStatus getEvaluationStatus() {
    return evaluationStatus;
  }

  public void setEvaluationStatus(EvaluationStatus evaluationStatus) {
    this.evaluationStatus = evaluationStatus;
  }

  public EvaluationReason getEvaluationReason() {
    return evaluationReason;
  }

  public void setEvaluationReason(EvaluationReason evaluationReason) {
    this.evaluationReason = evaluationReason;
  }

  public TargetDose getTargetDose() {
    return targetDose;
  }

  public void setTargetDose(TargetDose targetDose) {
    this.targetDose = targetDose;
  }
}
