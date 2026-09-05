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
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

/**
 * Section 8.5 "In-process Patient Series" (Logic Specification for ACIP
 * Recommendations v4.6, page 90; Table 8-9 "How Many Points Are Awarded to a
 * Scorable Patient Series That Is an In-process Patient Series?" and Table 8-10
 * business rules SELECTB-2, SELECTB-3, SELECTB-5, SELECTB-11, SELECTB-12,
 * SELECTB-16, SELECTB-19 and SELECTB-23) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/08-05-in-process-patient-series/index.md}.
 *
 * <p>
 * 8.5 is the middle member of the 8.4/8.5/8.6 scoring family and carries the
 * largest rule set of the three. It runs when 8.3 found two or more in-process
 * patient series and no complete patient series in the series group, and it
 * scores five independent conditions onto one running integer per patient
 * series, {@code PatientSeries.getScorePatientSeries()}, which 8.7 later
 * consumes:
 *
 * <pre>
 * Is a product series AND has all valid doses   +2 / n/a / -2
 * Is completable                                +3 / n/a / -3
 * Has the most valid doses                      +2 /  0  / -2
 * Is closest to completion                      +2 /  0  / -2
 * Can finish earliest                           +1 /  0  / -1
 * </pre>
 *
 * <p>
 * <b>Isolation.</b> Each of Table 8-9's five rows has its own private method on
 * {@link InProcessPatientSeries}, and {@code process()} calls all five in
 * order. Testing a single row through {@code process()} would mean asserting a
 * sum of five outcomes, so every row test invokes its own method reflectively -
 * the precedent {@link NoValidDosesCompletableTest} set for this class's sibling
 * in 8.6 - and only the State Changes and Next Steps tests at the bottom drive
 * the public {@code process()}. Either way the fixture is hand-built: no
 * Supporting Data release, no loader and no upstream step is involved. The step
 * reads {@code dataModel.getPatientSeriesStepper().getList()}, each series'
 * {@code targetDoseList}, its tracked {@code AntigenSeries} (for the series name
 * it logs and the product path flag), its {@code Forecast}, and the patient's
 * date of birth.
 *
 * <p>
 * <b>Dates.</b> Every fixture patient is born 01/01/2020 and every forecast
 * target dose ages out five years later, on 01/01/2025, unless a test says
 * otherwise - so "finishes 01/01/2024" is completable and "finishes 01/01/2026"
 * is not.
 *
 * <p>
 * <b>Note on list order.</b> Several tests below build the same group in both
 * possible orders and assert the same outcome for each. That is deliberate
 * rather than redundant: Table 8-9 awards points on properties of a series, not
 * on where the series sits in a list, so a conforming implementation must answer
 * the same way either way.
 */
public class InProcessPatientSeriesTest {

  /** Series group names as they appear in the bundled Supporting Data. */
  private static final String STANDARD_GROUP = "Standard";
  private static final String INCREASED_RISK_GROUP = "Increased Risk";

  private static final Date DATE_OF_BIRTH = date(2020, 1, 1);
  /** Every forecast target dose's maximum age, unless a test overrides it. */
  private static final String DEFAULT_MAXIMUM_AGE = "5 years";
  /** {@link #DEFAULT_MAXIMUM_AGE} after {@link #DATE_OF_BIRTH}. */
  private static final Date AGES_OUT = date(2025, 1, 1);

  private static final String PRODUCT_AND_ALL_VALID_DOSES =
      "evaluate_ACandidatePatientSeriesIsAProductPatientSeriesAndHasAllValidDoses";
  private static final String COMPLETABLE = "evaluate_ACandidatePatientSeriesIsCompletable";
  private static final String MOST_VALID_DOSES = "evaluate_ACandidatePatientSeriesHasTheMostValidDoses";
  private static final String CLOSEST_TO_COMPLETION = "evaluate_ACandidatePatientSeriesIsClosestToCompletion";
  private static final String CAN_FINISH_EARLIEST = "evaluate_ACandidatePatientSeriesCanFinishEarliest";

  private DataModel dataModel;
  private Antigen hepB;
  private List<PatientSeries> patientSeriesList;

  @Before
  public void setUp() {
    Patient patient = new Patient();
    patient.setDateOfBirth(DATE_OF_BIRTH);

    dataModel = new DataModel();
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date(2024, 6, 1));
    hepB = dataModel.getOrCreateAntigen("HepB");
    // The state 4.5 and 8.1 leave behind for one antigen pass.
    dataModel.setAntigen(hepB);
    dataModel.setSelectedPatientSeriesList(new ArrayList<PatientSeries>());
    dataModel.setScorablePatientSeriesList(new ArrayList<PatientSeries>());
    // The list 8.5 actually reads.
    patientSeriesList = dataModel.getPatientSeriesStepper().getList();
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the minimal shape 8.5 actually reads.
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

  /** A HepB patient series of the Standard series group. */
  private PatientSeries standardSeries(String seriesName, YesNo productPath) {
    return series(seriesName, hepB, STANDARD_GROUP, SeriesType.STANDARD, null, productPath);
  }

  private static TargetDose newTargetDose(PatientSeries patientSeries) {
    SeriesDose seriesDose = new SeriesDose();
    seriesDose.setAntigenSeries(patientSeries.getTrackedAntigenSeries());
    seriesDose.setDoseNumber(String.valueOf(patientSeries.getTargetDoseList().size() + 1));
    seriesDose.getAgeList().add(new Age());

    TargetDose targetDose = new TargetDose(seriesDose);
    patientSeries.getTargetDoseList().add(targetDose);
    return targetDose;
  }

  /**
   * A target dose satisfied by a vaccine dose administered that 6.10 evaluated
   * 'Valid' - one valid dose, in the state both SELECTB-2 and SELECTB-19's valid
   * dose count read.
   */
  private static TargetDose satisfiedTargetDose(PatientSeries patientSeries) {
    TargetDose targetDose = newTargetDose(patientSeries);
    targetDose.setTargetDoseStatus(TargetDoseStatus.SATISFIED);
    targetDose.setSatisfiedByVaccineDoseAdministered(new VaccineDoseAdministered());
    Evaluation evaluation = new Evaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.VALID);
    targetDose.setEvaluation(evaluation);
    return targetDose;
  }

  /**
   * A target dose of the series that no dose has yet been given against - the
   * remainder of an in-process patient series. It carries no evaluation at all,
   * because nothing has been evaluated against it.
   */
  private static TargetDose remainingTargetDose(PatientSeries patientSeries) {
    TargetDose targetDose = newTargetDose(patientSeries);
    targetDose.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
    return targetDose;
  }

  /**
   * A target dose a vaccine dose administered was evaluated against and failed -
   * 6.10 recorded an evaluation status of 'Not Valid', so this series does
   * <i>not</i> have all valid doses.
   */
  private static TargetDose notValidTargetDose(PatientSeries patientSeries) {
    TargetDose targetDose = newTargetDose(patientSeries);
    targetDose.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
    Evaluation evaluation = new Evaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.NOT_VALID);
    evaluation.setVaccineDoseAdministered(new VaccineDoseAdministered());
    targetDose.setEvaluation(evaluation);
    return targetDose;
  }

  /** Table 6-4's Maximum Age for the series dose this target dose tracks. */
  private static void maximumAge(TargetDose targetDose, String maximumAge) {
    targetDose.getTrackedSeriesDose().getAgeList().get(0).setMaximumAge(new TimePeriod(maximumAge));
  }

  /**
   * A minimum interval on the series dose this target dose tracks - one of the
   * "remaining doses" whose latest minimum interval SELECTB-12 adds to the
   * earliest date to get the forecast finish date.
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
   * An in-process patient series (SELECTB-16): {@code satisfiedDoses} target
   * doses satisfied by valid doses administered, plus {@code remainingDoses}
   * target doses still to be given, the first of which the forecast targets. The
   * forecast target dose ages out on {@link #AGES_OUT}.
   */
  private PatientSeries inProcessSeries(String seriesName, YesNo productPath, int satisfiedDoses,
      int remainingDoses) {
    PatientSeries patientSeries = standardSeries(seriesName, productPath);
    for (int i = 0; i < satisfiedDoses; i++) {
      satisfiedTargetDose(patientSeries);
    }
    TargetDose next = null;
    for (int i = 0; i < remainingDoses; i++) {
      TargetDose remaining = remainingTargetDose(patientSeries);
      if (next == null) {
        next = remaining;
      }
    }
    maximumAge(next, DEFAULT_MAXIMUM_AGE);
    forecast(patientSeries, next);
    return patientSeries;
  }

  /** The default in-process shape: some valid doses, one dose still to come. */
  private PatientSeries inProcessSeries(String seriesName, int satisfiedDoses) {
    return inProcessSeries(seriesName, YesNo.NO, satisfiedDoses, 1);
  }

  /** The date the forecast finish date is read from today - the adjusted past due date. */
  private static PatientSeries finishingOn(PatientSeries patientSeries, Date finishDate) {
    patientSeries.getForecast().setAdjustedPastDueDate(finishDate);
    return patientSeries;
  }

  private static PatientSeries withLatestDate(PatientSeries patientSeries, Date latestDate) {
    patientSeries.getForecast().setLatestDate(latestDate);
    return patientSeries;
  }

  // ---------------------------------------------------------------------
  // Driving one row of Table 8-9, or the whole step.
  // ---------------------------------------------------------------------

  private void score(String conditionMethod) throws Exception {
    InProcessPatientSeries step = new InProcessPatientSeries(dataModel);
    Method method = InProcessPatientSeries.class.getDeclaredMethod(conditionMethod);
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

  /** Runs all five rows of Table 8-9 and reports the step control is handed to. */
  private LogicStepType scoreWholeTable() throws Exception {
    return new InProcessPatientSeries(dataModel).process().getLogicStepType();
  }

  // ---------------------------------------------------------------------
  // Table 8-9, row 1 - "Is a product series AND has all valid doses" (+2/n/a/-2)
  // ---------------------------------------------------------------------

  /**
   * Table 8-9's first row, first outcome column: a scorable patient series that
   * is a product patient series (SELECTB-23, product path flag 'Y') and has all
   * valid doses (SELECTB-2) is awarded +2.
   *
   * <p>
   * The fixture is a genuine in-process patient series, because that is the only
   * kind of series 8.5 scores: SELECTB-16 makes a series in-process when at
   * least one target dose is satisfied and the series is not complete, so an
   * in-process patient series always has at least one target dose still to be
   * given. SELECTB-2 is therefore a statement about the doses that <i>have</i>
   * been administered and evaluated - every one of them 'Valid' - and cannot
   * mean "every target dose in the series is satisfied", because on that reading
   * the +2 column of a table about in-process patient series would be
   * unreachable by construction.
   */
  @Test
  public void tableEightNineAProductSeriesWhoseAdministeredDosesAreAllValidAwardsPlusTwo() throws Exception {
    PatientSeries productSeries = inProcessSeries("HepB product", YesNo.YES, 2, 1);

    score(PRODUCT_AND_ALL_VALID_DOSES);

    assertEquals("a product patient series with no invalid dose administered against it", 2,
        productSeries.getScorePatientSeries());
  }

  /**
   * Table 8-9's first row, third outcome column: the condition is not true, so
   * -2. SELECTB-23 makes a patient series a product patient series only when its
   * product path flag is 'Y', so a series whose flag is 'N' fails the row
   * however valid its doses are.
   */
  @Test
  public void tableEightNineASeriesThatIsNotAProductSeriesAwardsMinusTwo() throws Exception {
    PatientSeries notAProductSeries = inProcessSeries("HepB standard", YesNo.NO, 2, 1);

    score(PRODUCT_AND_ALL_VALID_DOSES);

    assertEquals(-2, notAProductSeries.getScorePatientSeries());
  }

  /**
   * The other half of Table 8-9's first row: SELECTB-2 fails when a dose
   * administered against the series was evaluated and found not valid, so a
   * product patient series carrying an invalid dose is awarded -2 rather than
   * +2.
   */
  @Test
  public void selectbTwoASeriesWithADoseAdministeredEvaluatedNotValidDoesNotHaveAllValidDoses() throws Exception {
    PatientSeries productSeries = standardSeries("HepB product", YesNo.YES);
    satisfiedTargetDose(productSeries);
    notValidTargetDose(productSeries);
    TargetDose remaining = remainingTargetDose(productSeries);
    maximumAge(remaining, DEFAULT_MAXIMUM_AGE);
    forecast(productSeries, remaining);

    score(PRODUCT_AND_ALL_VALID_DOSES);

    assertEquals("an invalid dose administered means the series does not have all valid doses", -2,
        productSeries.getScorePatientSeries());
  }

  /**
   * SELECTB-23 asks whether <i>this</i> patient series is a product patient
   * series - "the patient series has a product path of 'Y'" - so the answer must
   * come from the series being scored. A series whose product path is 'N' is not
   * a product patient series, whatever the series scored before it was.
   *
   * <p>
   * Both series here have every target dose satisfied, the shape every reading
   * of SELECTB-2 agrees has all valid doses, so the test turns only on the
   * product path flag.
   */
  @Test
  public void selectbTwentyThreeTheProductPathFlagIsReadFromTheSeriesBeingScored() throws Exception {
    PatientSeries productSeries = standardSeries("HepB product", YesNo.YES);
    satisfiedTargetDose(productSeries);
    PatientSeries notAProductSeries = standardSeries("HepB standard", YesNo.NO);
    satisfiedTargetDose(notAProductSeries);

    score(PRODUCT_AND_ALL_VALID_DOSES);

    assertEquals(2, productSeries.getScorePatientSeries());
    assertEquals("a product path of 'N' is not a product patient series, whoever was scored first", -2,
        notAProductSeries.getScorePatientSeries());
  }

  /**
   * The same question asked of SELECTB-2: whether a patient series has all valid
   * doses is a property of that series' own doses. A series whose doses are all
   * valid is awarded +2 even when a series scored before it had an invalid dose.
   */
  @Test
  public void selectbTwoTheAllValidDosesConditionIsReadFromTheSeriesBeingScored() throws Exception {
    PatientSeries withAnInvalidDose = standardSeries("HepB invalid dose", YesNo.YES);
    satisfiedTargetDose(withAnInvalidDose);
    notValidTargetDose(withAnInvalidDose);
    PatientSeries allValid = standardSeries("HepB all valid", YesNo.YES);
    satisfiedTargetDose(allValid);
    satisfiedTargetDose(allValid);

    score(PRODUCT_AND_ALL_VALID_DOSES);

    assertEquals(-2, withAnInvalidDose.getScorePatientSeries());
    assertEquals("this series' own doses are all valid, whoever was scored first", 2,
        allValid.getScorePatientSeries());
  }

  /**
   * Table 8-9's first row has "n/a" in its tie column: unlike the last three
   * rows the condition is not a comparison between series, so two series that
   * both satisfy it are both awarded the full +2 rather than being treated as
   * tied.
   */
  @Test
  public void tableEightNineTheProductRowHasNoTieOutcomeSoTwoQualifyingSeriesBothScorePlusTwo() throws Exception {
    PatientSeries first = standardSeries("HepB product one", YesNo.YES);
    satisfiedTargetDose(first);
    PatientSeries second = standardSeries("HepB product two", YesNo.YES);
    satisfiedTargetDose(second);

    score(PRODUCT_AND_ALL_VALID_DOSES);

    assertEquals(2, first.getScorePatientSeries());
    assertEquals(2, second.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Table 8-9, row 2 - "Is completable" (+3/n/a/-3)
  // ---------------------------------------------------------------------

  /**
   * Table 8-9's second row, first outcome column: SELECTB-3 makes a patient
   * series completable when its forecast finish date is before the maximum age
   * date of the last target dose, and a completable series is awarded +3.
   */
  @Test
  public void tableEightNineACompletableSeriesAwardsPlusThree() throws Exception {
    PatientSeries completable = finishingOn(inProcessSeries("HepB completable", 2), date(2024, 1, 1));

    score(COMPLETABLE);

    assertEquals("finishes 01/01/2024, ages out 01/01/2025", 3, completable.getScorePatientSeries());
  }

  /** Table 8-9's second row, third outcome column: not completable is -3. */
  @Test
  public void tableEightNineASeriesThatIsNotCompletableAwardsMinusThree() throws Exception {
    PatientSeries notCompletable = finishingOn(inProcessSeries("HepB not completable", 2), date(2026, 1, 1));

    score(COMPLETABLE);

    assertEquals("cannot finish until 01/01/2026, but ages out 01/01/2025", -3,
        notCompletable.getScorePatientSeries());
  }

  /**
   * SELECTB-3's comparison is strict - the finish date must be <i>before</i> the
   * maximum age date, not on or before it - so a series that cannot finish until
   * the very day it ages out is not completable.
   */
  @Test
  public void selectbThreeTheComparisonIsStrictSoFinishingOnTheMaximumAgeDateIsNotCompletable() throws Exception {
    PatientSeries finishesOnTheDay = finishingOn(inProcessSeries("HepB on the day", 2), AGES_OUT);

    score(COMPLETABLE);

    assertEquals(-3, finishesOnTheDay.getScorePatientSeries());
  }

  /**
   * SELECTB-3's maximum age date is the whole maximum age applied to the date of
   * birth, compound periods included. The bundled 4.65-508 release defines 77
   * {@code <maxAge>} values and 8 of them are "8 months + 1 day", so a patient
   * born 01/01/2020 ages out of those series doses on 09/02/2020, not 09/01/2020
   * - and a series that finishes on 09/01/2020 is still completable.
   *
   * <p>
   * {@code TimePeriod.getDateFrom(Date)} already computes this correctly, child
   * period and all; {@code InProcessPatientSeries.addTimePeriodtotoDate()} is a
   * second implementation of the same calculation that reads only the outermost
   * amount and type.
   */
  @Test
  public void selectbThreeTheMaximumAgeDateIncludesEveryPartOfACompoundMaximumAge() throws Exception {
    PatientSeries patientSeries = inProcessSeries("HepB compound maximum age", 2);
    maximumAge(patientSeries.getForecast().getTargetDose(), "8 months + 1 day");
    finishingOn(patientSeries, date(2020, 9, 1));

    score(COMPLETABLE);

    assertEquals("finishes 09/01/2020, ages out 09/02/2020", 3, patientSeries.getScorePatientSeries());
  }

  /**
   * SELECTB-3 measures the finish date against "the maximum age date of the
   * <b>last</b> target dose" - the age at which the series as a whole can no
   * longer be completed - not against the maximum age of whichever dose the
   * forecast happens to be recommending next. A series whose next dose ages out
   * in 2024 but whose final dose may still be given until 2040 is completable if
   * it can finish before 2040.
   */
  @Test
  public void selectbThreeCompletabilityIsMeasuredAgainstTheLastTargetDosesMaximumAgeDate() throws Exception {
    PatientSeries patientSeries = standardSeries("HepB three dose", YesNo.NO);
    satisfiedTargetDose(patientSeries);
    TargetDose nextDose = remainingTargetDose(patientSeries);
    maximumAge(nextDose, "4 years");
    TargetDose lastDose = remainingTargetDose(patientSeries);
    maximumAge(lastDose, "20 years");
    forecast(patientSeries, nextDose);
    finishingOn(patientSeries, date(2030, 1, 1));

    score(COMPLETABLE);

    assertEquals("the series finishes 01/01/2030 and its last target dose ages out 01/01/2040", 3,
        patientSeries.getScorePatientSeries());
  }

  /**
   * SELECTB-12 defines the forecast finish date the completable row rests on:
   * "the earliest date of the patient series forecast plus the latest minimum
   * interval of the remaining doses". It is not the adjusted past due date,
   * which is Table 7-12's due date for the <i>next</i> dose alone.
   *
   * <p>
   * The fixture separates the two on purpose: the earliest date plus the latest
   * of the two remaining doses' minimum intervals is 01/07/2024, comfortably
   * inside the 01/01/2025 maximum age date, while the adjusted past due date is
   * deliberately set to a later date that is not the finish date of anything.
   */
  @Test
  public void selectbTwelveTheForecastFinishDateIsTheEarliestDatePlusTheLatestMinimumIntervalRemaining()
      throws Exception {
    PatientSeries patientSeries = standardSeries("HepB two doses left", YesNo.NO);
    satisfiedTargetDose(patientSeries);
    TargetDose nextDose = remainingTargetDose(patientSeries);
    minimumInterval(nextDose, "4 weeks");
    TargetDose lastDose = remainingTargetDose(patientSeries);
    minimumInterval(lastDose, "6 months");
    maximumAge(lastDose, DEFAULT_MAXIMUM_AGE);
    maximumAge(nextDose, DEFAULT_MAXIMUM_AGE);
    Forecast forecast = forecast(patientSeries, nextDose);
    forecast.setEarliestDate(date(2024, 1, 1));
    forecast.setAdjustedPastDueDate(date(2026, 1, 1));

    score(COMPLETABLE);

    assertEquals("01/01/2024 plus the latest remaining minimum interval of 6 months is before 01/01/2025", 3,
        patientSeries.getScorePatientSeries());
  }

  /**
   * Table 8-9's second row also has "n/a" in its tie column, so two completable
   * series are each awarded the full +3 - completability is a property of one
   * series measured against its own maximum age date, not a comparison between
   * series.
   */
  @Test
  public void tableEightNineTheCompletableRowHasNoTieOutcomeSoTwoCompletableSeriesBothScorePlusThree()
      throws Exception {
    PatientSeries first = finishingOn(inProcessSeries("HepB first", 2), date(2024, 1, 1));
    PatientSeries second = finishingOn(inProcessSeries("HepB second", 1), date(2024, 6, 1));

    score(COMPLETABLE);

    assertEquals(3, first.getScorePatientSeries());
    assertEquals(3, second.getScorePatientSeries());
  }

  /**
   * Table 8-9's rows have three outcome columns and no fourth "not scored"
   * column, so a scorable patient series that cannot be shown completable falls
   * in the "not true" column and is awarded -3 like any other. A patient series
   * carrying no forecast at all cannot be shown completable.
   */
  @Test
  public void aPatientSeriesWithNoForecastFallsInTheNotTrueColumnAndAwardsMinusThree() throws Exception {
    PatientSeries noForecast = standardSeries("HepB no forecast", YesNo.NO);
    satisfiedTargetDose(noForecast);
    remainingTargetDose(noForecast);

    score(COMPLETABLE);

    assertEquals(-3, noForecast.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Table 8-9, row 3 - "Has the most valid doses" (+2/0/-2)
  // ---------------------------------------------------------------------

  /**
   * Table 8-9's third row, first outcome column: SELECTB-19's condition is true
   * for this series alone, so +2.
   */
  @Test
  public void tableEightNineTheMostValidDosesAloneAwardsPlusTwo() throws Exception {
    inProcessSeries("HepB one valid dose", 1);
    PatientSeries winner = inProcessSeries("HepB three valid doses", 3);

    score(MOST_VALID_DOSES);

    assertEquals(2, winner.getScorePatientSeries());
  }

  /** Table 8-9's third row, third outcome column: not true for this series, -2. */
  @Test
  public void tableEightNineNotTheMostValidDosesAwardsMinusTwo() throws Exception {
    PatientSeries loser = inProcessSeries("HepB one valid dose", 1);
    inProcessSeries("HepB three valid doses", 3);

    score(MOST_VALID_DOSES);

    assertEquals(-2, loser.getScorePatientSeries());
  }

  /**
   * Table 8-9's third row, second outcome column: SELECTB-19 is tie-inclusive (a
   * series has the most valid doses when its count is greater than or equal to
   * every other scorable series' count in the group), so two series with equal
   * counts both have the most and each is awarded 0 - neither the +2 of a lone
   * winner nor the -2 of a series that does not have the most.
   */
  @Test
  public void tableEightNineATieForTheMostValidDosesAwardsZeroToEachTiedSeries() throws Exception {
    PatientSeries first = inProcessSeries("HepB first", 2);
    PatientSeries second = inProcessSeries("HepB second", 2);

    score(MOST_VALID_DOSES);

    assertEquals(0, first.getScorePatientSeries());
    assertEquals(0, second.getScorePatientSeries());
  }

  /**
   * The tie outcome applies to every tied series and changes nothing for the
   * rest: two series tied at two valid doses and one series with one valid dose
   * score 0, 0 and -2.
   */
  @Test
  public void tableEightNineTheMostValidDosesTieOutcomeLeavesTheRemainingSeriesAtMinusTwo() throws Exception {
    PatientSeries first = inProcessSeries("HepB first", 2);
    PatientSeries second = inProcessSeries("HepB second", 2);
    PatientSeries behind = inProcessSeries("HepB behind", 1);

    score(MOST_VALID_DOSES);

    assertEquals(0, first.getScorePatientSeries());
    assertEquals(0, second.getScorePatientSeries());
    assertEquals(-2, behind.getScorePatientSeries());
  }

  /**
   * SELECTB-19 rests on the valid dose count, which counts the target doses of
   * the patient series with a target dose status of 'Satisfied'. A series
   * holding one satisfied target dose and three still to come has a valid dose
   * count of one, not four, so it loses to a series with two satisfied target
   * doses.
   */
  @Test
  public void selectbNineteenTheValidDoseCountCountsOnlyTargetDosesWithStatusSatisfied() throws Exception {
    PatientSeries oneValidDose = inProcessSeries("HepB one valid dose", YesNo.NO, 1, 3);
    PatientSeries twoValidDoses = inProcessSeries("HepB two valid doses", YesNo.NO, 2, 1);

    score(MOST_VALID_DOSES);

    assertEquals("three target doses still to come are not valid doses", -2,
        oneValidDose.getScorePatientSeries());
    assertEquals(2, twoValidDoses.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Table 8-9, row 4 - "Is closest to completion" (+2/0/-2)
  // ---------------------------------------------------------------------

  /**
   * Table 8-9's fourth row, first and third outcome columns: SELECTB-5 makes the
   * series with the fewest not-satisfied target doses the one closest to
   * completion, awarded +2, and every other series -2.
   *
   * <p>
   * Both series here have the same number of target doses, so "fewest
   * not-satisfied" and "most satisfied" pick the same winner and the test turns
   * only on the outcome columns.
   */
  @Test
  public void tableEightNineClosestToCompletionAloneAwardsPlusTwo() throws Exception {
    PatientSeries closest = inProcessSeries("HepB three of four", YesNo.NO, 3, 1);
    PatientSeries furthest = inProcessSeries("HepB one of four", YesNo.NO, 1, 3);

    score(CLOSEST_TO_COMPLETION);

    assertEquals(2, closest.getScorePatientSeries());
    assertEquals(-2, furthest.getScorePatientSeries());
  }

  /**
   * Table 8-9's fourth row, second outcome column: two series equally close to
   * completion are tied, and the tie awards 0 to each of them - not +2 to
   * whichever the implementation reaches first.
   */
  @Test
  public void tableEightNineATieForClosestToCompletionAwardsZeroToEachTiedSeries() throws Exception {
    PatientSeries first = inProcessSeries("HepB first", YesNo.NO, 2, 2);
    PatientSeries second = inProcessSeries("HepB second", YesNo.NO, 2, 2);

    score(CLOSEST_TO_COMPLETION);

    assertEquals("two series with two target doses left each are tied", 0, first.getScorePatientSeries());
    assertEquals(0, second.getScorePatientSeries());
  }

  /**
   * SELECTB-5 counts the target doses a series still has to satisfy - "the
   * patient series with the fewest number of not-satisfied target doses" - which
   * is a different question from SELECTB-19's "most valid doses" whenever the
   * series being compared have different numbers of target doses. Table 8-9
   * scores them as two separate rows precisely because they are two separate
   * conditions.
   *
   * <p>
   * The three-dose series has two doses in hand and one to go; the five-dose
   * series has three in hand and two to go. The five-dose series has more valid
   * doses, but the three-dose series is closer to completion.
   */
  @Test
  public void selectbFiveClosestToCompletionIsTheFewestNotSatisfiedTargetDosesNotTheMostValidDoses()
      throws Exception {
    PatientSeries threeDoseSeries = inProcessSeries("HepB three dose", YesNo.NO, 2, 1);
    PatientSeries fiveDoseSeries = inProcessSeries("HepB five dose", YesNo.NO, 3, 2);

    score(CLOSEST_TO_COMPLETION);

    assertEquals("one not-satisfied target dose is closer to completion than two", 2,
        threeDoseSeries.getScorePatientSeries());
    assertEquals(-2, fiveDoseSeries.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Table 8-9, row 5 - "Can finish earliest" (+1/0/-1)
  // ---------------------------------------------------------------------

  /**
   * Table 8-9's fifth row, first and third outcome columns: SELECTB-11 makes a
   * completable patient series able to finish earliest when its finish date is
   * on or before every other completable series' finish date, awarded +1, and
   * every other series -1.
   */
  @Test
  public void tableEightNineCanFinishEarliestAloneAwardsPlusOne() throws Exception {
    PatientSeries earliest =
        withLatestDate(finishingOn(inProcessSeries("HepB earliest", 2), date(2024, 1, 1)), date(2024, 1, 1));
    PatientSeries later =
        withLatestDate(finishingOn(inProcessSeries("HepB later", 2), date(2024, 6, 1)), date(2024, 6, 1));

    score(CAN_FINISH_EARLIEST);

    assertEquals(1, earliest.getScorePatientSeries());
    assertEquals(-1, later.getScorePatientSeries());
  }

  /**
   * SELECTB-11 awards its +1 on a property of the series - its finish date is on
   * or before every other completable series' - so the series that can finish
   * earliest is awarded +1 wherever it sits in the list.
   * {@link #tableEightNineCanFinishEarliestAloneAwardsPlusOne} is the same group
   * in the other order and is the control for this test.
   */
  @Test
  public void tableEightNineCanFinishEarliestIsAwardedWhateverTheSeriesPositionInTheList() throws Exception {
    PatientSeries later =
        withLatestDate(finishingOn(inProcessSeries("HepB later", 2), date(2024, 6, 1)), date(2024, 6, 1));
    PatientSeries earliest =
        withLatestDate(finishingOn(inProcessSeries("HepB earliest", 2), date(2024, 1, 1)), date(2024, 1, 1));

    score(CAN_FINISH_EARLIEST);

    assertEquals("the +1 is awarded on the finish date, not on the series' list position", 1,
        earliest.getScorePatientSeries());
    assertEquals(-1, later.getScorePatientSeries());
  }

  /**
   * Table 8-9's fifth row, second outcome column: SELECTB-11's comparison is "on
   * or before", so two series that finish on the same date can both finish
   * earliest, and the tie awards 0 to each of them.
   *
   * <p>
   * The two dates are the same calendar day held in two different objects, which
   * is what two independently computed forecasts produce - see 08-05's Review
   * Findings on the reference-equality comparison in this condition, and
   * {@link #theTieForFinishingEarliestIsDetectedWhenTwoSeriesShareOneDateObject},
   * which is the same group with one shared object.
   */
  @Test
  public void tableEightNineATieForFinishingEarliestAwardsZeroToEachTiedSeries() throws Exception {
    PatientSeries first =
        withLatestDate(finishingOn(inProcessSeries("HepB first", 2), date(2024, 1, 1)), date(2024, 1, 1));
    PatientSeries second =
        withLatestDate(finishingOn(inProcessSeries("HepB second", 2), date(2024, 1, 1)), date(2024, 1, 1));

    score(CAN_FINISH_EARLIEST);

    assertEquals("both series finish on 01/01/2024", 0, first.getScorePatientSeries());
    assertEquals(0, second.getScorePatientSeries());
  }

  /**
   * The control for
   * {@link #tableEightNineATieForFinishingEarliestAwardsZeroToEachTiedSeries}:
   * the same two series finishing on the same day, differing only in that both
   * forecasts happen to hold the identical {@code Date} object. The
   * specification draws no distinction between the two fixtures - both are two
   * series that finish on 01/01/2024 - so both must produce Table 8-9's tie
   * outcome of 0.
   */
  @Test
  public void theTieForFinishingEarliestIsDetectedWhenTwoSeriesShareOneDateObject() throws Exception {
    Date sharedFinishDate = date(2024, 1, 1);
    PatientSeries first =
        withLatestDate(finishingOn(inProcessSeries("HepB first", 2), sharedFinishDate), sharedFinishDate);
    PatientSeries second =
        withLatestDate(finishingOn(inProcessSeries("HepB second", 2), sharedFinishDate), sharedFinishDate);

    score(CAN_FINISH_EARLIEST);

    assertEquals(0, first.getScorePatientSeries());
    assertEquals(0, second.getScorePatientSeries());
  }

  /**
   * SELECTB-11 has two clauses: a patient series can finish earliest when it "is
   * a completable patient series <b>and</b> the finish date is on or before the
   * finish date of every other completable patient series". A series that will
   * age out before it can finish is not completable, so it is not in the
   * comparison at all and cannot take the +1 from the series that is.
   *
   * <p>
   * The fixture points the two clauses in opposite directions on purpose: the
   * series with the earlier date is the one that cannot finish in time.
   */
  @Test
  public void selectbElevenOnlyACompletableSeriesCanFinishEarliest() throws Exception {
    PatientSeries notCompletable =
        withLatestDate(finishingOn(inProcessSeries("HepB ages out", 2), date(2031, 1, 1)), date(2024, 1, 1));
    PatientSeries completable =
        withLatestDate(finishingOn(inProcessSeries("HepB completable", 2), date(2024, 6, 1)), date(2030, 1, 1));

    score(CAN_FINISH_EARLIEST);

    assertEquals("the only completable series in the group finishes earliest of them", 1,
        completable.getScorePatientSeries());
    assertEquals(-1, notCompletable.getScorePatientSeries());
  }

  /**
   * The date SELECTB-11 compares is the forecast finish date SELECTB-12 defines
   * - the earliest date plus the latest minimum interval of the remaining doses
   * - which is the same quantity SELECTB-3 uses one row above. It is not the
   * patient series forecast's latest date, which is Table 7-12's last day the
   * next dose may be given and says nothing about when the series finishes.
   *
   * <p>
   * Here the series that finishes first (12/01/2025) is the one whose latest
   * date is later (01/01/2030), so the two readings pick opposite winners.
   */
  @Test
  public void selectbTwelveTheFinishDateComparedIsTheForecastFinishDateNotTheForecastsLatestDate()
      throws Exception {
    PatientSeries finishesLast = inProcessSeries("HepB finishes 2031", YesNo.NO, 2, 1);
    maximumAge(finishesLast.getForecast().getTargetDose(), "20 years");
    finishesLast.getForecast().setEarliestDate(date(2030, 1, 1));
    minimumInterval(finishesLast.getForecast().getTargetDose(), "1 year");
    finishingOn(finishesLast, date(2031, 1, 1));
    withLatestDate(finishesLast, date(2025, 1, 1));

    PatientSeries finishesFirst = inProcessSeries("HepB finishes 2025", YesNo.NO, 2, 1);
    maximumAge(finishesFirst.getForecast().getTargetDose(), "20 years");
    finishesFirst.getForecast().setEarliestDate(date(2025, 6, 1));
    minimumInterval(finishesFirst.getForecast().getTargetDose(), "6 months");
    finishingOn(finishesFirst, date(2025, 12, 1));
    withLatestDate(finishesFirst, date(2030, 1, 1));

    score(CAN_FINISH_EARLIEST);

    assertEquals("12/01/2025 is before 01/01/2031", 1, finishesFirst.getScorePatientSeries());
    assertEquals(-1, finishesLast.getScorePatientSeries());
  }

  /**
   * Table 8-9's fifth row is scored for every scorable patient series in the
   * group, so the row's outcome for one series cannot depend on what some other
   * series does or does not carry. A patient series forecast has a latest date
   * only where the series dose defines a maximum age, so a group in which the
   * first series has none is ordinary rather than exotic - and the remaining
   * series must still be scored on their own finish dates.
   */
  @Test
  public void theRowIsScoredForEverySeriesEvenWhenTheFirstSeriesHasNoLatestDate() throws Exception {
    PatientSeries noLatestDate = finishingOn(inProcessSeries("HepB no latest date", 2), date(2024, 6, 1));
    PatientSeries earliest =
        withLatestDate(finishingOn(inProcessSeries("HepB earliest", 2), date(2024, 1, 1)), date(2024, 1, 1));

    score(CAN_FINISH_EARLIEST);

    assertEquals("01/01/2024 is the earliest finish date in the group", 1, earliest.getScorePatientSeries());
    assertEquals(-1, noLatestDate.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Purpose and Entry Conditions - the scope 8.5 scores over
  // ---------------------------------------------------------------------

  /**
   * Every row of Table 8-9 is about a <b>scorable</b> patient series - the
   * table's own title asks how many points are awarded "to a Scorable Patient
   * Series That Is an In-process Patient Series", and 8.3's Table 8-5 Rule 2,
   * the only route into this step, hands 8.5 the in-process patient series it
   * counted in the scorable list. The scorable patient series are what 8.1
   * produced: SELECTSCORE-2 keeps only the highest-priority Risk series of a
   * series group, so a priority "B" Risk series alongside a priority "A" one is
   * not scorable and must not set the maximum valid dose count the scorable
   * series are measured against.
   *
   * <p>
   * 8.5 reads {@code dataModel.getPatientSeriesStepper().getList()} - 5.1's
   * unfiltered list of every relevant patient series for every antigen, three
   * pipeline stages earlier than the scorable list. See the 2026-09-05 "Chapter
   * 8 has no series group" entry in
   * {@code cdsi-reference/step-tests/cross-cutting-notes.md}.
   */
  @Test
  public void theStepScoresTheScorablePatientSeriesEightOneProducedNotEveryRelevantSeries() throws Exception {
    PatientSeries scorable = series("HepB risk priority A", hepB, INCREASED_RISK_GROUP, SeriesType.RISK, "A",
        YesNo.NO);
    satisfiedTargetDose(scorable);
    satisfiedTargetDose(scorable);
    remainingTargetDose(scorable);
    PatientSeries dropped = series("HepB risk priority B", hepB, INCREASED_RISK_GROUP, SeriesType.RISK, "B",
        YesNo.NO);
    for (int i = 0; i < 5; i++) {
      satisfiedTargetDose(dropped);
    }
    remainingTargetDose(dropped);
    // What 8.1 leaves behind: the priority B risk series is not scorable.
    dataModel.getScorablePatientSeriesList().add(scorable);

    score(MOST_VALID_DOSES);

    assertEquals("only the scorable patient series compete for the most valid doses", 2,
        scorable.getScorePatientSeries());
  }

  /**
   * 4.5 and Figure 4-7 wrap Chapter 8 in a per-antigen loop, and 8.5's rules are
   * all phrased over the patient series of one series group of one antigen. A
   * run of 8.5 for HepB must therefore find HepB's own winner, whatever series
   * of some other antigen are still sitting in the stepper's list.
   */
  @Test
  public void theStepScoresThePatientSeriesOfTheAntigenBeingProcessed() throws Exception {
    Antigen measles = dataModel.getOrCreateAntigen("Measles");
    PatientSeries hepBWinner = inProcessSeries("HepB standard", 2);
    inProcessSeries("HepB alternate", 1);
    PatientSeries measlesSeries = series("Measles standard", measles, STANDARD_GROUP, SeriesType.STANDARD, null,
        YesNo.NO);
    for (int i = 0; i < 5; i++) {
      satisfiedTargetDose(measlesSeries);
    }
    remainingTargetDose(measlesSeries);

    score(MOST_VALID_DOSES);

    assertEquals("8.5 runs inside 4.5's per-antigen loop, so only HepB's series are in scope", 2,
        hepBWinner.getScorePatientSeries());
  }

  /**
   * Chapter 8's overview: "Process steps 8.1 through 8.7 are repeated <b>for
   * each series group</b> to identify one prioritized patient series per series
   * group." SELECTB-19 is scoped the same way - a series has the most valid
   * doses when its count is greater than or equal to every other scorable
   * patient series' count <i>in the series group</i> - so a run of 8.5 for the
   * Standard group must find its own winner whatever the Increased Risk group
   * holds.
   */
  @Test
  public void theStepScoresThePatientSeriesOfOneSeriesGroup() throws Exception {
    PatientSeries standardWinner = inProcessSeries("HepB standard two valid doses", 2);
    inProcessSeries("HepB standard one valid dose", 1);
    PatientSeries increasedRisk = series("HepB increased risk", hepB, INCREASED_RISK_GROUP, SeriesType.RISK, "A",
        YesNo.NO);
    for (int i = 0; i < 5; i++) {
      satisfiedTargetDose(increasedRisk);
    }
    remainingTargetDose(increasedRisk);

    score(MOST_VALID_DOSES);

    assertEquals("one run of 8.5 scores one series group, whose winner has two valid doses", 2,
        standardWinner.getScorePatientSeries());
  }

  /**
   * SELECTB-16 decides which series Table 8-9 applies to. The table asks how
   * many points are awarded "to a Scorable Patient Series That Is an
   * <b>In-process</b> Patient Series", and 8.3's Table 8-5 Rule 2 - the only
   * route into this step - says the in-process patient series scoring business
   * rules are applied "to these scorable patient series only". A series with no
   * valid doses at all is not an in-process patient series, so it is outside
   * this table's scope entirely and must come out of 8.5 with the score Chapter
   * 7 and 8.1 left it holding.
   *
   * <p>
   * This mirrors {@code CompletePatientSeriesTest#theStepScoresOnlyTheComplete
   * PatientSeriesInTheGroup} and, like it, asserts only the half the
   * specification settles - that a table about in-process patient series does
   * not penalise a series that is not one - taking no position on whether such
   * series should be dropped from consideration by some other means.
   */
  @Test
  public void selectbSixteenTheStepScoresTheInProcessPatientSeriesOfTheGroupOnly() throws Exception {
    PatientSeries noValidDoses = standardSeries("HepB no valid doses", YesNo.NO);
    remainingTargetDose(noValidDoses);
    remainingTargetDose(noValidDoses);
    inProcessSeries("HepB in process", 2);
    inProcessSeries("HepB also in process", 1);

    score(MOST_VALID_DOSES);

    assertEquals("Table 8-9 scores in-process patient series only", 0, noValidDoses.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // State Changes
  // ---------------------------------------------------------------------

  /**
   * 08-05's State Changes: the five rows of Table 8-9 all write to one running
   * integer per patient series, so a series in the "not true" column of every
   * row comes out of the step at the sum of the five negative outcomes, -2 -3 -2
   * -2 -1 = -10.
   *
   * <p>
   * The fixture is a two-series group in which the second series loses every
   * row: it is not a product patient series, it cannot finish before it ages
   * out, it has fewer valid doses, it is further from completion and it finishes
   * later.
   */
  @Test
  public void theFiveTableEightNineRowsAccumulateIntoOneRunningScore() throws Exception {
    PatientSeries winner = inProcessSeries("HepB standard", YesNo.YES, 3, 1);
    finishingOn(winner, date(2024, 1, 1));
    withLatestDate(winner, date(2024, 1, 1));

    PatientSeries loser = inProcessSeries("HepB alternate", YesNo.NO, 1, 3);
    finishingOn(loser, date(2026, 1, 1));
    withLatestDate(loser, date(2030, 1, 1));

    scoreWholeTable();

    assertEquals("the 'not true' column of all five rows of Table 8-9", -10, loser.getScorePatientSeries());
  }

  /**
   * 08-05's State Changes names {@code PatientSeries.incPatientScoreSeries()} /
   * {@code descPatientScoreSeries()} - a running integer score field consumed
   * later by 8.7 - so Table 8-9's outcomes are applied to whatever score the
   * series already carries rather than replacing it.
   *
   * <p>
   * Worth pinning explicitly because the score is never reset between
   * selections - see the 2026-09-02 "Patient series scores accumulate across a
   * whole assessment" entry in
   * {@code cdsi-reference/step-tests/cross-cutting-notes.md}. Whether that
   * accumulation is correct is not 8.5's question, but it is the reason the tie
   * column of rows three to five ("0") cannot be implemented by leaving a series
   * untouched.
   */
  @Test
  public void theScoreIsARunningTotalTheStepIncrementsOrDecrementsRatherThanSets() throws Exception {
    PatientSeries completable = finishingOn(inProcessSeries("HepB completable", 2), date(2024, 1, 1));
    completable.setScorePatientSeriesScore(5);

    score(COMPLETABLE);

    assertEquals("+3 is added to the running score, not assigned to it", 8, completable.getScorePatientSeries());
  }

  /**
   * 08-05's State Changes names the score field and nothing else. Whichever
   * columns of Table 8-9 apply, each patient series' status, its target doses
   * and their statuses, its forecast and the list itself must come back exactly
   * as Chapter 7 and 8.1 left them, and neither the prioritized nor the scorable
   * patient series list may gain an entry - selecting a prioritized series
   * belongs to 8.7, which {@code next()} constructs but does not run.
   */
  @Test
  public void theStepChangesNoPatientSeriesStateOtherThanTheScore() throws Exception {
    PatientSeries first = inProcessSeries("HepB standard", YesNo.YES, 2, 1);
    finishingOn(first, date(2024, 1, 1));
    withLatestDate(first, date(2024, 1, 1));
    PatientSeries second = inProcessSeries("HepB alternate", YesNo.NO, 1, 2);
    finishingOn(second, date(2026, 1, 1));
    withLatestDate(second, date(2030, 1, 1));

    scoreWholeTable();

    assertEquals("the patient series list is not re-filtered here", 2, patientSeriesList.size());
    assertEquals(PatientSeriesStatus.NOT_COMPLETE, first.getPatientSeriesStatus());
    assertEquals(PatientSeriesStatus.NOT_COMPLETE, second.getPatientSeriesStatus());
    assertEquals(3, first.getTargetDoseList().size());
    assertEquals(3, second.getTargetDoseList().size());
    assertEquals(TargetDoseStatus.SATISFIED, first.getTargetDoseList().get(0).getTargetDoseStatus());
    assertEquals(TargetDoseStatus.NOT_SATISFIED, first.getTargetDoseList().get(2).getTargetDoseStatus());
    assertEquals(date(2024, 1, 1), first.getForecast().getAdjustedPastDueDate());
    assertTrue("8.5 prioritizes no patient series", dataModel.getPrioritizedPatientSeriesList().isEmpty());
    assertTrue("8.5 does not re-derive the scorable patient series list",
        dataModel.getScorablePatientSeriesList().isEmpty());
  }

  // ---------------------------------------------------------------------
  // Next Steps
  // ---------------------------------------------------------------------

  /**
   * 08-05's Next Steps: unconditional to 8.7 Select Prioritized Patient Series,
   * per {@code transitions.yaml}. "Unconditional" means whatever the scoring
   * found - a lone winner of every row, or a group tied on all of them.
   */
  @Test
  public void theStepTransitionsUnconditionallyToSelectPrioritizedPatientSeries() throws Exception {
    PatientSeries winner = inProcessSeries("HepB standard", YesNo.YES, 3, 1);
    finishingOn(winner, date(2024, 1, 1));
    withLatestDate(winner, date(2024, 1, 1));
    PatientSeries loser = inProcessSeries("HepB alternate", YesNo.NO, 1, 3);
    finishingOn(loser, date(2026, 1, 1));
    withLatestDate(loser, date(2030, 1, 1));
    assertEquals("a lone winner goes to 8.7", LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES, scoreWholeTable());

    setUp();
    Date sharedDate = date(2024, 1, 1);
    PatientSeries first = inProcessSeries("HepB first", YesNo.YES, 2, 1);
    finishingOn(first, sharedDate);
    withLatestDate(first, sharedDate);
    PatientSeries second = inProcessSeries("HepB second", YesNo.YES, 2, 1);
    finishingOn(second, sharedDate);
    withLatestDate(second, sharedDate);
    assertEquals("a group tied on every row goes to 8.7", LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES,
        scoreWholeTable());
  }
}
