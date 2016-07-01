package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTSKIP_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTSKIP_2;

import java.io.PrintWriter;

import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.SkipTargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateConditionalSkip extends LogicStep
{

  private ConditionAttribute<Date> caDateAdministered = null;
  private ConditionAttribute<Date> caAssessmentDate = null;
  private ConditionAttribute<Integer> caAdministeredDoseCount = null;
  private ConditionAttribute<Date> caConditionalSkipBeginAgeDate = null;
  private ConditionAttribute<Date> caConditionalSkipEndAgeDate = null;
  private ConditionAttribute<Date> caConditionalSkipIntervalDate= null;
  private ConditionAttribute<Date> caConditionalSkipStartDate = null;
  private ConditionAttribute<Date> caConditionalSkipEndDate = null;
  private ConditionAttribute<String> caConditionalDoseType = null;
  private ConditionAttribute<String> caConditionalSkipDoseCountLogic = null;
  private ConditionAttribute<Integer> caConditonalSkipDoseCount = null;
 

  public EvaluateConditionalSkip(DataModel dataModel) {
    super(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION, dataModel);
    setConditionTableName("Table 4.4 Skip Target Dose Attributes");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caAssessmentDate = new ConditionAttribute<Date>("Processing data", "Assessment Date");
    caAdministeredDoseCount = new ConditionAttribute<Integer>("Patient Immunization History", "Administered Dose Count");
    caConditionalSkipBeginAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTSKIP-3)","Conditional Skip Begin Age Date");
    caConditionalSkipEndAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTSKIP-4)","Conditional Skip End Age Date");
    caConditionalSkipIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTSKIP-5)" , "Conditional Skip Interval Date");
    caConditionalSkipStartDate = new ConditionAttribute<Date>("Supporting Data (Conditional Skip)" , "Conditional Skip Start Date");
    caConditionalSkipEndDate = new ConditionAttribute<Date>("Supporting Data (Conditional Skip)" , "Conditional SKip End Date");
    caConditionalDoseType = new ConditionAttribute<String>("Supporting Data (Conditional Skip)" , "Conditional Skip Dose Type");
    caConditionalSkipDoseCountLogic = new ConditionAttribute<String>("Supporting Data (Conditional Skip)" , "Conditional Skip Doese Count Logic");
    caConditonalSkipDoseCount = new ConditionAttribute<Integer>("Supporting Data (Conditional Skip)" , "Conditional Skip Dose Count");

    caAssessmentDate.setAssumedValue(new Date());

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caAssessmentDate);
    conditionAttributesList.add(caAdministeredDoseCount);
    conditionAttributesList.add(caConditionalSkipBeginAgeDate);
    conditionAttributesList.add(caConditionalSkipEndAgeDate);
    conditionAttributesList.add(caConditionalSkipIntervalDate);
    conditionAttributesList.add(caConditionalSkipStartDate);
    conditionAttributesList.add(caConditionalSkipEndDate);
    conditionAttributesList.add(caConditionalDoseType);
    conditionAttributesList.add(caConditionalSkipDoseCountLogic);
    conditionAttributesList.add(caConditonalSkipDoseCount);
    
    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    caAssessmentDate.setInitialValue(dataModel.getAssessmentDate());
    // caAdministeredDoseCount.setInitialValue(dataModel.getAntigenAdministeredRecord().get);
    
    //caTriggerAgeDate.setInitialValue(CALCDTSKIP_1.evaluate(dataModel, this));
//    caTriggerIntervalDate.setInitialValue(CALCDTSKIP_2.evaluate(dataModel, this));
    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    log("Looking to set Trigger values");
//    if (seriesDose.getSkipTargetDoseList().size() > 0) {
//      log("  + skip target dose list has entries");
//      SkipTargetDose skipTargetDose = seriesDose.getSkipTargetDoseList().get(0);
//      // caTriggerIntervalDate.setInitialValue(initialValue);
//      if (skipTargetDose.getTriggerSeriesDose() != null) {
//        log("  + trigger dose is specified, looking for dose " + skipTargetDose.getTriggerSeriesDose().getDoseNumber());
//        for (TargetDose targetDose : dataModel.getTargetDoseList()) {
//          if (targetDose.getTrackedSeriesDose() == skipTargetDose.getTriggerSeriesDose()) {
//            skipTargetDose.setTriggerTargetDose(targetDose);
//            log("  + found trigger target dose ");
//          }
//        }
//      }
////      caTargetDose.setInitialValue(skipTargetDose.getTriggerTargetDose());
//    }
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
    out.println("<p>Skip target dose addresses times when a target dose can be skipped. In most settings, this occurs when a patient is behind schedule and the total number of doses needed to satisfy patient series can be reduced. In cases where a target dose does not specify skip target dose attributes, the target dose cannot be skipped.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
    printLog(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(3, 4, "Table 4-5 Can the Target Dose be Skipped?");

      setLogicCondition(0, new LogicCondition("date administered < lot expiration date?") {
        @Override
        public LogicResult evaluateInternal() {
//          if (caDateAdministered.getFinalValue() == null || caTriggerAgeDate.getFinalValue() == null) {
//            return LogicResult.NO;
//          }
//          if (caDateAdministered.getFinalValue().before(caTriggerAgeDate.getFinalValue())) {
//            return LogicResult.YES;
//          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(1, new LogicCondition("date administered < trigger interval date?") {
        @Override
        public LogicResult evaluateInternal() {
//          if (caDateAdministered.getFinalValue() == null || caTriggerIntervalDate.getFinalValue() == null) {
//            return LogicResult.NO;
//          }
//          if (caDateAdministered.getFinalValue().before(caTriggerIntervalDate.getFinalValue())) {
//            return LogicResult.YES;
//          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(2, new LogicCondition("Is the trigger target dose status \"satisfied\"?") {
        @Override
        public LogicResult evaluateInternal() {
//          if (caTargetDose.getFinalValue() != null
//              && caTargetDose.getFinalValue().getTargetDoseStatus() == TargetDoseStatus.SATISFIED) {
//            return LogicResult.YES;
//          }
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
