package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;


public class CompletePatientSeries extends LogicStep
{

	
	private List<PatientSeries> patientSeriesList = dataModel.getPatientSeriesList();
	
	
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
	

	
	
	
	 private  Map<Integer, Integer> sortByComparator(Map<Integer, Integer> unsortMap)  {

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
	
	
  public CompletePatientSeries(DataModel dataModel)
  {
    super(LogicStepType.COMPLETE_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");
  }
  
  /***cond1
	 * A candidate patient series has the most valid doses
	 */
  
  private void evaluate_ACandidatePatientSeriesHasTheMostValidDoses(){
	  HashMap<Integer,Integer> condMap = new HashMap<Integer,Integer>();
	  for(int i=0; i<patientSeriesList.size();i++){
		  condMap.put(i,numberOfValidDoses(patientSeriesList.get(i)));
	  }
	  condMap =(HashMap<Integer, Integer>) sortByComparator(condMap);
	  int j = 0;
	  int tmp = 0;
	  int greatestElementPos = 0;
	  ArrayList<Integer> pos = new ArrayList<Integer>();
	  boolean twoOrMore = false;
	  for(Entry<Integer,Integer> entry:condMap.entrySet()){
		  if(j==0){
			  tmp = entry.getValue();
			  greatestElementPos = entry.getKey();
			  j++;
		  }
		  if(j>0){
			  if(tmp == entry.getValue()){
				  twoOrMore = true;
				 pos.add(entry.getKey());
				  
			  }
		  }
		  
	  }
	  if(twoOrMore){
		  pos.add(greatestElementPos);
	  }
	  
	  if(!twoOrMore){
		  patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
		  if(patientSeriesList.size()>1){
			  patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
			  for(PatientSeries patientSeries: patientSeriesList){
				  patientSeries.descPatientScoreSeries();
			  }
		  }
	  }else{
		  for(PatientSeries patientSeries: patientSeriesList){
			  patientSeries.descPatientScoreSeries();
		  }
		  for(int i:pos){
			  patientSeriesList.get(i).incPatientScoreSeries();
		  }
	  }  
	  
  }
  /***
	 * cond2
	 * A candidate patient series is a product patient series and has all valid doses.
	 * 
	 * P66 SelectB-23 "patient path" or "product path"
	 */
  
  private int isAProductPatientSeriesAndHasAllValidDoses(PatientSeries patientSeries){
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
				//	
				}else{
					hasAllValidDoses = true;
				}
			}
		}
		
		if(productPatientSeries && hasAllValidDoses){
			return 1;
		}else{
			return -1;
		}
	}
  
 private void evaluate_ACandidatePatientSeriesIsAProductPatientSeriesAndHasAllValidDoses(){
	 for(PatientSeries patientSeries:patientSeriesList){
		 if(isAProductPatientSeriesAndHasAllValidDoses(patientSeries)==1){
			 patientSeries.incPatientScoreSeries();
		 }else if(isAProductPatientSeriesAndHasAllValidDoses(patientSeries)==-1){
			 patientSeries.descPatientScoreSeries();
		 }
	 }
	  
  }
 
 /***
	 * cond3
	 * A candidate patient series is the earliest completing.
	 * 
	 * A complete patient series must be considered to be the earliest completing if the actual finish date is before the actual finish date for all 
	 * other complete patient series.
	 */
 
 private void evaluate_ACondidatePatientSeriesIsTheEarliestCompleting(){
	 HashMap<Integer, Date> finishDateMap = new HashMap<Integer,Date>();
	 for(int i=0 ;i<patientSeriesList.size();i++){
		 PatientSeries patientSeries=patientSeriesList.get(i);
		 Date finishDate = patientSeries.getForecast().getAdjustedPastDueDate();
		 finishDateMap.put(i, finishDate);
	 }
	 HashMap<Integer,Integer> amountMap = new HashMap<Integer, Integer>();
	 @SuppressWarnings("deprecation")
	Date refDate = new Date(1900,1,1);
	 for(Entry<Integer,Date> entry:finishDateMap.entrySet()){
		 Date tmpDate = entry.getValue();	
		  Calendar c = Calendar.getInstance();
          Calendar c2 = Calendar.getInstance();
          c.setTime(tmpDate);
          c2.setTime(refDate);
		 int nDifferenceInDays =  c.get(Calendar.DAY_OF_YEAR) - c2.get(Calendar.DAY_OF_YEAR);
		 amountMap.put(entry.getKey(), nDifferenceInDays); 
	 }
	 amountMap = (HashMap<Integer, Integer>) sortByComparator(amountMap);
	 
	 int j = 0;
	  int tmp = 0;
	  int greatestElementPos = 0;
	  ArrayList<Integer> pos = new ArrayList<Integer>();
	  boolean twoOrMore = false;
	  for(Entry<Integer,Integer> entry:amountMap.entrySet()){
		  if(j==0){
			  tmp = entry.getValue();
			  greatestElementPos = entry.getKey();
			  j++;
		  }
		  if(j>0){
			  if(tmp == entry.getValue()){
				  twoOrMore = true;
				 pos.add(entry.getKey());
				  
			  }
		  }
		  
	  }
	  if(twoOrMore){
		  pos.add(greatestElementPos);
	  }
	  
	  if(!twoOrMore){
		  patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
		  patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
		  if(patientSeriesList.size()>1){
			  patientSeriesList.get(greatestElementPos).descPatientScoreSeries();
			  for(PatientSeries patientSeries: patientSeriesList){
				  patientSeries.descPatientScoreSeries();
			  }
		  }
	  }else{
		  for(PatientSeries patientSeries: patientSeriesList){
			  patientSeries.descPatientScoreSeries();
		  }
		  for(int i:pos){
			  patientSeriesList.get(i).incPatientScoreSeries();
			  patientSeriesList.get(i).incPatientScoreSeries();
		  }
	  }  
	  
 }

  
  private void evaluateTable(){
	  evaluate_ACandidatePatientSeriesHasTheMostValidDoses();
	  evaluate_ACandidatePatientSeriesIsAProductPatientSeriesAndHasAllValidDoses();
	  evaluate_ACondidatePatientSeriesIsTheEarliestCompleting();
	  
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_BEST_CANDIDATE_PATIENT_SERIES);
    evaluateTable();
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
