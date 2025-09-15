package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.time.DateUtils;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriodType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class InProcessPatientSeries extends LogicStep {

  // private ConditionAttribute<Date> caDateAdministered = null;

  private List<PatientSeries> patientSeriesList = dataModel.getPatientSeriesList();

  /***
   * cond1 A candidate patient series is a product patient series and has all
   * valid doses.
   * 
   */

  private void evaluate_ACandidatePatientSeriesIsAProductPatientSeriesAndHasAllValidDoses() {
    boolean productPatientSeries = false;
    boolean hasAllValidDoses = true;

    for (PatientSeries patientSeries : patientSeriesList) {
      if (patientSeries.getTrackedAntigenSeries().getSelectPatientSeries()
          .getProductPath() != null) {
        if (patientSeries.getTrackedAntigenSeries().getSelectPatientSeries().getProductPath()
            .equals(YesNo.YES)) {
          productPatientSeries = true;
        }

      }

      for (TargetDose target : patientSeries.getTargetDoseList()) {
        if (target.getTargetDoseStatus() != null) {
          if (target.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {

          } else {
            hasAllValidDoses = false;
          }
        }

      }
      if (productPatientSeries && hasAllValidDoses) {
        patientSeries.incPatientScoreSeries();
        patientSeries.incPatientScoreSeries();
      } else {
        patientSeries.descPatientScoreSeries();
        patientSeries.descPatientScoreSeries();
      }
    }
  }

  /**
   * Cond2 A candidate patient series is completable.
   */

  private void evaluate_ACandidatePatientSeriesIsCompletable() {
    for (PatientSeries patientSeries : patientSeriesList) {
      Date finishDate = patientSeries.getForecast().getAdjustedPastDueDate();
      Date maximumAgeDate = findMaximumAgeDate(patientSeries);
      if (finishDate != null && finishDate.before(maximumAgeDate)) {
        patientSeries.incPatientScoreSeries();
        patientSeries.incPatientScoreSeries();
        patientSeries.incPatientScoreSeries();
      } else {
        patientSeries.descPatientScoreSeries();
        patientSeries.descPatientScoreSeries();
        patientSeries.descPatientScoreSeries();
      }
    }
  }

  private Date findMaximumAgeDate(PatientSeries patientSeries) {
    Date dob = dataModel.getPatient().getDateOfBirth();
    SeriesDose referenceSeriesDose = patientSeries.getForecast().getTargetDose().getTrackedSeriesDose();
    ;
    TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getMaximumAge();
    Date maximumAgeDate = addTimePeriodtotoDate(dob, timePeriod);
    return maximumAgeDate;
  }

  public Date addTimePeriodtotoDate(Date date, TimePeriod timePeriod) {
    int amount = timePeriod.getAmount();
    TimePeriodType type = timePeriod.getType();
    switch (type) {
      case DAY:
        date = DateUtils.addDays(date, amount);
        break;
      case WEEK:
        date = DateUtils.addWeeks(date, amount);
        break;
      case MONTH:
        date = DateUtils.addMonths(date, amount);
        break;
      case YEAR:
        date = DateUtils.addYears(date, amount);
        break;
      default:
        break;
    }
    return date;
  }

  /**
   * cond3 A candidate patient series has the most valid doses
   */

  private int numberOfValidDoses(PatientSeries patientSeries) {
    int nbOfValidDoses = 0;
    for (TargetDose target : patientSeries.getTargetDoseList()) {
      if (target.getTargetDoseStatus() != null) {
        if (target.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {
          nbOfValidDoses++;
        }
      }

    }
    return nbOfValidDoses;
  }

  private Map<Integer, Integer> sortByComparator(Map<Integer, Integer> unsortMap) {

    List<Entry<Integer, Integer>> list = new LinkedList<Entry<Integer, Integer>>(unsortMap.entrySet());

    // Sorting the list based on values
    Collections.sort(list, new Comparator<Entry<Integer, Integer>>() {

      @Override
      public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
        // Sort Desc
        return o2.getValue().compareTo(o1.getValue());
      }
    });

    // Maintaining insertion order with the help of LinkedList
    Map<Integer, Integer> sortedMap = new LinkedHashMap<Integer, Integer>();
    for (Entry<Integer, Integer> entry : list) {

      sortedMap.put(entry.getKey(), entry.getValue());

    }

    return sortedMap;
  }

  private void evaluate_ACandidatePatientSeriesHasTheMostValidDoses() {
    HashMap<Integer, Integer> condMap = new HashMap<Integer, Integer>();
    for (int i = 0; i < patientSeriesList.size(); i++) {
      condMap.put(i, numberOfValidDoses(patientSeriesList.get(i)));
    }
    condMap = (HashMap<Integer, Integer>) sortByComparator(condMap);
    int j = 0;
    int tmp = 0;
    int greatestElementPos = 0;
    ArrayList<Integer> pos = new ArrayList<Integer>();
    boolean twoOrMore = false;
    for (Entry<Integer, Integer> entry : condMap.entrySet()) {
      if (j == 0) {
        tmp = entry.getValue();
        greatestElementPos = entry.getKey();
        j++;
      }
      if (j > 0) {
        if (tmp == entry.getValue()) {
          twoOrMore = true;
          pos.add(entry.getKey());

        }
      }

    }
    if (twoOrMore) {
      pos.add(greatestElementPos);
    }

    if (!twoOrMore) {
      patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
      patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
      if (patientSeriesList.size() > 1) {
        patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
        patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
        for (PatientSeries patientSeries : patientSeriesList) {
          patientSeries.descPatientScoreSeries();
          patientSeries.descPatientScoreSeries();
        }
      }
    } else {
      for (PatientSeries patientSeries : patientSeriesList) {
        patientSeries.descPatientScoreSeries();
        patientSeries.descPatientScoreSeries();
      }
      for (int i : pos) {
        patientSeriesList.get(i).incPatientScoreSeries();
        patientSeriesList.get(i).incPatientScoreSeries();
      }
    }

  }

  /**
   * Cond4 A candidate patient series is closest to completion.
   */

  private void evaluate_ACandidatePatientSeriesIsClosestToCompletion() {
    HashMap<Integer, Integer> condMap = new HashMap<Integer, Integer>();
    for (int i = 0; i < patientSeriesList.size(); i++) {
      condMap.put(i, numberOfNotSatisfiedTargetDoses(patientSeriesList.get(i)));
    }
    condMap = (HashMap<Integer, Integer>) sortByComparator(condMap);
    int j = 0;
    int tmp = 0;
    int greatestElementPos = 0;
    ArrayList<Integer> pos = new ArrayList<Integer>();
    boolean twoOrMore = false;
    for (Entry<Integer, Integer> entry : condMap.entrySet()) {
      if (j == 0) {
        tmp = entry.getValue();
        greatestElementPos = entry.getKey();
        j++;
      }
      if (j > 0) {
        if (tmp == entry.getValue()) {
          twoOrMore = true;
          pos.add(entry.getKey());

        }
      }

    }
    if (twoOrMore) {
      pos.add(greatestElementPos);
    }

    if (!twoOrMore) {
      patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
      patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
      if (patientSeriesList.size() > 1) {
        patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
        patientSeriesList.get(greatestElementPos).incPatientScoreSeries();
        for (PatientSeries patientSeries : patientSeriesList) {
          patientSeries.descPatientScoreSeries();
          patientSeries.descPatientScoreSeries();
        }
      }
    } else {
      for (PatientSeries patientSeries : patientSeriesList) {
        patientSeries.descPatientScoreSeries();
        patientSeries.descPatientScoreSeries();
      }
      for (int i : pos) {
        patientSeriesList.get(i).incPatientScoreSeries();
        patientSeriesList.get(i).incPatientScoreSeries();
      }
    }

  }

  private int numberOfNotSatisfiedTargetDoses(PatientSeries patientSeries) {
    int nbOfNotSatisfiedTargetDoses = 0;
    for (TargetDose target : patientSeries.getTargetDoseList()) {
      if (target.getTargetDoseStatus() != null) {
        if (target.getTargetDoseStatus().equals(TargetDoseStatus.NOT_SATISFIED)) {
          nbOfNotSatisfiedTargetDoses++;
        }
      }

    }
    return nbOfNotSatisfiedTargetDoses;
  }

  /**
   * Cond5 A candidate patient series can finish earliest.
   */

  private void evaluate_ACandidatePatientSeriesCanFinishEarliest() {
    int j = 0;
    if (patientSeriesList.get(0).getForecast() != null) {
      Date tmpDate = patientSeriesList.get(0).getForecast().getLatestDate();
      if(tmpDate != null) {
        for (int i = 0; i < patientSeriesList.size(); i++) {
          PatientSeries patientSeries = patientSeriesList.get(i);
          if (tmpDate == patientSeries.getForecast().getLatestDate()) {
            j++;
          } else {
            if (tmpDate.after(patientSeries.getForecast().getLatestDate())) {
              tmpDate = patientSeries.getForecast().getLatestDate();
              j = 0;
            }
          }
        }
      }
      for (PatientSeries patientSeries : patientSeriesList) {
        if (patientSeries.getForecast().getLatestDate() != tmpDate) {
          patientSeries.descPatientScoreSeries();
        } else {
          if (j == 1)
            patientSeries.incPatientScoreSeries();
        }
      }
    }
  }

  private void evaluateTable() {
    evaluate_ACandidatePatientSeriesIsAProductPatientSeriesAndHasAllValidDoses();
    evaluate_ACandidatePatientSeriesIsCompletable();
    evaluate_ACandidatePatientSeriesHasTheMostValidDoses();
    evaluate_ACandidatePatientSeriesIsClosestToCompletion();
    evaluate_ACandidatePatientSeriesCanFinishEarliest();
  }

  public InProcessPatientSeries(DataModel dataModel) {
    super(LogicStepType.IN_PROCESS_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");

  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES);
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
    out.println(
        "<p>In-process  patient series provides the decision table for determining the number of points to assign to an  inprocess patient series based on a specified condition.</p>");
    printTable(out);
    printBestPatientSeries(out);
  }

  private void printTable(PrintWriter out) {
     out.println("<table BORDER=\"1\"> ");
    out.println("  <tr> ");
    out.println(" <th> Conditions </th> ");
    out.println(" <th> If this condition is true for the candidate patient series </th> ");
    out.println(" <th>If this condition is true for two or more candidate patient series </th> ");
    out.println(" <th>If this condition is not true for the candidate patient serie </th> ");
    out.println("  </tr> ");
    out.println("  <tr> ");
    out.println(
        " <td >A candidate patient series is a product patient series and has all valid doses </th> ");
    out.println(" <td align=\"center\"> +2</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
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
    out.println(" <td align=\"center\"> +2</td> ");
    out.println(" <td align=\"center\"> 0</td> ");
    out.println(" <td align=\"center\"> -2 </td> ");
    out.println("  </tr> ");
    out.println("  <tr> ");
    out.println(" <td>A candidate patient series can finish earliest. </th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> 0</td> ");
    out.println(" <td align=\"center\"> -1 </td> ");
    out.println("  </tr> ");
    out.println("</table>");
  }

}
