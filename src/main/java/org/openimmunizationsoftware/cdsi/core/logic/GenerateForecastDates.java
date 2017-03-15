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
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriodType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;

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
  
  private Date dob = dataModel.getPatient().getDateOfBirth();
  private SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
  private Date patientReferenceDoseDate = computePatientReferenceDoseDate();

  
  public Date addTimePeriodtotoDate(Date date, TimePeriod timePeriod){
		int amount = timePeriod.getAmount();
		TimePeriodType  type = timePeriod.getType();
		switch (type) {
			case DAY:
	  		date = DateUtils.addDays(date, amount);    		
				break;
			case WEEK:
	  		date = DateUtils.addWeeks(date, amount);    		
				break;
			case MONTH:
	  		date = DateUtils.addMonths(date, amount);    		
				break;
			case YEAR:
	  		date = DateUtils.addYears(date, amount);    		
				break;
			default:
				break;
			}
		return date;
	}
   
  
  private void findMinimumAgeDate(){
	  TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getMaximumAge();
	  Date minimumAgeDate = addTimePeriodtotoDate(dob, timePeriod);
	  caMinimumAgeDate.setInitialValue(minimumAgeDate);
  }
  
  private void findMaximumAgeDate(){
	  TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getMaximumAge();
	  Date maximumAgeDate = addTimePeriodtotoDate(dob, timePeriod);
	  caMaximumAgeDate.setInitialValue(maximumAgeDate);
  }
  
  private void findEarliestRecommendedAgeDate(){
	  TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getEarliestRecommendedAge();
	  Date earliestRecommendedAgeDate = addTimePeriodtotoDate(dob, timePeriod);
	  caEarliestRecommendedAgeDate.setInitialValue(earliestRecommendedAgeDate);  
  }
  
  private void findLatestRecommendedAgeDate(){
	  TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getLatestRecommendedAge();
	  Date latestRecommendedAgeDate  = addTimePeriodtotoDate(dob, timePeriod);
	  caLatestRecommendedAgeDate.setInitialValue(latestRecommendedAgeDate);
	  
  }
  
  private Date computePatientReferenceDoseDate(){
	  Date tmppatientReferenceDoseDate =  new Date();
//	  try{
//		  	  AntigenAdministeredRecord prevAar = dataModel.getPreviousAntigenAdministeredRecord();
//	  //System.err.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@ prevAar : "+prevAar.toString());
//	  VaccineDoseAdministered prevVacine = prevAar.getVaccineDoseAdministered();
//	  System.err.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@ prevVaccine_AntigenName : "+prevVacine.getAntigenAssigned().getName());
//	  }catch(NullPointerException p){
//		  p.printStackTrace();
//	  }
//
////	  System.err.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@ EvaluationMapSize : "+prevVacine.getEvaluationMap().size());
//	  //Date tmpPtientReferenceDoseDate1 = new Date();
//	  /*try{
//			 /***
//			  * TO DO
//			  */
//			// tmpPtientReferenceDoseDate1 = dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered();
//		// }catch(NullPointerException np){
//			 //TO DO
//		// }
  
	  return tmppatientReferenceDoseDate;
  }
  
  
  private void findEarliestRecommendedIntervalDate(){
	  TimePeriod earliestRecommendedInterval;
	  try{
		 earliestRecommendedInterval =  referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList().get(0).getEarliestRecommendedInterval();
		 Date earliestRecommendedIntervalDate = new Date();
		 earliestRecommendedIntervalDate = addTimePeriodtotoDate(patientReferenceDoseDate, earliestRecommendedInterval);
		 caEarliestRecommendedIntervalDate.setInitialValue(earliestRecommendedIntervalDate);
	  }catch(NullPointerException np){
		  ////System.err.println("earliestRecommendedInterval is null");
	  }	  
  }
  
  
  private void findLatestRecommendedIntervalDate(){
	  TimePeriod latestRecommendedInterval;
	  try{
		 latestRecommendedInterval =  referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList().get(0).getLatestRecommendedInterval();
		 Date latestRecommendedIntervalDate = new Date();
		 //Date patientReferenceDoseDate = new Date();
		 latestRecommendedIntervalDate = addTimePeriodtotoDate(patientReferenceDoseDate, latestRecommendedInterval);
		 caLatestRecommendedIntervalDate.setInitialValue(latestRecommendedIntervalDate);
	  }catch(NullPointerException np){
		  ////System.err.println("latestRecommendedInterval is null");
	  }	  
  }
  
  private void findMinimalIntervalDate(){
	  TimePeriod minimalInterval;
	  try{
		 minimalInterval =  referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList().get(0).getMinimumInterval();
		 Date minimalIntervalDate = new Date();
		 Date patientReferenceDoseDate = new Date();		
		 minimalIntervalDate = addTimePeriodtotoDate(patientReferenceDoseDate, minimalInterval);
		 System.out.println("minimalInterval "+minimalInterval);
		 caMinimumIntervalDate.setInitialValue(minimalIntervalDate);
	  }catch(NullPointerException np){
		  System.err.println("MinimumIntervalDate is null");
	  }	  
  }
  private void findLatestConflictEndIntervalDate(){
	  try{
		  //////System.err.println("####################"+dataModel.getLiveVirusConflictList().size());
	  }catch (NullPointerException np) {
		////System.err.println(np.getStackTrace());
	}
  }
  
  private void findSeasonalRecommendationStartDate(){
	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose(); // might be the problem here
	  Date seasonalRecommendationstartDate = new DateTime(1900, 1, 1, 0, 0).toDate();
	  caSeasonalRecommendationStartDate.setAssumedValue(seasonalRecommendationstartDate);
	    if(referenceSeriesDose.getSeasonalRecommendationList().size()>0){
	    	seasonalRecommendationstartDate = referenceSeriesDose.getSeasonalRecommendationList().get(0).getSeasonalRecommendationStartDate();
	    	caSeasonalRecommendationStartDate.setInitialValue(seasonalRecommendationstartDate);
	    }else{
	    	////System.err.println("Couldn't find seasonalRecommandation start date");
	    }
	    
  }
  
  int size = dataModel.getVaccineGroupForcastList().size();

  public GenerateForecastDates(DataModel dataModel) {
    super(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES, dataModel);
    setConditionTableName("Table ");

    caMinimumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-4)", "Minimum Age Date");
    caEarliestRecommendedAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-3)",  "Earliest Recommended Age Date");
    caLatestRecommendedAgeDate = new ConditionAttribute<Date>("Calcualted date (CALCDTAGE-2)",  "Latest Recommended Age Date");
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)", "Maximum Age Date");
    caMinimumIntervalDate = new ConditionAttribute<Date>("Calcualated date (CALCDTINT-4)", "Minimal Interval Date(s)");
    caEarliestRecommendedIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-5)", "Earliest Recommended Interval Date(s)");
    caLatestRecommendedIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-6)", "Lastest Recommended Interval Date(s)");
    caLatestConflictEndIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-4)", "Latest Conflict End Interval Date");
    caSeasonalRecommendationStartDate = new ConditionAttribute<Date>("Supporting data (Seasonal Recommendation)", "Seasonal Recommendation Start Date");
    caVaccineType = new ConditionAttribute<VaccineType>("Supporting data (Preferable Vaccine", "Vaccine Type (CVX)");
    caForecastVaccineType = new ConditionAttribute<YesNo>("Supporting data (Preferable Vaccine)", "Forecast Vaccine Type");
   
    findMinimumAgeDate();
    findMaximumAgeDate();
    findEarliestRecommendedAgeDate();
    findLatestRecommendedAgeDate();
    findEarliestRecommendedIntervalDate();
    findLatestRecommendedIntervalDate();
    findMinimalIntervalDate();
    findLatestConflictEndIntervalDate();
    findSeasonalRecommendationStartDate();  
    
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

  }

  @Override
  public LogicStep process() throws Exception {
    /**
     * ByPassing "For Each Patient Series"
     */
	  Forecast forecast = new Forecast();
	  forecast.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
	  forecast.setTargetDose(dataModel.getTargetDose());
	  generateForcastDates(forecast);
	  
	  Antigen newAntigenForeCast = forecast.getAntigen();
	  List<Antigen> antigenFromForcastList = new ArrayList<Antigen>();
	  List<Forecast> forecastList = dataModel.getForecastList();
	  
    for(Forecast foreCast:forecastList){
    	antigenFromForcastList.add(foreCast.getAntigen());
    }
    
    if(!antigenFromForcastList.contains(newAntigenForeCast)){
      forecastList.add(forecast);
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
    out.println("<img src=\"Figure 5.4.png\"/>");
    out.println("<p>FIGURE 5 - 4 FORECAST DATES TIMELINE</p>");
    out.println("<p>The following process model, attribute table, and business rule table are used to generate forecast dates.If an attribute value is empty, then the date calculations will remain empty. No assumptions will be made for the attribute.</p>");
    out.println("<img src=\"Figure 5.5.png\"/>");
    out.print("<p>FIGURE 5 - 5GENERATE FORECAST DATESAND RECOMMENDED VACCINE PROCESS MODEL</p>");
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

  private void TablePre(PrintWriter out) {
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
      if (item != null && (tmp == null || item.after(tmp))) {
        tmp = item;
      }
    }
    return tmp;

  }

  private Date computeEarliestDate() {
    List<Date> list = new ArrayList<Date>();
    list.add(caMinimumAgeDate.getFinalValue());
    list.add(caMinimumIntervalDate.getFinalValue());
    list.add(caLatestConflictEndIntervalDate.getFinalValue());
    list.add(caSeasonalRecommendationStartDate.getFinalValue());
    Date earliestDate = getEarliestDate(list);
    return earliestDate;
  }

  private Date computeUnadjustedRecommandedDate() {
    Date unadjustedRecommandedDate = new Date();
    Date earliestRecommendedAgeDate = caEarliestRecommendedAgeDate.getFinalValue();
    if(earliestRecommendedAgeDate!=null){
    	unadjustedRecommandedDate = earliestRecommendedAgeDate;
    	return unadjustedRecommandedDate;
    }else{
    	List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();
    	int biggeestAmout = 0;
    	for(Interval interval:intervalList){
    		int tmp = 0;
   		 	switch (interval.getEarliestRecommendedInterval().getType()) {
 			case DAY:
 				tmp = interval.getEarliestRecommendedInterval().getAmount();
 				break;
 			case WEEK:
 				tmp = 7*interval.getEarliestRecommendedInterval().getAmount();    		
 				break;
 			case MONTH:
 				tmp = 30*interval.getEarliestRecommendedInterval().getAmount();  		
 				break;
 			case YEAR:
 				tmp = 365* interval.getEarliestRecommendedInterval().getAmount();		
 				break;
 			default:
 				break;
 			}
   		 	if(tmp>biggeestAmout){
   		 		biggeestAmout = tmp;
   		 	}
    	}
    	 Date patientReferenceDoseDate;
		 try{
			 /***
			  * TO DO
			  */
			 patientReferenceDoseDate = dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered();
			 unadjustedRecommandedDate = DateUtils.addDays(patientReferenceDoseDate, biggeestAmout); 
			 if(biggeestAmout!=0 || patientReferenceDoseDate!=null){
				 unadjustedRecommandedDate = DateUtils.addDays(patientReferenceDoseDate, biggeestAmout);
			 }else{
				 if(dataModel.getVaccineGroupForcastList().size()>0){
					 Forecast forecast = dataModel.getVaccineGroupForcastList().get(dataModel.getVaccineGroupForcastList().size()-1);
					 unadjustedRecommandedDate = forecast.getEarliestDate();
				 } 
			 }	 
		 }catch(NullPointerException np){
		 }	
    }  
    return unadjustedRecommandedDate;
  }

  private Date computeUnadjustedPastDueDate() {

    Date unadjustedPastDueDate = new Date();
    if(caLatestRecommendedAgeDate.getFinalValue()!=null){
    	unadjustedPastDueDate = DateUtils.addDays(caLatestRecommendedAgeDate.getFinalValue(),-1);
    }else{
    	List<Interval> intervalList = dataModel.getTargetDose().getTrackedSeriesDose().getIntervalList();
    	int biggeestAmout = 0;
    	for(Interval interval:intervalList){
    		int tmp = 0;
   		 	switch (interval.getLatestRecommendedInterval().getType()) {
 			case DAY:
 				tmp = interval.getLatestRecommendedInterval().getAmount();
 				break;
 			case WEEK:
 				tmp = 7*interval.getLatestRecommendedInterval().getAmount();    		
 				break;
 			case MONTH:
 				tmp = 30*interval.getLatestRecommendedInterval().getAmount();  		
 				break;
 			case YEAR:
 				tmp = 365* interval.getLatestRecommendedInterval().getAmount();		
 				break;
 			default:
 				break;
 			}
   		 	if(tmp>biggeestAmout){
   		 		biggeestAmout = tmp;
   		 	}
   		 	biggeestAmout = biggeestAmout-1;
    	}
    	Date patientReferenceDoseDate;
		 try{
			 /***
			  * TO DO
			  */
			 patientReferenceDoseDate = dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered();
			 unadjustedPastDueDate = DateUtils.addDays(patientReferenceDoseDate, biggeestAmout); 
			 if(biggeestAmout!=0 || patientReferenceDoseDate!=null){
				 unadjustedPastDueDate = DateUtils.addDays(patientReferenceDoseDate, biggeestAmout);
			 }else{
				unadjustedPastDueDate = null;
			 }	 
		 }catch(NullPointerException np){
			 ////System.err.println("Patient reference dose date is NULL");	 
		 }	
    	
    }
    
    return unadjustedPastDueDate;
  }

  private Date computeAdjustedRecommendedDate() {
	Date adjustedRecommendedDate = new Date();
	Date earliestDate =  computeEarliestDate();
	Date unadjustedRecommendedDate = computeUnadjustedRecommandedDate();
	if(earliestDate.after(unadjustedRecommendedDate)){
		adjustedRecommendedDate = earliestDate;
	}else{
		adjustedRecommendedDate = unadjustedRecommendedDate;
	}
    return adjustedRecommendedDate;
  }

  private Date computeAdjustedPastDueDate() {
    Date adjustedPastDueDate = null;
	Date earliestDate =  computeEarliestDate();
	Date unadjustedPastDueDate = computeUnadjustedPastDueDate();
	if(earliestDate.after(unadjustedPastDueDate)){
		adjustedPastDueDate = earliestDate;
	}else{
		adjustedPastDueDate = unadjustedPastDueDate;
	}
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
	  forecast.setAdjustedPastDueDate(computeAdjustedPastDueDate());
	  Date d = computeAdjustedRecommendedDate();
	  forecast.setAdjustedRecommendedDate(d);
	  forecast.setEarliestDate(computeEarliestDate());
	
    return forecast;

  }

  private void TablePost(PrintWriter out) throws ParseException {
    insertTableRow(out, "BusinessRuleID", "Term", "BusinessRule");
    insertTableRow(out, "FORECASTDT-1", "Earliest Date", computeEarliestDate().toString() );
    insertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date",computeUnadjustedRecommandedDate().toString());
    insertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date", computeUnadjustedPastDueDate().toString());
    insertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date", computeAdjustedRecommendedDate().toString());
    insertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date", computeAdjustedPastDueDate().toString());
    insertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", "recommendedVaccine");
  }


}
