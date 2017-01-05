package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class SatisfyTargetDose extends LogicStep {

  public SatisfyTargetDose(DataModel dataModel) {
    super(LogicStepType.SATISFY_TARGET_DOSE, dataModel);
    setConditionTableName("Table ");

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {

    /***
     * Bypassing "4 Evaluate Vaccine Dose Administrated"
     * setNextLogicStepType(LogicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED);
     */
    setNextLogicStepType(logicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED);
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
        "<p>Satisfy  target  dose  uses  the  results  from  the  previous  evaluation  sections  as  conditions  to  determine  if the target dose is satisfied.  </p>");

    out.println(
        "<p>The following processing model and decision table are used to determine if the target dose was satisfied</p>");
    out.println("<p>ADD MODEL PICTURE</p>");

    // printConditionAttributesTable(out);
    // printLogicTables(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(0, 0, "Table ?-?");

      // setLogicCondition(0, new LogicCondition("date administered > lot
      // expiration date?") {
      // @Override
      // public LogicResult evaluateInternal() {
      // if (caDateAdministered.getFinalValue() == null ||
      // caTriggerAgeDate.getFinalValue() == null) {
      // return LogicResult.NO;
      // }
      // if
      // (caDateAdministered.getFinalValue().before(caTriggerAgeDate.getFinalValue()))
      // {
      // return LogicResult.YES;
      // }
      // return LogicResult.NO;
      // }
      // });

      // setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO,
      // LogicResult.ANY);

      // setLogicOutcome(0, new LogicOutcome() {
      // @Override
      // public void perform() {
      // log("No. The target dose cannot be skipped. ");
      // log("Setting next step: 4.3 Substitute Target Dose");
      // setNextLogicStep(LogicStep.SUBSTITUTE_TARGET_DOSE_FOR_EVALUATION);
      // }
      // });
      //
    }
  }

}
