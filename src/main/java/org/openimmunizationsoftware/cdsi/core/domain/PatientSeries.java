package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.List;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;

public class PatientSeries
{
  private PatientSeriesStatus patientSeriesStatus = null;
  private AntigenSeries trackedAntigenSeries = null;
  private int scorePatientSerie = 0;
  private List<TargetDose> targetDoseList = null;

  public List<TargetDose> getTargetDoseList() {
	return targetDoseList;
}

public void setTargetDoseList(List<TargetDose> targetDoseList) {
	this.targetDoseList = targetDoseList;
}

public PatientSeries()
  {
    // default;
  }
  
  public PatientSeries(AntigenSeries trackedAntigenSeries)
  {
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
  
  public void setScorePatientSeriesScore(int scorePatientSerie){
	  this.scorePatientSerie = scorePatientSerie;
  }
  
  public void incPatientScoreSeries(){
	  scorePatientSerie++;
  }
  
  public void descPatientScoreSeries(){
	  scorePatientSerie--;
  }
  
  
  public int getScorePatientSerie(){
	  return scorePatientSerie;
  }
  
  public void addScore(int value){
	  
	  scorePatientSerie = scorePatientSerie + value;
  }
}
