package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class GenerateForecastDates extends LogicStep {

  private ConditionAttribute<Date> caMinimumAgeDate = null;
  private ConditionAttribute<Date> caEarliestRecommendedAgeDate = null;
  private ConditionAttribute<Date> caLatestRecommendedAgeDate = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<Date> caMinimumIntervalDate = null;
  private ConditionAttribute<Date> caEarliestRecommendedIntervalDate = null;
  private ConditionAttribute<Date> caLatestRecommendedIntervalDate = null;
  private ConditionAttribute<Date> caLatestConflictEndIntervalDate = null;
  private ConditionAttribute<Date> caSeasonalRecommendationStartDate = null;
  private ConditionAttribute<VaccineType> caVaccineType = null;
  private ConditionAttribute<YesNo> caForecastVaccineType = null;
  
  private Forecast  forecast = new Forecast();
  
  int size = dataModel.getVaccineGroupForecast().getForecastList().size();

  public GenerateForecastDates(DataModel dataModel) {
    super(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES, dataModel);
    setConditionTableName("Table ");

    caMinimumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-4)", "Minimum Age Date");
    caEarliestRecommendedAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-3)",
        "Earliest Recommended Age Date");
    caLatestRecommendedAgeDate = new ConditionAttribute<Date>("Calcualted date (CALCDTAGE-2)",
        "Latest Recommended Age Date");
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)", "Maximum Age Date");
    caMinimumIntervalDate = new ConditionAttribute<Date>("Calcualated date (CALCDTINT-4)", "Minimal Interval Date(s)");
    caEarliestRecommendedIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-5)",
        "Earliest Recommended Interval Date(s)");
    caLatestRecommendedIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-6)",
        "Lastest Recommended Interval Date(s)");
    caLatestConflictEndIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-4)",
        "Latest Conflict End Interval Date");
    caSeasonalRecommendationStartDate = new ConditionAttribute<Date>("Supporting data (Seasonal Recommendation)",
        "Seasonal Recommendation Start Date");
    caVaccineType = new ConditionAttribute<VaccineType>("Supporting data (Preferable Vaccine", "Vaccine Type (CVX)");
    caForecastVaccineType = new ConditionAttribute<YesNo>("Supporting data (Preferable Vaccine)",
        "Forecast Vaccine Type");

    caSeasonalRecommendationStartDate.setAssumedValue(PAST);
    caForecastVaccineType.setAssumedValue(YesNo.NO);

    conditionAttributesList.add(caMinimumAgeDate);
    conditionAttributesList.add(caEarliestRecommendedAgeDate);
    conditionAttributesList.add(caLatestRecommendedAgeDate);
    conditionAttributesList.add(caMaximumAgeDate);
    conditionAttributesList.add(caMinimumIntervalDate);
    conditionAttributesList.add(caEarliestRecommendedIntervalDate);
    conditionAttributesList.add(caLatestRecommendedIntervalDate);
    conditionAttributesList.add(caLatestConflictEndIntervalDate);
    conditionAttributesList.add(caSeasonalRecommendationStartDate);
    conditionAttributesList.add(caVaccineType);
    conditionAttributesList.add(caForecastVaccineType);

    TargetDose targetDose = dataModel.getTargetDose();
    Patient patient = dataModel.getPatient();

    if (targetDose.getTrackedSeriesDose().getAgeList().size() > 0) {
      Age age = targetDose.getTrackedSeriesDose().getAgeList().get(0);
      if (age.getMinimumAge() != null) {
        caMinimumAgeDate.setInitialValue(age.getMinimumAge().getDateFrom(patient.getDateOfBirth()));
      }
      if (age.getEarliestRecommendedAge() != null) {
        caEarliestRecommendedAgeDate.setInitialValue(age.getMinimumAge().getDateFrom(patient.getDateOfBirth()));
      }
      if (age.getLatestRecommendedAge() != null) {
        caLatestRecommendedAgeDate.setInitialValue(age.getMinimumAge().getDateFrom(patient.getDateOfBirth()));
      }
    }
    if (targetDose.getTrackedSeriesDose().getIntervalList().size() > 0) {
      Date minimumIntervalDate = null;
      Date earliestRecommendedIntervalDate = null;
      Date latestRecommendedIntervalDate = null;
      for (Interval interval : targetDose.getTrackedSeriesDose().getIntervalList()) {
        AntigenAdministeredRecord aar = null;
        if (interval.getFromImmediatePreviousDoseAdministered() == YesNo.YES) {
          aar = dataModel.getPreviousAntigenAdministeredRecord();
        } else {
          for (AntigenAdministeredRecord aarCheck : dataModel.getAntigenAdministeredRecordList()) {
            if (aarCheck.getAssignedTargetDoseNumberInSeries().equals(interval.getFromTargetDoseNumberInSeries()))
              aar = aarCheck;
          }
        }
        if (aar != null) {
          {
            Date d = interval.getMinimumInterval().getDateFrom(aar.getDateAdministered());
            if (minimumIntervalDate == null || d.after(minimumIntervalDate)) {
              minimumIntervalDate = d;
            }
          }
          {
            Date d = interval.getEarliestRecommendedInterval().getDateFrom(aar.getDateAdministered());
            if (earliestRecommendedIntervalDate == null || d.after(earliestRecommendedIntervalDate)) {
              earliestRecommendedIntervalDate = d;
            }
          }
          {
            Date d = interval.getLatestRecommendedInterval().getDateFrom(aar.getDateAdministered());
            if (latestRecommendedIntervalDate == null || d.after(latestRecommendedIntervalDate)) {
              latestRecommendedIntervalDate = d;
            }
          }
        }
      }
      if (minimumIntervalDate != null) {
        caMinimumIntervalDate.setInitialValue(minimumIntervalDate);
      }
      if (earliestRecommendedIntervalDate != null) {
        caEarliestRecommendedIntervalDate.setInitialValue(earliestRecommendedIntervalDate);
      }
      if (latestRecommendedIntervalDate != null) {
        caLatestRecommendedIntervalDate.setInitialValue(latestRecommendedIntervalDate);
      }

    }
    if (targetDose.getTrackedSeriesDose().getPreferrableVaccineList().size() > 0) {
      PreferrableVaccine pv = targetDose.getTrackedSeriesDose().getPreferrableVaccineList().get(0);
      caVaccineType.setInitialValue(pv.getVaccineType());
      caForecastVaccineType.setInitialValue(pv.getForecastVaccineType());
    }

  }

  @Override
  public LogicStep process() throws Exception {
    /**
     * ByPassing "For Each Patient Series"
     */

    generateForcastDates(forecast);
    forecast.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
    forecast.setTargetDose(dataModel.getTargetDose());
    Antigen newAntigenForeCast = forecast.getAntigen();
    List<Antigen> antigenFromForcastList = new ArrayList<Antigen>();
    List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
    for(Forecast forecast:forecastList){
    	antigenFromForcastList.add(forecast.getAntigen());
    }
    if(!antigenFromForcastList.contains(newAntigenForeCast)){
    	 dataModel.getVaccineGroupForecast().getForecastList().add(forecast);
    	 Date now = new Date();
    	 forecast.setAssessmentDate(now);
    }

    setNextLogicStepType(LogicStepType.FOR_EACH_PATIENT_SERIES);
    return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
    TablePre(out);
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
    TablePost(out);
  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + getTitle() + "</h1>");
    out.println(
        "<p>Generate forecast dates  and recommend vaccines  determines the forecast dates for the next  target dose  and identifies one  or  more recommended vaccines if the target dose warrants specific vaccine recommendations. The forecast dates are generated based on the patient’s immunization history. If the patient has not adhered to  the preferred schedule, then the forecast dates  are  adjusted to provide  the best  dates for the next target dose.</p>");
    out.println("<p>Figure 5-4 below provides an illustration of how forecast dates appear on the timeline.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private void insertTableRow(PrintWriter out, String BusinessRuleID, String Term, String BusinessRule) {
    out.println("  <tr>");
    out.println("    <th>" + BusinessRuleID + "</th>");
    out.println("    <th>" + Term + "</th> ");
    out.println("    <th>" + BusinessRule + "</th>");
    out.println("  </tr>");

  }

  private void TablePost(PrintWriter out) {
    insertTableRow(out, "BusinessRuleID", "Term", "BusinessRule");
    insertTableRow(out, "FORECASTDT-1", "Earliest Date", "");
    insertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date", "");
    insertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date", "");
    insertTableRow(out, "FORECASTDT-4", "Latest Date", "");
    insertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date", "");
    insertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date", "");
    insertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", "");
  }

  private Date getEarliestDate(List<Date> dateList) {
    Date tmp = null;
    for (Date item : dateList) {
      if (item != null && (tmp == null || item.before(tmp))) {
        tmp = item;
      }
    }
    return tmp;

  }

  private Date computeEarliestDate(Forecast forecast) {
    List<Date> list = new ArrayList<Date>();
    list.add(caMinimumAgeDate.getAssumedValue());
    list.add(caMinimumIntervalDate.getAssumedValue());
    list.add(caLatestConflictEndIntervalDate.getAssumedValue());
    list.add(caSeasonalRecommendationStartDate.getAssumedValue());
    Date earliestDate = getEarliestDate(list);
    forecast.setEarliestDate(earliestDate);
    return earliestDate;
  }

  private Date computeUnadjustedRecommandedDate(Forecast forecast) {
    Date unadjustedRecommandedDate = new Date();
    Date earliestRecommendedAgeDate = caEarliestRecommendedAgeDate.getAssumedValue();

    List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();
    int biggestAmount = 0;
    for (Interval interval : intervalList) {
      int tmp = interval.getEarliestRecommendedInterval().getAmount();
      if (tmp > biggestAmount) {
        biggestAmount = tmp;
      }
    }

    Date patientBirthDate = dataModel.getPatient().getDateOfBirth();
    Calendar c = Calendar.getInstance();
    c.setTime(patientBirthDate);
    c.add(Calendar.DATE, biggestAmount);
    Date latestOfAllEarliestRecommendedIntervalDates = c.getTime();

    Date forecastEarliestDate = computeEarliestDate(forecast);

    if (earliestRecommendedAgeDate != null) {
      unadjustedRecommandedDate = earliestRecommendedAgeDate;
    } else if (latestOfAllEarliestRecommendedIntervalDates != null) {
      unadjustedRecommandedDate = latestOfAllEarliestRecommendedIntervalDates;
    } else {
      unadjustedRecommandedDate = forecastEarliestDate;
    }

    forecast.setUnadjustedRecommendedDate(unadjustedRecommandedDate);
    return unadjustedRecommandedDate;
  }

  private Date computeUnadjustedPastDueDate(Forecast forecast) {

    Date unadjustedPastDueDate = new Date();
    Date latestRecommendedAgeDate = caLatestRecommendedAgeDate.getAssumedValue();

    List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();
    int biggestAmount = 0;
    for (Interval interval : intervalList) {
      int tmp = interval.getLatestRecommendedInterval().getAmount();
      if (tmp > biggestAmount) {
        biggestAmount = tmp;
      }
    }

    Date patientBirthDate = dataModel.getPatient().getDateOfBirth();
    Calendar c = Calendar.getInstance();
    c.setTime(patientBirthDate);
    c.add(Calendar.DATE, biggestAmount);
    Date latestOfAllLatestRecommendedIntervalDates = c.getTime();

    if (latestRecommendedAgeDate != null) {
      unadjustedPastDueDate = latestRecommendedAgeDate;
    } else if (latestOfAllLatestRecommendedIntervalDates != null) {
      unadjustedPastDueDate = latestOfAllLatestRecommendedIntervalDates;
    } else {
      unadjustedPastDueDate = null;
    }

    forecast.setUnadjustedPastDueDate(unadjustedPastDueDate);
    return unadjustedPastDueDate;
  }

  private Date computeLatestDate(Forecast forecast) {
    Date maximumAgeDate = caMaximumAgeDate.getAssumedValue();
    if (maximumAgeDate != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(maximumAgeDate);
      calendar.add(Calendar.DAY_OF_MONTH, -1);
      forecast.setLatestDate(calendar.getTime());
    }
    return forecast.getLatestDate();

  }

  private Date computeAdjustedRecommendedDate(Forecast forecast) {
    Date adjustedRecommendedDate = new Date();
    Date earliestDate = computeEarliestDate(forecast);
    Date unadjustedRecommendedDate = computeUnadjustedRecommandedDate(forecast);
    if (earliestDate.after(unadjustedRecommendedDate)) {
      adjustedRecommendedDate = earliestDate;
    } else {
      adjustedRecommendedDate = unadjustedRecommendedDate;
    }
    forecast.setAdjustedRecommendedDate(adjustedRecommendedDate);
    return adjustedRecommendedDate;
  }

  private Date computeAdjustedPastDueDate(Forecast forecast) {
    Date adjustedPastDueDate = new Date();
    Date earliestDate = computeEarliestDate(forecast);
    Date unadjustedPastDueDate = computeUnadjustedPastDueDate(forecast);
    if (unadjustedPastDueDate != null) {
      if (earliestDate.after(unadjustedPastDueDate)) {
        adjustedPastDueDate = earliestDate;
      } else {
        adjustedPastDueDate = unadjustedPastDueDate;
      }
    }
    forecast.setAdjustedPastDueDate(adjustedPastDueDate);
    return adjustedPastDueDate;

  }

  private List<Vaccine> recommendedVaccines() {
    List<Vaccine> vaccineList = new ArrayList<Vaccine>();
    List<AntigenSeries> antigenSeriesList = dataModel.getAntigenSeriesList();
    for (AntigenSeries antigenSeries : antigenSeriesList) {
      List<SeriesDose> serieDoseList = antigenSeries.getSeriesDoseList();
      for (SeriesDose serieDose : serieDoseList) {
        List<PreferrableVaccine> preferrableVaccinesList = serieDose.getPreferrableVaccineList();
        for (PreferrableVaccine preferrableVaccine : preferrableVaccinesList) {
          VaccineType vaccineType = preferrableVaccine.getVaccineType();
          if (vaccineType.equals(caForecastVaccineType.getAssumedValue())) {
            vaccineList.add(preferrableVaccine);
          }
        }
      }
    }

    return vaccineList;
  }

  private Forecast generateForcastDates(Forecast forecast) {
    forecast.setEarliestDate(computeEarliestDate(forecast));
    forecast.setUnadjustedRecommendedDate(computeUnadjustedRecommandedDate(forecast));
    forecast.setUnadjustedPastDueDate(computeUnadjustedPastDueDate(forecast));
    forecast.setLatestDate(computeLatestDate(forecast));
    forecast.setAdjustedRecommendedDate(computeAdjustedRecommendedDate(forecast));
    forecast.setAdjustedPastDueDate(computeAdjustedPastDueDate(forecast));
    return forecast;

  }

  private void TablePre(PrintWriter out) throws ParseException {
    Forecast forecast = new Forecast();
    insertTableRow(out, "BusinessRuleID", "Term", "BusinessRule");
    insertTableRow(out, "FORECASTDT-1", "Earliest Date", computeEarliestDate(forecast).toString());
    insertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date",
        computeUnadjustedPastDueDate(forecast).toString());
    insertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date", computeUnadjustedPastDueDate(forecast).toString());
    Date d = computeLatestDate(forecast);
    if (d == null) {
      insertTableRow(out, "FORECASTDT-4", "Latest Date", "null");
    } else {
      insertTableRow(out, "FORECASTDT-4", "Latest Date", d.toString());
    }
    insertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date",
        computeAdjustedRecommendedDate(forecast).toString());
    insertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date", computeAdjustedPastDueDate(forecast).toString());
    insertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", "recommendedVaccine");
  }


}
