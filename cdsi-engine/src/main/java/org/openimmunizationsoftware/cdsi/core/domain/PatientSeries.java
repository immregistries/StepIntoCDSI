package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.List;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class PatientSeries {
  private PatientSeriesStatus patientSeriesStatus = null;
  private AntigenSeries trackedAntigenSeries = null;
  private int scorePatientSeries = 0;
  private List<TargetDose> targetDoseList = null;
  private Forecast forecast = null;

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

  public void resetScore() {
    scorePatientSeries = 0;
  }

  public int getValidDoseCount() {
    int validDoseCount = 0;
    if (targetDoseList != null) {
      for (TargetDose targetDose : targetDoseList) {
        if (targetDose.getTargetDoseStatus() == TargetDoseStatus.SATISFIED) {
          validDoseCount++;
        }
      }
    }
    return validDoseCount;
  }

  public int getNotSatisfiedDoseCount() {
    int notSatisfiedDoseCount = 0;
    if (targetDoseList != null) {
      for (TargetDose targetDose : targetDoseList) {
        if (targetDose.getTargetDoseStatus() == TargetDoseStatus.NOT_SATISFIED) {
          notSatisfiedDoseCount++;
        }
      }
    }
    return notSatisfiedDoseCount;
  }

  public boolean isProductPatientSeries() {
    return trackedAntigenSeries != null
        && trackedAntigenSeries.getSelectPatientSeries() != null
        && trackedAntigenSeries.getSelectPatientSeries().getProductPath() != null
        && trackedAntigenSeries.getSelectPatientSeries().getProductPath().name().equals("YES");
  }

  public java.util.Date getMaximumAgeDateOfLastTargetDose(java.util.Date dateOfBirth) {
    if (dateOfBirth == null || targetDoseList == null || targetDoseList.isEmpty()) {
      return null;
    }
    TargetDose lastTargetDose = targetDoseList.get(targetDoseList.size() - 1);
    if (lastTargetDose.getTrackedSeriesDose() == null
        || lastTargetDose.getTrackedSeriesDose().getAgeList().isEmpty()
        || lastTargetDose.getTrackedSeriesDose().getAgeList().get(0).getMaximumAge() == null) {
      return null;
    }
    return lastTargetDose.getTrackedSeriesDose().getAgeList().get(0).getMaximumAge().getDateFrom(dateOfBirth);
  }

  public void addScore(int value) {

    scorePatientSeries = scorePatientSeries + value;
  }

  @Override
  public String toString() {
    return this.getTrackedAntigenSeries().getTargetDisease() + ": " + this.getTrackedAntigenSeries().getSeriesName();
  }

}
