package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class SelectBestPatientSeries extends LogicStep
{
  public SelectBestPatientSeries(DataModel dataModel) {
    super(LogicStepType.SELECT_BEST_PATIENT_SERIES, dataModel);
  }
  
  @Override
  public LogicStep process() {
    setNextLogicStepType(LogicStepType.ONE_BEST_PATIENT_SERIES);
    return next();
  }
  
  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println("<h1>8.5 Select Best Patient Series</h1>");
    
  }

  @Override
  public void printPost(PrintWriter out) {
    out.println("<h1>8.5 Select Best Patient Series</h1>");

  }

}
