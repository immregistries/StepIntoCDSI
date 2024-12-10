package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class SkipTargetDoseForForecast extends LogicStep {

  public SkipTargetDoseForForecast(DataModel dataModel) {
    super(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY);
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
    out.println(
        "<p>Evaluate Conditional Skip  addresses times when  a  target dose can be skipped.  A dose should be considered necessary unless it is determined that it can be skipped</p>");
  }

}
