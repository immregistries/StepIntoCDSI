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
import org.openimmunizationsoftware.cdsi.core.domain.AllowableInterval;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
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
 * Section 6.6 "Evaluate Allowable Interval" (Logic Specification for ACIP
 * Recommendations v4.6, pages 58-60, Figures 6-12 through 6-14, Table 6-20,
 * Table 6-21, Table 6-22) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/06-06-evaluate-allowable-interval/index.md}.
 *
 * <p>
 * 6.6 is the stricter backstop behind 6.5: where 6.5 grades a dose against a
 * three-zone preferable-interval timeline, 6.6 asks one question against one
 * calculated date.
 *
 * <pre>
 * Table 6-21 Did the Vaccine Dose Administered Satisfy the Allowable Interval
 *            for the Target Dose?
 *
 *   Condition                                             Rule 1   Rule 2
 *   Is the date administered &lt; absolute minimum
 *      interval date?                                       Yes      No
 *   Outcome                                               No. ...   Yes. ...
 *                                                         did not   satisfied
 *                                                         satisfy,
 *                                                         Evaluation
 *                                                         Reason is
 *                                                         'Too soon'.
 * </pre>
 *
 * <p>
 * The one calculated date comes from Table 6-22 CALCDTINT-3, "the patient's
 * reference dose date plus the absolute minimum interval". Table 6-22 gives 6.6
 * exactly two reference-dose-date rules rather than 6.5's four - CALCDTINT-1
 * (the immediate previous vaccine dose administered) and CALCDTINT-2 (the dose
 * that satisfied a named target dose number) - and the bundled Supporting Data
 * uses only those two shapes for allowable intervals. Table 6-20 gives the
 * absolute minimum interval date an assumed value of 01/01/1900 when it cannot
 * be calculated.
 *
 * <p>
 * The section text adds one rule the decision table does not carry, and it runs
 * the opposite way from most CDSi "unspecified means valid" defaults: "In cases
 * where a target dose does not specify allowable interval attributes, evaluate
 * allowable interval cannot be used to validate a vaccine dose administered. To
 * avoid a false validation, the allowable interval should be considered 'not
 * valid' in these cases."
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads: the
 * {@code AntigenAdministeredRecord} 4.4 has made current (carrying the date
 * administered), the current {@code TargetDose} whose {@code SeriesDose} holds
 * the Supporting Data {@code AllowableInterval} records, and - because both of
 * Table 6-22's reference date rules are reached through
 * {@code Interval.getPatientReferenceDoseDate}, which returns null outright
 * unless a previous target dose with an evaluation exists - the previous target
 * dose and previous antigen administered record. The current target dose is
 * seeded with the evaluation 6.4 leaves behind, because 6.6's Rule 1 writes an
 * evaluation reason into it rather than creating one.
 *
 * <p>
 * The fixtures use the bundled release's own allowable-interval shapes: the
 * Varicella childhood 2-dose series Dose 2 shape for CALCDTINT-1
 * ({@code <fromPrevious>Y</fromPrevious>}, {@code <absMinInt>4 weeks</absMinInt>})
 * and the HepB Heplisav-B 2-dose series Dose 2 shape for CALCDTINT-2
 * ({@code <fromPrevious>N</fromPrevious>}, {@code <fromTargetDose>1</fromTargetDose>},
 * {@code <absMinInt>4 weeks - 4 days</absMinInt>}).
 *
 * <p>
 * Unlike 6.5, 6.6 publishes Table 6-20's attributes once in
 * {@code getConditionAttributeList()} rather than per interval in
 * {@code getConditionAttributesAdditionalMap()}, so that list is what these
 * tests read. Its decision table is a {@code private} inner class, so it is read
 * through the public {@code getLogicTableList()} as a plain {@link LogicTable}.
 *
 * <h2>What is deliberately not asserted</h2>
 *
 * <p>
 * What evaluation reason, if any, the "no allowable interval attributes at all"
 * fallback should record. The section says the allowable interval "should be
 * considered 'not valid'" but Table 6-21's Rule 1 did not fire, and the
 * specification never says whether 'Too soon' - the reason belonging to a dose
 * that actually is too early - applies to a target dose that simply defines no
 * interval. Only the recorded interval failure itself is asserted.
 *
 * <p>
 * Whether a Rule 2 (satisfied) outcome should clear an evaluation reason left
 * behind by 6.5, or whether Rule 1 should overwrite one. Table 6-21's outcomes
 * name only what 6.6 records, not what it preserves, and 6.5's own reason
 * handling is under investigation in that unit.
 *
 * <p>
 * The evaluation <em>status</em>. Table 6-21 Rule 1 records an evaluation reason
 * and a failed interval; section 6.10 Evaluate Target Dose is where the status
 * is decided from {@code statusCause}, so asserting a status here would test the
 * wrong section.
 *
 * <p>
 * The wording of Table 6-21's condition row and of its title question. The step
 * labels its table "Table 6 - 21 Did the vaccine dose administered satisfy the
 * defined Allowable interval?" against the specification's "... satisfy the
 * allowable interval for the target dose?"; pinning either would assert a
 * transcription rather than behaviour, so only the table number is pinned,
 * following 6.4's and 6.5's precedent.
 *
 * <p>
 * Table 6-20's effective date and cessation date handling. The loader reads both
 * onto {@code AllowableInterval} but nothing in 6.6 consults them; selecting
 * supporting data by date is section 3.3's subject, not 6.6's.
 *
 * <p>
 * The exact contents of the target dose's status cause. 6.6's {@code process()}
 * ends by calling {@code next()}, and {@code EvaluateVaccineConflict}'s
 * constructor evaluates its own Table 4-20 straight away, whose third rule
 * appends a "VirusConflict" marker to the same field before control returns
 * here. That is 6.7's behaviour to justify or fix in its own unit, so these
 * tests only ever assert the presence or absence of 6.6's own "Interval" marker.
 *
 * <p>
 * The structured log events, for the same reason 6.3, 6.4 and 6.5 left theirs
 * alone.
 */
public class EvaluateAllowableIntervalTest {

  /** Table 6-20 row 1. */
  private static final String DATE_ADMINISTERED = "Date Administered";
  /** Table 6-20 row 2. */
  private static final String ALLOWABLE_INTERVAL_ELEMENTS = "Allowable Interval elements";
  /** Table 6-20 row 3, CALCDTINT-3. */
  private static final String ABSOLUTE_MINIMUM_INTERVAL_DATE = "Absolute Minimum Interval Date";

  /** Table 6-20's assumed value for the one calculated date. */
  private static final String ASSUMED_INTERVAL_DATE = "01/01/1900";

  /**
   * The marker 6.6 appends to the target dose's status cause when the allowable
   * interval is not satisfied, for 6.10 Evaluate Target Dose to read later.
   */
  private static final String INTERVAL_STATUS_CAUSE = "Interval";

  /** The date the previous dose was administered in every fixture below. */
  private static final String PREVIOUS_DOSE = "01/01/2016";

  private DataModel dataModel;
  private SeriesDose seriesDose;
  private TargetDose targetDose;
  private Evaluation evaluation;
  private EvaluateAllowableInterval step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    Patient patient = new Patient();
    patient.setDateOfBirth(date("01/01/2015"));
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date("06/01/2021"));
    dataModel.setTargetDoseList(new ArrayList<TargetDose>());

    seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("2");
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    // 6.4 always runs first and always records an evaluation; 6.6's Rule 1 writes
    // a reason into it rather than creating one.
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
    dataModel.setAntigenAdministeredRecord(administeredRecord(monthDayYear, "21")); // Varicella
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
    dataModel.setPreviousAntigenAdministeredRecord(administeredRecord(monthDayYear, "21"));
  }

  /**
   * A Supporting Data allowable interval, written the way
   * {@code DataModelLoader.readSeriesDose} writes one.
   */
  private AllowableInterval allowableInterval(YesNo fromImmediatePreviousDose,
      String fromTargetDoseNumberInSeries, String absoluteMinimumInterval) {
    AllowableInterval allowableInterval = new AllowableInterval();
    allowableInterval.setSeriesDose(seriesDose);
    allowableInterval.setFromImmediatePreviousDoseAdministered(fromImmediatePreviousDose);
    allowableInterval.setFromTargetDoseNumberInSeries(fromTargetDoseNumberInSeries);
    allowableInterval.setAbsoluteMinimumInterval(new TimePeriod(absoluteMinimumInterval));
    seriesDose.getAllowableintervalList().add(allowableInterval);
    return allowableInterval;
  }

  /**
   * The allowable interval every rule-by-rule test below measures against - the
   * bundled release's Varicella childhood 2-dose series Dose 2 shape, from the
   * immediate previous dose administered with an absolute minimum interval of 4
   * weeks. Previous dose 01/01/2016 gives an absolute minimum interval date of
   * 01/29/2016.
   */
  private AllowableInterval theStandardAllowableInterval() {
    return allowableInterval(YesNo.YES, "", "4 weeks");
  }

  private LogicStep run() throws Exception {
    step = new EvaluateAllowableInterval(dataModel);
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  private ConditionAttribute<?> attribute(int row) {
    List<ConditionAttribute<?>> attributes = step.getConditionAttributeList();
    assertTrue("Table 6-20 row " + (row + 1) + " is not registered", attributes.size() > row);
    return attributes.get(row);
  }

  private LogicTable tableSixTwentyOne() {
    assertEquals("one allowable interval means one decision table",
        1, step.getLogicTableList().size());
    return step.getLogicTableList().get(0);
  }

  private LogicResult conditionResult() {
    return tableSixTwentyOne().getLogicConditions()[0].getLogicResult();
  }

  /**
   * The status cause is only ever inspected for 6.6's own "Interval" marker,
   * never compared whole. {@code process()} ends by calling {@code next()}, which
   * constructs 6.7 Evaluate Vaccine Conflict, and that class's constructor
   * evaluates its Table 4-20 immediately and appends its own "VirusConflict"
   * marker before this test regains control. That is 6.7's behaviour, not 6.6's,
   * and belongs to 6.7's own unit.
   */
  private void assertIntervalFailureRecorded(String why) {
    assertTrue(why + ", but the status cause was '" + targetDose.getStatusCause() + "'",
        targetDose.getStatusCause().contains(INTERVAL_STATUS_CAUSE));
  }

  private void assertNoIntervalFailureRecorded(String why) {
    assertTrue(why + ", but the status cause was '" + targetDose.getStatusCause() + "'",
        !targetDose.getStatusCause().contains(INTERVAL_STATUS_CAUSE));
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

  // ==================================================== Entry: what 6.6's class is

  /** 6.6 identifies itself as {@code EVALUATE_ALLOWABLE_INTERVAL}. */
  @Test
  public void theStepIsSixSix() throws Exception {
    theStandardAllowableInterval();

    run();

    assertEquals(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL, step.getLogicStepType());
    assertEquals("6.6", LogicStepType.EVALUATE_ALLOWABLE_INTERVAL.getChapter());
  }

  /**
   * Every other step in the chapter-6 evaluation chain names its attribute table
   * - 6.3 "Table 6-12 Inadvertent Vaccine Attributes", 6.4 "Table 6-14 Age
   * Attributes" - so that the table the step publishes can be identified against
   * the specification. 6.6's is Table 6-20 Allowable Interval Attributes.
   */
  @Test
  public void theStepNamesTableSixTwentyAsItsAttributeTable() throws Exception {
    theStandardAllowableInterval();

    run();

    assertLabelIs("Table 6-20 Allowable Interval Attributes", step.getConditionTableName());
  }

  // ============================================ Table 6-20: the three attributes

  /**
   * Table 6-20 lists three attributes and no more: the vaccine dose
   * administered's date administered, the Supporting Data allowable interval
   * elements, and the one calculated date. The first two carry the vaccine dose
   * administered 4.4 has made current and the {@code AllowableInterval} being
   * checked. (6.6 has no Minimum Interval Date row - that is 6.5's Table 6-17,
   * which lists four.)
   */
  @Test
  public void tableSixTwentyRegistersThreeAttributesCarryingTheirValues() throws Exception {
    AllowableInterval theAllowableInterval = theStandardAllowableInterval();
    administeredOn("02/15/2016");

    run();

    assertEquals("Table 6-20 lists exactly three attributes",
        3, step.getConditionAttributeList().size());
    assertEquals("Vaccine dose administered", attribute(0).getAttributeType());
    assertEquals(date("02/15/2016"), attribute(0).getFinalValue());
    assertEquals("Supporting Data", attribute(1).getAttributeType());
    assertSame("the second attribute carries the allowable interval being checked",
        theAllowableInterval, attribute(1).getFinalValue());
    assertTrue("Table 6-20 row 3 is a calculated date, but its type was '"
        + attribute(2).getAttributeType() + "'",
        normalized(attribute(2).getAttributeType()).startsWith("calculateddate"));
  }

  /**
   * Table 6-20's three attribute names, as the specification writes them. The
   * attribute name is what the step publishes for each row, so it is what
   * identifies the row against the specification's own table.
   */
  @Test
  public void tableSixTwentyNamesItsAttributesAsTheSpecificationDoes() throws Exception {
    theStandardAllowableInterval();

    run();

    assertLabelIs(DATE_ADMINISTERED, attribute(0).getAttributeName());
    assertLabelIs(ALLOWABLE_INTERVAL_ELEMENTS, attribute(1).getAttributeName());
    assertLabelIs(ABSOLUTE_MINIMUM_INTERVAL_DATE, attribute(2).getAttributeName());
  }

  /**
   * Table 6-20's "Assumed Value if Empty" column: when the absolute minimum
   * interval date cannot be worked out - here because the interval names a target
   * dose number no target dose has been satisfied by yet - it falls back to
   * 01/01/1900, which makes Table 6-21 Rule 2 true for every date administered.
   */
  @Test
  public void tableSixTwentyAssumedValueAppliesWhenTheDateCannotBeCalculated() throws Exception {
    allowableInterval(YesNo.NO, "1", "4 weeks - 4 days"); // no target dose satisfied yet

    run();

    assertEquals("Table 6-20 '" + ABSOLUTE_MINIMUM_INTERVAL_DATE + "'",
        date(ASSUMED_INTERVAL_DATE), attribute(2).getFinalValue());
    assertEquals(LogicResult.NO, conditionResult());
  }

  // ============================================== Table 6-21: shape of the table

  /**
   * Table 6-21's single condition and its two-rule grid, as the specification
   * writes them. 6.6 has no grace-period zone: one condition, Yes then No.
   */
  @Test
  public void tableSixTwentyOneIsEncodedWithOneConditionAndTwoRules() throws Exception {
    theStandardAllowableInterval();

    run();
    LogicTable table = tableSixTwentyOne();

    assertTrue("the decision table identifies itself as Table 6-21, but was '"
        + table.getLabel() + "'", normalized(table.getLabel()).startsWith("table621"));
    assertEquals("Table 6-21 has one condition", 1, table.getLogicConditions().length);
    assertEquals("Table 6-21 has two rules", 2, table.getLogicOutcomes().length);
    assertArrayEquals("Table 6-21 row 1: Yes / No",
        new LogicResult[] { LogicResult.YES, LogicResult.NO },
        table.getLogicResultTable()[0]);
  }

  // ========================================================= Table 6-21 Rule 1

  /**
   * Table 6-21 Rule 1: the date administered is before the absolute minimum
   * interval date. "No. The vaccine dose administered did not satisfy the
   * allowable interval for the target dose." The failure is recorded on the
   * target dose's status cause for 6.10 Evaluate Target Dose to read later.
   */
  @Test
  public void ruleOneReportsTheAllowableIntervalAsNotSatisfied() throws Exception {
    theStandardAllowableInterval();
    administeredOn("01/20/2016"); // absolute minimum interval date is 01/29/2016

    run();

    assertEquals(LogicResult.YES, conditionResult());
    assertIntervalFailureRecorded(
        "Rule 1 records the interval failure on the target dose's status cause");
  }

  /**
   * Table 6-21 Rule 1's evaluation reason: "Evaluation Reason is 'Too soon'."
   * This is the case 6.5's Review Findings compare against - 6.5's Table 6-18
   * Rule 1 sets {@code GRACE_PERIOD} where its own log line says 'Too Soon'; 6.6
   * has no grace-period rule at all, so 'Too soon' is the only reason Table 6-21
   * names.
   */
  @Test
  public void ruleOneRecordsEvaluationReasonTooSoon() throws Exception {
    theStandardAllowableInterval();
    administeredOn("01/20/2016"); // absolute minimum interval date is 01/29/2016

    run();

    assertEquals(LogicResult.YES, conditionResult());
    assertEquals("Rule 1: evaluation reason 'Too soon'",
        EvaluationReason.TOO_SOON, evaluation.getEvaluationReason());
  }

  /**
   * The interval failure is appended to whatever status cause the earlier
   * chapter-6 steps have already recorded rather than replacing it, so a dose
   * that failed both age and interval carries both markers into 6.10.
   */
  @Test
  public void ruleOneAppendsToAnExistingStatusCause() throws Exception {
    targetDose.setStatusCause("Age");
    theStandardAllowableInterval();
    administeredOn("01/20/2016");

    run();

    assertTrue("the interval failure is appended to the existing status cause rather than "
        + "replacing it, but the status cause was '" + targetDose.getStatusCause() + "'",
        targetDose.getStatusCause().startsWith("AgeInterval"));
  }

  // ========================================================= Table 6-21 Rule 2

  /**
   * Table 6-21 Rule 2: the date administered is not before the absolute minimum
   * interval date. "Yes. The vaccine dose administered satisfied the allowable
   * interval for the target dose." Rule 2 names no evaluation reason and no
   * interval failure.
   */
  @Test
  public void ruleTwoAcceptsADoseAtOrAfterTheAbsoluteMinimumIntervalDate() throws Exception {
    theStandardAllowableInterval();
    administeredOn("02/15/2016"); // after the 01/29/2016 absolute minimum interval date

    run();

    assertEquals(LogicResult.NO, conditionResult());
    assertNull("Rule 2 states no evaluation reason", evaluation.getEvaluationReason());
    assertNoIntervalFailureRecorded("Rule 2 records no interval failure");
  }

  /**
   * Table 6-21's only condition is strictly "&lt;", so the absolute minimum
   * interval date itself is the first satisfying day rather than the last
   * too-soon day. 6.5's Table 6-18 draws its first boundary at the same date and
   * in the same direction.
   */
  @Test
  public void theAbsoluteMinimumIntervalDateItselfSatisfiesTheAllowableInterval() throws Exception {
    theStandardAllowableInterval();
    administeredOn("01/29/2016"); // exactly the absolute minimum interval date

    run();

    assertEquals(LogicResult.NO, conditionResult());
    assertNull(evaluation.getEvaluationReason());
    assertNoIntervalFailureRecorded("the absolute minimum interval date itself is satisfying");
  }

  /** The day before it is still Rule 1's too-soon zone. */
  @Test
  public void theDayBeforeTheAbsoluteMinimumIntervalDateIsTooSoon() throws Exception {
    theStandardAllowableInterval();
    administeredOn("01/28/2016"); // one day before the absolute minimum interval date

    run();

    assertEquals(LogicResult.YES, conditionResult());
    assertEquals(EvaluationReason.TOO_SOON, evaluation.getEvaluationReason());
  }

  // ============================================ Section 6.6's 'not valid' fallback

  /**
   * Section 6.6: "In cases where a target dose does not specify allowable
   * interval attributes, evaluate allowable interval cannot be used to validate a
   * vaccine dose administered. To avoid a false validation, the allowable
   * interval should be considered 'not valid' in these cases." No allowable
   * interval means no Table 6-21 check at all, and the interval failure recorded
   * anyway - the opposite default from 6.5, where an unspecified preferable
   * interval is considered valid.
   *
   * <p>
   * This is the dominant path in practice: of the 484 {@code <seriesDose>}
   * elements in the bundled Supporting Data release, 465 carry a bare
   * {@code <allowableInterval/>} and only 19 carry a populated one.
   */
  @Test
  public void aTargetDoseThatSpecifiesNoAllowableIntervalsIsConsideredNotValid() throws Exception {
    assertTrue("the fixture's series dose defines no allowable intervals",
        seriesDose.getAllowableintervalList().isEmpty());
    administeredOn("06/01/2020"); // a day that would satisfy any real allowable interval

    run();

    assertEquals("no allowable interval means no decision table",
        0, step.getLogicTableList().size());
    assertIntervalFailureRecorded("an unspecified allowable interval is 'not valid', "
        + "so the interval failure is recorded anyway");
  }

  // ================================ Table 6-22: the business rules 6.6 defines

  /**
   * Table 6-22 CALCDTINT-3: "A patient's absolute minimum interval date must be
   * calculated as the patient's reference dose date plus the absolute minimum
   * interval." Reference dose date 01/01/2016 + 4 weeks = 01/29/2016.
   */
  @Test
  public void calcdtintThreeCalculatesTheAbsoluteMinimumIntervalDate() throws Exception {
    theStandardAllowableInterval();

    run();

    assertEquals(date("01/29/2016"), attribute(2).getFinalValue());
  }

  /**
   * Table 6-22 CALCDTINT-1: "A patient's reference dose date for an interval must
   * be calculated as the date administered of the most immediate previous vaccine
   * dose administered if all the following are true: the interval has a from
   * immediate previous dose administered flag of 'Y'; the vaccine dose
   * administered has an evaluation status of 'Valid' or 'Not Valid'; the vaccine
   * dose administered is not an inadvertent administration." (7 of the bundled
   * release's 19 populated allowable intervals use this method - Diphtheria,
   * Pertussis and Tetanus Dose 4 at "4 months - 4 days", and Varicella Dose 2 at
   * "4 weeks".)
   */
  @Test
  public void calcdtintOneMeasuresFromTheImmediatePreviousDoseAdministered() throws Exception {
    previousDoseAdministeredOn("03/01/2016", EvaluationStatus.VALID, null);
    theStandardAllowableInterval();

    run();

    assertEquals(date("03/29/2016"), attribute(2).getFinalValue());
  }

  /**
   * CALCDTINT-1's second bullet admits an evaluation status of 'Not Valid' as
   * well as 'Valid' - a dose that failed evaluation still spaces the next one.
   */
  @Test
  public void calcdtintOneAlsoMeasuresFromANotValidPreviousDose() throws Exception {
    previousDoseAdministeredOn("03/01/2016", EvaluationStatus.NOT_VALID, EvaluationReason.TOO_SOON);
    theStandardAllowableInterval();

    run();

    assertEquals(date("03/29/2016"), attribute(2).getFinalValue());
  }

  /**
   * CALCDTINT-1's third bullet excludes an inadvertent administration. With no
   * other rule applying, no reference dose date can be calculated and Table
   * 6-20's assumed 01/01/1900 stands in - which makes Table 6-21 Rule 2 true.
   */
  @Test
  public void calcdtintOneDoesNotMeasureFromAnInadvertentPreviousDose() throws Exception {
    previousDoseAdministeredOn("03/01/2016", EvaluationStatus.NOT_VALID,
        EvaluationReason.INADVERTENT_ADMINISTRATION);
    theStandardAllowableInterval();

    run();

    assertEquals(date(ASSUMED_INTERVAL_DATE), attribute(2).getFinalValue());
    assertEquals(LogicResult.NO, conditionResult());
  }

  /**
   * Table 6-22 CALCDTINT-2: "A patient's reference dose date for an interval must
   * be calculated as the date administered of the vaccine dose administered that
   * satisfies the target dose with the same target dose number as the from target
   * dose number in series if all the following are true for the interval: from
   * immediate previous dose administered flag is 'N'; from target dose number in
   * series is not 'n/a'." This is the HepB Heplisav-B 2-dose series Dose 2 shape -
   * measured from dose 1, absolute minimum "4 weeks - 4 days": 06/01/2015 plus 4
   * weeks is 06/29/2015, less 4 days is 06/25/2015.
   *
   * <p>
   * (12 of the bundled release's 19 populated allowable intervals use this
   * method: HepB 6, Meningococcal B 4, HepA 2.)
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
    dataModel.getTargetDoseList().add(targetDoseOne);

    allowableInterval(YesNo.NO, "1", "4 weeks - 4 days");

    run();

    assertEquals(date("06/25/2015"), attribute(2).getFinalValue());
  }

  // ============================ The Purpose: "allowable interval(s)", plural

  /**
   * The Purpose validates the date administered "against defined allowable
   * interval(s)", plural, and the step gives each one its own Table 6-21 check.
   */
  @Test
  public void eachAllowableIntervalGetsItsOwnTableSixTwentyOneCheck() throws Exception {
    allowableInterval(YesNo.YES, "", "4 weeks");
    allowableInterval(YesNo.YES, "", "4 months - 4 days");

    run();

    assertEquals("two allowable intervals means two decision tables",
        2, step.getLogicTableList().size());
  }

  /**
   * Table 6-22 CALCDTINT-3 calculates an absolute minimum interval date from
   * <em>the</em> interval's own absolute minimum interval, so each Table 6-21
   * check must be answered against its own calculated date. Here the first
   * allowable interval (2 years from the previous dose, so 01/01/2018) is not
   * satisfied by a dose given on 02/15/2016 and the second (4 weeks, so
   * 01/29/2016) is; the section's own "to avoid a false validation" principle
   * means the unsatisfied one has to be reported.
   *
   * <p>
   * The step builds one Table 6-21 per allowable interval but all of them read
   * the same three step-level {@code ConditionAttribute} objects, which the
   * constructor overwrites once per interval - so every table is answered against
   * the last interval's absolute minimum interval date. (6.5 avoids this by
   * giving each of its tables its own attribute objects.)
   *
   * <p>
   * No series dose in the bundled Supporting Data release carries more than one
   * populated {@code <allowableInterval>} - all 19 sit on distinct series doses -
   * so this has no effect on the bundled data as it stands.
   */
  @Test
  public void eachAllowableIntervalIsCheckedAgainstItsOwnAbsoluteMinimumIntervalDate()
      throws Exception {
    allowableInterval(YesNo.YES, "", "2 years"); // 01/01/2018 - not satisfied
    allowableInterval(YesNo.YES, "", "4 weeks"); // 01/29/2016 - satisfied
    administeredOn("02/15/2016");

    run();

    assertEquals(2, step.getLogicTableList().size());
    assertIntervalFailureRecorded("an allowable interval the dose does not satisfy must be "
        + "reported as an interval failure even when another one is satisfied");
  }

  // ================================================================ Next step

  /**
   * The step package's {@code transitions.yaml}: unconditional to 6.7 Evaluate
   * Vaccine Conflict. Unlike 6.5, 6.6 does not branch on its own outcome - the
   * failure, if any, is recorded on the status cause for 6.10 to read later.
   */
  @Test
  public void aSatisfiedAllowableIntervalContinuesToSixSeven() throws Exception {
    theStandardAllowableInterval();
    administeredOn("02/15/2016");

    run();

    assertEquals(LogicStepType.EVALUATE_VACCINE_CONFLICT, step.getNextLogicStepType());
  }

  /** The same transition when the allowable interval is not satisfied. */
  @Test
  public void anUnsatisfiedAllowableIntervalAlsoContinuesToSixSeven() throws Exception {
    theStandardAllowableInterval();
    administeredOn("01/20/2016");

    run();

    assertEquals(LogicStepType.EVALUATE_VACCINE_CONFLICT, step.getNextLogicStepType());
  }

  /** And when the target dose specifies no allowable interval at all. */
  @Test
  public void aTargetDoseWithNoAllowableIntervalAlsoContinuesToSixSeven() throws Exception {
    run();

    assertEquals(LogicStepType.EVALUATE_VACCINE_CONFLICT, step.getNextLogicStepType());
  }

  // ================== Table 6-20's Supporting Data row against the real release

  /**
   * Confirms Table 6-20's "Allowable Interval elements" row can actually be
   * filled from the bundled release: its own markup for a CALCDTINT-2 allowable
   * interval - HepB Heplisav-B 2-dose series Dose 2's, verbatim except for the
   * surrounding series dose - read through {@code DataModelLoader.readSeriesDose},
   * must produce an {@code AllowableInterval} carrying all three of the elements
   * 6.6 reads.
   *
   * <p>
   * This is the check that found two holes in 6.5's equivalent
   * {@code <interval>} branch; 6.6's two reference date rules use only
   * {@code fromPrevious} and {@code fromTargetDose}, which that branch does read.
   */
  @Test
  public void theSupportingDatasAllowableIntervalMarkupReachesTheSeriesDose() throws Exception {
    SeriesDose loaded = new SeriesDose();
    readSeriesDose(loaded, ""
        + "<seriesDose>"
        + "<doseNumber>Dose 2</doseNumber>"
        + "<allowableInterval>"
        + "<fromPrevious>N</fromPrevious>"
        + "<fromTargetDose>1</fromTargetDose>"
        + "<absMinInt>4 weeks - 4 days</absMinInt>"
        + "<effectiveDate/>"
        + "<cessationDate/>"
        + "</allowableInterval>"
        + "</seriesDose>");

    assertEquals("the loader read the series dose", "2", loaded.getDoseNumber());
    assertEquals("the allowable interval is loaded", 1, loaded.getAllowableintervalList().size());
    AllowableInterval loadedInterval = loaded.getAllowableintervalList().get(0);
    assertEquals(YesNo.NO, loadedInterval.getFromImmediatePreviousDoseAdministered());
    assertEquals("CALCDTINT-2 needs the interval's from target dose number in series",
        "1", loadedInterval.getFromTargetDoseNumberInSeries());
    assertNotNull("CALCDTINT-3 needs the interval's absolute minimum interval",
        loadedInterval.getAbsoluteMinimumInterval());
    assertEquals(date("06/25/2015"),
        loadedInterval.getAbsoluteMinimumInterval().getDateFrom(date("06/01/2015")));
  }

  /**
   * The other half of the same check, and the reason section 6.6's 'not valid'
   * fallback is the path most series doses take: the bare
   * {@code <allowableInterval/>} that 465 of the bundled release's 484 series
   * doses carry loads as no allowable interval at all.
   */
  @Test
  public void theSupportingDatasEmptyAllowableIntervalLoadsAsNoAllowableInterval()
      throws Exception {
    SeriesDose loaded = new SeriesDose();
    readSeriesDose(loaded, ""
        + "<seriesDose>"
        + "<doseNumber>Dose 1</doseNumber>"
        + "<allowableInterval/>"
        + "</seriesDose>");

    assertEquals("the loader read the series dose", "1", loaded.getDoseNumber());
    assertTrue("an empty <allowableInterval/> defines no allowable interval attributes",
        loaded.getAllowableintervalList().isEmpty());
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
