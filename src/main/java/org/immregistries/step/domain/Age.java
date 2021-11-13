package org.immregistries.step.domain;

import java.util.Date;
import org.immregistries.step.core.domain.datatypes.TimePeriod;

public class Age {
  private SeriesDose seriesDose = null;
  private TimePeriod absoluteMinimumAge = null;
  private TimePeriod minimumAge = null;
  private TimePeriod earliestRecommendedAge = null;
  private TimePeriod latestRecommendedAge = null;
  private TimePeriod maximumAge = null;
  private Date effectiveDate = null;
  private Date cessationDate = null;

  public Date getEffectiveDate() {
    return effectiveDate;
  }

  public void setEffectiveDate(Date effectiveDate) {
    this.effectiveDate = effectiveDate;
  }

  public Date getCessationDate() {
    return cessationDate;
  }

  public void setCessationDate(Date cessationDate) {
    this.cessationDate = cessationDate;
  }

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

  public TimePeriod getAbsoluteMinimumAge() {
    return absoluteMinimumAge;
  }

  public void setAbsoluteMinimumAge(TimePeriod absoluteMinimumAge) {
    this.absoluteMinimumAge = absoluteMinimumAge;
  }

  public TimePeriod getMinimumAge() {
    return minimumAge;
  }

  public void setMinimugeAge(TimePeriod minimugeAge) {
    this.minimumAge = minimugeAge;
  }

  public TimePeriod getEarliestRecommendedAge() {
    return earliestRecommendedAge;
  }

  public void setEarliestRecommendedAge(TimePeriod earliestRecommendedAge) {
    this.earliestRecommendedAge = earliestRecommendedAge;
  }

  public TimePeriod getLatestRecommendedAge() {
    return latestRecommendedAge;
  }

  public void setLatestRecommendedAge(TimePeriod latestRecommendedAge) {
    this.latestRecommendedAge = latestRecommendedAge;
  }

  public TimePeriod getMaximumAge() {
    return maximumAge;
  }

  public void setMaximumAge(TimePeriod maximumAge) {
    this.maximumAge = maximumAge;
  }
}
