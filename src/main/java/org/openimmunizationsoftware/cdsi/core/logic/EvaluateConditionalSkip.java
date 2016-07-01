package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTSKIP_3;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTSKIP_4;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTSKIP_5;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkip;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipCondition;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipSet;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateConditionalSkip extends LogicStep {

  private ConditionAttribute<Date> caDateAdministered = null;
  private ConditionAttribute<Date> caAssessmentDate = null;
  private ConditionAttribute<Integer> caAdministeredDoseCount = null;

  private class ForEachConditionalSkipSet {
    public class ForEachConditionInSet {
      private ConditionAttribute<Date> caConditionalSkipBeginAgeDate = null;
      private ConditionAttribute<Date> caConditionalSkipEndAgeDate = null;
      private ConditionAttribute<Date> caConditionalSkipIntervalDate = null;
      private ConditionAttribute<Date> caConditionalSkipStartDate = null;
      private ConditionAttribute<Date> caConditionalSkipEndDate = null;
      private ConditionAttribute<String> caConditionalDoseType = null;
      private ConditionAttribute<String> caConditionalSkipDoseCountLogic = null;
      private ConditionAttribute<Integer> caConditonalSkipDoseCount = null;
    }

    public List<ForEachConditionInSet> forEachConditionInSetList = new ArrayList<EvaluateConditionalSkip.ForEachConditionalSkipSet.ForEachConditionInSet>();
  }

  private List<ForEachConditionalSkipSet> forEachConditionalSkipSetList = new ArrayList<ForEachConditionalSkipSet>();

  public EvaluateConditionalSkip(DataModel dataModel) {
    super(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION, dataModel);
    setConditionTableName("Table 4.4 Skip Target Dose Attributes");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caAssessmentDate = new ConditionAttribute<Date>("Processing data", "Assessment Date");
    caAdministeredDoseCount = new ConditionAttribute<Integer>("Patient Immunization History",
        "Administered Dose Count");

    caAssessmentDate.setAssumedValue(new Date());

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caAssessmentDate);
    conditionAttributesList.add(caAdministeredDoseCount);

    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    caAssessmentDate.setInitialValue(dataModel.getAssessmentDate());
    // caAdministeredDoseCount.setInitialValue(dataModel.getAntigenAdministeredRecord().get);


    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    if (seriesDose.getConditionalSkip() != null) {
      log("Conditional skip has been defined, now looking at the details.");
      ConditionalSkip conditionalSkip = seriesDose.getConditionalSkip();
      for (ConditionalSkipSet conditionalSkipSet : conditionalSkip.getConditionalSkipSetList()) {
        ForEachConditionalSkipSet forEachConditionalSkipSet = new ForEachConditionalSkipSet();
        forEachConditionalSkipSetList.add(forEachConditionalSkipSet);
        for (ConditionalSkipCondition condition : conditionalSkipSet.getConditionList()) {
          EvaluateConditionalSkip.ForEachConditionalSkipSet.ForEachConditionInSet f = forEachConditionalSkipSet.new ForEachConditionInSet();
          forEachConditionalSkipSet.forEachConditionInSetList.add(f);
          f.caConditionalSkipBeginAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTSKIP-3)",
              "Conditional Skip Begin Age Date");
          f.caConditionalSkipEndAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTSKIP-4)",
              "Conditional Skip End Age Date");
          f.caConditionalSkipIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTSKIP-5)",
              "Conditional Skip Interval Date");
          f.caConditionalSkipStartDate = new ConditionAttribute<Date>("Supporting Data (Conditional Skip)",
              "Conditional Skip Start Date");
          f.caConditionalSkipEndDate = new ConditionAttribute<Date>("Supporting Data (Conditional Skip)",
              "Conditional SKip End Date");
          f.caConditionalDoseType = new ConditionAttribute<String>("Supporting Data (Conditional Skip)",
              "Conditional Skip Dose Type");
          f.caConditionalSkipDoseCountLogic = new ConditionAttribute<String>("Supporting Data (Conditional Skip)",
              "Conditional Skip Doese Count Logic");
          f.caConditonalSkipDoseCount = new ConditionAttribute<Integer>("Supporting Data (Conditional Skip)",
              "Conditional Skip Dose Count");

          List<ConditionAttribute<?>> caList = new ArrayList<ConditionAttribute<?>>();
          caList.add(f.caConditionalSkipBeginAgeDate);
          caList.add(f.caConditionalSkipEndAgeDate);
          caList.add(f.caConditionalSkipIntervalDate);
          caList.add(f.caConditionalSkipStartDate);
          caList.add(f.caConditionalSkipEndDate);
          caList.add(f.caConditionalDoseType);
          caList.add(f.caConditionalSkipDoseCountLogic);
          caList.add(f.caConditonalSkipDoseCount);
          conditionAttributesAdditionalMap.put("Table 4 - 4 Conditional Skip Attributes "
              + conditionalSkipSet.getSetId() + "." + condition.getConditionId(), caList);

          f.caConditionalSkipBeginAgeDate.setInitialValue(CALCDTSKIP_3.evaluate(dataModel, this, condition));
          f.caConditionalSkipEndAgeDate.setInitialValue(CALCDTSKIP_4.evaluate(dataModel, this, condition));
          f.caConditionalSkipIntervalDate.setInitialValue(CALCDTSKIP_5.evaluate(dataModel, this, condition));

        }
      }
    } else {
      log("No conditional skips are defined. ");
    }
    log("Looking to set Trigger values");
    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_AGE);
    evaluateLogicTables();
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
    out.println(
        "<p>Skip target dose addresses times when a target dose can be skipped. In most settings, this occurs when a patient is behind schedule and the total number of doses needed to satisfy patient series can be reduced. In cases where a target dose does not specify skip target dose attributes, the target dose cannot be skipped.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
    printLog(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(3, 4, "Table 4-5 Can the Target Dose be Skipped?");

      setLogicCondition(0, new LogicCondition("date administered < lot expiration date?") {
        @Override
        public LogicResult evaluateInternal() {
          // if (caDateAdministered.getFinalValue() == null ||
          // caTriggerAgeDate.getFinalValue() == null) {
          // return LogicResult.NO;
          // }
          // if
          // (caDateAdministered.getFinalValue().before(caTriggerAgeDate.getFinalValue()))
          // {
          // return LogicResult.YES;
          // }
          return LogicResult.NO;
        }
      });

      setLogicCondition(1, new LogicCondition("date administered < trigger interval date?") {
        @Override
        public LogicResult evaluateInternal() {
          // if (caDateAdministered.getFinalValue() == null ||
          // caTriggerIntervalDate.getFinalValue() == null) {
          // return LogicResult.NO;
          // }
          // if
          // (caDateAdministered.getFinalValue().before(caTriggerIntervalDate.getFinalValue()))
          // {
          // return LogicResult.YES;
          // }
          return LogicResult.NO;
        }
      });

      setLogicCondition(2, new LogicCondition("Is the trigger target dose status \"satisfied\"?") {
        @Override
        public LogicResult evaluateInternal() {
          // if (caTargetDose.getFinalValue() != null
          // && caTargetDose.getFinalValue().getTargetDoseStatus() ==
          // TargetDoseStatus.SATISFIED) {
          // return LogicResult.YES;
          // }
          return LogicResult.NO;
        }
      });

      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.ANY);
      setLogicResults(1, LogicResult.ANY, LogicResult.YES, LogicResult.NO, LogicResult.ANY);
      setLogicResults(2, LogicResult.NO, LogicResult.NO, LogicResult.ANY, LogicResult.YES);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The target dose cannot be skipped. ");
          log("Setting next step: 4.3 Evaluate Age");
          setNextLogicStepType(LogicStepType.EVALUATE_AGE);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The target dose cannot be skipped. ");
          log("Setting next step: 4.3 Evaluate Age");
          setNextLogicStepType(LogicStepType.EVALUATE_AGE);
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The target dose can be skipped.");
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SKIPPED);
          log("The target dose status is \"skipped\"");
          log("Setting next step: Evaluate Immunization History");
          TargetDose targetDoseNext = dataModel.findNextTargetDose(dataModel.getTargetDose());
          dataModel.setTargetDose(targetDoseNext);
          setNextLogicStepType(LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION);
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The target dose can be skipped.");
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SKIPPED);
          log("The taget dose status is \"skipped\"");
          log("Setting next step: Evaluate Immunization History");
          TargetDose targetDoseNext = dataModel.findNextTargetDose(dataModel.getTargetDose());
          dataModel.setTargetDose(targetDoseNext);
          setNextLogicStepType(LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION);
        }
      });

    }
  }

}
