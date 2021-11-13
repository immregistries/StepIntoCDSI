package org.openimmunizationsoftware.cdsi.core.domain;

public class PreferrableVaccine extends Vaccine {
  private SeriesDose seriesDose = null;
  private Vaccine vaccine = null;

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

  public Vaccine getVaccine() {
    return vaccine;
  }

  public void setVaccine(Vaccine vaccine) {
    this.vaccine = vaccine;
  }
}
