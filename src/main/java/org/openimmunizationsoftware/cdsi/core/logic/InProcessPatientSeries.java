package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class InProcessPatientSeries extends LogicStep
{

  // private ConditionAttribute<Date> caDateAdministered = null;
	
	private List<PatientSeries> patientSeriesList = dataModel.getPatientSeriesList();
	
	/***
	 * cond1
	 * A candidate patient series is a product patient series and has all valid doses.
	 * 
	 * P66 SelectB-23 "patient path" or "product path"
	 */
	
	private int cond1(PatientSeries patientSeries){
		boolean productPatientSeries = false;
		boolean hasAllValidDoses = false;
		
		
		if(patientSeries.getTrackedAntigenSeries().getSelectBestPatientSeries().getProductPath()!=null){
			if(patientSeries.getTrackedAntigenSeries().getSelectBestPatientSeries().getProductPath().equals(YesNo.YES)){
				productPatientSeries = true;
			}
			
		}
		
		for(TargetDose target:patientSeries.getTargetDoseList()){
			if(target.getTargetDoseStatus()!=null){
				if(!target.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)){
					
				}else{
					hasAllValidDoses = true;
				}
			}
			
		}
		
		if(productPatientSeries && hasAllValidDoses){
			return 1;
		}else{
			return 0;
		}
	}
	
	/**
	 * Cond2
	 * A candidate patient series is completable.
	 */
	
	private int cond2(PatientSeries patientSeries){
		return 0;
	}
	
	/**
	 * cond3
	 * Number of valid doses
	 */
	
	private int  numberOfValidDoses (PatientSeries patientSeries){
		int nbOfValidDoses = 0;
		for(TargetDose target:patientSeries.getTargetDoseList()){
			if(target.getTargetDoseStatus()!=null){
				if(target.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)){
					nbOfValidDoses++;
				}
			}
			
		}
		return nbOfValidDoses;
	}
	
	/**
	 * Cond4
	 * A candidate patient series is closest to completion.
	 */
	private int isClosestToCompletion(PatientSeries patientSeries){
		return 0;
	}
	
	/**
	 * Cond5
	 * A candidate patient series can finish earliest.
	 */
	
	private int canFinishEarliest(PatientSeries patientSeries){
		return 0;
	}
	
	/**
	 * Cond6
	 * A candidate patient series exceeded maximum age to start
	 */
	
	private int exceededTheMaximumAgeToStart(PatientSeries patientSeries){
		int maximumAgetoStart;
		return 0;
	}
	
	

 

  public InProcessPatientSeries(DataModel dataModel)
  {
    super(LogicStepType.IN_PROCESS_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");
    
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_BEST_CANDIDATE_PATIENT_SERIES);
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
    out.println("<p>In-process  patient series provides the decision table for determining the number of points to assign to an  inprocess patient series based on a specified condition.</p>");
    printTable(out);
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
	  out.println(" <td >A candidate patient series is a product patient series and has all valid doses </th> ");
	  out.println(" <td align=\"center\"> +2</td> ");
	  out.println(" <td align=\"center\"> 0</td> ");
	  out.println(" <td align=\"center\"> -2 </td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println(" <td>A candidate patient series is completable.</th> ");
	  out.println(" <td align=\"center\"> +3</td> ");
	  out.println(" <td align=\"center\"> n/a</td> ");
	  out.println(" <td align=\"center\"> -3 </td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println(" <td>A candidate patient series has the most valid doses.</th> ");
	  out.println(" <td align=\"center\"> +2</td> ");
	  out.println(" <td align=\"center\"> 0</td> ");
	  out.println(" <td align=\"center\"> -2 </td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println(" <td>A candidate patient series is closest to completion. </th> ");
	  out.println(" <td align=\"center\"> -2</td> ");
	  out.println(" <td align=\"center\"> n/a</td> ");
	  out.println(" <td align=\"center\"> +2 </td> ");
	  out.println("  </tr> ");
	  out.println("  <tr> ");
	  out.println(" <td>A candidate patient series can finish earliest. </th> ");
	  out.println(" <td align=\"center\"> -1</td> ");
	  out.println(" <td align=\"center\"> n/a</td> ");
	  out.println(" <td align=\"center\"> +1 </td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println(" <td>A candidate patient series exceeded maximum age to start. </th> ");
	  out.println(" <td align=\"center\"> -1</td> ");
	  out.println(" <td align=\"center\"> n/a</td> ");
	  out.println(" <td align=\"center\"> +1 </td> ");
	  out.println("  </tr> ");
	  out.println("</table>");
	  }
  
  
  
  

  


}
