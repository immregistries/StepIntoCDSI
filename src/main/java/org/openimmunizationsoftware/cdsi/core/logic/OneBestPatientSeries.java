package org.openimmunizationsoftware.cdsi.core.logic;


import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class OneBestPatientSeries extends LogicStep
{

  // private ConditionAttribute<Date> caDateAdministered = null;

  public OneBestPatientSeries(DataModel dataModel)
  {
    super(LogicStepType.ONE_BEST_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");
    
    // caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    
   // caTriggerAgeDate.setAssumedValue(FUTURE);
    
//    conditionAttributesList.add(caDateAdministered);
    
    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
    setNextLogicStepType(LogicStepType.CLASSIFY_PATIENT_SERIES);
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
    out.println("<p>One best patient series  examines all of the  patient series  for a given antigen to determine if one of the  patient series is superior to all other patient series and can be considered the best patient series.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
    	super(4, 5, "Table 6-3 : Is there one best patient series ?");
    	
    	setLogicCondition(0, new LogicCondition("Antigen contains only one patient series ?") {
			@Override
			protected LogicResult evaluateInternal() {
				int numberOfPatientSeries = 0;
				List<AntigenSeries> asl = dataModel.getAntigenSeriesSelectedList();
				for(AntigenSeries as: asl){
					boolean isDefaultSeries = as.getSelectBestPatientSeries().getDefaultSeries().equals(YES);
					if(isDefaultSeries){
						numberOfPatientSeries++;
					}
				}
				if(numberOfPatientSeries==1){
					return LogicResult.YES;
				}else{
					return LogicResult.NO;
				}
	
			}
		});
    	
    	
    	setLogicCondition(1, new LogicCondition("Patient has only 1 complete patient series?") {
			@Override
			protected LogicResult evaluateInternal() {
				int completePatientSeries = 0;
				List<PatientSeries> psl = dataModel.getPatientSeriesList();
				for(PatientSeries ps:psl ){
					if(ps.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)){
						completePatientSeries++;
					}
				}
				if(completePatientSeries==1){
					return LogicResult.YES;
				}else{
					return LogicResult.NO;
				}
			}
		});
    	
    	setLogicCondition(2, new LogicCondition("Patient has only 1 in-process patient series and no complete patient series ?") {
			
			@Override
			protected LogicResult evaluateInternal() {
				
				int notCompletePatientSeries = 0;
				List<PatientSeries> psl = dataModel.getPatientSeriesList();
				for(PatientSeries ps:psl ){
					if(ps.getPatientSeriesStatus().equals(PatientSeriesStatus.NOT_COMPLETE)){
						notCompletePatientSeries++;
					}
				}
				/**
				 * An in-process patient series must be a patient series with at least one
				 * target dose status satisfied and the patient series status not complete.
				 */
				List<String> antigenSerieNameWithASatisfiedTargetDose = new ArrayList<String>();
				
				List<TargetDose> targetDoseList = dataModel.getTargetDoseList();
				for(TargetDose targetDose:targetDoseList){
					if(targetDose.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)){
							String antigenSeriesName1 = targetDose.getTrackedSeriesDose().getAntigenSeries().getSeriesName();
							antigenSerieNameWithASatisfiedTargetDose.add(antigenSeriesName1);
					}
				}
				
				List<String> antigenSerieNameWithANotCompletePatientSerieStatus = new ArrayList<String>();
				
				List<PatientSeries> ps2 = dataModel.getPatientSeriesList();
				for(PatientSeries ps:ps2 ){
					if(ps.getPatientSeriesStatus().equals(PatientSeriesStatus.NOT_COMPLETE)){
						String antigenSeriesName2  =  ps.getTrackedAntigenSeries().getSeriesName();
						antigenSerieNameWithANotCompletePatientSerieStatus.add(antigenSeriesName2);
					}
				}
				
				antigenSerieNameWithANotCompletePatientSerieStatus.retainAll(new HashSet<String> (antigenSerieNameWithASatisfiedTargetDose));
				
				int inProcessPatientSeriesNumber  = antigenSerieNameWithANotCompletePatientSerieStatus.size();
				
				
				if(inProcessPatientSeriesNumber==1 && notCompletePatientSeries==0){
					return LogicResult.YES;
				}else{
					return LogicResult.NO;
				}
			}
		});
    	
    	
    	setLogicCondition(3, new LogicCondition("Patient has all Patient Series with 0 valid doses and 1 patient series is identified as the default patient series ?") {		
			@Override
			protected LogicResult evaluateInternal() {
				int numberOfDefaultPatientSeries = 0;
				List<AntigenSeries> asl = dataModel.getAntigenSeriesSelectedList();
				for(AntigenSeries as: asl){
					boolean isDefaultSeries = as.getSelectBestPatientSeries().getDefaultSeries().equals(YES);
					if(isDefaultSeries){
						numberOfDefaultPatientSeries ++;
					}
				}
				System.err.println("VALID DOSE ??????????????????????????????");
				if(numberOfDefaultPatientSeries==1){
					return LogicResult.YES;
				}else{
					return LogicResult.NO;
				}
			}
		});
    	
    	
    	setLogicResults(0, YES,NO,NO,NO,NO);
    	setLogicResults(1,ANY,YES,NO,NO,NO);
    	setLogicResults(2, ANY,ANY,YES,NO,NO);
    	setLogicResults(3, ANY,ANY,ANY,YES,NO);
    	
    	setLogicOutcome(0, new LogicOutcome() {
			
			@Override
			public void perform() {
				// TODO Auto-generated method stub
				log("Yes. The lone patient series is the best patient series.");				
			}
		});
    	
    	setLogicOutcome(1, new LogicOutcome() {
			
			@Override
			public void perform() {
				// TODO Auto-generated method stub
				log("Yes. The lone complete patient series is the best patient series");
			}
		});
    	
    	setLogicOutcome(2, new LogicOutcome() {
			
			@Override
			public void perform() {
				// TODO Auto-generated method stub
				log("Yes. The lone in-process patient series is the best patient series");
				
			}
		});
    	
    	setLogicOutcome(3, new LogicOutcome() {
			
			@Override
			public void perform() {
				// TODO Auto-generated method stub
				log("Yes. The lone default patient series is the best patient series");				
			}
		});
    	
    	setLogicOutcome(4, new LogicOutcome() {
			
			@Override
			public void perform() {
				// TODO Auto-generated method stub
				log("No. More than one patient series has potential. All patient series are examined to see which should be scored and selected as the best patient series");
				
			}
		});

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
