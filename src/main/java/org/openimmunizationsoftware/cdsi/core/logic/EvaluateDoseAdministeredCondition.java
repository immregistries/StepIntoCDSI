package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateDoseAdministeredCondition extends LogicStep {

  private ConditionAttribute<Date> caDateAdministered = null;
  private ConditionAttribute<Date> caLotExpirationDate = null;
  private ConditionAttribute<DoseCondition> caDoseCondition = null;

  public EvaluateDoseAdministeredCondition(DataModel dataModel) {
    super(LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION, dataModel);
    setConditionTableName("Table 4 - 2 Dose Administered Condition Attributes");
    
    // initialize condition attributes
    caDateAdministered =
        new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caDoseCondition =
        new ConditionAttribute<DoseCondition>("Vaccine dose administered", "Dose Condition Flag");
    //use CALCDTLOTEXP-1 business rules now(not implemented yet), change attribute type to "Calculated date"
    caLotExpirationDate =
      new ConditionAttribute<Date>("Calculated date", "Lot Expiration Date");

    // set assumed values, if possible
    caLotExpirationDate.setAssumedValue(FUTURE);

    // set actual values
    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    caDoseCondition.setInitialValue(aar.getDoseCondition());
    caLotExpirationDate.setInitialValue(aar.getLotExpirationDate()); 

    // add to list for display purposes
    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caLotExpirationDate);
    conditionAttributesList.add(caDoseCondition);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() {
    setNextLogicStepType(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST);
    evaluateLogicTables();
    return next();
  }

  private class LT extends LogicTable {
    public LT() {
      super(2, 3, "Table 4 - 3 Can the vaccine dose administered be evaluated?");
      setLogicCondition(0, new LogicCondition("Date administered > lot expiration date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caDateAdministered.getFinalValue() == null
              || caLotExpirationDate.getFinalValue() == null) {
            return LogicResult.NO;
          }
          if (caDateAdministered.getFinalValue().after(caLotExpirationDate.getFinalValue()))
            return LogicResult.YES;
          return LogicResult.NO;
        }
      });
      setLogicCondition(1, new LogicCondition("Is the dose condition flag 'Y'?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caDoseCondition == null) {
            return LogicResult.NO;
          }
          if (caDoseCondition.getFinalValue().equals(YesNo.YES)) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });
      setLogicResults(0, new LogicResult[] {LogicResult.YES, LogicResult.NO, LogicResult.NO});
      setLogicResults(1, new LogicResult[] {LogicResult.ANY, LogicResult.YES, LogicResult.NO});
      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The vaccine dose administered cannot be evaluated. ");
          log("Setting target dose to \"not satisfied\"");
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("Setting evaluation status to \"sub-standard\"");
          log("Setting next step: 6.2 Evaluate Conditional Skip For Evaluation");
          dataModel.setEvaluationStatus(EvaluationStatus.SUB_STANDARD);
          setNextLogicStepType(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION);
        }
      });
      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The vaccine dose administered cannot be evaluated.");
          log("Setting target dose to \"not satisfied\"");
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          log("Setting evaluation status to \"sub-standard\"");
          log("Setting next step:6.2 Evaluate Conditional Skip For Evaluation");
          dataModel.setEvaluationStatus(EvaluationStatus.SUB_STANDARD);
          setNextLogicStepType(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION);
        }
      });
      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The vaccine dose administered can be evaluated.");
          log("Setting next step: 6 Evaluate Vaccine Dose Administered");
          setNextLogicStepType(LogicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED);
        }
      });

    }
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + getTitle() + "</h1>");
    out.println(
        "<p>Target dose : " + dataModel.getTargetDose().getTrackedSeriesDose().getDoseNumber() + " "
            + dataModel.getTargetDose().getTrackedSeriesDose().getAntigenSeries().getSeriesName()
            + " </p>");
    out.println(
        "<p>Dose administered condition checks the dose administered to see if the dose must be repeated regardless of the other evaluation rules.</p>");
    out.println("<p>Relationship to ACIP recommendations:</p>");
    out.println("<ul>");
    out.println(
        "  <li>Doses which were administered after the lot expiration date or which contain a condition do not need to be evaluated.</li>");
    out.println(
        "  <li>Examples of conditions which would prevent evaluation of dose range from misadministration to recalls to cold chain breaks.</li>");
    out.println("</ul>");
    out.println("<img src=\"Figure 4.2.png\"/>");
    out.println("<p>FIGURE 4 - 2 VACCINE DOSE ADMINISTERED CONDITION PROCESS MODEL</p>");


    printConditionAttributesTable(out);
    printLogicTables(out);
  }



  @Override
  public void printPost(PrintWriter out) {
    printStandard(out);

  }
}
