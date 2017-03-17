package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class NoValidDoses extends LogicStep
{


  public NoValidDoses(DataModel dataModel)
  {
    super(LogicStepType.NO_VALID_DOSES, dataModel);
    //setConditionTableName("Table ");
    //System.out.println("-->Patient series size : "+dataModel.getPatientSeriesList().size());
    //System.out.println("--> "+dataModel.getPatientSeriesList().get(0));

  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_BEST_CANDIDATE_PATIENT_SERIES);
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
    out.println("<h1> " + getTitle() + "</h1>");
    out.println("<p>This section  provides the decision table for determining the number of points to assign to a  candidate  patient series when there are no valid doses.</p>");
    printTable(out);
    //printConditionAttributesTable(out);
    //printLogicTables(out);
  }
  
  private void printTable(PrintWriter out){
	  out.println("<table BORDER=\"1\"> ");
	  out.println("  <tr> ");
	  out.println(" <th> Conditions </th> ");
	  out.println(" <th> If this condition is true for the candidate patient series </th> ");
	  out.println(" <th>If this condition is true for two or more candidate patient series </th> ");
	  out.println(" <th>If this condition is not true for the candidate patient serie </th> ");
	  out.println("  </tr> ");
	  out.println("  <tr> ");
	  out.println(" <td >A candidate patient series can start earliest. </th> ");
	  out.println(" <td align=\"center\"> +1</td> ");
	  out.println(" <td align=\"center\"> 0</td> ");
	  out.println(" <td align=\"center\"> -1 </td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println(" <td>A candidate patient series is completable.</th> ");
	  out.println(" <td align=\"center\"> +1</td> ");
	  out.println(" <td align=\"center\"> n/a</td> ");
	  out.println(" <td align=\"center\"> -1 </td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println(" <td>A candidate patient series is a gender-specific patient series and the patient‘s gender matches a required gender specified on the first target dose.</th> ");
	  out.println(" <td align=\"center\"> +1</td> ");
	  out.println(" <td align=\"center\"> n/a</td> ");
	  out.println(" <td align=\"center\"> 0 </td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println(" <td>A candidate patient series is a product patient series. </th> ");
	  out.println(" <td align=\"center\"> -1</td> ");
	  out.println(" <td align=\"center\"> n/a</td> ");
	  out.println(" <td align=\"center\"> +1 </td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println(" <td>A candidate patient series has exceeded maximum age. </th> ");
	  out.println(" <td align=\"center\"> -1</td> ");
	  out.println(" <td align=\"center\"> n/a</td> ");
	  out.println(" <td align=\"center\"> +1 </td> ");
	  out.println("  </tr> ");
	  out.println("</table>");


  }



}
