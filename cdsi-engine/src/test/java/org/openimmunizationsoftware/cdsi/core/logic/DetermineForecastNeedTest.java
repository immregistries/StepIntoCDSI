package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication_TO_BE_REMOVED;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeasonalRecommendation;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * Section 7.4 "Determine Forecast Need" (Logic Specification for ACIP
 * Recommendations v4.6, pages 78-80) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/07-04-determine-forecast-need/index.md}.
 *
 * <p>
 * 7.4 is the gate in front of the forecast: "determine forecast need determines
 * if there is a need to forecast dates ... This is a prerequisite before a CDS
 * engine can produce forecast dates and reasons." It is made of Table 7-9
 * (Determine Forecast Need Attributes), Table 7-10 (Should the Patient Receive
 * Another Target Dose? - seven conditions by eight rules) and Table 7-11's two
 * business rules, CALCDTAGE-1 and FORECASTDTCAN-1.
 *
 * <h2>The seven stop conditions</h2>
 *
 * <p>
 * Per the step package's walkthrough: "Seven independent conditions, each
 * capable of ending the process for this series on their own: dose statuses that
 * already answer the question (all satisfied = complete, one still open = keep
 * going), immunity, contraindication, a missed vaccination season, or simple old
 * age (either the patient is already past the cutoff, or the earliest they could
 * get the dose is past the cutoff, so there's no point forecasting a date that
 * can never be valid). Only when none of those seven 'stop' conditions apply
 * does the engine proceed to actually compute a date, in 7.5."
 *
 * <p>
 * Each of Table 7-10's eight rules sets a {@link PatientSeriesStatus} and - for
 * rules 2 through 8 - a forecast reason, and only Rule 1 continues to 7.5; the
 * other seven loop back to 4.4.
 *
 * <h2>Reading 7.2's and 7.3's outcomes</h2>
 *
 * <p>
 * Table 7-9 lists "the outcome of 7.2" (Evidence of Immunity) and "the outcome
 * of 7.3" (Contraindicated Patient Series) as inputs to this section. Both of
 * those outcomes are patient-series statuses ({@code IMMUNE},
 * {@code CONTRAINDICATED}) that neither 7.2 nor 7.3 can currently produce - see
 * {@code cdsi-reference/step-tests/cross-cutting-notes.md}. That upstream gap is
 * deliberately bypassed here: every test below sets the status it needs directly
 * on the hand-built {@link PatientSeries}, so what is under test is 7.4's own
 * consumption of those statuses, not whether 7.2/7.3 can produce them.
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} 7.4's constructor reads:
 * a target dose whose tracked series dose carries an {@link Age} (both
 * {@code computeEarliestDate()} and {@code findMaximumAgeDate()} dereference
 * {@code getAgeList().get(0)}), a patient with a date of birth, an assessment
 * date, a current patient series, and a {@link Forecast} for the series' target
 * disease so the forecast reasons the outcomes write have somewhere to land.
 *
 * <p>
 * {@code process()} is not called: it ends in {@code next()}, which constructs
 * whichever step comes next. Instead {@link #run()} does exactly what
 * {@code process()} does before that point - seed 7.5 as the default next step,
 * then evaluate Table 7-10 - so the step under test stays isolated. The decision
 * table is a {@code private} inner class, so it is reached through the
 * {@link LogicTable} base type from {@code getLogicTableList()}.
 */
public class DetermineForecastNeedTest {

  private static final String DATE_OF_BIRTH = "01/15/2015";
  private static final String ASSESSMENT_DATE = "06/15/2025";

  /** Table 7-9: Seasonal Recommendation End Date, "Assumed Value if Empty". */
  private static final String ASSUMED_SEASONAL_RECOMMENDATION_END_DATE = "12/31/2999";

  // ---- The forecast reasons Table 7-10's outcomes 2 through 8 write.

  private static final String REASON_COMPLETE = "Patient series is complete";
  private static final String REASON_PAST_HISTORY =
      "Not recommended at this time due to past immunization history";
  private static final String REASON_IMMUNE = "Patient has evidence of immunity";
  private static final String REASON_CONTRAINDICATED = "Patient has contraindication";
  private static final String REASON_SEASON_ENDED = "Past seasonal recommendation end date";
  private static final String REASON_PAST_MAXIMUM_AGE = "Patient has exceeded the maximum age";
  private static final String REASON_CANNOT_FINISH =
      "Patient is unable to finish the series prior to the maximum age";

  private DataModel dataModel;
  private Patient patient;
  private Antigen measles;
  private AntigenSeries antigenSeries;
  private SeriesDose seriesDose;
  private Age age;
  private PatientSeries patientSeries;
  private TargetDose targetDose;
  private Forecast forecast;
  private DetermineForecastNeed step;

  /**
   * The benign fixture: a ten-year-old with one still-open target dose, no
   * immunity, no contraindication, no seasonal recommendation and no maximum age.
   * All seven of Table 7-10's conditions answer the way Rule 1 needs them to, so
   * the unmodified fixture is the "yes, forecast a date" case; every other test
   * perturbs exactly one condition.
   */
  @Before
  public void setUp() {
    dataModel = new DataModel();

    patient = new Patient();
    patient.setDateOfBirth(date(DATE_OF_BIRTH));
    patient.getMedicalHistory().setImmunizationHistory(new ImmunizationHistory());
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date(ASSESSMENT_DATE));

    measles = new Antigen();
    measles.setName("Measles");

    antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName("Measles 2 dose series");
    antigenSeries.setTargetDisease(measles);

    seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("1");
    seriesDose.setAntigenSeries(antigenSeries);
    age = new Age();
    age.setSeriesDose(seriesDose);
    age.setMinimugeAge(new TimePeriod("0 days"));
    // An empty TimePeriod is the "no maximum age" case: Table 7-9's Maximum Age
    // Date then falls back to its assumed value, and nobody ages out.
    age.setMaximumAge(new TimePeriod(""));
    seriesDose.getAgeList().add(age);
    antigenSeries.getSeriesDoseList().add(seriesDose);

    targetDose = new TargetDose(seriesDose);
    targetDose.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
    dataModel.setTargetDose(targetDose);
    dataModel.setTargetDoseList(new ArrayList<TargetDose>());
    dataModel.getTargetDoseList().add(targetDose);

    patientSeries = new PatientSeries(antigenSeries);
    dataModel.getPatientSeriesStepper().add(patientSeries);
    dataModel.getPatientSeriesStepper().increment();

    forecast = new Forecast();
    forecast.setAntigen(measles);
    dataModel.getForecastList().add(forecast);

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

  private DetermineForecastNeed build() {
    step = new DetermineForecastNeed(dataModel);
    return step;
  }

  /**
   * What {@code DetermineForecastNeed.process()} does up to (but not including)
   * {@code next()}: 7.5 is the default next step, and only Table 7-10's outcomes
   * redirect it to 4.4.
   */
  private DetermineForecastNeed run() {
    build();
    step.setNextLogicStepType(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES);
    step.getLogicTableList().get(0).evaluate();
    return step;
  }

  /** Gives the series dose a real maximum age, so CALCDTAGE-1 has something to compute. */
  private void maximumAge(String timePeriod) {
    age.setMaximumAge(new TimePeriod(timePeriod));
  }

  private void minimumAge(String timePeriod) {
    age.setMinimugeAge(new TimePeriod(timePeriod));
  }

  /** Gives the series dose a seasonal recommendation window. */
  private SeasonalRecommendation seasonalRecommendation(String startDate, String endDate) {
    SeasonalRecommendation seasonalRecommendation = new SeasonalRecommendation();
    seasonalRecommendation.setSeriesDose(seriesDose);
    if (startDate != null) {
      seasonalRecommendation.setSeasonalRecommendationStartDate(date(startDate));
    }
    if (endDate != null) {
      seasonalRecommendation.setSeasonalRecommendationEndDate(date(endDate));
    }
    seriesDose.getSeasonalRecommendationList().add(seasonalRecommendation);
    return seasonalRecommendation;
  }

  private void everyTargetDose(TargetDoseStatus targetDoseStatus) {
    for (TargetDose each : dataModel.getTargetDoseList()) {
      each.setTargetDoseStatus(targetDoseStatus);
    }
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

  private Object finalValueOf(String attributeName) {
    ConditionAttribute<?> conditionAttribute = attribute(attributeName);
    assertNotNull("Table 7-9 lists an attribute named '" + attributeName + "'", conditionAttribute);
    return conditionAttribute.getFinalValue();
  }

  private Date candidateEarliestDate() {
    return (Date) finalValueOf("Candidate Earliest Date");
  }

  private LogicTable table(String tableNumber) {
    for (LogicTable logicTable : step.getLogicTableList()) {
      if (logicTable.getLabel() != null
          && normalized(logicTable.getLabel()).contains(normalized(tableNumber))) {
        return logicTable;
      }
    }
    fail("7.4 must build " + tableNumber + ", but the step has "
        + step.getLogicTableList().size() + " decision table(s)");
    return null;
  }

  private static String normalized(String label) {
    if (label == null) {
      return null;
    }
    return label.replaceAll("\\s+", "").replace(".", "").replace("-", "").replace("?", "")
        .replace(":", "").toLowerCase();
  }

  private static void assertLabelIs(String expected, String actual) {
    assertEquals("expected label '" + expected + "' but was '" + actual + "'",
        normalized(expected), normalized(actual));
  }

  private void assertStatusIs(PatientSeriesStatus expected, String because) {
    assertEquals(because, expected, patientSeries.getPatientSeriesStatus());
  }

  private void assertForecastReasonIs(String expected, String because) {
    assertEquals(because, expected, forecast.getForecastReason());
  }

  private void assertLoopsBackToFourFour(String because) {
    assertEquals(because, LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES,
        step.getNextLogicStepType());
  }

  // ================================================= What 7.4's own class is

  /**
   * 7.4's identity: {@code LogicStepFactory} is how the engine reaches it (7.3
   * hands it {@code DETERMINE_FORECAST_NEED}), it publishes chapter "7.4", and it
   * names Table 7-9 as its attribute table. Both factory overloads are checked,
   * since the pipeline's dispatch and the web renderer's use different ones.
   */
  @Test
  public void theFactoryBuildsThisClassForStepSevenFour() {
    LogicStep built =
        LogicStepFactory.createLogicStep(LogicStepType.DETERMINE_FORECAST_NEED, dataModel);
    LogicStep builtAgain =
        LogicStepFactory.createLogicStep(LogicStepType.DETERMINE_FORECAST_NEED, dataModel, true);

    assertTrue("7.4 is built as DetermineForecastNeed", built instanceof DetermineForecastNeed);
    assertTrue("7.4 is built as DetermineForecastNeed",
        builtAgain instanceof DetermineForecastNeed);
    assertEquals("7.4", built.getLogicStepType().getChapter());
    assertLabelIs("Table 7-9 Determine Forecast Need Attributes", built.getConditionTableName());
  }

  /**
   * Table 7-9 Determine Forecast Need Attributes prints eight attributes: the
   * immunization history's Vaccine Dose(s) Administered, the relevant patient
   * series' Target Dose Statuses, the Supporting Data Seasonal Recommendation End
   * Date, the outcome of 7.2 (Evidence of Immunity), the outcome of 7.3
   * (Contraindicated Patient Series), the runtime Assessment Date, and the two
   * calculated dates CALCDTAGE-1 and FORECASTDTCAN-1 produce. All eight are
   * inputs to Table 7-10, so all eight belong on the step's attribute list - that
   * list is the printed table.
   */
  @Test
  public void tableSevenNineRegistersEveryAttributeItPrints() {
    build();

    assertNotNull("Table 7-9: Immunization history / Vaccine Dose(s) Administered",
        attribute("Vaccine Dose(s) Administered"));
    assertNotNull("Table 7-9: Relevant Patient series / Target Dose Statuses",
        attribute("Target Dose Statuses"));
    assertNotNull("Table 7-9: Supporting Data / Seasonal Recommendation End Date",
        attribute("Seasonal Recommendation End Date"));
    assertNotNull("Table 7-9: Section 7.2 Outcome / Evidence of Immunity",
        attribute("Evidence of Immunity"));
    assertNotNull("Table 7-9: Section 7.3 Outcome / Contraindicated Patient Series",
        attribute("Contraindicated Patient Series"));
    assertNotNull("Table 7-9: Runtime data / Assessment Date", attribute("Assessment Date"));
    assertNotNull("Table 7-9: Calculated date (CALCDTAGE-1) / Maximum Age Date",
        attribute("Maximum Age Date"));
    assertNotNull("Table 7-9: Calculated date (FORECASTDTCAN-1) / Candidate Earliest Date",
        attribute("Candidate Earliest Date"));
  }

  /**
   * Table 7-9 gives the Seasonal Recommendation End Date the assumed value
   * <strong>12/31/2999</strong>. That is what makes Table 7-10's fifth condition
   * ("is the assessment date &le; the seasonal recommendation end date?") answer
   * Yes for the great majority of series doses, which define no seasonal
   * recommendation at all: a series with no season never falls out of season.
   */
  @Test
  public void tableSevenNinesAssumedSeasonalRecommendationEndDateIsInTheFuture() {
    build();

    assertEquals("Table 7-9: Seasonal Recommendation End Date, Assumed Value if Empty",
        date(ASSUMED_SEASONAL_RECOMMENDATION_END_DATE),
        attribute("Seasonal Recommendation End Date").getAssumedValue());
  }

  /**
   * Table 7-9 sources the Assessment Date from the runtime data, and the step is
   * handed a real one, so the attribute must carry the assessment date under
   * which this patient is being evaluated - Table 7-10's fifth and sixth
   * conditions both compare against it.
   */
  @Test
  public void tableSevenNinesAssessmentDateIsTheAssessmentDate() {
    build();

    assertEquals("Table 7-9: Runtime data / Assessment Date", date(ASSESSMENT_DATE),
        finalValueOf("Assessment Date"));
  }

  /**
   * Table 7-9 lists the outcome of 7.2 as an input to this section, with the
   * assumed value "no evidence [of immunity]". An attribute with an assumed value
   * always has a value: either the outcome 7.2 actually produced, or - when 7.2
   * produced nothing - the assumption the table prints. Table 7-10's third
   * condition is a question about this attribute, so it cannot be answered from
   * an attribute that is never given any value at all.
   */
  @Test
  public void theSevenTwoOutcomeAttributeCarriesItsAssumedNoEvidenceValue() {
    build();

    assertNotNull("Table 7-9 gives Evidence of Immunity the assumed value 'no evidence', so the "
        + "attribute must carry a value even when 7.2 produced none",
        finalValueOf("Evidence of Immunity"));
  }

  /**
   * The same for the outcome of 7.3, whose Table 7-9 assumed value is "not
   * contraindicated". Table 7-10's fourth condition ("is the relevant patient
   * series a contraindicated patient series?") is a question about this
   * attribute.
   */
  @Test
  public void theSevenThreeOutcomeAttributeCarriesItsAssumedNotContraindicatedValue() {
    build();

    assertNotNull("Table 7-9 gives Contraindicated Patient Series the assumed value 'not "
        + "contraindicated', so the attribute must carry a value even when 7.3 produced none",
        finalValueOf("Contraindicated Patient Series"));
  }

  // ============================================ Table 7-11, the business rules

  /**
   * <strong>CALCDTAGE-1.</strong> "The patient's maximum age date must be
   * calculated as the patient's date of birth plus the maximum age." This is the
   * same rule 6.4 uses; here it is 7.4's own Maximum Age Date attribute, the
   * input to Table 7-10's sixth and seventh conditions. A patient born 01/15/2015
   * whose series dose has a maximum age of "12 years" ages out on 01/15/2027.
   */
  @Test
  public void calcdtageOneIsTheDateOfBirthPlusTheMaximumAge() {
    maximumAge("12 years");

    build();

    assertEquals("CALCDTAGE-1: date of birth 01/15/2015 plus a maximum age of '12 years'",
        date("01/15/2027"), finalValueOf("Maximum Age Date"));
  }

  /**
   * <strong>FORECASTDTCAN-1, first of six candidate dates.</strong> "The
   * candidate earliest date of a patient series forecast must be the latest of
   * the following dates: minimum age date; latest of all minimum interval dates;
   * latest of all forecast conflict end dates; seasonal recommendation start
   * date; latest of all dates administered of any inadvertent administration ...;
   * date administered of the most recent vaccine dose administered ..."
   *
   * <p>
   * With only the minimum age in play - the series dose here has a minimum age of
   * "12 months" and nothing else that could push the date later - the latest of
   * the six is the minimum age date, 01/15/2016.
   */
  @Test
  public void forecastdtcanOneIncludesTheMinimumAgeDate() {
    minimumAge("12 months");

    build();

    assertEquals("FORECASTDTCAN-1's first candidate date: the minimum age date, date of birth "
        + "01/15/2015 plus '12 months'", date("01/15/2016"), candidateEarliestDate());
  }

  /**
   * <strong>FORECASTDTCAN-1, second of six candidate dates:</strong> "latest of
   * all minimum interval dates". The series dose here carries one interval, four
   * weeks from target dose 1, which was satisfied by a dose administered
   * 03/10/2024 - so the minimum interval date is 04/07/2024, later than the
   * minimum age date of 01/15/2015, and it is therefore the candidate earliest
   * date.
   */
  @Test
  public void forecastdtcanOneIncludesTheLatestMinimumIntervalDate() {
    VaccineDoseAdministered administered = new VaccineDoseAdministered();
    administered.setDateAdministered(date("03/10/2024"));
    targetDose.setSatisfiedByVaccineDoseAdministered(administered);

    Interval interval = new Interval();
    interval.setSeriesDose(seriesDose);
    interval.setFromImmediatePreviousDoseAdministered(YesNo.NO);
    interval.setFromTargetDoseNumberInSeries("1");
    interval.setAbsoluteMinimumInterval(new TimePeriod("4 weeks"));
    interval.setMinimumInterval(new TimePeriod("4 weeks"));
    seriesDose.getIntervalList().add(interval);

    // The interval calculation needs a previous dose to measure from.
    Evaluation evaluation = new Evaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.VALID);
    TargetDose previousTargetDose = new TargetDose(seriesDose);
    previousTargetDose.setEvaluation(evaluation);
    dataModel.setPreviousTargetDose(previousTargetDose);
    dataModel.setAntigenAdministeredRecord(new AntigenAdministeredRecord());

    build();

    assertEquals("FORECASTDTCAN-1's second candidate date: the latest minimum interval date, "
        + "a dose administered 03/10/2024 plus a minimum interval of '4 weeks'",
        date("04/07/2024"), candidateEarliestDate());
  }

  /**
   * <strong>FORECASTDTCAN-1, fourth of six candidate dates:</strong> "seasonal
   * recommendation start date". A series dose that may only be given inside a
   * season cannot be given before that season opens, so the season's start date
   * is one of the six dates the candidate earliest date is the latest of.
   *
   * <p>
   * This is not a cosmetic omission: the candidate earliest date is the input to
   * Table 7-10's seventh condition, "is the candidate earliest date &lt; the
   * maximum age date?", whose whole purpose is to stop the engine forecasting a
   * date the patient can never validly receive. A candidate earliest date
   * computed without the seasonal start can answer Yes there where the true one
   * would answer No.
   */
  @Test
  public void forecastdtcanOneIncludesTheSeasonalRecommendationStartDate() {
    seasonalRecommendation("09/01/2030", "03/31/2031");

    build();

    assertFalse("FORECASTDTCAN-1's fourth candidate date: the candidate earliest date ("
        + candidateEarliestDate() + ") must be at least the seasonal recommendation start date "
        + "(09/01/2030)", candidateEarliestDate().before(date("09/01/2030")));
  }

  /**
   * <strong>FORECASTDTCAN-1, sixth of six candidate dates:</strong> "date
   * administered of the most recent vaccine dose administered being evaluated
   * against a target dose that is part of a patient series that is the basis of
   * the patient series forecast" - a next dose cannot be earlier than the last
   * dose the patient already received.
   *
   * <p>
   * The fixture is the shape 7.5's own {@code computeEarliestDate()} reads for
   * this same rule: a selected antigen administered record whose vaccine dose
   * administered is attached to a target dose. Here the most recent dose was
   * given 03/10/2024, well after the minimum age date of 01/15/2015.
   */
  @Test
  public void forecastdtcanOneIncludesTheMostRecentDateAdministered() {
    VaccineDoseAdministered administered = new VaccineDoseAdministered();
    administered.setDateAdministered(date("03/10/2024"));
    administered.setTargetDose(targetDose);

    AntigenAdministeredRecord record = new AntigenAdministeredRecord();
    record.setAntigen(measles);
    record.setDateAdministered(date("03/10/2024"));
    record.setVaccineDoseAdministered(administered);
    dataModel.setSelectedAntigenAdministeredRecordList(
        new ArrayList<AntigenAdministeredRecord>());
    dataModel.getSelectedAntigenAdministeredRecordList().add(record);

    build();

    assertFalse("FORECASTDTCAN-1's sixth candidate date: the candidate earliest date ("
        + candidateEarliestDate() + ") must be at least the date administered of the most recent "
        + "vaccine dose administered (03/10/2024)",
        candidateEarliestDate().before(date("03/10/2024")));
  }

  // ==================================== Table 7-10, as the specification prints it

  /**
   * Table 7-10 "Should the Patient Receive Another Target Dose?", seven
   * conditions by eight rules, exactly as printed:
   *
   * <pre>
   * &ge;1 target dose 'Not Satisfied'?             Yes  No   No   -    -    -    -    -
   * &ge;1 target dose 'Satisfied'?                 -    Yes  No   -    -    -    -    -
   * Evidence of immunity?                          No   -    -    Yes  -    -    -    -
   * Series is contraindicated?                     No   -    -    -    Yes  -    -    -
   * Assessment &le; seasonal recommendation end?    Yes  -    -    -    -    No   -    -
   * Assessment &lt; maximum age date?               Yes  -    -    -    -    -    No   -
   * Candidate earliest &lt; maximum age date?       Yes  -    -    -    -    -    -    No
   * </pre>
   */
  @Test
  public void tableSevenTenIsEncodedExactlyAsTheSpecificationPrintsIt() {
    build();

    LogicTable tableSevenTen = table("Table 7-10");
    assertLabelIs("Table 7-10 Should the patient receive another target dose?",
        tableSevenTen.getLabel());
    assertEquals("Table 7-10 has seven conditions", 7,
        tableSevenTen.getLogicConditions().length);
    assertEquals("Table 7-10 has eight rules", 8, tableSevenTen.getLogicOutcomes().length);

    LogicResult[][] grid = tableSevenTen.getLogicResultTable();
    assertArrayEquals("Table 7-10 condition 1: does the patient have at least one target dose "
        + "with a target dose status of 'not satisfied'?",
        new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.ANY,
            LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY },
        grid[0]);
    assertArrayEquals("Table 7-10 condition 2: does the patient have at least one target dose "
        + "with a target dose status of 'satisfied'?",
        new LogicResult[] { LogicResult.ANY, LogicResult.YES, LogicResult.NO, LogicResult.ANY,
            LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY },
        grid[1]);
    assertArrayEquals("Table 7-10 condition 3: does the patient have evidence of immunity?",
        new LogicResult[] { LogicResult.NO, LogicResult.ANY, LogicResult.ANY, LogicResult.YES,
            LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY },
        grid[2]);
    assertArrayEquals("Table 7-10 condition 4: is the relevant patient series a contraindicated "
        + "patient series?",
        new LogicResult[] { LogicResult.NO, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY,
            LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY },
        grid[3]);
    assertArrayEquals("Table 7-10 condition 5: is the assessment date <= the seasonal "
        + "recommendation end date?",
        new LogicResult[] { LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY,
            LogicResult.ANY, LogicResult.NO, LogicResult.ANY, LogicResult.ANY },
        grid[4]);
    assertArrayEquals("Table 7-10 condition 6: is the assessment date < the maximum age date?",
        new LogicResult[] { LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY,
            LogicResult.ANY, LogicResult.ANY, LogicResult.NO, LogicResult.ANY },
        grid[5]);
    assertArrayEquals("Table 7-10 condition 7: is the candidate earliest date < the maximum age "
        + "date?",
        new LogicResult[] { LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY,
            LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.NO },
        grid[6]);
  }

  // ==================================== Table 7-10's eight rules and their outcomes

  /**
   * <strong>Table 7-10 Rule 1 - "Yes, the patient should receive another target
   * dose."</strong> The only rule that answers Yes, and the only one that needs
   * all seven conditions to line up: a target dose is still not satisfied, there
   * is no immunity, no contraindication, the season has not ended and neither the
   * patient nor the earliest possible date is past the maximum age.
   *
   * <p>
   * Its state change is the patient series status becoming Not Complete, and it
   * is the one rule that goes on to 7.5 to actually compute the forecast dates -
   * "only when none of those seven 'stop' conditions apply does the engine
   * proceed to actually compute a date, in 7.5."
   */
  @Test
  public void ruleOneAnOpenTargetDoseMeansThePatientNeedsAnotherDose() {
    run();

    assertStatusIs(PatientSeriesStatus.NOT_COMPLETE, "Table 7-10 Rule 1: with a target dose still "
        + "'not satisfied' and no stop condition applying, the patient series status is 'Not "
        + "Complete'");
    assertEquals("Table 7-10 Rule 1 is the only rule that proceeds to 7.5 Generate Forecast Dates "
        + "and Recommended Vaccines",
        LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES,
        step.getNextLogicStepType());
  }

  /**
   * <strong>Table 7-10 Rule 2 - "No (Complete)."</strong> No target dose is still
   * not satisfied and at least one is satisfied, so the series is finished: the
   * patient series status becomes Complete, the forecast reason becomes "patient
   * series is complete", and the engine loops back to 4.4 rather than forecasting
   * a date.
   */
  @Test
  public void ruleTwoASeriesWithNoOpenTargetDoseIsComplete() {
    everyTargetDose(TargetDoseStatus.SATISFIED);

    run();

    assertStatusIs(PatientSeriesStatus.COMPLETE,
        "Table 7-10 Rule 2: every target dose satisfied means the patient series is 'Complete'");
    assertForecastReasonIs(REASON_COMPLETE, "Table 7-10 Rule 2's forecast reason");
    assertLoopsBackToFourFour("Table 7-10 Rule 2 has nothing left to forecast, so it returns to "
        + "4.4 Evaluate and Forecast All Patient Series");
  }

  /**
   * <strong>Table 7-10 Rule 3 - "No (Not Recommended - past history)."</strong>
   * Neither condition 1 nor condition 2 is Yes: no target dose is not satisfied
   * and none is satisfied either, which is what happens when every target dose in
   * the series was skipped, substituted or found unnecessary on the strength of
   * the patient's existing immunization history. The status becomes Not
   * Recommended with the reason naming that history.
   */
  @Test
  public void ruleThreeASeriesWithNeitherSatisfiedNorUnsatisfiedDosesIsNotRecommended() {
    everyTargetDose(TargetDoseStatus.SKIPPED);

    run();

    assertStatusIs(PatientSeriesStatus.NOT_RECOMMENDED, "Table 7-10 Rule 3: with no target dose "
        + "'not satisfied' and none 'satisfied', the patient series status is 'Not Recommended'");
    assertForecastReasonIs(REASON_PAST_HISTORY, "Table 7-10 Rule 3's forecast reason");
    assertLoopsBackToFourFour("Table 7-10 Rule 3 returns to 4.4");
  }

  /**
   * <strong>Table 7-10 Rule 4 - "No (Immune)."</strong> The section's third
   * condition, "does the patient have evidence of immunity?", reads back the
   * outcome 7.2 records: the patient series status {@code IMMUNE}. An immune
   * patient needs no further dose of this antigen however many of its target
   * doses are still open, so Rule 4's other conditions are all "-".
   *
   * <p>
   * The {@code IMMUNE} status is set directly on the patient series here, which
   * is what 7.2's own state change does; whether 7.2 can currently reach that
   * state change is 7.2's question, not this one.
   */
  @Test
  public void ruleFourEvidenceOfImmunityStopsTheForecast() {
    patientSeries.setPatientSeriesStatus(PatientSeriesStatus.IMMUNE);

    run();

    assertStatusIs(PatientSeriesStatus.IMMUNE, "Table 7-10 Rule 4: a patient with evidence of "
        + "immunity keeps the patient series status 'Immune'");
    assertForecastReasonIs(REASON_IMMUNE, "Table 7-10 Rule 4's forecast reason");
    assertLoopsBackToFourFour("Table 7-10 Rule 4 returns to 4.4");
  }

  /**
   * <strong>Table 7-10 Rule 5 - "No (Contraindicated)."</strong> The fourth
   * condition is "is the relevant patient series a contraindicated patient
   * series?" - a question about <em>this patient series</em>, and precisely the
   * question 7.3's Table 7-7 answers. 7.3's one state change is the patient
   * series status becoming {@code CONTRAINDICATED}, and the step package for 7.2
   * describes the same handshake for immunity, so Rule 5 is Rule 4's exact
   * counterpart: 7.4 reads back what 7.3 recorded.
   *
   * <p>
   * The status is set directly on the patient series here - the same way Rule 4's
   * test sets {@code IMMUNE} - so what is under test is 7.4's reading of it, not
   * whether 7.3 can produce it.
   */
  @Test
  public void ruleFiveAContraindicatedPatientSeriesStopsTheForecast() {
    patientSeries.setPatientSeriesStatus(PatientSeriesStatus.CONTRAINDICATED);

    run();

    assertForecastReasonIs(REASON_CONTRAINDICATED, "Table 7-10 Rule 5's forecast reason");
    assertLoopsBackToFourFour("Table 7-10 Rule 5 returns to 4.4 rather than forecasting a date "
        + "for a contraindicated series");
    assertStatusIs(PatientSeriesStatus.CONTRAINDICATED, "Table 7-10 Rule 5: a contraindicated "
        + "patient series keeps the patient series status 'Contraindicated'");
  }

  /**
   * The other direction of the same condition. Table 7-9 names "the outcome of
   * 7.3 (Contraindicated Patient Series)" as the input to condition 4, and the
   * condition's own wording scopes it to "the relevant patient series". A
   * contraindication recorded against the patient as a whole is not that outcome:
   * 7.3 is what decides, per series, whether an antigen contraindication applies
   * and whether every preferable vaccine for <em>that</em> series is
   * contraindicated. A patient series 7.3 did not mark contraindicated must
   * therefore still be forecast.
   *
   * <p>
   * The distinction is not academic. A patient-level read makes one antigen's
   * contraindication silence every other antigen's series too, which is the
   * opposite of what 7.3's own section says an antigen contraindication does:
   * "an antigen contraindication prevents all relevant patient series
   * <em>for that antigen</em> from recommending further vaccination".
   */
  @Test
  public void theContraindicationConditionAsksAboutThisPatientSeriesNotThePatientAsAWhole() {
    Antigen otherAntigen = new Antigen();
    otherAntigen.setName("Influenza");
    Contraindication_TO_BE_REMOVED patientLevelRecord = new Contraindication_TO_BE_REMOVED();
    patientLevelRecord.setAntigen(otherAntigen);
    patientLevelRecord.setContraindicationLanguage("Immunocompromised");
    patient.getMedicalHistory().getContraindicationSet().add(patientLevelRecord);

    run();

    assertStatusIs(PatientSeriesStatus.NOT_COMPLETE, "Table 7-10 condition 4 asks whether this "
        + "relevant patient series is a contraindicated patient series - 7.3 did not make it one, "
        + "so Rule 1 still applies and the series is 'Not Complete'");
  }

  /**
   * <strong>Table 7-10 Rule 6 - "No (Not Recommended - season ended)."</strong>
   * The fifth condition is "is the assessment date &le; the seasonal
   * recommendation end date?"; a No there is enough on its own. The series dose
   * here has a season that closed 03/31/2025 and the patient is being assessed
   * 06/15/2025, so the season for this dose is over and there is nothing left to
   * recommend.
   */
  @Test
  public void ruleSixAnAssessmentPastTheSeasonalRecommendationEndDateStopsTheForecast() {
    seasonalRecommendation("09/01/2024", "03/31/2025");

    run();

    assertStatusIs(PatientSeriesStatus.NOT_RECOMMENDED, "Table 7-10 Rule 6: an assessment date "
        + "past the seasonal recommendation end date makes the patient series 'Not Recommended'");
    assertForecastReasonIs(REASON_SEASON_ENDED, "Table 7-10 Rule 6's forecast reason");
    assertLoopsBackToFourFour("Table 7-10 Rule 6 returns to 4.4");
  }

  /**
   * <strong>Table 7-10 Rule 7 - "No (Aged Out - assessment past max age)."</strong>
   * The sixth condition is "is the assessment date &lt; the maximum age date?".
   * The patient here was born 01/15/2015 and the series dose has a maximum age of
   * "6 years", so they aged out on 01/15/2021, four years before the 06/15/2025
   * assessment. Their earliest possible date (01/15/2015, the minimum age date)
   * is still before the maximum age date, so condition 7 is Yes and only Rule 7
   * applies.
   */
  @Test
  public void ruleSevenAPatientPastTheMaximumAgeHasAgedOut() {
    maximumAge("6 years");

    run();

    assertStatusIs(PatientSeriesStatus.AGED_OUT, "Table 7-10 Rule 7: an assessment date at or "
        + "past the maximum age date makes the patient series 'Aged Out'");
    assertForecastReasonIs(REASON_PAST_MAXIMUM_AGE, "Table 7-10 Rule 7's forecast reason");
    assertLoopsBackToFourFour("Table 7-10 Rule 7 returns to 4.4");
  }

  /**
   * <strong>Table 7-10 Rule 8 - "No (Aged Out - can't finish before max
   * age)."</strong> The seventh condition is "is the candidate earliest date &lt;
   * the maximum age date?". The patient here is still inside the age window on
   * the assessment date - born 01/15/2015, maximum age "12 years", so the maximum
   * age date is 01/15/2027 - but the series dose cannot be given before a minimum
   * age of "18 years", i.e. 01/15/2033. The earliest date they could possibly
   * receive it is already past the date they age out, so there is no point
   * forecasting one.
   */
  @Test
  public void ruleEightASeriesThatCannotBeStartedBeforeTheMaximumAgeHasAgedOut() {
    maximumAge("12 years");
    minimumAge("18 years");

    run();

    assertStatusIs(PatientSeriesStatus.AGED_OUT, "Table 7-10 Rule 8: a candidate earliest date "
        + "(01/15/2033) at or past the maximum age date (01/15/2027) makes the patient series "
        + "'Aged Out'");
    assertForecastReasonIs(REASON_CANNOT_FINISH, "Table 7-10 Rule 8's forecast reason");
    assertLoopsBackToFourFour("Table 7-10 Rule 8 returns to 4.4");
  }

  /**
   * The everyday partly-completed series, and the case where Table 7-10's first
   * two conditions have to be read together. Dose 1 is satisfied and dose 2 is
   * not, so condition 1 is Yes <em>and</em> condition 2 is Yes.
   *
   * <p>
   * The step package's walkthrough states the intended reading directly: "dose
   * statuses that already answer the question (<strong>all</strong> satisfied =
   * complete, one still open = keep going)". Rule 2's outcome is Complete only
   * where nothing is still open - which is why Rule 2's first condition is No in
   * Table 7-10's printed grid for Rule 3 and why Rule 1 asks for a not-satisfied
   * dose at all. A patient halfway through a two-dose series needs their second
   * dose; answering Complete would end the forecast for an antigen the patient is
   * not yet protected against.
   */
  @Test
  public void aPartlyCompletedSeriesStillNeedsItsRemainingDose() {
    SeriesDose secondSeriesDose = new SeriesDose();
    secondSeriesDose.setDoseNumber("2");
    secondSeriesDose.setAntigenSeries(antigenSeries);
    Age secondAge = new Age();
    secondAge.setSeriesDose(secondSeriesDose);
    secondAge.setMinimugeAge(new TimePeriod("0 days"));
    secondAge.setMaximumAge(new TimePeriod(""));
    secondSeriesDose.getAgeList().add(secondAge);
    antigenSeries.getSeriesDoseList().add(secondSeriesDose);

    // Dose 1 is already satisfied; dose 2 - the one being forecast - is not.
    targetDose.setTargetDoseStatus(TargetDoseStatus.SATISFIED);
    TargetDose secondTargetDose = new TargetDose(secondSeriesDose);
    secondTargetDose.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
    dataModel.getTargetDoseList().add(secondTargetDose);
    dataModel.setTargetDose(secondTargetDose);

    run();

    assertStatusIs(PatientSeriesStatus.NOT_COMPLETE, "Table 7-10 Rule 1: dose 2 of a two-dose "
        + "series is still 'not satisfied', so the patient series is 'Not Complete' and needs "
        + "another target dose - it is not 'Complete' merely because dose 1 was satisfied");
    assertEquals("a partly completed series proceeds to 7.5 to have its next dose forecast",
        LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES,
        step.getNextLogicStepType());
  }
}
