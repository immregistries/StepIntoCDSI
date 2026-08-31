package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class SeriesDose {
  private String doseNumber = "";
  private AntigenSeries antigenSeries = null;
  private List<Age> ageList = new ArrayList<Age>();
  private List<Interval> intervalList = new ArrayList<Interval>();
  private List<AllowableInterval> allowableintervalList = new ArrayList<AllowableInterval>();
  private ConditionalSkip conditionalSkip = null;
  private List<RecurringDose> recurringDoseList = new ArrayList<RecurringDose>();
  private List<ConditionalNeed> conditionalNeedList = new ArrayList<ConditionalNeed>();
  private List<SeasonalRecommendation> seasonalRecommendationList = new ArrayList<SeasonalRecommendation>();
  private List<SubstituteDose> substituteDoseList = new ArrayList<SubstituteDose>();
  private List<RequiredGender> requiredGenderList = new ArrayList<RequiredGender>();
  private List<PreferrableVaccine> preferrableVaccineList = new ArrayList<PreferrableVaccine>();
  private List<AllowableVaccine> allowableVaccineList = new ArrayList<AllowableVaccine>();
  private List<VaccineType> inadvertentVaccineList = new ArrayList<VaccineType>();

  @Override
  public String toString() {
    if (antigenSeries != null) {
      return antigenSeries.getSeriesName() + " dose " + doseNumber;
    } else {
      return "dose " + doseNumber;
    }
  }

  public ConditionalSkip getConditionalSkip() {
    return conditionalSkip;
  }

  public void setConditionalSkip(ConditionalSkip conditionalSkip) {
    this.conditionalSkip = conditionalSkip;
  }

  public String getDoseNumber() {
    return doseNumber;
  }

  public void setDoseNumber(String doseNumber) {
    this.doseNumber = doseNumber;
  }

  public AntigenSeries getAntigenSeries() {
    return antigenSeries;
  }

  public void setAntigenSeries(AntigenSeries antigenSeries) {
    this.antigenSeries = antigenSeries;
  }

  public List<Age> getAgeList() {
    return ageList;
  }

  public List<Interval> getIntervalList() {
    return intervalList;
  }

  public List<RecurringDose> getRecurringDoseList() {
    return recurringDoseList;
  }

  public RecurringDose getRecurringDose() {
    if (recurringDoseList.size() > 0) {
      return recurringDoseList.get(0);
    }
    return null;
  }

  public List<ConditionalNeed> getConditionalNeedList() {
    return conditionalNeedList;
  }

  public List<SeasonalRecommendation> getSeasonalRecommendationList() {
    return seasonalRecommendationList;
  }

  public List<SubstituteDose> getSubstituteDoseList() {
    return substituteDoseList;
  }

  public List<RequiredGender> getRequiredGenderList() {
    return requiredGenderList;
  }

  public List<PreferrableVaccine> getPreferrableVaccineList() {
    return preferrableVaccineList;
  }

  public List<AllowableVaccine> getAllowableVaccineList() {
    return allowableVaccineList;
  }

  public List<AllowableInterval> getAllowableintervalList() {
    return allowableintervalList;
  }

  public void setAllowableintervalList(List<AllowableInterval> allowableintervalList) {
    this.allowableintervalList = allowableintervalList;
  }

  public List<VaccineType> getInadvertentVaccineList() {
    return inadvertentVaccineList;
  }

  public void setInadvertentVaccineList(List<VaccineType> inadvertentVaccineList) {
    this.inadvertentVaccineList = inadvertentVaccineList;
  }
}
