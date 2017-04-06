package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class SubstituteDose {
  private SeriesDose seriesDose = null;
  private TimePeriod firstDoseBeginAge = null;
  private TimePeriod firstDoseEndAge = null;
  private int totalCountOfValidDoses = 0;
  private int numberOfTargetDosesToSubstitue = 0;

  public TimePeriod getFirstDoseBeginAge() {
    return firstDoseBeginAge;
  }

  public void setFirstDoseBeginAge(TimePeriod firstDoseBeginAge) {
    this.firstDoseBeginAge = firstDoseBeginAge;
  }

  public TimePeriod getFirstDoseEndAge() {
    return firstDoseEndAge;
  }

  public void setFirstDoseEndAge(TimePeriod firstDoseEndAge) {
    this.firstDoseEndAge = firstDoseEndAge;
  }

  public int getTotalCountOfValidDoses() {
    return totalCountOfValidDoses;
  }

  public void setTotalCountOfValidDoses(int totalCountOfValidDoses) {
    this.totalCountOfValidDoses = totalCountOfValidDoses;
  }

  public int getNumberOfTargetDosesToSubstitue() {
    return numberOfTargetDosesToSubstitue;
  }

  public void setNumberOfTargetDosesToSubstitue(int numberOfTargetDosesToSubstitue) {
    this.numberOfTargetDosesToSubstitue = numberOfTargetDosesToSubstitue;
  }

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

}
