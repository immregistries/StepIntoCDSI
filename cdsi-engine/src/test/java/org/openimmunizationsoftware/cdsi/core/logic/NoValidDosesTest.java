package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.RequiredGender;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

/**
 * Section 8.6 "No Valid Doses" (Logic Specification for ACIP Recommendations
 * v4.6, page 91; Table 8-11 "How Many Points are Awarded to a Scorable Patient
 * Series that has No Valid Doses?" and Table 8-12 business rules SELECTB-3,
 * SELECTB-12, SELECTB-14 and SELECTB-23) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/08-06-no-valid-doses/index.md}.
 *
 * <p>
 * 8.6 is the last member of the 8.4/8.5/8.6 scoring family. It runs when 8.3
 * classified the series group as having no valid doses at all, and Table 8-11
 * scores three conditions onto one running integer per patient series,
 * {@code PatientSeries.getScorePatientSeries()}, which 8.7 later consumes:
 *
 * <pre>
 * A scorable patient series can start earliest      +1 /  0  / -1
 * A scorable patient series is completable          +1 / n/a / -1
 * A scorable patient series is a product series     -1 / n/a / +1
 * </pre>
 *
 * <p>
 * Note the third row's inverted sign convention: in Table 8-11 being a product
 * patient series is a <i>penalty</i>, unlike 8.5's Table 8-9 where the product
 * flag is half of a positive condition. That is the specification's own
 * convention, not a transcription slip - the extracted table text reads
 * "A scorable patient series is a product patient series. | -1 | n/a | +1".
 *
 * <p>
 * <b>Relationship to {@link NoValidDosesCompletableTest}.</b> That class covers
 * the "is completable" row's two basic outcomes, written alongside the
 * SPEC-4.6-0007 fix (both branches of the row's if/else used to increment). It
 * is deliberately left untouched; this class covers everything else Table 8-11
 * and 08-06's other business rules require - the other two rows, the two
 * scoring conditions the implementation runs that Table 8-11 does not define at
 * all, the parts of SELECTB-3 and SELECTB-12 the completable row rests on, the
 * scope 8.6 scores over, its State Changes and its Next Steps.
 *
 * <p>
 * <b>Isolation.</b> Each of Table 8-11's rows has its own private method on
 * {@link NoValidDoses}, and {@code process()} calls all five (three documented,
 * two not) in order. Testing a single row through {@code process()} would mean
 * asserting a sum, so every row test invokes its own method reflectively - the
 * precedent {@link NoValidDosesCompletableTest} set for this class - and only
 * the State Changes and Next Steps tests at the bottom drive the public
 * {@code process()}. Either way the fixture is hand-built: no Supporting Data
 * release, no loader and no upstream step is involved.
 *
 * <p>
 * <b>Dates.</b> Every fixture patient is born 01/01/2020, every assessment is
 * made 06/01/2024, and every forecast target dose ages out five years after
 * birth, on 01/01/2025, unless a test says otherwise - so "finishes 01/01/2024"
 * is completable and "finishes 01/01/2026" is not.
 *
 * <p>
 * <b>On SELECTB-14's "start date".</b> The rule reads "A patient series must be
 * considered start earliest if the start date is before the start date for all
 * other patient series with a start date", and the specification defines no
 * "start date" attribute of its own. The only date in the domain model that can
 * mean "the first day this patient series could be started" is the patient
 * series forecast's earliest date, which is what
 * {@code evaluate_AScorablePatientSeriesCanStartEarliest()} reads, so these
 * tests adopt that reading rather than inventing a second one. Where a test
 * turns on which date is compared it says so.
 */
public class NoValidDosesTest {

  /** Series group names as they appear in the bundled Supporting Data. */
  private static final String STANDARD_GROUP = "Standard";
  private static final String INCREASED_RISK_GROUP = "Increased Risk";

  private static final Date DATE_OF_BIRTH = date(2020, 1, 1);
  private static final Date ASSESSMENT_DATE = date(2024, 6, 1);
  /** Every forecast target dose's maximum age, unless a test overrides it. */
  private static final String DEFAULT_MAXIMUM_AGE = "5 years";
  /** {@link #DEFAULT_MAXIMUM_AGE} after {@link #DATE_OF_BIRTH}. */
  private static final Date AGES_OUT = date(2025, 1, 1);

  private static final String CAN_START_EARLIEST = "evaluate_AScorablePatientSeriesCanStartEarliest";
  private static final String COMPLETABLE = "evaluate_ACandidatePatientSeriesIsCompletable";
  private static final String GENDER_SPECIFIC = "evaluate_ACandidatePatientSeriesGenderSpecific";
  private static final String PRODUCT_SERIES = "evaluate_ACandidatePatientSeriesIsAProductPatientSeries";
  private static final String EXCEEDED_MAXIMUM_AGE = "evaluate_ACandidatePatientSeriesHasExceededTheMaximumAge";

  private DataModel dataModel;
  private Antigen hepB;
  private List<PatientSeries> patientSeriesList;

  @Before
  public void setUp() {
    Patient patient = new Patient();
    patient.setDateOfBirth(DATE_OF_BIRTH);

    dataModel = new DataModel();
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(ASSESSMENT_DATE);
    hepB = dataModel.getOrCreateAntigen("HepB");
    // The state 4.5 and 8.1 leave behind for one antigen pass.
    dataModel.setAntigen(hepB);
    dataModel.setSelectedPatientSeriesList(new ArrayList<PatientSeries>());
    dataModel.setScorablePatientSeriesList(new ArrayList<PatientSeries>());
    // The list 8.6 actually reads.
    patientSeriesList = dataModel.getScorablePatientSeriesList();
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the minimal shape 8.6 actually reads.
  // ---------------------------------------------------------------------

  private static Date date(int year, int month, int day) {
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(year, month - 1, day);
    return calendar.getTime();
  }

  /** A patient series of a named antigen, series group, series type and product path. */
  private PatientSeries series(String seriesName, Antigen targetDisease, String seriesGroupName,
      SeriesType seriesType, String seriesPriority, YesNo productPath) {
    SelectPatientSeries selectPatientSeries = new SelectPatientSeries();
    selectPatientSeries.setSeriesGroupName(seriesGroupName);
    selectPatientSeries.setSeriesGroup(seriesGroupName);
    selectPatientSeries.setSeriesPriority(seriesPriority);
    selectPatientSeries.setProductPath(productPath);

    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setSeriesType(seriesType);
    antigenSeries.setTargetDisease(targetDisease);
    antigenSeries.setSelectPatientSeries(selectPatientSeries);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
    patientSeries.setTargetDoseList(new ArrayList<TargetDose>());
    patientSeriesList.add(patientSeries);
    return patientSeries;
  }

  /**
   * A target dose of the series that no dose has ever been given against - which
   * is every target dose of every patient series 8.6 scores, since 8.3 only
   * routes a series group here when it has no valid doses at all.
   */
  private static TargetDose remainingTargetDose(PatientSeries patientSeries) {
    SeriesDose seriesDose = new SeriesDose();
    seriesDose.setAntigenSeries(patientSeries.getTrackedAntigenSeries());
    seriesDose.setDoseNumber(String.valueOf(patientSeries.getTargetDoseList().size() + 1));
    seriesDose.getAgeList().add(new Age());

    TargetDose targetDose = new TargetDose(seriesDose);
    targetDose.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
    patientSeries.getTargetDoseList().add(targetDose);
    return targetDose;
  }

  /** Table 6-4's Maximum Age for the series dose this target dose tracks. */
  private static void maximumAge(TargetDose targetDose, String maximumAge) {
    targetDose.getTrackedSeriesDose().getAgeList().get(0).setMaximumAge(new TimePeriod(maximumAge));
  }

  /**
   * A minimum interval on the series dose this target dose tracks - one of the
   * "remaining target dose(s)" whose latest minimum interval SELECTB-12 adds to
   * the forecast's earliest date to get the forecast finish date.
   */
  private static void minimumInterval(TargetDose targetDose, String interval) {
    Interval minimumInterval = new Interval();
    minimumInterval.setSeriesDose(targetDose.getTrackedSeriesDose());
    minimumInterval.setMinimumInterval(new TimePeriod(interval));
    targetDose.getTrackedSeriesDose().getIntervalList().add(minimumInterval);
  }

  /** The patient series forecast Chapter 7 leaves behind, forecasting one target dose. */
  private static Forecast forecast(PatientSeries patientSeries, TargetDose forecastTargetDose) {
    Forecast forecast = new Forecast();
    forecast.setTargetDose(forecastTargetDose);
    patientSeries.setForecast(forecast);
    return forecast;
  }

  /**
   * A HepB Standard-group patient series with no valid doses: {@code doseCount}
   * target doses, none of them satisfied, the first of which the forecast
   * targets and which ages out on {@link #AGES_OUT}.
   */
  private PatientSeries noValidDosesSeries(String seriesName, YesNo productPath, int doseCount) {
    PatientSeries patientSeries = series(seriesName, hepB, STANDARD_GROUP, SeriesType.STANDARD, null, productPath);
    TargetDose first = null;
    for (int i = 0; i < doseCount; i++) {
      TargetDose targetDose = remainingTargetDose(patientSeries);
      if (first == null) {
        first = targetDose;
      }
    }
    maximumAge(first, DEFAULT_MAXIMUM_AGE);
    forecast(patientSeries, first);
    return patientSeries;
  }

  /** The default no-valid-doses shape: a two dose, non-product Standard series. */
  private PatientSeries noValidDosesSeries(String seriesName) {
    return noValidDosesSeries(seriesName, YesNo.NO, 2);
  }

  /** SELECTB-14's start date: the first day the patient series could be started. */
  private static PatientSeries startingOn(PatientSeries patientSeries, Date startDate) {
    patientSeries.getForecast().setEarliestDate(startDate);
    return patientSeries;
  }

  /** The date the forecast finish date is read from today - the adjusted past due date. */
  private static PatientSeries finishingOn(PatientSeries patientSeries, Date finishDate) {
    patientSeries.getForecast().setAdjustedPastDueDate(finishDate);
    return patientSeries;
  }

  /** A required gender on the series dose the forecast target dose tracks. */
  private static void requiredGender(PatientSeries patientSeries, String gender) {
    RequiredGender requiredGender = new RequiredGender();
    requiredGender.setSeriesDose(patientSeries.getForecast().getTargetDose().getTrackedSeriesDose());
    requiredGender.setValue(gender);
    patientSeries.getForecast().getTargetDose().getTrackedSeriesDose().getRequiredGenderList().add(requiredGender);
  }

  // ---------------------------------------------------------------------
  // Driving one row of Table 8-11, or the whole step.
  // ---------------------------------------------------------------------

  private void score(String conditionMethod) throws Exception {
    NoValidDoses step = new NoValidDoses(dataModel);
    Method method = NoValidDoses.class.getDeclaredMethod(conditionMethod);
    method.setAccessible(true);
    try {
      method.invoke(step);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception) {
        throw (Exception) e.getCause();
      }
      throw e;
    }
  }

  /** Runs every condition the step scores and reports the step control is handed to. */
  private LogicStepType scoreWholeTable() throws Exception {
    return new NoValidDoses(dataModel).process().getLogicStepType();
  }

  // ---------------------------------------------------------------------
  // Table 8-11, row 1 - "A scorable patient series can start earliest" (+1/0/-1)
  // ---------------------------------------------------------------------

  /**
   * Table 8-11's first row, first and third outcome columns: SELECTB-14 makes a
   * patient series start earliest when its start date is before the start date
   * of every other patient series with a start date, awarded +1, and every other
   * series -1.
   */
  @Test
  public void tableEightElevenCanStartEarliestAloneAwardsPlusOne() throws Exception {
    PatientSeries earliest = startingOn(noValidDosesSeries("HepB earliest"), date(2024, 1, 1));
    PatientSeries later = startingOn(noValidDosesSeries("HepB later"), date(2025, 1, 1));

    score(CAN_START_EARLIEST);

    assertEquals("01/01/2024 is before 01/01/2025", 1, earliest.getScorePatientSeries());
    assertEquals(-1, later.getScorePatientSeries());
  }

  /**
   * SELECTB-14 awards its +1 on a property of the series - its start date is
   * before every other series' - so the series that can start earliest is
   * awarded +1 wherever it happens to sit in the list.
   * {@link #tableEightElevenCanStartEarliestAloneAwardsPlusOne} is the same
   * group in the other order and is the control for this test.
   */
  @Test
  public void tableEightElevenCanStartEarliestIsAwardedWhateverTheSeriesPositionInTheList() throws Exception {
    PatientSeries later = startingOn(noValidDosesSeries("HepB later"), date(2025, 1, 1));
    PatientSeries earliest = startingOn(noValidDosesSeries("HepB earliest"), date(2024, 1, 1));

    score(CAN_START_EARLIEST);

    assertEquals("the +1 is awarded on the start date, not on the series' list position", 1,
        earliest.getScorePatientSeries());
    assertEquals(-1, later.getScorePatientSeries());
  }

  /**
   * Table 8-11's first row, second outcome column: unlike the other two rows
   * this row has a real tie outcome, so two scorable patient series that share
   * the earliest start date are each awarded 0 - neither the +1 of a lone
   * earliest starter nor the -1 of a series that starts later.
   *
   * <p>
   * The two dates here are the same calendar day held in two different objects,
   * which is what two independently computed forecasts produce - see 08-06's
   * Review Findings on the reference-equality comparison in this condition, and
   * {@link #theTieForStartingEarliestIsDetectedWhenTwoSeriesShareOneDateObject},
   * which is the same group with one shared object.
   */
  @Test
  public void tableEightElevenATieForStartingEarliestAwardsZeroToEachTiedSeries() throws Exception {
    PatientSeries first = startingOn(noValidDosesSeries("HepB first"), date(2024, 1, 1));
    PatientSeries second = startingOn(noValidDosesSeries("HepB second"), date(2024, 1, 1));

    score(CAN_START_EARLIEST);

    assertEquals("both series start on 01/01/2024", 0, first.getScorePatientSeries());
    assertEquals(0, second.getScorePatientSeries());
  }

  /**
   * The control for
   * {@link #tableEightElevenATieForStartingEarliestAwardsZeroToEachTiedSeries}:
   * the same two series starting on the same day, differing only in that both
   * forecasts happen to hold the identical {@code Date} object. The
   * specification draws no distinction between the two fixtures - both are two
   * series that start on 01/01/2024 - so both must produce Table 8-11's tie
   * outcome of 0.
   */
  @Test
  public void theTieForStartingEarliestIsDetectedWhenTwoSeriesShareOneDateObject() throws Exception {
    Date sharedStartDate = date(2024, 1, 1);
    PatientSeries first = startingOn(noValidDosesSeries("HepB first"), sharedStartDate);
    PatientSeries second = startingOn(noValidDosesSeries("HepB second"), sharedStartDate);

    score(CAN_START_EARLIEST);

    assertEquals(0, first.getScorePatientSeries());
    assertEquals(0, second.getScorePatientSeries());
  }

  /**
   * Table 8-11's first row is scored for every scorable patient series in the
   * group, so whether the row is scored at all cannot depend on what one
   * particular series carries. A patient series forecast has an earliest date
   * only where Chapter 7 got as far as computing one, so a group in which the
   * first series has none is ordinary rather than exotic - and the remaining
   * series must still be scored on their own start dates.
   */
  @Test
  public void theRowIsScoredForEverySeriesEvenWhenTheFirstSeriesHasNoStartDate() throws Exception {
    PatientSeries noStartDate = noValidDosesSeries("HepB no start date");
    PatientSeries onlyStarter = startingOn(noValidDosesSeries("HepB starts 2024"), date(2024, 1, 1));

    score(CAN_START_EARLIEST);

    assertEquals("the only series with a start date starts earliest of them", 1,
        onlyStarter.getScorePatientSeries());
    assertEquals(-1, noStartDate.getScorePatientSeries());
  }

  /**
   * SELECTB-14 compares this series' start date against "the start date for all
   * other patient series <b>with a start date</b>", so a series carrying no
   * start date is not part of the comparison - it neither takes the +1 from the
   * series that does start earliest nor prevents it being awarded. Having no
   * start date, it cannot itself be shown to start earliest, so it falls in the
   * row's "not true" column at -1.
   */
  @Test
  public void selectbFourteenASeriesWithNoStartDateFallsInTheNotTrueColumn() throws Exception {
    PatientSeries onlyStarter = startingOn(noValidDosesSeries("HepB starts 2024"), date(2024, 1, 1));
    PatientSeries noStartDate = noValidDosesSeries("HepB no start date");

    score(CAN_START_EARLIEST);

    assertEquals(1, onlyStarter.getScorePatientSeries());
    assertEquals(-1, noStartDate.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Table 8-11, row 2 - "A scorable patient series is completable" (+1/n/a/-1)
  //
  // NoValidDosesCompletableTest already covers this row's two basic outcomes;
  // what follows is the rest of SELECTB-3 and SELECTB-12.
  // ---------------------------------------------------------------------

  /**
   * SELECTB-3's comparison is strict - the forecast finish date must be "less
   * than the maximum age date of the last target dose", not on or before it - so
   * a series that cannot finish until the very day it ages out is not
   * completable.
   */
  @Test
  public void selectbThreeTheComparisonIsStrictSoFinishingOnTheMaximumAgeDateIsNotCompletable() throws Exception {
    PatientSeries finishesOnTheDay = finishingOn(noValidDosesSeries("HepB on the day"), AGES_OUT);

    score(COMPLETABLE);

    assertEquals(-1, finishesOnTheDay.getScorePatientSeries());
  }

  /**
   * SELECTB-3's maximum age date is the whole maximum age applied to the date of
   * birth, compound periods included. The bundled 4.65-508 release defines 77
   * {@code <maxAge>} values and 8 of them are "8 months + 1 day", so a patient
   * born 01/01/2020 ages out of those series doses on 09/02/2020, not
   * 09/01/2020 - and a series that finishes on 09/01/2020 is still completable.
   *
   * <p>
   * {@code TimePeriod.getDateFrom(Date)} already computes this correctly, child
   * period and all; {@code NoValidDoses.addTimePeriodtotoDate()} is a byte-wise
   * copy of {@code InProcessPatientSeries}' second implementation of the same
   * calculation, which reads only the outermost amount and type. See the
   * 2026-09-05 "SELECTB-3's maximum age date is calculated three times" entry in
   * {@code cdsi-reference/step-tests/cross-cutting-notes.md}, which predicted
   * this from 8.5's side.
   */
  @Test
  public void selectbThreeTheMaximumAgeDateIncludesEveryPartOfACompoundMaximumAge() throws Exception {
    PatientSeries patientSeries = noValidDosesSeries("HepB compound maximum age");
    maximumAge(patientSeries.getForecast().getTargetDose(), "8 months + 1 day");
    finishingOn(patientSeries, date(2020, 9, 1));

    score(COMPLETABLE);

    assertEquals("finishes 09/01/2020, ages out 09/02/2020", 1, patientSeries.getScorePatientSeries());
  }

  /**
   * SELECTB-3 measures the forecast finish date against "the maximum age date of
   * the <b>last</b> target dose" - the age at which the series as a whole can no
   * longer be completed - not against the maximum age of whichever dose the
   * forecast happens to be recommending next. A series whose next dose ages out
   * in 2024 but whose final dose may still be given until 2040 is completable if
   * it can finish before 2040.
   */
  @Test
  public void selectbThreeCompletabilityIsMeasuredAgainstTheLastTargetDosesMaximumAgeDate() throws Exception {
    PatientSeries patientSeries = series("HepB three dose", hepB, STANDARD_GROUP, SeriesType.STANDARD, null,
        YesNo.NO);
    TargetDose nextDose = remainingTargetDose(patientSeries);
    maximumAge(nextDose, "4 years");
    TargetDose lastDose = remainingTargetDose(patientSeries);
    maximumAge(lastDose, "20 years");
    forecast(patientSeries, nextDose);
    finishingOn(patientSeries, date(2030, 1, 1));

    score(COMPLETABLE);

    assertEquals("the series finishes 01/01/2030 and its last target dose ages out 01/01/2040", 1,
        patientSeries.getScorePatientSeries());
  }

  /**
   * SELECTB-12 defines the forecast finish date this row rests on: "the earliest
   * date of the patient series forecast made from the scorable patient series
   * plus the latest minimum interval from the remaining target dose(s)". It is
   * not the adjusted past due date, which is Table 7-12's due date for the
   * <i>next</i> dose alone and says nothing about when the series finishes.
   *
   * <p>
   * The fixture separates the two on purpose: the earliest date plus the latest
   * of the two remaining doses' minimum intervals is 07/01/2024, comfortably
   * inside the 01/01/2025 maximum age date, while the adjusted past due date is
   * deliberately set to a later date that is not the finish date of anything.
   */
  @Test
  public void selectbTwelveTheForecastFinishDateIsTheEarliestDatePlusTheLatestMinimumIntervalRemaining()
      throws Exception {
    PatientSeries patientSeries = series("HepB two doses left", hepB, STANDARD_GROUP, SeriesType.STANDARD, null,
        YesNo.NO);
    TargetDose nextDose = remainingTargetDose(patientSeries);
    minimumInterval(nextDose, "4 weeks");
    maximumAge(nextDose, DEFAULT_MAXIMUM_AGE);
    TargetDose lastDose = remainingTargetDose(patientSeries);
    minimumInterval(lastDose, "6 months");
    maximumAge(lastDose, DEFAULT_MAXIMUM_AGE);
    Forecast forecast = forecast(patientSeries, nextDose);
    forecast.setEarliestDate(date(2024, 1, 1));
    forecast.setAdjustedPastDueDate(date(2026, 1, 1));

    score(COMPLETABLE);

    assertEquals("01/01/2024 plus the latest remaining minimum interval of 6 months is before 01/01/2025", 1,
        patientSeries.getScorePatientSeries());
  }

  /**
   * Table 8-11's second row has "n/a" in its tie column, so two completable
   * series are each awarded the full +1 - completability is a property of one
   * series measured against its own maximum age date, not a comparison between
   * series.
   */
  @Test
  public void tableEightElevenTheCompletableRowHasNoTieOutcomeSoTwoCompletableSeriesBothScorePlusOne()
      throws Exception {
    PatientSeries first = finishingOn(noValidDosesSeries("HepB first"), date(2024, 1, 1));
    PatientSeries second = finishingOn(noValidDosesSeries("HepB second"), date(2024, 6, 1));

    score(COMPLETABLE);

    assertEquals(1, first.getScorePatientSeries());
    assertEquals(1, second.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Table 8-11, row 3 - "A scorable patient series is a product patient
  // series" (-1/n/a/+1)
  //
  // Note the inverted signs: in this table being a product patient series is a
  // penalty. Table 8-11's own row reads "-1 | n/a | +1".
  // ---------------------------------------------------------------------

  /**
   * Table 8-11's third row, first outcome column: SELECTB-23 makes a patient
   * series a product patient series when the product path flag is 'Y' for the
   * select patient series, and in <i>this</i> table that costs the series a
   * point.
   */
  @Test
  public void tableEightElevenAProductPatientSeriesAwardsMinusOne() throws Exception {
    PatientSeries productSeries = noValidDosesSeries("HepB product", YesNo.YES, 2);

    score(PRODUCT_SERIES);

    assertEquals("Table 8-11 penalises a product patient series", -1, productSeries.getScorePatientSeries());
  }

  /**
   * Table 8-11's third row, third outcome column: a patient series whose product
   * path flag is not 'Y' is not a product patient series, and in this table that
   * earns it a point.
   */
  @Test
  public void tableEightElevenASeriesThatIsNotAProductPatientSeriesAwardsPlusOne() throws Exception {
    PatientSeries notAProductSeries = noValidDosesSeries("HepB standard", YesNo.NO, 2);

    score(PRODUCT_SERIES);

    assertEquals(1, notAProductSeries.getScorePatientSeries());
  }

  /**
   * SELECTB-23 asks whether <i>this</i> patient series is a product patient
   * series - "the product path flag is 'Y' for the select patient series" - so
   * the answer must come from the series being scored. A series whose product
   * path is 'N' is not a product patient series, whatever the series scored
   * before it was.
   */
  @Test
  public void selectbTwentyThreeTheProductPathFlagIsReadFromTheSeriesBeingScored() throws Exception {
    PatientSeries productSeries = noValidDosesSeries("HepB product", YesNo.YES, 2);
    PatientSeries notAProductSeries = noValidDosesSeries("HepB standard", YesNo.NO, 2);

    score(PRODUCT_SERIES);

    assertEquals(-1, productSeries.getScorePatientSeries());
    assertEquals("a product path of 'N' is not a product patient series, whoever was scored first", 1,
        notAProductSeries.getScorePatientSeries());
  }

  /**
   * The same question asked without reference to Table 8-11's sign convention,
   * so that the row's two defects can be told apart: whichever way round the
   * points fall, a row whose whole purpose is to separate product patient series
   * from the rest must give the two kinds of series different scores.
   *
   * <p>
   * This is the sibling of the sticky-flag defect
   * {@code InProcessPatientSeriesTest} records for 8.5's first row - here
   * {@code productPatientSeries} is likewise declared outside the per-series
   * loop and never reset, so once one series in the list sets it, every series
   * scored after it inherits it.
   */
  @Test
  public void theProductRowMustDiscriminateBetweenAProductSeriesAndOneThatIsNot() throws Exception {
    PatientSeries productSeries = noValidDosesSeries("HepB product", YesNo.YES, 2);
    PatientSeries notAProductSeries = noValidDosesSeries("HepB standard", YesNo.NO, 2);

    score(PRODUCT_SERIES);

    assertTrue(
        "Table 8-11 scores a product patient series below one that is not, but the two scored "
            + productSeries.getScorePatientSeries() + " and " + notAProductSeries.getScorePatientSeries(),
        productSeries.getScorePatientSeries() < notAProductSeries.getScorePatientSeries());
  }

  /**
   * Table 8-11's third row also has "n/a" in its tie column: being a product
   * patient series is a property of one series' own select patient series, not a
   * comparison between series, so two product patient series are each awarded
   * the full -1 rather than being treated as tied.
   */
  @Test
  public void tableEightElevenTheProductRowHasNoTieOutcomeSoTwoProductSeriesBothScoreMinusOne() throws Exception {
    PatientSeries first = noValidDosesSeries("HepB product one", YesNo.YES, 2);
    PatientSeries second = noValidDosesSeries("HepB product two", YesNo.YES, 2);

    score(PRODUCT_SERIES);

    assertEquals(-1, first.getScorePatientSeries());
    assertEquals(-1, second.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Conditions Table 8-11 does not define
  //
  // 08-06's Review Findings record these as an open question: they may be a
  // deliberate undocumented refinement or a copy-forward from another section.
  // These two tests take the specification at its word - Table 8-11 has three
  // rows - and are the concrete evidence for whichever way that question is
  // settled.
  // ---------------------------------------------------------------------

  /**
   * Table 8-11 defines three conditions and a gender match is not one of them,
   * so a scorable patient series whose forecast target dose requires a gender
   * the patient happens to match must come out of 8.6 scored on the three rows
   * the table does define and nothing else.
   *
   * <p>
   * The implementation runs
   * {@code evaluate_ACandidatePatientSeriesGenderSpecific()}, which awards a
   * bonus point for exactly this. Gender is genuinely relevant elsewhere in the
   * specification - Table 6-5's Gender evaluation - but not in this table.
   */
  @Test
  public void tableEightElevenDefinesNoGenderMatchCondition() throws Exception {
    dataModel.getPatient().setGender("F");
    PatientSeries genderSpecific = noValidDosesSeries("HepB female only");
    requiredGender(genderSpecific, "F");

    score(GENDER_SPECIFIC);

    assertEquals("Table 8-11 awards no points for a gender match", 0, genderSpecific.getScorePatientSeries());
  }

  /**
   * Table 8-11 likewise defines no condition about the maximum age to start, so
   * a scorable patient series that is already past the maximum age date of its
   * forecast target dose must not be penalised by 8.6 for it. Whether such a
   * series should have reached Chapter 8's scoring at all is 7.4's question
   * (Table 7-10's maximum age gate) and 8.1's, not this table's.
   *
   * <p>
   * The implementation runs
   * {@code evaluate_ACandidatePatientSeriesHasExceededTheMaximumAge()}, which
   * decrements for a series past that date and - the half that touches every
   * series, not only the aged-out ones - increments for every series that is
   * not.
   */
  @Test
  public void tableEightElevenDefinesNoMaximumAgeToStartCondition() throws Exception {
    PatientSeries agedOut = noValidDosesSeries("HepB aged out");
    maximumAge(agedOut.getForecast().getTargetDose(), "1 year");
    PatientSeries withinAge = noValidDosesSeries("HepB within age");

    score(EXCEEDED_MAXIMUM_AGE);

    assertEquals("assessed 06/01/2024, past a maximum age date of 01/01/2021 - not a Table 8-11 condition", 0,
        agedOut.getScorePatientSeries());
    assertEquals("assessed 06/01/2024, inside a maximum age date of 01/01/2025 - also not a condition", 0,
        withinAge.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Purpose and Entry Conditions - the scope 8.6 scores over
  // ---------------------------------------------------------------------

  /**
   * Table 8-11's title asks how many points are awarded "to a <b>Scorable</b>
   * Patient Series that has No Valid Doses", and 8.3's Table 8-5 - the only
   * route into this step - hands 8.6 the series it counted in the scorable list.
   * The scorable patient series are what 8.1 produced: SELECTSCORE-2 keeps only
   * the highest-priority Risk series of a series group, so a priority "B" Risk
   * series alongside a priority "A" one is not scorable and must not take the
   * start-earliest point from the series that is.
   *
   * <p>
   * 8.6 reads {@code dataModel.getPatientSeriesStepper().getList()} - 5.1's
   * unfiltered list of every relevant patient series for every antigen, three
   * pipeline stages earlier than the scorable list. See the 2026-09-05 "Chapter
   * 8 has no series group" entry in
   * {@code cdsi-reference/step-tests/cross-cutting-notes.md}.
   */
  @Test
  public void theStepScoresTheScorablePatientSeriesEightOneProducedNotEveryRelevantSeries() throws Exception {
    PatientSeries scorable = series("HepB risk priority A", hepB, INCREASED_RISK_GROUP, SeriesType.RISK, "A",
        YesNo.NO);
    TargetDose scorableDose = remainingTargetDose(scorable);
    maximumAge(scorableDose, DEFAULT_MAXIMUM_AGE);
    forecast(scorable, scorableDose);
    startingOn(scorable, date(2024, 1, 1));

    PatientSeries dropped = series("HepB risk priority B", hepB, INCREASED_RISK_GROUP, SeriesType.RISK, "B",
        YesNo.NO);
    TargetDose droppedDose = remainingTargetDose(dropped);
    maximumAge(droppedDose, DEFAULT_MAXIMUM_AGE);
    forecast(dropped, droppedDose);
    startingOn(dropped, date(2023, 1, 1));

    // What 8.1 leaves behind: the priority B risk series is not scorable.
    dataModel.getScorablePatientSeriesList().remove(dropped);

    score(CAN_START_EARLIEST);

    assertEquals("only the scorable patient series compete for the earliest start date", 1,
        scorable.getScorePatientSeries());
  }

  /**
   * 4.5 and Figure 4-7 wrap Chapter 8 in a per-antigen loop, and 8.6's rules are
   * all phrased over the patient series of one series group of one antigen. A
   * run of 8.6 for HepB must therefore find HepB's own earliest starter,
   * whatever series of some other antigen are still sitting in the stepper's
   * list.
   */
  @Test
  public void theStepScoresThePatientSeriesOfTheAntigenBeingProcessed() throws Exception {
    Antigen measles = dataModel.getOrCreateAntigen("Measles");
    PatientSeries hepBWinner = startingOn(noValidDosesSeries("HepB standard"), date(2024, 1, 1));
    startingOn(noValidDosesSeries("HepB alternate"), date(2025, 1, 1));

    PatientSeries measlesSeries = series("Measles standard", measles, STANDARD_GROUP, SeriesType.STANDARD, null,
        YesNo.NO);
    TargetDose measlesDose = remainingTargetDose(measlesSeries);
    maximumAge(measlesDose, DEFAULT_MAXIMUM_AGE);
    forecast(measlesSeries, measlesDose);
    startingOn(measlesSeries, date(2023, 1, 1));
    // What 8.1 (fed by SelectNextSeriesGroup's antigen scoping) actually leaves
    // behind - no other antigen's series can reach this list.
    dataModel.getScorablePatientSeriesList().remove(measlesSeries);

    score(CAN_START_EARLIEST);

    assertEquals("8.6 runs inside 4.5's per-antigen loop, so only HepB's series are in scope", 1,
        hepBWinner.getScorePatientSeries());
  }

  /**
   * Chapter 8's overview: "Process steps 8.1 through 8.7 are repeated <b>for
   * each series group</b> to identify one prioritized patient series per series
   * group." A run of 8.6 for the Standard group must therefore find its own
   * earliest starter whatever the Increased Risk group holds.
   */
  @Test
  public void theStepScoresThePatientSeriesOfOneSeriesGroup() throws Exception {
    PatientSeries standardWinner = startingOn(noValidDosesSeries("HepB standard earliest"), date(2024, 1, 1));
    startingOn(noValidDosesSeries("HepB standard later"), date(2025, 1, 1));

    PatientSeries increasedRisk = series("HepB increased risk", hepB, INCREASED_RISK_GROUP, SeriesType.RISK, "A",
        YesNo.NO);
    TargetDose riskDose = remainingTargetDose(increasedRisk);
    maximumAge(riskDose, DEFAULT_MAXIMUM_AGE);
    forecast(increasedRisk, riskDose);
    startingOn(increasedRisk, date(2023, 1, 1));
    // SelectNextSeriesGroup hands 8.1 (and, through it, 8.6) one series group at
    // a time; simulate the Standard group's own pass.
    dataModel.getScorablePatientSeriesList().remove(increasedRisk);

    score(CAN_START_EARLIEST);

    assertEquals("one run of 8.6 scores one series group, whose earliest starter starts 01/01/2024", 1,
        standardWinner.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // State Changes
  // ---------------------------------------------------------------------

  /**
   * 08-06's State Changes: the rows of Table 8-11 all write to one running
   * integer per patient series. A single scorable patient series that starts
   * earliest (it is the only one), is completable, and is a product patient
   * series scores +1 +1 -1 = +1, and nothing else - Table 8-11 has three rows.
   *
   * <p>
   * This is the whole-step counterpart of
   * {@link #tableEightElevenDefinesNoGenderMatchCondition} and
   * {@link #tableEightElevenDefinesNoMaximumAgeToStartCondition}: run through
   * {@code process()}, the two undocumented conditions are indistinguishable
   * from the three documented ones except in the total.
   */
  @Test
  public void theWholeTableAwardsOnlyTheThreeConditionsTableEightElevenDefines() throws Exception {
    PatientSeries patientSeries = noValidDosesSeries("HepB product", YesNo.YES, 2);
    startingOn(patientSeries, date(2024, 1, 1));
    finishingOn(patientSeries, date(2024, 6, 1));

    scoreWholeTable();

    assertEquals("+1 starts earliest, +1 completable, -1 product patient series", 1,
        patientSeries.getScorePatientSeries());
  }

  /**
   * The three rows accumulate into one running score, so a scorable patient
   * series that starts later than another, cannot finish before it ages out, and
   * is a product patient series scores -1 -1 -1 = -3.
   */
  @Test
  public void theThreeTableEightElevenRowsAccumulateIntoOneRunningScore() throws Exception {
    PatientSeries winner = noValidDosesSeries("HepB standard", YesNo.NO, 2);
    startingOn(winner, date(2024, 1, 1));
    finishingOn(winner, date(2024, 6, 1));

    PatientSeries loser = noValidDosesSeries("HepB product alternate", YesNo.YES, 2);
    startingOn(loser, date(2025, 1, 1));
    finishingOn(loser, date(2026, 1, 1));

    scoreWholeTable();

    assertEquals("does not start earliest, is not completable, and is a product patient series", -3,
        loser.getScorePatientSeries());
  }

  /**
   * 08-06's State Changes names {@code PatientSeries.incPatientScoreSeries()} /
   * {@code descPatientScoreSeries()} - a running integer score field consumed
   * later by 8.7 - so Table 8-11's outcomes are applied to whatever score the
   * series already carries rather than replacing it.
   *
   * <p>
   * Worth pinning explicitly because the score is never reset between
   * selections - see the 2026-09-02 "Patient series scores accumulate across a
   * whole assessment" entry in
   * {@code cdsi-reference/step-tests/cross-cutting-notes.md}, which this step's
   * own SPEC-4.6-0007 investigation raised. Whether that accumulation is correct
   * is not settled here, but it is the reason the tie column of row one ("0")
   * cannot be implemented by leaving a series untouched.
   */
  @Test
  public void theScoreIsARunningTotalTheStepIncrementsOrDecrementsRatherThanSets() throws Exception {
    PatientSeries completable = finishingOn(noValidDosesSeries("HepB completable"), date(2024, 1, 1));
    completable.setScorePatientSeriesScore(5);

    score(COMPLETABLE);

    assertEquals("+1 is added to the running score, not assigned to it", 6, completable.getScorePatientSeries());
  }

  /**
   * 08-06's State Changes names the score field and nothing else. Whichever
   * columns of Table 8-11 apply, each patient series' status, its target doses
   * and their statuses, its forecast and the list itself must come back exactly
   * as Chapter 7 and 8.1 left them, and neither the prioritized nor the scorable
   * patient series list may gain an entry - selecting a prioritized series
   * belongs to 8.7, which {@code next()} constructs but does not run.
   */
  @Test
  public void theStepChangesNoPatientSeriesStateOtherThanTheScore() throws Exception {
    PatientSeries first = noValidDosesSeries("HepB standard", YesNo.NO, 2);
    startingOn(first, date(2024, 1, 1));
    finishingOn(first, date(2024, 6, 1));
    PatientSeries second = noValidDosesSeries("HepB alternate", YesNo.YES, 3);
    startingOn(second, date(2025, 1, 1));
    finishingOn(second, date(2026, 1, 1));
    int scorableSizeBeforeScoring = dataModel.getScorablePatientSeriesList().size();

    scoreWholeTable();

    assertEquals("the patient series list is not re-filtered here", 2, patientSeriesList.size());
    assertEquals(PatientSeriesStatus.NOT_COMPLETE, first.getPatientSeriesStatus());
    assertEquals(PatientSeriesStatus.NOT_COMPLETE, second.getPatientSeriesStatus());
    assertEquals(2, first.getTargetDoseList().size());
    assertEquals(3, second.getTargetDoseList().size());
    assertEquals(TargetDoseStatus.NOT_SATISFIED, first.getTargetDoseList().get(0).getTargetDoseStatus());
    assertEquals(TargetDoseStatus.NOT_SATISFIED, second.getTargetDoseList().get(2).getTargetDoseStatus());
    assertEquals(date(2024, 1, 1), first.getForecast().getEarliestDate());
    assertEquals(date(2024, 6, 1), first.getForecast().getAdjustedPastDueDate());
    assertTrue("8.6 prioritizes no patient series", dataModel.getPrioritizedPatientSeriesList().isEmpty());
    assertEquals("8.6 does not re-derive the scorable patient series list", scorableSizeBeforeScoring,
        dataModel.getScorablePatientSeriesList().size());
  }

  // ---------------------------------------------------------------------
  // Next Steps
  // ---------------------------------------------------------------------

  /**
   * 08-06's Next Steps: unconditional to 8.7 Select Prioritized Patient Series,
   * per {@code transitions.yaml}. "Unconditional" means whatever the scoring
   * found - a lone winner of every row, or a group tied on all of them.
   */
  @Test
  public void theStepTransitionsUnconditionallyToSelectPrioritizedPatientSeries() throws Exception {
    PatientSeries winner = noValidDosesSeries("HepB standard", YesNo.NO, 2);
    startingOn(winner, date(2024, 1, 1));
    finishingOn(winner, date(2024, 6, 1));
    PatientSeries loser = noValidDosesSeries("HepB alternate", YesNo.YES, 2);
    startingOn(loser, date(2025, 1, 1));
    finishingOn(loser, date(2026, 1, 1));
    assertEquals("a lone winner goes to 8.7", LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES, scoreWholeTable());

    setUp();
    Date sharedDate = date(2024, 1, 1);
    PatientSeries first = noValidDosesSeries("HepB first", YesNo.NO, 2);
    startingOn(first, sharedDate);
    finishingOn(first, date(2024, 6, 1));
    PatientSeries second = noValidDosesSeries("HepB second", YesNo.NO, 2);
    startingOn(second, sharedDate);
    finishingOn(second, date(2024, 6, 1));
    assertEquals("a group tied on every row goes to 8.7", LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES,
        scoreWholeTable());
  }
}
