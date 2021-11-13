package org.immregistries.step.core.logic;

import java.io.PrintWriter;
import org.immregistries.step.core.data.DataModel;

public class EvaluateAndForecastAllPatientSeries extends LogicStep {
  public EvaluateAndForecastAllPatientSeries(DataModel dataModel) {
    super(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() {
    return LogicStepFactory.createLogicStep(LogicStepType.FOR_EACH_PATIENT_SERIES, dataModel);
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println("<h1>4.4 Evaluate and Forecast all Patient Series</h1>");

    out.println(
        "<p>This step is the core of the business logic and decision points many people think of when describing "
            + "evaluation and forecasting. In the Logic Specification, this step contains all of the clinical business "
            + "rules and decision logic in the form of business rules and decision tables.</p>");
    out.println(
        "<p>At the end of this step, each patient series will have an evaluated history and a forecast.</p>");

  }

  @Override
  public void printPost(PrintWriter out) {
    out.println("<h1>4.4 Evaluate and Forecast all Patient Series</h1>");

    out.println(
        "<p>This step is the core of the business logic and decision points many people think of when describing "
            + "evaluation and forecasting. In the Logic Specification, this step contains all of the clinical business "
            + "rules and decision logic in the form of business rules and decision tables.</p>");
    out.println(
        "<p>At the end of this step, each patient series will have an evaluated history and a forecast.</p>");

  }

}
