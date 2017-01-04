package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class GenerateForecastDates extends LogicStep
{

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
  
  private ConditionAttribute<Date> getEarliest (List<ConditionAttribute<Date>> list){
	  ConditionAttribute<Date> tmp = list.get(0);
	  for(ConditionAttribute<Date> item:list){
		  if(item.getAssumedValue().before(tmp.getAssumedValue())){
			  tmp = item;
		  }
	  }
	  return tmp;
	  
  }
  
  private ConditionAttribute<Date> getLatest (List<ConditionAttribute<Date>> list){
	  ConditionAttribute<Date> tmp = list.get(0);
	  for(ConditionAttribute<Date> item:list){
		  if(item.getAssumedValue().after(tmp.getAssumedValue())){
			  tmp = item;
		  }
	  }
	  return tmp;
	  
  }
  
  private String earliestDate(){
	  List<ConditionAttribute<Date>> list = new ArrayList<ConditionAttribute<Date>>() ;
	  list.add(caMinimumAgeDate);
	  list.add(caMinimumIntervalDate);
	  list.add(caLatestConflictEndIntervalDate);
	  list.add(caSeasonalRecommendationStartDate);
	  
	  return getEarliest(list).toString();
	
  }

  private TimePeriod getLatest(List<TimePeriod> timePeriodList){
	return timePeriodList.get(0);
	  
  }
  //Where can I get the The forecast earliest date
  private String unadjustedRecommandedDate(){
	  List<TimePeriod> earliestRecommendedIntervalList = new ArrayList<TimePeriod>();
	  List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();
	  for(Interval interval:intervalList){
		  earliestRecommendedIntervalList.add(interval.getEarliestRecommendedInterval());
	  }
	  if(caEarliestRecommendedAgeDate.getAssumedValue()!=null){
		  return caEarliestRecommendedAgeDate.toString();
	  }else if(caEarliestRecommendedAgeDate.getAssumedValue()==null && earliestRecommendedIntervalList!=null){
		  getLatest(earliestRecommendedIntervalList).toString();
	  }else{
		  dataModel.getVaccineGroupMap();
	  }
	  
	return null;
	  
  }
  
  private String unadjastedPastDueDate(){
	  Date entryDate = caLatestRecommendedAgeDate.getAssumedValue();
	  Date d1 = new Date(1);
	  long diff  = entryDate.getTime() - d1.getTime();
	  Date latestRecommandedAgeDate = new Date(diff);
	  
	  List<TimePeriod> latestRecommendedIntervalList = new ArrayList<TimePeriod>();
	  List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();
	  for(Interval interval:intervalList){
		  latestRecommendedIntervalList.add(interval.getLatestRecommendedInterval());
	  }
	  
	  TimePeriod latestLatestRecommendedIntervalList = getLatest(latestRecommendedIntervalList);
	  if(latestRecommandedAgeDate!=null && latestRecommendedIntervalList.isEmpty()){
		  return latestRecommandedAgeDate.toString();
	  }
	  if(!latestRecommendedIntervalList.isEmpty()){
		  return latestLatestRecommendedIntervalList.toString();
	  }
	  return null;
  }
  
  private String latestDate(){
	  Date maximumAgeDate = caMaximumAgeDate.getAssumedValue();
	  Date d1 = new Date(1);
	  long diff = maximumAgeDate.getTime() - d1.getTime();
	  Date latestDate = new Date(diff);	   
	  return latestDate.toString();

  }
  
  private String adjustedRecommendedDate() throws ParseException{
	  DateFormat formatter = new SimpleDateFormat("MM/dd/yy");  //Check system format
	  Date d1 = formatter.parse(earliestDate());
	  Date d2 = formatter.parse(unadjustedRecommandedDate());
	  if(d1.after(d2)){
		  return d1.toString();
	  }else{
		  return d2.toString();
	  }  
  }
  
  private String adjustedPastDueDate() throws ParseException{
	  if(unadjastedPastDueDate()==null){
		  return null;
	  }else{
		  DateFormat formatter = new SimpleDateFormat("MM/dd/yy");  //Check system format
		  Date d1 = formatter.parse(earliestDate());
		  Date d2 = formatter.parse(unadjastedPastDueDate());
		  if(d1.after(d2)){
			  return d1.toString();
		  }else{
			  return d2.toString();
		  }  
		  
	  }
  }
  
  private String recommendedVaccine(){
	  //ToDO
	  return null;
  }
  
  private void TablePre (PrintWriter out) throws ParseException{
	  insertTableRow(out, "BusinessRuleID", "Term", "BusinessRule");
	  insertTableRow(out, "FORECASTDT-1", "Earliest Date", earliestDate());
	  insertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date", unadjustedRecommandedDate());
	  insertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date", unadjastedPastDueDate());
	  insertTableRow(out, "FORECASTDT-4", "Latest Date", latestDate());
	  insertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date",adjustedRecommendedDate());
	  insertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date", adjustedPastDueDate());
	  insertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", recommendedVaccine()); 
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
