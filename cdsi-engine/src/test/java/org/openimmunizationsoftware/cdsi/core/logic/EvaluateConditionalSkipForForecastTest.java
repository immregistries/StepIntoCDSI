package org.openimmunizationsoftware.cdsi.core.logic;

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
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkip;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipCondition;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipConditionType;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipSet;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * Section 7.1 "Evaluate Conditional Skip" (Logic Specification for ACIP
 * Recommendations v4.6, page 72) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/07-01-evaluate-conditional-skip/index.md}.
 *
 * <h2>Why this class is deliberately small</h2>
 *
 * <p>
 * 7.1 has no attribute table, no business rules and no decision tables of its
 * own. The specification says outright that "the process model, attribute
 * table, business rules, and decision tables used to determine if the target
 * dose can be skipped is the same as described in Chapter 6.2", and the code
 * agrees: {@code spec-to-code.yaml} maps 7.1 onto
 * {@link EvaluateConditionalSkipForForecast} - a two-line subclass - plus the
 * same shared {@link EvaluateConditionalSkip} base class that 6.2
 * ({@link EvaluateConditionalSkipForEvaluation}) also subclasses.
 *
 * <p>
 * {@code EvaluateConditionalSkipForEvaluationTest} already covers, through the
 * evaluation subclass, everything in that shared base class that does not
 * depend on the context it runs in: Table 6-4's top-level and per-condition
 * attributes with their sources and assumed values, CALCDTSKIP-3/-4/-5, all
 * four criteria of CONDSKIP-1, the encoding and every rule of Tables 6-6, 6-7,
 * 6-8 and 6-9, Table 6-10's and Table 6-11's AND/OR roll-ups, and Figure 6-3's
 * nesting. <strong>None of that is re-tested here.</strong> Re-asserting it
 * under 7.1's name would duplicate 6.2's work under a different unit number
 * without exercising a single additional line of code.
 *
 * <p>
 * What is genuinely 7.1's own, and is what this class covers:
 * <ol>
 * <li>the FORECAST arm of the shared constructor's one context switch -
 * CONDSKIP-2's Conditional Skip Reference Date, which is the Assessment Date
 * when determining a forecast rather than 6.2's Date Administered - and what
 * that substitution actually does to the skip decision;</li>
 * <li>{@code EvaluateConditionalSkipForForecast}'s own construction: the step
 * type it publishes, that {@code LogicStepFactory} builds it for 7.1, and its
 * two destinations, which are <em>not</em> 6.2's (no skip goes to 7.2 Determine
 * Evidence of Immunity, not 6.3);</li>
 * <li>7.1's own entry condition - only Conditional Skip instances with a
 * context of Forecast or Both apply here.</li>
 * </ol>
 *
 * <p>
 * (3) covers the same domain-model gap 6.2 found, but it is 7.1's own entry
 * condition read from the opposite direction and reaches a different
 * conclusion about the bundled data, so it is asserted once here rather than
 * inherited. See the class comment on that test.
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads and calls
 * {@code process()} directly; it ends by constructing its chosen next step,
 * which is inert (no Supporting Data release is loaded and {@code process()} is
 * never called on the returned step). The step's inner decision tables are
 * {@code protected} inner classes, so this test - in the same package - reads
 * their condition attributes and {@code isMet()} flags directly.
 */
public class EvaluateConditionalSkipForForecastTest {

  private static final String ASSESSMENT_DATE = "Assessment Date";

  /** The patient's date of birth in every fixture below. */
  private static final String BIRTH = "01/01/2015";
  /** Outside every "1 year to 4 years" window used below, inside "5 to 10". */
  private static final String ASSESSMENT = "06/01/2021";
  /** Inside every "1 year to 4 years" window used below, outside "5 to 10". */
  private static final String ADMINISTERED = "06/01/2016";

  private DataModel dataModel;
  private Patient patient;
  private AntigenAdministeredRecord antigenAdministeredRecord;
  private SeriesDose seriesDose;
  private TargetDose targetDose;
  private ConditionalSkip conditionalSkip;
  private EvaluateConditionalSkipForForecast step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    patient = new Patient();
    patient.setDateOfBirth(date(BIRTH));
    dataModel.setPatient(patient);

    dataModel.setImmunizationHistory(new ImmunizationHistory());
    dataModel.setAssessmentDate(date(ASSESSMENT));

    antigenAdministeredRecord = new AntigenAdministeredRecord();
    antigenAdministeredRecord.setDateAdministered(date(ADMINISTERED));
    dataModel.setAntigenAdministeredRecord(antigenAdministeredRecord);
    List<AntigenAdministeredRecord> selected = new ArrayList<AntigenAdministeredRecord>();
    selected.add(antigenAdministeredRecord);
    dataModel.setSelectedAntigenAdministeredRecordList(selected);

    seriesDose = new SeriesDose();
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    // 4.4 has a current patient series whenever it hands control to 7.1. Nothing
    // in 7.1 reads it, but the no-skip destination it constructs on the way out
    // (7.2 Determine Evidence of Immunity) does, in its constructor.
    Antigen antigen = new Antigen();
    antigen.setName("Pneumococcal");
    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName("Pneumococcal 4 dose series");
    antigenSeries.setTargetDisease(antigen);
    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    dataModel.getPatientSeriesStepper().add(patientSeries);
    dataModel.getPatientSeriesStepper().increment();

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

  private ConditionalSkipSet set(String conditionLogic) {
    if (conditionalSkip == null) {
      conditionalSkip = new ConditionalSkip();
      conditionalSkip.setSetLogic("n/a");
      seriesDose.setConditionalSkip(conditionalSkip);
    }
    ConditionalSkipSet conditionalSkipSet = new ConditionalSkipSet();
    conditionalSkipSet.setSetId(conditionalSkip.getConditionalSkipSetList().size() + 1);
    conditionalSkipSet.setConditionLogic(conditionLogic);
    conditionalSkip.getConditionalSkipSetList().add(conditionalSkipSet);
    return conditionalSkipSet;
  }

  /** One set with condition logic "n/a" holding one condition of the given type. */
  private ConditionalSkipCondition soleCondition(ConditionalSkipConditionType conditionType) {
    ConditionalSkipSet conditionalSkipSet = set("n/a");
    ConditionalSkipCondition condition = new ConditionalSkipCondition(seriesDose);
    condition.setConditionId(conditionalSkipSet.getConditionList().size() + 1);
    condition.setConditionType(conditionType);
    conditionalSkipSet.getConditionList().add(condition);
    return condition;
  }

  private ConditionalSkipCondition ageCondition(String beginAge, String endAge) {
    ConditionalSkipCondition condition = soleCondition(ConditionalSkipConditionType.AGE);
    if (beginAge != null) {
      condition.setBeginAge(new TimePeriod(beginAge));
    }
    if (endAge != null) {
      condition.setEndAge(new TimePeriod(endAge));
    }
    return condition;
  }

  private ConditionalSkipCondition intervalCondition(String interval, String previousDoseAdministered) {
    ConditionalSkipCondition condition = soleCondition(ConditionalSkipConditionType.INTERVAL);
    condition.setInterval(new TimePeriod(interval));
    AntigenAdministeredRecord previous = new AntigenAdministeredRecord();
    previous.setDateAdministered(date(previousDoseAdministered));
    dataModel.setAntigenAdministeredRecordThatSatisfiedPreviousTargetDose(previous);
    return condition;
  }

  private LogicStep run() throws Exception {
    step = new EvaluateConditionalSkipForForecast(dataModel);
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  private ConditionAttribute<?> requiredAttribute(String attributeName) {
    for (ConditionAttribute<?> conditionAttribute : step.getConditionAttributeList()) {
      if (conditionAttribute != null
          && attributeName.equalsIgnoreCase(conditionAttribute.getAttributeName())) {
        return conditionAttribute;
      }
    }
    fail("Table 6-4 attribute '" + attributeName + "' is not registered by the step");
    return null;
  }

  private EvaluateConditionalSkip.LTInnerSet onlyConditionTable() {
    List<EvaluateConditionalSkip.LTInnerSet> tables = new ArrayList<EvaluateConditionalSkip.LTInnerSet>();
    for (LogicTable logicTable : step.getLogicTableList()) {
      if (logicTable instanceof EvaluateConditionalSkip.LTInnerSet) {
        tables.add((EvaluateConditionalSkip.LTInnerSet) logicTable);
      }
    }
    assertEquals("exactly one per-condition table was expected", 1, tables.size());
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

  /** The Conditional Skip Reference Date CONDSKIP-2 handed the only condition. */
  private Date referenceDate() {
    return onlyConditionTable().caConditionalSkipReferenceDate.getFinalValue();
  }

  /**
   * Labels are compared with whitespace and the punctuation the specification
   * and the implementation disagree about removed - "Table 6-4" against
   * "Table 6.4" is a transcription difference, not a behavioural one.
   */
  private static void assertLabelIs(String expected, String actual) {
    assertEquals("expected label '" + expected + "' but was '" + actual + "'",
        normalized(expected), normalized(actual));
  }

  private static String normalized(String label) {
    if (label == null) {
      return null;
    }
    return label.replaceAll("\\s+", "").replace(".", "").replace("-", "").toLowerCase();
  }

  // ================================== Entry: what 7.1's own class is, and how

  /**
   * 7.1's own class is the shared conditional-skip logic bound to the forecast
   * context: it is an {@link EvaluateConditionalSkip} - the same base class 6.2
   * drives - it identifies itself as
   * {@code EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST} / chapter "7.1", and it
   * names the same Table 6-4 attribute table, because 7.1 borrows chapter 6.2's
   * attribute table wholesale.
   */
  @Test
  public void theForecastStepIsTheSharedConditionalSkipLogicBoundToTheForecastContext()
      throws Exception {
    ageCondition("1 year", "4 years");

    run();

    assertTrue("7.1's class extends the same shared implementation 6.2 uses",
        step instanceof EvaluateConditionalSkip);
    assertEquals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, step.getLogicStepType());
    assertEquals("7.1", step.getLogicStepType().getChapter());
    assertLabelIs("Table 6-4 Conditional Skip Attributes", step.getConditionTableName());
  }

  /**
   * Entry conditions: 7.1 "runs as part of the forecast-dates-and-reasons
   * sequence (Chapter 7)". The engine never constructs the step directly - 4.4,
   * 6.1 and {@code ForecastDatesAndReasons} all hand
   * {@code EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST} to
   * {@link LogicStepFactory}, so what that factory builds for 7.1 is the step
   * this class tests. Both of the factory's overloads are checked, since 4.4's
   * dispatch and the web renderer's use different ones.
   */
  @Test
  public void theFactoryBuildsThisClassForStepSevenOne() {
    ageCondition("1 year", "4 years");

    LogicStep built = LogicStepFactory.createLogicStep(
        LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, dataModel);
    LogicStep builtAgain = LogicStepFactory.createLogicStep(
        LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, dataModel, true);

    assertTrue("7.1 is built as EvaluateConditionalSkipForForecast",
        built instanceof EvaluateConditionalSkipForForecast);
    assertTrue("7.1 is built as EvaluateConditionalSkipForForecast",
        builtAgain instanceof EvaluateConditionalSkipForForecast);
  }

  /**
   * 7.1's entry condition, the counterpart of 6.2's: "Only Conditional Skip
   * Instances with a context of Forecast or Both apply here." As at 6.2, the
   * base class carries a comment about eliminating instances by context but no
   * code implementing it, and it could not - {@code ConditionalSkip} has no
   * context at all and {@code SeriesDose} holds exactly one.
   *
   * <p>
   * From 7.1's side the consequence is different from 6.2's, and worth stating
   * precisely. In the Supporting Data release bundled with {@code cdsi-engine}
   * there are 264 real {@code <conditionalSkip>} elements (140 "Both", 57
   * "Evaluation", 67 "Forecast") spread over 197 series doses;
   * {@code DataModelLoader} calls {@code setConditionalSkip} once per element,
   * so the last one parsed is the one retained. 67 series doses define two: 54
   * as Evaluation-then-Forecast and 13 as Both-then-Forecast. In all 67 the
   * retained instance is the Forecast one - which is the instance 7.1 is
   * <em>supposed</em> to use. So where 6.2 loses its instance, 7.1 happens to
   * receive the right one, by the accident of document order rather than by any
   * filtering. The mirror-image failure is smaller but real: 3 series doses
   * define a single Evaluation-only conditional skip, and 7.1 applies it to the
   * forecast context even though its context excludes it.
   *
   * <p>
   * The gap is in the domain model and the loader rather than in
   * {@code EvaluateConditionalSkip}, so this test asks the smallest question
   * that can be asked of 7.1 in isolation: can its entry condition even be
   * expressed?
   */
  @Test
  public void aConditionalSkipInstanceCarriesTheContextThatDecidesWhetherSevenOneMayUseIt() {
    boolean representable = false;
    for (Method method : ConditionalSkip.class.getMethods()) {
      if (method.getName().toLowerCase().contains("context")) {
        representable = true;
      }
    }
    assertTrue("7.1 must use only Conditional Skip instances with a context of Forecast or Both, "
        + "but ConditionalSkip carries no context for it to read", representable);
  }

  // ========================================== CONDSKIP-2, the forecasting arm

  /**
   * CONDSKIP-2: "The Conditional Skip Reference Date must be one of the
   * following: the Date Administered of the vaccine dose administered when
   * evaluating a vaccine dose administered; <strong>the Assessment Date when
   * determining a forecast</strong>; the Earliest Date when validating a
   * forecast." This is the one place the shared class branches on its context,
   * and 7.1 is the forecasting arm.
   */
  @Test
  public void condskipTwoUsesTheAssessmentDateAsTheReferenceDateWhenForecasting() throws Exception {
    ageCondition("1 year", "10 years");

    run();

    assertEquals("CONDSKIP-2, forecasting: the reference date is the assessment date",
        date(ASSESSMENT), referenceDate());
    assertFalse("the date administered is the evaluating arm's reference date, not this one",
        date(ADMINISTERED).equals(referenceDate()));
  }

  /**
   * Table 6-4 gives the Assessment Date an assumed value - "if no assessment
   * date is supplied, the assessment date is assumed to be the current date."
   * Because 7.1's reference date <em>is</em> the assessment date, that assumed
   * value is what CONDSKIP-2 falls back to here; 6.2's arm never consults it.
   */
  @Test
  public void condskipTwoFallsBackToTheAssumedAssessmentDateWhenNoneIsSupplied() throws Exception {
    dataModel.setAssessmentDate(null);
    ageCondition("1 year", "10 years");

    run();

    assertNull("the fixture supplies no assessment date",
        requiredAttribute(ASSESSMENT_DATE).getInitialValue());
    assertNotNull("Table 6-4 assumes the current date when the assessment date is empty",
        requiredAttribute(ASSESSMENT_DATE).getAssumedValue());
    assertEquals("CONDSKIP-2 forecasting uses the assessment date's final value, assumed or not",
        requiredAttribute(ASSESSMENT_DATE).getFinalValue(), referenceDate());
  }

  /**
   * Forecasting happens after every vaccine dose administered has been
   * evaluated, so there need be no "current" vaccine dose administered when 7.1
   * runs. The forecasting arm of CONDSKIP-2 does not read one - had 7.1 been
   * bound to the evaluating arm the reference date would be null here and Table
   * 6-6 would answer "No" for want of a date rather than on the merits.
   */
  @Test
  public void theForecastReferenceDateDoesNotDependOnACurrentVaccineDoseAdministered()
      throws Exception {
    dataModel.setAntigenAdministeredRecord(null);
    ageCondition("5 years", "10 years");

    run();

    assertEquals("no current dose administered does not disturb the forecast reference date",
        date(ASSESSMENT), referenceDate());
    assertTrue("01/01/2020 <= 06/01/2021 < 01/01/2025", onlyConditionTable().isMet());
  }

  // ================= What the forecast reference date does to the skip decision

  /**
   * The substitution is not cosmetic: Table 6-6's age window is measured
   * against the assessment date when forecasting, so a target dose whose
   * conditional skip did not apply when the dose was evaluated can apply by the
   * time it is forecast, and vice versa. Both directions are pinned with the
   * same patient: born 01/01/2015, dose administered 06/01/2016, assessed
   * 06/01/2021.
   */
  @Test
  public void theAgeConditionIsAnsweredAgainstTheAssessmentDateNotTheDateAdministered()
      throws Exception {
    // 5 to 10 years -> 01/01/2020 to 01/01/2025: excludes the date administered,
    // includes the assessment date.
    ageCondition("5 years", "10 years");
    run();
    assertTrue("forecasting: 06/01/2021 is inside 01/01/2020 - 01/01/2025",
        onlyConditionTable().isMet());

    // 1 to 4 years -> 01/01/2016 to 01/01/2019: includes the date administered,
    // excludes the assessment date.
    setUp();
    ageCondition("1 year", "4 years");
    run();
    assertFalse("forecasting: 06/01/2021 is outside 01/01/2016 - 01/01/2019",
        onlyConditionTable().isMet());
  }

  /**
   * Table 6-8's second condition ("Is the Conditional Skip Reference Date &ge;
   * Conditional Skip Interval Date?") reads the same reference date, so an
   * interval condition is answered against the assessment date too. The
   * conditional skip interval date here - the previous dose 01/15/2016 plus
   * three years, 01/15/2019 - falls between the date administered and the
   * assessment date, so the condition is met when forecasting and would not
   * have been when evaluating.
   */
  @Test
  public void theIntervalConditionIsAnsweredAgainstTheAssessmentDateToo() throws Exception {
    intervalCondition("3 years", "01/15/2016");

    run();

    assertEquals(date(ASSESSMENT), referenceDate());
    assertEquals("CALCDTSKIP-5: 01/15/2016 + 3 years",
        date("01/15/2019"), onlyConditionTable().caConditionalSkipIntervalDate.getFinalValue());
    assertTrue("forecasting: 06/01/2021 has passed the interval date 01/15/2019",
        onlyConditionTable().isMet());
  }

  // ============================================= 7.1's own two destinations

  /**
   * Table 6-11's "Yes" outcome in the forecasting context: the target dose
   * status is 'Skipped' and control returns to 4.4 to move on to the next
   * target dose. This destination happens to be the same one 6.2 uses.
   */
  @Test
  public void skippingTheTargetDoseSetsItsStatusToSkippedAndReturnsToFourFour() throws Exception {
    ageCondition("5 years", "10 years");

    run();

    assertEquals(LogicResult.YES, conditionResult(tableSixEleven(), 0));
    assertEquals(TargetDoseStatus.SKIPPED, targetDose.getTargetDoseStatus());
    assertEquals("a skipped target dose returns to 4.4 Evaluate and Forecast All Patient Series",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());
  }

  /**
   * Table 6-11's "No" outcome in the forecasting context. This is where 7.1
   * differs from 6.2: forecasting continues at 7.2 Determine Evidence of
   * Immunity, where evaluation would have continued at 6.3 Evaluate for
   * Inadvertent Vaccine.
   */
  @Test
  public void notSkippingTheTargetDoseContinuesToSevenTwo() throws Exception {
    ageCondition("1 year", "4 years");

    run();

    assertEquals(LogicResult.NO, conditionResult(tableSixEleven(), 0));
    assertFalse("the target dose was not skipped",
        TargetDoseStatus.SKIPPED.equals(targetDose.getTargetDoseStatus()));
    assertEquals("forecasting continues at 7.2 Determine Evidence of Immunity",
        LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY, step.getNextLogicStepType());
    assertFalse("6.3 is the evaluation context's destination, not 7.1's",
        LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE.equals(step.getNextLogicStepType()));
  }

  /**
   * "In cases where a target dose does not specify Conditional Skip attributes,
   * the target dose cannot be skipped" - and forecasting proceeds to 7.2. No
   * decision table is built at all, so the no-skip destination the constructor
   * was given is the one {@code process()} leaves in place.
   */
  @Test
  public void aTargetDoseWithNoConditionalSkipAttributesContinuesToSevenTwo() throws Exception {
    assertNull("the fixture's series dose defines no conditional skip",
        seriesDose.getConditionalSkip());

    run();

    assertEquals("no conditional skip means no decision tables", 0, step.getLogicTableList().size());
    assertFalse("the target dose cannot be skipped",
        TargetDoseStatus.SKIPPED.equals(targetDose.getTargetDoseStatus()));
    assertEquals("forecasting continues at 7.2 Determine Evidence of Immunity",
        LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY, step.getNextLogicStepType());
  }
}
