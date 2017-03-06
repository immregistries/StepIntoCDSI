package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
//import org.openimmunizationsoftware.cdsi.core.logic.EvaluateAllowableInterval.LT;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateForPreferableVaccine extends LogicStep
{



  public EvaluateForPreferableVaccine(DataModel dataModel) {
    super(LogicStepType.EVALUATE_PREFERABLE_VACCINE_ADMINISTERED, dataModel);
    setConditionTableName("Table ");

    
  	Date birthDate = dataModel.getPatient().getDateOfBirth();

    int i=0, j=0, k=0;
    boolean allTrue = true;
    for ( PreferrableVaccine pi : dataModel.getTargetDose().getTrackedSeriesDose().getPreferrableVaccineList()){
    	//for (j=0; j<dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().size(); j++){
    		//for (k=0; k<dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getPreferrableVaccineList().size() ; k++){
	    LT logicTable = new LT();
	    
	    logicTable.caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
//	     _____________>Logic Problem : Check logic specification
	    
	    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
	    logicTable.caPreferableVaccineTradeName = new ConditionAttribute<String>("Supporting data (Preferable Vaccine)", "Preferable Vaccine Trade Name ") ;
	    logicTable.caTradeName = new ConditionAttribute<String>("Vaccine dose administered", "Trade Name ") ;
	    logicTable.caVaccineTypeBeginAgeDate = new ConditionAttribute<Date>("Vaccine Type Begin Age Date", "Calculated date (CALCDTPREF-1)");
	    logicTable.caVaccineTypeEndAgeDate = new ConditionAttribute<Date>("Vaccine Type End Age Date", "Calculated date (CALCDTPREF-2)");
	    logicTable.caPreferableVaccineVolume = new ConditionAttribute<String>("Preferable Vaccine Volume ","Supporting data (Preferable Vaccine)");
	    logicTable.caPreferableVaccineType = new ConditionAttribute<String>("Preferable Vaccine Type ","Supporting data (Preferable Vaccine)");
	    logicTable.caVolume = aar.getVolume();
	    logicTable.caTradeName.setInitialValue(aar.getAntigen().getName());
	    //caPreferableVaccineTradeName.setInitialValue( dataModel.getAntigenSeriesList().get(0).getSeriesDoseList().get(0).getAllowableVaccineList().get(0).getTradeName()) ;
	    logicTable.caDateAdministered.setInitialValue(aar.getDateAdministered());
	    
	    logicTable.caVaccineTypeBeginAgeDate.setAssumedValue(PAST);
	    logicTable.caVaccineTypeEndAgeDate.setAssumedValue(FUTURE);
	    logicTable.caPreferableVaccineTradeName.setAssumedValue(aar.getTradeName());
	    logicTable.caPreferableVaccineVolume.setAssumedValue(aar.getVolume());
	    
	    conditionAttributesList.add(logicTable.caDateAdministered);
	  	conditionAttributesList.add(logicTable.caTradeName);
	  	conditionAttributesList.add(logicTable.caVaccineTypeBeginAgeDate);
	  	conditionAttributesList.add(logicTable.caVaccineTypeEndAgeDate);
	  	conditionAttributesList.add(logicTable.caPreferableVaccineTradeName);
	  	conditionAttributesList.add(logicTable.caPreferableVaccineVolume);
	  	conditionAttributesList.add(logicTable.caPreferableVaccineType);
	    
	  	logicTable.caPreferableVaccineTradeName.setInitialValue( pi.getTradeName()) ;
	  	logicTable.caPreferableVaccineVolume.setInitialValue( pi.getVolume()) ;
	  	logicTable.pv = pi.getVolume();
	  	logicTable.caPreferableVaccineType.setInitialValue(pi.getVaccineType().getCvxCode());
    		    if (pi.getVaccineTypeBeginAge()!=null)
    		    	logicTable.caVaccineTypeBeginAgeDate.setInitialValue(pi.getVaccineTypeBeginAge().getDateFrom(birthDate));
    		    if (pi.getVaccineTypeEndAge()!=null)
    		    	logicTable.caVaccineTypeEndAgeDate.setInitialValue(pi.getVaccineTypeEndAge().getDateFrom(birthDate));

    		    
    		    //if (logicTable.getLogicOutcomes().equals())
    		   
    		    logicTableList.add(logicTable);
    		   // k++;
    		//}
    		//j++;
    	//}
    	//i++;
    }
    
    //    // set assumed values
//    caVaccineTypeBeginAgeDate.setAssumedValue(PAST);
//    caVaccineTypeEndAgeDate.setAssumedValue(FUTURE);
//    caPreferableVaccineTradeName.setAssumedValue(aar.getTradeName());
//    caPreferableVaccineVolume.setAssumedValue(aar.getVolume());
//
//    // set actual values
//    caDateAdministered.setInitialValue(aar.getDateAdministered());
//    caTradeName.setInitialValue(aar.getTradeName());
//    caVaccineTypeBeginAgeDate.setInitialValue(CALCDTPREF_1.evaluate(dataModel, this, null));
//    caVaccineTypeEndAgeDate.setInitialValue(CALCDTPREF_2.evaluate(dataModel, this, null));
//    //caPreferableVaccineTradeName.setInitialValue(initialValue);
//
//    conditionAttributesList.add(caDateAdministered);
//    conditionAttributesList.add(caTradeName);
//    conditionAttributesList.add(caVaccineTypeBeginAgeDate);
//    conditionAttributesList.add(caVaccineTypeEndAgeDate);
//    conditionAttributesList.add(caPreferableVaccineTradeName);
//    conditionAttributesList.add(caPreferableVaccineVolume);


  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
    //setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
    YesNo y=YesNo.NO;
    for (LogicTable logicTable : logicTableList) {
        logicTable.evaluate();
        if (((LT) logicTable).getResult() == YesNo.YES) {
      	  y=YesNo.YES;
      	setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
        }
      }
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
    out.println("<p>Evaluate  for  preferable  vaccine  validates  the  vaccine  of  a  vaccine  dose  administered  against  the  list  of preferable vaccines.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
	  private ConditionAttribute<Date> caDateAdministered = null;
	  //private ConditionAttribute<>
	  private ConditionAttribute<String> caTradeName = null;
	  private ConditionAttribute<Date> caVaccineTypeBeginAgeDate = null;
	  private ConditionAttribute<Date> caVaccineTypeEndAgeDate = null;
	  private ConditionAttribute<String> caPreferableVaccineTradeName = null;
	  private ConditionAttribute<String> caPreferableVaccineVolume = null;
	  private ConditionAttribute<String> caPreferableVaccineType = null;
	  private String caVolume = null;
	  private String pv = null;
	  private YesNo result = null;
	  
    public LT() {
      super(4, 5, "Table 4-5 Was the supporting data defined preferrable vaccine administered?");

      setLogicCondition(0, new LogicCondition(
          "Is the vaccine type of the vaccine dose administered the same as the vaccine type of the preferable vaccine? ") {
        @Override
        public LogicResult evaluateInternal() {
          if (caPreferableVaccineType.getFinalValue().equals(dataModel.getAntigenAdministeredRecord().getVaccineType().getCvxCode())) {
            return LogicResult.YES;
          }
          else{
          return LogicResult.NO;
          }
        }
      });
      setLogicCondition(1, new LogicCondition(
              "Is the Preferable vaccine type begin age date ≤ date administered < preferable vaccine type end age date? ") {
            @Override
            public LogicResult evaluateInternal() {
              if (caVaccineTypeBeginAgeDate.getFinalValue().before(caDateAdministered.getFinalValue()) && caDateAdministered.getFinalValue().before(caVaccineTypeEndAgeDate.getFinalValue())) {
                return LogicResult.YES;
              }
              else{
              return LogicResult.NO;
              }
            }
          });
      setLogicCondition(2, new LogicCondition(
              "Is the vaccine dose administered trade name the same as the preferable vaccine trade name? ") {
            @Override
            public LogicResult evaluateInternal() {
              if (caTradeName.getFinalValue().equals(caPreferableVaccineTradeName.getFinalValue())) {
                return LogicResult.YES;
              }
              else{
              return LogicResult.NO;
              }
            }
          });
      setLogicCondition(3, new LogicCondition(
              "Is the Vaccine dose administered volume >= preferable vaccine volume? ") {
            @Override
            public LogicResult evaluateInternal() {
            	if (!(caVolume.equals(new String()) || caPreferableVaccineVolume.getFinalValue().equals(new String()))){
            		if (Double.parseDouble(caPreferableVaccineVolume.getFinalValue()) <= Double.parseDouble(caVolume)) {
                        return LogicResult.YES;
                      }
                      else{
                      return LogicResult.NO;
                      }
            	}
              else{
              return LogicResult.NO;
              }
            }
          });
      setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.NO, LogicResult.YES, LogicResult.YES });
      setLogicResults(1, new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.ANY, LogicResult.NO, LogicResult.YES });
      setLogicResults(2, new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.NO });
      setLogicResults(3, new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY });

      setLogicOutcome(0, new LogicOutcome() {
          @Override
          public void perform() {
              result = YesNo.YES;
log("Yes. A preferable vaccine was administered.");
            //setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
          }
        });

      setLogicOutcome(1, new LogicOutcome() {
          @Override
          public void perform() {
              result = YesNo.YES;
 log("Yes. A preferable vaccine was administered. Evaluation Reason is volume administered is “less than recommended volume.”");
            //setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
          }
        });
      setLogicOutcome(2, new LogicOutcome() {
          @Override
          public void perform() {
        	 // setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
              result = YesNo.NO;
 log("No.  This supporting data defined preferable vaccine was not administered.");
          }
        });
      setLogicOutcome(3, new LogicOutcome() {
          @Override
          public void perform() {
        	 // setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
              result = YesNo.NO;
 log("No.  This supporting data defined preferable vaccine was administered out of the preferred age range.");
          }
        });
      setLogicOutcome(4, new LogicOutcome() {
          @Override
          public void perform() {
        	  //setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
              result = YesNo.NO;
 log("No. This supporting data defined preferable vaccine was of the wrong trade name. ");
          }
        });
      
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
    public YesNo getResult() { // TODO Auto-generated method stub
        return result;
      }
  }

}
