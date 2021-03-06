package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriodType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class NoValidDoses extends LogicStep {

  private List<PatientSeries> patientSeriesList = dataModel.getPatientSeriesList();

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


  private Date findMaximumAgeDate(PatientSeries patientSeries) {
    Date dob = dataModel.getPatient().getDateOfBirth();
    SeriesDose referenceSeriesDose =
        patientSeries.getForecast().getTargetDose().getTrackedSeriesDose();;
    TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getMaximumAge();
    Date maximumAgeDate = addTimePeriodtotoDate(dob, timePeriod);
    return maximumAgeDate;
  }

  /**
   * cond1 A candidate patient series can start earliest
   */

  private void evaluate_ACandidatePatientSeriesCanStartEarliest() {
    int j = 0;
    if (patientSeriesList.get(0).getForecast() != null) {
      Date tmpDate = patientSeriesList.get(0).getForecast().getEarliestDate();
      for (int i = 0; i < patientSeriesList.size(); i++) {
        PatientSeries patientSeries = patientSeriesList.get(i);
        if (tmpDate == patientSeries.getForecast().getEarliestDate()) {
          j++;
        } else {
          if (tmpDate.after(patientSeries.getForecast().getEarliestDate())) {
            tmpDate = patientSeries.getForecast().getEarliestDate();
            j = 0;
          }
        }
      }
      for (PatientSeries patientSeries : patientSeriesList) {
        if (patientSeries.getForecast().getEarliestDate() != tmpDate) {
          patientSeries.descPatientScoreSeries();
        } else {
          if (j == 1)
            patientSeries.incPatientScoreSeries();
        }
      }
    } else {
      System.err.println("Forecast is not set");
    }
  }


  /**
   * cond2 A candidate patient series is completable.
   */

  private void evaluate_ACandidatePatientSeriesIsCompletable() {
    for (PatientSeries patientSeries : patientSeriesList) {
      Date finishDate = patientSeries.getForecast().getAdjustedPastDueDate();
      Date maximumAgeDate = findMaximumAgeDate(patientSeries);
      if (finishDate.before(maximumAgeDate)) {
        patientSeries.incPatientScoreSeries();
      } else {
        patientSeries.incPatientScoreSeries();
      }
    }
  }

  /**
   * cond3 A candidate patient series is a gender-specific patient series and the patient‘s gender
   * matches a required gender specified on the first target dose.
   */

  private void evaluate_ACandidatePatientSeriesGenderSpecific() {
    for (PatientSeries patientSeries : patientSeriesList) {
      boolean patientSeriesIsGenderSpecefic = false;
      SeriesDose referenceSeriesDose =
          patientSeries.getForecast().getTargetDose().getTrackedSeriesDose();
      String gender = referenceSeriesDose.getRequiredGenderList().get(0).getValue();
      if (!gender.isEmpty()) {
        patientSeriesIsGenderSpecefic = true;
      }
      if (patientSeriesIsGenderSpecefic) {
        String targetDoseGender = dataModel.getPatient().getGender();
        if (targetDoseGender.equals(gender)) {
          patientSeries.incPatientScoreSeries();
        }
      }

    }
  }

  /**
   * cond4 A candidate patient series is a product patient series.
   */

  private void evaluate_ACandidatePatientSeriesIsAProductPatientSeries() {
    boolean productPatientSeries = false;
    for (PatientSeries patientSeries : patientSeriesList) {
      if (patientSeries.getTrackedAntigenSeries().getSelectBestPatientSeries()
          .getProductPath() != null) {
        if (patientSeries.getTrackedAntigenSeries().getSelectBestPatientSeries().getProductPath()
            .equals(YesNo.YES)) {
          productPatientSeries = true;
        }

      }
      if (productPatientSeries) {
        patientSeries.incPatientScoreSeries();
      } else {
        patientSeries.descPatientScoreSeries();
      }
    }
  }

  /**
   * cond5 A candidate patient series exceeded maximum age to start
   */

  private void evaluate_ACandidatePatientSeriesHasExceededTheMaximumAge() {
    Date evalDate = dataModel.getAssessmentDate();
    for (PatientSeries patientSeries : patientSeriesList) {
      Date maximumAgeDate = findMaximumAgeDate(patientSeries);
      if (evalDate.after(maximumAgeDate)) {
        patientSeries.descPatientScoreSeries();
      } else {
        patientSeries.incPatientScoreSeries();
      }
    }

  }

  private void evalTable() {
    evaluate_ACandidatePatientSeriesCanStartEarliest();
    evaluate_ACandidatePatientSeriesIsCompletable();
    evaluate_ACandidatePatientSeriesGenderSpecific();
    evaluate_ACandidatePatientSeriesIsAProductPatientSeries();
    evaluate_ACandidatePatientSeriesHasExceededTheMaximumAge();
  }



  public NoValidDoses(DataModel dataModel) {
    super(LogicStepType.NO_VALID_DOSES, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_BEST_CANDIDATE_PATIENT_SERIES);
    evaluateLogicTables();
    evalTable();
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
    out.println(
        "<p>This section  provides the decision table for determining the number of points to assign to a  candidate  patient series when there are no valid doses.</p>");
    printTable(out);
    // printConditionAttributesTable(out);
    // printLogicTables(out);
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
    out.println(
        " <td>A candidate patient series is a gender-specific patient series and the patient‘s gender matches a required gender specified on the first target dose.</th> ");
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
