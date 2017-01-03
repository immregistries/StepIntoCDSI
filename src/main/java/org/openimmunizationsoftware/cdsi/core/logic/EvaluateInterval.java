package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_2;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_3;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_4;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_5;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_6;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_7;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateInterval extends LogicStep {

  public EvaluateInterval(DataModel dataModel) {
    super(LogicStepType.EVALUATE_INTERVAL, dataModel);
    setConditionTableName("Table ");

    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();

    int intervalCount = 0;
    if (seriesDose.getAgeList().size() > 0) {
      for (Interval interval : seriesDose.getIntervalList()) {
        intervalCount++;
        LT logicTable = new LT();
        logicTableList.add(logicTable);

        logicTable.caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
        logicTable.caFromImmediatePreviousDoseAdministered = new ConditionAttribute<YesNo>("Supporting Data",
            "From Immediate Previous Dose Administered");
        logicTable.caFromTargetDoseNumberInSeries = new ConditionAttribute<String>("Supporting Data",
            "From Target Dose Number In Series");
        logicTable.caFromMostRecent = new ConditionAttribute<String>("Supporting Data (Interval)",
            "From Most Recent (CVX)");
        logicTable.caAbsoluteMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date",
            "Absolute Minimum Interval Date");
        logicTable.caMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date", "Mimium Interval Date");

        logicTable.caAbsoluteMinimumIntervalDate.setAssumedValue(PAST);
        logicTable.caMinimumIntervalDate.setAssumedValue(PAST);

        List<ConditionAttribute<?>> list = new ArrayList<ConditionAttribute<?>>();
        list.add(logicTable.caDateAdministered);
        list.add(logicTable.caFromImmediatePreviousDoseAdministered);
        list.add(logicTable.caFromTargetDoseNumberInSeries);
        list.add(logicTable.caFromMostRecent);
        list.add(logicTable.caAbsoluteMinimumIntervalDate);
        list.add(logicTable.caMinimumIntervalDate);
        conditionAttributesAdditionalMap.put("Interval Check #" + intervalCount, list);

        AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
        logicTable.caDateAdministered.setInitialValue(aar.getDateAdministered());
        logicTable.caFromImmediatePreviousDoseAdministered
            .setInitialValue(interval.getFromImmediatePreviousDoseAdministered());
        logicTable.caFromTargetDoseNumberInSeries.setInitialValue(interval.getFromTargetDoseNumberInSeries());
        logicTable.caAbsoluteMinimumIntervalDate.setInitialValue(CALCDTINT_3.evaluate(dataModel, this, null));
        logicTable.caMinimumIntervalDate.setInitialValue(CALCDTINT_4.evaluate(dataModel, this, null));
      }
    }
  }

  @Override
  public LogicStep process() throws Exception {
    YesNo result = YesNo.YES;
    for (LogicTable logicTable : logicTableList) {
      logicTable.evaluate();
      if (((LT) logicTable).getResult() == YesNo.NO) {
        result = YesNo.NO;
      }
    }
    if (result == YesNo.YES) {
      setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL);
    } else {
      setNextLogicStepType(LogicStepType.EVALUATE_FOR_LIVE_VIRUS_CONFLICT);
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
        "<p>Evaluate interval validates the date administered of a vaccine dose administered against defined interval(s) from previous vaccine dose(s) administered.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {

    private ConditionAttribute<Date> caDateAdministered = null;
    private ConditionAttribute<YesNo> caFromImmediatePreviousDoseAdministered = null;
    private ConditionAttribute<String> caFromTargetDoseNumberInSeries = null;
    private ConditionAttribute<String> caFromMostRecent = null;
    private ConditionAttribute<Date> caAbsoluteMinimumIntervalDate = null;
    private ConditionAttribute<Date> caMinimumIntervalDate = null;

    private YesNo result = null;

    public YesNo getResult() {
      return result;
    }

    public LT() {
      super(5, 5, "Table 4 - 14 Did the vaccine dose administered satisfy the defined interval?");

      setLogicCondition(0, new LogicCondition("Absolute minimum interval date > date administered?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caAbsoluteMinimumIntervalDate.getFinalValue().after(caDateAdministered.getFinalValue())) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(1,
          new LogicCondition("Absolute minimum interval date <= date administered < minimum interval date?") {
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
          if (caFromTargetDoseNumberInSeries.equals(String.valueOf(1))) {
            return LogicResult.YES;
          }
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

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("");
          result = YesNo.NO;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("");
          result = YesNo.NO;
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("");
          result = YesNo.YES;
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("");
          result = YesNo.YES;
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("");
          result = YesNo.YES;
        }
      });

    }
  }

}
