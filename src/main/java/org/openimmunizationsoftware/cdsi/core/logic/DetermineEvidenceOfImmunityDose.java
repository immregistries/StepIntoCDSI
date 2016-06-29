package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

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
    caImmunityGuideline = new ConditionAttribute<String>("Supporting Data (Clinical History Immunity",
        "Immunity Guideline");
    caImmunityDate = new ConditionAttribute<Date>("Supporting Data (Birth Date Immunity", "Immunity Date");
    caExclusionCondition = new ConditionAttribute<String>("Supporting Data (Birth Date Immunity)",
        "Exclusion Condition");
    caCountryofBirthWorking = new ConditionAttribute<String>("Supporting Data (Birth Date Immunity)",
        "Country of Birth");

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
    out.println("<p>TODO</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(0, 0, "Table ?-?");

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
