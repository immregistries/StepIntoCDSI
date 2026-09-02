package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

/**
 * Section 8.6 "No Valid Doses", Table 8-11, condition "Is completable"
 * (business rule SELECTB-3).
 *
 * <p>
 * Table 8-11 awards <b>+1</b> when a scorable patient series is completable
 * and <b>-1</b> when it is not. Before SPEC-4.6-0007 was fixed, both branches
 * of {@code evaluate_ACandidatePatientSeriesIsCompletable()}'s if/else called
 * {@code incPatientScoreSeries()}, so the condition contributed +1
 * unconditionally and never discriminated between a completable and a
 * non-completable series - the "not true" column of the table was
 * unreachable.
 *
 * <p>
 * These tests drive that one condition in isolation (the enclosing step's
 * {@code process()} would need a fully loaded schedule and Supporting Data),
 * which is why the private method is invoked reflectively rather than through
 * the public step API. {@code notCompletableSeriesIsPenalized} is the
 * regression test proper: it fails with the pre-fix code and passes with the
 * fix.
 */
public class NoValidDosesCompletableTest {

  private static final String CONDITION_METHOD = "evaluate_ACandidatePatientSeriesIsCompletable";

  private static Date date(int year, int month, int day) {
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(year, month - 1, day);
    return calendar.getTime();
  }

  /**
   * A patient series whose forecast finish date ("adjusted past due date") is
   * {@code finishDate}, tracked against a series dose whose maximum age is
   * {@code maximumAge} after the patient's date of birth.
   */
  private static PatientSeries patientSeries(Date finishDate, String maximumAge) {
    Age age = new Age();
    if (maximumAge != null) {
      age.setMaximumAge(new TimePeriod(maximumAge));
    }
    SeriesDose seriesDose = new SeriesDose();
    seriesDose.getAgeList().add(age);

    TargetDose targetDose = new TargetDose();
    targetDose.setTrackedSeriesDose(seriesDose);

    Forecast forecast = new Forecast();
    forecast.setTargetDose(targetDose);
    forecast.setAdjustedPastDueDate(finishDate);

    PatientSeries patientSeries = new PatientSeries();
    patientSeries.setForecast(forecast);
    return patientSeries;
  }

  private static DataModel dataModelFor(PatientSeries... patientSeriesList) {
    Patient patient = new Patient();
    patient.setDateOfBirth(date(2020, 1, 1));

    DataModel dataModel = new DataModel();
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date(2024, 6, 1));
    for (PatientSeries patientSeries : patientSeriesList) {
      dataModel.getPatientSeriesStepper().add(patientSeries);
    }
    return dataModel;
  }

  private static void evaluateIsCompletable(DataModel dataModel) throws Exception {
    NoValidDoses step = new NoValidDoses(dataModel);
    Method method = NoValidDoses.class.getDeclaredMethod(CONDITION_METHOD);
    method.setAccessible(true);
    method.invoke(step);
  }

  /** Table 8-11, "Is completable" = true: +1. */
  @Test
  public void completableSeriesScoresPlusOne() throws Exception {
    // Max age to complete is 2025-01-01; the series finishes well before it.
    PatientSeries completable = patientSeries(date(2024, 1, 1), "5 years");
    evaluateIsCompletable(dataModelFor(completable));
    assertEquals(1, completable.getScorePatientSeries());
  }

  /**
   * Table 8-11, "Is completable" = not true: -1.
   *
   * <p>
   * This is the SPEC-4.6-0007 regression test - the pre-fix code scored +1
   * here, because both branches of the if/else incremented.
   */
  @Test
  public void notCompletableSeriesIsPenalized() throws Exception {
    // Max age to complete is 2025-01-01; the series cannot finish until after it.
    PatientSeries notCompletable = patientSeries(date(2026, 1, 1), "5 years");
    evaluateIsCompletable(dataModelFor(notCompletable));
    assertEquals(-1, notCompletable.getScorePatientSeries());
  }

  /**
   * The point of the condition is to separate the two, so a completable series
   * must end up strictly ahead of a non-completable one. Under the pre-fix code
   * both landed on +1 and the condition was a no-op for ranking.
   */
  @Test
  public void completableSeriesOutscoresNotCompletableSeries() throws Exception {
    PatientSeries completable = patientSeries(date(2024, 1, 1), "5 years");
    PatientSeries notCompletable = patientSeries(date(2026, 1, 1), "5 years");
    evaluateIsCompletable(dataModelFor(completable, notCompletable));
    assertEquals(1, completable.getScorePatientSeries());
    assertEquals(-1, notCompletable.getScorePatientSeries());
  }

  /**
   * The condition cannot be shown true when there is no finish date to compare,
   * so it falls in the table's "not true" column. Documents the behaviour of the
   * minimal fix rather than asserting a separate specification rule.
   */
  @Test
  public void seriesWithNoFinishDateIsNotCompletable() throws Exception {
    PatientSeries noFinishDate = patientSeries(null, "5 years");
    evaluateIsCompletable(dataModelFor(noFinishDate));
    assertEquals(-1, noFinishDate.getScorePatientSeries());
  }

  /** A patient series with no forecast at all is not scored by this condition. */
  @Test
  public void seriesWithoutForecastIsNotScored() throws Exception {
    PatientSeries noForecast = new PatientSeries();
    evaluateIsCompletable(dataModelFor(noForecast));
    assertEquals(0, noForecast.getScorePatientSeries());
  }
}
