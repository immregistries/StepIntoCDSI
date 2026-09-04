package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkip;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipCondition;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipConditionType;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipSet;
import org.openimmunizationsoftware.cdsi.core.domain.DoseType;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * Section 6.2 "Evaluate Conditional Skip" (Logic Specification for ACIP
 * Recommendations v4.6, pages 47-51, Figure 6-3, Tables 6-4 to 6-11, business
 * rules CALCDTSKIP-3/4/5, CONDSKIP-1, CONDSKIP-2) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/06-02-evaluate-conditional-skip/index.md}.
 *
 * <h2>What this class is testing, and what it deliberately is not</h2>
 *
 * <p>
 * {@code spec-to-code.yaml} maps 6.2 onto two classes:
 * {@link EvaluateConditionalSkipForEvaluation}, and the shared
 * {@link EvaluateConditionalSkip} it extends.
 * {@code EvaluateConditionalSkipForEvaluation} is a near-empty subclass: its
 * only content is a constructor that passes
 * {@code ConditionalSkipType.EVALUATE}, the step type
 * {@code EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION} and the two destinations
 * (6.3 on "no skip", 4.4 on "skip") up to the base class. Everything the
 * specification actually describes - the attribute table, the five business
 * rules and the six decision tables - lives in the base class, which builds all
 * of it in its constructor and evaluates it in {@code process()}. So this class
 * drives the shared logic through the evaluation subclass, which is exactly how
 * the running engine reaches it.
 *
 * <p>
 * The base class is shared with 7.1 ({@code EvaluateConditionalSkipForForecast})
 * and with {@code ValidateRecommendation}. It branches on the context in exactly
 * one place: CONDSKIP-2's reference date, a {@code switch} in the constructor
 * that takes the date administered when evaluating, the assessment date when
 * forecasting, and 01/01/1900 when validating. Only the EVALUATE arm is
 * exercised here; the FORECAST arm, and the two forecast-side destinations, are
 * 7.1's own unit and are intentionally left alone. Everything else covered below
 * - Tables 6-6 through 6-11, CALCDTSKIP-3/4/5, CONDSKIP-1, the per-condition
 * attributes - is context-independent shared logic and does not need
 * re-litigating under 7.1.
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads: a patient
 * with a date of birth (CALCDTSKIP-3/4), an immunization history (CONDSKIP-1),
 * the {@code AntigenAdministeredRecord} 4.4 has made current, a selected-record
 * list, and the current {@code TargetDose} with the {@code SeriesDose} whose
 * Conditional Skip definition is under test. {@code process()} is called
 * directly; it ends by constructing its chosen next step, which is inert (no
 * Supporting Data release is loaded and {@code process()} is never called on the
 * returned step). The step's inner decision tables are {@code protected} inner
 * classes, so this test - in the same package - reads their condition
 * attributes, their {@code isMet()} flags and their encoded result grids
 * directly rather than reflectively.
 */
public class EvaluateConditionalSkipForEvaluationTest {

  private static final String DATE_ADMINISTERED = "Date Administered";
  private static final String ADMINISTERED_DOSE_COUNT = "Administered Dose Count";
  private static final String ASSESSMENT_DATE = "Assessment Date";
  private static final String EARLIEST_DATE = "Earliest Date";

  private static final String SUPPORTING_DATA = "Supporting Data (Conditional Skip)";

  private DataModel dataModel;
  private Patient patient;
  private ImmunizationHistory immunizationHistory;
  private AntigenAdministeredRecord antigenAdministeredRecord;
  private SeriesDose seriesDose;
  private TargetDose targetDose;
  private ConditionalSkip conditionalSkip;
  private EvaluateConditionalSkipForEvaluation step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    patient = new Patient();
    patient.setDateOfBirth(date("01/01/2015"));
    dataModel.setPatient(patient);

    immunizationHistory = new ImmunizationHistory();
    dataModel.setImmunizationHistory(immunizationHistory);

    dataModel.setAssessmentDate(date("06/01/2021"));

    antigenAdministeredRecord = new AntigenAdministeredRecord();
    antigenAdministeredRecord.setDateAdministered(date("06/01/2016"));
    dataModel.setAntigenAdministeredRecord(antigenAdministeredRecord);
    List<AntigenAdministeredRecord> selected = new ArrayList<AntigenAdministeredRecord>();
    selected.add(antigenAdministeredRecord);
    dataModel.setSelectedAntigenAdministeredRecordList(selected);

    seriesDose = new SeriesDose();
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    conditionalSkip = null;
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

  /** Stands in for 6.1 having accepted this dose for evaluation. */
  private void administered(String monthDayYear) {
    antigenAdministeredRecord.setDateAdministered(date(monthDayYear));
  }

  private void bornOn(String monthDayYear) {
    patient.setDateOfBirth(date(monthDayYear));
  }

  private ConditionalSkip conditionalSkip(String setLogic) {
    conditionalSkip = new ConditionalSkip();
    conditionalSkip.setSetLogic(setLogic);
    seriesDose.setConditionalSkip(conditionalSkip);
    return conditionalSkip;
  }

  private ConditionalSkipSet set(String conditionLogic) {
    if (conditionalSkip == null) {
      conditionalSkip("n/a");
    }
    ConditionalSkipSet conditionalSkipSet = new ConditionalSkipSet();
    conditionalSkipSet.setSetId(conditionalSkip.getConditionalSkipSetList().size() + 1);
    conditionalSkipSet.setConditionLogic(conditionLogic);
    conditionalSkip.getConditionalSkipSetList().add(conditionalSkipSet);
    return conditionalSkipSet;
  }

  private ConditionalSkipCondition condition(ConditionalSkipSet conditionalSkipSet,
      ConditionalSkipConditionType conditionType) {
    ConditionalSkipCondition condition = new ConditionalSkipCondition(seriesDose);
    condition.setConditionId(conditionalSkipSet.getConditionList().size() + 1);
    condition.setConditionType(conditionType);
    conditionalSkipSet.getConditionList().add(condition);
    return condition;
  }

  /** One set with condition logic "n/a" holding one condition of the given type. */
  private ConditionalSkipCondition soleCondition(ConditionalSkipConditionType conditionType) {
    return condition(set("n/a"), conditionType);
  }

  private ConditionalSkipCondition ageCondition(String beginAge, String endAge) {
    ConditionalSkipCondition condition = soleCondition(ConditionalSkipConditionType.AGE);
    ages(condition, beginAge, endAge);
    return condition;
  }

  private static void ages(ConditionalSkipCondition condition, String beginAge, String endAge) {
    if (beginAge != null) {
      condition.setBeginAge(new TimePeriod(beginAge));
    }
    if (endAge != null) {
      condition.setEndAge(new TimePeriod(endAge));
    }
  }

  private ConditionalSkipCondition intervalCondition(String interval, String previousDoseAdministered) {
    ConditionalSkipCondition condition = soleCondition(ConditionalSkipConditionType.INTERVAL);
    condition.setInterval(new TimePeriod(interval));
    if (previousDoseAdministered != null) {
      AntigenAdministeredRecord previous = new AntigenAdministeredRecord();
      previous.setDateAdministered(date(previousDoseAdministered));
      dataModel.setAntigenAdministeredRecordThatSatisfiedPreviousTargetDose(previous);
    }
    return condition;
  }

  private ConditionalSkipCondition vaccineCountCondition(String doseCountLogic, int doseCount) {
    ConditionalSkipCondition condition = soleCondition(ConditionalSkipConditionType.VACCINE_COUNT_BY_AGE);
    condition.setDoseCountLogic(doseCountLogic);
    condition.setDoseCount(doseCount);
    condition.setDoseType(DoseType.TOTAL);
    return condition;
  }

  private static VaccineType vaccineType(String cvxCode) {
    VaccineType vaccineType = new VaccineType();
    vaccineType.setCvxCode(cvxCode);
    vaccineType.setShortDescription("CVX " + cvxCode);
    return vaccineType;
  }

  /**
   * Adds one evaluated vaccine dose administered to the patient's immunization
   * history - the population CONDSKIP-1 counts over.
   */
  private VaccineDoseAdministered historicDose(VaccineType type, String monthDayYear,
      EvaluationStatus evaluationStatus) {
    Vaccine vaccine = new Vaccine();
    vaccine.setVaccineType(type);

    VaccineDoseAdministered vaccineDoseAdministered = new VaccineDoseAdministered();
    vaccineDoseAdministered.setVaccine(vaccine);
    vaccineDoseAdministered.setDateAdministered(date(monthDayYear));

    if (evaluationStatus != null) {
      TargetDose historicTargetDose = new TargetDose(new SeriesDose());
      Evaluation evaluation = new Evaluation();
      evaluation.setEvaluationStatus(evaluationStatus);
      historicTargetDose.setEvaluation(evaluation);
      vaccineDoseAdministered.setTargetDose(historicTargetDose);
    }

    immunizationHistory.getVaccineDoseAdministeredList().add(vaccineDoseAdministered);
    return vaccineDoseAdministered;
  }

  private LogicStep run() throws Exception {
    step = new EvaluateConditionalSkipForEvaluation(dataModel);
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  private ConditionAttribute<?> attribute(String attributeName) {
    for (ConditionAttribute<?> conditionAttribute : step.getConditionAttributeList()) {
      if (conditionAttribute != null
          && attributeName.equalsIgnoreCase(conditionAttribute.getAttributeName())) {
        return conditionAttribute;
      }
    }
    return null;
  }

  private ConditionAttribute<?> requiredAttribute(String attributeName) {
    ConditionAttribute<?> conditionAttribute = attribute(attributeName);
    if (conditionAttribute == null) {
      fail("Table 6-4 attribute '" + attributeName + "' is not registered by the step");
    }
    return conditionAttribute;
  }

  private List<EvaluateConditionalSkip.LTInnerSet> conditionTables() {
    List<EvaluateConditionalSkip.LTInnerSet> tables = new ArrayList<EvaluateConditionalSkip.LTInnerSet>();
    for (LogicTable logicTable : step.getLogicTableList()) {
      if (logicTable instanceof EvaluateConditionalSkip.LTInnerSet) {
        tables.add((EvaluateConditionalSkip.LTInnerSet) logicTable);
      }
    }
    return tables;
  }

  private EvaluateConditionalSkip.LTInnerSet onlyConditionTable() {
    List<EvaluateConditionalSkip.LTInnerSet> tables = conditionTables();
    assertEquals("exactly one per-condition table was expected", 1, tables.size());
    return tables.get(0);
  }

  private List<EvaluateConditionalSkip.LT610> setTables() {
    List<EvaluateConditionalSkip.LT610> tables = new ArrayList<EvaluateConditionalSkip.LT610>();
    for (LogicTable logicTable : step.getLogicTableList()) {
      if (logicTable instanceof EvaluateConditionalSkip.LT610) {
        tables.add((EvaluateConditionalSkip.LT610) logicTable);
      }
    }
    return tables;
  }

  private EvaluateConditionalSkip.LT610 onlySetTable() {
    List<EvaluateConditionalSkip.LT610> tables = setTables();
    assertEquals("exactly one Table 6-10 was expected", 1, tables.size());
    return tables.get(0);
  }

  private EvaluateConditionalSkip.LT611 tableSixEleven() {
    for (LogicTable logicTable : step.getLogicTableList()) {
      if (logicTable instanceof EvaluateConditionalSkip.LT611) {
        return (EvaluateConditionalSkip.LT611) logicTable;
      }
    }
    fail("the step built no Table 6-11");
    return null;
  }

  private static LogicResult conditionResult(LogicTable logicTable, int condition) {
    return logicTable.getLogicConditions()[condition].getLogicResult();
  }

  /** Answers Table 6-6/6-7/6-8/6-9 for the single condition under test. */
  private boolean conditionMet() throws Exception {
    run();
    return onlyConditionTable().isMet();
  }

  /**
   * Labels are compared with whitespace, case, and the punctuation the two
   * documents disagree about ('.', '-', en dash, quotes) removed: the
   * specification writes "TABLE 6-10 IS THE CONDITIONAL SKIP SET MET?" where the
   * implementation writes "Table 6 - 10 Is the Conditional Skip Set Met?", and
   * the specification's "&ge;" is the implementation's "&gt;=". Those are
   * transcription differences, not behavioural ones - a different table
   * <em>number</em> is not.
   */
  private static String normalized(String label) {
    if (label == null) {
      return null;
    }
    return label.replaceAll("\\s+", "")
        .replace(".", "").replace("-", "").replace("–", "").replace("—", "")
        .replace("'", "").replace("‘", "").replace("’", "")
        .replace("≥", ">=")
        .toLowerCase();
  }

  private static void assertLabelIs(String expected, String actual) {
    assertEquals("expected label '" + expected + "' but was '" + actual + "'",
        normalized(expected), normalized(actual));
  }

  // ============================================== Entry: what 6.2's own class is

  /**
   * 6.2's own class is the shared conditional-skip logic bound to the evaluation
   * context: it is an {@link EvaluateConditionalSkip}, it identifies itself as
   * {@code EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION}, and it names Table 6-4 as
   * its attribute table.
   */
  @Test
  public void theEvaluationStepIsTheSharedConditionalSkipLogicBoundToTheEvaluationContext() throws Exception {
    ageCondition("1 year", "5 years");

    run();

    assertTrue("6.2's class extends the shared implementation 7.1 also uses",
        step instanceof EvaluateConditionalSkip);
    assertEquals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION, step.getLogicStepType());
    assertLabelIs("Table 6-4 Conditional Skip Attributes", step.getConditionTableName());
  }

  /**
   * Purpose: "In cases where a target dose does not specify Conditional Skip
   * attributes, the target dose cannot be skipped." No decision table is built
   * at all, no status is recorded, and evaluation continues at 6.3.
   */
  @Test
  public void aTargetDoseWithNoConditionalSkipAttributesCannotBeSkipped() throws Exception {
    assertNull("the fixture's series dose defines no conditional skip", seriesDose.getConditionalSkip());

    run();

    assertEquals("no conditional skip means no decision tables", 0, step.getLogicTableList().size());
    assertFalse("the target dose cannot be skipped",
        TargetDoseStatus.SKIPPED.equals(targetDose.getTargetDoseStatus()));
    assertEquals("evaluation continues at 6.3 Evaluate for Inadvertent Vaccine",
        LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE, step.getNextLogicStepType());
  }

  /**
   * Entry condition: "Only Conditional Skip Instances with a context of
   * Evaluation or Both should be used." The base class carries a code comment
   * saying exactly this ("before the for loop starts, we should eliminate
   * Conditional Skip instances without a context of Evaluation or Both") but no
   * code implementing it - and it could not, because {@code ConditionalSkip}
   * has no context at all and {@code SeriesDose} holds exactly one of them.
   *
   * <p>
   * This is not hypothetical. In the Supporting Data release bundled with
   * {@code cdsi-engine} every {@code <conditionalSkip>} element carries a
   * {@code <context>} (140 "Both", 57 "Evaluation", 67 "Forecast"), and 67
   * series doses carry <em>two</em> of them - an Evaluation-or-Both instance
   * followed by a Forecast one. {@code DataModelLoader} calls
   * {@code seriesDose.setConditionalSkip(...)} once per element, so for all 67
   * the Forecast-only instance is the one retained and the
   * Evaluation-or-Both instance is discarded; 6.2 then evaluates a conditional
   * skip the specification says it must not use, and never sees the one it
   * must.
   *
   * <p>
   * The gap is in the domain model and the loader rather than in
   * {@code EvaluateConditionalSkip} itself, so this test asks the smallest
   * question that can be asked in isolation: can 6.2's entry condition even be
   * expressed?
   */
  @Test
  public void aConditionalSkipInstanceCarriesTheContextThatDecidesWhetherSixTwoMayUseIt() {
    boolean representable = false;
    for (Method method : ConditionalSkip.class.getMethods()) {
      if (method.getName().toLowerCase().contains("context")) {
        representable = true;
      }
    }
    assertTrue("6.2 must use only Conditional Skip instances with a context of Evaluation or Both, "
        + "but ConditionalSkip carries no context for it to read", representable);
  }

  // ================================================== Table 6-4: the attributes

  /**
   * Table 6-4's top-level attributes: Date Administered from the vaccine dose
   * administered, Administered Dose Count from the patient immunization history,
   * and the Assessment Date from runtime data, assumed to be the current date
   * when empty.
   */
  @Test
  public void tableSixFourCarriesTheTopLevelAttributesTheSpecificationNames() throws Exception {
    administered("06/15/2016");
    ageCondition("1 year", "5 years");

    run();

    assertEquals("Vaccine dose administered", requiredAttribute(DATE_ADMINISTERED).getAttributeType());
    assertEquals(date("06/15/2016"), requiredAttribute(DATE_ADMINISTERED).getInitialValue());
    assertNull("Table 6-4 gives Date Administered no assumed value",
        requiredAttribute(DATE_ADMINISTERED).getAssumedValue());

    assertEquals("Patient Immunization History",
        requiredAttribute(ADMINISTERED_DOSE_COUNT).getAttributeType());

    assertEquals("Runtime data", requiredAttribute(ASSESSMENT_DATE).getAttributeType());
    assertEquals(date("06/01/2021"), requiredAttribute(ASSESSMENT_DATE).getInitialValue());
    assertNotNull("Table 6-4 assumes the Assessment Date is the current date when empty",
        requiredAttribute(ASSESSMENT_DATE).getAssumedValue());
  }

  /**
   * Table 6-4 lists a fifth top-level attribute, "Runtime data / Earliest Date",
   * which CONDSKIP-2 uses as the reference date when validating a forecast. The
   * base class declares a {@code caEarliestDate} field, never constructs it, and
   * then adds it to the step's attribute list - so the list the step publishes
   * contains a null entry where this attribute should be.
   */
  @Test
  public void tableSixFourRegistersTheEarliestDateAttributeItLists() throws Exception {
    ageCondition("1 year", "5 years");

    run();

    for (ConditionAttribute<?> conditionAttribute : step.getConditionAttributeList()) {
      assertNotNull("Table 6-4's attribute list must not contain a null entry", conditionAttribute);
    }
    assertEquals("Table 6-4 sources the Earliest Date from runtime data",
        "Runtime data", requiredAttribute(EARLIEST_DATE).getAttributeType());
  }

  /**
   * Table 6-4's per-condition attributes and the assumed values it gives them:
   * Start Date 01/01/1900, End Date 12/31/2999, Conditional Skip Begin Age Date
   * 01/01/1900 (CALCDTSKIP-3), Conditional Skip End Age Date 12/31/2999
   * (CALCDTSKIP-4), and Conditional Skip Interval Date (CALCDTSKIP-5) with no
   * assumed value at all.
   */
  @Test
  public void tableSixFourPerConditionAttributesCarryTheirSourcesAndAssumedValues() throws Exception {
    ageCondition("1 year", "5 years");

    run();
    EvaluateConditionalSkip.LTInnerSet table = onlyConditionTable();

    assertEquals(SUPPORTING_DATA, table.caConditionalSkipElements.getAttributeType());
    assertEquals("Conditional Skip Elements", table.caConditionalSkipElements.getAttributeName());

    assertEquals(SUPPORTING_DATA, table.caStartDate.getAttributeType());
    assertEquals(date("01/01/1900"), table.caStartDate.getAssumedValue());
    assertEquals(SUPPORTING_DATA, table.caEndDate.getAttributeType());
    assertEquals(date("12/31/2999"), table.caEndDate.getAssumedValue());

    assertEquals("Calculated date (CALCDTSKIP-3)", table.caConditionalSkipBeginAgeDate.getAttributeType());
    assertEquals(date("01/01/1900"), table.caConditionalSkipBeginAgeDate.getAssumedValue());
    assertEquals("Calculated date (CALCDTSKIP-4)", table.caConditionalSkipEndAgeDate.getAttributeType());
    assertEquals(date("12/31/2999"), table.caConditionalSkipEndAgeDate.getAssumedValue());
    assertEquals("Calculated date (CALCDTSKIP-5)", table.caConditionalSkipIntervalDate.getAttributeType());
    assertNull("Table 6-4 gives the Conditional Skip Interval Date no assumed value",
        table.caConditionalSkipIntervalDate.getAssumedValue());
  }

  /**
   * Table 6-4 sources the Administered Dose Count from the "Patient Immunization
   * History", and Table 6-8's first condition asks whether at least one dose has
   * been administered "to the patient". The base class labels the attribute that
   * way but initializes it from
   * {@code dataModel.getSelectedAntigenAdministeredRecordList()}, which 4.2/4.4
   * have narrowed to the doses relevant to the antigen currently being
   * evaluated.
   */
  @Test
  public void theAdministeredDoseCountComesFromThePatientImmunizationHistory() throws Exception {
    historicDose(vaccineType("08"), "06/01/2016", EvaluationStatus.VALID);
    historicDose(vaccineType("20"), "08/01/2016", EvaluationStatus.VALID);
    historicDose(vaccineType("10"), "10/01/2016", EvaluationStatus.VALID);
    ageCondition("1 year", "5 years");

    run();

    assertEquals("three doses are recorded in the patient's immunization history",
        Integer.valueOf(3), requiredAttribute(ADMINISTERED_DOSE_COUNT).getFinalValue());
  }

  // ======================================================== CONDSKIP-2

  /**
   * CONDSKIP-2: "The Conditional Skip Reference Date must be one of the
   * following: the Date Administered of the vaccine dose administered when
   * evaluating a vaccine dose administered; the Assessment Date when determining
   * a forecast; the Earliest Date when validating a forecast." This is the one
   * place the shared class branches on its context; 6.2 is the evaluating arm.
   */
  @Test
  public void condskipTwoUsesTheDateAdministeredAsTheReferenceDateWhenEvaluating() throws Exception {
    administered("03/15/2017");
    ageCondition("1 year", "5 years");

    run();

    assertEquals("CONDSKIP-2, evaluating: the reference date is the date administered",
        date("03/15/2017"), onlyConditionTable().caConditionalSkipReferenceDate.getFinalValue());
    assertFalse("the assessment date is the forecasting arm's reference date, not this one",
        date("06/01/2021").equals(onlyConditionTable().caConditionalSkipReferenceDate.getFinalValue()));
  }

  // ========================================== CALCDTSKIP-3 / -4 / -5

  /**
   * CALCDTSKIP-3: "A patient's conditional skip begin age date must be
   * calculated as the patient's date of birth plus the conditional skip begin
   * age of a conditional skip."
   */
  @Test
  public void calcdtskipThreeIsDateOfBirthPlusTheConditionalSkipBeginAge() throws Exception {
    bornOn("01/01/2015");
    ageCondition("4 years", "18 years");

    run();

    assertEquals(date("01/01/2019"), onlyConditionTable().caConditionalSkipBeginAgeDate.getFinalValue());
  }

  /**
   * CALCDTSKIP-4: "A patient's conditional skip end age date must be calculated
   * as the patient's date of birth plus the conditional skip end age of a
   * conditional skip."
   */
  @Test
  public void calcdtskipFourIsDateOfBirthPlusTheConditionalSkipEndAge() throws Exception {
    bornOn("01/01/2015");
    ageCondition("4 years", "18 years");

    run();

    assertEquals(date("01/01/2033"), onlyConditionTable().caConditionalSkipEndAgeDate.getFinalValue());
  }

  /**
   * CALCDTSKIP-5: "A patient's conditional skip interval date must be calculated
   * as the vaccine date administered from the immediate previous vaccine dose
   * administered plus the Interval of the conditional skip condition."
   */
  @Test
  public void calcdtskipFiveIsThePreviousDoseAdministeredPlusTheConditionInterval() throws Exception {
    intervalCondition("6 months", "01/15/2016");

    run();

    assertEquals(date("07/15/2016"), onlyConditionTable().caConditionalSkipIntervalDate.getFinalValue());
  }

  /**
   * A condition that names no ages and no interval falls back to Table 6-4's
   * assumed values: an open-ended age window, and no interval date at all.
   */
  @Test
  public void aConditionWithNoAgesOrIntervalFallsBackToTableSixFoursAssumedDates() throws Exception {
    soleCondition(ConditionalSkipConditionType.AGE);

    run();
    EvaluateConditionalSkip.LTInnerSet table = onlyConditionTable();

    assertNull(table.caConditionalSkipBeginAgeDate.getInitialValue());
    assertEquals(date("01/01/1900"), table.caConditionalSkipBeginAgeDate.getFinalValue());
    assertNull(table.caConditionalSkipEndAgeDate.getInitialValue());
    assertEquals(date("12/31/2999"), table.caConditionalSkipEndAgeDate.getFinalValue());
    assertNull("no interval means no interval date",
        table.caConditionalSkipIntervalDate.getFinalValue());
  }

  // ================================================================ CONDSKIP-1

  /**
   * CONDSKIP-1, first bullet: "The vaccine type of the vaccine dose administered
   * is one of the conditional skip vaccine types."
   */
  @Test
  public void condskipOneCountsOnlyDosesOfTheConditionalSkipVaccineTypes() throws Exception {
    VaccineType counted = vaccineType("20");
    historicDose(counted, "06/01/2016", EvaluationStatus.VALID);
    historicDose(counted, "08/01/2016", EvaluationStatus.VALID);
    historicDose(vaccineType("10"), "09/01/2016", EvaluationStatus.VALID);

    ConditionalSkipCondition condition = vaccineCountCondition("greater than", 1);
    condition.getVaccineTypeSet().add(counted);

    run();

    assertEquals("only the two doses of the conditional skip's vaccine type count",
        Integer.valueOf(2), onlyConditionTable().caNumberofConditionalDosesAdministered.getFinalValue());
  }

  /**
   * CONDSKIP-1, second bullet: "The date administered is on or after the
   * conditional skip begin age date and before the conditional skip end age
   * date." Both boundaries are pinned here: a dose given exactly on the begin
   * age date counts, one given exactly on the end age date does not.
   */
  @Test
  public void condskipOneCountsDosesOnOrAfterTheBeginAgeDateAndBeforeTheEndAgeDate() throws Exception {
    bornOn("01/01/2015");
    historicDose(vaccineType("20"), "12/31/2015", EvaluationStatus.VALID); // before begin age date
    historicDose(vaccineType("20"), "01/01/2016", EvaluationStatus.VALID); // on begin age date
    historicDose(vaccineType("20"), "06/01/2016", EvaluationStatus.VALID); // inside
    historicDose(vaccineType("20"), "01/01/2017", EvaluationStatus.VALID); // on end age date
    historicDose(vaccineType("20"), "06/01/2017", EvaluationStatus.VALID); // after end age date

    ConditionalSkipCondition condition = vaccineCountCondition("greater than", 0);
    ages(condition, "1 year", "2 years");

    run();

    assertEquals("on the begin age date counts, on the end age date does not",
        Integer.valueOf(2), onlyConditionTable().caNumberofConditionalDosesAdministered.getFinalValue());
  }

  /**
   * CONDSKIP-1, third bullet: "The date administered is on or after the
   * conditional skip start date and before conditional skip end date."
   */
  @Test
  public void condskipOneCountsDosesOnOrAfterTheStartDateAndBeforeTheEndDate() throws Exception {
    historicDose(vaccineType("20"), "12/31/2015", EvaluationStatus.VALID);
    historicDose(vaccineType("20"), "01/01/2016", EvaluationStatus.VALID);
    historicDose(vaccineType("20"), "06/01/2016", EvaluationStatus.VALID);
    historicDose(vaccineType("20"), "01/01/2017", EvaluationStatus.VALID);

    ConditionalSkipCondition condition = vaccineCountCondition("greater than", 0);
    condition.setStartDate(date("01/01/2016"));
    condition.setEndDate(date("01/01/2017"));

    run();

    assertEquals("on the start date counts, on the end date does not",
        Integer.valueOf(2), onlyConditionTable().caNumberofConditionalDosesAdministered.getFinalValue());
  }

  /**
   * CONDSKIP-1, fourth bullet: the evaluation status must be 'Valid' when the
   * conditional skip dose type is 'Valid'.
   */
  @Test
  public void condskipOneCountsOnlyValidDosesWhenTheDoseTypeIsValid() throws Exception {
    historicDose(vaccineType("20"), "06/01/2016", EvaluationStatus.VALID);
    historicDose(vaccineType("20"), "08/01/2016", EvaluationStatus.NOT_VALID);
    historicDose(vaccineType("20"), "10/01/2016", EvaluationStatus.SUB_STANDARD);

    ConditionalSkipCondition condition = vaccineCountCondition("greater than", 0);
    condition.setDoseType(DoseType.VALID);

    run();

    assertEquals("dose type 'Valid' counts only doses evaluated Valid",
        Integer.valueOf(1), onlyConditionTable().caNumberofConditionalDosesAdministered.getFinalValue());
  }

  /**
   * CONDSKIP-1, fourth bullet, other half: any status counts when the
   * conditional skip dose type is 'Total'.
   */
  @Test
  public void condskipOneCountsDosesOfAnyEvaluationStatusWhenTheDoseTypeIsTotal() throws Exception {
    historicDose(vaccineType("20"), "06/01/2016", EvaluationStatus.VALID);
    historicDose(vaccineType("20"), "08/01/2016", EvaluationStatus.NOT_VALID);
    historicDose(vaccineType("20"), "10/01/2016", EvaluationStatus.SUB_STANDARD);

    ConditionalSkipCondition condition = vaccineCountCondition("greater than", 0);
    condition.setDoseType(DoseType.TOTAL);

    run();

    assertEquals("dose type 'Total' counts every status",
        Integer.valueOf(3), onlyConditionTable().caNumberofConditionalDosesAdministered.getFinalValue());
  }

  // =========================================================== Table 6-6 (Age)

  /**
   * Table 6-6's single condition and its two-rule grid, as the specification
   * writes them.
   */
  @Test
  public void tableSixSixIsEncodedWithTheConditionAndRuleGridTheSpecificationWrites() throws Exception {
    ageCondition("1 year", "5 years");

    run();
    LogicTable table = onlyConditionTable();

    assertEquals("Table 6-6 has one condition", 1, table.getLogicConditions().length);
    assertEquals("Table 6-6 has two rules", 2, table.getLogicOutcomes().length);
    assertLabelIs("Is the Conditional Skip End Age Date > Conditional Skip Reference Date "
        + "≥ Conditional Skip Begin Age Date?", table.getLogicConditions()[0].getLabel());
    assertArrayEquals("Table 6-6: Yes / No",
        new LogicResult[] { LogicResult.YES, LogicResult.NO }, table.getLogicResultTable()[0]);
  }

  /**
   * The age table is Table 6-6 of the current specification. The implementation
   * labels it "Table 4-6", a leftover from an earlier chapter numbering; every
   * other table in this step names itself correctly.
   */
  @Test
  public void theAgeTableNamesItselfWithItsCurrentSpecificationNumber() throws Exception {
    ageCondition("1 year", "5 years");

    run();

    assertLabelIs("Table 6-6 CONDITIONAL Type of Age - Is the Condition Met?",
        onlyConditionTable().getLabel());
  }

  /**
   * Table 6-6 Rule 1: the reference date falls inside the conditional skip age
   * window, so the condition is met.
   */
  @Test
  public void tableSixSixRuleOneAnAgeConditionInsideItsWindowIsMet() throws Exception {
    bornOn("01/01/2015");
    administered("06/01/2016");
    ageCondition("1 year", "5 years");

    assertTrue("01/01/2016 <= 06/01/2016 < 01/01/2020", conditionMet());
    assertEquals(LogicResult.YES, conditionResult(onlyConditionTable(), 0));
  }

  /**
   * Table 6-6 Rule 2: the reference date is outside the window - before it, and
   * on or after its end - so the condition is not met.
   */
  @Test
  public void tableSixSixRuleTwoAnAgeConditionOutsideItsWindowIsNotMet() throws Exception {
    bornOn("01/01/2015");
    administered("06/01/2015");
    ageCondition("1 year", "5 years");
    assertFalse("06/01/2015 is before the begin age date 01/01/2016", conditionMet());

    setUp();
    bornOn("01/01/2015");
    administered("06/01/2021");
    ageCondition("1 year", "5 years");
    assertFalse("06/01/2021 is after the end age date 01/01/2020", conditionMet());
  }

  /**
   * Table 6-6's condition is "End Age Date &gt; Reference Date &ge; Begin Age
   * Date": inclusive of the begin age date, exclusive of the end age date. The
   * implementation writes both comparisons as {@code Date.after(...)}, making
   * the lower bound exclusive too, so a dose administered on exactly the
   * conditional skip begin age date falls outside its own window.
   */
  @Test
  public void theAgeWindowIsInclusiveOfItsBeginAgeDateAndExclusiveOfItsEndAgeDate() throws Exception {
    bornOn("01/01/2015");
    administered("01/01/2020");
    ageCondition("1 year", "5 years");
    assertFalse("the end age date itself is outside the window", conditionMet());

    setUp();
    bornOn("01/01/2015");
    administered("01/01/2016");
    ageCondition("1 year", "5 years");
    assertTrue("the begin age date itself is inside the window", conditionMet());
  }

  /**
   * With no ages supplied at all, Table 6-4's assumed 01/01/1900 and 12/31/2999
   * make the window open-ended and any reference date meets the condition.
   */
  @Test
  public void anAgeConditionWithNoAgesIsMetForAnyReferenceDate() throws Exception {
    administered("06/01/2016");
    soleCondition(ConditionalSkipConditionType.AGE);

    assertTrue("01/01/1900 <= 06/01/2016 < 12/31/2999", conditionMet());
  }

  // ============================================== Table 6-7 (Completed Series)

  /** Table 6-7's single condition and its two-rule grid. */
  @Test
  public void tableSixSevenIsEncodedWithOneConditionAndTwoRules() throws Exception {
    soleCondition(ConditionalSkipConditionType.COMPLETED_SERIES);

    run();
    LogicTable table = onlyConditionTable();

    assertLabelIs("Table 6-7 CONDITIONAL TYPE OF COMPLETED SERIES - IS THE CONDITION MET?",
        table.getLabel());
    assertEquals(1, table.getLogicConditions().length);
    assertEquals(2, table.getLogicOutcomes().length);
    assertArrayEquals("Table 6-7: Yes / No",
        new LogicResult[] { LogicResult.YES, LogicResult.NO }, table.getLogicResultTable()[0]);
  }

  /**
   * Table 6-7: "Does the Conditional Skip Series Group identify a Series Group
   * with at least one relevant patient series with a patient series status of
   * 'Complete'?" The implementation's condition body is
   * {@code return LogicResult.NO;} - it reads neither the condition's series
   * group nor the patient's series statuses, so a COMPLETED_SERIES condition can
   * never be met.
   *
   * <p>
   * Two things stand behind that. {@code ConditionalSkipCondition} has no series
   * group field and {@code DataModelLoader.readCondition} ignores the
   * {@code <seriesGroups>} element the Supporting Data supplies, so the left
   * half of the question is not representable either. And it is used: the
   * bundled release defines 25 COMPLETED_SERIES conditions, across HepB,
   * Pneumococcal and Polio, every one of them inside an AND set - where an
   * always-No condition defeats the whole set.
   *
   * <p>
   * The fixture supplies exactly what the condition asks about: a patient series
   * in series group "1" whose status is Complete.
   */
  @Test
  public void tableSixSevenIsMetWhenTheSeriesGroupHasACompleteRelevantPatientSeries() throws Exception {
    SelectPatientSeries selectPatientSeries = new SelectPatientSeries();
    selectPatientSeries.setSeriesGroup("1");
    selectPatientSeries.setSeriesGroupName("Pneumococcal standard");

    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName("Pneumococcal 4 dose series");
    antigenSeries.setSelectPatientSeries(selectPatientSeries);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setPatientSeriesStatus(PatientSeriesStatus.COMPLETE);
    dataModel.getPatientSeriesStepper().add(patientSeries);
    dataModel.getSelectedPatientSeriesList().add(patientSeries);

    soleCondition(ConditionalSkipConditionType.COMPLETED_SERIES);

    assertTrue("series group 1 holds a relevant patient series with status 'Complete'", conditionMet());
  }

  // ====================================================== Table 6-8 (Interval)

  /**
   * Table 6-8's two conditions and its three-rule grid, exactly as the
   * specification writes them - including the third rule, which answers "not
   * met" on the strength of the first condition alone.
   */
  @Test
  public void tableSixEightIsEncodedWithTheTwoConditionsAndThreeRulesTheSpecificationWrites()
      throws Exception {
    intervalCondition("6 months", "01/15/2016");

    run();
    LogicTable table = onlyConditionTable();

    assertLabelIs("Table 6-8 CONDITIONAL Type of Interval - Is the Condition Met?", table.getLabel());
    assertEquals("Table 6-8 has two conditions", 2, table.getLogicConditions().length);
    assertEquals("Table 6-8 has three rules", 3, table.getLogicOutcomes().length);
    assertEquals("Has at least one dose been administered to the patient?",
        table.getLogicConditions()[0].getLabel());
    assertLabelIs("Is the Conditional Skip Reference Date ≥ Conditional Skip Interval Date?",
        table.getLogicConditions()[1].getLabel());
    assertArrayEquals("Table 6-8 row 1: Yes / Yes / No",
        new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.NO },
        table.getLogicResultTable()[0]);
    assertArrayEquals("Table 6-8 row 2: Yes / No / -",
        new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.ANY },
        table.getLogicResultTable()[1]);
  }

  /**
   * Table 6-8 Rule 1: a dose has been administered and the reference date has
   * reached the conditional skip interval date, so the condition is met.
   */
  @Test
  public void tableSixEightRuleOneIsMetWhenADoseWasGivenAndTheIntervalDateHasPassed() throws Exception {
    administered("09/01/2016");
    intervalCondition("6 months", "01/15/2016");

    assertTrue("09/01/2016 >= the interval date 07/15/2016", conditionMet());
    assertEquals(LogicResult.YES, conditionResult(onlyConditionTable(), 0));
    assertEquals(LogicResult.YES, conditionResult(onlyConditionTable(), 1));
  }

  /**
   * Table 6-8 Rule 2: a dose has been administered but the reference date has
   * not reached the interval date.
   */
  @Test
  public void tableSixEightRuleTwoIsNotMetBeforeTheIntervalDate() throws Exception {
    administered("05/01/2016");
    intervalCondition("6 months", "01/15/2016");

    assertFalse("05/01/2016 is before the interval date 07/15/2016", conditionMet());
    assertEquals(LogicResult.YES, conditionResult(onlyConditionTable(), 0));
    assertEquals(LogicResult.NO, conditionResult(onlyConditionTable(), 1));
  }

  /**
   * Table 6-8 Rule 3: no dose has been administered to the patient at all, so
   * the interval condition is not met whatever the dates say - the second
   * condition is '-'.
   */
  @Test
  public void tableSixEightRuleThreeIsNotMetWhenNoDoseHasBeenAdministered() throws Exception {
    dataModel.setSelectedAntigenAdministeredRecordList(new ArrayList<AntigenAdministeredRecord>());
    administered("09/01/2016");
    intervalCondition("6 months", "01/15/2016");

    assertFalse("no dose has been administered to the patient", conditionMet());
    assertEquals(LogicResult.NO, conditionResult(onlyConditionTable(), 0));
  }

  /**
   * Table 6-8's second condition is "Reference Date &ge; Interval Date". The
   * implementation writes it as {@code intervalDate.before(referenceDate)},
   * which is a strict "&gt;", so a dose administered on exactly the conditional
   * skip interval date does not satisfy the interval it has in fact reached.
   */
  @Test
  public void theIntervalConditionIsMetWhenTheReferenceDateEqualsTheIntervalDate() throws Exception {
    administered("07/15/2016");
    intervalCondition("6 months", "01/15/2016");

    assertTrue("the interval date itself satisfies 'reference date >= interval date'", conditionMet());
  }

  // ================================================= Table 6-9 (Vaccine Count)

  /**
   * Table 6-9's "Greater Than" row: met when the number of conditional doses
   * administered is greater than the conditional skip dose count, not met when
   * it is equal to or less than it.
   */
  @Test
  public void tableSixNineGreaterThanRow() throws Exception {
    assertEquals("3 doses is greater than a dose count of 2", true, vaccineCount("greater than", 2, 3));
    assertEquals("2 doses is not greater than a dose count of 2", false, vaccineCount("greater than", 2, 2));
    assertEquals("1 dose is not greater than a dose count of 2", false, vaccineCount("greater than", 2, 1));
  }

  /**
   * Table 6-9's "Equal" row, spelled the way the CDC Supporting Data spells it.
   * The implementation matches the dose count logic against the bare word
   * "equal" ({@code equalsIgnoreCase("equal")}), but all 69 equality conditions
   * in the release bundled with {@code cdsi-engine} are written "equal to", so
   * none of them matches and every one falls through to the method's closing
   * "No" - the Equal row of Table 6-9 is unreachable in practice. ("greater
   * than" and "Greater Than", the other 234, match as written.)
   */
  @Test
  public void tableSixNineEqualRowAsTheSupportingDataSpellsIt() throws Exception {
    assertEquals("2 doses is equal to a dose count of 2", true, vaccineCount("equal to", 2, 2));
    assertEquals("3 doses is not equal to a dose count of 2", false, vaccineCount("equal to", 2, 3));
    assertEquals("1 dose is not equal to a dose count of 2", false, vaccineCount("equal to", 2, 1));
  }

  /** Table 6-9's "Equal" row under the spelling the implementation recognises. */
  @Test
  public void tableSixNineEqualRowAsTheImplementationSpellsIt() throws Exception {
    assertEquals(true, vaccineCount("equal", 2, 2));
    assertEquals(false, vaccineCount("equal", 2, 3));
    assertEquals(false, vaccineCount("equal", 2, 1));
  }

  /**
   * Table 6-9's "Less Than" row: met when the number of conditional doses
   * administered is less than the conditional skip dose count.
   */
  @Test
  public void tableSixNineLessThanRow() throws Exception {
    assertEquals("1 dose is less than a dose count of 2", true, vaccineCount("less than", 2, 1));
    assertEquals("2 doses is not less than a dose count of 2", false, vaccineCount("less than", 2, 2));
    assertEquals("3 doses is not less than a dose count of 2", false, vaccineCount("less than", 2, 3));
  }

  /**
   * Table 6-9 covers "Vaccine Count by Age and/or Date", which the Supporting
   * Data spells as three separate condition types; all three route to the same
   * table.
   */
  @Test
  public void allThreeVaccineCountConditionTypesUseTableSixNine() throws Exception {
    ConditionalSkipConditionType[] types = {
        ConditionalSkipConditionType.VACCINE_COUNT_BY_AGE,
        ConditionalSkipConditionType.VACCINE_COUNT_BY_DATE,
        ConditionalSkipConditionType.VACCINE_COUNT_BY_DATE_AND_AGE };

    for (ConditionalSkipConditionType type : types) {
      setUp();
      historicDose(vaccineType("20"), "06/01/2016", EvaluationStatus.VALID);
      ConditionalSkipCondition condition = soleCondition(type);
      condition.setConditionType(type);
      condition.setDoseCountLogic("greater than");
      condition.setDoseCount(0);
      condition.setDoseType(DoseType.TOTAL);

      run();

      assertLabelIs("Table 6-9 CONDITIONAL TYPE OF VACCINE COUNT BY AGE AND/OR DATE - IS THE CONDITION MET?",
          onlyConditionTable().getLabel());
      assertTrue("condition type " + type + " is met by 1 dose > a dose count of 0",
          onlyConditionTable().isMet());
    }
  }

  /**
   * Builds a vaccine count condition with the given dose count logic and
   * conditional skip dose count, gives the patient {@code dosesAdministered}
   * matching doses, and answers Table 6-9.
   */
  private boolean vaccineCount(String doseCountLogic, int doseCount, int dosesAdministered)
      throws Exception {
    setUp();
    VaccineType type = vaccineType("20");
    for (int i = 0; i < dosesAdministered; i++) {
      historicDose(type, "0" + (i + 1) + "/01/2016", EvaluationStatus.VALID);
    }
    ConditionalSkipCondition condition = vaccineCountCondition(doseCountLogic, doseCount);
    condition.getVaccineTypeSet().add(type);

    run();
    assertEquals("the fixture supplied " + dosesAdministered + " conditional doses",
        Integer.valueOf(dosesAdministered),
        onlyConditionTable().caNumberofConditionalDosesAdministered.getFinalValue());
    return onlyConditionTable().isMet();
  }

  // ================================== Table 6-10 (Is the Conditional Skip Set Met?)

  /** Table 6-10's single condition and its two outcomes. */
  @Test
  public void tableSixTenIsEncodedWithOneConditionAndTwoOutcomes() throws Exception {
    ageCondition("1 year", "5 years");

    run();
    LogicTable table = onlySetTable();

    assertLabelIs("Table 6-10 Is the Conditional Skip Set Met?", table.getLabel());
    assertEquals(1, table.getLogicConditions().length);
    assertEquals(2, table.getLogicOutcomes().length);
    assertArrayEquals(new LogicResult[] { LogicResult.YES, LogicResult.NO },
        table.getLogicResultTable()[0]);
  }

  /**
   * Table 6-10, Condition Logic Type AND: the set is met when all of its
   * conditions were met, and not met when only some or none were.
   */
  @Test
  public void tableSixTenAndIsMetOnlyWhenEveryConditionInTheSetIsMet() throws Exception {
    assertEquals("all conditions met", true, twoAgeConditions("AND", true, true));
    assertEquals("at least one, but not all", false, twoAgeConditions("AND", true, false));
    assertEquals("at least one, but not all", false, twoAgeConditions("AND", false, true));
    assertEquals("none", false, twoAgeConditions("AND", false, false));
  }

  /**
   * Table 6-10, Condition Logic Type OR: the set is met when all its conditions
   * were met and when at least one but not all were; it is not met only when
   * none were.
   */
  @Test
  public void tableSixTenOrIsMetWhenAtLeastOneConditionInTheSetIsMet() throws Exception {
    assertEquals("all conditions met", true, twoAgeConditions("OR", true, true));
    assertEquals("at least one, but not all", true, twoAgeConditions("OR", true, false));
    assertEquals("at least one, but not all", true, twoAgeConditions("OR", false, true));
    assertEquals("none", false, twoAgeConditions("OR", false, false));
  }

  /**
   * Builds one set with the given condition logic holding two age conditions,
   * each either satisfied or not by the reference date 06/01/2016, and answers
   * Table 6-10.
   */
  private boolean twoAgeConditions(String conditionLogic, boolean firstMet, boolean secondMet)
      throws Exception {
    setUp();
    bornOn("01/01/2015");
    administered("06/01/2016");
    ConditionalSkipSet conditionalSkipSet = set(conditionLogic);
    ages(condition(conditionalSkipSet, ConditionalSkipConditionType.AGE),
        firstMet ? "1 year" : "5 years", firstMet ? "5 years" : "10 years");
    ages(condition(conditionalSkipSet, ConditionalSkipConditionType.AGE),
        secondMet ? "1 year" : "5 years", secondMet ? "5 years" : "10 years");

    run();

    assertEquals("two conditions were built", 2, conditionTables().size());
    assertEquals("first condition", firstMet, conditionTables().get(0).isMet());
    assertEquals("second condition", secondMet, conditionTables().get(1).isMet());
    return onlySetTable().isMet();
  }

  // ================================ Table 6-11 (Can the Target Dose Be Skipped?)

  /** Table 6-11's single condition and its two outcomes. */
  @Test
  public void tableSixElevenIsEncodedWithOneConditionAndTwoOutcomes() throws Exception {
    ageCondition("1 year", "5 years");

    run();
    LogicTable table = tableSixEleven();

    assertLabelIs("Table 6-11 Can the Target Dose Be Skipped?", table.getLabel());
    assertEquals(1, table.getLogicConditions().length);
    assertEquals(2, table.getLogicOutcomes().length);
    assertArrayEquals(new LogicResult[] { LogicResult.YES, LogicResult.NO },
        table.getLogicResultTable()[0]);
  }

  /**
   * Table 6-11, Set Logic Type AND: the target dose can be skipped only when all
   * of the conditional skip's sets were met.
   */
  @Test
  public void tableSixElevenAndCanSkipOnlyWhenEverySetIsMet() throws Exception {
    assertEquals("all sets met", true, twoSets("AND", true, true));
    assertEquals("at least one, but not all", false, twoSets("AND", true, false));
    assertEquals("at least one, but not all", false, twoSets("AND", false, true));
    assertEquals("none", false, twoSets("AND", false, false));
  }

  /**
   * Table 6-11, Set Logic Type OR: the target dose can be skipped when at least
   * one set was met.
   */
  @Test
  public void tableSixElevenOrCanSkipWhenAtLeastOneSetIsMet() throws Exception {
    assertEquals("all sets met", true, twoSets("OR", true, true));
    assertEquals("at least one, but not all", true, twoSets("OR", true, false));
    assertEquals("at least one, but not all", true, twoSets("OR", false, true));
    assertEquals("none", false, twoSets("OR", false, false));
  }

  /**
   * Builds a conditional skip with the given set logic and two single-condition
   * sets, each met or not, and answers Table 6-11 by the target dose status it
   * left behind.
   */
  private boolean twoSets(String setLogic, boolean firstMet, boolean secondMet) throws Exception {
    setUp();
    bornOn("01/01/2015");
    administered("06/01/2016");
    conditionalSkip(setLogic);
    ages(condition(set("n/a"), ConditionalSkipConditionType.AGE),
        firstMet ? "1 year" : "5 years", firstMet ? "5 years" : "10 years");
    ages(condition(set("n/a"), ConditionalSkipConditionType.AGE),
        secondMet ? "1 year" : "5 years", secondMet ? "5 years" : "10 years");

    run();

    assertEquals("two sets were built", 2, setTables().size());
    assertEquals("first set", firstMet, setTables().get(0).isMet());
    assertEquals("second set", secondMet, setTables().get(1).isMet());
    return TargetDoseStatus.SKIPPED.equals(targetDose.getTargetDoseStatus());
  }

  /**
   * Table 6-11's "Yes" outcome: "The target dose can be skipped. The target dose
   * status is 'Skipped.'" Figure 6-3 then returns control to 4.4 without running
   * 6.3 onward for this target dose.
   */
  @Test
  public void skippingTheTargetDoseSetsItsStatusToSkippedAndReturnsToFourFour() throws Exception {
    bornOn("01/01/2015");
    administered("06/01/2016");
    ageCondition("1 year", "5 years");

    run();

    assertEquals(LogicResult.YES, conditionResult(tableSixEleven(), 0));
    assertEquals(TargetDoseStatus.SKIPPED, targetDose.getTargetDoseStatus());
    assertEquals("a skipped dose returns to 4.4 Evaluate and Forecast All Patient Series",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());
  }

  /**
   * Table 6-11's "No" outcome: the target dose cannot be skipped. No status is
   * recorded - a target dose status set earlier is left alone - and evaluation
   * continues at 6.3.
   */
  @Test
  public void notSkippingTheTargetDoseRecordsNothingAndContinuesToSixThree() throws Exception {
    bornOn("01/01/2015");
    administered("06/01/2021");
    targetDose.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
    ageCondition("1 year", "5 years");

    run();

    assertEquals(LogicResult.NO, conditionResult(tableSixEleven(), 0));
    assertEquals("Table 6-11's 'No' outcome records no target dose status",
        TargetDoseStatus.NOT_SATISFIED, targetDose.getTargetDoseStatus());
    assertEquals("evaluation continues at 6.3 Evaluate for Inadvertent Vaccine",
        LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE, step.getNextLogicStepType());
  }

  // ================================================================= structure

  /**
   * Figure 6-3's nesting, as decision tables: one per-condition table for every
   * condition, one Table 6-10 for every set, and exactly one Table 6-11 for the
   * target dose - evaluated in that order, so each roll-up sees the answers
   * below it.
   */
  @Test
  public void oneTableIsBuiltPerConditionAndPerSetPlusOneTableSixEleven() throws Exception {
    ConditionalSkipSet first = set("AND");
    ages(condition(first, ConditionalSkipConditionType.AGE), "1 year", "5 years");
    ages(condition(first, ConditionalSkipConditionType.AGE), "1 year", "10 years");
    ConditionalSkipSet second = set("n/a");
    ages(condition(second, ConditionalSkipConditionType.AGE), "1 year", "5 years");

    run();

    assertEquals("one per-condition table per condition", 3, conditionTables().size());
    assertEquals("one Table 6-10 per set", 2, setTables().size());
    assertNotNull("one Table 6-11 for the target dose", tableSixEleven());
    assertEquals("no other table is built", 6, step.getLogicTableList().size());
    assertTrue("Table 6-11 is evaluated last",
        step.getLogicTableList().get(5) instanceof EvaluateConditionalSkip.LT611);
  }
}
