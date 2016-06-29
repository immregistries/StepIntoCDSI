package org.openimmunizationsoftware.cdsi.core.domain;

public class PreferrableVaccine extends Vaccine
{
  private SeriesDose seriesDose = null;

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }
}
