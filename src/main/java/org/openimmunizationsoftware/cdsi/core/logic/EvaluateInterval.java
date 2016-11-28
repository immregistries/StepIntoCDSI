package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_2;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_3;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_4;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_5;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_6;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_7;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateInterval extends LogicStep
{

  private ConditionAttribute<Date> caDateAdministered = null;
  private ConditionAttribute<YesNo> caFromImmediatePreviousDoseAdministered = null;
  private ConditionAttribute<String> caFromTargetDoseNumberInSeries = null;
  private ConditionAttribute<String> caFromMostRecent = null;
  private ConditionAttribute<Date> caAbsoluteMinimumIntervalDate = null;
  private ConditionAttribute<Date> caMinimumIntervalDate = null;

  public EvaluateInterval(DataModel dataModel) {
    super(LogicStepType.EVALUATE_INTERVAL, dataModel);
    setConditionTableName("Table ");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caFromImmediatePreviousDoseAdministered = new ConditionAttribute<YesNo>("Supporting Data",
        "From Immediate Previous Dose Administered");
    caFromTargetDoseNumberInSeries = new ConditionAttribute<String>("Supporting Data",
        "From Target Dose Number In Series");
    caFromMostRecent = new ConditionAttribute<String>("Supporting Data (Interval)", "From Most Recent (CVX)");
    caAbsoluteMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date", "Absolute Minimum Interval Date");
    caMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date", "Mimium Interval Date");

    caAbsoluteMinimumIntervalDate.setAssumedValue(PAST);
    caMinimumIntervalDate.setAssumedValue(PAST);

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caFromImmediatePreviousDoseAdministered);
    conditionAttributesList.add(caFromTargetDoseNumberInSeries);
    conditionAttributesList.add(caFromMostRecent);
    conditionAttributesList.add(caAbsoluteMinimumIntervalDate);
    conditionAttributesList.add(caMinimumIntervalDate);

    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    if (seriesDose.getAgeList().size() > 0) {
      if (seriesDose.getIntervalList().size() > 0) {
        Interval interval = seriesDose.getIntervalList().get(0);
        caFromImmediatePreviousDoseAdministered.setInitialValue(interval.getFromImmediatePreviousDoseAdministered());
        caFromTargetDoseNumberInSeries.setInitialValue(interval.getFromTargetDoseNumberInSeries());
      }
    }
    caAbsoluteMinimumIntervalDate.setInitialValue(CALCDTINT_3.evaluate(dataModel, this, null));
    caMinimumIntervalDate.setInitialValue(CALCDTINT_4.evaluate(dataModel, this, null));


    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL);
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
    out.println("<p>Evaluate interval validates the date administered of a vaccine dose administered against defined interval(s) from previous vaccine dose(s) administered.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(5, 5, "Table 4-11 Was the Vaccine Dose Administered at a Valid Interval?");

      setLogicCondition(0, new LogicCondition("Absolute minimum interval date > date administered?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caAbsoluteMinimumIntervalDate.getFinalValue().after(caDateAdministered.getFinalValue())) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Absolute minimum interval date <= date administered < minimum interval date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caAbsoluteMinimumIntervalDate.getFinalValue().after(caDateAdministered.getFinalValue())) {
            return LogicResult.NO;
          }
          if (caDateAdministered.getFinalValue().before(caMinimumIntervalDate.getFinalValue())) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(2, new LogicCondition("Minimum interval date <= date administered?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caDateAdministered.getFinalValue().before(caMinimumIntervalDate.getFinalValue())) {
            return LogicResult.NO;
          }
          return LogicResult.YES;
        }
      });

      setLogicCondition(3, new LogicCondition("Is this the first target dose?") {
        @Override
        public LogicResult evaluateInternal() {
          return LogicResult.NO;
        }
      });

      setLogicCondition(4, new LogicCondition(
          "Is the previous vaccine dose administered \"not valid\" due to age or interval requirements?") {
        @Override
        public LogicResult evaluateInternal() {
          return LogicResult.NO;
        }
      });

      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.NO);
      setLogicResults(1, LogicResult.NO, LogicResult.YES, LogicResult.YES, LogicResult.YES, LogicResult.NO);
      setLogicResults(2, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.NO);
      setLogicResults(3, LogicResult.ANY, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.ANY);
      setLogicResults(4, LogicResult.ANY, LogicResult.YES, LogicResult.NO, LogicResult.ANY, LogicResult.ANY);

      //      setLogicOutcome(0, new LogicOutcome() {
      //        @Override
      //        public void perform() {
      //          log("No. The target dose cannot be skipped. ");
      //          log("Setting next step: 4.3 Substitute Target Dose");
      //          setNextLogicStep(LogicStep.SUBSTITUTE_TARGET_DOSE_FOR_EVALUATION);
      //        }
      //      });
      
    }
  }

}
