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
    SeriesDose referenceSeriesDose = patientSeries.getForecast().getTargetDose().getTrackedSeriesDose();
    ;
    TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getMaximumAge();
    Date maximumAgeDate = addTimePeriodtotoDate(dob, timePeriod);
    return maximumAgeDate;
  }

  /**
   * cond1 A scorable patient series can start earliest
   */

  private void evaluate_AScorablePatientSeriesCanStartEarliest() {
    int numOfEarliestDates = 0;
    if (patientSeriesList.size() != 0 && patientSeriesList.get(0).getForecast() != null
        && patientSeriesList.get(0).getForecast().getEarliestDate() != null) {
      Date earliestDate = patientSeriesList.get(0).getForecast().getEarliestDate();
      for (PatientSeries patientSeries : patientSeriesList) {
        if (earliestDate == null) {
          earliestDate = patientSeries.getForecast().getEarliestDate();
          continue;
        }
        if (patientSeries.getForecast() == null || patientSeries.getForecast().getEarliestDate() == null) {
          continue;
        }
        if (earliestDate == patientSeries.getForecast().getEarliestDate()) {
          numOfEarliestDates++;
        } else {
          if (earliestDate.after(patientSeries.getForecast().getEarliestDate())) {
            earliestDate = patientSeries.getForecast().getEarliestDate();
            numOfEarliestDates = 0;
          }
        }
      }
      for (PatientSeries patientSeries : patientSeriesList) {
        if (patientSeries.getForecast() == null) {
          continue;
        }
        if (patientSeries.getForecast().getEarliestDate() != earliestDate) {
          patientSeries.descPatientScoreSeries();
        } else {
          if (numOfEarliestDates == 1) {
            patientSeries.incPatientScoreSeries();
          }
        }
      }
    } else {
      log("Forecast is not set");
    }
  }

  /**
   * cond2 A scorable patient series is completable.
   */

  private void evaluate_ACandidatePatientSeriesIsCompletable() {
    for (PatientSeries patientSeries : patientSeriesList) {
      Date finishDate = patientSeries.getForecast().getAdjustedPastDueDate();
      Date maximumAgeDate = findMaximumAgeDate(patientSeries);
      if (finishDate != null && finishDate.before(maximumAgeDate)) {
        patientSeries.incPatientScoreSeries();
      } else {
        patientSeries.incPatientScoreSeries();
      }
    }
  }

  /**
   * cond3 A scorable patient series is a gender-specific patient series and the
   * patient‘s gender
   * matches a required gender specified on the first target dose.
   */

  private void evaluate_ACandidatePatientSeriesGenderSpecific() {
    for (PatientSeries patientSeries : patientSeriesList) {
      boolean patientSeriesIsGenderSpecefic = false;
      SeriesDose referenceSeriesDose = patientSeries.getForecast().getTargetDose().getTrackedSeriesDose();
      String gender = referenceSeriesDose.getRequiredGenderList().size() == 0 ? null
          : referenceSeriesDose.getRequiredGenderList().get(0).getValue();
      if (gender != null && !gender.isEmpty()) {
        patientSeriesIsGenderSpecefic = true;
      }
      if (patientSeriesIsGenderSpecefic) {
        String targetDoseGender = dataModel.getPatient().getGender();
        if (gender != null && targetDoseGender.equals(gender)) {
          patientSeries.incPatientScoreSeries();
        }
      }

    }
  }

  /**
   * cond4 A scorable patient series is a product patient series.
   */

  private void evaluate_ACandidatePatientSeriesIsAProductPatientSeries() {
    boolean productPatientSeries = false;
    for (PatientSeries patientSeries : patientSeriesList) {
      if (patientSeries.getTrackedAntigenSeries().getSelectPatientSeries() != null &&
          patientSeries.getTrackedAntigenSeries().getSelectPatientSeries()
              .getProductPath() != null) {
        if (patientSeries.getTrackedAntigenSeries().getSelectPatientSeries().getProductPath()
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
   * cond5 A scorable patient series exceeded maximum age to start
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
    evaluate_AScorablePatientSeriesCanStartEarliest();
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
    setNextLogicStepType(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES);
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
    out.println(
        "<p>This section  provides the decision table for determining the number of points to assign to a scorable patient series when there are no valid doses.</p>");
    printTable(out);
    // printConditionAttributesTable(out);
    // printLogicTables(out);
    printBestPatientSeries(out);
  }

  private void printTable(PrintWriter out) {
    out.println("<table BORDER=\"1\"> ");
    out.println("  <tr> ");
    out.println(" <th> Conditions </th> ");
    out.println(" <th> If this condition is true for the scorable patient series </th> ");
    out.println(" <th>If this condition is true for two or more scorable patient series </th> ");
    out.println(" <th>If this condition is not true for the scorable patient series </th> ");
    out.println("  </tr> ");
    out.println("  <tr> ");
    out.println(" <td >A scorable patient series can start earliest. </th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> 0</td> ");
    out.println(" <td align=\"center\"> -1 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(" <td>A scorable patient series is completable.</th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
    out.println(" <td align=\"center\"> -1 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(
        " <td>A scorable patient series is a gender-specific patient series and the patient's gender matches a required gender specified on the first target dose.</th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
    out.println(" <td align=\"center\"> 0 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(" <td>A scorable patient series is a product patient series. </th> ");
    out.println(" <td align=\"center\"> -1</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
    out.println(" <td align=\"center\"> +1 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(" <td>A scorable patient series has exceeded maximum age. </th> ");
    out.println(" <td align=\"center\"> -1</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
    out.println(" <td align=\"center\"> +1 </td> ");
    out.println("  </tr> ");
    out.println("</table>");

  }

}
