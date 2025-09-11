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
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class SelectPrioritizedPatientSeries extends LogicStep {

  private List<PatientSeries> patientSeriesList = dataModel.getSelectedPatientSeriesList();

  public SelectPrioritizedPatientSeries(DataModel dataModel) {
    super(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");

  }

  private PatientSeries prioritizedPatientSeries = null;

  private void selectPrioritizedPatientSeries() {
    if(patientSeriesList.size() > 0) {
      prioritizedPatientSeries = patientSeriesList.get(0);
    }
    for (PatientSeries patientSeries : patientSeriesList) {
      if(patientSeries.getScorePatientSeries() == prioritizedPatientSeries.getScorePatientSeries()) {
        String currentSeriesPreference = patientSeries.getTrackedAntigenSeries().getSelectPatientSeries().getSeriesPreference();
        String newSeriesPreference = prioritizedPatientSeries.getTrackedAntigenSeries().getSelectPatientSeries().getSeriesPreference();
        if(currentSeriesPreference != "" && newSeriesPreference != "") {
          if(Integer.parseInt(currentSeriesPreference) < Integer.parseInt(newSeriesPreference)) {
            prioritizedPatientSeries = patientSeries;
          }
        }
      }

      if(patientSeries.getScorePatientSeries() > prioritizedPatientSeries.getScorePatientSeries()) {
        prioritizedPatientSeries = patientSeries;
      }
    }

  }

  @Override
  public LogicStep process() throws Exception {
    selectPrioritizedPatientSeries();
    if (prioritizedPatientSeries != null) {
      dataModel.getPrioritizedPatientSeriesList().add(prioritizedPatientSeries);
    }
    setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
    return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  private int numberOfValidDoses(PatientSeries patientSeries) {
    int nbOfValidDoses = 0;
    for (TargetDose target : patientSeries.getTargetDoseList()) {
      if(target.getTargetDoseStatus() != null) {
        if(target.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {
          nbOfValidDoses++;
        }
      }

    }
    return nbOfValidDoses;
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
    out.println("<p>Prioritized Patient Series: " + prioritizedPatientSeries + "</p>");
    for (PatientSeries patientSeries : patientSeriesList) {
      out.println(
          "<p> PatientSeries : " +patientSeries.getTrackedAntigenSeries().getSeriesName() + " Value : " + patientSeries.getScorePatientSeries() + " valid doses : " + numberOfValidDoses(patientSeries) + " </p>");
    }
  }

  private void printStandard(PrintWriter out) {
    out.println(
        "<p>Select prioritized patient series provides the business rules to be applied to the scored patient series which will result in the prioritized patient series for the series group.</p>");

    out.print("<h4> " + dataModel.getAntigen().getName() + " </h4>");
  }

}
