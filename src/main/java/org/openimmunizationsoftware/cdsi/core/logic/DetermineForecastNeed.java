package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.SeasonalRecommendation;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;


public class DetermineForecastNeed extends LogicStep {

  private ConditionAttribute<String> caVaccineDoseAdministered = null;
  private ConditionAttribute<String> caAdvereseEvents = null;
  private ConditionAttribute<String> caRelevantMedicalObservation = null;
  private ConditionAttribute<String> caTargetDose = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<Date> caEndDate = null;
  private ConditionAttribute<Date> caAssessmentDate = null;
  private ConditionAttribute<String> caContraindication = null;
  private ConditionAttribute<String> caImmunity = null;
  
  private void findEndDate(){
	  	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	  if(referenceSeriesDose.getSeasonalRecommendationList().size()>0){
	    	//System.out.println("#####"+referenceSeriesDose.getSeasonalRecommendationList().get(0).getSeasonalRecommendationEndDate());
	    	Date seasonalRecommendationEndDate = referenceSeriesDose.getSeasonalRecommendationList().get(0).getSeasonalRecommendationEndDate();
	    	caEndDate.setInitialValue(seasonalRecommendationEndDate);
	    }else{
	    	//System.err.println("Recommendation End date is not referenced");
	    }
  }
  
  
  private void findMaximumAgeDate(){
	  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	  Date dob = dataModel.getPatient().getDateOfBirth();
	  int maximumAgeAmount = referenceSeriesDose.getAgeList().get(0).getMaximumAge().getAmount();
	  ////System.err.println(referenceSeriesDose.getAgeList().size());
		Date patientMaximumAgeDate = new Date();
		switch (referenceSeriesDose.getAgeList().get(0).getMinimumAge().getType()) {
		case DAY:
  		patientMaximumAgeDate = DateUtils.addDays(dob, maximumAgeAmount);    		
			break;
		case WEEK:
  		patientMaximumAgeDate = DateUtils.addWeeks(dob, maximumAgeAmount);    		
			break;
		case MONTH:
  		patientMaximumAgeDate = DateUtils.addMonths(dob, maximumAgeAmount);    		
			break;
		case YEAR:
  		patientMaximumAgeDate = DateUtils.addYears(dob, maximumAgeAmount);    		
			break;
		default:
			break;
		}
		
		caMaximumAgeDate.setInitialValue(patientMaximumAgeDate);
		////System.out.println("#######"+ caMaximumAgeDate.getFinalValue().toString());
  }
  

  public DetermineForecastNeed(DataModel dataModel) {
    super(LogicStepType.DETERMINE_FORECAST_NEED, dataModel);
    setConditionTableName("Table 5-4 : Determine forecast need attributes");

    caVaccineDoseAdministered = new ConditionAttribute<String>("Immunization history", "Vaccine Dose(s) Administered");
    caAdvereseEvents = new ConditionAttribute<String>("Immunization history", "Adverse Events");
    caRelevantMedicalObservation = new ConditionAttribute<String>("Medical History", "Relevant Medical Observation");
    caTargetDose = new ConditionAttribute<String>("Patient series", "Target Dose(s)");
    caTargetDose.setInitialValue(dataModel.getTargetDose().getTrackedSeriesDose().toString());
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)", "Maximum Age Date");
    findMaximumAgeDate();
    caEndDate = new ConditionAttribute<Date>("Supporting data (Seasonal Recommendation", "End Date");
    findEndDate();
    caAssessmentDate = new ConditionAttribute<Date>("Data Entry", "Assessment Date");
    caContraindication = new ConditionAttribute<String>("Supporting Data", "Contraindication");
    caImmunity = new ConditionAttribute<String>("Supporting Data", "Immunity");
    

    caMaximumAgeDate.setAssumedValue(FUTURE);
    caEndDate.setAssumedValue(FUTURE);
    //SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    Date now = new Date();
    caAssessmentDate.setAssumedValue(now);

    conditionAttributesList.add(caVaccineDoseAdministered);
    conditionAttributesList.add(caAdvereseEvents);
    conditionAttributesList.add(caRelevantMedicalObservation);
    conditionAttributesList.add(caTargetDose);
    conditionAttributesList.add(caMaximumAgeDate);
    conditionAttributesList.add(caEndDate);
    conditionAttributesList.add(caAssessmentDate);
    conditionAttributesList.add(caContraindication);
    conditionAttributesList.add(caImmunity);
    
    

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
	  ////System.err.println("DETERMINE_FORECAST_NEED");
	  setNextLogicStepType(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES);
	  evaluateLogicTables();
	  //setNextLogicStepType(LogicStepType.FOR_EACH_PATIENT_SERIES);
	  return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + getTitle() + "</h1>");
    out.println("<p>Determine forecast need determines  if there is a need to forecast dates. This involves reviewing patient data, antigen  administered  records,  and  patient  series.  This  is  a  prerequisite  before  a  CDS  engine  can  produce forecast dates and reasons </p>");
    out.println("<p>The following process model, attribute table, and decision table are used to determine the need to generate forecast dates.</p>");
    out.println("<img src=\"Figure 5.3.png\"/>");
    out.println("<p>FIGURE 5 - 3 DETERMINE FORECAST NEEDPROCESS MODEL</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(5, 6, "Table 5-5 Should the patient receive another target dose ?");
      setLogicCondition(0, new LogicCondition("Does the patient have at least one target dose with a target dose status of \"not satisfied\"?") {
        @Override
        protected LogicResult evaluateInternal() {
        	caTargetDose.setInitialValue(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease().toString());
          if (caTargetDose.equals(TargetDoseStatus.NOT_SATISFIED)) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(1, new LogicCondition("Does the patient have at least one target dose with a target dose status of \"satisfied\"?") {
        @Override
        protected LogicResult evaluateInternal() {
        	caTargetDose.setInitialValue(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease().toString());
          if (caTargetDose.equals(TargetDoseStatus.SATISFIED)) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(2, new LogicCondition("Is the patient without a contradiction for this patient series ?") {
        @Override
        protected LogicResult evaluateInternal() {
      	
         if (dataModel.getContraindicationList().isEmpty()) {
            return LogicResult.YES;
          }
          return LogicResult.NO;

        }
      });

      setLogicCondition(3, new LogicCondition("Is the assement date < the maximum age date ?") {
        @Override
        protected LogicResult evaluateInternal() {
        	//List<SeriesDose> seriesDosesList = dataModel.getPatientSeries().get
        	////Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
        	//List<Age> ageList = dataModel.getTargetDose().getTrackedSeriesDose().getAgeList();
          if (caAssessmentDate.getAssumedValue().before(caMaximumAgeDate.getAssumedValue())) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(4, new LogicCondition("Is the assement date < seasonal recommendation end date ?") {
        @Override
        protected LogicResult evaluateInternal() {
          List<TargetDose> targetDoseList = dataModel.getTargetDoseList();
          int targetDoseListLength = targetDoseList.size();
          for (int i = 0; i < targetDoseListLength; i++) {
            TargetDose targetDose = targetDoseList.get(i);
            List<SeasonalRecommendation> seasonalRecommendationList = targetDose.getTrackedSeriesDose()
                .getSeasonalRecommendationList();
            int seasonalRecommendationLength = seasonalRecommendationList.size();
            for (int j = 0; j < seasonalRecommendationLength; j++) {
              SeasonalRecommendation seasonalRecommendation = seasonalRecommendationList.get(j);
              if (caAssessmentDate.getAssumedValue()
                  .before(seasonalRecommendation.getSeasonalRecommendationEndDate())) {
                return LogicResult.YES;
              }
            }
          }
          return LogicResult.NO;

        }
      });

      setLogicResults(0, YES, NO, NO, ANY, ANY, ANY);
      setLogicResults(1, ANY, YES, NO, ANY, ANY, ANY);
      setLogicResults(2, YES, ANY, ANY, NO, ANY, ANY);
      setLogicResults(3, YES, ANY, ANY, ANY, NO, ANY);
      setLogicResults(4, YES, ANY, ANY, ANY, ANY, NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
        	////System.out.println("DetermineForecastNeed_00");
        	log("Yes. The patient should receive another dose.");
        	dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
        	////System.out.println("DetermineForecastNeed_01");
        	log("No. The patient should not receive another dose .");
        	dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.COMPLETE);
        	/*VaccineGroupForecast vaccineGroupForecast = dataModel.getVaccineGroupForecast();
        	Forecast newForecast = new Forecast();
        	newForecast.setVaccineGroupForecast(vaccineGroupForecast);
        	newForecast.setForecastReason("Patient series is complete");
        	vaccineGroupForecast.getForecastList().add(newForecast);*/
        	Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          	List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
          	for(Forecast forecast:forecastList){
          		if(forecast.getAntigen().equals(tmpAntigen)){
          			forecast.setForecastReason("Patient series is complete");
          		}
          	}
          	
        	

        	log("Forecast reason is \"patient series is complete.\"");
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
        	
        	////System.out.println("DetermineForecastNeed_02");
        	log("No. The patient should not receive another dose .");
        	dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_RECOMMENDED);
        	////System.out.println(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
        	//VaccineGroupForecast vaccineGroupForecast = dataModel.getVaccineGroupForecast();
   /*       	//System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX     "+vaccineGroupForecast.getForecastList().size());
          	if(vaccineGroupForecast.getForecastList().size()>1){
          		//System.out.println("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP     "+vaccineGroupForecast.getForecastList().get(vaccineGroupForecast.getForecastList().size()-1).getAntigen());

          	}
          	//System.out.println("YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY     "+vaccineGroupForecast.getAntigensNeededList().size());
          	//System.out.println("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ     "+dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
          	//System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW     "+dataModel.getPatientSeries().getTargetDoseList().size());*/
        /*	List<Antigen> antigenFromForecast = new ArrayList<Antigen>();
        	List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
        	boolean isNew = false;
        	for(Forecast forecast:forecastList){
        		Antigen tmpAntigen = forecast.getAntigen();
        		if(!antigenFromForecast.contains(tmpAntigen)){
        			isNew = true;
        		}
        	}
        	if(isNew){
        		Forecast newForecast = new Forecast();
            	newForecast.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
            	newForecast.setTargetDose(dataModel.getTargetDose());
            	newForecast.setVaccineGroupForecast(vaccineGroupForecast);
            	newForecast.setForecastReason("Not recommended at this time due to past immunization history");
            	vaccineGroupForecast.getForecastList().add(newForecast);
        	}*/
        	Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          	List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
          	for(Forecast forecast:forecastList){
          		////System.err.println("888888888888888888"+tmpAntigen.getName());
          		////System.err.println("999999999999999999"+forecast.getAntigen());
          		if(forecast.getAntigen().equals(tmpAntigen)){
          			forecast.setForecastReason("Not recommended at this time due to past immuniszation history");
          		}
          	}

        	log("Forecast reason is \"not recommended at this time due to past immunization history.\"");
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
        	////System.out.println("DetermineForecastNeed_03");
        	log("No. The patient should not receive another dose .");
        	dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.CONTRAINDICATED);
/*        	VaccineGroupForecast vaccineGroupForecast = dataModel.getVaccineGroupForecast();
        	List<Antigen> antigenFromForecast = new ArrayList<Antigen>();
        	List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
        	boolean isNew = false;
        	for(Forecast forecast:forecastList){
        		Antigen tmpAntigen = forecast.getAntigen();
        		if(!antigenFromForecast.contains(tmpAntigen)){
        			isNew = true;
        		}
        	}
        	if(isNew){
        		Forecast newForecast = new Forecast();
            	newForecast.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
            	newForecast.setTargetDose(dataModel.getTargetDose());
            	newForecast.setVaccineGroupForecast(vaccineGroupForecast);
            	newForecast.setForecastReason("Patient has a contraindication");
            	vaccineGroupForecast.getForecastList().add(newForecast);
            	
        	}*/
        	Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          	List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
          	for(Forecast forecast:forecastList){
          		if(forecast.getAntigen().equals(tmpAntigen)){
          			forecast.setForecastReason("Patient has contraindiction");
          		}
          	}
        	log("Forecast reason is \"patient has a contraindication.\"");
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
        	////System.out.println("DetermineForecastNeed_04");
        	log("No. The patient should not receive another dose .");
        	dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.AGED_OUT);
        	/*VaccineGroupForecast vaccineGroupForecast = dataModel.getVaccineGroupForecast();
        	List<Antigen> antigenFromForecast = new ArrayList<Antigen>();
        	List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
        	boolean isNew = false;
        	for(Forecast forecast:forecastList){
        		Antigen tmpAntigen = forecast.getAntigen();
        		if(!antigenFromForecast.contains(tmpAntigen)){
        			isNew = true;
        		}
        	}
        	if(isNew){
        		Forecast newForecast = new Forecast();
            	newForecast.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
            	newForecast.setTargetDose(dataModel.getTargetDose());
            	newForecast.setVaccineGroupForecast(vaccineGroupForecast);
            	newForecast.setForecastReason("Patient has exceeded the minimum age");
            	vaccineGroupForecast.getForecastList().add(newForecast);
        	}*/
        	Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          	List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
          	for(Forecast forecast:forecastList){
          		if(forecast.getAntigen().equals(tmpAntigen)){
          			forecast.setForecastReason("Patient has exceeded the maximum age");
          		}
          	}
        	log("Forecast reason is \"patient has exceeded the maximum age.\"");
        }
      });

      setLogicOutcome(5, new LogicOutcome() {
        @Override
        public void perform() {
        	////System.out.println("DetermineForecastNeed_05");
        	log("No. The patient should not receive another dose .");
        	dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
        /*	VaccineGroupForecast vaccineGroupForecast = dataModel.getVaccineGroupForecast();
        	List<Antigen> antigenFromForecast = new ArrayList<Antigen>();
        	List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
        	boolean isNew = false;
        	for(Forecast forecast:forecastList){
        		Antigen tmpAntigen = forecast.getAntigen();
        		if(!antigenFromForecast.contains(tmpAntigen)){
        			isNew = true;
        		}
        	}
        	if(isNew){
        		Forecast newForecast = new Forecast();
            	newForecast.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
            	newForecast.setTargetDose(dataModel.getTargetDose());
            	newForecast.setVaccineGroupForecast(vaccineGroupForecast);
            	newForecast.setForecastReason("Past seasonal recommendation end date");
            	vaccineGroupForecast.getForecastList().add(newForecast);
        	}*/
        	Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          	List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
          	for(Forecast forecast:forecastList){
          		if(forecast.getAntigen().equals(tmpAntigen)){
          			forecast.setForecastReason("Forecast reason is past seasonal date");
          		}
          	}
        	log("Forecast reason is \"past seasonal recommendation end date.\"");
        	
        }
      });

    }
  }

}
