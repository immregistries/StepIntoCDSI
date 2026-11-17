package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.EXTRANEOUS;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.ForecastInput;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

import java.util.List;

public class SatisfyTargetDose extends LogicStep {

  public SatisfyTargetDose(DataModel dataModel) {
    super(LogicStepType.SATISFY_TARGET_DOSE, dataModel);
    setConditionTableName("Table ");

    LT logicTable = new LT();
    logicTable.setLogicStepSink(this.getLogicStepSink());
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
    evaluateLogicTables();
    TargetDoseStatus status = dataModel.getTargetDose().getTargetDoseStatus();
    if (status == TargetDoseStatus.SATISFIED) {
      log(LogLevel.CONTROL, "TARGET DOSE SATISFIED - All conditions met");
      // PR: track the original VaccinationInput that satisfied this target dose,
      // so callers can retrieve satisfied inputs after the engine run.
      VaccineDoseAdministered vda = dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered();
      ForecastInput.VaccinationInput vi = dataModel.getVaccinationInputMap().get(vda);
      // PR: detect copies by comparing every field (not identity/equals), so a
      // duplicate VaccinationInput with identical values is not added twice.
      if (vi != null && !containsVaccinationInput(dataModel.getSatisfiedVaccinationInputList(), vi)) {
        dataModel.getSatisfiedVaccinationInputList().add(vi);
      }
    } else if (status == TargetDoseStatus.NOT_SATISFIED) {
      log(LogLevel.CONTROL, "TARGET DOSE NOT SATISFIED - Requirements not met");
      // PR: track the original VaccinationInput that did not satisfy this target dose.
      VaccineDoseAdministered vda = dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered();
      ForecastInput.VaccinationInput vi = dataModel.getVaccinationInputMap().get(vda);
      // PR: detect copies by comparing every field (not identity/equals), so a
      // duplicate VaccinationInput with identical values is not added twice.
      if (vi != null && !containsVaccinationInput(dataModel.getUnsatisfiedVaccinationInputList(), vi)) {
        dataModel.getUnsatisfiedVaccinationInputList().add(vi);
      }
    } else if (status == TargetDoseStatus.SKIPPED) {
      log(LogLevel.CONTROL, "TARGET DOSE SKIPPED - Conditional skip applied");
    }
    dataModel.getTargetDose().setStatusCause("");
    return next(true);
  }

  // PR: field-by-field comparison so value-equal copies are detected (default
  // equals is identity-based and would miss duplicates with identical fields).
  private boolean containsVaccinationInput(
      List<ForecastInput.VaccinationInput> list, ForecastInput.VaccinationInput vi) {
    for (ForecastInput.VaccinationInput existing : list) {
      if (existing == vi) {
        return true;
      }
      if (existing == null || vi == null) {
        continue;
      }
      if (eq(existing.getDateAdministered(), vi.getDateAdministered())
          && eq(existing.getVaccineCvx(), vi.getVaccineCvx())
          && eq(existing.getVaccineMvx(), vi.getVaccineMvx())
          && eq(existing.getDoseCondition(), vi.getDoseCondition())) {
        return true;
      }
    }
    return false;
  }

  private boolean eq(Object a, Object b) {
    return (a == null) ? (b == null) : a.equals(b);
  }

  private class LT extends LogicTable {
    public LT() {
      super(4, 6, "TABLE 6 - 31 WAS THE TARGET DOSE SATISFIED?");

      setLogicCondition(0, new LogicCondition("Was the vaccine dose administered at a valid age for the target dose?") {
        @Override
        public LogicResult evaluateInternal() {
          TargetDose targetDose = dataModel.getTargetDose();
          log("Evaluating if the vaccine dose administered at a valid age for the target dose");
          if (targetDose.getEvaluation() == null) {
            log(" - NO, evaluation was null");
            return NO;
          }
          if (targetDose.getEvaluation().getEvaluationStatus() == EvaluationStatus.VALID) {
            log(" - Yes, evaluation was valid");
            return YES;
          } else if (targetDose.getEvaluation().getEvaluationStatus() == EvaluationStatus.EXTRANEOUS) {
            log(" - No, evaluation was extraneous");
            return EXTRANEOUS;
          }
          log(" - No, evaluation was " + targetDose.getEvaluation().getEvaluationStatus());
          return NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Did the vaccine dose administered satisfy all preferable intervals or all allowable intervals for the target dose?") {
        @Override
        public LogicResult evaluateInternal() {
          if (dataModel.getTargetDose().getStatusCause().contains("Interval")) {
            return NO;
          }
          return YES;
        }
      });

      setLogicCondition(2, new LogicCondition(
          "Is the current vaccine dose administered an impacted vaccine dose administered?") {
        @Override
        public LogicResult evaluateInternal() {
          if (dataModel.getTargetDose().getStatusCause().contains("VirusConflict")) {
            return YES;
          }
          return NO;
        }
      });

      setLogicCondition(3,
          new LogicCondition(
              "Was the vaccine dose administered a preferable vaccine or an allowable vaccine for the target dose?") {
            @Override
            public LogicResult evaluateInternal() {
              if (dataModel.getTargetDose().getStatusCause().contains("Vaccine")) {
                return NO;
              }
              return YES;
            }
          });

      setLogicResults(0, LogicResult.YES, LogicResult.EXTRANEOUS, LogicResult.NO, LogicResult.ANY,
          LogicResult.ANY, LogicResult.ANY);
      setLogicResults(1, LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.NO,
          LogicResult.ANY, LogicResult.ANY);
      setLogicResults(2, LogicResult.NO, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY,
          LogicResult.YES, LogicResult.ANY);
      setLogicResults(3, LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY,
          LogicResult.ANY, LogicResult.NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SATISFIED);
          log("Yes. The target dose status is 'Satisfied'. Evaluation status is 'Valid'.");
          dataModel.getTargetDose().setSatisfiedByVaccineDoseAdministered(
              dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered());
          dataModel.getTargetDose().getEvaluation().setEvaluationStatus(EvaluationStatus.VALID);
          // setting the target dose of the VDA to this target dose, so that the target
          // dose can be accessed through the VDA
          dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered()
              .setTargetDose(dataModel.getTargetDose());
          dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered()
              .setEvaluatedAgainstTargetDose(dataModel.getTargetDose());
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is 'Not Satisfied'. Evaluation status is 'Extraneous' with evaluation reasons.");
          dataModel.getTargetDose().getEvaluation().setEvaluationStatus(EvaluationStatus.EXTRANEOUS);
          dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered()
              .setEvaluatedAgainstTargetDose(dataModel.getTargetDose());
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is 'Not Satisfied'. Evaluation status is 'Not Valid' with evaluation reasons.");
          dataModel.getTargetDose().getEvaluation().setEvaluationStatus(EvaluationStatus.NOT_VALID);
          dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered()
              .setEvaluatedAgainstTargetDose(dataModel.getTargetDose());
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is 'Not Satisfied'. Evaluation status is 'Not Valid' with evaluation reasons.");
          dataModel.getTargetDose().getEvaluation().setEvaluationStatus(EvaluationStatus.NOT_VALID);
          dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered()
              .setEvaluatedAgainstTargetDose(dataModel.getTargetDose());
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is 'Not Satisfied'. Evaluation status is 'Not Valid' with evaluation reasons. ");
          dataModel.getTargetDose().getEvaluation().setEvaluationStatus(EvaluationStatus.NOT_VALID);
          dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered()
              .setEvaluatedAgainstTargetDose(dataModel.getTargetDose());
        }
      });

      setLogicOutcome(5, new LogicOutcome() {
        @Override
        public void perform() {
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is 'Not Satisfied'. Evaluation status is 'Not Valid' with evaluation reasons.");
          dataModel.getTargetDose().getEvaluation().setEvaluationStatus(EvaluationStatus.NOT_VALID);
          dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered()
              .setEvaluatedAgainstTargetDose(dataModel.getTargetDose());
        }
      });
    }
  }
}
