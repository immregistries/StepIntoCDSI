package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.Neighborhood;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.RecurringDose;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

/**
 * Section 4.4 "Evaluate and Forecast all Relevant Patient Series" (Logic
 * Specification for ACIP Recommendations v4.6, pages 35-38, Figures 4-5 and
 * 4-6) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/04-04-evaluate-and-forecast-all-relevant-patient-series/index.md}.
 *
 * <p>
 * 4.4 declares no business rules and no decision tables of its own - the
 * clinical rules it orchestrates live in Chapters 6 (evaluation) and 7
 * (forecasting). What 4.4 <i>does</i> define, in Figures 4-5 and 4-6, is a
 * state machine, and that is what these tests pin. Figure 4-5 is the outer loop
 * ("for each relevant patient series, evaluate and forecast"). Figure 4-6 is
 * the inner walk of two collections together - the current patient series'
 * target doses and its antigen administered records (AARs) - until one is
 * exhausted:
 *
 * <ol>
 * <li>Get the first target dose, then the first AAR (if no AARs exist,
 * evaluation is over before it starts and forecasting begins).</li>
 * <li>Evaluate the AAR against the target dose (Chapter 6).</li>
 * <li>If the target dose ends up satisfied, ask whether it is a
 * <i>recurring</i> dose - if so create another target dose - then get the next
 * target dose; if not satisfied, keep the same target dose and get the next
 * AAR.</li>
 * <li>Repeat until the target doses are exhausted ("set evaluation status to
 * 'Extraneous' for any un-evaluated antigen administered records") or the AARs
 * are exhausted (evaluation for this series ends and forecasting begins for
 * whichever target dose evaluation stopped on).</li>
 * </ol>
 *
 * <p>
 * {@link EvaluateAndForecastAllPatientSeries#process()} advances that state
 * machine by exactly one step per call and then hands off to whichever Chapter
 * 6/7 step runs next, so each test here drives {@code process()} directly and
 * inspects the {@code DataModel} it leaves behind, rather than running a whole
 * scenario through the engine. Domain objects are hand-built: the step reads
 * only the patient series stepper, the series' {@code SeriesDose} list, and the
 * organized AAR list, so no Supporting Data release is needed. Constructing the
 * returned next step is not inert (6.1 reads the current AAR, 7.1 reads the
 * current target dose and the selected AAR list), but {@code process()} is
 * never called on it here, so no Chapter 6/7 logic runs.
 *
 * <p>
 * Where a test pins behaviour the specification does not describe - the
 * implementation's {@code TargetDoseStatus} switch, its "don't advance the AAR
 * until Chapter 6 has actually evaluated" guard, and the two loop guards the
 * step package's Review Findings flag as having no specification basis - the
 * javadoc says so explicitly.
 */
public class EvaluateAndForecastAllPatientSeriesTest {

  private static final String HEPB = "HepB";
  private static final String POLIO = "Polio";

  private DataModel dataModel;

  @Before
  public void setUp() {
    dataModel = new DataModel();
  }

  // ---------------------------------------------------------------- fixtures

  private static Date date(int year, int month, int day) {
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(year, month - 1, day);
    return calendar.getTime();
  }

  private Antigen antigen(String name) {
    return dataModel.getOrCreateAntigen(name);
  }

  /** A series dose with no recurring-dose attribute at all. */
  private static SeriesDose seriesDose(String doseNumber) {
    SeriesDose seriesDose = new SeriesDose();
    seriesDose.setDoseNumber(doseNumber);
    return seriesDose;
  }

  /**
   * A series dose carrying an explicit Recurring Dose attribute. Every
   * {@code seriesDose} in the bundled CDC Supporting Data releases carries one
   * (mostly {@code No}), so this - not {@link #seriesDose(String)} - is the
   * shape the engine actually sees in production.
   */
  private static SeriesDose seriesDose(String doseNumber, YesNo recurring) {
    SeriesDose seriesDose = seriesDose(doseNumber);
    RecurringDose recurringDose = new RecurringDose();
    recurringDose.setValue(recurring);
    recurringDose.setSeriesDose(seriesDose);
    seriesDose.getRecurringDoseList().add(recurringDose);
    return seriesDose;
  }

  /** Adds one relevant patient series, in order, to the patient series stepper. */
  private PatientSeries relevantPatientSeries(String seriesName, Antigen targetDisease, SeriesDose... seriesDoses) {
    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setTargetDisease(targetDisease);
    for (SeriesDose seriesDose : seriesDoses) {
      seriesDose.setAntigenSeries(antigenSeries);
      antigenSeries.getSeriesDoseList().add(seriesDose);
    }
    dataModel.getAntigenSeriesList().add(antigenSeries);
    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    dataModel.getPatientSeriesStepper().add(patientSeries);
    return patientSeries;
  }

  /** Adds one antigen administered record to the organized history 4.2 produced. */
  private AntigenAdministeredRecord administered(Antigen antigen, Date dateAdministered) {
    VaccineDoseAdministered vaccineDoseAdministered = new VaccineDoseAdministered();
    vaccineDoseAdministered.setDateAdministered(dateAdministered);
    AntigenAdministeredRecord aar = new AntigenAdministeredRecord();
    aar.setAntigen(antigen);
    aar.setDateAdministered(dateAdministered);
    aar.setVaccineDoseAdministered(vaccineDoseAdministered);
    dataModel.getAntigenAdministeredRecordList().add(aar);
    return aar;
  }

  private LogicStep process() throws Exception {
    return new EvaluateAndForecastAllPatientSeries(dataModel).process();
  }

  /**
   * Stands in for Chapter 6 having run: the step only advances the AAR on a
   * NOT_SATISFIED target dose once an evaluation with a status has been
   * recorded against it.
   */
  private void chapterSixRecordedEvaluation(EvaluationStatus evaluationStatus) {
    Evaluation evaluation = new Evaluation();
    evaluation.setEvaluationStatus(evaluationStatus);
    dataModel.getTargetDose().setEvaluation(evaluation);
  }

  /** Dates of the AARs that ended up with an EXTRANEOUS evaluation, in list order. */
  private List<Date> datesMarkedExtraneous() {
    List<Date> dates = new ArrayList<Date>();
    for (TargetDose targetDose : dataModel.getTargetDoseList()) {
      for (Evaluation evaluation : targetDose.getEvaluationList()) {
        if (evaluation.getEvaluationStatus() == EvaluationStatus.EXTRANEOUS) {
          dates.add(evaluation.getVaccineDoseAdministered() == null
              ? null
              : evaluation.getVaccineDoseAdministered().getDateAdministered());
        }
      }
    }
    return dates;
  }

  /** How many target doses in the list track {@code seriesDose}. */
  private int targetDosesTracking(SeriesDose seriesDose) {
    int count = 0;
    for (TargetDose targetDose : dataModel.getTargetDoseList()) {
      if (targetDose.getTrackedSeriesDose() == seriesDose) {
        count++;
      }
    }
    return count;
  }

  // --------------------------------------------- Figure 4-6: getting started

  /**
   * Figure 4-6, first two boxes: "Get first target dose", then "Get first
   * antigen administered record", then evaluate it against that target dose
   * (Chapter 6). One target dose is created per series dose of the series being
   * evaluated, in the series' own order, and the walk starts at the first of
   * each.
   */
  @Test
  public void firstCallGetsTheFirstTargetDoseAndTheFirstAntigenAdministeredRecord() throws Exception {
    Antigen hepB = antigen(HEPB);
    SeriesDose doseOne = seriesDose("1");
    SeriesDose doseTwo = seriesDose("2");
    PatientSeries patientSeries = relevantPatientSeries("HepB 3 dose series", hepB, doseOne, doseTwo);
    AntigenAdministeredRecord firstRecord = administered(hepB, date(2011, 3, 1));
    administered(hepB, date(2011, 5, 1));

    LogicStep next = process();

    assertEquals("One target dose per series dose", 2, dataModel.getTargetDoseList().size());
    assertSame("The target dose list belongs to the patient series being evaluated",
        dataModel.getTargetDoseList(), patientSeries.getTargetDoseList());
    assertSame("Evaluation starts on the first target dose", doseOne,
        dataModel.getTargetDose().getTrackedSeriesDose());
    assertEquals(0, dataModel.getTargetDoseListPos());
    assertSame("Evaluation starts on the first antigen administered record", firstRecord,
        dataModel.getAntigenAdministeredRecord());
    assertEquals(0, dataModel.getSelectedAntigenAdministeredRecordPos());
    assertSame("The antigen being evaluated is the series' target disease", hepB, dataModel.getAntigen());
    assertEquals("With both collections non-empty, Chapter 6 evaluates the first pair",
        LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION, next.getLogicStepType());
    assertSame(Neighborhood.EVALUATE, dataModel.getNeighborhood());
    assertNotNull(doseTwo);
  }

  /**
   * Figure 4-6's left-hand branch: "If no antigen administered records exist",
   * the evaluate loop is never entered at all and the series goes straight to
   * forecasting (Chapter 7).
   */
  @Test
  public void seriesWithNoAntigenAdministeredRecordsGoesStraightToForecasting() throws Exception {
    Antigen hepB = antigen(HEPB);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"));

    LogicStep next = process();

    assertTrue("Nothing to evaluate against", dataModel.getSelectedAntigenAdministeredRecordList().isEmpty());
    assertEquals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, next.getLogicStepType());
    assertSame(Neighborhood.FORECAST, dataModel.getNeighborhood());
  }

  /**
   * Figure 4-6 walks "each antigen administered record" of the series under
   * evaluation - i.e. the records for that series' target disease, not the
   * patient's whole organized history. Records for another antigen are not part
   * of this series' walk.
   */
  @Test
  public void onlyTheRecordsForThisSeriesAntigenAreWalked() throws Exception {
    Antigen hepB = antigen(HEPB);
    Antigen polio = antigen(POLIO);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"));
    AntigenAdministeredRecord hepBRecord = administered(hepB, date(2011, 3, 1));
    administered(polio, date(2011, 4, 1));

    process();

    assertEquals("Only the HepB record belongs to this series' walk",
        Arrays.asList(hepBRecord), dataModel.getSelectedAntigenAdministeredRecordList());
  }

  // -------------------------------------------- Figure 4-6: the two-way walk

  /**
   * Figure 4-6: target dose status satisfied? No -> "Is there another antigen
   * administered record to process?" Yes -> "Get next antigen administered
   * record" and evaluate it against the <i>same</i> target dose.
   */
  @Test
  public void notSatisfiedKeepsTheTargetDoseAndAdvancesToTheNextRecord() throws Exception {
    Antigen hepB = antigen(HEPB);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"), seriesDose("2"));
    administered(hepB, date(2011, 3, 1));
    AntigenAdministeredRecord secondRecord = administered(hepB, date(2011, 5, 1));

    process();
    TargetDose targetDoseUnderEvaluation = dataModel.getTargetDose();
    chapterSixRecordedEvaluation(EvaluationStatus.NOT_VALID);

    LogicStep next = process();

    assertSame("A not-satisfied target dose stays under evaluation", targetDoseUnderEvaluation,
        dataModel.getTargetDose());
    assertEquals(0, dataModel.getTargetDoseListPos());
    assertSame("The next record is evaluated against it", secondRecord, dataModel.getAntigenAdministeredRecord());
    assertEquals(1, dataModel.getSelectedAntigenAdministeredRecordPos());
    assertEquals(LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION, next.getLogicStepType());
  }

  /**
   * Figure 4-6: target dose not satisfied and "Is there another antigen
   * administered record to process?" -> No. The AARs are exhausted, so
   * evaluation for this series ends and forecasting begins for the target dose
   * evaluation stopped on.
   */
  @Test
  public void notSatisfiedWithNoRecordsLeftEndsEvaluationAndBeginsForecasting() throws Exception {
    Antigen hepB = antigen(HEPB);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"), seriesDose("2"));
    administered(hepB, date(2011, 3, 1));

    process();
    TargetDose targetDoseEvaluationStoppedOn = dataModel.getTargetDose();
    chapterSixRecordedEvaluation(EvaluationStatus.NOT_VALID);

    LogicStep next = process();

    assertEquals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, next.getLogicStepType());
    assertSame(Neighborhood.FORECAST, dataModel.getNeighborhood());
    assertSame("Forecasting starts from the target dose evaluation stopped on",
        targetDoseEvaluationStoppedOn, dataModel.getForecast().getTargetDose());
  }

  /**
   * Implementation behaviour with no specification counterpart, pinned because
   * it decides when rule 3 fires: a NOT_SATISFIED target dose only advances the
   * AAR once an evaluation with a status has been recorded against that target
   * dose. Entering evaluation for a target dose no Chapter 6 step has evaluated
   * yet leaves both positions alone, so the same pair is evaluated rather than
   * the first record being skipped.
   */
  @Test
  public void notSatisfiedWithoutACompletedEvaluationDoesNotAdvanceTheRecord() throws Exception {
    Antigen hepB = antigen(HEPB);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"));
    AntigenAdministeredRecord firstRecord = administered(hepB, date(2011, 3, 1));
    administered(hepB, date(2011, 5, 1));

    process();
    LogicStep next = process();

    assertEquals("No Chapter 6 evaluation has been recorded, so the record position stands still",
        0, dataModel.getSelectedAntigenAdministeredRecordPos());
    assertSame(firstRecord, dataModel.getAntigenAdministeredRecord());
    assertEquals(LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION, next.getLogicStepType());
  }

  /**
   * Figure 4-6: target dose status satisfied? Yes -> (not recurring) -> "Is
   * there another target dose to process?" Yes -> "Get next target dose" ->
   * "Is there another antigen administered record to process?" Yes -> "Get next
   * antigen administered record". Both collections advance by one.
   */
  @Test
  public void satisfiedAdvancesBothTheTargetDoseAndTheRecord() throws Exception {
    assertAdvancesBothTargetDoseAndRecord(TargetDoseStatus.SATISFIED);
  }

  /**
   * The step package's implementation note records that SUBSTITUTED and
   * UNNECESSARY are treated the same way as SATISFIED by the
   * {@code TargetDoseStatus} switch - the specification's Figure 4-6 knows only
   * "satisfied / not satisfied", so these two extra statuses are an
   * implementation concept, pinned here.
   */
  @Test
  public void substitutedAndUnnecessaryAdvanceTheSameWayAsSatisfied() throws Exception {
    assertAdvancesBothTargetDoseAndRecord(TargetDoseStatus.SUBSTITUTED);
    setUp();
    assertAdvancesBothTargetDoseAndRecord(TargetDoseStatus.UNNECESSARY);
  }

  private void assertAdvancesBothTargetDoseAndRecord(TargetDoseStatus status) throws Exception {
    Antigen hepB = antigen(HEPB);
    SeriesDose doseOne = seriesDose("1");
    SeriesDose doseTwo = seriesDose("2");
    relevantPatientSeries("HepB 3 dose series", hepB, doseOne, doseTwo);
    AntigenAdministeredRecord firstRecord = administered(hepB, date(2011, 3, 1));
    AntigenAdministeredRecord secondRecord = administered(hepB, date(2011, 5, 1));

    process();
    dataModel.getTargetDose().setTargetDoseStatus(status);

    LogicStep next = process();

    assertSame(status + " advances to the next target dose", doseTwo,
        dataModel.getTargetDose().getTrackedSeriesDose());
    assertEquals(1, dataModel.getTargetDoseListPos());
    assertSame(status + " advances to the next antigen administered record", secondRecord,
        dataModel.getAntigenAdministeredRecord());
    assertEquals(1, dataModel.getSelectedAntigenAdministeredRecordPos());
    assertSame("The record just consumed is remembered as the previous one", firstRecord,
        dataModel.getPreviousAntigenAdministeredRecord());
    assertEquals(LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION, next.getLogicStepType());
  }

  /**
   * Implementation behaviour with no counterpart in Figure 4-6, which knows
   * nothing of a skipped target dose (conditional skip is Chapter 6): a SKIPPED
   * target dose advances the target dose only, keeping the same antigen
   * administered record so it can be evaluated against the dose after the
   * skipped one.
   */
  @Test
  public void skippedAdvancesTheTargetDoseButNotTheRecord() throws Exception {
    Antigen hepB = antigen(HEPB);
    SeriesDose doseOne = seriesDose("1");
    SeriesDose doseTwo = seriesDose("2");
    relevantPatientSeries("HepB 3 dose series", hepB, doseOne, doseTwo);
    AntigenAdministeredRecord firstRecord = administered(hepB, date(2011, 3, 1));

    process();
    dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SKIPPED);

    LogicStep next = process();

    assertSame(doseTwo, dataModel.getTargetDose().getTrackedSeriesDose());
    assertEquals(1, dataModel.getTargetDoseListPos());
    assertSame("The same record is carried over to the next target dose", firstRecord,
        dataModel.getAntigenAdministeredRecord());
    assertEquals(0, dataModel.getSelectedAntigenAdministeredRecordPos());
    assertEquals(LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION, next.getLogicStepType());
  }

  // ----------------------------------------------- Figure 4-6: recurring dose

  /**
   * Figure 4-6: satisfied -> "Is this target dose a recurring target dose?" Yes
   * -> "Create another target dose" -> "Get next target dose". The duplicate
   * tracks the same series dose (yearly flu, decennial Td), starts unsatisfied,
   * and becomes the target dose under evaluation, so the walk continues instead
   * of ending.
   */
  @Test
  public void satisfyingARecurringLastTargetDoseCreatesAnotherOneAndKeepsEvaluating() throws Exception {
    Antigen hepB = antigen(HEPB);
    SeriesDose recurring = seriesDose("1", YesNo.YES);
    relevantPatientSeries("Influenza yearly series", hepB, recurring);
    administered(hepB, date(2011, 3, 1));
    AntigenAdministeredRecord secondRecord = administered(hepB, date(2012, 3, 1));

    process();
    TargetDose firstTargetDose = dataModel.getTargetDose();
    dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SATISFIED);

    LogicStep next = process();

    assertEquals("A duplicate target dose is created for the recurring dose", 2,
        dataModel.getTargetDoseList().size());
    assertNotSame("The duplicate is a new target dose", firstTargetDose, dataModel.getTargetDose());
    assertSame("The duplicate tracks the same series dose", recurring,
        dataModel.getTargetDose().getTrackedSeriesDose());
    assertEquals("The duplicate starts unsatisfied", TargetDoseStatus.NOT_SATISFIED,
        dataModel.getTargetDose().getTargetDoseStatus());
    assertSame(firstTargetDose, dataModel.getPreviousTargetDose());
    assertSame("The next record is evaluated against the duplicate", secondRecord,
        dataModel.getAntigenAdministeredRecord());
    assertEquals("Evaluation continues rather than ending", LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION,
        next.getLogicStepType());
  }

  /**
   * Figure 4-6 asks "Is this target dose a recurring target dose?" of every
   * satisfied target dose, and creates another target dose whenever the answer
   * is Yes - the "Is there another target dose to process?" question is only
   * reached on the No branch. So a recurring dose that is <i>not</i> the last
   * dose of its series should still produce a duplicate when it is satisfied.
   *
   * <p>
   * This is not hypothetical: the bundled CDC Supporting Data releases contain
   * nine series doses with {@code <recurringDose>Yes</recurringDose>} that are
   * not the final dose of their series (all in COVID-19, e.g. "COVID-19 start
   * at 2 years+ shared clinical decision-making series" dose 1 of 2).
   *
   * <p>
   * The assertion is deliberately position-agnostic - it asserts only that the
   * duplicate exists, not where in the list it lands - because Figure 4-6 does
   * not say where "another target dose" is created, and the step package's
   * State Changes ("inserted immediately after it") and the implementation
   * (appended at the end of the list) read it differently.
   */
  @Test
  public void satisfyingARecurringMidSeriesTargetDoseAlsoCreatesAnotherOne() throws Exception {
    Antigen hepB = antigen(HEPB);
    SeriesDose recurring = seriesDose("1", YesNo.YES);
    SeriesDose doseTwo = seriesDose("2", YesNo.NO);
    relevantPatientSeries("COVID-19 shared clinical decision-making series", hepB, recurring, doseTwo);
    administered(hepB, date(2011, 3, 1));
    administered(hepB, date(2012, 3, 1));
    administered(hepB, date(2013, 3, 1));

    process();
    dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SATISFIED);

    process();

    assertEquals("A satisfied recurring dose creates another target dose wherever it sits in the series",
        2, targetDosesTracking(recurring));
  }

  // ------------------------------------------------- Figure 4-6: extraneous

  /**
   * Figure 4-6's terminal box: when the target doses run out ("Is there another
   * target dose to process?" -> No), "Set evaluation status to 'Extraneous' for
   * any un-evaluated antigen administered records". Here the first record
   * satisfies the only target dose and the two records after it are never
   * evaluated, so both should be marked extraneous.
   */
  @Test
  public void everyRecordLeftUnevaluatedWhenTargetDosesRunOutIsMarkedExtraneous() throws Exception {
    Antigen hepB = antigen(HEPB);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"));
    administered(hepB, date(2011, 3, 1));
    administered(hepB, date(2011, 5, 1));
    administered(hepB, date(2011, 7, 1));

    process();
    dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SATISFIED);

    process();

    assertEquals("Both records left after the target doses ran out are un-evaluated, so both are extraneous",
        Arrays.asList(date(2011, 5, 1), date(2011, 7, 1)), datesMarkedExtraneous());
  }

  /**
   * The same terminal box, for a series dose that carries an explicit Recurring
   * Dose attribute of "No" rather than omitting the attribute. Figure 4-6 asks
   * whether the dose recurs, and a "No" answer leads to the same "another
   * target dose? No -> mark the rest extraneous" path as a dose with no
   * recurring-dose data at all.
   *
   * <p>
   * This is the shape that matters in production: every {@code seriesDose} in
   * the bundled CDC Supporting Data releases carries a {@code recurringDose}
   * element (62 of 62 in the HepB release, for instance), and
   * {@code DomUtils.getInternalValueYesNo} maps "No" to {@code YesNo.NO}, so
   * the attribute-absent case this step also handles never actually occurs.
   */
  @Test
  public void anExplicitlyNonRecurringLastTargetDoseAlsoMarksTheRestExtraneous() throws Exception {
    Antigen hepB = antigen(HEPB);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1", YesNo.NO));
    administered(hepB, date(2011, 3, 1));
    administered(hepB, date(2011, 5, 1));
    administered(hepB, date(2011, 7, 1));

    process();
    dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SATISFIED);

    process();

    assertEquals("An explicit 'No' recurring dose is still a non-recurring last dose",
        Arrays.asList(date(2011, 5, 1), date(2011, 7, 1)), datesMarkedExtraneous());
  }

  /**
   * Figure 4-6: once the target doses are exhausted the evaluate loop is over
   * and control leaves it - forecasting (Chapter 7) begins for this series.
   */
  @Test
  public void runningOutOfTargetDosesEndsEvaluationAndBeginsForecasting() throws Exception {
    Antigen hepB = antigen(HEPB);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1", YesNo.NO));
    administered(hepB, date(2011, 3, 1));

    process();
    dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SATISFIED);

    LogicStep next = process();

    assertEquals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, next.getLogicStepType());
    assertSame(Neighborhood.FORECAST, dataModel.getNeighborhood());
  }

  /**
   * Purpose: "At the end of this step, each relevant patient series will have
   * an evaluated history and a forecast." The forecast object is created and
   * attached to the patient series when its evaluation ends, carrying the
   * series' antigen and the target dose evaluation stopped on.
   */
  @Test
  public void eachPatientSeriesGetsAForecastWhenItsEvaluationEnds() throws Exception {
    Antigen hepB = antigen(HEPB);
    PatientSeries patientSeries = relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"), seriesDose("2"));
    administered(hepB, date(2011, 3, 1));

    process();
    TargetDose targetDoseEvaluationStoppedOn = dataModel.getTargetDose();
    chapterSixRecordedEvaluation(EvaluationStatus.NOT_VALID);

    process();

    assertNotNull("The patient series carries its own forecast", patientSeries.getForecast());
    assertSame(patientSeries.getForecast(), dataModel.getForecast());
    assertSame(hepB, patientSeries.getForecast().getAntigen());
    assertSame(targetDoseEvaluationStoppedOn, patientSeries.getForecast().getTargetDose());
    assertSame("Chapter 7 is told which series it is forecasting for", patientSeries,
        dataModel.getForecastingForPatientSeries());
  }

  // ------------------------------------------- Figure 4-5: the outer series loop

  /**
   * Figure 4-5: after "Forecast Dates and Reasons" (Chapter 7), "Is there
   * another relevant patient series to process?" Yes -> "Get next relevant
   * patient series" -> evaluate it. The next series is set up from scratch: its
   * own target doses, its own antigen, and the records for that antigen.
   */
  @Test
  public void finishingAForecastMovesOnToTheNextRelevantPatientSeries() throws Exception {
    Antigen hepB = antigen(HEPB);
    Antigen polio = antigen(POLIO);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"));
    SeriesDose polioDose = seriesDose("1");
    PatientSeries polioSeries = relevantPatientSeries("Polio 4 dose series", polio, polioDose);
    administered(hepB, date(2011, 3, 1));
    AntigenAdministeredRecord polioRecord = administered(polio, date(2011, 4, 1));

    process();
    dataModel.setNeighborhood(Neighborhood.FORECAST);

    LogicStep next = process();

    assertSame("The second relevant patient series is now under evaluation", polioSeries,
        dataModel.getPatientSeriesStepper().getCurrent());
    assertSame(polio, dataModel.getAntigen());
    assertEquals(1, dataModel.getTargetDoseList().size());
    assertSame(polioDose, dataModel.getTargetDose().getTrackedSeriesDose());
    assertSame("The new series walks its own antigen's records", polioRecord,
        dataModel.getAntigenAdministeredRecord());
    assertSame(Neighborhood.EVALUATE, dataModel.getNeighborhood());
    assertEquals(LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION, next.getLogicStepType());
  }

  /**
   * {@code transitions.yaml}: "a patient series' forecast neighborhood still
   * has a target dose to forecast" -> 7.1. A target dose skipped for forecast
   * (Chapter 6's conditional skip for forecast) moves forecasting on to the
   * next target dose of the same series rather than ending the series.
   */
  @Test
  public void aSkippedTargetDoseInForecastMovesToTheNextTargetDoseOfTheSameSeries() throws Exception {
    Antigen hepB = antigen(HEPB);
    SeriesDose doseTwo = seriesDose("2");
    PatientSeries patientSeries = relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"), doseTwo);
    administered(hepB, date(2011, 3, 1));

    process();
    dataModel.setNeighborhood(Neighborhood.FORECAST);
    dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SKIPPED);

    LogicStep next = process();

    assertSame("The series being forecast does not change", patientSeries,
        dataModel.getPatientSeriesStepper().getCurrent());
    assertSame(doseTwo, dataModel.getTargetDose().getTrackedSeriesDose());
    assertSame(Neighborhood.FORECAST, dataModel.getNeighborhood());
    assertEquals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, next.getLogicStepType());
  }

  /**
   * Figure 4-5: "Is there another relevant patient series to process?" No ->
   * the loop ends. 4.4 hands off to 4.5 Select Best Patient Series, and clears
   * the per-series working state it was carrying so 4.5 does not see a stale
   * target dose, antigen, or record.
   */
  @Test
  public void finishingTheLastPatientSeriesTransitionsToSelectBestPatientSeries() throws Exception {
    Antigen hepB = antigen(HEPB);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"));
    administered(hepB, date(2011, 3, 1));

    process();
    dataModel.setNeighborhood(Neighborhood.FORECAST);

    LogicStep next = process();

    assertEquals(LogicStepType.SELECT_BEST_PATIENT_SERIES, next.getLogicStepType());
    assertSame(Neighborhood.SELECT_BEST_SERIES, dataModel.getNeighborhood());
    assertNull(dataModel.getTargetDose());
    assertNull(dataModel.getPreviousTargetDose());
    assertNull(dataModel.getTargetDoseList());
    assertEquals(-1, dataModel.getTargetDoseListPos());
    assertNull(dataModel.getAntigen());
    assertNull(dataModel.getAntigenAdministeredRecord());
    assertNull(dataModel.getSelectedAntigenAdministeredRecordList());
  }

  /**
   * Degenerate case of Figure 4-5's loop: no relevant patient series at all
   * means nothing to evaluate or forecast, so 4.4 goes straight to 4.5.
   */
  @Test
  public void noRelevantPatientSeriesAtAllGoesStraightToSelectBestPatientSeries() throws Exception {
    LogicStep next = process();

    assertEquals(LogicStepType.SELECT_BEST_PATIENT_SERIES, next.getLogicStepType());
    assertNull(dataModel.getTargetDose());
    assertNull(dataModel.getTargetDoseList());
  }

  // ---------------------------------------------------- implementation-only guards

  /**
   * Implementation-only loop guard with <b>no specification basis</b>, recorded
   * in the step package's Review Findings: after more than 1000 calls the step
   * forces a transition to 4.5 rather than let the evaluate/forecast loop run
   * on. Figures 4-5/4-6 describe the loop as terminating only when its
   * collections are exhausted and say nothing about a maximum iteration count.
   * Pinned so the escape hatch stays visible and keeps resetting its counters
   * on the way out.
   */
  @Test
  public void exceedingTheTotalCycleGuardForcesATransitionToSelectBestPatientSeries() throws Exception {
    Antigen hepB = antigen(HEPB);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"));
    administered(hepB, date(2011, 3, 1));
    for (int i = 0; i < 1001; i++) {
      dataModel.incrementEvaluateForecastTotalCycleCount();
    }

    LogicStep next = process();

    assertEquals(LogicStepType.SELECT_BEST_PATIENT_SERIES, next.getLogicStepType());
    assertSame(Neighborhood.SELECT_BEST_SERIES, dataModel.getNeighborhood());
    assertEquals("The guard resets its counters as it escapes", 0,
        dataModel.getEvaluateForecastTotalCycleCount());
  }

  /**
   * The second implementation-only loop guard, also with no specification
   * basis: the same loop state (neighborhood, series, both positions, target
   * dose status, both list sizes) repeating more than 200 times forces a
   * transition to 4.5. The signature the guard compares is built by a private
   * method, so it is primed reflectively here rather than by driving the engine
   * into a real 200-iteration stall.
   */
  @Test
  public void repeatingTheSameLoopStateForcesATransitionToSelectBestPatientSeries() throws Exception {
    Antigen hepB = antigen(HEPB);
    relevantPatientSeries("HepB 3 dose series", hepB, seriesDose("1"));
    administered(hepB, date(2011, 3, 1));

    EvaluateAndForecastAllPatientSeries step = new EvaluateAndForecastAllPatientSeries(dataModel);
    Method buildLoopSignature = EvaluateAndForecastAllPatientSeries.class.getDeclaredMethod("buildLoopSignature");
    buildLoopSignature.setAccessible(true);
    String loopSignature = (String) buildLoopSignature.invoke(step);
    for (int i = 0; i < 201; i++) {
      dataModel.recordEvaluateForecastLoopSignature(loopSignature);
    }

    LogicStep next = step.process();

    assertEquals(LogicStepType.SELECT_BEST_PATIENT_SERIES, next.getLogicStepType());
    assertSame(Neighborhood.SELECT_BEST_SERIES, dataModel.getNeighborhood());
    assertEquals("The guard resets its counters as it escapes", 0,
        dataModel.getEvaluateForecastRepeatedStateCount());
  }
}
