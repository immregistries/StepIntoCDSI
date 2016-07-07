package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class ConditionalSkipCondition {
  private SeriesDose seriesDose = null;
  private int conditionId = 0;
  private ConditionalSkipConditionType conditionType = ConditionalSkipConditionType.AGE;
  private Date startDate = null;
  private Date endDate = null;
  private TimePeriod beginAge = null;
  private TimePeriod endAge = null;
  private TimePeriod interval = null;
  private int doseCount = 0;
  private DoseType doseType = null;
  private String doseCountLogic = "";
  private Set<VaccineType> vaccineTypeSet = new HashSet<VaccineType>();
  
  public SeriesDose getSeriesDose() {
    return seriesDose;
  }
  
  public ConditionalSkipCondition(SeriesDose seriesDose)
  {
    this.seriesDose = seriesDose;
  }

  public Set<VaccineType> getVaccineTypeSet() {
    return vaccineTypeSet;
  }

  public int getConditionId() {
    return conditionId;
  }

  public void setConditionId(int conditionId) {
    this.conditionId = conditionId;
  }

  public ConditionalSkipConditionType getConditionType() {
    return conditionType;
  }

  public void setConditionType(ConditionalSkipConditionType conditionType) {
    this.conditionType = conditionType;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  public TimePeriod getBeginAge() {
    return beginAge;
  }

  public void setBeginAge(TimePeriod beginAge) {
    this.beginAge = beginAge;
  }

  public TimePeriod getEndAge() {
    return endAge;
  }

  public void setEndAge(TimePeriod endAge) {
    this.endAge = endAge;
  }

  public TimePeriod getInterval() {
    return interval;
  }

  public void setInterval(TimePeriod interval) {
    this.interval = interval;
  }

  public int getDoseCount() {
    return doseCount;
  }

  public void setDoseCount(int doseCount) {
    this.doseCount = doseCount;
  }

  public DoseType getDoseType() {
    return doseType;
  }

  public void setDoseType(DoseType doseType) {
    this.doseType = doseType;
  }

  public String getDoseCountLogic() {
    return doseCountLogic;
  }

  public void setDoseCountLogic(String doseCountLogic) {
    this.doseCountLogic = doseCountLogic;
  }

}
