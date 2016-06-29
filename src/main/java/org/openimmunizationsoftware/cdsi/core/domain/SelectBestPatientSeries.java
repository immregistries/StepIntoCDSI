package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class SelectBestPatientSeries
{
  private YesNo defaultSeries = null;
  private YesNo productPath = null;
  private String seriesPreference = "";
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

  public String getSeriesPreference() {
    return seriesPreference;
  }

  public void setSeriesPreference(String seriesPreference) {
    this.seriesPreference = seriesPreference;
  }

  public TimePeriod getMaxAgeToStart() {
    return maxAgeToStart;
  }

  public void setMaxAgeToStart(TimePeriod maxAgeToStart) {
    this.maxAgeToStart = maxAgeToStart;
  }
}
