package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class EvaluateConditionalSkipForForecast extends EvaluateConditionalSkipForEvaluation {

  public EvaluateConditionalSkipForForecast(DataModel dataModel) {

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
    out.println("<h1> " + logicStepType.getDisplay() + "</h1>");
    out.println(
        "<p>Evaluate Conditional Skip addresses times when a target dose can be skipped. A dose should be considered necessary unless it is determined that it can be skipped. The most common scenarios for skipping a dose are:</p>");
    out.println("<ul>");
    out.println(
        "    <li>Catch-up doses where the patient is current with their administrations and does not need to catch-up</li>");
    out.println(
        "    <li>The patient is behind schedule and the total number of doses needed to satisfy the patient series can be reuced</li>");
    out.println(
        "    <li>The previously administered dose(s) negates the need for the current target dose</li>");
    out.println("</ul>");
    out.println(
        "<p>In cases where a target dose does not specify Conditional Skip attributes, the target dose cannot be skipped.</p>");
    out.println(
        "<p>The process model, attribute table, and decision table are used to determine if the target dose can be skipped is the same as described in Chapter 4.2.</p>");
  }


}
