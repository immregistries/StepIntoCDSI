package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.RequiredGender;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateGender extends LogicStep
{

  private ConditionAttribute<String> caGender = null;
  private ConditionAttribute<List<RequiredGender>> caRequiredGender = null;
 

  public EvaluateGender(DataModel dataModel)
  {
    super(LogicStepType.EVALUATE_GENDER, dataModel);
    setConditionTableName("Table ");
    
    caGender = new ConditionAttribute<String>("Patient", "Gender");
    caRequiredGender = new ConditionAttribute<List<RequiredGender>>("Supporting data (Gender)" , "Required Gender");
    
    caGender.setAssumedValue("UNKNOWN");
    caGender.setInitialValue(dataModel.getPatient().getGender());
    caRequiredGender.setInitialValue(dataModel.getTargetDose().getTrackedSeriesDose().getRequiredGenderList());
    
    conditionAttributesList.add(caGender);
    conditionAttributesList.add(caRequiredGender);
    
    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SATISFY_TARGET_DOSE);
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
    out.println("<p>Evaluate gender  validates the  patient gender  against the  required  gender.  In cases where a  target dose  does not specify gender attributes, the gender is valid.</p>"); 
    
    out.println("<p>The following process model, attribute table, and decision table are used to evaluate the gender.</p>");
    out.println("<p>ADD MODEL PICTURE</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(0, 0, "Table 4-31");

            setLogicCondition(0, new LogicCondition("Is the patient’s gender the same as one of the required genders? ") {
              @Override
              public LogicResult evaluateInternal() {
            	  List<RequiredGender> rg = caRequiredGender.getFinalValue();
                if (rg == null) {
                  return LogicResult.YES;
                }
                if (rg.contains(caGender.getFinalValue())) {
                  return LogicResult.YES;
                }
                return LogicResult.NO;
              }
            });

            setLogicResults(0, LogicResult.YES);
            setLogicResults(1, LogicResult.NO);

            setLogicOutcome(0, new LogicOutcome() {
              @Override
              public void perform() {
                log("No. The target dose cannot be skipped. ");
                log("Setting next step: 4.3 Substitute Target Dose");
                setNextLogicStepType(LogicStepType.SATISFY_TARGET_DOSE);
              }
            });
            
    }
  }


}
