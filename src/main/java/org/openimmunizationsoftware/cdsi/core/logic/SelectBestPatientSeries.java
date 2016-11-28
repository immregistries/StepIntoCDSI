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
    //out.println("<h1>8.5 Select Best Patient Series</h1>");
	  printStandard(out);
    
  }

  @Override
  public void printPost(PrintWriter out) {
    //out.println("<h1>8.5 Select Best Patient Series</h1>");
	  printStandard(out);

  }
  
  private void printStandard(PrintWriter out) {
	    out.println("<h1> " + getTitle() + "</h1>");
	    out.println("<p>Select Best  Patient Series  determines the best path to immunity (patient series) for the patient based on the evaluated immunization history and forecast. Each antigen evaluated and forecasted may contain more than one  patient series  and the goal  of  select best  patient series  is to select one of those  patient series  as being superior  to  the  others  based  on  several  factors.   </p>");
	  }

}
