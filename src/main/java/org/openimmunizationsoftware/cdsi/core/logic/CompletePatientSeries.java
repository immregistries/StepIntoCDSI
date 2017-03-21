package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class CompletePatientSeries extends LogicStep
{

  // private ConditionAttribute<Date> caDateAdministered = null;
	
	private List<PatientSeries> patientSeriesList = dataModel.getPatientSeriesList();
	
	/***cond1
	 * A candidate patient series has the most valid doses
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
	
	/***
	 * cond2
	 * A candidate patient series is a product patient series and has all valid doses.
	 * 
	 * P66 SelectB-23 "patient path" or "product path"
	 */
	
	private int cond2(PatientSeries patientSeries){
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
	
	/***
	 * cond3
	 * A candidate patient series is the earliest completing.
	 * 
	 * A complete patient series must be considered to be the earliest completing if the actual finish date is before the actual finish date for all 
	 * other complete patient series.
	 */
	
	private boolean isTheEarliestCompleting(PatientSeries patientSeries){
		// Don't know how to verify the condition
		return false;
	}
	
	private HashMap<Integer, Integer> evaluateCond1(){
		HashMap<Integer, Integer> condMap = new HashMap<Integer, Integer>();
		for(int i=0; i<patientSeriesList.size();i++){
			condMap.put(i,  numberOfValidDoses(patientSeriesList.get(i)));
		}
		
		return condMap;
	}
	
	private HashMap<Integer, Integer> evaluateCond2(){
		HashMap<Integer, Integer> condMap = new HashMap<Integer, Integer>();
		for(int i=0; i<patientSeriesList.size();i++){
			condMap.put(i, cond2(patientSeriesList.get(i)));
		}
		
		return condMap;
	}
	private HashMap<Integer, Integer> evaluateCond3(){
		HashMap<Integer, Integer> condMap = new HashMap<Integer, Integer>();
		
		return condMap;
	}
	
	 private  Map<Integer, Integer> sortByComparator(Map<Integer, Integer> unsortMap)
	    {

	        List<Entry<Integer, Integer>> list = new LinkedList<Entry<Integer, Integer>>(unsortMap.entrySet());

	        // Sorting the list based on values
	        Collections.sort(list, new Comparator<Entry<Integer, Integer>>()        {
	  
				@Override
				public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
					// Sort Desc
					return  o2.getValue().compareTo(o1.getValue());
				}
	        });

	        // Maintaining insertion order with the help of LinkedList
	        Map<Integer, Integer> sortedMap = new LinkedHashMap<Integer, Integer>();
	        for (Entry<Integer, Integer> entry : list){
	        
	            sortedMap.put(entry.getKey(), entry.getValue());
	            
	        }

	        return sortedMap;
	    }
	
	private void evalateTable(){
		HashMap<Integer,Integer> cond1 = evaluateCond1();
		cond1 = (HashMap<Integer, Integer>) sortByComparator(cond1);
		Entry<Integer, Integer> entry = cond1.entrySet().iterator().next();
		int biggestValue = entry.getValue();
		for (Entry<Integer, Integer> entry0 : cond1.entrySet()){
			if(biggestValue>=entry0.getValue()){
				patientSeriesList.get(entry0.getKey()).incPatientScoreSeries();
			}else{
				patientSeriesList.get(entry0.getKey()).descPatientScoreSeries();
			}
			
		}
		HashMap<Integer,Integer> cond2 = evaluateCond2();
		for(Entry<Integer,Integer> entry0:cond2.entrySet()){
			if(entry0.getValue()==1){
				patientSeriesList.get(entry0.getKey()).incPatientScoreSeries();
			}else{
				patientSeriesList.get(entry0.getKey()).descPatientScoreSeries();
			}
		}
		//HashMap<Integer,Integer> cond3 = evaluateCond3();
		
		
	}


  public CompletePatientSeries(DataModel dataModel)
  {
    super(LogicStepType.COMPLETE_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_BEST_CANDIDATE_PATIENT_SERIES);
    evalateTable();
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
    out.println("<p>Complete  patient  series  provides  the  decision  table  for  determining  the  number  of  points  to  assign  to  a complete patient series based on a specified condition. </p>");
    printTable(out);
  }
  
  private void printTable(PrintWriter out ){
	  out.println("");
	  out.println("<table BORDER=\"1\"> ");
	  out.println("  <tr> ");
	  out.println(" <th> Conditions </th> ");
	  out.println(" <th> If this condition is true for the candidate patient series </th> ");
	  out.println(" <th>If this condition is true for two or more candidate patient series </th> ");
	  out.println(" <th>If this condition is not true for the candidate patient serie </th> ");
	  out.println("  </tr> ");
	  out.println("  <tr> ");
	  out.println(" <td >A candidate patient series has the most valid doses.</th> ");
	  out.println(" <td align=\"center\"> +1</td> ");
	  out.println(" <td align=\"center\"> 0</td> ");
	  out.println(" <td align=\"center\"> -1 </td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println(" <td>A candidate patient series is a product patient series and has all valid doses.</th> ");
	  out.println(" <td align=\"center\"> +1</td> ");
	  out.println(" <td align=\"center\"> n/a</td> ");
	  out.println(" <td align=\"center\"> -1 </td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println(" <td>A candidate patient series is the earliest completing.</th> ");
	  out.println(" <td align=\"center\"> +2</td> ");
	  out.println(" <td align=\"center\"> +1</td> ");
	  out.println(" <td align=\"center\"> -1</td> ");
	  out.println("  </tr> ");
	  out.println("<tr> ");
	  out.println("</table>");

	  }
 


}
