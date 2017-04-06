package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.EXTRANEOUS;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class SatisfyTargetDose extends LogicStep {

  public SatisfyTargetDose(DataModel dataModel) {
    super(LogicStepType.SATISFY_TARGET_DOSE, dataModel);
    setConditionTableName("Table ");

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  // :P
  @Override
  public LogicStep process() throws Exception {

    /***
     * Bypassing "4 Evaluate Vaccine Dose Administrated"
     * setNextLogicStepType(LogicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED);
     */
    setNextLogicStepType(logicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED);
    evaluateLogicTables();
    dataModel.getTargetDose().setStatusCause("");
    return next(true);
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
        "<p>Satisfy  target  dose  uses  the  results  from  the  previous  evaluation  sections  as  conditions  to  determine  if the target dose is satisfied.  </p>");

    out.println(
        "<p>The following processing model and decision table are used to determine if the target dose was satisfied</p>");
    out.println("<img src=\"Figure 4.22.PNG\"/>");
    out.println("<p>FIGURE 4 - 22 SATISFY TARGET DOSE PROCESS MODEL</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(5, 7, "TABLE 4 - 32 WAS THE TARGET DOSE SATISFIED?");

      setLogicCondition(0, new LogicCondition("Was the vaccine dose administered at a valid age?") {
        @Override
        public LogicResult evaluateInternal() {
          AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
          // if (aar.getEvaluation().getEvaluationReason() == EvaluationReason.TOO_OLD) {
          // return NO;
          // }
          if (aar.getEvaluation().getEvaluationStatus() == EvaluationStatus.VALID) {
            return YES;
          } else if (aar.getEvaluation().getEvaluationStatus() == EvaluationStatus.EXTRANEOUS) {
            return EXTRANEOUS;
          }
          return NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Was the vaccine dose administered at a valid or allowable interval?") {
        @Override
        public LogicResult evaluateInternal() {
          if (dataModel.getTargetDose().getStatusCause().contains("Interval")) {
            return NO;
          }
          return YES;
        }
      });


      setLogicCondition(2, new LogicCondition(
          "Was the vaccine dose administered in conflict with any previous live virus vaccine doses administered?") {
        @Override
        public LogicResult evaluateInternal() {
          if (dataModel.getTargetDose().getStatusCause().contains("VirusConflict")) {
            return YES;
          }
          return NO;
        }
      });

      setLogicCondition(3,
          new LogicCondition("Did the patient receive either a preferable or allowable vaccine?") {
            @Override
            public LogicResult evaluateInternal() {
              if (dataModel.getTargetDose().getStatusCause().contains("Vaccine")) {
                return NO;
              }
              return YES;
            }
          });

      setLogicCondition(4,
          new LogicCondition("Is the patient's gender one of the required genders?") {
            @Override
            public LogicResult evaluateInternal() {
              if (dataModel.getTargetDose().getStatusCause().contains("Gender")) {
                return NO;
              }
              return YES;
            }
          });

      setLogicResults(0, LogicResult.YES, LogicResult.EXTRANEOUS, LogicResult.NO, LogicResult.ANY,
          LogicResult.ANY, LogicResult.ANY, LogicResult.ANY);
      setLogicResults(1, LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.NO,
          LogicResult.ANY, LogicResult.ANY, LogicResult.ANY);
      setLogicResults(2, LogicResult.NO, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY,
          LogicResult.YES, LogicResult.ANY, LogicResult.ANY);
      setLogicResults(3, LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY,
          LogicResult.ANY, LogicResult.NO, LogicResult.ANY);
      setLogicResults(4, LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY,
          LogicResult.ANY, LogicResult.ANY, LogicResult.NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SATISFIED);
          dataModel.getTargetDose().setSatisfiedByVaccineDoseAdministered(
              dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered());
          log("Yes. The target dose status is \"satisfied.\" Evaluation status is \"valid\" with possible evaluation reason(s).");
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is \"not satisfied.\" Evaluation status is \"extraneous\" with possible evaluation reason(s).");

        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is \"not satisfied.\" Evaluation status is \"not valid\" with evaluation reason(s).");

        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is \"not satisfied.\" Evaluation status is \"not valid\" with evaluation reason(s).");

        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is \"not satisfied.\" Evaluation status is \"not valid\" with evaluation reason(s). ");

        }
      });

      setLogicOutcome(5, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is \"not satisfied.\" Evaluation status is \"not valid\" with evaluation reason(s).");

        }
      });

      setLogicOutcome(6, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is \"not satisfied.\" Evaluation status is \"not valid\" with evaluation reason(s).");

        }
      });
      // setLogicCondition(0, new LogicCondition("date administered > lot
      // expiration date?") {
      // @Override
      // public LogicResult evaluateInternal() {
      // if (caDateAdministered.getFinalValue() == null ||
      // caTriggerAgeDate.getFinalValue() == null) {
      // return LogicResult.NO;
      // }
      // if
      // (caDateAdministered.getFinalValue().before(caTriggerAgeDate.getFinalValue()))
      // {
      // return LogicResult.YES;
      // }
      // return LogicResult.NO;
      // }
      // });

      // setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO,
      // LogicResult.ANY);

      // setLogicOutcome(0, new LogicOutcome() {
      // @Override
      // public void perform() {
      // log("No. The target dose cannot be skipped. ");
      // log("Setting next step: 4.3 Substitute Target Dose");
      // setNextLogicStep(LogicStep.SUBSTITUTE_TARGET_DOSE_FOR_EVALUATION);
      // }
      // });
      //
    }
  }

}
