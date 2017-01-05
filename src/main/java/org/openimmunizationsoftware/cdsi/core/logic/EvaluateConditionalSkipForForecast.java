package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class EvaluateConditionalSkipForForecast extends EvaluateConditionalSkipForEvaluation {

  public EvaluateConditionalSkipForForecast(DataModel dataModel) {
    super(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, dataModel);
    setConditionTableName("Table 4.4 Skip Target Dose Attributes");
    setupInternal(dataModel, LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY, LogicStepType.FORECAST_DATES_AND_REASONS);
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


}