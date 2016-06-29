package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTPREF_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTPREF_2;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateForPreferableVaccine extends LogicStep
{

  private ConditionAttribute<Date> caDateAdministered = null;
  private ConditionAttribute<String> caTradeName = null;
  private ConditionAttribute<Date> caVaccineTypeBeginAgeDate = null;
  private ConditionAttribute<Date> caVaccineTypeEndAgeDate = null;
  private ConditionAttribute<String> caPreferableVaccineTradeName = null;
  private ConditionAttribute<String> caPreferableVaccineVolume = null;

  public EvaluateForPreferableVaccine(DataModel dataModel) {
    super(LogicStepType.EVALUATE_PREFERABLE_VACCINE_ADMINISTERED, dataModel);
    setConditionTableName("Table ");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");

    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    // set assumed values
    caVaccineTypeBeginAgeDate.setAssumedValue(PAST);
    caVaccineTypeEndAgeDate.setAssumedValue(FUTURE);
    caPreferableVaccineTradeName.setAssumedValue(aar.getTradeName());
    caPreferableVaccineVolume.setAssumedValue(aar.getVolume());

    // set actual values
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    caTradeName.setInitialValue(aar.getTradeName());
    caVaccineTypeBeginAgeDate.setInitialValue(CALCDTPREF_1.evaluate(dataModel, this));
    caVaccineTypeEndAgeDate.setInitialValue(CALCDTPREF_2.evaluate(dataModel, this));
    //caPreferableVaccineTradeName.setInitialValue(initialValue);

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caTradeName);
    conditionAttributesList.add(caVaccineTypeBeginAgeDate);
    conditionAttributesList.add(caVaccineTypeEndAgeDate);
    conditionAttributesList.add(caPreferableVaccineTradeName);
    conditionAttributesList.add(caPreferableVaccineVolume);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
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
      super(0, 0, "Table 4-5 Was the supporting data defined preferrable vaccine administered?");

//      setLogicCondition(0, new LogicCondition(
//          "Is the vaccine type of the vaccine dose administered the same as the vaccine type of the preferable vaccine? ") {
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
