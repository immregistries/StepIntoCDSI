package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class RecurringDose {
  private SeriesDose seriesDose = null;
  private YesNo value;

  public YesNo getValue() {
    return value;
  }

  public void setValue(YesNo value) {
    this.value = value;
  }

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

}
