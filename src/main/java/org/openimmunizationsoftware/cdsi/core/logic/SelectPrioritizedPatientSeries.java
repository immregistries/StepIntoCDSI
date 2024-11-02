package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;

public class SelectPrioritizedPatientSeries extends LogicStep {

  private List<PatientSeries> patientSeriesList = dataModel.getSelectedPatientSeriesList();

  public SelectPrioritizedPatientSeries(DataModel dataModel) {
    super(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");

  }

  private LinkedHashMap<PatientSeries, Integer> sortByComparator(
      LinkedHashMap<PatientSeries, Integer> unsortMap) {

    List<Entry<PatientSeries, Integer>> list = new LinkedList<Entry<PatientSeries, Integer>>(unsortMap.entrySet());

    // Sorting the list based on values
    Collections.sort(list, new Comparator<Entry<PatientSeries, Integer>>() {

      @Override
      public int compare(Entry<PatientSeries, Integer> o1, Entry<PatientSeries, Integer> o2) {
        // Sort Desc
        return o2.getValue().compareTo(o1.getValue());
      }
    });

    // Maintaining insertion order with the help of LinkedList
    LinkedHashMap<PatientSeries, Integer> sortedMap = new LinkedHashMap<PatientSeries, Integer>();
    for (Entry<PatientSeries, Integer> entry : list) {

      sortedMap.put(entry.getKey(), entry.getValue());

    }

    return sortedMap;
  }

  private LinkedHashMap<PatientSeries, Integer> patientSeriesMap = new LinkedHashMap<PatientSeries, Integer>();
  private PatientSeries bestPatientSeries = null;

  private void selectBestPatientSeries() {
    for (PatientSeries patientSeries : patientSeriesList) {
      patientSeriesMap.put(patientSeries, patientSeries.getScorePatientSeries());
    }
    patientSeriesMap = (LinkedHashMap<PatientSeries, Integer>) sortByComparator(patientSeriesMap);
    if (patientSeriesMap.size() > 0) {
      bestPatientSeries = (PatientSeries) patientSeriesMap.keySet().toArray()[0];
    }
  }

  @Override
  public LogicStep process() throws Exception {
    selectBestPatientSeries();
    if (bestPatientSeries != null) {
      dataModel.getBestPatientSeriesList().add(bestPatientSeries);
    }
    setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
    return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
    out.println("<p>Best Patient Series: " + bestPatientSeries + "</p>");
    for (Entry<PatientSeries, Integer> entry : patientSeriesMap.entrySet()) {
      out.println(
          "<p> PatientSeries : " + entry.getKey() + " Value : " + entry.getValue() + " </p>");
    }

  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + getTitle() + "</h1>");
    out.println(
        "<p>Select best candidate patient series  provides the business rules to  be applied to  the scored candidate patient series which will result in the best patient series for the patient.</p>");

    out.print("<h4> " + dataModel.getAntigen().getName() + " </h4>");

  }

}
