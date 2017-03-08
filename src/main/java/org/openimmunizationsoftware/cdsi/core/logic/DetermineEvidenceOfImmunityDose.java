package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.BirthDateImmunity;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class DetermineEvidenceOfImmunityDose extends LogicStep {

  private ConditionAttribute<Date> caDateofBirth = null;
  private ConditionAttribute<String> caCountryofBirth = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<String> caImmunityGuideline = null;
  private ConditionAttribute<Date> caImmunityDate = null;
  private ConditionAttribute<String> caExclusionCondition = null;
  private ConditionAttribute<String> caCountryofBirthWorking = null;

  public DetermineEvidenceOfImmunityDose(DataModel dataModel) {
    super(LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY, dataModel);
    setConditionTableName("Table 5-2 Immunity attributes");
    caDateofBirth = new ConditionAttribute<Date>("Patient Data", "Date of Birth");
    caDateofBirth.setInitialValue(dataModel.getPatient().getDateOfBirth());
    caCountryofBirth = new ConditionAttribute<String>("Patient Data", "Country of Birth");
    caCountryofBirth.setInitialValue(dataModel.getPatient().getCountryOfBirth());
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)", "Maximum Age Date");
  
    caImmunityGuideline = new ConditionAttribute<String>("Supporting Data (Clinical History Immunity",
        "Immunity Guideline");
    caImmunityDate = new ConditionAttribute<Date>("Supporting Data (Birth Date Immunity", "Immunity Date");
    caExclusionCondition = new ConditionAttribute<String>("Supporting Data (Birth Date Immunity)",
        "Exclusion Condition");
    caCountryofBirthWorking = new ConditionAttribute<String>("Supporting Data (Birth Date Immunity)",
        "Country of Birth");

    caMaximumAgeDate.setAssumedValue(FUTURE);

    conditionAttributesList.add(caDateofBirth);
    caDateofBirth.setInitialValue(dataModel.getPatient().getDateOfBirth());
    conditionAttributesList.add(caCountryofBirth);
    caCountryofBirth.setInitialValue(dataModel.getPatient().getCountryOfBirth());
    conditionAttributesList.add(caMaximumAgeDate);
	caMaximumAgeDate.setAssumedValue(FUTURE);
    conditionAttributesList.add(caImmunityGuideline);
    conditionAttributesList.add(caImmunityDate);
    conditionAttributesList.add(caExclusionCondition);
    conditionAttributesList.add(caCountryofBirthWorking);

    LT logicTable = new LT();
    logicTableList.add(logicTable);


  }

  @Override
  public LogicStep process() throws Exception { 
    setNextLogicStepType(LogicStepType.DETERMINE_FORECAST_NEED);
    evaluateLogicTables();
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
    out.println("<h1> " + logicStepType.getDisplay() + "</h1>");
    out.println("<p>Determine evidence of immunity  assesses the patient’s profile to determine if the patient is already potentially immune to the target disease, negating the need for additional doses.</p>");
    out.println("<p>A patient may be considered immune due to their clinical history or if they were born before a defined date for the given target disease.</p>");
    out.println("<img src=\"Figure 5.2.png\"/>");
    out.println("<p>FIGURE 5 - 2 EVIDENCE OF IMMUNITY PROCESS MODEL</p>");
    
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(4, 5, "Table 5-3 Does the patient have evidence of immunity ?");   
          
       
      
      setLogicCondition(0, new LogicCondition("Does the patient clinical history contain one of the supporting data defined immunity guidelines ?") {
        @Override
        protected LogicResult evaluateInternal() {
        	if(dataModel.getPatient().getMedicalHistory()!=null){
        		/**
        		 * To Complete
        		 */
        		return YES;
        	}else{
        		return NO;
        	}

        }
      });

      setLogicCondition(1,
          new LogicCondition("Is the patient's date of the birth < the supporting data defined immunity date ?") {
            @Override
            protected LogicResult evaluateInternal() {
            	Date dob = caDateofBirth.getFinalValue();
            	if(dob!=null){
            		if(dataModel.getImmunityList().size()>0){
            			if(dataModel.getImmunityList().get(0).getBirthDateImmunityList().size()>0){
            				List<BirthDateImmunity> birthDateImmunityList = dataModel.getImmunityList().get(0).getBirthDateImmunityList();
            				for(BirthDateImmunity bdi:birthDateImmunityList){
            					if(dob.before(bdi.getImmunityBirthDate())){
            						return YES;
            					}
            				}
            				return NO;
            			}else{
            				return NO;
            			}
            		}else{
            			return NO;
            		}
            	}else{
            		return NO;
            	}
            	
            }
          });

      setLogicCondition(2, new LogicCondition("Does the patient the an exclusion condition to  the immunity ?") {
        @Override
        protected LogicResult evaluateInternal() {
        	if(dataModel.getImmunityList().size()>0){
        		if(dataModel.getImmunityList().get(0).getBirthDateImmunityList().size()>0){
        			List<BirthDateImmunity> birthDateImmunityList = dataModel.getImmunityList().get(0).getBirthDateImmunityList();
        			for(BirthDateImmunity bdi:birthDateImmunityList){
        				if(bdi.getExclusionList().size()>0){
        					/***
        					 * Checking if the patient has any immunity
        					 */
        					return YES;
        				}
        			}
        			return NO;
        		}else{
        			return NO;
        		}
        	}else{
        		return NO;
        	}
        }
      });
     
      setLogicCondition(3, new LogicCondition("Is the patient's country of birth the same as the supporting data defined country of birth ?") {
       @Override
        protected LogicResult evaluateInternal() {   
    	   String patientCountry = caCountryofBirth.getFinalValue().toString();
    	   if(patientCountry!=null){
    		  if(dataModel.getImmunityList().size()>0){    			 
    			if(dataModel.getImmunityList().get(0).getBirthDateImmunityList().size()>0){
    				List<BirthDateImmunity> birthDateImmunityList = dataModel.getImmunityList().get(0).getBirthDateImmunityList();
    				for(BirthDateImmunity bdi:birthDateImmunityList){
    					if(bdi.getCountryOfBirth().equals(patientCountry)){
    						return YES;
    					}
    				}
    				return NO;
    			}else{
    				return NO;
    			}
    		  }else{
    			return NO;
    		  }
    	   }else{
    		   return NO;
    	   }

        }
      });
      
      setLogicResults(0, YES, NO,NO,NO,NO);
      setLogicResults(1, ANY, YES, YES, YES, NO);
      setLogicResults(2, ANY, YES, NO, NO, ANY);
      setLogicResults(3, ANY, ANY, YES, NO, ANY);


      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
        	System.out.println("DetermineEvidenceOfImmunityDose-0");
        	log("Yes. The patient has evidence of immunity.");
          	dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.IMMUNE); 
      		log("Forecast reason is \"patient has evidence of immunity\". ");
      		
      		dataModel.getVaccineGroupForecast().getForecastList().get(dataModel.getVaccineGroupForecast().getForecastList().size()-1).setForecastReason("Patient has Evidence of immunity");
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
        	System.out.println("DetermineEvidenceOfImmunityDose-1");
        	log("No. The patient does not have evidence of immunity.");
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
        	System.out.println("DetermineEvidenceOfImmunityDose-2");
        	log("Yes. The patient has evidence of immunity.");
        	dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.IMMUNE);
           	Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          	List<Forecast> forecastList = dataModel.getVaccineGroupForecast().getForecastList();
          	for(Forecast forecast:forecastList){
          		if(forecast.getAntigen().equals(tmpAntigen)){
          			forecast.setForecastReason("Patient has evidence of immunity");
          		}
          	}
    		log("Forecast reason is \"patient has evidence of immunity\". ");
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
        	System.out.println("DetermineEvidenceOfImmunityDose-3");
        	log("No. The patient does not have evidence of immunity.");
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
        	System.out.println("DetermineEvidenceOfImmunityDose-4");
        	log("No. The patient does not have evidence of immunity.");
        	/*if(dataModel.getVaccineGroupForecast().getForecastList().size()>2){
        		dataModel.getVaccineGroupForecast().getForecastList().get(dataModel.getVaccineGroupForecast().getForecastList().size()-1).setForecastReason("###############");
        	}*/

        }
      });
    }
  }

}
