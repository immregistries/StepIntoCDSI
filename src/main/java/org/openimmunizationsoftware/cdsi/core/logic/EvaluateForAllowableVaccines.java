package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
//import org.openimmunizationsoftware.cdsi.core.logic.EvaluateForPreferableVaccine.LT;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateForAllowableVaccines extends LogicStep {



  public EvaluateForAllowableVaccines(DataModel dataModel) {
    super(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED, dataModel);
    setConditionTableName("Table 4.8");

    for ( AllowableVaccine pi : dataModel.getTargetDose().getTrackedSeriesDose().getAllowableVaccineList()){

    	LT logicTable = new LT();

    	logicTable.caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    	logicTable.caVaccineType = new ConditionAttribute<VaccineType>("Vaccine Dose Administered", "Vaccine Type");
    	logicTable.caVaccineTypeAllowable = new ConditionAttribute<AllowableVaccine>("Supporting data (Allowable Vaccine)",
            "Vaccine Type");
    	logicTable.caAllowableVaccineTypeBeginAgeDate = new ConditionAttribute<Date>("Calculated data (CALCDTALLOW-1)",
            "Allowable Vaccine Type Begin Age Date");
    	logicTable.caAllowableVaccineTypeEndAgeDate = new ConditionAttribute<Date>("Calculated Data (CALCDTALLOW-2)",
            "Allowable Vaccine Type End Age Date");

    	logicTable.caAllowableVaccineTypeBeginAgeDate.setAssumedValue(PAST);
    	logicTable.caAllowableVaccineTypeEndAgeDate.setAssumedValue(FUTURE);
        
    	conditionAttributesList.add(logicTable.caDateAdministered);
        conditionAttributesList.add(logicTable.caVaccineType);
        conditionAttributesList.add(logicTable.caVaccineTypeAllowable);
        conditionAttributesList.add(logicTable.caAllowableVaccineTypeBeginAgeDate);
        conditionAttributesList.add(logicTable. caAllowableVaccineTypeEndAgeDate);

        AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
        logicTable.caDateAdministered.setInitialValue(aar.getDateAdministered());
        logicTable.caVaccineType.setInitialValue(aar.getVaccineType());
        logicTable.caVaccineTypeAllowable.setInitialValue(pi);


        logicTableList.add(logicTable);
    }
    
    
  }

  @Override
  public LogicStep process() throws Exception {
    	setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
	    //setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
	    YesNo y=YesNo.NO;
	    for (LogicTable logicTable : logicTableList) {
	        logicTable.evaluate();
	        if (((LT) logicTable).getResult() == YesNo.YES) {
	      	  y=YesNo.YES;
	        }
	      }
	    if (y == YesNo.YES){
	          dataModel.getTargetDose().setStatusCause(dataModel.getTargetDose().getStatusCause()+"Vaccine");
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
    out.println(
        "<p>Evaluate  for  allowable  vaccine  validates  the  vaccine  of  a  vaccine  dose  administered  against  the  list  of allowable vaccines.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
	  
	  private ConditionAttribute<Date> caDateAdministered = null;
	  private ConditionAttribute<VaccineType> caVaccineType = null;
	  private ConditionAttribute<AllowableVaccine> caVaccineTypeAllowable = null;
	  private ConditionAttribute<Date> caAllowableVaccineTypeBeginAgeDate = null;
	  private ConditionAttribute<Date> caAllowableVaccineTypeEndAgeDate = null;
	  private YesNo result = null;
	  
    public LT() {
      super(2, 3, "Table 4.28");

      setLogicCondition(0, new LogicCondition(
          "Is the vaccine type of the vaccine dose administered the same as the vaccine type of the allowable vaccine?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caDateAdministered.getFinalValue() == null) {
            return LogicResult.NO;
          }
          //for (int i = 0; i < caVaccineTypeAllowable.getFinalValue().size(); i++) {
            VaccineType vt = caVaccineType.getFinalValue();
            AllowableVaccine av = caVaccineTypeAllowable.getFinalValue();
            Date birthDate = dataModel.getPatient().getDateOfBirth();
            if (vt == av.getVaccineType()) {
              caAllowableVaccineTypeBeginAgeDate.setInitialValue(av.getVaccineTypeBeginAge().getDateFrom(birthDate));
              caAllowableVaccineTypeEndAgeDate.setInitialValue(av.getVaccineTypeEndAge().getDateFrom(birthDate));
              return LogicResult.YES;

            }
          //}
          return LogicResult.NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Is the Allowable vaccine type begin age date ≤ date administered < allowable vaccine type end age date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caDateAdministered.getFinalValue() == null || caAllowableVaccineTypeBeginAgeDate.getFinalValue() == null
              || caAllowableVaccineTypeEndAgeDate.getFinalValue() == null) {
            return LogicResult.NO;
          }
          if (caDateAdministered.getFinalValue().before(caAllowableVaccineTypeEndAgeDate.getFinalValue())
              && caDateAdministered.getFinalValue().after(caAllowableVaccineTypeBeginAgeDate.getFinalValue())) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.YES });
      setLogicResults(1, new LogicResult[] { LogicResult.YES, LogicResult.ANY, LogicResult.NO });

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
        	result=YesNo.YES;
          log("Yes. An allowable vaccine was administered ");
          log("Setting next step: 4.9 EvaluateGender");
        }
      });
      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
        	result=YesNo.NO;
          log("No.  This supporting data defined allowable vaccine was not administered.");
          log("Setting next step: 4.9 EvaluateGender");
        }
      });
      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
        	result=YesNo.NO;
log("No.  This supporting data defined allowable vaccine was administered out of the allowable age range.");
          log("Setting next step: 4.9 EvaluateGender");
        }
      });

    }
    public YesNo getResult() {
        // TODO Auto-generated method stub
        return result;
      }
  }

}
