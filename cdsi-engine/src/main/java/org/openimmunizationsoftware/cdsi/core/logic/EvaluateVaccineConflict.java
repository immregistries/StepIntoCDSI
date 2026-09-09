package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateVaccineConflict extends LogicStep {

  private ConditionAttribute<Date> caDateAdministered = null;

  private ConditionAttribute<VaccineType> caCurrentVaccineType = null;
  private YesNo y = null;

  public EvaluateVaccineConflict(DataModel dataModel) {
    super(LogicStepType.EVALUATE_VACCINE_CONFLICT, dataModel);
    setConditionTableName("Table ");
    // change chapter to be more Business rule based to match the 4.5 document

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    // caConflictBeginIntervalDate = new ConditionAttribute<Date>("Calculated date
    // (CALCDTLIVE-1)",
    // "Conflict Begin Interval Date");
    // caConflictEndIntervalDate = new ConditionAttribute<Date>("Calculated
    // date(CALCDTLIVE-2 &
    // CALCDTLIVE-3",
    // "Conflict End Interval Date");
    caCurrentVaccineType = new ConditionAttribute<VaccineType>(
        "Vaccine dose administered", "Vaccine Type");

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caCurrentVaccineType);
    // conditionAttributesList.add(caPreviousVaccineType);

    caDateAdministered
        .setInitialValue(dataModel.getAntigenAdministeredRecord().getDateAdministered());
    // if (dataModel.getAntigenAdministeredRecord().getVaccineType()!= null)
    caCurrentVaccineType.setInitialValue(dataModel.getAntigenAdministeredRecord().getVaccineType());

    LT420 logicTable = new LT420();
    // logicTableList.add(logicTable);
    logicTable.setLogicStepSink(this.getLogicStepSink());
    logicTableList.add(logicTable);
    logicTable.evaluate();
    y = YesNo.NO;
    if (logicTable.getY420() == YesNo.YES) {
      // CALCDTCONFLICT-1/2 both name "the previous vaccine dose administered" -
      // scan the doses administered before this one (4.2 sorts the list
      // ascending by date), not after it.
      for (int i = 0; i < dataModel.getSelectedAntigenAdministeredRecordPos(); i++) {
        AntigenAdministeredRecord vaccineAdministered = dataModel.getSelectedAntigenAdministeredRecordList().get(i);
        LT421 logicTab = new LT421();
        logicTab.caPreviousVaccineType = new ConditionAttribute<VaccineType>(
            "Supporting Data (Live Virus Conflict)", "Previous Vaccine Type");
        logicTab.caPreviousVaccineType.setInitialValue(vaccineAdministered.getVaccineType());
        conditionAttributesList.add(logicTab.caPreviousVaccineType);
        logicTab.setLogicStepSink(this.getLogicStepSink());
        logicTableList.add(logicTab);
        logicTab.evaluate();
        if (logicTab.y421 == YesNo.YES) {
          LT422 lt = new LT422();
          lt.caConflictBeginIntervalDate = new ConditionAttribute<Date>(
              "Calculated date (CALCDTLIVE-1)", "Conflict Begin Interval Date");
          lt.caConflictEndIntervalDate = new ConditionAttribute<Date>(
              "Calculated date(CALCDTLIVE-2 & CALCDTLIVE-3)", "Conflict End Interval Date");

          conditionAttributesList.add(lt.caConflictBeginIntervalDate);
          conditionAttributesList.add(lt.caConflictEndIntervalDate);

          lt.setIntervalDate(vaccineAdministered);
          lt.setLogicStepSink(this.getLogicStepSink());
          logicTableList.add(lt);
          lt.evaluate();
          if (lt.y422 == YesNo.YES) {
            y = YesNo.YES;
          }
        }
      }
    }
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_FOR_PREFERABLE_VACCINE);
    evaluateLogicTables();
    if (y == YesNo.YES) {
      log(LogLevel.CONTROL, "LIVE VIRUS CONFLICT DETECTED - between current and previous vaccine doses");
      dataModel.getTargetDose()
          .setStatusCause(dataModel.getTargetDose().getStatusCause() + "VirusConflict");
    } else {
      log(LogLevel.STATE, "No live virus conflict detected");
    }
    return next();
  }

  private class LT420 extends LogicTable {
    private YesNo y420 = YesNo.NO;

    public LT420() {
      super(2, 3,
          "Table 4-20 Should the current vaccine dose administrated be evaluted for a live virus conflict ? ");

      setLogicCondition(0, new LogicCondition(
          "Is the current vaccine type of the vaccine dose administered one of the supporting data defined live virus conflict current vaccine types?") {
        @Override
        protected LogicResult evaluateInternal() {
          for (LiveVirusConflict liveVirusConflict : dataModel.getLiveVirusConflictList()) {
            if (liveVirusConflict.getCurrentVaccineType()
                .equals(caCurrentVaccineType.getFinalValue())) {
              return YES;
            }
          }
          return NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Is there at least one vaccine dose administered on or before the current vaccine dose administered date?") {
        @Override
        protected LogicResult evaluateInternal() {
          for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {

            if (!aar.getDateAdministered().after(caDateAdministered.getFinalValue())) {
              return YES;
            }
          }
          return NO;
        }
      });

      setLogicResults(0, YES, NO, ANY);
      setLogicResults(1, YES, ANY, NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The vaccine dose administered should be evaluated for a live virus conflict");
          setY420(YesNo.YES);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The vaccine dose administered should not be evaluated for a live virus conflict.");
          setY420(YesNo.NO);
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The vaccine dose administered should not be evaluated for a live virus conflict "
              + "- there is no vaccine dose administered on or before the current one to conflict with.");
        }
      });
    }

    public YesNo getY420() {
      return y420;
    }

    public void setY420(YesNo y420) {
      this.y420 = y420;
    }
  }

  private class LT421 extends LogicTable {
    private ConditionAttribute<VaccineType> caPreviousVaccineType = null;
    private YesNo y421 = null;

    public LT421() {
      super(1, 2, "Table 4-21 Could the two vaccine doses administrated be in conflict?");

      setLogicCondition(0, new LogicCondition(
          "Is the vaccine type of the previous vaccine dose administered the same as one of the supporting data defined live virus conflict previous vaccine types when the current vaccine dose administered type is same as the live virus conflict current vaccine type ?") {
        @Override
        protected LogicResult evaluateInternal() {
          for (LiveVirusConflict lvc : dataModel.getLiveVirusConflictList()) {
            if (lvc.getPreviousVaccineType().equals(caPreviousVaccineType.getFinalValue())) {
              if (lvc.getCurrentVaccineType().equals(caCurrentVaccineType.getFinalValue())) {
                return YES;
              }
            }
          }
          return NO;
        }
      });

      setLogicResults(0, YES, NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The two doses must be checked for a live virus conflict");
          y421 = YesNo.YES;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The two doses need not be checked for a live virus conflict");
          y421 = YesNo.NO;
        }
      });

    }

  }

  private class LT422 extends LogicTable {
    private ConditionAttribute<Date> caConflictBeginIntervalDate = null;
    private ConditionAttribute<Date> caConflictEndIntervalDate = null;
    private YesNo y422 = null;

    public void setIntervalDate(AntigenAdministeredRecord vaccineAdministered) {
      for (LiveVirusConflict liveVirusConflict : dataModel.getLiveVirusConflictList()) {
        // Match on both the impacted (current) and conflicting (previous) vaccine
        // type, the same pair LT421 already matched on - an impacted type can name
        // more than one conflicting type, each with its own intervals, so matching
        // on the impacted type alone lets the last such entry silently win.
        if (liveVirusConflict.getCurrentVaccineType().equals(caCurrentVaccineType.getFinalValue())
            && liveVirusConflict.getPreviousVaccineType().equals(vaccineAdministered.getVaccineType())) {
          Date previousDateAdministered = vaccineAdministered.getDateAdministered();
          Date beginIntervalDate = liveVirusConflict.getConflictBeginInterval()
              .getDateFrom(previousDateAdministered);
          // CALCDTCONFLICT-2: the minimum conflict end interval when the previous dose
          // has no evaluation status or one of 'Valid'; the conflict end interval when
          // it has an evaluation status that is not 'Valid'.
          Date endIntervalDate = isPreviousDoseValidOrUnevaluated(vaccineAdministered)
              ? liveVirusConflict.getMinimalConflictEndInterval().getDateFrom(previousDateAdministered)
              : liveVirusConflict.getConflictEndInterval().getDateFrom(previousDateAdministered);
          caConflictBeginIntervalDate.setInitialValue(beginIntervalDate);
          caConflictEndIntervalDate.setInitialValue(endIntervalDate);
        }
      }
    }

    private boolean isPreviousDoseValidOrUnevaluated(AntigenAdministeredRecord vaccineAdministered) {
      TargetDose evaluatedAgainst = vaccineAdministered.getVaccineDoseAdministered().getEvaluatedAgainstTargetDose();
      if (evaluatedAgainst == null || evaluatedAgainst.getEvaluation() == null) {
        return true;
      }
      EvaluationStatus status = evaluatedAgainst.getEvaluation().getEvaluationStatus();
      return status == null || status == EvaluationStatus.VALID;
    }

    public LT422() {
      super(1, 2,
          "Table 4-22 Is the current vaccine dose administrated in conflict with previous vaccine dose administrated ?");

      setLogicCondition(0, new LogicCondition(
          "Is the conflict begin interval date <= current date administered < conflict end interval date?") {

        @Override
        protected LogicResult evaluateInternal() {
          if (!caConflictBeginIntervalDate.getFinalValue()
              .after(caDateAdministered.getFinalValue())
              && caDateAdministered.getFinalValue()
                  .before(caConflictEndIntervalDate.getFinalValue())) {
            return YES;
          }

          return NO;
        }
      });

      setLogicResults(0, YES, NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The vaccine dose administered is in conflict with a previous vaccine dose administered");
          y422 = YesNo.YES;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The vaccine dose administered is not in conflict with a previous vaccine dose administered.");
          y422 = YesNo.NO;
        }
      });
    }
  }
}
