package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateAge extends LogicStep {

  private ConditionAttribute<Date> caDateAdministered = null;
  private ConditionAttribute<Date> caMinimumAgeDate = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<Date> caAbsoluteMinimumAgeDate = null;
  private ConditionAttribute<Date> caDateOfBirth = null;

  public EvaluateAge(DataModel dataModel) {
    super(LogicStepType.EVALUATE_AGE, dataModel);
    setConditionTableName("Table 6-14 Age Attributes");

    caDateOfBirth = new ConditionAttribute<Date>("Patient","Date of birth");
    caDateAdministered =
        new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caMinimumAgeDate = new ConditionAttribute<Date>("Calculated Date", "Minimum Age Date");
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated Date", "Maximum Age Date");
    caAbsoluteMinimumAgeDate =
        new ConditionAttribute<Date>("Calculated Date", "Absolute Minimum Age Date");

    caMaximumAgeDate.setAssumedValue(FUTURE);
    caMinimumAgeDate.setAssumedValue(PAST);
    caAbsoluteMinimumAgeDate.setAssumedValue(PAST);

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caMinimumAgeDate);
    conditionAttributesList.add(caMaximumAgeDate);
    conditionAttributesList.add(caAbsoluteMinimumAgeDate);

    LT logicTable = new LT();
    logicTableList.add(logicTable);

    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    Date dateOfBirth = dataModel.getPatient().getDateOfBirth();
    log("Date of Birth = " + sdf.format(dateOfBirth));
    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    if (seriesDose.getAgeList().size() > 0) {
      Age age = seriesDose.getAgeList().get(0);
      log("Found Age information from series dose, now calculating dates");
      log(" + Absolute minimum age time period = " + age.getAbsoluteMinimumAge());
      log(" + Minimum age time period = " + age.getMinimumAge());
      log(" + Maximum age time period = " + age.getMaximumAge());
      caAbsoluteMinimumAgeDate
          .setInitialValue(age.getAbsoluteMinimumAge().getDateFrom(dateOfBirth));
      caMinimumAgeDate.setInitialValue(age.getMinimumAge().getDateFrom(dateOfBirth));
      if (age.getMaximumAge().isValued()) {
        caMaximumAgeDate.setInitialValue(age.getMaximumAge().getDateFrom(dateOfBirth));
      }
    }
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_INTERVAL);
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
        "<p>Evaluate age validates the age at administration of a vaccine dose administered against a defined age range of a target dose. In cases where a target dose does not specify age attributes, the age at administration is considered �valid.�</p>");
    out.println("<img src=\"Figure 6.5.PNG\"/>");
    out.println("<p>FIGURE 6 - 5 EVALUATE AGE TIMELINE</p>");
    out.println("<img src=\"Figure 6.6.PNG\"/>");
    out.println("<p>FIGURE 6 - 6 EVALUATE AGE PROCESS MODEL</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    seriesDose.toHtml(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(4, 4, "Table 6-15 Was the Vaccine Dose Administered at a Valid Age?");

      setLogicCondition(0, new LogicCondition("Date administered < absolute minimum age date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caDateAdministered.getFinalValue().before(caAbsoluteMinimumAgeDate.getFinalValue())) {
            return YES;
          }
          return NO;
        }
      });

      setLogicCondition(1,
          // Removed check for absolute minimum age date
          new LogicCondition("Date administered < minimum age date?") {
            @Override
            public LogicResult evaluateInternal() {
              if (caDateAdministered.getFinalValue().before(caMinimumAgeDate.getFinalValue())) {
                return YES;
              }
              return NO;
            }
          });

      setLogicCondition(2,
          new LogicCondition("Minimum age date <= date administered < maximum age date?") {
            @Override
            public LogicResult evaluateInternal() {
              if (caDateAdministered.getFinalValue().before(caMinimumAgeDate.getFinalValue())) {
                return NO;
              }
              if (caDateAdministered.getFinalValue().before(caMaximumAgeDate.getFinalValue())) {
                return YES;
              }
              return NO;
            }
          });

      setLogicCondition(3, new LogicCondition("Date administered >= maximum age date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caMaximumAgeDate.getFinalValue().after(caDateAdministered.getFinalValue())) {
            return NO;
          }
          return YES;
        }
      });

      // removed "Is this the first target dose?" check

      // removed "Is the previous vaccine dose administered 'not valid' due to age or interval requirements?" check

      setLogicResults(0, YES, NO, NO, NO);
      setLogicResults(1, NO, YES, NO, NO);
      setLogicResults(2, NO, NO, YES, NO);
      setLogicResults(3, NO, NO, NO, YES);
      //setLogicResults(4, ANY, NO, NO, YES, ANY, ANY);
      //setLogicResults(5, ANY, YES, NO, ANY, ANY, ANY);


      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
          aar.getEvaluation().setEvaluationStatus(EvaluationStatus.NOT_VALID);
          log("No. The vaccine dose was not administered at a valid age. ");
          aar.getEvaluation().setEvaluationReason(EvaluationReason.TOO_YOUNG);
          log("Evaluation reason is \"too young.\"");
        }
      });
      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
          aar.getEvaluation().setEvaluationStatus(EvaluationStatus.VALID);
          log("Yes. The vaccine dose was not administered at a valid age for the target dose. ");
          aar.getEvaluation().setEvaluationReason(EvaluationReason.GRACE_PERIOD);
          log("Evaluation reason is \"Grace period.\"");
        }
      });
      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
          aar.getEvaluation().setEvaluationStatus(EvaluationStatus.VALID);
          log("Yes. The vaccine dose was administered at a valid age for the target dose.");
          // No EvaluationReason is given in the 4.5 table? Leaving it alone for now.
          log("Evaluation reason is \"grace period.\"");
          aar.getEvaluation().setEvaluationReason(EvaluationReason.GRACE_PERIOD);
        }
      });
      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
          aar.getEvaluation().setEvaluationStatus(EvaluationStatus.NOT_VALID);
          log("No. The vaccine dose was administered at a valid age for the target dose.");
          log("It is \"extraneous.\"");
          // There is no EvaluationReason that properly reflects the reason being "it is extraneous." Leaving it alone for now.
          aar.getEvaluation().setEvaluationReason(EvaluationReason.GRACE_PERIOD);
        }
      });
    }
  }

}
