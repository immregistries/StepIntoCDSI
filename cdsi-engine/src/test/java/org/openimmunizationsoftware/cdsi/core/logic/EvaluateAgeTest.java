package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Section 6.4 "Evaluate Age" (Logic Specification for ACIP Recommendations
 * v4.6, pages 52-53, Figure 6-5, Figure 6-6, Table 6-14, Table 6-15, Table
 * 6-16) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/06-04-evaluate-age/index.md}.
 *
 * <p>
 * 6.4 places the date the vaccine dose was administered on the timeline three
 * calculated dates divide into four zones, and records the resulting evaluation:
 *
 * <pre>
 * Table 6-15 Was the Vaccine Dose Administered at a Valid Age?
 *
 *   Condition                                                Rule 1  Rule 2  Rule 3  Rule 4
 *   Is the date administered &lt; absolute minimum age date?       Yes     No      No      No
 *   Is the absolute minimum age date &le; date administered
 *      &lt; minimum age date?                                      No     Yes      No      No
 *   Is the minimum age date &le; date administered
 *      &lt; maximum age date?                                      No      No     Yes      No
 *   Is the date administered &ge; maximum age date?               No      No      No     Yes
 *   Outcome                                                  Not     Valid   Valid   Extra-
 *                                                            valid   (grace)         neous
 *                                                            (too                    (too
 *                                                            young)                  old)
 * </pre>
 *
 * <p>
 * The three calculated dates come from Table 6-16's business rules - maximum age
 * date is date of birth plus maximum age (CALCDTAGE-1), minimum age date is date
 * of birth plus minimum age (CALCDTAGE-4), absolute minimum age date is date of
 * birth plus absolute minimum age (CALCDTAGE-5) - and Table 6-14 gives each an
 * assumed value for when the target dose does not define it: 12/31/2999 for the
 * maximum age date, 01/01/1900 for both minimum dates. Those assumed values are
 * what makes the Purpose's fallback work: "In cases where a target dose does not
 * specify age attributes, the age at administration is considered 'valid.'"
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads: the
 * patient's date of birth, the {@code AntigenAdministeredRecord} 4.4 has made
 * current (carrying the date administered), and the current {@code TargetDose}
 * whose {@code SeriesDose} holds the Supporting Data {@code Age} record. Ages
 * are built the way {@code DataModelLoader.readSeriesDose} builds them - all
 * three {@link TimePeriod}s always assigned, an absent Supporting Data value
 * becoming an unvalued {@code TimePeriod} rather than a null one, exactly as the
 * bundled release's markup produces (every one of its 502 {@code <age>} elements
 * carries all of {@code absMinAge}, {@code minAge} and {@code maxAge}, valued or
 * self-closed). {@code process()} is called directly; it ends by constructing
 * 6.5, whose constructor loops over the series dose's interval list, so leaving
 * that list empty is enough. No Supporting Data release is loaded and
 * {@code process()} is never called on the returned step.
 *
 * <p>
 * The step's decision table is a {@code private} inner class, so it is read here
 * through the public {@code getLogicTableList()} as a plain {@link LogicTable},
 * and its five condition attributes through {@code getConditionAttributeList()}.
 *
 * <h2>What is deliberately not asserted</h2>
 *
 * <p>
 * The wording of Table 6-15's four condition rows and of Table 6-14's attribute
 * <em>types</em>. The specification writes the attribute type of the three
 * calculated dates as "Calculated date (CALCDTAGE-1)" and so on, naming the
 * business rule in the cell; the step writes "Calculated Date" for all three.
 * Pinning either would assert a transcription, so the attribute <em>names</em>,
 * which both documents agree on, are pinned instead.
 *
 * <p>
 * Figure 6-5's "Decision Table 6-15 Columns 1 &amp; 2 / 3 &amp; 4 / Column 5 /
 * Column 6" labels. The step package records these as a
 * specification-internal inconsistency (Table 6-15 has four rule columns, not
 * six) and explicitly declines to resolve it; no test here takes a position on
 * which of the two the specification meant.
 *
 * <p>
 * Selection among multiple {@code <age>} records for one series dose. The step
 * reads {@code getAgeList().get(0)} and the specification section says nothing
 * about choosing between several, nor about the {@code effectiveDate} /
 * {@code cessationDate} that would be the natural basis for choosing - so there
 * is no documented behaviour to assert. (The bundled release has 502
 * {@code <age>} elements across 484 series doses, so the case is real but
 * undocumented here.)
 */
public class EvaluateAgeTest {

  /** Table 6-14 row 1. */
  private static final String DATE_OF_BIRTH = "Date of birth";
  /** Table 6-14 row 2. */
  private static final String DATE_ADMINISTERED = "Date Administered";
  /** Table 6-14 row 3, CALCDTAGE-1. */
  private static final String MAXIMUM_AGE_DATE = "Maximum Age Date";
  /** Table 6-14 row 4, CALCDTAGE-4. */
  private static final String MINIMUM_AGE_DATE = "Minimum Age Date";
  /** Table 6-14 row 5, CALCDTAGE-5. */
  private static final String ABSOLUTE_MINIMUM_AGE_DATE = "Absolute Minimum Age Date";

  /** Table 6-14's assumed value for the maximum age date. */
  private static final String ASSUMED_MAXIMUM_AGE_DATE = "12/31/2999";
  /** Table 6-14's assumed value for both minimum age dates. */
  private static final String ASSUMED_MINIMUM_AGE_DATE = "01/01/1900";

  private static final String DOB = "01/01/2015";

  private DataModel dataModel;
  private Patient patient;
  private AntigenAdministeredRecord antigenAdministeredRecord;
  private VaccineDoseAdministered vaccineDoseAdministered;
  private SeriesDose seriesDose;
  private TargetDose targetDose;
  private EvaluateAge step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    patient = new Patient();
    patient.setDateOfBirth(date(DOB));
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date("06/01/2021"));

    seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("1");
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    administeredOn("06/01/2016");

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

  /** Stands in for 4.4 having made this vaccine dose administered the current one. */
  private void administeredOn(String monthDayYear) {
    VaccineType vaccineType = new VaccineType();
    vaccineType.setCvxCode("10"); // IPV
    vaccineType.setShortDescription("CVX 10");

    Vaccine vaccine = new Vaccine();
    vaccine.setVaccineType(vaccineType);

    vaccineDoseAdministered = new VaccineDoseAdministered();
    vaccineDoseAdministered.setVaccine(vaccine);
    vaccineDoseAdministered.setDateAdministered(date(monthDayYear));

    antigenAdministeredRecord = new AntigenAdministeredRecord();
    antigenAdministeredRecord.setDateAdministered(date(monthDayYear));
    antigenAdministeredRecord.setVaccineType(vaccineType);
    antigenAdministeredRecord.setVaccineDoseAdministered(vaccineDoseAdministered);
    dataModel.setAntigenAdministeredRecord(antigenAdministeredRecord);
  }

  /**
   * The target dose's Supporting Data age attributes, written the way
   * {@code DataModelLoader.readSeriesDose} writes them: every one of the three
   * {@link TimePeriod}s assigned, an empty or absent Supporting Data value
   * becoming an unvalued {@code TimePeriod}.
   */
  private void ageAttributes(String absoluteMinimumAge, String minimumAge, String maximumAge) {
    Age age = new Age();
    age.setAbsoluteMinimumAge(new TimePeriod(absoluteMinimumAge));
    age.setMinimugeAge(new TimePeriod(minimumAge));
    age.setMaximumAge(new TimePeriod(maximumAge));
    seriesDose.getAgeList().add(age);
  }

  /** The age window every rule-by-rule test below places a date administered in. */
  private void theStandardAgeWindow() {
    // date of birth 01/01/2015 -> absolute minimum 01/29/2015,
    // minimum 02/12/2015, maximum 01/01/2020
    ageAttributes("4 weeks", "6 weeks", "5 years");
  }

  private LogicStep run() throws Exception {
    step = new EvaluateAge(dataModel);
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  private ConditionAttribute<?> attributeNamed(String attributeName) {
    for (ConditionAttribute<?> conditionAttribute : step.getConditionAttributeList()) {
      if (conditionAttribute != null && attributeName.equals(conditionAttribute.getAttributeName())) {
        return conditionAttribute;
      }
    }
    fail("Table 6-14 attribute '" + attributeName + "' is not registered by the step");
    return null;
  }

  private void assertAttributeIs(String monthDayYear, String attributeName) {
    assertEquals("Table 6-14 '" + attributeName + "'",
        date(monthDayYear), attributeNamed(attributeName).getFinalValue());
  }

  private LogicTable tableSixFifteen() {
    assertEquals("6.4 builds exactly one decision table", 1, step.getLogicTableList().size());
    return step.getLogicTableList().get(0);
  }

  private LogicResult conditionResult(int row) {
    return tableSixFifteen().getLogicConditions()[row].getLogicResult();
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

  // ==================================================== Entry: what 6.4's class is

  /**
   * 6.4 identifies itself as {@code EVALUATE_AGE} and names Table 6-14 as its
   * attribute table.
   */
  @Test
  public void theStepIsSixFourAndNamesTableSixFourteenAsItsAttributeTable() throws Exception {
    theStandardAgeWindow();

    run();

    assertEquals(LogicStepType.EVALUATE_AGE, step.getLogicStepType());
    assertEquals("6.4", LogicStepType.EVALUATE_AGE.getChapter());
    assertLabelIs("Table 6-14 Age Attributes", step.getConditionTableName());
  }

  // ============================================ Table 6-14: the five attributes

  /**
   * Table 6-14 lists five attributes: the patient's date of birth, the vaccine
   * dose administered's date administered, and the three calculated dates. All
   * five are registered, and the two input attributes carry the patient and the
   * vaccine dose administered 4.4 has made current.
   */
  @Test
  public void tableSixFourteenRegistersItsFivePatientAndDoseAttributes() throws Exception {
    theStandardAgeWindow();
    administeredOn("06/01/2016");

    run();

    assertEquals("Table 6-14 lists exactly five attributes", 5, step.getConditionAttributeList().size());
    assertEquals("Patient", attributeNamed(DATE_OF_BIRTH).getAttributeType());
    assertEquals("Vaccine dose administered", attributeNamed(DATE_ADMINISTERED).getAttributeType());
    assertAttributeIs(DOB, DATE_OF_BIRTH);
    assertAttributeIs("06/01/2016", DATE_ADMINISTERED);
    assertNotNull(attributeNamed(MINIMUM_AGE_DATE));
    assertNotNull(attributeNamed(MAXIMUM_AGE_DATE));
    assertNotNull(attributeNamed(ABSOLUTE_MINIMUM_AGE_DATE));
  }

  /**
   * Table 6-14's "Assumed Value if Empty" column: a target dose that defines no
   * age attributes at all leaves the maximum age date at 12/31/2999 and both
   * minimum age dates at 01/01/1900.
   */
  @Test
  public void tableSixFourteenAssumedValuesApplyWhenTheTargetDoseHasNoAgeAttributes() throws Exception {
    assertTrue("the fixture's series dose defines no age attributes", seriesDose.getAgeList().isEmpty());

    run();

    assertAttributeIs(ASSUMED_MAXIMUM_AGE_DATE, MAXIMUM_AGE_DATE);
    assertAttributeIs(ASSUMED_MINIMUM_AGE_DATE, MINIMUM_AGE_DATE);
    assertAttributeIs(ASSUMED_MINIMUM_AGE_DATE, ABSOLUTE_MINIMUM_AGE_DATE);
  }

  // ================================= Table 6-16: the three calculated-date rules

  /**
   * Table 6-16 CALCDTAGE-5: "A patient's absolute minimum age date must be
   * calculated as the patient's date of birth plus the absolute minimum age."
   * 01/01/2015 + 4 weeks = 01/29/2015.
   */
  @Test
  public void calcdtageFiveCalculatesTheAbsoluteMinimumAgeDateFromTheDateOfBirth() throws Exception {
    ageAttributes("4 weeks", "", "");

    run();

    assertAttributeIs("01/29/2015", ABSOLUTE_MINIMUM_AGE_DATE);
  }

  /**
   * Table 6-16 CALCDTAGE-4: "A patient's minimum age date must be calculated as
   * the patient's date of birth plus the minimum age." 01/01/2015 + 6 weeks =
   * 02/12/2015.
   */
  @Test
  public void calcdtageFourCalculatesTheMinimumAgeDateFromTheDateOfBirth() throws Exception {
    ageAttributes("", "6 weeks", "");

    run();

    assertAttributeIs("02/12/2015", MINIMUM_AGE_DATE);
  }

  /**
   * Table 6-16 CALCDTAGE-1: "A patient's maximum age date must be calculated as
   * the patient's date of birth plus the maximum age." 01/01/2015 + 5 years =
   * 01/01/2020.
   */
  @Test
  public void calcdtageOneCalculatesTheMaximumAgeDateFromTheDateOfBirth() throws Exception {
    ageAttributes("", "", "5 years");

    run();

    assertAttributeIs("01/01/2020", MAXIMUM_AGE_DATE);
  }

  // ============================================== Table 6-15: shape of the table

  /**
   * Table 6-15's four conditions and its four-rule grid, as the specification
   * writes them.
   */
  @Test
  public void tableSixFifteenIsEncodedWithFourConditionsAndFourRules() throws Exception {
    theStandardAgeWindow();

    run();
    LogicTable table = tableSixFifteen();

    assertLabelIs("Table 6-15 Was the Vaccine Dose Administered at a Valid Age?", table.getLabel());
    assertEquals("Table 6-15 has four conditions", 4, table.getLogicConditions().length);
    assertEquals("Table 6-15 has four rules", 4, table.getLogicOutcomes().length);
    LogicResult[][] grid = table.getLogicResultTable();
    assertArrayEquals("Table 6-15 row 1: Yes / No / No / No",
        new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.NO }, grid[0]);
    assertArrayEquals("Table 6-15 row 2: No / Yes / No / No",
        new LogicResult[] { LogicResult.NO, LogicResult.YES, LogicResult.NO, LogicResult.NO }, grid[1]);
    assertArrayEquals("Table 6-15 row 3: No / No / Yes / No",
        new LogicResult[] { LogicResult.NO, LogicResult.NO, LogicResult.YES, LogicResult.NO }, grid[2]);
    assertArrayEquals("Table 6-15 row 4: No / No / No / Yes",
        new LogicResult[] { LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.YES }, grid[3]);
  }

  // ========================================================= Table 6-15 Rule 1

  /**
   * Table 6-15 Rule 1: the date administered is before the absolute minimum age
   * date. "No. The vaccine dose administered was not administered at a valid age
   * for the target dose. Evaluation reason is 'Too young'."
   */
  @Test
  public void ruleOneRejectsADoseAdministeredBeforeTheAbsoluteMinimumAgeAsTooYoung() throws Exception {
    theStandardAgeWindow();
    administeredOn("01/15/2015"); // absolute minimum age date is 01/29/2015

    run();

    assertEquals(LogicResult.YES, conditionResult(0));
    assertEquals("Rule 1: evaluation status 'Not Valid'",
        EvaluationStatus.NOT_VALID, targetDose.getEvaluation().getEvaluationStatus());
    assertEquals("Rule 1: evaluation reason 'Too young'",
        EvaluationReason.TOO_YOUNG, targetDose.getEvaluation().getEvaluationReason());
  }

  // ========================================================= Table 6-15 Rule 2

  /**
   * Table 6-15 Rule 2: the date administered is on or after the absolute minimum
   * age date but before the minimum age date. "Yes. The vaccine dose
   * administered was administered at a valid age for the target dose. Evaluation
   * reason is 'Grace period'."
   */
  @Test
  public void ruleTwoAcceptsADoseInTheGracePeriodAsValid() throws Exception {
    theStandardAgeWindow();
    administeredOn("02/01/2015"); // between 01/29/2015 and 02/12/2015

    run();

    assertEquals(LogicResult.YES, conditionResult(1));
    assertEquals("Rule 2: evaluation status 'Valid'",
        EvaluationStatus.VALID, targetDose.getEvaluation().getEvaluationStatus());
    assertEquals("Rule 2: evaluation reason 'Grace period'",
        EvaluationReason.GRACE_PERIOD, targetDose.getEvaluation().getEvaluationReason());
  }

  // ========================================================= Table 6-15 Rule 3

  /**
   * Table 6-15 Rule 3: the date administered is on or after the minimum age date
   * and before the maximum age date. "Yes. The vaccine dose administered was
   * administered at a valid age for the target dose." The specification gives no
   * evaluation reason for the plain-valid case - a valid dose needs no
   * explanation.
   */
  @Test
  public void ruleThreeAcceptsADoseAtAValidAgeWithNoEvaluationReason() throws Exception {
    theStandardAgeWindow();
    administeredOn("06/01/2016"); // between 02/12/2015 and 01/01/2020

    run();

    assertEquals(LogicResult.YES, conditionResult(2));
    assertEquals("Rule 3: evaluation status 'Valid'",
        EvaluationStatus.VALID, targetDose.getEvaluation().getEvaluationStatus());
    assertNull("Rule 3 states no evaluation reason",
        targetDose.getEvaluation().getEvaluationReason());
  }

  // ========================================================= Table 6-15 Rule 4

  /**
   * Table 6-15 Rule 4: the date administered is on or after the maximum age
   * date. "No. The vaccine dose administered was not administered at a valid age
   * for the target dose. It is extraneous. Evaluation reason is 'Too old'."
   */
  @Test
  public void ruleFourMarksADoseAdministeredAtOrAfterTheMaximumAgeAsExtraneous() throws Exception {
    theStandardAgeWindow();
    administeredOn("06/01/2021"); // maximum age date is 01/01/2020

    run();

    assertEquals(LogicResult.YES, conditionResult(3));
    assertEquals("Rule 4: evaluation status 'Extraneous'",
        EvaluationStatus.EXTRANEOUS, targetDose.getEvaluation().getEvaluationStatus());
    assertEquals("Rule 4: evaluation reason 'Too old'",
        EvaluationReason.TOO_OLD, targetDose.getEvaluation().getEvaluationReason());
  }

  // ===================================== Table 6-15's boundaries between the zones

  /**
   * Table 6-15's second condition is inclusive at its lower end ("Is the
   * absolute minimum age date &le; date administered ...?"), so the absolute
   * minimum age date itself is the first day of the grace period, not the last
   * too-young day.
   */
  @Test
  public void theAbsoluteMinimumAgeDateItselfBeginsTheGracePeriod() throws Exception {
    theStandardAgeWindow();
    administeredOn("01/29/2015"); // exactly the absolute minimum age date

    run();

    assertEquals(LogicResult.NO, conditionResult(0));
    assertEquals(LogicResult.YES, conditionResult(1));
    assertEquals(EvaluationStatus.VALID, targetDose.getEvaluation().getEvaluationStatus());
    assertEquals(EvaluationReason.GRACE_PERIOD, targetDose.getEvaluation().getEvaluationReason());
  }

  /**
   * Table 6-15's third condition is inclusive at its lower end ("Is the minimum
   * age date &le; date administered ...?") and its second exclusive at its upper
   * end, so the minimum age date itself is the first plainly valid day rather
   * than the last grace-period day.
   */
  @Test
  public void theMinimumAgeDateItselfIsTheFirstPlainlyValidDay() throws Exception {
    theStandardAgeWindow();
    administeredOn("02/12/2015"); // exactly the minimum age date

    run();

    assertEquals(LogicResult.NO, conditionResult(1));
    assertEquals(LogicResult.YES, conditionResult(2));
    assertEquals(EvaluationStatus.VALID, targetDose.getEvaluation().getEvaluationStatus());
    assertNull("the first plainly valid day needs no evaluation reason",
        targetDose.getEvaluation().getEvaluationReason());
  }

  /**
   * Table 6-15's fourth condition is inclusive ("Is the date administered &ge;
   * maximum age date?") and its third exclusive at the same boundary, so the
   * maximum age date itself is already too old rather than the last valid day.
   */
  @Test
  public void theMaximumAgeDateItselfIsAlreadyExtraneous() throws Exception {
    theStandardAgeWindow();
    administeredOn("01/01/2020"); // exactly the maximum age date

    run();

    assertEquals(LogicResult.NO, conditionResult(2));
    assertEquals(LogicResult.YES, conditionResult(3));
    assertEquals(EvaluationStatus.EXTRANEOUS, targetDose.getEvaluation().getEvaluationStatus());
    assertEquals(EvaluationReason.TOO_OLD, targetDose.getEvaluation().getEvaluationReason());
  }

  // ============================================================ Purpose fallback

  /**
   * The Purpose: "In cases where a target dose does not specify age attributes,
   * the age at administration is considered 'valid.'" Table 6-14's assumed
   * values do the work - every real-world date administered falls between
   * 01/01/1900 and 12/31/2999 - and the outcome is plain Rule 3 valid, with no
   * evaluation reason.
   */
  @Test
  public void aTargetDoseThatSpecifiesNoAgeAttributesMakesTheAgeAtAdministrationValid() throws Exception {
    assertTrue("the fixture's series dose defines no age attributes", seriesDose.getAgeList().isEmpty());
    administeredOn("06/01/2016");

    run();

    assertEquals(LogicResult.YES, conditionResult(2));
    assertEquals(EvaluationStatus.VALID, targetDose.getEvaluation().getEvaluationStatus());
    assertNull(targetDose.getEvaluation().getEvaluationReason());
  }

  /**
   * The same fallback applied to one attribute rather than all of them: a target
   * dose that defines a minimum age but no absolute minimum age gets Table
   * 6-14's assumed 01/01/1900 absolute minimum age date, which puts every early
   * dose inside Table 6-15 Rule 2's grace period rather than Rule 1's too-young
   * zone. (The bundled Supporting Data release has 2 such {@code <age>} rows.)
   */
  @Test
  public void anUnspecifiedAbsoluteMinimumAgeMakesAnEarlyDoseAGracePeriodDose() throws Exception {
    ageAttributes("", "6 weeks", "");
    administeredOn("01/05/2015"); // long before the 02/12/2015 minimum age date

    run();

    assertAttributeIs(ASSUMED_MINIMUM_AGE_DATE, ABSOLUTE_MINIMUM_AGE_DATE);
    assertEquals(LogicResult.NO, conditionResult(0));
    assertEquals(LogicResult.YES, conditionResult(1));
    assertEquals(EvaluationStatus.VALID, targetDose.getEvaluation().getEvaluationStatus());
    assertEquals(EvaluationReason.GRACE_PERIOD, targetDose.getEvaluation().getEvaluationReason());
  }

  // ================================================================ Next Steps

  /**
   * "Age validity is a piece of evaluation state carried forward, not a fork in
   * processing": all four of Table 6-15's outcomes continue to 6.5 Evaluate
   * Preferable Interval.
   */
  @Test
  public void everyRuleContinuesToSixFiveEvaluatePreferableInterval() throws Exception {
    String[][] rules = {
        { "01/15/2015", "Rule 1 (too young)" },
        { "02/01/2015", "Rule 2 (grace period)" },
        { "06/01/2016", "Rule 3 (valid)" },
        { "06/01/2021", "Rule 4 (too old)" },
    };
    for (String[] rule : rules) {
      setUp();
      theStandardAgeWindow();
      administeredOn(rule[0]);

      run();

      assertEquals(rule[1] + " continues to 6.5 Evaluate Preferable Interval",
          LogicStepType.EVALUATE_PREFERABLE_INTERVAL, step.getNextLogicStepType());
    }
  }

  // ====================================== The evaluation 6.4's outcomes record

  /**
   * Every one of Table 6-15's four outcomes records an evaluation status (and,
   * for three of them, a reason) on the current target dose. In the running
   * engine no evaluation exists yet at that point: {@code TargetDose}'s
   * evaluation list starts empty, and neither 4.4's dispatch into 6.1, nor 6.1's
   * "can be evaluated" outcome, nor 6.2's "cannot be skipped" outcome, nor 6.3's
   * "not inadvertent" outcome creates one. 6.4 is the first step on that path to
   * call {@code DataModel.setEvaluationForCurrentTargetDose(...)}, which
   * constructs an {@code Evaluation} rather than writing into an existing one -
   * so recording 6.4's own outcome is also what brings the target dose's
   * evaluation into existence.
   *
   * <p>
   * This test pins that as 6.4's observable behaviour: given a target dose with
   * an empty evaluation list, 6.4 leaves exactly one evaluation on it, carrying
   * its outcome and the vaccine dose administered it evaluated.
   */
  @Test
  public void sixFourCreatesTheFirstEvaluationOnATargetDoseThatHasNoneYet() throws Exception {
    theStandardAgeWindow();
    administeredOn("06/01/2016");

    assertEquals("4.4/6.1/6.2/6.3 hand 6.4 a target dose with no evaluation recorded yet",
        0, targetDose.getEvaluationList().size());

    run();

    assertEquals("6.4 records exactly one evaluation", 1, targetDose.getEvaluationList().size());
    assertNotNull(targetDose.getEvaluation());
    assertEquals(EvaluationStatus.VALID, targetDose.getEvaluation().getEvaluationStatus());
    assertSame("the evaluation records the vaccine dose administered it evaluated",
        vaccineDoseAdministered, targetDose.getEvaluation().getVaccineDoseAdministered());
  }

  // ============================ Table 6-15's four rules against real Supporting Data

  /**
   * Table 6-15 Rule 1 for a target dose whose Supporting Data gives an absolute
   * minimum age but no minimum age. The specification's first condition asks
   * only "Is the date administered &lt; absolute minimum age date?", so a dose
   * given before that date is Rule 1 - not valid, "Too young" - whatever the
   * minimum age date is.
   *
   * <p>
   * With no minimum age defined, Table 6-14's assumed minimum age date is
   * 01/01/1900, which is <em>before</em> the absolute minimum age date. Table
   * 6-15's third condition ("Is the minimum age date &le; date administered
   * &lt; maximum age date?") is then also true for the same dose, so as encoded
   * two of the four condition rows answer Yes at once and no rule column
   * matches: no outcome runs, no evaluation is recorded, and
   * {@code process()}'s own guard throws
   * {@code NullPointerException("Evaluation should not be null at this point")}.
   *
   * <p>
   * This is not a constructed corner. The Supporting Data release bundled with
   * {@code cdsi-engine} has 5 series doses shaped exactly like this fixture -
   * {@code <absMinAge>19 years</absMinAge>} with {@code <minAge/>} and
   * {@code <maxAge/>} empty, and a single {@code <age>} row so it is the one the
   * step reads - all in Pneumococcal: Dose 3 of "Pneumococcal risk 19+ years CSF
   * Leaks or Cochlear Implants PCV-PPSV series" and of the matching PPSV-PCV
   * series, and Dose 4 of the three "Pneumococcal risk 19+ years
   * immunocompromised" series (PCV-PPSV-PPSV, PPSV-PCV-PPSV, PPSV-PPSV-PCV).
   * Every dose administered before the patient's 19th birthday against one of
   * those target doses reaches this path. (Across all 502 {@code <age>} elements
   * in that release, 298 give both an absolute minimum and a minimum age, 197
   * give neither, 2 give a minimum age only, and these 5 give an absolute
   * minimum age only.)
   */
  @Test
  public void aDoseBeforeTheAbsoluteMinimumAgeIsTooYoungEvenWithNoMinimumAgeDefined() throws Exception {
    ageAttributes("19 years", "", ""); // Pneumococcal risk 19+ years, Dose 3 / Dose 4
    administeredOn("06/01/2016"); // age 1, far short of the 01/01/2034 absolute minimum age date

    try {
      run();
    } catch (NullPointerException npe) {
      fail("Table 6-15 Rule 1 requires evaluation status 'Not Valid' and evaluation reason "
          + "'Too young' for a dose administered before the absolute minimum age date, but the "
          + "step threw NullPointerException: conditions 1 and 3 both answered Yes (the assumed "
          + "01/01/1900 minimum age date precedes the absolute minimum age date), so no rule "
          + "column matched and no outcome recorded an evaluation");
    }

    assertEquals(LogicResult.YES, conditionResult(0));
    assertNotNull("Rule 1 records an evaluation status and reason, so an evaluation must exist",
        targetDose.getEvaluation());
    assertEquals("Rule 1: evaluation status 'Not Valid'",
        EvaluationStatus.NOT_VALID, targetDose.getEvaluation().getEvaluationStatus());
    assertEquals("Rule 1: evaluation reason 'Too young'",
        EvaluationReason.TOO_YOUNG, targetDose.getEvaluation().getEvaluationReason());
  }

  /**
   * Confirms the fixture the test above uses is the Supporting Data's own shape
   * rather than a hypothetical: the markup of Pneumococcal Dose 3, read through
   * {@code DataModelLoader.readSeriesDose}, produces an {@code Age} whose
   * absolute minimum age is valued and whose minimum and maximum ages are not -
   * which is what makes Table 6-14's assumed 01/01/1900 minimum age date apply.
   */
  @Test
  public void theSupportingDatasAbsoluteMinimumOnlyAgeShapeLoadsWithAnUnvaluedMinimumAge()
      throws Exception {
    SeriesDose loaded = new SeriesDose();
    readSeriesDose(loaded, ""
        + "<seriesDose>"
        + "<doseNumber>Dose 3</doseNumber>"
        + "<age>"
        + "<absMinAge>19 years</absMinAge>"
        + "<minAge/>"
        + "<earliestRecAge/>"
        + "<latestRecAge/>"
        + "<maxAge/>"
        + "<effectiveDate/>"
        + "<cessationDate/>"
        + "</age>"
        + "</seriesDose>");

    assertEquals("the loader read the series dose", "3", loaded.getDoseNumber());
    assertEquals("one age row, so it is the one 6.4 reads", 1, loaded.getAgeList().size());
    Age age = loaded.getAgeList().get(0);
    assertTrue("absMinAge '19 years' is a valued time period",
        age.getAbsoluteMinimumAge().isValued());
    assertFalse("an empty <minAge/> is an unvalued time period, so Table 6-14's assumed "
        + "01/01/1900 minimum age date applies", age.getMinimumAge().isValued());
    assertFalse("an empty <maxAge/> is an unvalued time period, so Table 6-14's assumed "
        + "12/31/2999 maximum age date applies", age.getMaximumAge().isValued());
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
