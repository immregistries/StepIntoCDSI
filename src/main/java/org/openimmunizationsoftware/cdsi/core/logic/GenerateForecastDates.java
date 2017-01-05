package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class GenerateForecastDates extends LogicStep
{
	
  private Forecast forecast = new Forecast();
  
  private ConditionAttribute<Date> caMinimumAgeDate = null;
  private ConditionAttribute<Date> caEarliestRecommendedAgeDate = null;
  private ConditionAttribute<Date> caLatestRecommendedAgeDate = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<Date> caMinimumIntervalDate = null;
  private ConditionAttribute<Date> caEarliestRecommendedIntervalDate = null;
  private ConditionAttribute<Date> caLatestRecommendedIntervalDate = null;
  private ConditionAttribute<Date> caLatestConflictEndIntervalDate = null;
  private ConditionAttribute<Date> caSeasonalRecommendationStartDate = null;
  private ConditionAttribute<String> caVaccineType = null;
  private ConditionAttribute<String> caForecastVaccineType = null;

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
    caVaccineType = new ConditionAttribute<String>("Supporting data (Preferable Vaccine", "Vaccine Type (CVX)");
    caForecastVaccineType = new ConditionAttribute<String>("Supporting data (Preferable Vaccine)",
        "Forecast Vaccine Type");

    caSeasonalRecommendationStartDate.setAssumedValue(PAST);
    caForecastVaccineType.setAssumedValue("N");

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

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
	  /**
	   * ByPassing "For Each Patient Series"
	   */
    //setNextLogicStepType(LogicStepType.FOR_EACH_PATIENT_SERIES);
	  setNextLogicStepType(LogicStepType.SELECT_BEST_PATIENT_SERIES);
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
    out.println("<p>Generate forecast dates  and recommend vaccines  determines the forecast dates for the next  target dose  and identifies one  or  more recommended vaccines if the target dose warrants specific vaccine recommendations. The forecast dates are generated based on the patient’s immunization history. If the patient has not adhered to  the preferred schedule, then the forecast dates  are  adjusted to provide  the best  dates for the next target dose.</p>");
    out.println("<p>Figure 5-4 below provides an illustration of how forecast dates appear on the timeline.</p>");
    
    printConditionAttributesTable(out);
    printLogicTables(out);
  }
  
  
  
  private void insertTableRow(PrintWriter out,String BusinessRuleID, String Term, String  BusinessRule){
	  out.println("  <tr>");
	  out.println("    <th>"+BusinessRuleID+"</th>");
	  out.println("    <th>"+Term+"</th> ");
	  out.println("    <th>"+BusinessRule+"</th>");
	  out.println("  </tr>");
	  
  }
  
  private void TablePost (PrintWriter out){
	  insertTableRow(out, "BusinessRuleID", "Term", "BusinessRule");
	  insertTableRow(out, "FORECASTDT-1", "Earliest Date", "");
	  insertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date", "");
	  insertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date", "");
	  insertTableRow(out, "FORECASTDT-4", "Latest Date", "");
	  insertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date","");
	  insertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date", "");
	  insertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", ""); 
  }
  
  private Date getEarliestDate (List<Date> dateList){
	  Date tmp = dateList.get(0);
	  for(Date item:dateList){
		  if(item.before(tmp)){
			  tmp = item;
		  }
	  }
	  return tmp;
	  
  }
    
  private Date computeEarliestDate(){
	  List<Date> list = new ArrayList<Date>() ;
	  list.add(caMinimumAgeDate.getAssumedValue());
	  list.add(caMinimumIntervalDate.getAssumedValue());
	  list.add(caLatestConflictEndIntervalDate.getAssumedValue());
	  list.add(caSeasonalRecommendationStartDate.getAssumedValue());
	  Date earliestDate = getEarliestDate(list);
	  forecast.setEarliestDate(earliestDate);
	  
	  return earliestDate;
	
  }
  
  private Date computeUnadjustedRecommandedDate(){
	  Date unadjustedRecommandedDate = new Date();
	  Date earliestRecommendedAgeDate = caEarliestRecommendedAgeDate.getAssumedValue();
	  
	  List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();
	  int biggestAmount = 0;
	  for(Interval interval:intervalList){
		  int tmp = interval.getEarliestRecommendedInterval().getAmount();
		  if(tmp>biggestAmount){
			  biggestAmount = tmp;
		  }
	  }
	  
	  Date patientBirthDate = dataModel.getPatient().getDateOfBirth();
	  Calendar c = Calendar.getInstance();
	  c.setTime(patientBirthDate); 
	  c.add(Calendar.DATE, biggestAmount); 
	  Date  latestOfAllEarliestRecommendedIntervalDates = c.getTime();
	  
	  Date forecastEarliestDate = computeEarliestDate();
	  
	  
	  if(earliestRecommendedAgeDate!=null){
		  unadjustedRecommandedDate = earliestRecommendedAgeDate;
	  }else if(latestOfAllEarliestRecommendedIntervalDates!=null){
		  unadjustedRecommandedDate =  latestOfAllEarliestRecommendedIntervalDates;
	  }else{  
		  unadjustedRecommandedDate = forecastEarliestDate;
	  }
	  
	  forecast.setUnadjustedRecommendedDate(unadjustedRecommandedDate);
	  return unadjustedRecommandedDate;
  }
  
  
  private Date computeUnadjustedPastDueDate(){
	  
	  Date unadjustedPastDueDate = new Date();
	  Date latestRecommendedAgeDate = caLatestRecommendedAgeDate.getAssumedValue();
	  
	  List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();
	  int biggestAmount = 0;
	  for(Interval interval:intervalList){
		  int tmp = interval.getLatestRecommendedInterval().getAmount();
		  if(tmp>biggestAmount){
			  biggestAmount = tmp;
		  }
	  }
	  
	  Date patientBirthDate = dataModel.getPatient().getDateOfBirth();
	  Calendar c = Calendar.getInstance();
	  c.setTime(patientBirthDate); 
	  c.add(Calendar.DATE, biggestAmount); 
	  Date  latestOfAllLatestRecommendedIntervalDates = c.getTime();
	  	  
	  
	  if(latestRecommendedAgeDate!=null){
		  unadjustedPastDueDate = latestRecommendedAgeDate;
	  }else if(latestOfAllLatestRecommendedIntervalDates!=null){
		  unadjustedPastDueDate =  latestOfAllLatestRecommendedIntervalDates;
	  }else{  
		  unadjustedPastDueDate = null;
	  }
	  
	  forecast.setUnadjustedPastDueDate(unadjustedPastDueDate);
	  return unadjustedPastDueDate;
	  }
  
  private Date computeLatestDate(){
	  Date maximumAgeDate = caMaximumAgeDate.getAssumedValue();
	  Date d1 = new Date(1);
	  long diff = maximumAgeDate.getTime() - d1.getTime();
	  Date latestDate = new Date(diff);
	  forecast.setLatestDate(latestDate);
	  return latestDate;

  }
  
  private Date computeAdjustedRecommendedDate(){
	  Date adjustedRecommendedDate = new Date();
	  Date earliestDate = computeEarliestDate();
	  Date unadjustedRecommendedDate = computeUnadjustedRecommandedDate();
	  if(earliestDate.after(unadjustedRecommendedDate)){
		  adjustedRecommendedDate = earliestDate;
	  }else{
		  adjustedRecommendedDate = unadjustedRecommendedDate;
	  } 
	  forecast.setAdjustedRecommendedDate(adjustedRecommendedDate);
	  return adjustedRecommendedDate;
  }
  
  private Date computeAdjustedPastDueDate() {
	  Date adjustedPastDueDate = new Date();
	  Date earliestDate = computeAdjustedPastDueDate();
	  Date unadjustedPastDueDate = computeUnadjustedPastDueDate();
	  if(unadjustedPastDueDate!=null){
		  if(earliestDate.after(unadjustedPastDueDate)){
			  adjustedPastDueDate = earliestDate;
		  }else{
			  adjustedPastDueDate = unadjustedPastDueDate;
		  }
	  }
	  forecast.setAdjustedPastDueDate(adjustedPastDueDate);
	  return adjustedPastDueDate;

  }
  
  
  private List<Vaccine> recommendedVaccines(){
	  List<Vaccine> vaccineList = new ArrayList<Vaccine>();
	  List<AntigenSeries> antigenSeriesList = dataModel.getAntigenSeriesList();
	  for (AntigenSeries antigenSeries:antigenSeriesList){
		  List<SeriesDose> serieDoseList = antigenSeries.getSeriesDoseList();
		  for(SeriesDose serieDose:serieDoseList){
			  List<PreferrableVaccine> preferrableVaccinesList = serieDose.getPreferrableVaccineList();
			  for(PreferrableVaccine preferrableVaccine:preferrableVaccinesList){
				  VaccineType vaccineType = preferrableVaccine.getVaccineType();
				  if(vaccineType.equals(caForecastVaccineType.getAssumedValue())){
					  vaccineList.add(preferrableVaccine);
				  }
			  }
		  }
	  }
	  
	  return vaccineList;
  }
  
  private Forecast generateForcastDate(){
	  forecast.setEarliestDate(computeEarliestDate());
	  forecast.setUnadjustedRecommendedDate(computeUnadjustedRecommandedDate());
	  forecast.setUnadjustedPastDueDate(computeUnadjustedPastDueDate());
	  forecast.setLatestDate(computeLatestDate());
	  forecast.setAdjustedRecommendedDate(computeAdjustedRecommendedDate());
	  forecast.setAdjustedPastDueDate(computeAdjustedPastDueDate());
	  return forecast;
	  
	  
  }
  
  private void TablePre (PrintWriter out) throws ParseException{
	  insertTableRow(out, "BusinessRuleID", "Term", "BusinessRule");
	  insertTableRow(out, "FORECASTDT-1", "Earliest Date", computeEarliestDate().toString());
	  insertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date", computeUnadjustedPastDueDate().toString());
	  insertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date", computeUnadjustedPastDueDate().toString());
	  insertTableRow(out, "FORECASTDT-4", "Latest Date", computeLatestDate().toString());
	  insertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date",computeAdjustedRecommendedDate().toString());
	  insertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date", computeAdjustedPastDueDate().toString());
	  insertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", "recommendedVaccine"); 
  }


  private class LT extends LogicTable
  {
    public LT() {
      super(0, 0, "Table ?-?");

      //      setLogicCondition(0, new LogicCondition("date administered > lot expiration date?") {
      //        @Override
      //        public LogicResult evaluateInternal() {
      //          if (caDateAdministered.getFinalValue() == null || caTriggerAgeDate.getFinalValue() == null) {
      //            return LogicResult.NO;
      //          }
      //          if (caDateAdministered.getFinalValue().before(caTriggerAgeDate.getFinalValue())) {
      //            return LogicResult.YES;
      //          }
      //          return LogicResult.NO;
      //        }
      //      });

      //      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.ANY);

      //      setLogicOutcome(0, new LogicOutcome() {
      //        @Override
      //        public void perform() {
      //          log("No. The target dose cannot be skipped. ");
      //          log("Setting next step: 4.3 Substitute Target Dose");
      //          setNextLogicStep(LogicStep.SUBSTITUTE_TARGET_DOSE_FOR_EVALUATION);
      //        }
      //      });
      //      
    }
  }

}
