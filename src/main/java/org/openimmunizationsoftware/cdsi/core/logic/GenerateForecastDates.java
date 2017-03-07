package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
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
  
  private void findMinimumAgeDate(){
	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	  Date dob = dataModel.getPatient().getDateOfBirth();
	  int manimumAgeAmount = referenceSeriesDose.getAgeList().get(0).getMinimumAge().getAmount();
	  Date patientMaximumAgeDate = new Date();
		switch (referenceSeriesDose.getAgeList().get(0).getMinimumAge().getType()) {
		case DAY:
  		patientMaximumAgeDate = DateUtils.addDays(dob, manimumAgeAmount);    		
			break;
		case WEEK:
  		patientMaximumAgeDate = DateUtils.addWeeks(dob, manimumAgeAmount);    		
			break;
		case MONTH:
  		patientMaximumAgeDate = DateUtils.addMonths(dob, manimumAgeAmount);    		
			break;
		case YEAR:
  		patientMaximumAgeDate = DateUtils.addYears(dob, manimumAgeAmount);    		
			break;
		default:
			break;
		}
		caMinimumAgeDate.setInitialValue(patientMaximumAgeDate);
		//System.out.println("#######MinimumAgeDate : "+ caMinimumAgeDate.getFinalValue().toString());
  }
  
  private void findMaximumAgeDate(){
	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	  Date dob = dataModel.getPatient().getDateOfBirth();
	  int maximumAgeAmount = referenceSeriesDose.getAgeList().get(0).getMaximumAge().getAmount();
		Date patientMinimumAgeDate = new Date();
		switch (referenceSeriesDose.getAgeList().get(0).getMinimumAge().getType()) {
		case DAY:
  		patientMinimumAgeDate = DateUtils.addDays(dob, maximumAgeAmount);    		
			break;
		case WEEK:
  		patientMinimumAgeDate = DateUtils.addWeeks(dob, maximumAgeAmount);    		
			break;
		case MONTH:
  		patientMinimumAgeDate = DateUtils.addMonths(dob, maximumAgeAmount);    		
			break;
		case YEAR:
  		patientMinimumAgeDate = DateUtils.addYears(dob, maximumAgeAmount);    		
			break;
		default:
			break;
		}
		
		caMaximumAgeDate.setInitialValue(patientMinimumAgeDate);
		//System.out.println("#######MaximumAgeDate : "+ caMaximumAgeDate.getFinalValue().toString());
  }
  
  private void findEarliestRecommendedAgeDate(){
	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	  Date dob = dataModel.getPatient().getDateOfBirth();
	  int earliestRecommendedAgeAmount = referenceSeriesDose.getAgeList().get(0).getEarliestRecommendedAge().getAmount();
	  Date earliestRecommendedAgeDate  = new Date();
		switch (referenceSeriesDose.getAgeList().get(0).getEarliestRecommendedAge().getType()) {
		case DAY:
			earliestRecommendedAgeDate = DateUtils.addDays(dob, earliestRecommendedAgeAmount);    		
			break;
		case WEEK:
			earliestRecommendedAgeDate = DateUtils.addWeeks(dob, earliestRecommendedAgeAmount);    		
			break;
		case MONTH:
			earliestRecommendedAgeDate = DateUtils.addMonths(dob, earliestRecommendedAgeAmount);    		
			break;
		case YEAR:
			earliestRecommendedAgeDate = DateUtils.addYears(dob, earliestRecommendedAgeAmount);    		
			break;
		default:
			break;
		}
	caEarliestRecommendedAgeDate.setInitialValue(earliestRecommendedAgeDate);
	//System.err.println("EarlistRecommendedAgeDate : "+earliestRecommendedAgeDate);
	  
  }
  
  private void findLatestRecommendedAgeDate(){
	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	  Date dob = dataModel.getPatient().getDateOfBirth();
	  int latestRecommendedAgeAmount = referenceSeriesDose.getAgeList().get(0).getLatestRecommendedAge().getAmount();
	  Date latestRecommendedAgeDate  = new Date();
		switch (referenceSeriesDose.getAgeList().get(0).getEarliestRecommendedAge().getType()) {
		case DAY:
			latestRecommendedAgeDate = DateUtils.addDays(dob, latestRecommendedAgeAmount);    		
			break;
		case WEEK:
			latestRecommendedAgeDate = DateUtils.addWeeks(dob, latestRecommendedAgeAmount);    		
			break;
		case MONTH:
			latestRecommendedAgeDate = DateUtils.addMonths(dob, latestRecommendedAgeAmount);    		
			break;
		case YEAR:
			latestRecommendedAgeDate = DateUtils.addYears(dob, latestRecommendedAgeAmount);    		
			break;
		default:
			break;
		}
	caLatestRecommendedAgeDate.setInitialValue(latestRecommendedAgeDate);
	//System.err.println("LatestRecommendedAgeDate : "+latestRecommendedAgeDate);
	  
  }
  
  private void findEarliestRecommendedIntervalDate(){
	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	  TimePeriod earliestRecommendedInterval;
	  try{
		 earliestRecommendedInterval =  referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList().get(0).getEarliestRecommendedInterval();
		 int earliestRecommendedIntervalAmout = earliestRecommendedInterval.getAmount();
		 Date earliestRecommendedIntervalDate = new Date();
		 Date patientReferenceDoseDate = new Date();
		 try{
			 /***
			  * TO DO
			  */
			 patientReferenceDoseDate = dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered();
			 //System.out.println("patientReferenceDoseDate : "+patientReferenceDoseDate);
		 }catch(NullPointerException np){
			 System.err.println("Patient reference dose date is NULL");
			 //System.err.println(np.getStackTrace());
		 }
		 switch (earliestRecommendedInterval.getType()) {
			case DAY:
				earliestRecommendedIntervalDate = DateUtils.addDays(patientReferenceDoseDate, earliestRecommendedIntervalAmout);    		
				break;
			case WEEK:
				earliestRecommendedIntervalDate = DateUtils.addWeeks(patientReferenceDoseDate, earliestRecommendedIntervalAmout);    		
				break;
			case MONTH:
				earliestRecommendedIntervalDate = DateUtils.addMonths(patientReferenceDoseDate, earliestRecommendedIntervalAmout);    		
				break;
			case YEAR:
				earliestRecommendedIntervalDate = DateUtils.addYears(patientReferenceDoseDate, earliestRecommendedIntervalAmout);    		
				break;
			default:
				break;
			}
		 caEarliestRecommendedIntervalDate.setInitialValue(earliestRecommendedIntervalDate);
	  }catch(NullPointerException np){
		  System.err.println("earliestRecommendedInterval is null");
	  }	  
  }
  
  
  private void findLatestRecommendedIntervalDate(){
	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	  TimePeriod latestRecommendedInterval;
	  try{
		 latestRecommendedInterval =  referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList().get(0).getLatestRecommendedInterval();
		 int latestRecommendedIntervalAmout = latestRecommendedInterval.getAmount();
		 Date latestRecommendedIntervalDate = new Date();
		 Date patientReferenceDoseDate = new Date();
		 try{
			 /***
			  * TO DO
			  */
			 patientReferenceDoseDate = dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered();
			 //System.out.println("patientReferenceDoseDate : "+patientReferenceDoseDate);
		 }catch(NullPointerException np){
			 System.err.println("Patient reference dose date is NULL");
		 }
		 switch (latestRecommendedInterval.getType()) {
			case DAY:
				latestRecommendedIntervalDate = DateUtils.addDays(patientReferenceDoseDate, latestRecommendedIntervalAmout);    		
				break;
			case WEEK:
				latestRecommendedIntervalDate = DateUtils.addWeeks(patientReferenceDoseDate, latestRecommendedIntervalAmout);    		
				break;
			case MONTH:
				latestRecommendedIntervalDate = DateUtils.addMonths(patientReferenceDoseDate, latestRecommendedIntervalAmout);    		
				break;
			case YEAR:
				latestRecommendedIntervalDate = DateUtils.addYears(patientReferenceDoseDate, latestRecommendedIntervalAmout);    		
				break;
			default:
				break;
			}
		 caLatestRecommendedIntervalDate.setInitialValue(latestRecommendedIntervalDate);
		// System.err.println("LatestRecommendedIntervalDate :::::::::::::::: "+latestRecommendedIntervalDate);
	  }catch(NullPointerException np){
		  System.err.println("latestRecommendedInterval is null");
	  }	  
  }
  
  private void findMinimalIntervalDate(){
	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	  TimePeriod minimalInterval;
	  try{
		 minimalInterval =  referenceSeriesDose.getAgeList().get(0).getSeriesDose().getIntervalList().get(0).getMinimumInterval();
		 int minimalIntervalAmout = minimalInterval.getAmount();
		 Date minimalIntervalDate = new Date();
		 Date patientReferenceDoseDate = new Date();
		 try{
			 /***
			  * TO DO
			  */
			 patientReferenceDoseDate = dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered();
			// System.out.println("patientReferenceDoseDate : "+patientReferenceDoseDate);
		 }catch(NullPointerException np){
			 System.err.println("Patient reference dose date is NULL");
			 //System.err.println(np.getStackTrace());
		 }
		 switch (minimalInterval.getType()) {
			case DAY:
				minimalIntervalDate = DateUtils.addDays(patientReferenceDoseDate, minimalIntervalAmout);    		
				break;
			case WEEK:
				minimalIntervalDate = DateUtils.addWeeks(patientReferenceDoseDate, minimalIntervalAmout);    		
				break;
			case MONTH:
				minimalIntervalDate = DateUtils.addMonths(patientReferenceDoseDate, minimalIntervalAmout);    		
				break;
			case YEAR:
				minimalIntervalDate = DateUtils.addYears(patientReferenceDoseDate, minimalIntervalAmout);    		
				break;
			default:
				break;
			}
		 caMinimumIntervalDate.setInitialValue(minimalIntervalDate);
		// System.err.println("MinimumIntervalDate :::::::::::::::: "+minimalIntervalDate);
	  }catch(NullPointerException np){
		  System.err.println("MinimumIntervalDate is null");
	  }	  
  }
  private void findLatestConflictEndIntervalDate(){
	  try{
		  //System.err.println("####################"+dataModel.getLiveVirusConflictList().size());
	  }catch (NullPointerException np) {
		System.err.println(np.getStackTrace());
	}
  }
  
  private void findSeasonalRecommendationStartDate(){
	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	  Date seasonalRecommendationstartDate = new DateTime(1900, 1, 1, 0, 0).toDate();
	  caSeasonalRecommendationStartDate.setAssumedValue(seasonalRecommendationstartDate);
	    if(referenceSeriesDose.getSeasonalRecommendationList().size()>0){
	       //	System.out.println("#####"+referenceSeriesDose.getSeasonalRecommendationList().get(0).getSeasonalRecommendationStartDate());
	    	seasonalRecommendationstartDate = referenceSeriesDose.getSeasonalRecommendationList().get(0).getSeasonalRecommendationStartDate();
	    	caSeasonalRecommendationStartDate.setInitialValue(seasonalRecommendationstartDate);
	    }else{
	    	System.err.println("Couldn't find seasonalRecommandation start date");
	    }
	    
  }
  
  private void findEarliestDate(){
	  try{
		  //System.out.println("MinimumAgeDate: "+ caMinimumAgeDate.getFinalValue());
	  }catch(NullPointerException np){
		  System.err.println("Coudn't find MinimumAgeDate");
	  }
	  try{
		  //System.out.println("LatestMinimumIntervalDate: "+ caMinimumIntervalDate.getFinalValue());
	  }catch(NullPointerException np){
		  System.err.println("Coudn't find LatestMinimumIntervalDate");
	  }
	  try{
		  //System.out.println("LatestConflictEndIntervalDate: "+ caLatestConflictEndIntervalDate.getFinalValue());
	  }catch(NullPointerException np){
		  System.err.println("Coudn't find minimum age");
	  }
	  try{
		  //System.out.println("SeasonalRecommendationStartdate: "+ caSeasonalRecommendationStartDate.getFinalValue());
	  }catch(NullPointerException np){
		  System.err.println("Coudn't find minimum age");
	  }
	  
  }
	 
  
  
  
  int size = dataModel.getVaccineGroupForecast().getForecastList().size();

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
    findEarliestDate();
    
    
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
	  //generateForcastDates(forecast);
	  //forecast.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
	  //forecast.setAntigen(dataModel.getTargetDose().getTrackedSeriesDose().getAntigenSeries().getTargetDisease());
	  forecast.setAntigen(dataModel.getAntigen());

	  forecast.setTargetDose(dataModel.getTargetDose());
	  Antigen newAntigenForeCast = forecast.getAntigen();
	  List<Antigen> antigenFromForcastList = new ArrayList<Antigen>();
	  List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
    for(Forecast foreCast:forecastList){
    	antigenFromForcastList.add(foreCast.getAntigen());
    }
    if(!antigenFromForcastList.contains(newAntigenForeCast)){
    	//SeriesDose targetSeriesDose =
    	//dataModel.getPatientSeries().getTrackedAntigenSeries().get
    	generateForcastDates(forecast);
    	 dataModel.getVaccineGroupForecast().getForecastList().add(forecast);
    	 //Date now = new Date();
    	 //forecast.setAssessmentDate(now);
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
			 System.out.println("patientReferenceDoseDate : "+patientReferenceDoseDate);
			 unadjustedRecommandedDate = DateUtils.addDays(patientReferenceDoseDate, biggeestAmout); 
			 if(biggeestAmout!=0 || patientReferenceDoseDate!=null){
				 unadjustedRecommandedDate = DateUtils.addDays(patientReferenceDoseDate, biggeestAmout);
			 }else{
				 if(dataModel.getVaccineGroupForecast().getForecastList().size()>0){
					 Forecast forecast = dataModel.getVaccineGroupForecast().getForecastList().get(dataModel.getVaccineGroupForecast().getForecastList().size()-1);
					 unadjustedRecommandedDate = forecast.getEarliestDate();
				 } 
			 }	 
		 }catch(NullPointerException np){
			 System.err.println("Patient reference dose date is NULL");	 
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
			 System.out.println("patientReferenceDoseDate : "+patientReferenceDoseDate);
			 unadjustedPastDueDate = DateUtils.addDays(patientReferenceDoseDate, biggeestAmout); 
			 if(biggeestAmout!=0 || patientReferenceDoseDate!=null){
				 unadjustedPastDueDate = DateUtils.addDays(patientReferenceDoseDate, biggeestAmout);
			 }else{
				unadjustedPastDueDate = null;
			 }	 
		 }catch(NullPointerException np){
			 System.err.println("Patient reference dose date is NULL");	 
		 }	
    	
    }
    
    return unadjustedPastDueDate;
  }

  private Date computeAdjustedRecommendedDate() {
	Date adjustedRecommendedDate = new Date();
	Date earliestDate =  computeEarliestDate();
	Date unadjustedRecommendedDate = computeUnadjustedRecommandedDate();
	//System.out.println("earliestDate :"+earliestDate);
	//System.out.println("unadjustedRecommendedDate : "+unadjustedRecommendedDate);
	if(earliestDate.after(unadjustedRecommendedDate)){
		adjustedRecommendedDate = earliestDate;
	}else{
		adjustedRecommendedDate = unadjustedRecommendedDate;
	}
	//System.out.println("adjustedRecommendedDate : "+adjustedRecommendedDate);
    return adjustedRecommendedDate;
  }

  private Date computeAdjustedPastDueDate() {
    Date adjustedPastDueDate = null;
	Date earliestDate =  computeEarliestDate();
	Date unadjustedPastDueDate = computeUnadjustedPastDueDate();
	//System.out.println("earliestDate :"+earliestDate);
	//System.out.println("unadjustedPastDueDate : "+unadjustedPastDueDate);
	if(earliestDate.after(unadjustedPastDueDate)){
		adjustedPastDueDate = earliestDate;
	}else{
		adjustedPastDueDate = unadjustedPastDueDate;
	}
	//System.out.println("adjustedRecommendedDate : "+adjustedPastDueDate);
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
	  forecast.setAdjustedRecommendedDate(computeAdjustedRecommendedDate());
	  forecast.setEarliestDate(computeEarliestDate());
	
    return forecast;

  }

  private void TablePost(PrintWriter out) throws ParseException {
    //Forecast forecast = new Forecast();
    insertTableRow(out, "BusinessRuleID", "Term", "BusinessRule");
    insertTableRow(out, "FORECASTDT-1", "Earliest Date", computeEarliestDate().toString() );
    insertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date",computeUnadjustedRecommandedDate().toString());
    insertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date", computeUnadjustedPastDueDate().toString());
    insertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date", computeAdjustedRecommendedDate().toString());
    insertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date", computeAdjustedPastDueDate().toString());
    insertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", "recommendedVaccine");
  }


}
