package org.immregistries.step.domain;

import org.immregistries.step.core.domain.datatypes.TimePeriod;
import org.immregistries.step.core.domain.datatypes.YesNo;

public class SelectPatientSeries {
  private YesNo defaultSeries = null;
  private YesNo productPath = null;
  private String seriesGroupName = "";
  private String seriesPriority = "";
  private String seriesPreference = "";
  private TimePeriod minimumAgeToStart = null;
  private TimePeriod maximumAgeToStart = null;
  private AntigenSeries antigenSeries = null;

  public String getSeriesGroupName() {
    return seriesGroupName;
  }

  public void setSeriesGroupName(String seriesGroupName) {
    this.seriesGroupName = seriesGroupName;
  }

  public String getSeriesPriority() {
    return seriesPriority;
  }

  public void setSeriesPriority(String seriesPriority) {
    this.seriesPriority = seriesPriority;
  }

  public TimePeriod getMinimumAgeToStart() {
    return minimumAgeToStart;
  }

  public void setMinimumAgeToStart(TimePeriod minimumAgeToStart) {
    this.minimumAgeToStart = minimumAgeToStart;
  }

  public YesNo getDefaultSeries() {
    return defaultSeries;
  }

  public void setDefaultSeries(YesNo defaultSeries) {
    this.defaultSeries = defaultSeries;
  }

  public YesNo getProductPath() {
    return productPath;
  }

  public void setProductPath(YesNo productPath) {
    this.productPath = productPath;
  }

  public String getSeriesPreference() {
    return seriesPreference;
  }

  public void setSeriesPreference(String seriesPreference) {
    this.seriesPreference = seriesPreference;
  }

  public TimePeriod getMaximumAgeToStart() {
    return maximumAgeToStart;
  }

  public void setMaximumAgeToStart(TimePeriod maxAgeToStart) {
    this.maximumAgeToStart = maxAgeToStart;
  }

  public AntigenSeries getAntigenSeries() {
    return antigenSeries;
  }

  public void setAntigenSeries(AntigenSeries antigenSeries) {
    this.antigenSeries = antigenSeries;
  }
}
