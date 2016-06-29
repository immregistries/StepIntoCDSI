package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;

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
  
}
