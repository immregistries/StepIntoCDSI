package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class AllowableInterval {
  private SeriesDose seriesDose = null;
  private YesNo fromImmediatePreviousDoseAdministered = null;
  private String fromTargetDoseNumberInSeries = "";
  private TimePeriod absoluteMinimumInterval = null;

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

  public TimePeriod getAbsoluteMinimumInterval() {
    return absoluteMinimumInterval;
  }

  public void setAbsoluteMinimumInterval(TimePeriod absoluteMinimumInterval) {
    this.absoluteMinimumInterval = absoluteMinimumInterval;
  }
}
