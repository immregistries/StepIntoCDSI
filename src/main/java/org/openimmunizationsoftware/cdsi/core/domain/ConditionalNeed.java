package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;

public class ConditionalNeed {
  private SeriesDose seriesDose = null;
  private String conditionalSet = "";
  private Date conditionalStartDate = null;
  private Date conditionalEndDate = null;
  private String doseCount = "";

  public String getConditionalSet() {
    return conditionalSet;
  }

  public void setConditionalSet(String conditionalSet) {
    this.conditionalSet = conditionalSet;
  }

  public Date getConditionalStartDate() {
    return conditionalStartDate;
  }

  public void setConditionalStartDate(Date conditionalStartDate) {
    this.conditionalStartDate = conditionalStartDate;
  }

  public Date getConditionalEndDate() {
    return conditionalEndDate;
  }

  public void setConditionalEndDate(Date conditionalEndDate) {
    this.conditionalEndDate = conditionalEndDate;
  }

  public String getDoseCount() {
    return doseCount;
  }

  public void setDoseCount(String doseCount) {
    this.doseCount = doseCount;
  }

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

}
