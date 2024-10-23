package org.openimmunizationsoftware.cdsi.core.domain;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
  private List<SeasonalRecommendation> seasonalRecommendationList =
      new ArrayList<SeasonalRecommendation>();
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

  public void toHtml(PrintWriter out) {
    out.println("<h3>Series Dose</h3>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Dose Number</th>");
    out.println("    <td>" + doseNumber + "</td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <th>AntigenSeries</th>");
    out.println("    <td>" + antigenSeries.getSeriesName() + "</td>");
    out.println("  </tr>");
    out.println("<table>");
    if (ageList.size() > 0) {
      out.println("<h4>Age</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Absolute Minimum Age</th>");
      out.println("    <th>Minimum Age</th>");
      out.println("    <th>Earliest Recommended Age</th>");
      out.println("    <th>Latest Recommended Age</th>");
      out.println("    <th>Maximum Age</th>");
      out.println("  </tr>");
      for (Age age : ageList) {
        out.println("  <tr>");
        out.println("    <td>" + age.getAbsoluteMinimumAge() + "</td>");
        out.println("    <td>" + age.getMinimumAge() + "</td>");
        out.println("    <td>" + age.getEarliestRecommendedAge() + "</td>");
        out.println("    <td>" + age.getLatestRecommendedAge() + "</td>");
        out.println("    <td>" + age.getMaximumAge() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
    if (intervalList.size() > 0) {
      out.println("<h4>Interval</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>From Immediate Previous Dose Administered</th>");
      out.println("    <th>From Target Dose Number In Series</th>");
      out.println("    <th>Absolute Minimum Interval</th>");
      out.println("    <th>Minimum Interval</th>");
      out.println("    <th>Earliest Recommended Interval</th>");
      out.println("    <th>Latest RecommendedInterval</th>");
      out.println("  </tr>");
      for (Interval interval : intervalList) {
        out.println("  <tr>");
        out.println("    <td>" + interval.getFromImmediatePreviousDoseAdministered() + "</td>");
        out.println("    <td>" + interval.getFromTargetDoseNumberInSeries() + "</td>");
        out.println("    <td>" + interval.getAbsoluteMinimumInterval() + "</td>");
        out.println("    <td>" + interval.getMinimumInterval() + "</td>");
        out.println("    <td>" + interval.getEarliestRecommendedInterval() + "</td>");
        out.println("    <td>" + interval.getLatestRecommendedInterval() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
    if (allowableintervalList.size() > 0) {
      out.println("<h4>Allowable Interval</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>From Immediate Previous Dose Administered</th>");
      out.println("    <th>From Target Dose Number In Series</th>");
      out.println("    <th>Absolute Minimum Interval</th>");
      out.println("  </tr>");
      for (AllowableInterval ainterval : allowableintervalList) {
        out.println("  <tr>");
        out.println("    <td>" + ainterval.getFromImmediatePreviousDoseAdministered() + "</td>");
        out.println("    <td>" + ainterval.getFromTargetDoseNumberInSeries() + "</td>");
        out.println("    <td>" + ainterval.getAbsoluteMinimumInterval() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
    // if (skipTargetDoseList.size() > 0) {
    // out.println("<h4>Skip Target Dose</h4>");
    // out.println("<table>");
    // out.println(" <tr>");
    // out.println(" <th>Trigger Age</th>");
    // out.println(" <th>Trigger Interval</th>");
    // out.println(" <th>Trigger Target Dose</th>");
    // out.println(" <th>Trigger Series Dose</th>");
    // out.println(" </tr>");
    // for (SkipTargetDose skipTargetDose : skipTargetDoseList) {
    // out.println(" <tr>");
    // out.println(" <td>" + skipTargetDose.getTriggerAge() + "</td>");
    // out.println(" <td>" + skipTargetDose.getTriggerInterval() + "</td>");
    // out.println(" <td>" + skipTargetDose.getTriggerTargetDose() + "</td>");
    // out.println(" <td>" + skipTargetDose.getTriggerSeriesDose() + "</td>");
    // out.println(" </tr>");
    // }
    // out.println("</table>");
    // }

    if (recurringDoseList.size() > 0) {
      out.println("<h4>Recurring Dose</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Value</th>");
      out.println("  </tr>");
      for (RecurringDose recurringDose : recurringDoseList) {
        out.println("  <tr>");
        out.println("    <td>" + recurringDose.getValue() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (conditionalNeedList.size() > 0) {
      out.println("<h4>Conditional Need</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Conditional Set</th>");
      out.println("    <th>Conditional Start Date</th>");
      out.println("    <th>Conditional End Date</th>");
      out.println("    <th>Dose Count</th>");
      out.println("  </tr>");
      for (ConditionalNeed conditionalNeed : conditionalNeedList) {
        out.println("  <tr>");
        out.println("    <td>" + conditionalNeed.getConditionalSet() + "</td>");
        out.println("    <td>" + n(conditionalNeed.getConditionalStartDate()) + "</td>");
        out.println("    <td>" + n(conditionalNeed.getConditionalEndDate()) + "</td>");
        out.println("    <td>" + conditionalNeed.getDoseCount() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (seasonalRecommendationList.size() > 0) {
      out.println("<h4>Seasonal Recommendation</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Seasonal Recommendation Start Date</th>");
      out.println("    <th>Seasonal Recommendation End Date</th>");
      out.println("  </tr>");
      for (SeasonalRecommendation seasonalRecommendation : seasonalRecommendationList) {
        out.println("  <tr>");
        out.println(
            "    <td>" + n(seasonalRecommendation.getSeasonalRecommendationStartDate()) + "</td>");
        out.println(
            "    <td>" + n(seasonalRecommendation.getSeasonalRecommendationEndDate()) + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (substituteDoseList.size() > 0) {
      out.println("<h4>Subsititute Dose</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>First Dose Begin Age</th>");
      out.println("    <th>First Dose End Age</th>");
      out.println("    <th>Total Count of Valid Doses</th>");
      out.println("    <th>Number of Target Doses to Substitute</th>");
      out.println("  </tr>");
      for (SubstituteDose substituteDose : substituteDoseList) {
        out.println("  <tr>");
        out.println("    <td>" + substituteDose.getFirstDoseBeginAge() + "</td>");
        out.println("    <td>" + substituteDose.getFirstDoseEndAge() + "</td>");
        out.println("    <td>" + substituteDose.getTotalCountOfValidDoses() + "</td>");
        out.println("    <td>" + substituteDose.getNumberOfTargetDosesToSubstitue() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (requiredGenderList.size() > 0) {
      out.println("<h4>Required Gender</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Required Gender</th>");
      out.println("  </tr>");
      for (RequiredGender requiredGender : requiredGenderList) {
        out.println("  <tr>");
        out.println("    <td>" + requiredGender.getValue() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (preferrableVaccineList.size() > 0) {
      out.println("<h4>Preferrable Vaccine</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Lot Expiration Date</th>");
      out.println("    <th>Manufacturer</th>");
      out.println("    <th>Trade Name</th>");
      out.println("    <th>Vaccine Type</th>");
      out.println("    <th>Vaccine Type Begin Age</th>");
      out.println("    <th>Vaccine Type End Age</th>");
      out.println("    <th>Volume</th>");
      out.println("  </tr>");
      for (PreferrableVaccine preferrableVaccine : preferrableVaccineList) {
        out.println("  <tr>");
        out.println("    <td>" + n(preferrableVaccine.getLotExpirationDate()) + "</td>");
        out.println("    <td>" + preferrableVaccine.getManufacturer() + "</td>");
        out.println("    <td>" + preferrableVaccine.getTradeName() + "</td>");
        out.println("    <td>" + preferrableVaccine.getVaccineType() + "</td>");
        out.println("    <td>" + preferrableVaccine.getVaccineTypeBeginAge() + "</td>");
        out.println("    <td>" + preferrableVaccine.getVaccineTypeEndAge() + "</td>");
        out.println("    <td>" + preferrableVaccine.getVolume() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
    if (allowableVaccineList.size() > 0) {
      out.println("<h4>Allowable Vaccine</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Lot Expiration Date</th>");
      out.println("    <th>Manufacturer</th>");
      out.println("    <th>Trade Name</th>");
      out.println("    <th>Vaccine Type</th>");
      out.println("    <th>Vaccine Type Begin Age</th>");
      out.println("    <th>Vaccine Type End Age</th>");
      out.println("    <th>Volume</th>");
      out.println("  </tr>");
      for (AllowableVaccine allowableVaccine : allowableVaccineList) {
        out.println("  <tr>");
        out.println("    <td>" + n(allowableVaccine.getLotExpirationDate()) + "</td>");
        out.println("    <td>" + allowableVaccine.getManufacturer() + "</td>");
        out.println("    <td>" + allowableVaccine.getTradeName() + "</td>");
        out.println("    <td>" + allowableVaccine.getVaccineType() + "</td>");
        out.println("    <td>" + allowableVaccine.getVaccineTypeBeginAge() + "</td>");
        out.println("    <td>" + allowableVaccine.getVaccineTypeEndAge() + "</td>");
        out.println("    <td>" + allowableVaccine.getVolume() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

  }

  private static String n(Date date) {
    if (date == null) {
      return "";
    } else {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      return sdf.format(date);
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
