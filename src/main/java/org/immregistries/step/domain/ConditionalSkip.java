package org.immregistries.step.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.immregistries.step.core.domain.datatypes.TimePeriod;

public class ConditionalSkip {

  private String conditionalSkipContext = "";
  private String conditionalSkipSetLogic = "";
  private String conditionalSkipSetID = "";
  private String conditionalSkipDescription = "";
  private String conditionalSkipConditionLogic = "";
  private String conditionalSkipConditionID = "";
  private String conditionalSkipType = "";
  private Date conditionalSkipStartDate = null;
  private Date conditionalSkipEndDate = null;
  private TimePeriod conditionalSkipBeginAge = null;
  private TimePeriod conditionalSkipEndAge = null;
  private TimePeriod conditionalSkipInterval = null;
  private int conditionalSkipDoseCount = 0;
  private String conditionalSkipDoseType = "";
  private String conditionalSkipDoseCountLogic = "";
  private String conditionalSkipVaccineType = "";
  private String conditionalSkipSeriesGroup = "";
  private Date effectiveDate = null;
  private Date cessationDate = null;

  public static final String SET_LOGIC_AND = "AND";
  public static final String SET_LOGIC_OR = "OR";
  private String setLogic = "";
  private List<ConditionalSkipSet> conditionalSkipSetList = new ArrayList<ConditionalSkipSet>();
  private SeriesDose seriesDose = null;

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

  public String getConditionalSkipContext() {
    return conditionalSkipContext;
  }

  public void setConditionalSkipContext(String conditionalSkipContext) {
    this.conditionalSkipContext = conditionalSkipContext;
  }

  public String getConditionalSkipSetLogic() {
    return conditionalSkipSetLogic;
  }

  public void setConditionalSkipSetLogic(String conditionalSkipSetLogic) {
    this.conditionalSkipSetLogic = conditionalSkipSetLogic;
  }

  public String getConditionalSkipSetID() {
    return conditionalSkipSetID;
  }

  public void setConditionalSkipSetID(String conditionalSkipSetID) {
    this.conditionalSkipSetID = conditionalSkipSetID;
  }

  public String getConditionalSkipDescription() {
    return conditionalSkipDescription;
  }

  public void setConditionalSkipDescription(String conditionalSkipDescription) {
    this.conditionalSkipDescription = conditionalSkipDescription;
  }

  public String getConditionalSkipConditionLogic() {
    return conditionalSkipConditionLogic;
  }

  public void setConditionalSkipConditionLogic(String conditionalSkipConditionLogic) {
    this.conditionalSkipConditionLogic = conditionalSkipConditionLogic;
  }

  public String getConditionalSkipConditionID() {
    return conditionalSkipConditionID;
  }

  public void setConditionalSkipConditionID(String conditionalSkipConditionID) {
    this.conditionalSkipConditionID = conditionalSkipConditionID;
  }

  public String getConditionalSkipType() {
    return conditionalSkipType;
  }

  public void setConditionalSkipType(String conditionalSkipType) {
    this.conditionalSkipType = conditionalSkipType;
  }

  public Date getConditionalSkipStartDate() {
    return conditionalSkipStartDate;
  }

  public void setConditionalSkipStartDate(Date conditionalSkipStartDate) {
    this.conditionalSkipStartDate = conditionalSkipStartDate;
  }

  public Date getConditionalSkipEndDate() {
    return conditionalSkipEndDate;
  }

  public void setConditionalSkipEndDate(Date conditionalSkipEndDate) {
    this.conditionalSkipEndDate = conditionalSkipEndDate;
  }

  public TimePeriod getConditionalSkipBeginAge() {
    return conditionalSkipBeginAge;
  }

  public void setConditionalSkipBeginAge(TimePeriod conditionalSkipBeginAge) {
    this.conditionalSkipBeginAge = conditionalSkipBeginAge;
  }

  public TimePeriod getConditionalSkipEndAge() {
    return conditionalSkipEndAge;
  }

  public void setConditionalSkipEndAge(TimePeriod conditionalSkipEndAge) {
    this.conditionalSkipEndAge = conditionalSkipEndAge;
  }

  public TimePeriod getConditionalSkipInterval() {
    return conditionalSkipInterval;
  }

  public void setConditionalSkipInterval(TimePeriod conditionalSkipInterval) {
    this.conditionalSkipInterval = conditionalSkipInterval;
  }

  public int getConditionalSkipDoseCount() {
    return conditionalSkipDoseCount;
  }

  public void setConditionalSkipDoseCount(int conditionalSkipDoseCount) {
    this.conditionalSkipDoseCount = conditionalSkipDoseCount;
  }

  public String getConditionalSkipDoseType() {
    return conditionalSkipDoseType;
  }

  public void setConditionalSkipDoseType(String conditionalSkipDoseType) {
    this.conditionalSkipDoseType = conditionalSkipDoseType;
  }

  public String getConditionalSkipDoseCountLogic() {
    return conditionalSkipDoseCountLogic;
  }

  public void setConditionalSkipDoseCountLogic(String conditionalSkipDoseCountLogic) {
    this.conditionalSkipDoseCountLogic = conditionalSkipDoseCountLogic;
  }

  public String getConditionalSkipVaccineType() {
    return conditionalSkipVaccineType;
  }

  public void setConditionalSkipVaccineType(String conditionalSkipVaccineType) {
    this.conditionalSkipVaccineType = conditionalSkipVaccineType;
  }

  public String getConditionalSkipSeriesGroup() {
    return conditionalSkipSeriesGroup;
  }

  public void setConditionalSkipSeriesGroup(String conditionalSkipSeriesGroup) {
    this.conditionalSkipSeriesGroup = conditionalSkipSeriesGroup;
  }

  public Date getEffectiveDate() {
    return effectiveDate;
  }

  public void setEffectiveDate(Date effectiveDate) {
    this.effectiveDate = effectiveDate;
  }

  public Date getCessationDate() {
    return cessationDate;
  }

  public void setCessationDate(Date cessationDate) {
    this.cessationDate = cessationDate;
  }

  public void setConditionalSkipSetList(List<ConditionalSkipSet> conditionalSkipSetList) {
    this.conditionalSkipSetList = conditionalSkipSetList;
  }

  public String getSetLogic() {
    return setLogic;
  }

  public void setSetLogic(String setLogic) {
    this.setLogic = setLogic;
  }

  public List<ConditionalSkipSet> getConditionalSkipSetList() {
    return conditionalSkipSetList;
  }
}
