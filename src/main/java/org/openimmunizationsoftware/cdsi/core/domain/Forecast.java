package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Forecast {
  private Date adjustedRecommendedDate = null;
  private Date adjustedPastDueDate = null;
  private Date earliestDate = null;
  private String forecastReason = "";
  private Date latestDate = null;
  private Date unadjustedRecommendedDate = null;
  private Date unadjustedPastDueDate = null;
  private VaccineGroupForecast vaccineGroupForecast = null;
  private Date assessmentDate = null;
  private Antigen antigen = null;
  private TargetDose targetDose = null;
  private boolean bestPatientSeries = false;
  private Interval interval = null;
  private List<AdministrativeGuidance> administrativeGuidanceList =
      new ArrayList<AdministrativeGuidance>();
  private Vaccine recommendedVaccine = null;
  private Patient patient = null;

  public List<AdministrativeGuidance> getAdministrativeGuidanceList() {
    return administrativeGuidanceList;
  }

  public Interval getInterval() {
    return interval;
  }

  public void setInterval(Interval interval) {
    this.interval = interval;
  }

  public TargetDose getTargetDose() {
    return targetDose;
  }

  public void setTargetDose(TargetDose targetDose) {
    this.targetDose = targetDose;
  }

  public Antigen getAntigen() {
    return antigen;
  }

  public void setAntigen(Antigen antigen) {
    this.antigen = antigen;
  }

  public Date getAdjustedRecommendedDate() {
    return adjustedRecommendedDate;
  }

  public void setAdjustedRecommendedDate(Date adjustedRecommendedDate) {
    this.adjustedRecommendedDate = adjustedRecommendedDate;
  }

  public Date getAdjustedPastDueDate() {
    return adjustedPastDueDate;
  }

  public void setAdjustedPastDueDate(Date adjustedPastDueDate) {
    this.adjustedPastDueDate = adjustedPastDueDate;
  }

  public Date getEarliestDate() {
    return earliestDate;
  }

  public void setEarliestDate(Date earliestDate) {
    this.earliestDate = earliestDate;
  }

  public String getForecastReason() {
    return forecastReason;
  }

  public void setForecastReason(String forecastReason) {
    this.forecastReason = forecastReason;
  }

  public Date getLatestDate() {
    return latestDate;
  }

  public void setLatestDate(Date latestDate) {
    this.latestDate = latestDate;
  }

  public Date getUnadjustedRecommendedDate() {
    return unadjustedRecommendedDate;
  }

  public void setUnadjustedRecommendedDate(Date unadjustedRecommendedDate) {
    this.unadjustedRecommendedDate = unadjustedRecommendedDate;
  }

  public Date getUnadjustedPastDueDate() {
    return unadjustedPastDueDate;
  }

  public void setUnadjustedPastDueDate(Date unadjustedPastDueDate) {
    this.unadjustedPastDueDate = unadjustedPastDueDate;
  }

  public VaccineGroupForecast getVaccineGroupForecast() {
    return vaccineGroupForecast;
  }

  public void setVaccineGroupForecast(VaccineGroupForecast vaccineGroupForecast) {
    this.vaccineGroupForecast = vaccineGroupForecast;
  }

  public Date getAssessmentDate() {
    return assessmentDate;
  }

  public void setAssessmentDate(Date assessmentDate) {
    this.assessmentDate = assessmentDate;
  }

  public boolean isBestPatientSeries() {
    return bestPatientSeries;
  }

  public void setBestPatientSeries(boolean bestPatientSeries) {
    this.bestPatientSeries = bestPatientSeries;
  }

  public Vaccine getRecommendedVaccine() {
    return recommendedVaccine;
  }

  public void setRecommendedVaccine(Vaccine recommendedVaccine) {
    this.recommendedVaccine = recommendedVaccine;
  }

  public Patient getPatient() {
    return patient;
  }

  public void setPatient(Patient patient) {
    this.patient = patient;
  }

}
