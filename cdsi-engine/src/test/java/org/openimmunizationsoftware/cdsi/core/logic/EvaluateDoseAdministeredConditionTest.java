package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * Section 6.1 "Evaluate Dose Administered Condition" (Logic Specification for
 * ACIP Recommendations v4.6, pages 46-47, Figure 6-2, Table 6-2, Table 6-3,
 * business rule CALCDTLOTEXP-1) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/06-01-evaluate-dose-administered-condition/index.md}.
 *
 * <p>
 * 6.1 is the first evaluation step run against a vaccine dose administered. It
 * carries one attribute table (Table 6-2: Date Administered, Dose Condition
 * Flag, and a Lot Expiration Date the specification attributes to
 * CALCDTLOTEXP-1 and assumes to be 12/31/2999 when empty) and one decision
 * table:
 *
 * <pre>
 * Table 6-3 Can the Vaccine Dose Administered be Evaluated?
 *
 *   Condition                                  Rule 1   Rule 2   Rule 3
 *   Date administered &gt; lot expiration date?    Yes      No       No
 *   Is the dose condition flag 'Y'?             -        Yes      No
 *   Outcome                                    cannot   cannot   can be
 *                                              evaluate evaluate evaluated
 * </pre>
 *
 * <p>
 * Rules 1 and 2 set target dose status "not satisfied" and evaluation status
 * "sub-standard" and hand control back to 4.4; Rule 3 changes no state and
 * proceeds to 6.2.
 *
 * <h2>What replaced the previous test class</h2>
 *
 * <p>
 * This file previously held a {@code @RunWith(Parameterized.class)} class
 * extending {@code SectionTest} with three hand-built {@code DataModel}
 * scenarios, one per Table 6-3 rule. It passed 3/3 but asserted nothing: the
 * subclass declared its own {@code private List<TableInfo> tableInfo} field,
 * shadowing the {@code protected} field {@code SectionTest} actually iterates,
 * so {@code SectionTest.testEvaluateLogicTables}'s
 * {@code while (tablesIterator.hasNext() && tableInfoIterator.hasNext())} loop
 * body never executed. Two further problems were hidden behind that: the three
 * scenarios set Date Administered and Dose Condition Flag on the
 * {@code VaccineDoseAdministered}, while this step reads them from the
 * {@code AntigenAdministeredRecord} (so all three scenarios presented identical
 * inputs), and the {@code nextStep} constructor argument was stored by
 * {@code SectionTest} and never asserted. The parameterized harness also has no
 * way to express outcomes, state changes or the next step at all, which is most
 * of what Table 6-3 specifies - hence the rewrite in the plain-JUnit4 style used
 * by the other Phase 21 test classes. Every scenario the three parameterized
 * cases were written to cover is preserved here as an explicit rule test
 * ({@link #ruleOneRejectsADoseAdministeredAfterItsLotExpirationDate},
 * {@link #ruleTwoRejectsADoseWhoseConditionFlagIsY},
 * {@link #ruleThreeAcceptsADoseWithinLotExpirationWithNoConditionFlag}, plus
 * {@link #aDoseAdministeredExactlyOnItsLotExpirationDateIsNotExpired} for the
 * equal-dates case the third scenario actually built).
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads - one
 * {@code AntigenAdministeredRecord} and the current {@code TargetDose} - and
 * calls {@code process()} directly. {@code process()} ends by constructing its
 * chosen next step, so the fixture also supplies the two fields 6.2's
 * constructor dereferences (a selected-AAR list and a {@code SeriesDose} on the
 * target dose); no Supporting Data release is loaded and {@code process()} is
 * never called on the returned step. The step's condition attributes are read
 * through the public {@code getConditionAttributeList()} by attribute name, so
 * the order in which they are registered (a display concern) is not asserted.
 */
public class EvaluateDoseAdministeredConditionTest {

  private static final String DATE_ADMINISTERED = "Date Administered";
  private static final String DOSE_CONDITION_FLAG = "Dose Condition Flag";
  private static final String LOT_EXPIRATION_DATE = "Lot Expiration Date";

  private static final String CONDITION_LOT_EXPIRED = "Date administered > lot expiration date?";
  private static final String CONDITION_DOSE_CONDITION = "Is the dose condition flag 'Y'?";

  private DataModel dataModel;
  private AntigenAdministeredRecord antigenAdministeredRecord;
  private TargetDose targetDose;
  private EvaluateDoseAdministeredCondition step;

  @Before
  public void setUp() {
    dataModel = new DataModel();
    dataModel.setAssessmentDate(date("06/01/2021"));

    antigenAdministeredRecord = new AntigenAdministeredRecord();
    antigenAdministeredRecord.setDateAdministered(date("06/15/2020"));
    useRecord(antigenAdministeredRecord);

    targetDose = new TargetDose(new SeriesDose());
    dataModel.setTargetDose(targetDose);
  }

  // ---------------------------------------------------------------- fixtures

  private static Date date(String monthDayYear) {
    try {
      return new SimpleDateFormat("MM/dd/yyyy").parse(monthDayYear);
    } catch (java.text.ParseException pe) {
      throw new IllegalArgumentException(pe);
    }
  }

  /** Stands in for 4.4 having made {@code record} the current one. */
  private void useRecord(AntigenAdministeredRecord record) {
    antigenAdministeredRecord = record;
    dataModel.setAntigenAdministeredRecord(record);
    List<AntigenAdministeredRecord> selected = new ArrayList<AntigenAdministeredRecord>();
    selected.add(record);
    dataModel.setSelectedAntigenAdministeredRecordList(selected);
  }

  private void administered(String monthDayYear) {
    antigenAdministeredRecord.setDateAdministered(date(monthDayYear));
  }

  private void lotExpires(String monthDayYear) {
    antigenAdministeredRecord.setLotExpirationDate(monthDayYear == null ? null : date(monthDayYear));
  }

  private void doseConditionFlag(DoseCondition doseCondition) {
    antigenAdministeredRecord.setDoseCondition(doseCondition);
  }

  /**
   * Attaches an {@code Evaluation} to the current target dose so that Rule 1 and
   * Rule 2 can record "sub-standard" on it. In the running engine no such object
   * exists yet at 6.1 - see
   * {@link #ruleTwoSetsEvaluationStatusSubStandardOnATargetDoseThatHasNoEvaluationYet},
   * which is the test for that. Tests about anything other than that gap use this
   * so the gap does not confound them.
   */
  private Evaluation withEvaluation() {
    Evaluation evaluation = new Evaluation();
    targetDose.setEvaluation(evaluation);
    return evaluation;
  }

  private LogicStep run() throws Exception {
    step = new EvaluateDoseAdministeredCondition(dataModel);
    return step.process();
  }

  // ------------------------------------------------------ reading the step

  private LogicTable tableSixThree() {
    List<LogicTable> tables = step.getLogicTableList();
    assertEquals("6.1 builds exactly one decision table (Table 6-3)", 1, tables.size());
    return tables.get(0);
  }

  private ConditionAttribute<?> attribute(String attributeName) {
    for (ConditionAttribute<?> conditionAttribute : step.getConditionAttributeList()) {
      if (attributeName.equalsIgnoreCase(conditionAttribute.getAttributeName())) {
        return conditionAttribute;
      }
    }
    fail("Table 6-2 attribute '" + attributeName + "' is not registered by the step");
    return null;
  }

  private static LogicResult conditionResult(LogicTable logicTable, int condition) {
    return logicTable.getLogicConditions()[condition].getLogicResult();
  }

  /**
   * Table labels are compared with whitespace removed and case folded: the
   * specification writes "Table 6-3 Can the Vaccine Dose Administered be
   * Evaluated?" where the implementation writes "Table 6 - 3 Can the vaccine dose
   * administered be evaluated?". That is a transcription difference, not a
   * behavioural one.
   */
  private static String normalized(String label) {
    return label == null ? null : label.replaceAll("\\s+", "").toLowerCase();
  }

  private static void assertLabelIs(String expected, String actual) {
    assertEquals("expected label '" + expected + "' but was '" + actual + "'",
        normalized(expected), normalized(actual));
  }

  // --------------------------------------------- Table 6-2: input attributes

  /**
   * Table 6-2 "Dose Administered Condition Attributes": Date Administered and the
   * Dose Condition Flag come from the vaccine dose administered; the Lot
   * Expiration Date is a calculated date.
   */
  @Test
  public void tableSixTwoCarriesTheThreeAttributesTheSpecificationNames() throws Exception {
    administered("06/15/2020");
    lotExpires("12/31/2020");
    doseConditionFlag(DoseCondition.NO);
    withEvaluation();

    run();

    assertEquals("Table 6-2 sources Date Administered from the vaccine dose administered",
        "Vaccine dose administered", attribute(DATE_ADMINISTERED).getAttributeType());
    assertEquals(date("06/15/2020"), attribute(DATE_ADMINISTERED).getInitialValue());

    assertEquals("Table 6-2 sources the Dose Condition Flag from the vaccine dose administered",
        "Vaccine dose administered", attribute(DOSE_CONDITION_FLAG).getAttributeType());
    assertEquals(DoseCondition.NO, attribute(DOSE_CONDITION_FLAG).getInitialValue());

    assertEquals("Table 6-2 gives Lot Expiration Date as a calculated date (CALCDTLOTEXP-1)",
        "Calculated date", attribute(LOT_EXPIRATION_DATE).getAttributeType());
    assertEquals(date("12/31/2020"), attribute(LOT_EXPIRATION_DATE).getInitialValue());

    assertLabelIs("Table 6-2 Dose Administered Condition Attributes", step.getConditionTableName());
  }

  /**
   * Table 6-2 gives 12/31/2999 as the assumed Lot Expiration Date when the
   * attribute is empty; it gives no assumed value for the other two.
   */
  @Test
  public void tableSixTwoAssumesLotExpirationDateIsTheFarFutureWhenEmpty() throws Exception {
    withEvaluation();

    run();

    assertEquals("Table 6-2: an empty Lot Expiration Date is assumed to be 12/31/2999",
        date("12/31/2999"), attribute(LOT_EXPIRATION_DATE).getAssumedValue());
    assertNull("Table 6-2 gives no assumed value for Date Administered",
        attribute(DATE_ADMINISTERED).getAssumedValue());
    assertNull("Table 6-2 gives no assumed value for the Dose Condition Flag",
        attribute(DOSE_CONDITION_FLAG).getAssumedValue());
  }

  /**
   * Table 6-2 says both attributes are read "from the vaccine dose administered".
   * In the engine that arrives through
   * {@code new AntigenAdministeredRecord(vda, antigen)}, which copies the dose
   * condition off the dose and the lot expiration date off its vaccine; this pins
   * that 6.1 sees what that constructor produced rather than only what a test can
   * set by hand.
   */
  @Test
  public void theStepReadsWhatTheVaccineDoseAdministeredCarried() throws Exception {
    Vaccine vaccine = new Vaccine();
    vaccine.setVaccineType(new VaccineType());
    vaccine.setLotExpirationDate(date("12/31/2020"));

    VaccineDoseAdministered vaccineDoseAdministered = new VaccineDoseAdministered();
    vaccineDoseAdministered.setDateAdministered(date("06/15/2020"));
    vaccineDoseAdministered.setDoseCondition(DoseCondition.YES);
    vaccineDoseAdministered.setVaccine(vaccine);

    Antigen antigen = new Antigen();
    antigen.setName("HepB");
    useRecord(new AntigenAdministeredRecord(vaccineDoseAdministered, antigen));
    withEvaluation();

    run();

    assertEquals(date("06/15/2020"), attribute(DATE_ADMINISTERED).getInitialValue());
    assertEquals(DoseCondition.YES, attribute(DOSE_CONDITION_FLAG).getInitialValue());
    assertEquals(date("12/31/2020"), attribute(LOT_EXPIRATION_DATE).getInitialValue());
    assertEquals("Table 6-3 Rule 2 applies to a dose flagged 'Y'",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());
  }

  // -------------------------------------------------- Table 6-3: the encoding

  /**
   * Table 6-3's two conditions and its three-column rule grid, exactly as the
   * specification writes them.
   */
  @Test
  public void tableSixThreeIsEncodedWithTheConditionsAndRuleGridTheSpecificationWrites() throws Exception {
    withEvaluation();

    run();
    LogicTable table = tableSixThree();

    assertLabelIs("Table 6-3 Can the vaccine dose administered be evaluated?", table.getLabel());
    assertEquals("Table 6-3 has two conditions", 2, table.getLogicConditions().length);
    assertEquals("Table 6-3 has three rules", 3, table.getLogicOutcomes().length);

    assertEquals(CONDITION_LOT_EXPIRED, table.getLogicConditions()[0].getLabel());
    assertEquals(CONDITION_DOSE_CONDITION, table.getLogicConditions()[1].getLabel());

    assertArrayEquals("Table 6-3 row 1: Yes / No / No",
        new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.NO },
        table.getLogicResultTable()[0]);
    assertArrayEquals("Table 6-3 row 2: - / Yes / No",
        new LogicResult[] { LogicResult.ANY, LogicResult.YES, LogicResult.NO },
        table.getLogicResultTable()[1]);
  }

  // ----------------------------------------------------------------- Rule 1

  /**
   * Table 6-3 Rule 1: date administered &gt; lot expiration date. "No. The vaccine
   * dose administered cannot be evaluated. Target dose status is 'not satisfied.'
   * Evaluation status is 'sub-standard.'" - and control returns to 4.4.
   */
  @Test
  public void ruleOneRejectsADoseAdministeredAfterItsLotExpirationDate() throws Exception {
    administered("06/15/2020");
    lotExpires("06/14/2020");
    Evaluation evaluation = withEvaluation();

    run();

    assertEquals(LogicResult.YES, conditionResult(tableSixThree(), 0));
    assertEquals(TargetDoseStatus.NOT_SATISFIED, targetDose.getTargetDoseStatus());
    assertEquals(EvaluationStatus.SUB_STANDARD, evaluation.getEvaluationStatus());
    assertEquals(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());
  }

  /**
   * Table 6-3 Rule 1's second cell is '-': an expired lot rejects the dose
   * whatever the dose condition flag says, including 'Y' (where Rule 2 would
   * otherwise also have applied) and 'N'.
   */
  @Test
  public void ruleOneAppliesWhateverTheDoseConditionFlagSays() throws Exception {
    for (DoseCondition doseCondition : new DoseCondition[] { DoseCondition.YES, DoseCondition.NO, null }) {
      setUp();
      administered("06/15/2020");
      lotExpires("06/14/2020");
      doseConditionFlag(doseCondition);
      Evaluation evaluation = withEvaluation();

      run();

      String where = "with dose condition flag " + doseCondition;
      assertEquals(where, LogicResult.YES, conditionResult(tableSixThree(), 0));
      assertEquals(where, TargetDoseStatus.NOT_SATISFIED, targetDose.getTargetDoseStatus());
      assertEquals(where, EvaluationStatus.SUB_STANDARD, evaluation.getEvaluationStatus());
      assertEquals(where, LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES,
          step.getNextLogicStepType());
    }
  }

  // ----------------------------------------------------------------- Rule 2

  /**
   * Table 6-3 Rule 2: the lot had not expired but the dose condition flag is 'Y'
   * - the dose cannot be evaluated, target dose status "not satisfied",
   * evaluation status "sub-standard", control back to 4.4.
   */
  @Test
  public void ruleTwoRejectsADoseWhoseConditionFlagIsY() throws Exception {
    administered("06/15/2020");
    lotExpires("12/31/2020");
    doseConditionFlag(DoseCondition.YES);
    Evaluation evaluation = withEvaluation();

    run();

    assertEquals(LogicResult.NO, conditionResult(tableSixThree(), 0));
    assertEquals(LogicResult.YES, conditionResult(tableSixThree(), 1));
    assertEquals(TargetDoseStatus.NOT_SATISFIED, targetDose.getTargetDoseStatus());
    assertEquals(EvaluationStatus.SUB_STANDARD, evaluation.getEvaluationStatus());
    assertEquals(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());
  }

  // ----------------------------------------------------------------- Rule 3

  /**
   * Table 6-3 Rule 3: lot not expired, dose condition flag not 'Y' - "Yes. The
   * vaccine dose administered can be evaluated." Evaluation continues at 6.2.
   */
  @Test
  public void ruleThreeAcceptsADoseWithinLotExpirationWithNoConditionFlag() throws Exception {
    administered("06/15/2020");
    lotExpires("12/31/2020");
    doseConditionFlag(DoseCondition.NO);

    run();

    assertEquals(LogicResult.NO, conditionResult(tableSixThree(), 0));
    assertEquals(LogicResult.NO, conditionResult(tableSixThree(), 1));
    assertEquals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION, step.getNextLogicStepType());
  }

  /**
   * Rule 3 is the only outcome of Table 6-3 that records nothing: it neither
   * changes the target dose status nor writes an evaluation status. Both are
   * pre-set here to values 6.1 must leave alone.
   */
  @Test
  public void ruleThreeMakesNoStateChange() throws Exception {
    administered("06/15/2020");
    lotExpires("12/31/2020");
    doseConditionFlag(DoseCondition.NO);
    targetDose.setTargetDoseStatus(TargetDoseStatus.SATISFIED);
    Evaluation evaluation = withEvaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.VALID);

    run();

    assertEquals("Table 6-3 Rule 3 records no target dose status",
        TargetDoseStatus.SATISFIED, targetDose.getTargetDoseStatus());
    assertEquals("Table 6-3 Rule 3 records no evaluation status",
        EvaluationStatus.VALID, evaluation.getEvaluationStatus());
    assertSame("Table 6-3 Rule 3 adds no evaluation", evaluation, targetDose.getEvaluation());
    assertEquals(1, targetDose.getEvaluationList().size());
  }

  // --------------------------------------------------------------- edge cases

  /**
   * Table 6-3's first condition is a strict "&gt;": a dose given on the lot's
   * expiration date, or before it, has not been given after it. Only the day
   * after trips Rule 1.
   */
  @Test
  public void aDoseAdministeredExactlyOnItsLotExpirationDateIsNotExpired() throws Exception {
    setUp();
    administered("12/30/2020");
    lotExpires("12/31/2020");
    withEvaluation();
    run();
    assertEquals("the day before the lot expires is not after it",
        LogicResult.NO, conditionResult(tableSixThree(), 0));

    setUp();
    administered("12/31/2020");
    lotExpires("12/31/2020");
    withEvaluation();
    run();
    assertEquals("the lot expiration date itself is not after it",
        LogicResult.NO, conditionResult(tableSixThree(), 0));
    assertEquals("a dose given on its lot's expiration date can still be evaluated",
        LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION, step.getNextLogicStepType());

    setUp();
    administered("01/01/2021");
    lotExpires("12/31/2020");
    withEvaluation();
    run();
    assertEquals("the day after the lot expires is after it",
        LogicResult.YES, conditionResult(tableSixThree(), 0));
  }

  /**
   * With no lot expiration date recorded, Table 6-2's assumed 12/31/2999 applies
   * and Table 6-3's first condition can only answer "No". This is the path every
   * real forecast takes: nothing in the engine ever populates
   * {@code Vaccine.lotExpirationDate}, so the assumed value is always the one in
   * play.
   */
  @Test
  public void anEmptyLotExpirationDateFallsBackToTheAssumedFarFutureDate() throws Exception {
    administered("06/15/2020");
    lotExpires(null);
    doseConditionFlag(DoseCondition.NO);

    run();

    assertEquals(date("12/31/2999"), attribute(LOT_EXPIRATION_DATE).getFinalValue());
    assertEquals(LogicResult.NO, conditionResult(tableSixThree(), 0));
    assertEquals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION, step.getNextLogicStepType());
  }

  /**
   * Table 6-3's second condition asks whether the flag <em>is</em> 'Y'. An absent
   * flag is not 'Y', so the answer is "No" and Rule 3 applies. (Table 6-2 gives
   * the flag no assumed value, so this is the reading of the condition itself
   * rather than of an assumption.)
   */
  @Test
  public void anAbsentDoseConditionFlagIsNotY() throws Exception {
    administered("06/15/2020");
    lotExpires("12/31/2020");
    doseConditionFlag(null);

    run();

    assertEquals(LogicResult.NO, conditionResult(tableSixThree(), 1));
    assertEquals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION, step.getNextLogicStepType());
  }

  /**
   * Table 6-3's two conditions are exhaustive over Yes/No x Yes/No, so every
   * combination selects one of the three rules and the next step
   * {@code process()} assigns before evaluating the table (7.1
   * EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST) is never the one observed. This is
   * the claim {@code transitions.yaml} records as "provably dead code".
   */
  @Test
  public void everyConditionCombinationSelectsARuleAndNeverTheForecastDefault() throws Exception {
    String[][] combinations = {
        // date administered, lot expiration, dose condition, expected next step
        { "06/15/2020", "06/14/2020", "YES", "EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES" },
        { "06/15/2020", "06/14/2020", "NO", "EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES" },
        { "06/15/2020", "12/31/2020", "YES", "EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES" },
        { "06/15/2020", "12/31/2020", "NO", "EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION" },
    };

    for (String[] combination : combinations) {
      setUp();
      administered(combination[0]);
      lotExpires(combination[1]);
      doseConditionFlag(DoseCondition.valueOf(combination[2]));
      withEvaluation();

      run();

      String where = "administered " + combination[0] + ", lot expires " + combination[1]
          + ", dose condition flag " + combination[2];
      assertEquals(where, LogicStepType.valueOf(combination[3]), step.getNextLogicStepType());
      assertEquals("Table 6-3 is exhaustive, so 7.1's pre-evaluation default is never observed - " + where,
          false,
          LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST.equals(step.getNextLogicStepType()));
    }
  }

  // --------------------------------- State Changes on a freshly built target dose

  /**
   * Table 6-3 Rule 2's outcome includes "Evaluation status is 'sub-standard.'"
   * 4.4 builds each target dose as {@code new TargetDose(seriesDose)}, whose
   * evaluation list is empty, and 6.1 is the first evaluation step to run against
   * it - nothing between 4.4 and 6.1 calls
   * {@code DataModel.setEvaluationForCurrentTargetDose(...)}, and 6.4
   * {@code EvaluateAge} is the first step that does (it even asserts the
   * evaluation is non-null only <em>after</em> its own table has run). So in the
   * running engine the Evaluation this outcome writes to does not exist yet.
   */
  @Test
  public void ruleTwoSetsEvaluationStatusSubStandardOnATargetDoseThatHasNoEvaluationYet() throws Exception {
    administered("06/15/2020");
    lotExpires("12/31/2020");
    doseConditionFlag(DoseCondition.YES);
    assertNull("4.4 hands 6.1 a target dose with no evaluation recorded yet", targetDose.getEvaluation());

    try {
      run();
    } catch (NullPointerException npe) {
      fail("Table 6-3 Rule 2 requires evaluation status 'sub-standard', but the step threw "
          + "NullPointerException - dataModel.getTargetDose().getEvaluation() is null on the target dose "
          + "4.4 has just built, and the outcome writes to it without creating one.");
    }

    assertNotNull("Table 6-3 Rule 2 records an evaluation status, so an evaluation must exist",
        targetDose.getEvaluation());
    assertEquals(EvaluationStatus.SUB_STANDARD, targetDose.getEvaluation().getEvaluationStatus());
    assertEquals(TargetDoseStatus.NOT_SATISFIED, targetDose.getTargetDoseStatus());
  }

  /**
   * Table 6-3 Rule 1's outcome, on the same freshly built target dose - see
   * {@link #ruleTwoSetsEvaluationStatusSubStandardOnATargetDoseThatHasNoEvaluationYet}.
   */
  @Test
  public void ruleOneSetsEvaluationStatusSubStandardOnATargetDoseThatHasNoEvaluationYet() throws Exception {
    administered("06/15/2020");
    lotExpires("06/14/2020");
    assertNull("4.4 hands 6.1 a target dose with no evaluation recorded yet", targetDose.getEvaluation());

    try {
      run();
    } catch (NullPointerException npe) {
      fail("Table 6-3 Rule 1 requires evaluation status 'sub-standard', but the step threw "
          + "NullPointerException - dataModel.getTargetDose().getEvaluation() is null on the target dose "
          + "4.4 has just built, and the outcome writes to it without creating one.");
    }

    assertNotNull("Table 6-3 Rule 1 records an evaluation status, so an evaluation must exist",
        targetDose.getEvaluation());
    assertEquals(EvaluationStatus.SUB_STANDARD, targetDose.getEvaluation().getEvaluationStatus());
    assertEquals(TargetDoseStatus.NOT_SATISFIED, targetDose.getTargetDoseStatus());
  }
}
