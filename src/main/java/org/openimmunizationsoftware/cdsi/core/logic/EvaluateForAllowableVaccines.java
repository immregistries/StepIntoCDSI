package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTALLOW_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTALLOW_2;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateForAllowableVaccines extends LogicStep
{

  private ConditionAttribute<Date> caDateAdministered = null;
  private ConditionAttribute<String> caVaccineType = null;
  private ConditionAttribute<String> caVaccineTypeAllowable = null;
  private ConditionAttribute<Date> caAllowableVaccineTypeBeginAgeDate = null;
  private ConditionAttribute<Date> caAllowableVaccineTypeEndAgeDate = null;

  public EvaluateForAllowableVaccines(DataModel dataModel) {
    super(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED, dataModel);
    setConditionTableName("Table 4.8");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caVaccineType = new ConditionAttribute<String>("Vaccine Dose Administered", "Vaccine Type");
    caVaccineTypeAllowable = new ConditionAttribute<String>("Supporting data (Allowable Vaccine)", "Vaccine Type");
    caAllowableVaccineTypeBeginAgeDate = new ConditionAttribute<Date>("Calculated data (CALCDTALLOW-1)",
        "Allowable Vaccine Type Begin Age Date");
    caAllowableVaccineTypeEndAgeDate = new ConditionAttribute<Date>("Calculated Data (CALCDTALLOW-2)",
        "Allowable Vaccine Type End Age Date");

    caAllowableVaccineTypeBeginAgeDate.setAssumedValue(PAST);
    caAllowableVaccineTypeEndAgeDate.setAssumedValue(FUTURE);

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caVaccineType);
    conditionAttributesList.add(caVaccineTypeAllowable);
    conditionAttributesList.add(caAllowableVaccineTypeBeginAgeDate);
    conditionAttributesList.add(caAllowableVaccineTypeEndAgeDate);

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
    out.println("<p>Evaluate  for  allowable  vaccine  validates  the  vaccine  of  a  vaccine  dose  administered  against  the  list  of allowable vaccines.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(0, 0, "Table 4.8");

      setLogicCondition(0, new LogicCondition("Is the vaccine type of the vaccine dose administered the same as the vaccine type of the allowable vaccine?") {
          @Override
          public LogicResult evaluateInternal() {
            if (caDateAdministered.getFinalValue() == null) {
              return LogicResult.NO;
            }
            if (caVaccineType.getFinalValue().equals(caVaccineTypeAllowable.getFinalValue())) {
              return LogicResult.YES;
            }
            return LogicResult.NO;
          }
        });      
      
      setLogicCondition(1, new LogicCondition("Is the Allowable vaccine type begin age date ≤ date administered < allowable vaccine type end age date?") {
              @Override
              public LogicResult evaluateInternal() {
                if (caDateAdministered.getFinalValue() == null || caAllowableVaccineTypeBeginAgeDate.getFinalValue() == null || caAllowableVaccineTypeEndAgeDate.getFinalValue()==null ) {
                  return LogicResult.NO;
                }
                if (caDateAdministered.getFinalValue().before(caAllowableVaccineTypeEndAgeDate.getFinalValue()) && caDateAdministered.getFinalValue().after(caAllowableVaccineTypeBeginAgeDate.getFinalValue())) {
                  return LogicResult.YES;
                }
                return LogicResult.NO;
              }
            });
            
           setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.ANY);
           setLogicOutcome(0, new LogicOutcome() {
              @Override
              public void perform() {
                log("No. The target dose cannot be skipped. ");
                log("Setting next step: 4.9 EvaluateGender");
                setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
              }
            });
            
    }
  }

}
