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

public class SelectBestCandidatePatientSeries extends LogicStep
{
	
	private List<PatientSeries> patientSeriesList = dataModel.getPatientSeriesList();

  public SelectBestCandidatePatientSeries(DataModel dataModel)
  {
    super(LogicStepType.SELECT_BEST_CANDIDATE_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");

  }
  
  private  Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap)  {

      List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

      // Sorting the list based on values
      Collections.sort(list, new Comparator<Entry<String, Integer>>()        {

			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				// Sort Desc
				return  o2.getValue().compareTo(o1.getValue());
			}
      });

      // Maintaining insertion order with the help of LinkedList
      Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
      for (Entry<String, Integer> entry : list){
      
          sortedMap.put(entry.getKey(), entry.getValue());
          
      }

      return sortedMap;
  }

  
  private void ElectBestPatientSeries(PrintWriter out){
	  HashMap<String,Integer> patientSeriesMap= new HashMap<String,Integer>();
	  for(PatientSeries patientSeries:patientSeriesList){
		  patientSeriesMap.put(patientSeries.getTrackedAntigenSeries().getSeriesName(), patientSeries.getScorePatientSerie());
	  }
	  patientSeriesMap= (HashMap<String, Integer>) sortByComparator(patientSeriesMap);
			  
	  for (Entry<String, Integer> entry : patientSeriesMap.entrySet())
      {
          out.println("<p> PatientSeries : " + entry.getKey() + " Value : "+ entry.getValue()  +" </p>");
      }
	  
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_BEST_PATIENT_SERIES);
    return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
    ElectBestPatientSeries(out);
  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + getTitle() + "</h1>");
    out.println("<p>Select best candidate patient series  provides the business rules to  be applied to  the scored candidate patient series which will result in the best patient series for the patient.</p>");
    
    out.print("<h4> "+dataModel.getPatientSeries().getTrackedAntigenSeries().getSeriesName()+" </h4>");
    
  }

 



}
