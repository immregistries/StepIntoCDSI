package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class SkipTargetDoseForForecast extends LogicStep
{
  
  public SkipTargetDoseForForecast(DataModel dataModel)
  {
    super(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST);
    return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println("<h1>5.1 Evaluate Conditional Skip</h1>");

  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    out.println("<h1>5.1 Evaluate Conditional Skip</h1>");

  }

}
