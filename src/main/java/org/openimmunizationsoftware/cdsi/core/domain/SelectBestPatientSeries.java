package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class SelectBestPatientSeries {
  private YesNo defaultSeries = null;
  private YesNo productPath = null;
  private String seriesGroupName = "";
  private String seriesGroup = "";
  private String seriesPriority = "";
  private String seriesPreference = "";
  private TimePeriod minAgeToStart = null;
  private TimePeriod maxAgeToStart = null;
  

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

  public String getSeriesGroupName() {
    return this.seriesGroupName;
  }

  public void setSeriesGroupName(String seriesGroupName) {
    this.seriesGroupName = seriesGroupName;
  }

  public String getSeriesGroup() {
    return this.seriesGroup;
  }

  public void setSeriesGroup(String seriesGroup) {
    this.seriesGroup = seriesGroup;
  }

  public String getSeriesPriority() {
    return this.seriesPriority;
  }

  public void setSeriesPriority(String seriesPriority) {
    this.seriesPriority = seriesPriority;
  }

  public String getSeriesPreference() {
    return seriesPreference;
  }

  public void setSeriesPreference(String seriesPreference) {
    this.seriesPreference = seriesPreference;
  }

  public TimePeriod getMinAgeToStart() {
    return this.minAgeToStart;
  }

  public void setMinAgeToStart(TimePeriod minAgeToStart) {
    this.minAgeToStart = minAgeToStart;
  }

  public TimePeriod getMaxAgeToStart() {
    return maxAgeToStart;
  }

  public void setMaxAgeToStart(TimePeriod maxAgeToStart) {
    this.maxAgeToStart = maxAgeToStart;
  }
}
