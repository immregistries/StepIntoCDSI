package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class PreferrableVaccine extends Vaccine {
  private SeriesDose seriesDose = null;
  private String mvx = "";
  private YesNo forecastVaccineType = YesNo.NOT_APPLICABLE;

  public YesNo getForecastVaccineType() {
    return forecastVaccineType;
  }

  public void setForecastVaccineType(YesNo forecastVaccineType) {
    this.forecastVaccineType = forecastVaccineType;
  }

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

  public String getMvx() {
    return mvx;
  }

  public void setMvx(String mvx) {
    this.mvx = mvx;
  }
}
