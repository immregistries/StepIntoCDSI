package org.immregistries.step.core.logic;

import java.io.PrintWriter;
import org.immregistries.step.core.data.DataModel;

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
    // out.println("<h1>5.1 Evaluate Conditional Skip</h1>");
    printStandard(out);

  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
    // out.println("<h1>5.1 Evaluate Conditional Skip</h1>");

  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + getTitle() + "</h1>");
    out.println(
        "<p>Evaluate Conditional Skip  addresses times when  a  target dose can be skipped.  A dose should be considered necessary unless it is determined that it can be skipped</p>");
  }

}
