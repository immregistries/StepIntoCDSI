package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.io.PrintWriter;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.RequiredGender;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class ClassifyVaccineGroup extends LogicStep
{

  // private ConditionAttribute<Date> caDateAdministered = null;

  public ClassifyVaccineGroup(DataModel dataModel)
  {
    super(LogicStepType.CLASSIFY_VACCINE_GROUP, dataModel);
    setConditionTableName("Table ");
    // caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
   // caTriggerAgeDate.setAssumedValue(FUTURE);
//    conditionAttributesList.add(caDateAdministered);
  	for (VaccineGroupForecast vaccGpeFor : dataModel.getVaccineGroupForcastList()){
  	    LT logicTable = new LT();
  		logicTable.vaccineGroupForecast = vaccGpeFor;
  		logicTableList.add(logicTable);

  	}
  }

  @Override
  public LogicStep process() throws Exception {
   // setNextLogicStepType(LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP);
   //setNextLogicStepType(LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP);
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
    out.println("<p>Classify vaccine group provides initial questioning to determine which vaccine group forecast rules to apply.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
	  public VaccineGroupForecast vaccineGroupForecast;
	  public LT() {
	      super(1, 2, "TABLE 7 - 2 WHAT IS THE VACCINE GROUP TYPE?");

	      setLogicCondition(0, new LogicCondition("Does the vaccine group contain exactly 1 antigen?"){
	            public LogicResult evaluateInternal() {
	            		if (vaccineGroupForecast.getVaccineGroupForecast().getAntigensNeededList().size() == 1){
	            			return LogicResult.YES;
	            		}
	            	
	            	return LogicResult.NO;
	            }
	      });
	      setLogicResults(0, YES, NO);
	      //setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.NO });

	      setLogicOutcome(0, new LogicOutcome() {
	        @Override
	        public void perform() {
	        	setNextLogicStepType(LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP);
	        	log("Vaccine group is a single antigen vaccine group.");
	        }
	      });

	      setLogicOutcome(1, new LogicOutcome() {
	        @Override
	        public void perform() {
	          log("Vaccine group is a multiple antigen vaccine group.");
	          setNextLogicStepType(LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP);
	        }
	      });

	    }
  }


}
