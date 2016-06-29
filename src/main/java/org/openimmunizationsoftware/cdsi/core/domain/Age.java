package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class Age
{
  private SeriesDose seriesDose = null;
  private TimePeriod absoluteMinimumAge = null;
  private TimePeriod minimumAge = null;
  private TimePeriod earliestRecommendedAge = null;
  private TimePeriod latestRecommendedAge = null;
  private TimePeriod maximumAge = null;
  
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

  public TimePeriod getMinimugeAge() {
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
