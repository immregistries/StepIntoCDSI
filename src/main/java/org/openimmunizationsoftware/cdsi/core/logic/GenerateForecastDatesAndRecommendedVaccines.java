package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.springframework.web.client.HttpClientErrorException.Conflict;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_5;

public class GenerateForecastDatesAndRecommendedVaccines extends LogicStep {

  // TODO: Add rules FORECASTGUIDANCE-1, FORECASTDN-1
  // More TODOs below

  private ConditionAttribute<Date> caMinimumAgeDate = null;
  private ConditionAttribute<Date> caEarliestRecommendedAgeDate = null;
  private ConditionAttribute<Date> caLatestRecommendedAgeDate = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<List<Date>> caMinimumIntervalDates = null;
  private ConditionAttribute<List<Date>> caEarliestRecommendedIntervalDates = null;
  private ConditionAttribute<Date> caLatestRecommendedIntervalDate = null;
  private ConditionAttribute<Date> caLatestConflictEndIntervalDate = null;
  private ConditionAttribute<Date> caSeasonalRecommendationStartDate = null;
  private ConditionAttribute<VaccineType> caVaccineType = null;
  private ConditionAttribute<YesNo> caForecastVaccineType = null;

  private Date dob = dataModel.getPatient().getDateOfBirth();
  private SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();

  private void findMinimumAgeDate() {
    TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getMinimumAge();
    Date minimumAgeDate = timePeriod.getDateFrom(dob);
    caMinimumAgeDate.setInitialValue(minimumAgeDate);
  }

  private void findMaximumAgeDate() {
    TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getMaximumAge();
    if (timePeriod.isValued()) {
      Date maximumAgeDate = timePeriod.getDateFrom(dob);
      caMaximumAgeDate.setInitialValue(maximumAgeDate);
    }
  }

  private void findEarliestRecommendedAgeDate() {
    TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getEarliestRecommendedAge();
    Date earliestRecommendedAgeDate = timePeriod.getDateFrom(dob);
    caEarliestRecommendedAgeDate.setInitialValue(earliestRecommendedAgeDate);
  }

  private void findLatestRecommendedAgeDate() {
    TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getLatestRecommendedAge();
    Date latestRecommendedAgeDate = timePeriod.getDateFrom(dob);
    caLatestRecommendedAgeDate.setInitialValue(latestRecommendedAgeDate);
  }

  private Date computePatientReferenceDoseDate() {
    Interval tmpInterval = new Interval();
    //TODO update logic to intake interval and use correct Vaccine Dose Administered
    Date tmpPatientReferenceDoseDate = new Date();

    if(dataModel.getAntigenAdministeredRecord() == null) {
      return tmpPatientReferenceDoseDate;
    }

    VaccineDoseAdministered vda = dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered();
    
    if(vda.getTargetDose() == null) {
      return tmpPatientReferenceDoseDate;
    }
    Evaluation vdaEvaluation = vda.getTargetDose().getEvaluation();
    try {
      //CALCDTINT-1
      if(tmpInterval.getFromImmediatePreviousDoseAdministered().equals(YesNo.YES)) {
        if(vdaEvaluation.getEvaluationStatus().equals(EvaluationStatus.VALID)
        || vdaEvaluation.getEvaluationStatus().equals(EvaluationStatus.NOT_VALID)) {
          if(!vdaEvaluation.getEvaluationReason().equals(EvaluationReason.INADVERTENT_ADMINISTRATION)) {
            AntigenAdministeredRecord previousAAR = dataModel.getPreviousAntigenAdministeredRecord();
            tmpPatientReferenceDoseDate = previousAAR.getDateAdministered();
          }
        }
      }
      //CALCDTINT-2
      if(tmpInterval.getFromImmediatePreviousDoseAdministered().equals(YesNo.NO)) {
        if(!tmpInterval.getFromTargetDoseNumberInSeries().equals("")) {
          //TODO set tmpPatientReferenceDoseDate to 'the date administered of the vaccine dose administered that satisfies the target dose with the same target dose number as the from target dose number in series'
          //Maybe this?
          for(TargetDose td : dataModel.getTargetDoseList()) {
            if(tmpInterval.getFromTargetDoseNumberInSeries().equals(td.getTrackedSeriesDose().getDoseNumber())) {
              tmpPatientReferenceDoseDate = td.getSatisfiedByVaccineDoseAdministered().getDateAdministered();
            }
          }
        }
      }
      //CALCDTINT-8
      if(tmpInterval.getFromImmediatePreviousDoseAdministered().equals(YesNo.NO)) {
        //'from most recent vaccine type' does not exist. There should be an 'if' check here
        if(!vdaEvaluation.getEvaluationReason().equals(EvaluationReason.INADVERTENT_ADMINISTRATION)) {
          //TODO set tmpPatientReferenceDoseDate to 'the date administered of the most recent vaccine dose administered that is the same vaccine type as the from most recent vaccine type'
        }
      }
      //CALCDTINT-9
      if(tmpInterval.getFromImmediatePreviousDoseAdministered().equals(YesNo.NO)) {
        if(!tmpInterval.getFromRelevantObservation().getCode().equals("")) {
          //TODO set tmpPatientReferenceDoseDate to 'the observation date of the most recent active patient observation'
        }
      }
    } catch (NullPointerException np) {
      np.getCause();
    }
    return tmpPatientReferenceDoseDate;

  }

  private void findEarliestRecommendedIntervalDates() {
    List<Date> tmpEarliestRecommendedIntervalList = new ArrayList<Date>();
    try {
      for (Interval in : referenceSeriesDose.getAgeList().get(0).getSeriesDose()
          .getIntervalList()) {
        Date earliestRecommendedIntervalDate = CALCDTINT_5.evaluate(dataModel, this, in);
        tmpEarliestRecommendedIntervalList.add(earliestRecommendedIntervalDate);
      }
      if (tmpEarliestRecommendedIntervalList.size() > 0) {
        caEarliestRecommendedIntervalDates.setInitialValue(tmpEarliestRecommendedIntervalList);
      }
    } catch (NullPointerException np) {
      log("earliestRecommendedInterval is null");
    }

  }

  private void findLatestRecommendedIntervalDate() {
    TimePeriod latestRecommendedInterval;
    if (referenceSeriesDose == null) {
      log("referenceSeriesDose is null");
      return;
    }
    if (referenceSeriesDose.getAgeList() == null) {
      log("referenceSeriesDose.getAgeList() is null");
      return;
    }
    if (referenceSeriesDose.getAgeList().size() == 0) {
      log("referenceSeriesDose.getAgeList().size() is 0");
      return;
    }
    if (referenceSeriesDose.getAgeList().get(0) == null) {
      log("referenceSeriesDose.getAgeList().get(0) is null");
      return;
    }
    if (referenceSeriesDose.getAgeList().get(0).getSeriesDose() == null) {
      log("referenceSeriesDose.getAgeList().get(0).getSeriesDose() is null");
      return;
    }
    if (referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList() == null) {
      log("referenceSeriesDose.getAgeList().get(0).getSeriesDose():getIntervalList() is null");
      return;
    }
    if (referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList().size() == 0) {
      log("referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList().size() is 0");
      return;
    }
    if (referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList().get(0) == null) {
      log("referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList().get(0) is null");
      return;
    }
    latestRecommendedInterval = referenceSeriesDose.getAgeList().get(0).getSeriesDose()
        .getIntervalList().get(0).getLatestRecommendedInterval();
    Date patientReferenceDoseDate = computePatientReferenceDoseDate();
    Date latestRecommendedIntervalDate = latestRecommendedInterval.getDateFrom(patientReferenceDoseDate);
    caLatestRecommendedIntervalDate.setInitialValue(latestRecommendedIntervalDate);
  }

  private void findMinimumIntervalDates() {
    List<Date> minimumIntervalList = new ArrayList<Date>();
    if (referenceSeriesDose.getIntervalList() != null) {
      for (Interval minIn : referenceSeriesDose.getIntervalList()) {
        Date patientReferenceDoseDate = computePatientReferenceDoseDate();
        TimePeriod minimalIntervalFromReferenceSeriesDose = minIn.getMinimumInterval();
        if (minimalIntervalFromReferenceSeriesDose == null) {
          continue;
        }
        log("ADD adding to minimumIntervalList "
            + minimalIntervalFromReferenceSeriesDose.getDateFrom(patientReferenceDoseDate) + ",");
        minimumIntervalList.add(minimalIntervalFromReferenceSeriesDose.getDateFrom(patientReferenceDoseDate));
      }
      if (minimumIntervalList.size() > 0) {
        caMinimumIntervalDates.setInitialValue(minimumIntervalList);
      }
    } else {
      // log("nothing added to minimumIntervalList");
    }
  }

  private void findLatestConflictEndIntervalDate() {
    Date latestDate = null;
    List<Date> conflictEndIntervalDatesList = new ArrayList<>();

    // CALCDTCONFLICT-2, create list of conflict end interval dates
    for (LiveVirusConflict lvc : dataModel.getLiveVirusConflictList()) {
      boolean isImpactedVaccineDoseAdministered = false;
      boolean isPreviousVdaConflicting = false;

      if (dataModel.getAntigenAdministeredRecord().getVaccineType().equals(lvc.getCurrentVaccineType())) {
        isImpactedVaccineDoseAdministered = true;
        if (dataModel.getPreviousAntigenAdministeredRecord().getVaccineType().equals(lvc.getPreviousVaccineType())) {
          // if aar was administered before the conflict end date
          if (dataModel.getAntigenAdministeredRecord().getDateAdministered().before(lvc.getConflictBeginInterval()
              .getDateFrom(dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered()))) {
            isPreviousVdaConflicting = true;
          }
        }
      }

      if (isImpactedVaccineDoseAdministered && isPreviousVdaConflicting) {
        EvaluationStatus previousVdaStatus = dataModel.getPreviousAntigenAdministeredRecord()
            .getVaccineDoseAdministered().getTargetDose().getEvaluation().getEvaluationStatus();
        if (previousVdaStatus == EvaluationStatus.VALID || previousVdaStatus == null) {
          conflictEndIntervalDatesList.add(lvc.getMinimalConflictEndInterval()
              .getDateFrom(dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered()));
        }
        if (previousVdaStatus != null && previousVdaStatus != EvaluationStatus.VALID) {
          conflictEndIntervalDatesList.add(lvc.getConflictEndInterval()
              .getDateFrom(dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered()));
        }
      }
    }

    // CALCDTLIVE-4, which does not have logic defined in the 4.5 document, picks
    // latest date from list.
    for (Date d : conflictEndIntervalDatesList) {
      if (latestDate == null || d.after(latestDate)) {
        latestDate = d;
      }
    }
    caLatestConflictEndIntervalDate.setInitialValue(latestDate);
  }

  private void findSeasonalRecommendationStartDate() {
    SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    Date seasonalRecommendationStartDate = new DateTime(1900, 1, 1, 0, 0).toDate();
    caSeasonalRecommendationStartDate.setAssumedValue(seasonalRecommendationStartDate);
    if (referenceSeriesDose.getSeasonalRecommendationList().size() > 0) {
      seasonalRecommendationStartDate = referenceSeriesDose.getSeasonalRecommendationList().get(0)
          .getSeasonalRecommendationStartDate();
      caSeasonalRecommendationStartDate.setInitialValue(seasonalRecommendationStartDate);
    } else {
      // log("Couldn't find seasonalRecommendation start date");
    }

  }

  int size = dataModel.getVaccineGroupForecastList().size();

  public GenerateForecastDatesAndRecommendedVaccines(DataModel dataModel) {
    super(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES, dataModel);
    setConditionTableName("Table 7-12 GENERATE FORECAST DATE AND RECOMMENDED VACCINE ATTRIBUTES");

    caMinimumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-4)", "Minimum Age Date");
    caEarliestRecommendedAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-3)",
        "Earliest Recommended Age Date");
    caLatestRecommendedAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-2)",
        "Latest Recommended Age Date");
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)", "Maximum Age Date");
    caMinimumIntervalDates = new ConditionAttribute<List<Date>>("Calculated date (CALCDTINT-4)",
        "Minimum Interval Date(s)");
    caEarliestRecommendedIntervalDates = new ConditionAttribute<List<Date>>(
        "Calculated date (CALCDTINT-5)", "Earliest Recommended Interval Date(s)");
    caLatestRecommendedIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-6)",
        "Latest Recommended Interval Date(s)");
    caLatestConflictEndIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-4)",
        "Latest Conflict End Interval Date");
    caSeasonalRecommendationStartDate = new ConditionAttribute<Date>(
        "Supporting data (Seasonal Recommendation)", "Seasonal Recommendation Start Date");
    caVaccineType = new ConditionAttribute<VaccineType>("Supporting data (Preferable Vaccine",
        "Vaccine Type (CVX)");
    caForecastVaccineType = new ConditionAttribute<YesNo>("Supporting data (Preferable Vaccine)",
        "Forecast Vaccine Type");

    findMinimumAgeDate();
    findMaximumAgeDate();
    findEarliestRecommendedAgeDate();
    findLatestRecommendedAgeDate();
    findEarliestRecommendedIntervalDates();
    findLatestRecommendedIntervalDate();
    findMinimumIntervalDates();
    // findLatestConflictEndIntervalDate();
    findSeasonalRecommendationStartDate();

    caSeasonalRecommendationStartDate.setAssumedValue(PAST);
    caForecastVaccineType.setAssumedValue(YesNo.NO);

    conditionAttributesList.add(caMinimumAgeDate);
    conditionAttributesList.add(caEarliestRecommendedAgeDate);
    conditionAttributesList.add(caLatestRecommendedAgeDate);
    conditionAttributesList.add(caMaximumAgeDate);
    conditionAttributesList.add(caMinimumIntervalDates);
    conditionAttributesList.add(caEarliestRecommendedIntervalDates);
    conditionAttributesList.add(caLatestRecommendedIntervalDate);
    conditionAttributesList.add(caLatestConflictEndIntervalDate);
    conditionAttributesList.add(caSeasonalRecommendationStartDate);
    conditionAttributesList.add(caVaccineType);
    conditionAttributesList.add(caForecastVaccineType);

  }

  @Override
  public LogicStep process() throws Exception {
    /**
     * ByPassing "For Each Patient Series"
     */
    Forecast forecast = dataModel.getForecast();
    computeDates(forecast);

    Antigen newAntigenForeCast = forecast.getAntigen();
    List<Antigen> antigenFromForecastList = new ArrayList<Antigen>();
    List<Forecast> forecastList = dataModel.getForecastList();

    for (Forecast foreCast : forecastList) {
      antigenFromForecastList.add(foreCast.getAntigen());
    }

    if (!antigenFromForecastList.contains(newAntigenForeCast)) {
      forecastList.add(forecast);
    }
    dataModel.getPatientSeries().setForecast(forecast);
    List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();
    if (intervalList.size() > 0) {
      forecast.setInterval(intervalList.get(0));
    }

    setNextLogicStepType(LogicStepType.VALIDATE_RECOMMENDATION);
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
    out.println(
        "<p>Generate forecast dates  and recommend vaccines  determines the forecast dates for the next  target dose  and identifies one  or  more recommended vaccines if the target dose warrants specific vaccine recommendations. The forecast dates are generated based on the patient’s immunization history. If the patient has not adhered to  the preferred schedule, then the forecast dates  are  adjusted to provide  the best  dates for the next target dose.</p>");
    out.println(
        "<p>Figure 7-7 below provides an illustration of how forecast dates appear on the timeline.</p>");
    out.println("<img src=\"Figure 7.7.png\"/>");
    out.println("<p>FIGURE 7 - 7 FORECAST DATES TIMELINE</p>");
    out.println(
        "<p>The following process model, attribute table, and business rule table are used to generate forecast dates.If an attribute value is empty, then the date calculations will remain empty. No assumptions will be made for the attribute.</p>");
    out.println("<img src=\"Figure 7.8.png\"/>");
    out.print("<p>FIGURE 7 - 8 GENERATE FORECAST DATES AND RECOMMENDED VACCINE PROCESS MODEL</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
    out.println("<p>Patient Series Status: " + dataModel.getPatientSeries().getPatientSeriesStatus()
        + "</p>");
    out.println("<p>" + dataModel.getPatientSeries().getTrackedAntigenSeries().getSeriesName() + "</p>");
  }

  private void insertTableRow(PrintWriter out, String BusinessRuleID, String Term,
      String BusinessRule) {

    out.println("  <tr>");
    out.println("    <td>" + BusinessRuleID + "</td>");
    out.println("    <td>" + Term + "</td> ");
    out.println("    <td>" + BusinessRule + "</td>");
    out.println("  </tr>");

  }

  private void insertTableInit(PrintWriter out) {
    out.println("  <tr>");
    out.println("    <th> BusinessRuleID  </th>");
    out.println("    <th> Term </th> ");
    out.println("    <th> BusinessRule </th>");
    out.println("  </tr>");
  }

  public static Date getLatestDate(List<Date> dateList) {
    if (dateList == null || dateList.size() == 0) {
      return null;
    }
    if (dateList.size() == 1) {
      return dateList.get(0);
    }
    Date tmp = dateList.get(0);
    for (Date item : dateList) {
      if (tmp == null) {
        tmp = item;
      } else if (item != null && item.after(tmp)) {
        tmp = item;
      }
    }
    return tmp;

  }

  private Date computeEarliestDate() {
    // FORECASTDTCAN-1
    List<Date> list = new ArrayList<Date>();

    list.add(caMinimumAgeDate.getFinalValue());
    log("CONS Item for consideration for Earliest date is: " + caMinimumAgeDate.getAttributeName() + " with value of "
        + caMinimumAgeDate.getFinalValue());

    Date latestMinimumIntervalDate = getLatestDate(caMinimumIntervalDates.getFinalValue());
    list.add(latestMinimumIntervalDate);
    log("CONS Item for consideration for Earliest date is: " + caMinimumIntervalDates.getAttributeName()
        + " with value of "
        + caMinimumIntervalDates.getFinalValue());

    list.add(caLatestConflictEndIntervalDate.getFinalValue());
    log("CONS Item for consideration for Earliest date is: " + caLatestConflictEndIntervalDate.getAttributeName()
        + " with value of " + caLatestConflictEndIntervalDate.getFinalValue());

    list.add(caSeasonalRecommendationStartDate.getFinalValue());
    log("CONS Item for consideration for Earliest date is: " + caSeasonalRecommendationStartDate.getAttributeName()
        + " with value of " + caSeasonalRecommendationStartDate.getFinalValue());

    List<Date> allDatesAdministered = new ArrayList<Date>();
    for (AntigenAdministeredRecord aar : dataModel.getSelectedAntigenAdministeredRecordList()) {
      VaccineDoseAdministered vda = aar.getVaccineDoseAdministered();
      if (vda.getTargetDose() != null) {
        if (vda.getTargetDose().getEvaluation().getEvaluationReason()
            .equals(EvaluationReason.INADVERTENT_ADMINISTRATION)) {
          allDatesAdministered.add(vda.getDateAdministered());
        }
      }
    }
    // list.add(getLatestDate(allDatesAdministered));

    Date earliestDate = getLatestDate(list);
    return earliestDate;
  }

  private Date computeUnadjustedRecommendedDate() {
    Date unadjustedRecommendedDate = new Date();
    Date earliestRecommendedAgeDate = caEarliestRecommendedAgeDate.getFinalValue();
    List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();

    if (earliestRecommendedAgeDate != null) {
      unadjustedRecommendedDate = earliestRecommendedAgeDate;
      log("SET unadjusted recommended age date set to earliest recommended age date");
      return unadjustedRecommendedDate;
    } else {

      int biggestAmount = 0;
      for (Interval interval : intervalList) {
        int tmp = 0;
        switch (interval.getEarliestRecommendedInterval().getType()) {
          case DAY:
            tmp = interval.getEarliestRecommendedInterval().getAmount();
            break;
          case WEEK:
            tmp = 7 * interval.getEarliestRecommendedInterval().getAmount();
            break;
          case MONTH:
            tmp = 30 * interval.getEarliestRecommendedInterval().getAmount();
            break;
          case YEAR:
            tmp = 365 * interval.getEarliestRecommendedInterval().getAmount();
            break;
          default:
            break;
        }
        if (tmp > biggestAmount) {
          biggestAmount = tmp;
        }
      }
      Date patientReferenceDoseDate;
      try {
        /***
         * TO DO
         */
        patientReferenceDoseDate = dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered();
        unadjustedRecommendedDate = DateUtils.addDays(patientReferenceDoseDate, biggestAmount);
        if (biggestAmount != 0 || patientReferenceDoseDate != null) {
          unadjustedRecommendedDate = DateUtils.addDays(patientReferenceDoseDate, biggestAmount);
        } else {
          if (dataModel.getVaccineGroupForecastList().size() > 0) {
            Forecast forecast = dataModel.getVaccineGroupForecastList()
                .get(dataModel.getVaccineGroupForecastList().size() - 1);
            unadjustedRecommendedDate = forecast.getEarliestDate();
          }
        }
      } catch (NullPointerException np) {
      }
    }
    return unadjustedRecommendedDate;
  }

  private Date computeLatestDate() {
    Date latestDate = null;
    if (caMaximumAgeDate.getFinalValue() != null) {
      latestDate = DateUtils.addDays(caMaximumAgeDate.getFinalValue(), -1);
    }
    return latestDate;
  }

  private Date computeUnadjustedPastDueDate() {
    // TODO: Add instructions for what to do if no max age
    Date patientReferenceDoseDate = null;
    if (dataModel.getPreviousAntigenAdministeredRecord() != null) {
      patientReferenceDoseDate = dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered();
    }

    Date unadjustedPastDueDate = null;
    if (caLatestRecommendedAgeDate.getFinalValue() != null) {
      unadjustedPastDueDate = DateUtils.addDays(caLatestRecommendedAgeDate.getFinalValue(), -1);
    } else {
      // String log = "no patientReferenceDoseDate";
      if (patientReferenceDoseDate != null) {
        // log = "patientReferenceDoseDate: " + new
        // SimpleDateFormat("MM/dd/yyyy").format(patientReferenceDoseDate);
        List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();
        // log += ", intervalList.size() = " + intervalList.size();
        for (Interval interval : intervalList) {
          Date d = interval.getLatestRecommendedInterval().getDateFrom(patientReferenceDoseDate);
          // log += ", d = " + new SimpleDateFormat("MM/dd/yyyy").format(d);
          if (unadjustedPastDueDate == null || d.after(unadjustedPastDueDate)) {
            unadjustedPastDueDate = d;
          }
        }
      }
      // if (unadjustedPastDueDate == null) {
      // throw new NullPointerException(
      // "Unadjusted Past Due Date was not set " + log);
      // }
    }
    return unadjustedPastDueDate;
  }

  private Date computeAdjustedRecommendedDate() {
    // TODO: Change the first condition to the earliest date of the patient series
    // forecast.
    // TODO: Remove empty field specification (??)
    Date adjustedRecommendedDate = new Date();
    Date earliestDate = computeEarliestDate();
    Date unadjustedRecommendedDate = computeUnadjustedRecommendedDate();
    if (earliestDate.after(unadjustedRecommendedDate)) {
      adjustedRecommendedDate = earliestDate;
    } else {
      adjustedRecommendedDate = unadjustedRecommendedDate;
    }
    return adjustedRecommendedDate;
  }

  private Date computeAdjustedPastDueDate() {
    Date adjustedPastDueDate = null;
    Date earliestDate = computeEarliestDate();
    Date unadjustedPastDueDate = computeUnadjustedPastDueDate();
    if (unadjustedPastDueDate != null && earliestDate.after(unadjustedPastDueDate)) {
      adjustedPastDueDate = earliestDate;
    } else {
      adjustedPastDueDate = unadjustedPastDueDate;
    }
    return adjustedPastDueDate;

  }

  private List<Vaccine> recommendedVaccines() {
    // TODO: Completely rework
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

  private void computeDates(Forecast forecast) {
    forecast.setAdjustedPastDueDate(computeAdjustedPastDueDate());
    Date d = computeAdjustedRecommendedDate();
    forecast.setAdjustedRecommendedDate(d);
    forecast.setEarliestDate(computeEarliestDate());
    forecast.setLatestDate(computeLatestDate());
  }

  private void TablePre(PrintWriter out) {
    out.println("<p>TABLE 7 - 13 GENERATE FORECAST DATE AND RECOMMENDED VACCINE BUSINESS RULES</p>");
    out.println("<table BORDER=\"1\"> ");
    insertTableInit(out);
    insertTableRow(out, "FORECASTDT-1", "Earliest Date", "");
    insertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date", "");
    insertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date", "");
    insertTableRow(out, "FORECASTDT-4", "Latest Date", "");
    insertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date", "");
    insertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date", "");
    insertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", "");
    out.println("</table>");
  }

  private void TablePost(PrintWriter out) throws ParseException {
    out.println(
        "<p> TABLE 7 - 13 GENERATE FORECAST DATE AND RECOMMENDED VACCINE BUSINESS RULES</p>");
    out.println("<table BORDER=\"1\"> ");
    insertTableInit(out);
    insertTableRow(out, "FORECASTDT-1", "Earliest Date", computeEarliestDate().toString());
    insertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date", computeUnadjustedRecommendedDate().toString());
    insertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date", n(computeUnadjustedPastDueDate()));
    insertTableRow(out, "FORECASTDT-4", "Latest Date", n(computeUnadjustedPastDueDate()));
    insertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date", computeAdjustedRecommendedDate().toString());
    insertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date", n(computeAdjustedPastDueDate()));
    insertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", "recommendedVaccine");
    out.println("</table>");
  }

}
