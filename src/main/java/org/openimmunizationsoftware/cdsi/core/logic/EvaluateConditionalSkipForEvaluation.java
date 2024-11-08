package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;

public class EvaluateConditionalSkipForEvaluation extends EvaluateConditionalSkip {

  // Creating null attributes for use later
  protected ConditionAttribute<Date> caDateAdministered = null;
  protected ConditionAttribute<Integer> caAdministeredDoseCount = null;
  protected ConditionAttribute<Date> caAssessmentDate = null;
  protected ConditionAttribute<Date> caEarliestDate = null;

  protected boolean isForecast;
  protected boolean isValidating;

  protected LogicStepType noSkipLogicStep;
  protected LogicStepType skipLogicStep;

  // Constructor 1
  protected EvaluateConditionalSkipForEvaluation(DataModel dataModel) {
    super(dataModel, ConditionalSkipType.EVALUATE, LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION,
        LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE, LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
    setConditionTableName("Table 6.4 Conditional Skip Attributes");
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
        "<li>Catch-up doses where the patient is current with their administrations and does not need to catch-up</lui>");
    out.println(
        "<li>The patient is behind schedule and the total number of doses needed to satisfy the patient series can be reduced</lui>");
    out.println(
        "<li>The previously administered dose(s) negates the need for the current target dose</lui>");
    out.println("</ul>");

    out.println(
        "<p>In cases where a target dose does not specify Conditional Skip attributes, the target dose cannot be skipped.</p>");
    out.println(
        "<p>A dose may be skipped based on whether or not one or more conditions evaluates to true. Conditions are classified as one of a number of types, each with one or more parameters in the Supporting Data. Conditions are contained within sets. Each set contains one or more conditions to be evaluated. Within a set, one or more conditions must be met for the set to be met. In the case where a set contains multiple conditions, whether all conditions or just one condition must be met is specified by the Condition Logic (e.g., AND vs. OR). Similarly, a dose may contain multiple sets. In the case where a dose contains multiple sets, whether all sets or just one set must be met is specified by the Set Logic.</p>");
    out.println(
        "<p>Finally, in an effort to reduce page size and eliminate duplicate logic which could result in typographical and consistency errors, this section of logic is defined here once, but used in both Evaluation and Forecasting. The forecasting chapter refers the reader back to this section for appropriate logic.</p>");

    out.println("<img src=\"Figure 6.3.png\"/>");
    out.println("<p>FIGURE 6 - 3 CONDITIONAL SKIP PROCESS MODEL</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

}
