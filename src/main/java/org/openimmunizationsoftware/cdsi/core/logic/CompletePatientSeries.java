package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class CompletePatientSeries extends LogicStep
{

  // private ConditionAttribute<Date> caDateAdministered = null;

  public CompletePatientSeries(DataModel dataModel)
  {
    super(LogicStepType.COMPLETE_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");
    
    // caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    
   // caTriggerAgeDate.setAssumedValue(FUTURE);
    
//    conditionAttributesList.add(caDateAdministered);
    
    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_BEST_CANDIDATE_PATIENT_SERIES);
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
    out.println("<p>Complete  patient  series  provides  the  decision  table  for  determining  the  number  of  points  to  assign  to  a complete patient series based on a specified condition. </p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(3, 0, "Table 6-5 How many points are awarded to a complete patient series when 2 or more candidate patient series are complete ?");
      
      setLogicCondition(0, new  LogicCondition("A candidate patient series has the most valid doses") {
		
		@Override
		protected LogicResult evaluateInternal() {
			// TODO Auto-generated method stub
			return null;
		}
	});
    
    }
  }


}
