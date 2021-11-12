package org.openimmunizationsoftware.cdsi.core.domain;

public class Indication {
  private String indicationTextDescription = "";
  private Age indicationBeginAge = null;
  private Age indicationEndAge = null;

  public String getIndicationTextDescription() {
    return indicationTextDescription;
  }

  public void setIndicationTextDescription(String indicationTextDescription) {
    this.indicationTextDescription = indicationTextDescription;
  }

  public Age getIndicationBeginAge() {
    return indicationBeginAge;
  }

  public void setIndicationBeginAge(Age indicationBeginAge) {
    this.indicationBeginAge = indicationBeginAge;
  }

  public Age getIndicationEndAge() {
    return indicationEndAge;
  }

  public void setIndicationEndAge(Age indicationEndAge) {
    this.indicationEndAge = indicationEndAge;
  }
}
