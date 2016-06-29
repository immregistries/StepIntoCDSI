package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTALLOW_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTALLOW_2;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateForAllowableVaccines extends LogicStep
{

  private ConditionAttribute<Date> caDateAdministered = null;
  private ConditionAttribute<String> caVaccineType = null;
  private ConditionAttribute<String> caVaccineTypeAllowable = null;
  private ConditionAttribute<Date> caAllowableVaccineTYpeBeginAgeDate = null;
  private ConditionAttribute<Date> caAllowablwVaccineTypeEndAgeDate = null;

  public EvaluateForAllowableVaccines(DataModel dataModel) {
    super(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED, dataModel);
    setConditionTableName("Table ");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caVaccineType = new ConditionAttribute<String>("Vaccine Dose Administered", "Vaccine Type");
    caVaccineTypeAllowable = new ConditionAttribute<String>("Supporting data (Allowable Vaccine)", "Vaccine Type");
    caAllowableVaccineTYpeBeginAgeDate = new ConditionAttribute<Date>("Calculated data (CALCDTALLOW-1)",
        "Allowable Vaccine Type Begin Age Date");
    caAllowablwVaccineTypeEndAgeDate = new ConditionAttribute<Date>("Calcuated Data (CALCDTALLOW-2",
        "Allowable Vaccine Type End Age Date");

    caAllowableVaccineTYpeBeginAgeDate.setAssumedValue(PAST);
    caAllowablwVaccineTypeEndAgeDate.setAssumedValue(FUTURE);

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caVaccineType);
    conditionAttributesList.add(caVaccineTypeAllowable);
    conditionAttributesList.add(caAllowableVaccineTYpeBeginAgeDate);
    conditionAttributesList.add(caAllowablwVaccineTypeEndAgeDate);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
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
