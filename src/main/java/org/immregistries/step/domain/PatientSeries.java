package org.immregistries.step.domain;

import java.util.List;
import org.immregistries.step.core.domain.datatypes.PatientSeriesStatus;

public class PatientSeries {
  private PatientSeriesStatus patientSeriesStatus = null;
  private AntigenSeries trackedAntigenSeries = null;
  private int scorePatientSeries = 0;
  private List<TargetDose> targetDoseList = null;
  private Forecast forecast = null;
  private Patient patient = null;

  public Forecast getForecast() {
    return forecast;
  }

  public void setForecast(Forecast forecast) {
    this.forecast = forecast;
  }

  public List<TargetDose> getTargetDoseList() {
    return targetDoseList;
  }

  public void setTargetDoseList(List<TargetDose> targetDoseList) {
    this.targetDoseList = targetDoseList;
  }

  public PatientSeries() {
    // default;
  }

  public PatientSeries(AntigenSeries trackedAntigenSeries) {
    this.trackedAntigenSeries = trackedAntigenSeries;
  }

  public PatientSeriesStatus getPatientSeriesStatus() {
    return patientSeriesStatus;
  }

  public void setPatientSeriesStatus(PatientSeriesStatus patientSeriesStatus) {
    this.patientSeriesStatus = patientSeriesStatus;
  }

  public AntigenSeries getTrackedAntigenSeries() {
    return trackedAntigenSeries;
  }

  public void setTrackedAntigenSeries(AntigenSeries trackedAntigenSeries) {
    this.trackedAntigenSeries = trackedAntigenSeries;
  }

  public void setScorePatientSeriesScore(int scorePatientSerie) {
    this.scorePatientSeries = scorePatientSerie;
  }

  public void incPatientScoreSeries() {
    scorePatientSeries++;
  }

  public void descPatientScoreSeries() {
    scorePatientSeries--;
  }


  public int getScorePatientSeries() {
    return scorePatientSeries;
  }

  public void addScore(int value) {

    scorePatientSeries = scorePatientSeries + value;
  }

  @Override
  public String toString() {
    return this.getTrackedAntigenSeries().getSeriesName();
  }

  public Patient getPatient() {
    return patient;
  }

  public void setPatient(Patient patient) {
    this.patient = patient;
  }

}
