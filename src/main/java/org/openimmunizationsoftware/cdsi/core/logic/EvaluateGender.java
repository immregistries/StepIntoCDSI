package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.io.PrintWriter;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.RequiredGender;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateGender extends LogicStep {

  private ConditionAttribute<String> caGender = null;
  private ConditionAttribute<List<RequiredGender>> caRequiredGender = null;

  public EvaluateGender(DataModel dataModel) {
    super(LogicStepType.EVALUATE_GENDER, dataModel);
    setConditionTableName("Table ");

    caGender = new ConditionAttribute<String>("Patient", "Gender");
    caRequiredGender = new ConditionAttribute<List<RequiredGender>>("Supporting data (Gender)", "Required Gender");

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
    for (LogicTable logicTable : logicTableList) {
      logicTable.evaluate();
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
        "<p>Evaluate gender  validates the  patient gender  against the  required  gender.  In cases where a  target dose  does not specify gender attributes, the gender is valid.</p>");

    out.println(
        "<p>The following process model, attribute table, and decision table are used to evaluate the gender.</p>");
    out.println("<p>ADD MODEL PICTURE</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(1, 2, "Table 4-31");

      setLogicCondition(0, new LogicCondition("Is the patient's gender the same as one of the required genders? ") {
        @Override
        public LogicResult evaluateInternal() {
          List<RequiredGender> rg = caRequiredGender.getFinalValue();
          if (rg == null) {
            return LogicResult.YES;
          }
          if (caRequiredGender.getFinalValue().size() == 0) {
            return LogicResult.YES;
          }
          if (caRequiredGender.getFinalValue().contains(caGender.getFinalValue())) {
            return LogicResult.YES;
          }
          return LogicResult.YES;
        }
      });

      setLogicResults(0, YES, NO);
      //setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.NO });

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          if (caRequiredGender.getFinalValue().size() == 0) {
            log("Yes. Required patient's gender is not set." );
          }
          else
          {
            log("Yes. Patient's gender is one of the required genders.");
          }
          log("Setting next step: 4.10 Satisfy Target Dose");
          setNextLogicStepType(LogicStepType.SATISFY_TARGET_DOSE);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. Patient's gender is not one of the required genders. Evaluation Reason is “incorrect gender.”");
          log("Setting next step: 4.10 Satisfy Target Dose");
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          dataModel.getTargetDose().setStatusCause(dataModel.getTargetDose().getStatusCause()+"Gender");
          setNextLogicStepType(LogicStepType.SATISFY_TARGET_DOSE);
        }
      });

    }
  }

}
