package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.RequiredGender;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class MultipleAntigenVaccineGroup extends LogicStep
{

  // private ConditionAttribute<Date> caDateAdministered = null;

  public MultipleAntigenVaccineGroup(DataModel dataModel)
  {
    super(LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, dataModel);
    setConditionTableName("Table ");
    
    // caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    
   // caTriggerAgeDate.setAssumedValue(FUTURE);
    
//    conditionAttributesList.add(caDateAdministered);
    
    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
	    System.out.println("GGGGG");
    setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
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
    out.println("<p>The forecasting  decisions and  rules which need to be applied to a multiple antigen  vaccine group are  listed below</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(4, 6, "Table 7 - 4 WHAT IS THE VACCINE GROUP STATUS OF A MULTIPLE VACCINE GROUP?");

            setLogicCondition(0, new LogicCondition("Is there at least one best patient series status of \"Not Completed\"?") {
              @Override
              public LogicResult evaluateInternal() {
                  for (PatientSeries p : dataModel.getBestPatientSeriesList()){
                	  if (p.getPatientSeriesStatus().equals(PatientSeriesStatus.NOT_COMPLETE))
                		  return LogicResult.YES;
                  }
                  return LogicResult.NO;
                }
              });
            
            setLogicCondition(1, new LogicCondition("Are all best patient series status \"Immune\"?") {
                @Override
                public LogicResult evaluateInternal() {
                    for (PatientSeries p : dataModel.getBestPatientSeriesList()){
                  	  if (p.getPatientSeriesStatus().equals(PatientSeriesStatus.IMMUNE))
                  		  return LogicResult.NO;
                    }
                    return LogicResult.YES;
                  }
                });
            
            setLogicCondition(2, new LogicCondition("Is there at least one best patient series status of \"Contraindicated\"?") {
                @Override
                public LogicResult evaluateInternal() {
                    for (PatientSeries p : dataModel.getBestPatientSeriesList()){
                  	  if (p.getPatientSeriesStatus().equals(PatientSeriesStatus.CONTRAINDICATED))
                  		  return LogicResult.YES;
                    }
                    return LogicResult.NO;
                  }
                });
            
            setLogicCondition(3, new LogicCondition("Is the recommendation for the vaccine group to administer full vaccine group?") {
                @Override
                public LogicResult evaluateInternal() {

                    return LogicResult.NO;
                  }
                });
            
            setLogicResults(0, LogicResult.NO, LogicResult.NO, LogicResult.ANY, LogicResult.YES, LogicResult.YES, LogicResult.ANY);
            setLogicResults(1, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.YES);
            setLogicResults(2, LogicResult.NO, LogicResult.YES, LogicResult.YES, LogicResult.NO, LogicResult.ANY, LogicResult.ANY);
            setLogicResults(3, LogicResult.ANY, LogicResult.NO, LogicResult.YES, LogicResult.YES, LogicResult.NO, LogicResult.ANY);

            
            setLogicOutcome(0, new LogicOutcome() {
                @Override
                public void perform() {
                  log("Completed");
                  setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
                }
              });
            setLogicOutcome(1, new LogicOutcome() {
                @Override
                public void perform() {
                  log("Contraindicated");
                  setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
                }
              });
            setLogicOutcome(2, new LogicOutcome() {
                @Override
                public void perform() {
                  log("Contraindicated");
                  setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
                }
              });
            setLogicOutcome(3, new LogicOutcome() {
                @Override
                public void perform() {
                  log("Not Complete");
                  setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
                }
              });
            setLogicOutcome(4, new LogicOutcome() {
                @Override
                public void perform() {
                  log("Not Complete");
                  setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
                }
              });
            setLogicOutcome(5, new LogicOutcome() {
                @Override
                public void perform() {
                  log("Immune");
                  setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
                }
              });
            
    }
  }


}
