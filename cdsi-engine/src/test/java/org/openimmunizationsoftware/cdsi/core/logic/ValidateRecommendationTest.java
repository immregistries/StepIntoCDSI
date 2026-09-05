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
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
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
 * Section 7.6 "Validate Recommendation" (Logic Specification for ACIP
 * Recommendations v4.6, page 83, Figure 7-9) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/07-06-validate-recommendation/index.md}.
 *
 * <h2>What 7.6 is, and what this class therefore tests</h2>
 *
 * <p>
 * 7.6 is the third and last reuse of the shared conditional-skip logic, after
 * 6.2 (evaluating) and 7.1 (forecasting). Its purpose is a prospective sanity
 * check on the forecast 7.5 has just produced: "Validate Recommendation
 * interrogates the forecasted earliest date to ensure the forecast makes
 * logical sense. Conditional Skip is used to determine if a forecast is
 * illogical and thus in need of a complete re-forecasting." Section 7.6.1 then
 * borrows chapter 6.2 wholesale - "The process model, attribute table, and
 * decision table ... is the same as described in Chapter 6.2" - and states the
 * three things that make this invocation its own:
 * <ol>
 * <li>"Only Conditional Skip Instances with a context of Forecast or Both
 * should be used";</li>
 * <li>"In cases where a target dose does not specify Conditional Skip
 * attributes, the target dose cannot be skipped";</li>
 * <li>"In CONDSKIP-2, the Earliest Date is used" - the reference date for this
 * invocation is the forecast's own earliest date, neither 6.2's Date
 * Administered nor 7.1's Assessment Date.</li>
 * </ol>
 *
 * <p>
 * Everything context-independent in the shared base class -
 * {@link EvaluateConditionalSkip}'s Table 6-4 attributes, CALCDTSKIP-3/-4/-5,
 * CONDSKIP-1, and the encoding of Tables 6-6 through 6-11 with their AND/OR
 * roll-ups - is already covered by {@code EvaluateConditionalSkipForEvaluationTest}
 * under unit 6.2. <strong>None of that is re-tested here.</strong> What is
 * tested here is the three points above, the shape of the step itself, and the
 * one thing 7.6 has that neither 6.2 nor 7.1 has: a
 * {@code process()} override.
 *
 * <h2>Isolating the override from the logic it bypasses</h2>
 *
 * <p>
 * {@link ValidateRecommendation} overrides {@code process()} so that
 * {@code evaluateLogicTables()} is never called. That single override would, on
 * its own, make every assertion about the decision tables fail, which would
 * hide whether the inherited logic is <em>also</em> mis-bound for this context.
 * So the tests below use two entry points deliberately:
 * {@link #run()} calls the real {@code process()} (what the engine actually
 * does), while {@link #evaluateInheritedTables()} constructs the step and calls
 * the inherited {@code evaluateLogicTables()} directly, which is what
 * {@code process()} would have called had it not been overridden. Tests of the
 * override use the first; tests of CONDSKIP-2 and the per-condition tables use
 * the second, so the two defects are reported separately rather than as one.
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads - no
 * Supporting Data release is loaded and no scenario is run through the engine.
 * The step's inner decision tables are {@code protected} inner classes and
 * {@code evaluateLogicTables()} is {@code protected}, so this test, in the same
 * package, reaches both directly.
 */
public class ValidateRecommendationTest {

  private static final String EARLIEST_DATE = "Earliest Date";

  /** The patient's date of birth in every fixture below. */
  private static final String BIRTH = "01/01/2015";
  /** The date administered - 6.2's reference date, and no one else's. */
  private static final String ADMINISTERED = "06/01/2016";
  /** The assessment date - 7.1's reference date, and no one else's. */
  private static final String ASSESSMENT = "06/01/2021";
  /** The forecasted earliest date - 7.6's reference date, per 7.6.1. */
  private static final String EARLIEST = "09/01/2023";

  /** 01/01/2016 - 01/01/2019: contains the date administered only. */
  private static final String[] WINDOW_AROUND_ADMINISTERED = { "1 year", "4 years" };
  /** 01/01/2020 - 01/01/2022: contains the assessment date only. */
  private static final String[] WINDOW_AROUND_ASSESSMENT = { "5 years", "7 years" };
  /** 01/01/2023 - 01/01/2025: contains the forecasted earliest date only. */
  private static final String[] WINDOW_AROUND_EARLIEST = { "8 years", "10 years" };

  private DataModel dataModel;
  private Patient patient;
  private SeriesDose seriesDose;
  private TargetDose targetDose;
  private Forecast forecast;
  private ConditionalSkip conditionalSkip;
  private ValidateRecommendation step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    patient = new Patient();
    patient.setDateOfBirth(date(BIRTH));
    dataModel.setPatient(patient);

    dataModel.setImmunizationHistory(new ImmunizationHistory());
    dataModel.setAssessmentDate(date(ASSESSMENT));

    AntigenAdministeredRecord antigenAdministeredRecord = new AntigenAdministeredRecord();
    antigenAdministeredRecord.setDateAdministered(date(ADMINISTERED));
    dataModel.setAntigenAdministeredRecord(antigenAdministeredRecord);
    List<AntigenAdministeredRecord> selected = new ArrayList<AntigenAdministeredRecord>();
    selected.add(antigenAdministeredRecord);
    dataModel.setSelectedAntigenAdministeredRecordList(selected);

    seriesDose = new SeriesDose();
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    // 7.6 only ever runs on a forecast 7.5 has just produced, and 7.5 leaves that
    // forecast on the data model and on the current patient series.
    Antigen antigen = new Antigen();
    antigen.setName("Hib");
    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName("Hib 4 dose series");
    antigenSeries.setTargetDisease(antigen);
    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    dataModel.getPatientSeriesStepper().add(patientSeries);
    dataModel.getPatientSeriesStepper().increment();

    forecast = new Forecast();
    forecast.setAntigen(antigen);
    forecast.setTargetDose(targetDose);
    forecast.setAssessmentDate(date(ASSESSMENT));
    forecast.setEarliestDate(date(EARLIEST));
    dataModel.setForecast(forecast);
    dataModel.getForecastList().add(forecast);
    patientSeries.setForecast(forecast);

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

  /** One set with condition logic "n/a" holding one condition of the given type. */
  private ConditionalSkipCondition soleCondition(ConditionalSkipConditionType conditionType) {
    if (conditionalSkip == null) {
      conditionalSkip = new ConditionalSkip();
      conditionalSkip.setSetLogic("n/a");
      seriesDose.setConditionalSkip(conditionalSkip);
    }
    ConditionalSkipSet conditionalSkipSet = new ConditionalSkipSet();
    conditionalSkipSet.setSetId(conditionalSkip.getConditionalSkipSetList().size() + 1);
    conditionalSkipSet.setConditionLogic("n/a");
    conditionalSkip.getConditionalSkipSetList().add(conditionalSkipSet);

    ConditionalSkipCondition condition = new ConditionalSkipCondition(seriesDose);
    condition.setConditionId(1);
    condition.setConditionType(conditionType);
    conditionalSkipSet.getConditionList().add(condition);
    return condition;
  }

  private ConditionalSkipCondition ageCondition(String beginAge, String endAge) {
    ConditionalSkipCondition condition = soleCondition(ConditionalSkipConditionType.AGE);
    condition.setBeginAge(new TimePeriod(beginAge));
    condition.setEndAge(new TimePeriod(endAge));
    return condition;
  }

  private ConditionalSkipCondition ageCondition(String[] window) {
    return ageCondition(window[0], window[1]);
  }

  private ConditionalSkipCondition intervalCondition(String interval, String previousDoseAdministered) {
    ConditionalSkipCondition condition = soleCondition(ConditionalSkipConditionType.INTERVAL);
    condition.setInterval(new TimePeriod(interval));
    AntigenAdministeredRecord previous = new AntigenAdministeredRecord();
    previous.setDateAdministered(date(previousDoseAdministered));
    dataModel.setAntigenAdministeredRecordThatSatisfiedPreviousTargetDose(previous);
    return condition;
  }

  // ------------------------------------------------------ driving the step

  private ValidateRecommendation build() {
    step = new ValidateRecommendation(dataModel);
    return step;
  }

  /** What the engine actually does: the overridden {@code process()}. */
  private LogicStep run() throws Exception {
    return build().process();
  }

  /**
   * What 7.6.1 says should happen: the inherited conditional-skip decision
   * tables actually being evaluated. Called directly so that tests of the
   * borrowed logic are not all masked by the {@code process()} override, which
   * has its own tests below.
   */
  private ValidateRecommendation evaluateInheritedTables() {
    build();
    step.evaluateLogicTables();
    return step;
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

  // ================================ What 7.6's own class is, and how it is built

  /**
   * 7.6.1: the process model, attribute table and decision table are "the same
   * as described in Chapter 6.2". The class is therefore the shared
   * {@link EvaluateConditionalSkip} bound to a third context, it publishes
   * {@code VALIDATE_RECOMMENDATION} / chapter "7.6", and it names the same
   * Table 6-4 attribute table 6.2 and 7.1 do.
   */
  @Test
  public void sevenSixIsTheSharedConditionalSkipLogicBoundToTheValidatingContext() throws Exception {
    ageCondition(WINDOW_AROUND_EARLIEST);

    build();

    assertTrue("7.6's class extends the same shared implementation 6.2 and 7.1 use",
        step instanceof EvaluateConditionalSkip);
    assertEquals(LogicStepType.VALIDATE_RECOMMENDATION, step.getLogicStepType());
    assertEquals("7.6", step.getLogicStepType().getChapter());
    assertLabelIs("Table 6-4 Conditional Skip Attributes", step.getConditionTableName());
  }

  /**
   * The engine never constructs 7.6 directly: 7.5 ends by setting
   * {@code VALIDATE_RECOMMENDATION} and calling {@code next()}, which routes
   * through {@link LogicStepFactory}. Both of the factory's overloads are
   * checked, since the engine's dispatch and the web renderer use different
   * ones.
   */
  @Test
  public void theFactoryBuildsThisClassForStepSevenSix() {
    ageCondition(WINDOW_AROUND_EARLIEST);

    LogicStep built = LogicStepFactory.createLogicStep(
        LogicStepType.VALIDATE_RECOMMENDATION, dataModel);
    LogicStep builtAgain = LogicStepFactory.createLogicStep(
        LogicStepType.VALIDATE_RECOMMENDATION, dataModel, true);

    assertTrue("7.6 is built as ValidateRecommendation", built instanceof ValidateRecommendation);
    assertTrue("7.6 is built as ValidateRecommendation", builtAgain instanceof ValidateRecommendation);
  }

  /**
   * The two destinations 7.6's constructor hands the shared base class, which
   * are what the specification's two outcomes require: an invalid
   * recommendation needs "a complete re-forecasting", which is the
   * conditional-skip entry point of the forecasting chapter (7.1); a valid one
   * returns to the per-series driver (4.4). This test asserts only that the
   * wiring is present. Whether {@code process()} ever reaches it is a separate
   * question, asked further down.
   */
  @Test
  public void theConstructorWiresReForecastingOnSkipAndFourFourOnNoSkip() {
    ageCondition(WINDOW_AROUND_EARLIEST);

    build();

    assertEquals("an illogical forecast is re-forecast from 7.1",
        LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, step.skipLogicStep);
    assertEquals("a valid recommendation returns to 4.4",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.noSkipLogicStep);
  }

  // ================================================== 7.6.1's entry conditions

  /**
   * 7.6.1: "Only Conditional Skip Instances with a context of Forecast or Both
   * should be used." As at 6.2 and 7.1, the shared base class carries a comment
   * about eliminating instances by context but no code implementing it, and it
   * could not: {@code ConditionalSkip} has no context field at all and
   * {@code SeriesDose} holds exactly one instance, so
   * {@code DataModelLoader}'s repeated {@code setConditionalSkip} calls keep
   * only the last one parsed.
   *
   * <p>
   * 7.6 shares 7.1's context filter exactly ("Forecast or Both"), so it
   * inherits 7.1's version of the consequence rather than 6.2's: in the bundled
   * 4.65-508 release all 67 series doses that define two instances retain the
   * Forecast one - the one 7.6 is supposed to use - by the accident of document
   * order, while the 3 series doses defining a single Evaluation-only instance
   * are applied here even though their context excludes them. This is recorded
   * in {@code cdsi-reference/step-tests/cross-cutting-notes.md}; the test asks
   * the smallest question that can be asked of 7.6 in isolation - can its entry
   * condition even be expressed?
   */
  @Test
  public void aConditionalSkipInstanceCarriesTheContextThatDecidesWhetherSevenSixMayUseIt() {
    boolean representable = false;
    for (Method method : ConditionalSkip.class.getMethods()) {
      if (method.getName().toLowerCase().contains("context")) {
        representable = true;
      }
    }
    assertTrue("7.6 must use only Conditional Skip instances with a context of Forecast or Both, "
        + "but ConditionalSkip carries no context for it to read", representable);
  }

  /**
   * 7.6.1: "In cases where a target dose does not specify Conditional Skip
   * attributes, the target dose cannot be skipped." No decision table is built
   * at all, the forecast stands, and control returns to 4.4.
   */
  @Test
  public void aTargetDoseWithNoConditionalSkipAttributesCannotBeSkipped() throws Exception {
    assertNull("the fixture's series dose defines no conditional skip",
        seriesDose.getConditionalSkip());

    run();

    assertEquals("no conditional skip means no decision tables", 0, step.getLogicTableList().size());
    assertFalse("the target dose cannot be skipped",
        TargetDoseStatus.SKIPPED.equals(targetDose.getTargetDoseStatus()));
    assertEquals("the recommendation stands and control returns to 4.4",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());
  }

  // ========================================== CONDSKIP-2, the validating arm

  /**
   * 7.6.1: "In CONDSKIP-2, the Earliest Date is used." CONDSKIP-2 itself gives
   * three alternatives - "the Date Administered of the vaccine dose
   * administered when evaluating a vaccine dose administered; the Assessment
   * Date when determining a forecast; <strong>the Earliest Date when validating
   * a forecast</strong>" - and 7.6 is the validating arm, the only one of the
   * three that reads the forecast this step exists to interrogate.
   *
   * <p>
   * The fixture keeps all three candidate dates distinct so the assertion
   * cannot pass by coincidence: administered 06/01/2016, assessed 06/01/2021,
   * earliest 09/01/2023.
   */
  @Test
  public void condskipTwoUsesTheForecastedEarliestDateAsTheReferenceDateWhenValidating() {
    ageCondition(WINDOW_AROUND_EARLIEST);

    evaluateInheritedTables();

    assertEquals("CONDSKIP-2, validating: the reference date is the forecast's earliest date",
        date(EARLIEST), referenceDate());
  }

  /**
   * Table 6-4 lists the Earliest Date among the conditional skip attributes,
   * and 7.6 is the one context in which it carries a value. The shared base
   * class declares {@code caEarliestDate} and adds it to the attribute list,
   * but never constructs it and never gives it the forecast's earliest date, so
   * the attribute the whole section turns on is a null entry in the list rather
   * than an attribute with a source and a value.
   */
  @Test
  public void theEarliestDateIsATableSixFourAttributeTheStepRegisters() {
    ageCondition(WINDOW_AROUND_EARLIEST);

    build();

    ConditionAttribute<?> earliestDate = requiredAttribute(EARLIEST_DATE);
    assertNotNull("Table 6-4's Earliest Date attribute must be registered", earliestDate);
    assertEquals("it carries the earliest date of the forecast being validated",
        date(EARLIEST), earliestDate.getFinalValue());
  }

  // ============= What the earliest date does to the borrowed decision tables

  /**
   * The substitution is the point of the section, not a detail: Table 6-6's age
   * window has to be measured against the forecasted earliest date, because the
   * question 7.6 asks is whether the dose would still make sense
   * <em>on the day the patient is being told to come back</em>. All three
   * candidate reference dates are exercised against the same condition, so the
   * test distinguishes "the earliest date was used" from "some date happened to
   * fall in the window".
   */
  @Test
  public void theAgeConditionIsAnsweredAgainstTheForecastedEarliestDate() {
    // 01/01/2023 - 01/01/2025 contains the earliest date 09/01/2023 and neither
    // of the other two candidates.
    ageCondition(WINDOW_AROUND_EARLIEST);
    evaluateInheritedTables();
    assertTrue("validating: 09/01/2023 is inside 01/01/2023 - 01/01/2025",
        onlyConditionTable().isMet());

    // 01/01/2020 - 01/01/2022 contains the assessment date, which is 7.1's
    // reference date, not 7.6's.
    setUp();
    ageCondition(WINDOW_AROUND_ASSESSMENT);
    evaluateInheritedTables();
    assertFalse("the assessment date is the forecasting arm's reference date, not this one",
        onlyConditionTable().isMet());

    // 01/01/2016 - 01/01/2019 contains the date administered, which is 6.2's.
    setUp();
    ageCondition(WINDOW_AROUND_ADMINISTERED);
    evaluateInheritedTables();
    assertFalse("the date administered is the evaluating arm's reference date, not this one",
        onlyConditionTable().isMet());
  }

  /**
   * Table 6-8's second condition ("Is the Conditional Skip Reference Date &ge;
   * Conditional Skip Interval Date?") reads the same reference date, so an
   * interval condition is answered against the earliest date too. The interval
   * date here - the previous dose 01/15/2020 plus three years, 01/15/2023 -
   * falls after the assessment date and before the earliest date, so the
   * condition is met when validating and would not have been when forecasting.
   */
  @Test
  public void theIntervalConditionIsAnsweredAgainstTheForecastedEarliestDateToo() {
    intervalCondition("3 years", "01/15/2020");

    evaluateInheritedTables();

    assertEquals("CALCDTSKIP-5: 01/15/2020 + 3 years",
        date("01/15/2023"), onlyConditionTable().caConditionalSkipIntervalDate.getFinalValue());
    assertEquals(date(EARLIEST), referenceDate());
    assertTrue("validating: the earliest date 09/01/2023 has passed the interval date 01/15/2023",
        onlyConditionTable().isMet());
  }

  /**
   * The negative control for the section's Table 6-11 roll-up in this context:
   * a conditional skip whose window does not contain the forecasted earliest
   * date leaves the recommendation valid - the set is not met, Table 6-11
   * answers "No", and the target dose is not skipped.
   *
   * <p>
   * Note for whoever runs Role B: this test is green today, but it would also
   * be green with the CONDSKIP-2 defect in place, because the reference date
   * the class actually uses (01/01/1900) is outside this window too. It pins
   * the roll-up's "No" arm, not the reference date; the reference date is
   * pinned by the two tests above it, which fail.
   */
  @Test
  public void aConditionalSkipWhoseWindowExcludesTheEarliestDateLeavesTheDoseUnskipped() {
    ageCondition(WINDOW_AROUND_ADMINISTERED);

    evaluateInheritedTables();

    assertFalse("01/01/2016 - 01/01/2019 does not contain 09/01/2023",
        onlyConditionTable().isMet());
    assertEquals(LogicResult.NO, conditionResult(tableSixEleven(), 0));
    assertFalse("the target dose is not skipped",
        TargetDoseStatus.SKIPPED.equals(targetDose.getTargetDoseStatus()));
  }

  // =========================== The section's purpose: does the check ever run?

  /**
   * The Purpose, in the specification's own words: "Conditional Skip is used to
   * determine if a forecast is illogical". For that to be true of this class,
   * {@code process()} has to evaluate the decision tables it inherits - which
   * is what the base class's {@code process()} does for 6.2 and 7.1. Here it is
   * overridden to set the next step and return, so no condition of Table 6-11
   * is ever asked.
   */
  @Test
  public void processEvaluatesTheInheritedConditionalSkipDecisionTables() throws Exception {
    ageCondition(WINDOW_AROUND_EARLIEST);

    run();

    assertNotNull("7.6 must evaluate Table 6-11 - it is how a forecast is judged illogical",
        conditionResult(tableSixEleven(), 0));
  }

  /**
   * Table 6-11's "Yes" outcome in the validating context: the shared logic
   * records the target dose as Skipped. The specification's State Change for a
   * failed validation is that the recommendation is discarded and re-forecast,
   * and Skipped is how the shared table expresses "this dose need not be given
   * at the date under test".
   */
  @Test
  public void anIllogicalRecommendationSetsTheTargetDoseSkipped() throws Exception {
    ageCondition(WINDOW_AROUND_EARLIEST);

    run();

    assertEquals("the forecast is illogical at its own earliest date",
        TargetDoseStatus.SKIPPED, targetDose.getTargetDoseStatus());
  }

  /**
   * "If the recommendation is found to be invalid, re-forecasting for the next
   * target dose is required." The constructor already names the destination
   * that does that - 7.1, the forecasting chapter's conditional-skip entry
   * point - so the assertion is that an invalid recommendation actually reaches
   * it.
   */
  @Test
  public void anIllogicalRecommendationTriggersReForecasting() throws Exception {
    ageCondition(WINDOW_AROUND_EARLIEST);

    run();

    assertEquals("an invalid recommendation requires a complete re-forecasting",
        LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, step.getNextLogicStepType());
  }

  /**
   * The other half of the same decision: a recommendation that still makes
   * sense at its own earliest date is returned unchanged and control goes back
   * to 4.4 for the next target dose.
   */
  @Test
  public void aValidRecommendationContinuesToFourFour() throws Exception {
    ageCondition(WINDOW_AROUND_ADMINISTERED);

    run();

    assertFalse("the recommendation is valid, so the dose is not skipped",
        TargetDoseStatus.SKIPPED.equals(targetDose.getTargetDoseStatus()));
    assertEquals(date(EARLIEST), forecast.getEarliestDate());
    assertEquals("a valid recommendation returns to 4.4",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());
  }

  /**
   * The specification's own worked example for this section, asserted as a
   * scenario rather than as a rule: "a patient is behind on Hib and has just
   * received a first dose at 11 months and 1 week of age. The patient is then
   * recommended for a catch-up dose in four weeks, shortly after the 12-month
   * mark. However, upon returning to the provider office four weeks later, the
   * freshly updated forecast skips the previously forecasted target dose."
   *
   * <p>
   * Born 01/01/2024; first dose 12/08/2024 (11 months 1 week); earliest date
   * 01/05/2025, four weeks later and four days past the 12-month mark. The
   * target dose carries a conditional skip that opens at 12 months, so at
   * 12/08/2024 it does not apply and at 01/05/2025 it does. That is precisely
   * the "forecast that will already be wrong by the time it matters" case 7.6
   * exists to catch, so the recommendation must be reported invalid and sent
   * back for re-forecasting.
   */
  @Test
  public void theSpecificationsHibExampleIsCaughtAsAnIllogicalForecast() throws Exception {
    patient.setDateOfBirth(date("01/01/2024"));
    AntigenAdministeredRecord firstDose = new AntigenAdministeredRecord();
    firstDose.setDateAdministered(date("12/08/2024"));
    dataModel.setAntigenAdministeredRecord(firstDose);
    List<AntigenAdministeredRecord> selected = new ArrayList<AntigenAdministeredRecord>();
    selected.add(firstDose);
    dataModel.setSelectedAntigenAdministeredRecordList(selected);
    dataModel.setAssessmentDate(date("12/08/2024"));
    forecast.setEarliestDate(date("01/05/2025"));
    ageCondition("12 months", "5 years");

    run();

    assertEquals("the catch-up dose would already be skippable on the date it is recommended for",
        TargetDoseStatus.SKIPPED, targetDose.getTargetDoseStatus());
    assertEquals("so the forecast is illogical and a complete re-forecasting is required",
        LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, step.getNextLogicStepType());
  }
}
