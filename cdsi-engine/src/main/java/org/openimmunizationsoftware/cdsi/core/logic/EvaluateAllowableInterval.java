package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_3;

import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableInterval;
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

public class EvaluateAllowableInterval extends LogicStep {

  private ConditionAttribute<Date> caDateAdministered = null;
  private ConditionAttribute<AllowableInterval> caAllowableIntervalElements = null;
  private ConditionAttribute<Date> caAbsoluteMinimumIntervalDate = null;

  public EvaluateAllowableInterval(DataModel dataModel) {
    super(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL, dataModel);
    setConditionTableName("Table ");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caAllowableIntervalElements = new ConditionAttribute<AllowableInterval>("Supporting Data",
        "Allowable Interval Elements");
    caAbsoluteMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date", "Absolute Minimum Interval Date");

    caAbsoluteMinimumIntervalDate.setAssumedValue(PAST);

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caAllowableIntervalElements);
    conditionAttributesList.add(caAbsoluteMinimumIntervalDate);

    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();

    if (seriesDose.getAllowableintervalList().size() > 0) {
      for (AllowableInterval aInterval : seriesDose.getAllowableintervalList()) {
        caAllowableIntervalElements.setInitialValue(aInterval);
        Interval intervalFromAllowableInterval = aInterval.getInterval();
        caAbsoluteMinimumIntervalDate
            .setInitialValue(CALCDTINT_3.evaluate(dataModel, this, intervalFromAllowableInterval));

        LT logicTable = new LT();
        logicTable.setLogicStepSink(this.getLogicStepSink());
        logicTableList.add(logicTable);
      }
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
    if (satisfiedAll == YesNo.NO || logicTableList.size() == 0) {
      log(LogLevel.STATE, "Allowable interval NOT satisfied - interval requirement failed");
      dataModel.getTargetDose()
          .setStatusCause(dataModel.getTargetDose().getStatusCause() + "Interval");
    } else {
      log(LogLevel.STATE, "Allowable interval satisfied");
    }

    setNextLogicStepType(LogicStepType.EVALUATE_VACCINE_CONFLICT);
    return next();
  }

  private class LT extends LogicTable {
    private YesNo result = null;

    public LT() {
      super(1, 2,
          "Table 6 - 21 Did the vaccine dose administered satisfy the defined Allowable interval?");

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

      setLogicResults(0, LogicResult.YES, LogicResult.NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The vaccine dose administered did not satisfy the defined allowable interval for the target dose. Evaluation Reason is 'Too soon'.");
          Evaluation evaluation = dataModel.getTargetDose().getEvaluation();
          evaluation.setEvaluationReason(EvaluationReason.TOO_SOON);
          result = YesNo.NO;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The vaccine dose administered satisfied the allowable interval for the target dose.");
          result = YesNo.YES;
        }
      });

    }

    public YesNo getResult() {
      return result;
    }
  }
}
