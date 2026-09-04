package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.ObservationCode;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientObservation;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Section 6.5 "Evaluate Preferable Interval" (Logic Specification for ACIP
 * Recommendations v4.6, pages 54-57, Figures 6-7 through 6-11, Table 6-17,
 * Table 6-18, Table 6-19) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/06-05-evaluate-preferable-interval/index.md}.
 *
 * <p>
 * 6.5 places the date the vaccine dose was administered on a timeline that two
 * calculated dates divide into three zones, and records the result:
 *
 * <pre>
 * Table 6-18 Did the Vaccine Dose Administered Satisfy the Preferable Interval
 *            for the Target Dose?
 *
 *   Condition                                                Rule 1  Rule 2  Rule 3
 *   Is the date administered &lt; absolute minimum
 *      interval date?                                          Yes      No      No
 *   Is the absolute minimum interval date &le; date
 *      administered &lt; minimum interval date?                    No     Yes      No
 *   Is the minimum interval date &le; date administered?         No      No     Yes
 *   Outcome                                                  Not     Satis-  Satis-
 *                                                            satis-  fied    fied
 *                                                            fied    (grace
 *                                                            (too    period)
 *                                                            soon)
 * </pre>
 *
 * <p>
 * The two calculated dates come from Table 6-19's business rules. CALCDTINT-3
 * and CALCDTINT-4 add the absolute minimum interval and the minimum interval to
 * a <em>patient's reference dose date</em>; CALCDTINT-1, CALCDTINT-2,
 * CALCDTINT-8 and CALCDTINT-9 are the four rules that decide which prior event
 * that reference date comes from - the immediate previous dose administered, the
 * dose that satisfied a named target dose number, the most recent dose of a
 * named vaccine type, or the most recent matching patient observation. Table
 * 6-17 gives both calculated dates an assumed value of 01/01/1900 for when they
 * cannot be calculated.
 *
 * <p>
 * The Purpose adds two rules the decision table does not carry: "In cases where
 * a target dose does not specify preferable interval attributes, the interval is
 * considered 'valid,'" and "if multiple intervals are specified, then all
 * intervals must be satisfied in order for the dose to satisfy the interval
 * requirements."
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads: the
 * {@code AntigenAdministeredRecord} 4.4 has made current (carrying the date
 * administered), the current {@code TargetDose} whose {@code SeriesDose} holds
 * the Supporting Data {@code Interval} records, and - because every reference
 * date rule is reached through {@code Interval.getPatientReferenceDoseDate},
 * which returns null unless one exists - the previous target dose and previous
 * antigen administered record. The current target dose is seeded with the
 * evaluation 6.4 leaves behind (status 'Valid', no reason), because 6.5's
 * outcomes write an evaluation reason into it rather than creating one.
 * {@code Interval}s are built the way {@code DataModelLoader.readSeriesDose}
 * builds them - both {@link TimePeriod}s always assigned, an absent Supporting
 * Data value becoming an unvalued {@code TimePeriod} - which is what the bundled
 * release's markup produces (all 490 of its non-empty {@code <interval>}
 * elements carry both {@code absMinInt} and {@code minInt}, valued or empty).
 *
 * <p>
 * The step's decision table is a {@code private} inner class, so it is read here
 * through the public {@code getLogicTableList()} as a plain {@link LogicTable}.
 * Unlike 6.1-6.4, 6.5 registers its condition attributes per interval in
 * {@code getConditionAttributesAdditionalMap()} under "Interval Check #<i>n</i>"
 * rather than in {@code getConditionAttributeList()}, which stays empty; that is
 * deliberate - Table 6-17's attributes are per-interval and a target dose can
 * carry several - so the map is what these tests read.
 *
 * <h2>What is deliberately not asserted</h2>
 *
 * <p>
 * Which step 6.5 hands off to when the preferable interval <em>is</em>
 * satisfied. The step's {@code process()} routes a satisfied interval straight
 * to 6.7 Evaluate Vaccine Conflict and only a failed one to 6.6 Evaluate
 * Allowable Interval, while the step package's {@code transitions.yaml} records
 * an unconditional transition to 6.6. Neither 6.5's nor 6.6's specification text
 * settles it - the ordering lives only in Figures 6-11 and 6-14, which are
 * images, and {@code transitions.yaml} itself records
 * {@code spec_defines_transition: false}. Only the not-satisfied direction,
 * which both documents agree on, is asserted here.
 *
 * <p>
 * The wording of Table 6-18's three condition rows and of its title question.
 * The step labels its table "Table 6 - 18 Did the vaccine dose administered
 * satisfy the defined interval?" where the specification writes "... satisfy the
 * preferable interval for the target dose?"; pinning either would assert a
 * transcription, so only the table number is pinned.
 *
 * <p>
 * Table 6-18's behaviour when the absolute minimum interval date falls
 * <em>after</em> the minimum interval date. The three conditions as encoded are
 * exhaustive only while absolute minimum &le; minimum; reversed, a dose before
 * both dates answers Yes to conditions 1 and 3 at once, no rule column matches,
 * no outcome runs and the interval is silently treated as satisfied. The
 * specification does not describe that case, and it does not occur in the
 * bundled Supporting Data release: of its 490 non-empty {@code <interval>}
 * elements, 489 give both {@code absMinInt} and {@code minInt} and 1 gives
 * {@code minInt} only (RSV Dose 1), and none gives {@code absMinInt} without
 * {@code minInt} - which is the only shape that would put the assumed 01/01/1900
 * minimum interval date before a calculated absolute minimum interval date.
 *
 * <p>
 * That 6.5 runs after 6.4 (its Entry Condition) and the ordering of the two
 * relative to each other: that is 6.4's transition, already covered by
 * {@code EvaluateAgeTest}.
 */
public class EvaluatePreferableIntervalTest {

  /** Table 6-17 row 1. */
  private static final String DATE_ADMINISTERED = "Date Administered";
  /** Table 6-17 row 2. */
  private static final String PREFERABLE_INTERVAL_ELEMENTS = "Preferable Interval Elements";
  /** Table 6-17 row 3, CALCDTINT-3. */
  private static final String ABSOLUTE_MINIMUM_INTERVAL_DATE = "Absolute Minimum Interval Date";
  /** Table 6-17 row 4, CALCDTINT-4. */
  private static final String MINIMUM_INTERVAL_DATE = "Minimum Interval Date";

  /** Table 6-17's assumed value for both calculated dates. */
  private static final String ASSUMED_INTERVAL_DATE = "01/01/1900";

  /** The date the previous dose was administered in every fixture below. */
  private static final String PREVIOUS_DOSE = "01/01/2016";

  private DataModel dataModel;
  private SeriesDose seriesDose;
  private TargetDose targetDose;
  private Evaluation evaluation;
  private AntigenAdministeredRecord antigenAdministeredRecord;
  private EvaluatePreferableInterval step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    Patient patient = new Patient();
    patient.setDateOfBirth(date("01/01/2015"));
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date("06/01/2021"));

    seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("2");
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    // 6.4 always runs first and always records an evaluation; 6.5's outcomes
    // write a reason into it rather than creating one. Rule 3 of Table 6-15
    // leaves status 'Valid' with no reason, which is what is seeded here.
    evaluation = new Evaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.VALID);
    targetDose.setEvaluation(evaluation);

    previousDoseAdministeredOn(PREVIOUS_DOSE, EvaluationStatus.VALID, null);
    administeredOn("02/15/2016");

    step = null;
  }

  // ---------------------------------------------------------------- fixtures

  private static Date date(String monthDayYear) {
    try {
      return new SimpleDateFormat("MM/dd/yyyy").parse(monthDayYear);
    } catch (java.text.ParseException pe) {
      throw new IllegalArgumentException(pe);
    }
  }

  private static AntigenAdministeredRecord administeredRecord(String monthDayYear, String cvxCode) {
    VaccineType vaccineType = new VaccineType();
    vaccineType.setCvxCode(cvxCode);
    vaccineType.setShortDescription("CVX " + cvxCode);

    Vaccine vaccine = new Vaccine();
    vaccine.setVaccineType(vaccineType);

    VaccineDoseAdministered vaccineDoseAdministered = new VaccineDoseAdministered();
    vaccineDoseAdministered.setVaccine(vaccine);
    vaccineDoseAdministered.setDateAdministered(date(monthDayYear));

    AntigenAdministeredRecord aar = new AntigenAdministeredRecord();
    aar.setDateAdministered(date(monthDayYear));
    aar.setVaccineType(vaccineType);
    aar.setVaccineDoseAdministered(vaccineDoseAdministered);
    return aar;
  }

  /** Stands in for 4.4 having made this vaccine dose administered the current one. */
  private void administeredOn(String monthDayYear) {
    antigenAdministeredRecord = administeredRecord(monthDayYear, "10"); // IPV
    dataModel.setAntigenAdministeredRecord(antigenAdministeredRecord);
  }

  /**
   * Stands in for the previous trip round 4.4's target dose loop: the previous
   * target dose, its evaluation, and the antigen administered record CALCDTINT-1
   * measures from.
   */
  private void previousDoseAdministeredOn(String monthDayYear, EvaluationStatus status,
      EvaluationReason reason) {
    SeriesDose previousSeriesDose = new SeriesDose();
    previousSeriesDose.setDoseNumber("1");
    TargetDose previousTargetDose = new TargetDose(previousSeriesDose);

    Evaluation previousEvaluation = new Evaluation();
    previousEvaluation.setEvaluationStatus(status);
    previousEvaluation.setEvaluationReason(reason);
    previousTargetDose.setEvaluation(previousEvaluation);

    dataModel.setPreviousTargetDose(previousTargetDose);
    dataModel.setPreviousAntigenAdministeredRecord(administeredRecord(monthDayYear, "10"));
  }

  /**
   * A Supporting Data preferable interval, written the way
   * {@code DataModelLoader.readSeriesDose} writes one: both {@link TimePeriod}s
   * assigned, an empty Supporting Data value becoming an unvalued
   * {@code TimePeriod}.
   */
  private Interval interval(YesNo fromImmediatePreviousDose, String absoluteMinimumInterval,
      String minimumInterval) {
    Interval interval = new Interval();
    interval.setSeriesDose(seriesDose);
    interval.setFromImmediatePreviousDoseAdministered(fromImmediatePreviousDose);
    interval.setAbsoluteMinimumInterval(new TimePeriod(absoluteMinimumInterval));
    interval.setMinimumInterval(new TimePeriod(minimumInterval));
    seriesDose.getIntervalList().add(interval);
    return interval;
  }

  /**
   * The interval window every rule-by-rule test below places a date administered
   * in - the bundled release's COVID-19 shape, absolute minimum 24 days and
   * minimum 28 days from the immediate previous dose administered. Previous dose
   * 01/01/2016 gives an absolute minimum interval date of 01/25/2016 and a
   * minimum interval date of 01/29/2016.
   */
  private Interval theStandardIntervalWindow() {
    return interval(YesNo.YES, "24 days", "28 days");
  }

  private LogicStep run() throws Exception {
    step = new EvaluatePreferableInterval(dataModel);
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  private List<ConditionAttribute<?>> attributesForInterval(int intervalNumber) {
    List<ConditionAttribute<?>> attributes = step.getConditionAttributesAdditionalMap()
        .get("Interval Check #" + intervalNumber);
    assertNotNull("Table 6-17's attributes for interval " + intervalNumber + " are not registered",
        attributes);
    return attributes;
  }

  private ConditionAttribute<?> attribute(int intervalNumber, int row) {
    return attributesForInterval(intervalNumber).get(row);
  }

  private void assertCalculatedDateIs(String monthDayYear, int row, String attributeName) {
    assertEquals("Table 6-17 '" + attributeName + "'",
        date(monthDayYear), attribute(1, row).getFinalValue());
  }

  private LogicTable tableSixEighteen() {
    assertEquals("one interval means one decision table", 1, step.getLogicTableList().size());
    return step.getLogicTableList().get(0);
  }

  private LogicResult conditionResult(int row) {
    return tableSixEighteen().getLogicConditions()[row].getLogicResult();
  }

  /**
   * Labels are compared with whitespace, case and the punctuation the two
   * documents disagree about removed - transcription differences, not
   * behavioural ones. A different table <em>number</em> is not.
   */
  private static String normalized(String label) {
    if (label == null) {
      return null;
    }
    return label.replaceAll("\\s+", "")
        .replace(".", "").replace("-", "").replace("–", "").replace("—", "")
        .replace("'", "").replace("‘", "").replace("’", "")
        .toLowerCase();
  }

  private static void assertLabelIs(String expected, String actual) {
    assertEquals("expected label '" + expected + "' but was '" + actual + "'",
        normalized(expected), normalized(actual));
  }

  // ==================================================== Entry: what 6.5's class is

  /** 6.5 identifies itself as {@code EVALUATE_PREFERABLE_INTERVAL}. */
  @Test
  public void theStepIsSixFive() throws Exception {
    theStandardIntervalWindow();

    run();

    assertEquals(LogicStepType.EVALUATE_PREFERABLE_INTERVAL, step.getLogicStepType());
    assertEquals("6.5", LogicStepType.EVALUATE_PREFERABLE_INTERVAL.getChapter());
  }

  /**
   * Every other step in the chapter-6 evaluation chain names its attribute table
   * - 6.3 "Table 6-12 Inadvertent Vaccine Attributes", 6.4 "Table 6-14 Age
   * Attributes" - so that the table the step publishes can be identified against
   * the specification. 6.5's is Table 6-17 Preferable Interval Attributes.
   */
  @Test
  public void theStepNamesTableSixSeventeenAsItsAttributeTable() throws Exception {
    theStandardIntervalWindow();

    run();

    assertLabelIs("Table 6-17 Preferable Interval Attributes", step.getConditionTableName());
  }

  // ============================================ Table 6-17: the four attributes

  /**
   * Table 6-17 lists four attributes: the vaccine dose administered's date
   * administered, the Supporting Data preferable interval elements, and the two
   * calculated dates. All four are registered for the interval, and the first two
   * carry the vaccine dose administered 4.4 has made current and the
   * {@code Interval} being checked.
   */
  @Test
  public void tableSixSeventeenRegistersFourAttributesCarryingTheirValues() throws Exception {
    Interval preferableInterval = theStandardIntervalWindow();
    administeredOn("02/15/2016");

    run();

    assertEquals("Table 6-17 lists exactly four attributes", 4, attributesForInterval(1).size());
    assertEquals("Vaccine dose administered", attribute(1, 0).getAttributeType());
    assertEquals(date("02/15/2016"), attribute(1, 0).getFinalValue());
    assertEquals("Supporting Data", attribute(1, 1).getAttributeType());
    assertSame("the second attribute carries the interval being checked",
        preferableInterval, attribute(1, 1).getFinalValue());
    assertEquals("Calculated Date", attribute(1, 2).getAttributeType());
    assertEquals("Calculated Date", attribute(1, 3).getAttributeType());
  }

  /**
   * Table 6-17's four attribute names, as the specification writes them. The
   * attribute name is what the step publishes for each row, so it is what
   * identifies the row against the specification's own table.
   */
  @Test
  public void tableSixSeventeenNamesItsAttributesAsTheSpecificationDoes() throws Exception {
    theStandardIntervalWindow();

    run();

    assertEquals(DATE_ADMINISTERED, attribute(1, 0).getAttributeName());
    assertEquals(PREFERABLE_INTERVAL_ELEMENTS, attribute(1, 1).getAttributeName());
    assertEquals(ABSOLUTE_MINIMUM_INTERVAL_DATE, attribute(1, 2).getAttributeName());
    assertEquals(MINIMUM_INTERVAL_DATE, attribute(1, 3).getAttributeName());
  }

  /**
   * Table 6-17's "Assumed Value if Empty" column: when neither calculated date
   * can be worked out - here because the interval names no reference dose date
   * rule that applies - both fall back to 01/01/1900.
   */
  @Test
  public void tableSixSeventeenAssumedValuesApplyWhenNeitherDateCanBeCalculated() throws Exception {
    interval(YesNo.NOT_APPLICABLE, "24 days", "28 days");

    run();

    assertCalculatedDateIs(ASSUMED_INTERVAL_DATE, 2, ABSOLUTE_MINIMUM_INTERVAL_DATE);
    assertCalculatedDateIs(ASSUMED_INTERVAL_DATE, 3, MINIMUM_INTERVAL_DATE);
  }

  /**
   * The same fallback applied to one date rather than both: an interval that
   * defines a minimum interval but no absolute minimum interval gets Table
   * 6-17's assumed 01/01/1900 absolute minimum interval date, which puts every
   * early dose inside Table 6-18 Rule 2's grace period rather than Rule 1's
   * too-soon zone. (The bundled release has exactly one such interval - RSV Dose
   * 1, {@code <absMinInt/>} empty with {@code <minInt>32 weeks</minInt>}.)
   */
  @Test
  public void anIntervalWithNoAbsoluteMinimumIntervalMakesAnEarlyDoseAGracePeriodDose()
      throws Exception {
    interval(YesNo.YES, "", "28 days");
    administeredOn("01/03/2016"); // long before the 01/29/2016 minimum interval date

    run();

    assertCalculatedDateIs(ASSUMED_INTERVAL_DATE, 2, ABSOLUTE_MINIMUM_INTERVAL_DATE);
    assertEquals(LogicResult.NO, conditionResult(0));
    assertEquals(LogicResult.YES, conditionResult(1));
    assertEquals(EvaluationReason.GRACE_PERIOD, evaluation.getEvaluationReason());
  }

  // ============================================== Table 6-18: shape of the table

  /**
   * Table 6-18's three conditions and its three-rule grid, as the specification
   * writes them.
   */
  @Test
  public void tableSixEighteenIsEncodedWithThreeConditionsAndThreeRules() throws Exception {
    theStandardIntervalWindow();

    run();
    LogicTable table = tableSixEighteen();

    assertTrue("the decision table identifies itself as Table 6-18, but was '"
        + table.getLabel() + "'", normalized(table.getLabel()).startsWith("table618"));
    assertEquals("Table 6-18 has three conditions", 3, table.getLogicConditions().length);
    assertEquals("Table 6-18 has three rules", 3, table.getLogicOutcomes().length);
    LogicResult[][] grid = table.getLogicResultTable();
    assertArrayEquals("Table 6-18 row 1: Yes / No / No",
        new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.NO }, grid[0]);
    assertArrayEquals("Table 6-18 row 2: No / Yes / No",
        new LogicResult[] { LogicResult.NO, LogicResult.YES, LogicResult.NO }, grid[1]);
    assertArrayEquals("Table 6-18 row 3: No / No / Yes",
        new LogicResult[] { LogicResult.NO, LogicResult.NO, LogicResult.YES }, grid[2]);
  }

  // ========================================================= Table 6-18 Rule 1

  /**
   * Table 6-18 Rule 1: the date administered is before the absolute minimum
   * interval date. "No. The vaccine dose administered did not satisfy the
   * preferable interval for the target dose." A failed preferable interval does
   * not reject the dose - it falls through to 6.6 Evaluate Allowable Interval.
   */
  @Test
  public void ruleOneReportsThePreferableIntervalAsNotSatisfied() throws Exception {
    theStandardIntervalWindow();
    administeredOn("01/20/2016"); // absolute minimum interval date is 01/25/2016

    run();

    assertEquals(LogicResult.YES, conditionResult(0));
    assertEquals("Rule 1 falls through to 6.6 Evaluate Allowable Interval",
        LogicStepType.EVALUATE_ALLOWABLE_INTERVAL, step.getNextLogicStepType());
  }

  /**
   * Table 6-18 Rule 1's evaluation reason: "Evaluation reason is 'Too soon'."
   * Rule 2, not Rule 1, is the grace-period case; 6.6's equivalent single
   * condition records {@code EvaluationReason.TOO_SOON} for the same situation.
   */
  @Test
  public void ruleOneRecordsEvaluationReasonTooSoon() throws Exception {
    theStandardIntervalWindow();
    administeredOn("01/20/2016"); // absolute minimum interval date is 01/25/2016

    run();

    assertEquals(LogicResult.YES, conditionResult(0));
    assertEquals("Rule 1: evaluation reason 'Too soon'",
        EvaluationReason.TOO_SOON, evaluation.getEvaluationReason());
  }

  // ========================================================= Table 6-18 Rule 2

  /**
   * Table 6-18 Rule 2: the date administered is on or after the absolute minimum
   * interval date but before the minimum interval date. "Yes. The vaccine dose
   * administered satisfied the preferable interval for the target dose.
   * Evaluation reason is 'Grace period'."
   */
  @Test
  public void ruleTwoAcceptsADoseInTheGracePeriodAsSatisfied() throws Exception {
    theStandardIntervalWindow();
    administeredOn("01/26/2016"); // between 01/25/2016 and 01/29/2016

    run();

    assertEquals(LogicResult.YES, conditionResult(1));
    assertEquals("Rule 2: evaluation reason 'Grace period'",
        EvaluationReason.GRACE_PERIOD, evaluation.getEvaluationReason());
    assertEquals("Rule 2 satisfies the interval, so 6.6 is not needed to rescue the dose",
        false, LogicStepType.EVALUATE_ALLOWABLE_INTERVAL.equals(step.getNextLogicStepType()));
  }

  // ========================================================= Table 6-18 Rule 3

  /**
   * Table 6-18 Rule 3: the date administered is on or after the minimum interval
   * date. "Yes. The vaccine dose administered satisfied the preferable interval
   * for the target dose." The specification gives no evaluation reason for the
   * plain-satisfied case, so 6.4's evaluation is left as it was.
   */
  @Test
  public void ruleThreeAcceptsADoseAtTheFullIntervalWithNoEvaluationReason() throws Exception {
    theStandardIntervalWindow();
    administeredOn("02/15/2016"); // after the 01/29/2016 minimum interval date

    run();

    assertEquals(LogicResult.YES, conditionResult(2));
    assertNull("Rule 3 states no evaluation reason", evaluation.getEvaluationReason());
    assertEquals("Rule 3 satisfies the interval, so 6.6 is not needed to rescue the dose",
        false, LogicStepType.EVALUATE_ALLOWABLE_INTERVAL.equals(step.getNextLogicStepType()));
  }

  // ===================================== Table 6-18's boundaries between the zones

  /**
   * Table 6-18's second condition is inclusive at its lower end ("Is the absolute
   * minimum interval date &le; date administered ...?"), so the absolute minimum
   * interval date itself is the first grace-period day, not the last too-soon
   * day.
   */
  @Test
  public void theAbsoluteMinimumIntervalDateItselfBeginsTheGracePeriod() throws Exception {
    theStandardIntervalWindow();
    administeredOn("01/25/2016"); // exactly the absolute minimum interval date

    run();

    assertEquals(LogicResult.NO, conditionResult(0));
    assertEquals(LogicResult.YES, conditionResult(1));
    assertEquals(EvaluationReason.GRACE_PERIOD, evaluation.getEvaluationReason());
  }

  /**
   * Table 6-18's third condition is inclusive at its lower end ("Is the minimum
   * interval date &le; date administered?") and its second exclusive at the same
   * boundary, so the minimum interval date itself is the first plainly satisfied
   * day rather than the last grace-period day.
   */
  @Test
  public void theMinimumIntervalDateItselfIsTheFirstPlainlySatisfiedDay() throws Exception {
    theStandardIntervalWindow();
    administeredOn("01/29/2016"); // exactly the minimum interval date

    run();

    assertEquals(LogicResult.NO, conditionResult(1));
    assertEquals(LogicResult.YES, conditionResult(2));
    assertNull("the first plainly satisfied day needs no evaluation reason",
        evaluation.getEvaluationReason());
  }

  // ================================================= Purpose: multiple intervals

  /**
   * The Purpose: "It is possible for a given dose to use multiple preferable
   * interval types. For example, dose 3 of HepB and dose 3 of HPV, each have two
   * preferable intervals." Each interval gets its own Table 6-18 check, with its
   * own Table 6-17 attribute block. (The bundled release has 74 series doses
   * carrying more than one non-empty {@code <interval>}.)
   */
  @Test
  public void eachPreferableIntervalGetsItsOwnTableSixEighteenCheck() throws Exception {
    interval(YesNo.YES, "24 days", "28 days");
    interval(YesNo.NO, "5 months - 4 days", "5 months"); // HPV dose 3's second interval shape

    run();

    assertEquals("two intervals means two decision tables", 2, step.getLogicTableList().size());
    assertEquals("two intervals means two Table 6-17 attribute blocks",
        2, step.getConditionAttributesAdditionalMap().size());
    assertEquals(4, attributesForInterval(1).size());
    assertEquals(4, attributesForInterval(2).size());
  }

  /**
   * The Purpose: "if multiple intervals are specified, then all intervals must be
   * satisfied in order for the dose to satisfy the interval requirements." One
   * failing interval is enough to make the whole preferable interval requirement
   * unsatisfied, even when another one passes.
   */
  @Test
  public void allIntervalsMustBeSatisfiedForThePreferableIntervalToBeSatisfied() throws Exception {
    interval(YesNo.YES, "24 days", "28 days"); // satisfied by 02/15/2016
    interval(YesNo.YES, "2 years", "2 years"); // not satisfied until 01/01/2018
    administeredOn("02/15/2016");

    run();

    assertEquals(2, step.getLogicTableList().size());
    assertEquals("one unsatisfied interval leaves the preferable interval unsatisfied, "
        + "which falls through to 6.6 Evaluate Allowable Interval",
        LogicStepType.EVALUATE_ALLOWABLE_INTERVAL, step.getNextLogicStepType());
  }

  /**
   * The same rule the other way round: when every interval is satisfied the
   * preferable interval requirement is satisfied and the dose does not need
   * rescuing by 6.6.
   */
  @Test
  public void everyIntervalSatisfiedSatisfiesThePreferableInterval() throws Exception {
    interval(YesNo.YES, "24 days", "28 days");
    interval(YesNo.YES, "1 month", "45 days"); // minimum interval date 02/15/2016
    administeredOn("02/15/2016");

    run();

    assertEquals(2, step.getLogicTableList().size());
    assertEquals("all intervals satisfied means 6.6 is not needed to rescue the dose",
        false, LogicStepType.EVALUATE_ALLOWABLE_INTERVAL.equals(step.getNextLogicStepType()));
  }

  // ============================================================ Purpose fallback

  /**
   * The Purpose: "In cases where a target dose does not specify preferable
   * interval attributes, the interval is considered 'valid.'" No intervals means
   * no Table 6-18 check at all, no evaluation reason recorded, and no fall
   * through to 6.6.
   */
  @Test
  public void aTargetDoseThatSpecifiesNoPreferableIntervalsIsConsideredValid() throws Exception {
    assertTrue("the fixture's series dose defines no intervals",
        seriesDose.getIntervalList().isEmpty());
    administeredOn("01/03/2016"); // a day that would fail any real interval

    run();

    assertEquals("no intervals means no decision table", 0, step.getLogicTableList().size());
    assertNull("an unspecified preferable interval records no evaluation reason",
        evaluation.getEvaluationReason());
    assertEquals("an unspecified preferable interval is valid, not a fall through to 6.6",
        false, LogicStepType.EVALUATE_ALLOWABLE_INTERVAL.equals(step.getNextLogicStepType()));
  }

  // ================================ Table 6-19: the two calculated-date rules

  /**
   * Table 6-19 CALCDTINT-3: "A patient's absolute minimum interval date must be
   * calculated as the patient's reference dose date plus the absolute minimum
   * interval." Reference dose date 01/01/2016 + 24 days = 01/25/2016.
   */
  @Test
  public void calcdtintThreeCalculatesTheAbsoluteMinimumIntervalDate() throws Exception {
    theStandardIntervalWindow();

    run();

    assertCalculatedDateIs("01/25/2016", 2, ABSOLUTE_MINIMUM_INTERVAL_DATE);
  }

  /**
   * Table 6-19 CALCDTINT-4: "A patient's minimum interval date must be calculated
   * as the patient's reference dose date plus the minimum interval." Reference
   * dose date 01/01/2016 + 28 days = 01/29/2016.
   */
  @Test
  public void calcdtintFourCalculatesTheMinimumIntervalDate() throws Exception {
    theStandardIntervalWindow();

    run();

    assertCalculatedDateIs("01/29/2016", 3, MINIMUM_INTERVAL_DATE);
  }

  // ============================= Table 6-19: the four reference dose date rules

  /**
   * Table 6-19 CALCDTINT-1: "A patient's reference dose date for an interval must
   * be calculated as the date administered of the most immediate previous vaccine
   * dose administered if all the following are true: the interval has a from
   * immediate previous dose administered flag of 'Y'; the vaccine dose
   * administered has an evaluation status of 'Valid' or 'Not Valid'; the vaccine
   * dose administered is not an inadvertent administration." (388 of the bundled
   * release's 490 non-empty intervals use this method.)
   */
  @Test
  public void calcdtintOneMeasuresFromTheImmediatePreviousDoseAdministered() throws Exception {
    previousDoseAdministeredOn("03/01/2016", EvaluationStatus.VALID, null);
    interval(YesNo.YES, "24 days", "28 days");

    run();

    assertCalculatedDateIs("03/25/2016", 2, ABSOLUTE_MINIMUM_INTERVAL_DATE);
    assertCalculatedDateIs("03/29/2016", 3, MINIMUM_INTERVAL_DATE);
  }

  /**
   * CALCDTINT-1's second bullet admits an evaluation status of 'Not Valid' as
   * well as 'Valid' - a dose that failed evaluation still spaces the next one.
   */
  @Test
  public void calcdtintOneAlsoMeasuresFromANotValidPreviousDose() throws Exception {
    previousDoseAdministeredOn("03/01/2016", EvaluationStatus.NOT_VALID, EvaluationReason.TOO_SOON);
    interval(YesNo.YES, "24 days", "28 days");

    run();

    assertCalculatedDateIs("03/25/2016", 2, ABSOLUTE_MINIMUM_INTERVAL_DATE);
  }

  /**
   * CALCDTINT-1's third bullet excludes an inadvertent administration. With no
   * other rule applying, no reference dose date can be calculated and Table
   * 6-17's assumed 01/01/1900 stands in for both calculated dates.
   */
  @Test
  public void calcdtintOneDoesNotMeasureFromAnInadvertentPreviousDose() throws Exception {
    previousDoseAdministeredOn("03/01/2016", EvaluationStatus.NOT_VALID,
        EvaluationReason.INADVERTENT_ADMINISTRATION);
    interval(YesNo.YES, "24 days", "28 days");

    run();

    assertCalculatedDateIs(ASSUMED_INTERVAL_DATE, 2, ABSOLUTE_MINIMUM_INTERVAL_DATE);
    assertCalculatedDateIs(ASSUMED_INTERVAL_DATE, 3, MINIMUM_INTERVAL_DATE);
  }

  /**
   * Table 6-19 CALCDTINT-2: "A patient's reference dose date for an interval must
   * be calculated as the date administered of the vaccine dose administered that
   * satisfies the target dose with the same target dose number as the from target
   * dose number in series if all the following are true for the interval: from
   * immediate previous dose administered flag is 'N'; from target dose number in
   * series is not 'N/A'." This is HPV dose 3's second interval - measured from
   * dose 1, absolute minimum 5 months - 4 days: 06/01/2015 + 5 months = 11/01/2015,
   * less 4 days = 10/28/2015. (41 of the bundled release's intervals use this
   * method: HepB 17, HPV 12, Meningococcal B 6, HepA 4, Pneumococcal 2.)
   */
  @Test
  public void calcdtintTwoMeasuresFromTheDoseThatSatisfiedANamedTargetDoseNumber()
      throws Exception {
    SeriesDose doseOne = new SeriesDose();
    doseOne.setDoseNumber("1");
    TargetDose targetDoseOne = new TargetDose(doseOne);
    VaccineDoseAdministered satisfyingDose = new VaccineDoseAdministered();
    satisfyingDose.setDateAdministered(date("06/01/2015"));
    targetDoseOne.setSatisfiedByVaccineDoseAdministered(satisfyingDose);
    List<TargetDose> targetDoseList = new ArrayList<TargetDose>();
    targetDoseList.add(targetDoseOne);
    dataModel.setTargetDoseList(targetDoseList);

    Interval preferableInterval = interval(YesNo.NO, "5 months - 4 days", "5 months");
    preferableInterval.setFromTargetDoseNumberInSeries("1");

    run();

    assertCalculatedDateIs("10/28/2015", 2, ABSOLUTE_MINIMUM_INTERVAL_DATE);
    assertCalculatedDateIs("11/01/2015", 3, MINIMUM_INTERVAL_DATE);
  }

  /**
   * Table 6-19 CALCDTINT-8: "A patient's reference dose date for an interval must
   * be calculated as the date administered of the most recent vaccine dose
   * administered that is the same vaccine type as the from most recent vaccine
   * type if all the following are true for the interval: from immediate previous
   * dose administered flag is 'N'; from most recent vaccine type is not 'N/A';
   * the vaccine dose administered is not an inadvertent administration." This is
   * the Pneumococcal case the Purpose names - spacing PPSV23 from the most recent
   * PCV13 (CVX 133) rather than from whatever happened to be given last.
   *
   * <p>
   * The immunization history here holds a PCV13 dose on 03/01/2016 and a later
   * dose of an unrelated vaccine type; the reference dose date is the PCV13 one,
   * so 8 weeks later is 04/26/2016. The previous dose's evaluation is 6.4 Rule
   * 3's plain-valid one - status 'Valid', no reason - which is the ordinary case.
   *
   * <p>
   * (55 of the bundled release's 490 non-empty intervals use this method:
   * COVID-19 31, Pneumococcal 14, Pertussis 4, Zoster 4, Meningococcal 2.)
   */
  @Test
  public void calcdtintEightMeasuresFromTheMostRecentDoseOfANamedVaccineType() throws Exception {
    VaccineType pcv13 = new VaccineType();
    pcv13.setCvxCode("133");
    pcv13.setShortDescription("CVX 133");

    dataModel.getAntigenAdministeredRecordList().add(administeredRecord("03/01/2016", "133"));
    dataModel.getAntigenAdministeredRecordList().add(administeredRecord("06/01/2016", "10"));

    Interval preferableInterval = interval(YesNo.NO, "8 weeks", "8 weeks");
    preferableInterval.setFromMostRecentVaccineType(pcv13);

    run();

    assertCalculatedDateIs("04/26/2016", 2, ABSOLUTE_MINIMUM_INTERVAL_DATE);
  }

  /**
   * Table 6-19 CALCDTINT-9: "A patient's reference dose date for an interval must
   * be calculated as the observation date of the most recent active patient
   * observation if all the following are true for the interval: from immediate
   * previous dose administered flag is 'N'; from relevant observation code is not
   * 'N/A'." This is the Pertussis case the specification names - "the interval
   * for a dose of Pertussis vaccine is measured from the date of the onset of
   * pregnancy" - with that antigen's own Supporting Data values: observation code
   * 170, absolute minimum interval 0 days, minimum interval 27 weeks. Onset of
   * pregnancy 01/10/2016 plus 27 weeks is 07/17/2016.
   *
   * <p>
   * (6 of the bundled release's intervals use this method, one each in Hib,
   * Measles, Mumps, Pertussis, RSV and Rubella - observation codes 171 "Date of
   * hematopoietic stem cell transplant", 120 "Begin Date of antiviral therapy
   * [ART]" and 170 "Onset of pregnancy".)
   */
  @Test
  public void calcdtintNineMeasuresFromTheMostRecentMatchingPatientObservation() throws Exception {
    ObservationCode onsetOfPregnancy = new ObservationCode();
    onsetOfPregnancy.setCode("170");
    onsetOfPregnancy.setText("Onset of pregnancy");

    PatientObservation patientObservation = new PatientObservation();
    patientObservation.setObservationCode(onsetOfPregnancy);
    patientObservation.setObservationDate(date("01/10/2016"));
    dataModel.getPatient().getMedicalHistory().getPatientObservationList().add(patientObservation);

    Interval preferableInterval = interval(YesNo.NO, "0 days", "27 weeks");
    preferableInterval.setFromRelevantObservation(onsetOfPregnancy);

    run();

    assertCalculatedDateIs("01/10/2016", 2, ABSOLUTE_MINIMUM_INTERVAL_DATE);
    assertCalculatedDateIs("07/17/2016", 3, MINIMUM_INTERVAL_DATE);
  }

  // ================== Table 6-19's reference methods against real Supporting Data

  /**
   * Confirms CALCDTINT-8's input can actually reach the step: the bundled
   * release's own markup for a "from most recent vaccine type" interval, read
   * through {@code DataModelLoader.readSeriesDose}, must produce an
   * {@code Interval} whose from most recent vaccine type is set. The markup below
   * is Pneumococcal's shape - {@code <fromPrevious>N</fromPrevious>} with a
   * {@code <fromMostRecent>} vaccine type list.
   */
  @Test
  public void theSupportingDatasFromMostRecentVaccineTypeReachesTheInterval() throws Exception {
    SeriesDose loaded = new SeriesDose();
    readSeriesDose(loaded, ""
        + "<seriesDose>"
        + "<doseNumber>Dose 2</doseNumber>"
        + "<interval>"
        + "<fromPrevious>N</fromPrevious>"
        + "<fromTargetDose />"
        + "<fromMostRecent>133</fromMostRecent>"
        + "<fromRelevantObs />"
        + "<absMinInt>8 weeks</absMinInt>"
        + "<minInt>8 weeks</minInt>"
        + "<earliestRecInt />"
        + "<latestRecInt />"
        + "<intervalPriority />"
        + "<effectiveDate />"
        + "<cessationDate />"
        + "</interval>"
        + "</seriesDose>");

    assertEquals("the loader read the series dose", "2", loaded.getDoseNumber());
    assertEquals("the interval is loaded", 1, loaded.getIntervalList().size());
    Interval loadedInterval = loaded.getIntervalList().get(0);
    assertEquals(YesNo.NO, loadedInterval.getFromImmediatePreviousDoseAdministered());
    assertNotNull("CALCDTINT-8 needs the interval's from most recent vaccine type",
        loadedInterval.getFromMostRecentVaccineType());
  }

  /**
   * The same for CALCDTINT-9: the bundled release's own markup for a "from
   * relevant observation code" interval - Pertussis Dose 1's, verbatim except for
   * the surrounding series dose - must produce an {@code Interval} carrying that
   * observation code.
   */
  @Test
  public void theSupportingDatasFromRelevantObservationCodeReachesTheInterval() throws Exception {
    SeriesDose loaded = new SeriesDose();
    readSeriesDose(loaded, ""
        + "<seriesDose>"
        + "<doseNumber>Dose 1</doseNumber>"
        + "<interval>"
        + "<fromPrevious>N</fromPrevious>"
        + "<fromTargetDose />"
        + "<fromMostRecent />"
        + "<fromRelevantObs>"
        + "<text>Onset of pregnancy</text>"
        + "<code>170</code>"
        + "</fromRelevantObs>"
        + "<absMinInt>0 days</absMinInt>"
        + "<minInt>27 weeks</minInt>"
        + "<earliestRecInt>27 weeks</earliestRecInt>"
        + "<latestRecInt>36 weeks</latestRecInt>"
        + "<intervalPriority />"
        + "<effectiveDate />"
        + "<cessationDate />"
        + "</interval>"
        + "</seriesDose>");

    assertEquals("the loader read the series dose", "1", loaded.getDoseNumber());
    assertEquals("the interval is loaded", 1, loaded.getIntervalList().size());
    Interval loadedInterval = loaded.getIntervalList().get(0);
    assertNotNull("CALCDTINT-9 needs the interval's from relevant observation code",
        loadedInterval.getFromRelevantObservation());
    assertEquals("170", loadedInterval.getFromRelevantObservation().getCode());
  }

  /**
   * Invokes {@code DataModelLoader.readSeriesDose} - private, like the loader's
   * other per-element readers - on one {@code <seriesDose>} element.
   */
  private void readSeriesDose(SeriesDose target, String seriesDoseXml) throws Exception {
    DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = documentBuilder.parse(
        new ByteArrayInputStream(seriesDoseXml.getBytes(Charset.forName("UTF-8"))));
    Node node = document.getDocumentElement();

    Method readSeriesDose = DataModelLoader.class.getDeclaredMethod("readSeriesDose",
        SeriesDose.class, Map.class, DataModel.class, Node.class);
    readSeriesDose.setAccessible(true);
    try {
      readSeriesDose.invoke(null, target, new HashMap<String, SeriesDose>(), dataModel, node);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof Exception) {
        throw (Exception) ite.getCause();
      }
      throw ite;
    }
  }
}
