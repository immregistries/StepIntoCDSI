package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_3;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_4;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluatePreferableInterval extends LogicStep {

  public EvaluatePreferableInterval(DataModel dataModel) {
    super(LogicStepType.EVALUATE_PREFERABLE_INTERVAL, dataModel);
    setConditionTableName("Table ");

    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();

    int intervalCount = 0;
    for (Interval interval : seriesDose.getIntervalList()) {
      intervalCount++;
      LT logicTable = new LT();

      logicTable.caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
      logicTable.caPreferableIntervalElements = new ConditionAttribute<Interval>("Supporting Data",
          "Preferable Interval Elements");
      logicTable.caAbsoluteMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date",
          "Absolute Minimum Interval Date");
      logicTable.caMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date", "Mimium Interval Date");

      logicTable.caAbsoluteMinimumIntervalDate.setAssumedValue(PAST);
      logicTable.caMinimumIntervalDate.setAssumedValue(PAST);

      List<ConditionAttribute<?>> caList = new ArrayList<ConditionAttribute<?>>();
      caList.add(logicTable.caDateAdministered);
      caList.add(logicTable.caPreferableIntervalElements);
      caList.add(logicTable.caAbsoluteMinimumIntervalDate);
      caList.add(logicTable.caMinimumIntervalDate);
      conditionAttributesAdditionalMap.put("Interval Check #" + intervalCount,
          caList);

      AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
      logicTable.caDateAdministered.setInitialValue(aar.getDateAdministered());
      logicTable.caPreferableIntervalElements
          .setInitialValue(interval);
      logicTable.caAbsoluteMinimumIntervalDate
          .setInitialValue(CALCDTINT_3.evaluate(dataModel, this, interval));
      logicTable.caMinimumIntervalDate.setInitialValue(CALCDTINT_4.evaluate(dataModel, this, interval));

      logicTable.setLogicStepSink(this.getLogicStepSink());
      logicTableList.add(logicTable);
    }
  }

  @Override
  public LogicStep process() throws Exception {
    YesNo satisfiedAll = YesNo.YES;
    for (LogicTable logicTable : logicTableList) {
      logicTable.evaluate();
      if (((LT) logicTable).getResult() == YesNo.NO) {
        satisfiedAll = YesNo.NO;
      }
    }
    if (satisfiedAll == YesNo.YES) {
      log(LogLevel.STATE, "Preferable interval satisfied - continuing to vaccine conflict evaluation");
      setNextLogicStepType(LogicStepType.EVALUATE_VACCINE_CONFLICT);
    } else {
      log(LogLevel.STATE, "Preferable interval NOT satisfied - checking allowable interval");
      setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL);
    }
    return next();
  }

  private class LT extends LogicTable {
    private ConditionAttribute<Date> caDateAdministered = null;
    private ConditionAttribute<Interval> caPreferableIntervalElements = null;
    private ConditionAttribute<Date> caAbsoluteMinimumIntervalDate = null;
    private ConditionAttribute<Date> caMinimumIntervalDate = null;

    private YesNo result = null;

    public YesNo getResult() {
      return result;
    }

    public LT() {
      super(3, 3, "Table 6 - 18 Did the vaccine dose administered satisfy the defined interval?");

      setLogicCondition(0,
          new LogicCondition("Is the date administered < absolute minimum interval date?") {
            @Override
            public LogicResult evaluateInternal() {
              if (caDateAdministered.getFinalValue()
                  .before(caAbsoluteMinimumIntervalDate.getFinalValue())) {
                return LogicResult.YES;
              }
              return LogicResult.NO;
            }
          });

      setLogicCondition(1, new LogicCondition(
          "Absolute minimum interval date <= date administered < minimum interval date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caAbsoluteMinimumIntervalDate.getFinalValue()
              .after(caDateAdministered.getFinalValue())) {
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

      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO);
      setLogicResults(1, LogicResult.NO, LogicResult.YES, LogicResult.NO);
      setLogicResults(2, LogicResult.NO, LogicResult.NO, LogicResult.YES);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The vaccine dose administered did not satisfy the preferable interval for the target dose. Evaluation reason is 'Too Soon'.");
          Evaluation evaluation = dataModel.getTargetDose().getEvaluation();
          evaluation.setEvaluationReason(EvaluationReason.GRACE_PERIOD);
          result = YesNo.NO;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The vaccine dose administered satisfied the preferable interval for the target dose. Evaluation reason is 'Grace Period'.");
          Evaluation evaluation = dataModel.getTargetDose().getEvaluation();
          evaluation.setEvaluationReason(EvaluationReason.GRACE_PERIOD);
          result = YesNo.YES;
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The vaccine dose administered satisfied the preferable interval for the target dose.");
          result = YesNo.YES;
        }
      });
    }
  }
}
