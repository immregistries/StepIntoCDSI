package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;

public class AllowableInterval {
  private SeriesDose seriesDose = null;
  private YesNo fromImmediatePreviousDoseAdministered = null;
  private String fromTargetDoseNumberInSeries = "";
  private TimePeriod absoluteMinimumInterval = null;
  private TimePeriod effectiveDate = null;
  private TimePeriod cessationDate = null;

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

  public Interval getInterval() {
    Interval in = new Interval();
    in.setSeriesDose(seriesDose);
    in.setFromImmediatePreviousDoseAdministered(fromImmediatePreviousDoseAdministered);
    in.setFromTargetDoseNumberInSeries(fromTargetDoseNumberInSeries);
    in.setAbsoluteMinimumInterval(absoluteMinimumInterval);
    in.setEffectiveDate(effectiveDate);
    in.setCessationDate(cessationDate);
    return in;
  }
}
