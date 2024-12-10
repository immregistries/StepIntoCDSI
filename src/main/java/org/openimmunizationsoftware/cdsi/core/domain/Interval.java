package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class Interval {
  private SeriesDose seriesDose = null;
  private YesNo fromImmediatePreviousDoseAdministered = null;
  private String fromTargetDoseNumberInSeries = "";
  private VaccineType fromMostRecentVaccineType = null;
  private ObservationCode fromRelevantObservation = null;
  private TimePeriod absoluteMinimumInterval = null;
  private TimePeriod minimumInterval = null;
  private TimePeriod earliestRecommendedInterval = null;
  private TimePeriod latestRecommendedInterval = null;
  private IntervalPriority intervalPriority = null;
  private TimePeriod effectiveDate = null;
  private TimePeriod cessationDate = null;

  public IntervalPriority getIntervalPriority() {
    return intervalPriority;
  }

  public void setIntervalPriority(IntervalPriority intervalPriority) {
    this.intervalPriority = intervalPriority;
  }

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

  public YesNo getFromImmediatePreviousDoseAdministered() {
    return fromImmediatePreviousDoseAdministered;
  }

  public void setFromImmediatePreviousDoseAdministered(
      YesNo fromImmediatePreviousDoseAdministered) {
    this.fromImmediatePreviousDoseAdministered = fromImmediatePreviousDoseAdministered;
  }

  public String getFromTargetDoseNumberInSeries() {
    return fromTargetDoseNumberInSeries;
  }

  public void setFromTargetDoseNumberInSeries(String fromTargetDoseNumberInSeries) {
    this.fromTargetDoseNumberInSeries = fromTargetDoseNumberInSeries;
  }

  public VaccineType getFromMostRecentVaccineType() {
    return fromMostRecentVaccineType;
  }

  public void setFromMostRecentVaccineType(VaccineType fromMostRecentVaccineType) {
    this.fromMostRecentVaccineType = fromMostRecentVaccineType;
  }

  public ObservationCode getFromRelevantObservation() {
    return fromRelevantObservation;
  }

  public void setFromRelevantObservation(ObservationCode fromRelevantObservation) {
    this.fromRelevantObservation = fromRelevantObservation;
  }

  public TimePeriod getAbsoluteMinimumInterval() {
    return absoluteMinimumInterval;
  }

  public void setAbsoluteMinimumInterval(TimePeriod absoluteMinimumInterval) {
    this.absoluteMinimumInterval = absoluteMinimumInterval;
  }

  public TimePeriod getMinimumInterval() {
    return minimumInterval;
  }

  public void setMinimumInterval(TimePeriod minimumInterval) {
    this.minimumInterval = minimumInterval;
  }

  public TimePeriod getEarliestRecommendedInterval() {
    return earliestRecommendedInterval;
  }

  public void setEarliestRecommendedInterval(TimePeriod earliestRecommendedInterval) {
    this.earliestRecommendedInterval = earliestRecommendedInterval;
  }

  public TimePeriod getLatestRecommendedInterval() {
    return latestRecommendedInterval;
  }

  public void setLatestRecommendedInterval(TimePeriod latestRecommendedInterval) {
    this.latestRecommendedInterval = latestRecommendedInterval;
  }

  public TimePeriod getEffectiveDate() {
    return effectiveDate;
  }

  public void setEffectiveDate(TimePeriod effectiveDate) {
    this.effectiveDate = effectiveDate;
  }

  public TimePeriod getCessationDate() {
    return cessationDate;
  }

  public void setCessationDate(TimePeriod cessationDate) {
    this.cessationDate = cessationDate;
  }
}
