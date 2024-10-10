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
    setConditionTableName("Table 6.10");

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
    out.println("<img src=\"Figure 6.23.PNG\"/>");
    out.println("<p>FIGURE 6 - 23 SATISFY TARGET DOSE PROCESS MODEL</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(5, 7, "TABLE 6 - 31 WAS THE TARGET DOSE SATISFIED?");

      setLogicCondition(0, new LogicCondition("Was the vaccine dose administered at a valid age for the target dose?") {
        @Override
        public LogicResult evaluateInternal() {
          AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
          if (aar.getEvaluation().getEvaluationStatus() == EvaluationStatus.VALID) {
            return YES;
          } else if (aar.getEvaluation().getEvaluationStatus() == EvaluationStatus.EXTRANEOUS) {
            return EXTRANEOUS;
          }
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
          new LogicCondition("Was the vaccine dose administered a preferable vaccine or an allowable vaccine for the target dose?") {
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
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SATISFIED);
          dataModel.getTargetDose().setSatisfiedByVaccineDoseAdministered(
              dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered());
          log("Yes. The target dose status is 'Satisfied'. Evaluation status is 'Valid' with evaluation reasons.");
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is 'Not Satisfied'. Evaluation status is 'Extraneous' with evaluation reasons.");

        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is 'Not Satisfied'. Evaluation status is 'Not Valid' with evaluation reasons.");

        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is 'Not Satisfied'. Evaluation status is 'Not Valid' with evaluation reasons.");

        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is 'Not Satisfied'. Evaluation status is 'Not Valid' with evaluation reasons. ");

        }
      });

      setLogicOutcome(5, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("No. The target dose status is 'Not Satisfied'. Evaluation status is 'Not Valid' with evaluation reasons.");

        }
      });
    }
  }

}
