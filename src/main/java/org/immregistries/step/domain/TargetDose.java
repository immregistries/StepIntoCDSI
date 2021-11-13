package org.immregistries.step.domain;

import java.util.ArrayList;
import java.util.List;
import org.immregistries.step.core.domain.datatypes.TargetDoseStatus;

public class TargetDose {
  private TargetDoseStatus targetDoseStatus = TargetDoseStatus.NOT_SATISFIED;
  private SeriesDose trackedSeriesDose = null;
  private VaccineDoseAdministered satisfiedByVaccineDoseAdministered = null;
  private VaccineDoseAdministered evaluatedAgainstVaccineDoseAdministered = null;
  private String statusCause = "";
  private PatientSeries patientSeries = null;
  private List<Forecast> forecastList = new ArrayList<Forecast>();
  private List<Evaluation> evaluationList = new ArrayList<Evaluation>();

  public List<Evaluation> getEvaluationList() {
    return evaluationList;
  }

  public List<Forecast> getForecastList() {
    return forecastList;
  }

  public PatientSeries getPatientSeries() {
    return patientSeries;
  }

  public void setPatientSeries(PatientSeries patientSeries) {
    this.patientSeries = patientSeries;
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

  public VaccineDoseAdministered getEvaluatedAgainstVaccineDoseAdministered() {
    return evaluatedAgainstVaccineDoseAdministered;
  }

  public void setEvaluatedAgainstVaccineDoseAdministered(
      VaccineDoseAdministered evaluatedAgainstVaccineDoseAdministered) {
    this.evaluatedAgainstVaccineDoseAdministered = evaluatedAgainstVaccineDoseAdministered;
  }

}
