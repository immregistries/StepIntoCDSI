package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.BirthDateImmunity;
import org.openimmunizationsoftware.cdsi.core.domain.ClinicalHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Exclusion;
import org.openimmunizationsoftware.cdsi.core.domain.Immunity;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

public class DetermineEvidenceOfImmunityDose extends LogicStep
{

  private ConditionAttribute<Date> caDateofBirth = null;
  private ConditionAttribute<String> caCountryofBirth = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<String> caImmunityGuideline = null;
  private ConditionAttribute<Date> caImmunityDate = null;
  private ConditionAttribute<String> caExclusionCondition = null;
  private ConditionAttribute<String> caCountryofBirthWorking = null;

  public DetermineEvidenceOfImmunityDose(DataModel dataModel) {
    super(LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY, dataModel);
    setConditionTableName("Table ");

    caDateofBirth = new ConditionAttribute<Date>("Patient Data", "Date of Birth");
    caCountryofBirth = new ConditionAttribute<String>("Patient Data", "Country of Birth");
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)", "Maximum Age Date");
    caImmunityGuideline = new ConditionAttribute<String>("Supporting Data (Clinical History Immunity","Immunity Guideline");
    caImmunityDate = new ConditionAttribute<Date>("Supporting Data (Birth Date Immunity", "Immunity Date");
    caExclusionCondition = new ConditionAttribute<String>("Supporting Data (Birth Date Immunity)", "Exclusion Condition");
    caCountryofBirthWorking = new ConditionAttribute<String>("Supporting Data (Birth Date Immunity)", "Country of Birth");

    caMaximumAgeDate.setAssumedValue(FUTURE);

    conditionAttributesList.add(caDateofBirth);
    conditionAttributesList.add(caCountryofBirth);
    conditionAttributesList.add(caMaximumAgeDate);
    conditionAttributesList.add(caImmunityGuideline);
    conditionAttributesList.add(caImmunityDate);
    conditionAttributesList.add(caExclusionCondition);
    conditionAttributesList.add(caCountryofBirthWorking);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
    
    /**
     * Need to instansiate the attributes
     */
    
    
    
    
    
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.DETERMINE_FORECAST_NEED);
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
    
    System.out.println(caImmunityGuideline);
    
    
    
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
   public LT() {
     super(4, 5, "Table 5-3 Does the patient have evidence of immunity ?");
     
     setLogicCondition(0, new LogicCondition("Does the patient clinical history contain one of the supporting data defined immunity guidelines ?") {
    	 @Override
    	 protected LogicResult evaluateInternal() {
			List<Immunity> immunityList = dataModel.getImmunityList();
			int immunityListLength = immunityList.size();
			for(int j=0; j<immunityListLength;j++){
				List<ClinicalHistory> clinicalHistoryList = dataModel.getImmunityList().get(j).getClinicalHistoryList();
				int clinicalHistoryLength = clinicalHistoryList.size();
				for(int i=0; i<clinicalHistoryLength;i++){
					if(clinicalHistoryList.get(i).getImmunityGuideline().equals(caImmunityGuideline)){
						return LogicResult.YES;
					}
				}
			}
			 
			return LogicResult.NO;
		}
	});
     
     setLogicCondition(1, new LogicCondition("Is the patient's date of the birth < the supporting data defined immunity date ?") {
    	 @Override
    	 protected LogicResult evaluateInternal() {
			if (caDateofBirth.getAssumedValue().before(caImmunityDate.getAssumedValue())) {
				return LogicResult.YES;			
			}else{
				return LogicResult.NO;
			}
		}
	});
     
     setLogicCondition(2, new LogicCondition("Does the patient have the an exclusion condition to  the immunity ?") {
    	 @Override
    	 protected LogicResult evaluateInternal() {
			List<Immunity> immunityList = dataModel.getImmunityList();
			int immunityListLength = immunityList.size();
			for(int j=0; j<immunityListLength;j++){
				List<BirthDateImmunity> birthDateImmunityList = dataModel.getImmunityList().get(j).getBirthDateImmunityList();
				int birthDateImmunityListLength = birthDateImmunityList.size();
				for(int i=0; i<birthDateImmunityListLength;i++){
					List<Exclusion> exclusionList = birthDateImmunityList.get(i).getExclusionList();
					int exclusionListLength = exclusionList.size();
					for(int k=0; k<exclusionListLength;k++){
						Exclusion exclusion = exclusionList.get(k);
						if(exclusion.getExclusionCondition().equals(caExclusionCondition)){
							return LogicResult.YES;
						}
					}	
				}
			}
			 
			return LogicResult.NO;
		}
	});
     
    setLogicCondition(3, new LogicCondition("Is the patient's country of birth the same as the supporting data defined country of birth ?") {
		@Override
   	 protected LogicResult evaluateInternal() {
			List<Immunity> immunityList = dataModel.getImmunityList();
			int immunityListLength = immunityList.size();
			for(int j=0; j<immunityListLength;j++){
				List<BirthDateImmunity> birthDateImmunityList = dataModel.getImmunityList().get(j).getBirthDateImmunityList();
				int birthDateImmunityListLength = birthDateImmunityList.size();
				for(int i=0; i<birthDateImmunityListLength;i++){
					String countryOfBirth = birthDateImmunityList.get(i).getCountryOfBirth();
					if(countryOfBirth.equals(caCountryofBirth)){
						return LogicResult.YES;
								
					}
	
				}
			}
			 
			return LogicResult.NO;
		}
	});
    
    setLogicResults(0,YES,ANY,ANY,ANY,ANY);
    setLogicResults(1,NO,YES,YES,ANY);
    setLogicResults(2,NO,YES,NO,YES);
    setLogicResults(3,NO,YES,NO,YES);
    setLogicResults(4,NO,NO,ANY,ANY);
    
    
    setLogicOutcome(0, new LogicOutcome() {
		@Override
		public void perform() {
			log("Yes. The patient has evidence of immunity.");
			dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.IMMUNE);
			log("Forecast reason is \"patient has evidence of immunity\". ");
		}
	});
    
    setLogicOutcome(1, new LogicOutcome() {
		@Override
		public void perform() {
			log("No. The patient does not have evidence of immunity.");
		}
	});
    
    setLogicOutcome(2, new LogicOutcome() {
  		@Override
  		public void perform() {
  			log("Yes. The patient has evidence of immunity.");
  			dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.IMMUNE);
  			log("Forecast reason is \"patient has evidence of immunity\". ");
  		}
  	});
      
      setLogicOutcome(3, new LogicOutcome() {
  		@Override
  		public void perform() {
  			log("No. The patient does not have evidence of immunity.");
  		}
  	});
      
      setLogicOutcome(4, new LogicOutcome() {
  		@Override
  		public void perform() {
  			log("No. The patient does not have evidence of immunity.");
  		}
  	});

    }
  }
  
  

}
