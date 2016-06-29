package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;

public class SeasonalRecommendation
{
  private SeriesDose seriesDose = null;
  private Date seasonalRecommendationStartDate = null;
  private Date seasonalRecommendationEndDate = null;

  public Date getSeasonalRecommendationStartDate() {
    return seasonalRecommendationStartDate;
  }

  public void setSeasonalRecommendationStartDate(Date seasonalRecommendationStartDate) {
    this.seasonalRecommendationStartDate = seasonalRecommendationStartDate;
  }

  public Date getSeasonalRecommendationEndDate() {
    return seasonalRecommendationEndDate;
  }

  public void setSeasonalRecommendationEndDate(Date seasonalRecommendationEndDate) {
    this.seasonalRecommendationEndDate = seasonalRecommendationEndDate;
  }

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

}
