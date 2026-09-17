package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.SeasonalRecommendation;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;

/**
 * Section 7.5 "Generate Forecast Dates and Recommended Vaccines" (Logic
 * Specification for ACIP Recommendations v4.6, pages 80-82) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/07-05-generate-forecast-dates-and-recommended-vaccines/index.md}.
 *
 * <p>
 * 7.5 is the step that actually produces a forecast. It runs only when 7.4
 * decided another target dose is needed, and its job - per the specification -
 * is to "determine the forecast dates for the next target dose and identif[y]
 * one or more recommended vaccines if the target dose warrants specific vaccine
 * recommendations. Additional detail, such as administrative guidance for
 * providers may also be included."
 *
 * <p>
 * The section is Table 7-12 (eleven attributes, all calculated dates or
 * Supporting Data values) and Table 7-13 (nine business rules: FORECASTDT-1
 * through FORECASTDT-6, FORECASTGUIDANCE-1, FORECASTRECVAC-1 and FORECASTDN-1).
 * There is no decision table. One test method below corresponds to one business
 * rule or - where a rule prints a bulleted preference order - to one bullet of
 * one rule, which is the unit the specification actually states.
 *
 * <h2>What the attribute table promises</h2>
 *
 * <p>
 * Uniquely among the sections tested so far, Table 7-12 prints "-" in the
 * "Assumed Value if Empty" column for nine of its eleven attributes, and the
 * surrounding text says so explicitly: "If an attribute value is empty, then the
 * date calculations will remain empty. No assumptions will be made for the
 * attribute." Only the Seasonal Recommendation Start Date (01/01/1900) and the
 * Forecast Vaccine Type Flag ('N') carry an assumed value. That is why several
 * tests below assert a <em>blank</em> result rather than a computed one - a
 * blank forecast date is a specified outcome here, not a missing one.
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step's constructor
 * reads. The constructor is not passive: nine {@code find*()} methods run before
 * the constructor body finishes, and they dereference
 * {@code getTargetDose().getTrackedSeriesDose().getAgeList().get(0)} and each of
 * that {@link Age}'s four {@link TimePeriod}s without a null check, so the
 * fixture always carries a two-dose series - dose 1 already satisfied by a dose
 * administered 03/10/2024, dose 2 the one being forecast - whose {@link Age}
 * defines all four periods (empty where the test wants "no such date").
 *
 * <p>
 * The six FORECASTDT rules are public methods and are invoked directly.
 * {@code computeLatestDate()} and {@code computeDates(Forecast)} are private and
 * are reached reflectively, the same way {@code NoValidDosesCompletableTest}
 * reaches its scoring condition. {@code process()} is safe to call here (unlike
 * 7.4's, its {@code next()} builds {@code ValidateRecommendation}, which needs
 * nothing this fixture does not already provide), so the state changes and the
 * transition to 7.6 are asserted against the real method.
 */
public class GenerateForecastDatesAndRecommendedVaccinesTest {

  private static final String DATE_OF_BIRTH = "01/15/2015";
  private static final String ASSESSMENT_DATE = "06/15/2025";

  /** The date the already-satisfied dose 1 of the fixture series was given. */
  private static final String DOSE_ONE_ADMINISTERED = "03/10/2024";

  /** Table 7-12: Seasonal Recommendation Start Date, "Assumed Value if Empty". */
  private static final String ASSUMED_SEASONAL_RECOMMENDATION_START_DATE = "01/01/1900";

  private DataModel dataModel;
  private Patient patient;
  private Antigen measles;
  private AntigenSeries antigenSeries;
  private SeriesDose seriesDoseOne;
  private SeriesDose seriesDoseTwo;
  private Age age;
  private PatientSeries patientSeries;
  private TargetDose targetDoseOne;
  private TargetDose targetDoseTwo;
  private VaccineDoseAdministered doseOneAdministered;
  private Forecast forecast;
  private GenerateForecastDatesAndRecommendedVaccines step;

  /**
   * A ten-year-old part-way through a two-dose series: dose 1 was given
   * 03/10/2024 and is Satisfied, dose 2 is Not Satisfied and is the target dose
   * 7.5 is forecasting. Dose 2's {@link Age} defines a minimum age of "0 days"
   * and leaves the maximum, earliest recommended and latest recommended ages
   * empty, so the unmodified fixture is Table 7-12's "every date is empty" case
   * and each test supplies only the one attribute its own rule is about.
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

    seriesDoseOne = new SeriesDose();
    seriesDoseOne.setDoseNumber("1");
    seriesDoseOne.setAntigenSeries(antigenSeries);
    Age ageOne = new Age();
    ageOne.setSeriesDose(seriesDoseOne);
    ageOne.setMinimugeAge(new TimePeriod("0 days"));
    ageOne.setMaximumAge(new TimePeriod(""));
    ageOne.setEarliestRecommendedAge(new TimePeriod(""));
    ageOne.setLatestRecommendedAge(new TimePeriod(""));
    seriesDoseOne.getAgeList().add(ageOne);
    antigenSeries.getSeriesDoseList().add(seriesDoseOne);

    seriesDoseTwo = new SeriesDose();
    seriesDoseTwo.setDoseNumber("2");
    seriesDoseTwo.setAntigenSeries(antigenSeries);
    age = new Age();
    age.setSeriesDose(seriesDoseTwo);
    age.setMinimugeAge(new TimePeriod("0 days"));
    // Table 7-12 prints "-" for all four ages' assumed values: an empty
    // TimePeriod is how "there is no such date" is expressed.
    age.setMaximumAge(new TimePeriod(""));
    age.setEarliestRecommendedAge(new TimePeriod(""));
    age.setLatestRecommendedAge(new TimePeriod(""));
    seriesDoseTwo.getAgeList().add(age);
    antigenSeries.getSeriesDoseList().add(seriesDoseTwo);

    doseOneAdministered = new VaccineDoseAdministered();
    doseOneAdministered.setDateAdministered(date(DOSE_ONE_ADMINISTERED));

    targetDoseOne = new TargetDose(seriesDoseOne);
    targetDoseOne.setTargetDoseStatus(TargetDoseStatus.SATISFIED);
    targetDoseOne.setSatisfiedByVaccineDoseAdministered(doseOneAdministered);
    doseOneAdministered.setTargetDose(targetDoseOne);
    Evaluation doseOneEvaluation = new Evaluation();
    doseOneEvaluation.setEvaluationStatus(EvaluationStatus.VALID);
    targetDoseOne.setEvaluation(doseOneEvaluation);

    targetDoseTwo = new TargetDose(seriesDoseTwo);
    targetDoseTwo.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);

    dataModel.setTargetDoseList(new ArrayList<TargetDose>());
    dataModel.getTargetDoseList().add(targetDoseOne);
    dataModel.getTargetDoseList().add(targetDoseTwo);
    dataModel.setTargetDose(targetDoseTwo);
    // What an interval measures from: the previously evaluated target dose.
    dataModel.setPreviousTargetDose(targetDoseOne);
    dataModel.setAntigenAdministeredRecord(new AntigenAdministeredRecord());

    patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setTargetDoseList(dataModel.getTargetDoseList());
    dataModel.getPatientSeriesStepper().add(patientSeries);
    dataModel.getPatientSeriesStepper().increment();

    // computeEarliestDate()'s "most recent dose administered" candidate reads
    // this list; empty (but never null) keeps it out of the other tests' way.
    dataModel.setSelectedAntigenAdministeredRecordList(new ArrayList<AntigenAdministeredRecord>());

    forecast = new Forecast();
    forecast.setAntigen(measles);
    forecast.setTargetDose(targetDoseTwo);
    dataModel.setForecast(forecast);

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

  private GenerateForecastDatesAndRecommendedVaccines build() {
    step = new GenerateForecastDatesAndRecommendedVaccines(dataModel);
    return step;
  }

  private void minimumAge(String timePeriod) {
    age.setMinimugeAge(new TimePeriod(timePeriod));
  }

  private void maximumAge(String timePeriod) {
    age.setMaximumAge(new TimePeriod(timePeriod));
  }

  private void earliestRecommendedAge(String timePeriod) {
    age.setEarliestRecommendedAge(new TimePeriod(timePeriod));
  }

  private void latestRecommendedAge(String timePeriod) {
    age.setLatestRecommendedAge(new TimePeriod(timePeriod));
  }

  /**
   * An interval on the dose being forecast, measured from target dose 1 (given
   * {@value #DOSE_ONE_ADMINISTERED}). Any of the three recommended/minimum
   * periods may be null, which is how Supporting Data expresses "this interval
   * defines no such period".
   */
  private Interval interval(String minimum, String earliestRecommended, String latestRecommended) {
    Interval interval = new Interval();
    interval.setSeriesDose(seriesDoseTwo);
    interval.setFromImmediatePreviousDoseAdministered(YesNo.NO);
    interval.setFromTargetDoseNumberInSeries("1");
    interval.setAbsoluteMinimumInterval(new TimePeriod(minimum == null ? "0 days" : minimum));
    if (minimum != null) {
      interval.setMinimumInterval(new TimePeriod(minimum));
    }
    if (earliestRecommended != null) {
      interval.setEarliestRecommendedInterval(new TimePeriod(earliestRecommended));
    }
    if (latestRecommended != null) {
      interval.setLatestRecommendedInterval(new TimePeriod(latestRecommended));
    }
    seriesDoseTwo.getIntervalList().add(interval);
    return interval;
  }

  private SeasonalRecommendation seasonalRecommendation(String startDate, String endDate) {
    SeasonalRecommendation seasonalRecommendation = new SeasonalRecommendation();
    seasonalRecommendation.setSeriesDose(seriesDoseTwo);
    if (startDate != null) {
      seasonalRecommendation.setSeasonalRecommendationStartDate(date(startDate));
    }
    if (endDate != null) {
      seasonalRecommendation.setSeasonalRecommendationEndDate(date(endDate));
    }
    seriesDoseTwo.getSeasonalRecommendationList().add(seasonalRecommendation);
    return seasonalRecommendation;
  }

  private PreferrableVaccine preferableVaccine(String cvx, YesNo forecastVaccineType,
      String beginAge, String endAge) {
    VaccineType vaccineType = new VaccineType();
    vaccineType.setCvxCode(cvx);
    vaccineType.setShortDescription("MMR");
    vaccineType.setAntigenList(new ArrayList<Antigen>());
    vaccineType.getAntigenList().add(measles);

    PreferrableVaccine preferrableVaccine = new PreferrableVaccine();
    preferrableVaccine.setSeriesDose(seriesDoseTwo);
    preferrableVaccine.setVaccineType(vaccineType);
    preferrableVaccine.setForecastVaccineType(forecastVaccineType);
    preferrableVaccine.setVaccineTypeBeginAge(new TimePeriod(beginAge));
    preferrableVaccine.setVaccineTypeEndAge(new TimePeriod(endAge));
    seriesDoseTwo.getPreferrableVaccineList().add(preferrableVaccine);
    return preferrableVaccine;
  }

  private LiveVirusConflict liveVirusConflict(String previousCvx, String currentCvx,
      String conflictBeginInterval, String minimumConflictEndInterval,
      String conflictEndInterval) {
    LiveVirusConflict liveVirusConflict = new LiveVirusConflict();
    liveVirusConflict.setPreviousVaccineType(vaccineType(previousCvx));
    liveVirusConflict.setCurrentVaccineType(vaccineType(currentCvx));
    liveVirusConflict.setConflictBeginInterval(new TimePeriod(conflictBeginInterval));
    liveVirusConflict.setMinimalConflictEndInterval(new TimePeriod(minimumConflictEndInterval));
    liveVirusConflict.setConflictEndInterval(new TimePeriod(conflictEndInterval));
    dataModel.getLiveVirusConflictList().add(liveVirusConflict);
    return liveVirusConflict;
  }

  private AntigenAdministeredRecord historyDose(String administered, String cvx,
      EvaluationStatus status) {
    VaccineType type = vaccineType(cvx);
    VaccineDoseAdministered vda = new VaccineDoseAdministered();
    vda.setDateAdministered(date(administered));
    TargetDose evaluatedAgainst = new TargetDose(seriesDoseOne);
    Evaluation evaluation = new Evaluation();
    evaluation.setEvaluationStatus(status);
    evaluatedAgainst.setEvaluation(evaluation);
    vda.setEvaluatedAgainstTargetDose(evaluatedAgainst);

    AntigenAdministeredRecord aar = new AntigenAdministeredRecord();
    aar.setDateAdministered(date(administered));
    aar.setVaccineType(type);
    aar.setVaccineDoseAdministered(vda);
    dataModel.getAntigenAdministeredRecordList().add(aar);
    return aar;
  }

  private static VaccineType vaccineType(String cvxCode) {
    VaccineType vaccineType = new VaccineType();
    vaccineType.setCvxCode(cvxCode);
    vaccineType.setShortDescription("CVX " + cvxCode);
    return vaccineType;
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
    assertNotNull("Table 7-12 lists an attribute named '" + attributeName + "'", conditionAttribute);
    return conditionAttribute.getFinalValue();
  }

  /**
   * Sets one Table 7-12 attribute directly, for the two attributes whose own
   * calculation belongs to another section (CALCDTLIVE-4's conflict end date)
   * and would otherwise need a whole live-virus-conflict fixture to exercise a
   * FORECASTDT rule that only consumes the result.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void setAttribute(String attributeName, Object value) {
    ConditionAttribute conditionAttribute = attribute(attributeName);
    assertNotNull("Table 7-12 lists an attribute named '" + attributeName + "'", conditionAttribute);
    conditionAttribute.setInitialValue(value);
  }

  private Date computeLatestDate() {
    return (Date) invokePrivate("computeLatestDate", new Class<?>[] {}, new Object[] {});
  }

  private void computeDates(Forecast target) {
    invokePrivate("computeDates", new Class<?>[] { Forecast.class }, new Object[] { target });
  }

  private Object invokePrivate(String name, Class<?>[] parameterTypes, Object[] arguments) {
    try {
      Method method = GenerateForecastDatesAndRecommendedVaccines.class.getDeclaredMethod(name,
          parameterTypes);
      method.setAccessible(true);
      return method.invoke(step, arguments);
    } catch (NoSuchMethodException nsme) {
      fail("7.5 must implement " + name + "(): " + nsme.getMessage());
      return null;
    } catch (IllegalAccessException iae) {
      throw new IllegalStateException(iae);
    } catch (InvocationTargetException ite) {
      Throwable cause = ite.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new IllegalStateException(cause);
    }
  }

  /**
   * Looks for any accessor, on the {@link Forecast} a forecast is reported
   * through or on the step itself, that could carry what one of the three
   * non-date business rules produces. Used only by the FORECASTRECVAC-1,
   * FORECASTDN-1 and FORECASTGUIDANCE-1 tests: those rules name an output the
   * domain model has to hold somewhere, and this is how those tests ask whether
   * anywhere is.
   */
  private Method accessorMatching(String regex) {
    for (Method method : Forecast.class.getMethods()) {
      if (method.getParameterTypes().length == 0 && method.getName().matches(regex)) {
        return method;
      }
    }
    for (Method method : GenerateForecastDatesAndRecommendedVaccines.class.getMethods()) {
      if (method.getParameterTypes().length == 0 && method.getName().matches(regex)) {
        return method;
      }
    }
    return null;
  }

  // ================================================= What 7.5's own class is

  /**
   * 7.5's identity: {@code LogicStepFactory} is how the engine reaches it (7.4's
   * Table 7-10 Rule 1 hands it
   * {@code GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES}), it publishes
   * chapter "7.5", and it names Table 7-12 as its attribute table. Both factory
   * overloads are checked, since the pipeline's dispatch and the web renderer's
   * use different ones.
   */
  @Test
  public void theFactoryBuildsThisClassForStepSevenFive() {
    LogicStep built = LogicStepFactory.createLogicStep(
        LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES, dataModel);
    LogicStep builtAgain = LogicStepFactory.createLogicStep(
        LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES, dataModel, true);

    assertTrue("7.5 is built as GenerateForecastDatesAndRecommendedVaccines",
        built instanceof GenerateForecastDatesAndRecommendedVaccines);
    assertTrue("7.5 is built as GenerateForecastDatesAndRecommendedVaccines",
        builtAgain instanceof GenerateForecastDatesAndRecommendedVaccines);
    assertEquals("7.5", built.getLogicStepType().getChapter());
    assertTrue("7.5 names Table 7-12 as its attribute table, but was '"
        + built.getConditionTableName() + "'",
        built.getConditionTableName().replaceAll("\\s+", "").toLowerCase()
            .contains("table7-12"));
  }

  // ==================================== Table 7-12, the attributes 7.5 consumes

  /**
   * Table 7-12 Generate Forecast Date and Recommended Vaccine Attributes prints
   * eleven attributes: four calculated age dates (CALCDTAGE-4/3/2/1), three
   * calculated interval dates (CALCDTINT-4/5/6), the latest conflict end
   * interval date (CALCDTLIVE-4), the Supporting Data seasonal recommendation
   * start date, and the preferable vaccine's Vaccine Type (CVX) and Forecast
   * Vaccine Type Flag. All eleven are inputs to Table 7-13's rules, so all
   * eleven belong on the step's attribute list - that list is the printed table.
   *
   * <p>
   * The last attribute is matched leniently ("Forecast Vaccine Type" with or
   * without the table's trailing "Flag"), since a printed-label difference is
   * not what this test is about.
   */
  @Test
  public void tableSevenTwelveRegistersEveryAttributeItPrints() {
    build();

    assertNotNull("Table 7-12: Calculated date (CALCDTAGE-4) / Minimum Age Date",
        attribute("Minimum Age Date"));
    assertNotNull("Table 7-12: Calculated date (CALCDTAGE-3) / Earliest Recommended Age Date",
        attribute("Earliest Recommended Age Date"));
    assertNotNull("Table 7-12: Calculated date (CALCDTAGE-2) / Latest Recommended Age Date",
        attribute("Latest Recommended Age Date"));
    assertNotNull("Table 7-12: Calculated date (CALCDTAGE-1) / Maximum Age Date",
        attribute("Maximum Age Date"));
    assertNotNull("Table 7-12: Calculated date (CALCDTINT-4) / Minimum Interval Date(s)",
        attribute("Minimum Interval Date(s)"));
    assertNotNull(
        "Table 7-12: Calculated date (CALCDTINT-5) / Earliest Recommended Interval Date(s)",
        attribute("Earliest Recommended Interval Date(s)"));
    assertNotNull(
        "Table 7-12: Calculated date (CALCDTINT-6) / Latest Recommended Interval Date(s)",
        attribute("Latest Recommended Interval Date(s)"));
    assertNotNull("Table 7-12: Calculated date (CALCDTLIVE-4) / Latest Conflict End Interval Date",
        attribute("Latest Conflict End Interval Date"));
    assertNotNull(
        "Table 7-12: Supporting Data (Seasonal Recommendation) / Seasonal Recommendation Start Date",
        attribute("Seasonal Recommendation Start Date"));
    assertNotNull("Table 7-12: Supporting Data (Preferable Vaccine) / Vaccine Type (CVX)",
        attribute("Vaccine Type (CVX)"));
    assertTrue("Table 7-12: Supporting Data (Preferable Vaccine) / Forecast Vaccine Type Flag",
        attribute("Forecast Vaccine Type Flag") != null || attribute("Forecast Vaccine Type") != null);
  }

  /**
   * <strong>CALCDTAGE-4.</strong> Table 7-12's first attribute, the minimum age
   * date: the patient's date of birth plus the series dose's minimum age. A
   * patient born 01/15/2015 whose dose 2 has a minimum age of "12 months" cannot
   * receive it before 01/15/2016.
   */
  @Test
  public void theMinimumAgeDateIsTheDateOfBirthPlusTheMinimumAge() {
    minimumAge("12 months");

    build();

    assertEquals("CALCDTAGE-4: date of birth 01/15/2015 plus a minimum age of '12 months'",
        date("01/15/2016"), finalValueOf("Minimum Age Date"));
  }

  /**
   * Section 3.3 / RELEVANT-2: when a series dose has multiple {@code <age>} rows
   * with Effective/Cessation Dates, forecasting must use the row whose window
   * covers the assessment date - not {@code getAgeList().get(0)}. Polio Dose 4's
   * ceased "18 weeks" row preceding the current "4 years" row is the real-world
   * case (POL-2013-0632).
   */
  @Test
  public void theMinimumAgeDateUsesTheAgeRowRelevantForTheAssessmentDate() {
    Age ceased = new Age();
    ceased.setSeriesDose(seriesDoseTwo);
    ceased.setMinimugeAge(new TimePeriod("18 weeks"));
    ceased.setEarliestRecommendedAge(new TimePeriod("4 years"));
    ceased.setMaximumAge(new TimePeriod(""));
    ceased.setLatestRecommendedAge(new TimePeriod(""));
    ceased.setEffectiveDate(date("01/01/1900"));
    ceased.setCessationDate(date("08/06/2009"));

    age.setMinimugeAge(new TimePeriod("4 years"));
    age.setEarliestRecommendedAge(new TimePeriod("4 years"));
    age.setEffectiveDate(date("08/07/2009"));

    seriesDoseTwo.getAgeList().clear();
    seriesDoseTwo.getAgeList().add(ceased);
    seriesDoseTwo.getAgeList().add(age);

    build();

    assertEquals(
        "RELEVANT-2: assessment 06/15/2025 selects the post-2009 age row (minAge 4 years), not the ceased 18-week row",
        date("01/15/2019"), finalValueOf("Minimum Age Date"));
  }

  /**
   * <strong>CALCDTAGE-3.</strong> The earliest recommended age date - date of
   * birth plus the earliest recommended age - is FORECASTDT-2's first choice for
   * the unadjusted recommended date.
   */
  @Test
  public void theEarliestRecommendedAgeDateIsTheDateOfBirthPlusTheEarliestRecommendedAge() {
    earliestRecommendedAge("4 years");

    build();

    assertEquals(
        "CALCDTAGE-3: date of birth 01/15/2015 plus an earliest recommended age of '4 years'",
        date("01/15/2019"), finalValueOf("Earliest Recommended Age Date"));
  }

  /**
   * <strong>CALCDTAGE-2.</strong> The latest recommended age date - date of birth
   * plus the latest recommended age - is FORECASTDT-3's first choice for the
   * unadjusted past due date.
   */
  @Test
  public void theLatestRecommendedAgeDateIsTheDateOfBirthPlusTheLatestRecommendedAge() {
    latestRecommendedAge("6 years");

    build();

    assertEquals(
        "CALCDTAGE-2: date of birth 01/15/2015 plus a latest recommended age of '6 years'",
        date("01/15/2021"), finalValueOf("Latest Recommended Age Date"));
  }

  /**
   * <strong>CALCDTAGE-1.</strong> The maximum age date - date of birth plus the
   * maximum age - is FORECASTDT-4's only input.
   */
  @Test
  public void theMaximumAgeDateIsTheDateOfBirthPlusTheMaximumAge() {
    maximumAge("18 years");

    build();

    assertEquals("CALCDTAGE-1: date of birth 01/15/2015 plus a maximum age of '18 years'",
        date("01/15/2033"), finalValueOf("Maximum Age Date"));
  }

  /**
   * "If an attribute value is empty, then the date calculations will remain
   * empty. No assumptions will be made for the attribute." Table 7-12 prints "-"
   * in the Assumed Value column for all four age dates and all three interval
   * dates, so a series dose that defines no recommended ages and no intervals -
   * the common case - must leave those attributes blank rather than substituting
   * the date of birth or today.
   */
  @Test
  public void anEmptyAttributeStaysEmptyRatherThanBeingAssumed() {
    build();

    assertNull("Table 7-12: an empty earliest recommended age leaves the earliest recommended "
        + "age date empty", finalValueOf("Earliest Recommended Age Date"));
    assertNull("Table 7-12: an empty latest recommended age leaves the latest recommended age "
        + "date empty", finalValueOf("Latest Recommended Age Date"));
    assertNull("Table 7-12: an empty maximum age leaves the maximum age date empty",
        finalValueOf("Maximum Age Date"));
    assertNull("Table 7-12: a series dose with no intervals has no minimum interval dates",
        finalValueOf("Minimum Interval Date(s)"));
    assertNull("Table 7-12: a series dose with no intervals has no earliest recommended interval "
        + "dates", finalValueOf("Earliest Recommended Interval Date(s)"));
    assertNull("Table 7-12: a series dose with no intervals has no latest recommended interval "
        + "date", finalValueOf("Latest Recommended Interval Date(s)"));
  }

  /**
   * Table 7-12 gives the Seasonal Recommendation Start Date the assumed value
   * <strong>01/01/1900</strong> - one of only two attributes in the table with
   * an assumed value at all. It is a candidate for the earliest date, so a series
   * dose with no season must contribute a date so far in the past that it can
   * never be the latest of the candidates.
   */
  @Test
  public void tableSevenTwelvesSeasonalRecommendationStartDateAssumesNineteenHundred() {
    build();

    assertEquals("Table 7-12: Seasonal Recommendation Start Date, Assumed Value if Empty",
        date(ASSUMED_SEASONAL_RECOMMENDATION_START_DATE),
        attribute("Seasonal Recommendation Start Date").getAssumedValue());
  }

  /**
   * The other half of the same attribute: where the series dose <em>does</em>
   * define a season, the attribute carries that season's start date rather than
   * the assumption.
   */
  @Test
  public void theSeasonalRecommendationStartDateComesFromTheSeriesDosesSeason() {
    seasonalRecommendation("07/01/2025", "06/30/2026");
    dataModel.setAssessmentDate(date("08/15/2025"));

    build();

    assertEquals("Table 7-12: the seasonal recommendation start date of the series dose being "
        + "forecast", date("07/01/2025"), finalValueOf("Seasonal Recommendation Start Date"));
  }

  /**
   * Table 7-12's last attribute, the Forecast Vaccine Type Flag, has the assumed
   * value <strong>'N'</strong>. FORECASTRECVAC-1's second bullet requires the
   * flag to be 'Y' before a vaccine may be recommended, so the assumption is what
   * keeps a preferable vaccine that says nothing about forecasting out of the
   * recommendation.
   */
  @Test
  public void tableSevenTwelvesForecastVaccineTypeFlagAssumesNo() {
    build();

    ConditionAttribute<?> flag = attribute("Forecast Vaccine Type Flag") != null
        ? attribute("Forecast Vaccine Type Flag")
        : attribute("Forecast Vaccine Type");
    assertNotNull("Table 7-12 lists a Forecast Vaccine Type Flag attribute", flag);
    assertEquals("Table 7-12: Forecast Vaccine Type Flag, Assumed Value if Empty is 'N'",
        YesNo.NO, flag.getAssumedValue());
  }

  /**
   * Table 7-12 sources the Vaccine Type (CVX) from the Supporting Data preferable
   * vaccine. It is the attribute FORECASTRECVAC-1's third and fourth bullets are
   * about - the vaccine type checked against the contraindications, and the
   * vaccine whose begin/end age date bounds the recommendation - so where the
   * series dose being forecast has a preferable vaccine, the attribute has to
   * carry it.
   */
  @Test
  public void theVaccineTypeAttributeCarriesThePreferableVaccinesCvx() {
    PreferrableVaccine preferrableVaccine = preferableVaccine("03", YesNo.YES, "12 months", "13 years");

    build();

    assertEquals("Table 7-12: Supporting Data (Preferable Vaccine) / Vaccine Type (CVX)",
        preferrableVaccine.getVaccineType(), finalValueOf("Vaccine Type (CVX)"));
  }

  /**
   * The same for the Forecast Vaccine Type Flag: 'N' is Table 7-12's assumption
   * for a preferable vaccine that does not state one, not a value that survives a
   * preferable vaccine that states 'Y'. FORECASTRECVAC-1 cannot recommend
   * anything while every flag reads 'N'.
   */
  @Test
  public void theForecastVaccineTypeFlagCarriesThePreferableVaccinesValue() {
    preferableVaccine("03", YesNo.YES, "12 months", "13 years");

    build();

    ConditionAttribute<?> flag = attribute("Forecast Vaccine Type Flag") != null
        ? attribute("Forecast Vaccine Type Flag")
        : attribute("Forecast Vaccine Type");
    assertNotNull("Table 7-12 lists a Forecast Vaccine Type Flag attribute", flag);
    assertEquals("Table 7-12: the forecast vaccine type flag of the series dose's preferable "
        + "vaccine, which is 'Y'", YesNo.YES, flag.getFinalValue());
  }

  // ============================== Table 7-13 FORECASTDT-1, the earliest date

  /**
   * <strong>FORECASTDT-1.</strong> "The earliest date of a patient series
   * forecast made from a relevant patient series must be the candidate earliest
   * date." The candidate earliest date (FORECASTDTCAN-1, owned by 7.4) is the
   * latest of six dates; with only a minimum age of "12 months" in play and every
   * other candidate empty or in 1900, the latest of the six is the minimum age
   * date.
   */
  @Test
  public void forecastdtOneTheEarliestDateIsTheCandidateEarliestDate() {
    minimumAge("12 months");

    build();

    assertEquals("FORECASTDT-1: the earliest date is the candidate earliest date, here the "
        + "minimum age date (01/15/2016)", date("01/15/2016"), step.computeEarliestDate());
  }

  /**
   * <strong>FORECASTDT-1</strong> through FORECASTDTCAN-1's second candidate,
   * "latest of all minimum interval dates". Dose 2 here must wait "4 weeks" after
   * dose 1, which was given 03/10/2024, so the earliest it could be given is
   * 04/07/2024 - later than the minimum age date of 01/15/2015, and therefore the
   * earliest date.
   */
  @Test
  public void forecastdtOneTheEarliestDateAccountsForTheLatestMinimumIntervalDate() {
    interval("4 weeks", null, null);

    build();

    assertEquals("FORECASTDT-1: the candidate earliest date includes the latest minimum interval "
        + "date, dose 1 (03/10/2024) plus '4 weeks'", date("04/07/2024"),
        step.computeEarliestDate());
  }

  /**
   * CALCDTINT-1 / FORECASTDT-1 leftover after a Not Valid last shot: 6.10
   * leaves {@code getTargetDose()} null on that VDA and writes the evaluation
   * onto {@code getEvaluatedAgainstTargetDose()}. The previous target is still
   * SKIPPED (no evaluation). The 6-month preferable interval must still be
   * measured from the Not Valid date administered, so earliest/recommended
   * land six months later rather than on the last-shot / minimum-age date.
   */
  @Test
  public void forecastdtOneMinimumIntervalUsesNotValidPreviousDoseWhenPreviousTargetWasSkipped() {
    Interval fromImmediate = interval("6 months", null, null);
    fromImmediate.setFromImmediatePreviousDoseAdministered(YesNo.YES);
    fromImmediate.setFromTargetDoseNumberInSeries("");

    SeriesDose attemptedSeriesDose = new SeriesDose();
    attemptedSeriesDose.setDoseNumber("4");
    TargetDose attemptedTarget = new TargetDose(attemptedSeriesDose);
    attemptedTarget.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
    Evaluation notValid = new Evaluation();
    notValid.setEvaluationStatus(EvaluationStatus.NOT_VALID);
    notValid.setEvaluationReason(EvaluationReason.TOO_SOON);
    attemptedTarget.setEvaluation(notValid);

    VaccineDoseAdministered notValidDose = new VaccineDoseAdministered();
    notValidDose.setDateAdministered(date("09/01/2026"));
    notValidDose.setEvaluatedAgainstTargetDose(attemptedTarget);

    AntigenAdministeredRecord previousAar = new AntigenAdministeredRecord();
    previousAar.setAntigen(measles);
    previousAar.setDateAdministered(date("09/01/2026"));
    previousAar.setVaccineDoseAdministered(notValidDose);

    TargetDose skippedPrevious = new TargetDose(seriesDoseOne);
    skippedPrevious.setTargetDoseStatus(TargetDoseStatus.SKIPPED);

    dataModel.setPreviousTargetDose(skippedPrevious);
    dataModel.setPreviousAntigenAdministeredRecord(previousAar);
    dataModel.getSelectedAntigenAdministeredRecordList().add(previousAar);

    build();

    assertEquals("FORECASTDT-1: CALCDTINT-4 from the Not Valid last shot (09/01/2026) plus "
        + "'6 months' is later than that shot's own date", date("03/01/2027"),
        step.computeEarliestDate());
    assertEquals("FORECASTDT-5: the adjusted recommended date is pulled to that earliest date",
        date("03/01/2027"), step.computeAdjustedRecommendedDate());
  }

  /**
   * CALCDTINT-1 / FORECASTDT-1 after 6.3: the immediate previous VDA is
   * inadvertent, so the 6-month interval measures from the last Valid dose
   * (03/10/2024 + 6 months = 09/10/2024), not from the inadvertent date
   * (04/10/2024 + 6 months = 10/10/2024) and not assumed 01/01/1900 (which
   * would leave FORECASTDTCAN-1's last-administered candidate 04/10/2024 as
   * earliest). 4.4 leaves previousTargetDose as the Valid dose and
   * previousAAR as the inadvertent shot.
   */
  @Test
  public void forecastdtOneDoesNotMeasureIntervalFromAnInadvertentPreviousDose() {
    Interval fromImmediate = interval("6 months", null, null);
    fromImmediate.setFromImmediatePreviousDoseAdministered(YesNo.YES);
    fromImmediate.setFromTargetDoseNumberInSeries("");

    AntigenAdministeredRecord doseOneAar = new AntigenAdministeredRecord();
    doseOneAar.setAntigen(measles);
    doseOneAar.setDateAdministered(date(DOSE_ONE_ADMINISTERED));
    doseOneAar.setVaccineDoseAdministered(doseOneAdministered);

    TargetDose inadvertentTarget = new TargetDose(seriesDoseTwo);
    inadvertentTarget.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
    Evaluation inadvertentEval = new Evaluation();
    inadvertentEval.setEvaluationStatus(EvaluationStatus.NOT_VALID);
    inadvertentEval.setEvaluationReason(EvaluationReason.INADVERTENT_ADMINISTRATION);
    inadvertentTarget.setEvaluation(inadvertentEval);

    VaccineDoseAdministered inadvertentDose = new VaccineDoseAdministered();
    inadvertentDose.setDateAdministered(date("04/10/2024"));
    inadvertentDose.setInadvertentAdministration(true);
    inadvertentDose.setEvaluatedAgainstTargetDose(inadvertentTarget);

    AntigenAdministeredRecord inadvertentAar = new AntigenAdministeredRecord();
    inadvertentAar.setAntigen(measles);
    inadvertentAar.setDateAdministered(date("04/10/2024"));
    inadvertentAar.setVaccineDoseAdministered(inadvertentDose);

    dataModel.setPreviousAntigenAdministeredRecord(inadvertentAar);
    dataModel.getSelectedAntigenAdministeredRecordList().add(doseOneAar);
    dataModel.getSelectedAntigenAdministeredRecordList().add(inadvertentAar);
    dataModel.setSelectedAntigenAdministeredRecordPos(2);

    build();

    assertEquals("FORECASTDT-1: CALCDTINT-4 from the Valid dose (03/10/2024) plus '6 months' "
        + "is later than the inadvertent shot's own date", date("09/10/2024"),
        step.computeEarliestDate());
  }

  /**
   * <strong>FORECASTDT-1</strong> through FORECASTDTCAN-1's fourth candidate,
   * "seasonal recommendation start date": a dose that may only be given inside a
   * season cannot be given before the season opens.
   */
  @Test
  public void forecastdtOneTheEarliestDateAccountsForTheSeasonalRecommendationStartDate() {
    // Season must contain the assessment date (06/15/2025); otherwise
    // SeasonalRecommendationDates projects the template to another year.
    // Start after the other earliest-date candidates so season start wins.
    seasonalRecommendation("09/01/2025", "03/31/2026");
    dataModel.setAssessmentDate(date("10/15/2025"));

    build();

    assertEquals("FORECASTDT-1: the candidate earliest date includes the seasonal recommendation "
        + "start date (09/01/2025)", date("09/01/2025"), step.computeEarliestDate());
  }

  /**
   * <strong>FORECASTDT-1</strong> through FORECASTDTCAN-1's third candidate,
   * "latest of all forecast conflict end dates" (CALCDTLIVE-4). The date itself
   * is calculated from the live virus conflict Supporting Data by another
   * section; what FORECASTDT-1 owns is that the value, once calculated, is one of
   * the dates the earliest date is the latest of - so the attribute is set
   * directly here.
   */
  @Test
  public void forecastdtOneTheEarliestDateAccountsForTheLatestConflictEndIntervalDate() {
    build();
    setAttribute("Latest Conflict End Interval Date", date("08/01/2026"));

    assertEquals("FORECASTDT-1: the candidate earliest date includes the latest conflict end "
        + "interval date (08/01/2026)", date("08/01/2026"), step.computeEarliestDate());
  }

  /**
   * CALCDTLIVE-4 from a previous live-virus dose that is not on the antigen
   * being forecast. A Valid MMR on 08/31/2026 plus the MMR→Varicella minimum
   * conflict end interval of 28 days floors Varicella Dose 1 at 09/28/2026,
   * even though there is no previous Varicella AAR.
   */
  @Test
  public void calcdtliveFourFloorsTheEarliestDateAtAPreviousLiveVirusConflictEnd() {
    preferableVaccine("21", YesNo.YES, "12 months", "18 years");
    liveVirusConflict("03", "21", "1 day", "28 days", "28 days");
    historyDose("08/31/2026", "03", EvaluationStatus.VALID);

    build();

    assertEquals("CALCDTLIVE-4: Valid MMR 08/31/2026 plus minConflictEnd 28 days",
        date("09/28/2026"), step.computeEarliestDate());
  }

  /**
   * CALCDTCONFLICT-2's second branch: a previous dose that is not Valid takes
   * the conflict end interval, not the minimum. Not Valid Varicella on
   * 09/01/2026 plus 28 days (not the 24-day minimum) is 09/29/2026.
   */
  @Test
  public void calcdtliveFourUsesConflictEndIntervalWhenPreviousIsNotValid() {
    preferableVaccine("21", YesNo.YES, "12 months", "18 years");
    liveVirusConflict("21", "21", "1 day", "24 days", "28 days");
    historyDose("09/01/2026", "21", EvaluationStatus.NOT_VALID);

    build();

    assertEquals("CALCDTCONFLICT-2: Not Valid previous dose takes conflictEndInterval 28 days, "
        + "not minConflictEndInterval 24 days", date("09/29/2026"), step.computeEarliestDate());
  }

  /**
   * <strong>FORECASTDT-1</strong> through FORECASTDTCAN-1's sixth candidate,
   * "date administered of the most recent vaccine dose administered being
   * evaluated against a target dose that is part of a patient series that is the
   * basis of the patient series forecast" - the next dose cannot be earlier than
   * the last dose the patient already received.
   */
  @Test
  public void forecastdtOneTheEarliestDateAccountsForTheMostRecentDateAdministered() {
    AntigenAdministeredRecord record = new AntigenAdministeredRecord();
    record.setAntigen(measles);
    record.setDateAdministered(date(DOSE_ONE_ADMINISTERED));
    record.setVaccineDoseAdministered(doseOneAdministered);
    dataModel.getSelectedAntigenAdministeredRecordList().add(record);

    build();

    assertEquals("FORECASTDT-1: the candidate earliest date includes the date administered of the "
        + "most recent dose already given (03/10/2024)", date(DOSE_ONE_ADMINISTERED),
        step.computeEarliestDate());
  }

  /**
   * <strong>FORECASTDT-1, the whole point of the rule.</strong> The candidate
   * earliest date is a single value produced once by FORECASTDTCAN-1, and 7.4
   * already calculated it - it is Table 7-9's own "Calculated date
   * (FORECASTDTCAN-1) / Candidate Earliest Date" attribute, and 7.4's Table 7-10
   * Rule 1 only reaches 7.5 at all after testing that value against the maximum
   * age date. FORECASTDT-1 says 7.5's earliest date "must be the candidate
   * earliest date" - the same date, not a second opinion about it.
   *
   * <p>
   * This test builds both steps from one fixture and compares. A difference means
   * the gate 7.4 applied ("can this patient still receive the dose before they
   * age out?") was applied to a different date from the one the patient is
   * ultimately told.
   */
  @Test
  public void forecastdtOneTheEarliestDateIsTheSameCandidateEarliestDateSevenFourTested() {
    seasonalRecommendation("09/01/2025", "03/31/2026");
    dataModel.setAssessmentDate(date("10/15/2025"));

    DetermineForecastNeed sevenFour = new DetermineForecastNeed(dataModel);
    Date candidateEarliestDate = null;
    for (ConditionAttribute<?> conditionAttribute : sevenFour.getConditionAttributeList()) {
      if ("Candidate Earliest Date".equalsIgnoreCase(conditionAttribute.getAttributeName())) {
        candidateEarliestDate = (Date) conditionAttribute.getFinalValue();
      }
    }
    assertNotNull("7.4's Table 7-9 carries the candidate earliest date", candidateEarliestDate);

    build();

    assertEquals("FORECASTDT-1: 7.5's earliest date must be the candidate earliest date 7.4 "
        + "calculated for this same patient series - the two must be one date, and where they "
        + "differ it is 7.4's copy of FORECASTDTCAN-1 that omits candidates (see 07-04's Review "
        + "Findings), not 7.5's", candidateEarliestDate, step.computeEarliestDate());
  }

  // ================= Table 7-13 FORECASTDT-2, the unadjusted recommended date

  /**
   * <strong>FORECASTDT-2, first bullet.</strong> "The unadjusted recommended date
   * of a patient series forecast must be ... the earliest recommended age date."
   * With an earliest recommended age of "4 years" the date is 01/15/2019, and it
   * wins outright: the first bullet has no condition attached to it.
   */
  @Test
  public void forecastdtTwoTheUnadjustedRecommendedDateIsTheEarliestRecommendedAgeDate() {
    earliestRecommendedAge("4 years");
    interval(null, "8 weeks", null);

    build();

    assertEquals("FORECASTDT-2's first bullet: the earliest recommended age date (01/15/2019), "
        + "which takes precedence over any earliest recommended interval date",
        date("01/15/2019"), step.computeUnadjustedRecommendedDate());
  }

  /**
   * <strong>FORECASTDT-2, second bullet.</strong> "The latest of all earliest
   * recommended interval dates if there is no earliest recommended age date."
   * Dose 2 here carries two intervals from dose 1 (given 03/10/2024) - "8 weeks"
   * (05/05/2024) and "12 weeks" (06/02/2024) - and no earliest recommended age,
   * so the later of the two is the unadjusted recommended date.
   */
  @Test
  public void forecastdtTwoFallsBackToTheLatestEarliestRecommendedIntervalDate() {
    interval("4 weeks", "8 weeks", null);
    interval("4 weeks", "12 weeks", null);

    build();

    assertEquals("FORECASTDT-2's second bullet: the latest of the two earliest recommended "
        + "interval dates (05/05/2024 and 06/02/2024)", date("06/02/2024"),
        step.computeUnadjustedRecommendedDate());
  }

  /**
   * <strong>FORECASTDT-2, third bullet.</strong> "The earliest date of the
   * patient series forecast if there is no earliest recommended age date or
   * earliest recommended interval date." This is the ordinary case for the great
   * majority of series doses, which recommend no particular age and define no
   * earliest recommended interval: the recommendation is simply "as soon as it
   * can be given".
   *
   * <p>
   * The fixture's dose 2 has a minimum age of "12 months" and nothing else, so
   * the forecast's own earliest date is 01/15/2016 and that is what the rule says
   * the unadjusted recommended date must be. Anything anchored to when the engine
   * happens to be run rather than to the patient's own dates would make the same
   * patient's forecast change every day.
   */
  @Test
  public void forecastdtTwoFallsBackToTheForecastsOwnEarliestDate() {
    minimumAge("12 months");

    build();

    assertEquals("FORECASTDT-2's third bullet: with no earliest recommended age date and no "
        + "earliest recommended interval date, the unadjusted recommended date is the earliest "
        + "date of this patient series forecast (01/15/2016)", step.computeEarliestDate(),
        step.computeUnadjustedRecommendedDate());
  }

  // =================== Table 7-13 FORECASTDT-3, the unadjusted past due date

  /**
   * <strong>FORECASTDT-3, first bullet.</strong> "The latest recommended age date
   * minus 1 day." A latest recommended age of "6 years" on a patient born
   * 01/15/2015 gives a latest recommended age date of 01/15/2021, so the dose
   * becomes past due the day before: 01/14/2021.
   */
  @Test
  public void forecastdtThreeTheUnadjustedPastDueDateIsTheLatestRecommendedAgeDateMinusOneDay() {
    latestRecommendedAge("6 years");

    build();

    assertEquals("FORECASTDT-3's first bullet: the latest recommended age date (01/15/2021) minus "
        + "1 day", date("01/14/2021"), step.computeUnadjustedPastDueDate());
  }

  /**
   * <strong>FORECASTDT-3, second bullet.</strong> "The latest of all latest
   * recommended interval dates minus 1 day if there is no latest recommended age
   * date." Dose 2 here carries two intervals from dose 1 (given 03/10/2024) -
   * "6 weeks" (04/21/2024) and "10 weeks" (05/19/2024): the later is 05/19/2024,
   * so the unadjusted past due date is 05/18/2024.
   */
  @Test
  public void forecastdtThreeFallsBackToTheLatestLatestRecommendedIntervalDateMinusOneDay() {
    interval("4 weeks", null, "6 weeks");
    interval("4 weeks", null, "10 weeks");

    build();

    assertEquals("FORECASTDT-3's second bullet: the latest of the two latest recommended interval "
        + "dates (04/21/2024 and 05/19/2024) minus 1 day", date("05/18/2024"),
        step.computeUnadjustedPastDueDate());
  }

  /**
   * <strong>FORECASTDT-3, third bullet.</strong> "Blank if there is no latest
   * recommended age date or latest recommended interval date." A dose with no
   * recommended-by date never becomes past due, and the forecast has to say so by
   * leaving the date empty rather than inventing one.
   */
  @Test
  public void forecastdtThreeIsBlankWithoutALatestRecommendedAgeOrIntervalDate() {
    build();

    assertNull("FORECASTDT-3's third bullet: no latest recommended age date and no latest "
        + "recommended interval date means a blank unadjusted past due date",
        step.computeUnadjustedPastDueDate());
  }

  /**
   * The same third bullet, for the other way an interval can fail to produce a
   * latest recommended interval date: the interval defines a minimum interval but
   * carries no latest recommended interval at all. FORECASTDT-3 then falls to its
   * third bullet and the unadjusted past due date is blank - the presence of an
   * interval is not itself a latest recommended interval date.
   *
   * <p>
   * The domain model permits this shape ({@code Interval.latestRecommendedInterval}
   * is only assigned when the Supporting Data element is present) even though the
   * bundled 4.65-508 release happens not to exercise it: all 512 of its loaded
   * intervals carry a {@code <latestRecInt>} element, empty or otherwise. So this
   * is a latent case, not one the current release reaches.
   */
  @Test
  public void forecastdtThreeIsBlankWhenAnIntervalDefinesNoLatestRecommendedInterval() {
    interval("4 weeks", "8 weeks", null);

    build();

    assertNull("FORECASTDT-3's third bullet: an interval with no latest recommended interval "
        + "contributes no latest recommended interval date, so the unadjusted past due date is "
        + "blank", step.computeUnadjustedPastDueDate());
  }

  // ======================== Table 7-13 FORECASTDT-4, the latest date

  /**
   * <strong>FORECASTDT-4, first bullet.</strong> "The maximum age date minus 1
   * day if there is a maximum age date." A maximum age of "18 years" on a patient
   * born 01/15/2015 means the last day the dose can validly be given is
   * 01/14/2033 - the day before they age out.
   */
  @Test
  public void forecastdtFourTheLatestDateIsTheMaximumAgeDateMinusOneDay() {
    maximumAge("18 years");

    build();

    assertEquals("FORECASTDT-4's first bullet: the maximum age date (01/15/2033) minus 1 day",
        date("01/14/2033"), computeLatestDate());
  }

  /**
   * <strong>FORECASTDT-4, second bullet.</strong> "Blank if there is no maximum
   * age date." Most series doses have no maximum age at all, and a forecast for
   * one must report no latest date rather than a manufactured one.
   */
  @Test
  public void forecastdtFourIsBlankWithoutAMaximumAgeDate() {
    build();

    assertNull("FORECASTDT-4's second bullet: no maximum age date means a blank latest date",
        computeLatestDate());
  }

  // ================= Table 7-13 FORECASTDT-5, the adjusted recommended date

  /**
   * <strong>FORECASTDT-5, second bullet.</strong> "The unadjusted recommended
   * date of the patient series forecast if it is after the earliest date." The
   * patient here can have the dose from birth (minimum age "0 days") but it is
   * recommended at "4 years", so the recommendation stands as-is: 01/15/2019.
   */
  @Test
  public void forecastdtFiveTheAdjustedRecommendedDateKeepsALaterUnadjustedDate() {
    earliestRecommendedAge("4 years");

    build();

    assertEquals("FORECASTDT-5's second bullet: the unadjusted recommended date (01/15/2019) is "
        + "after the earliest date (01/15/2015), so it is kept", date("01/15/2019"),
        step.computeAdjustedRecommendedDate());
  }

  /**
   * <strong>FORECASTDT-5, first bullet.</strong> "The earliest date of the
   * patient series forecast." This is the "patient has not adhered to the
   * preferred schedule" case the section's Purpose describes: the dose was
   * recommended at "4 years" (01/15/2019) but cannot be given before a minimum
   * age of "8 years" (01/15/2023), so the recommendation is pulled forward to the
   * earliest date, which is later.
   */
  @Test
  public void forecastdtFiveTheAdjustedRecommendedDateFallsBackToTheEarliestDate() {
    minimumAge("8 years");
    earliestRecommendedAge("4 years");

    build();

    assertEquals("FORECASTDT-5's first bullet: the unadjusted recommended date (01/15/2019) is "
        + "not after the earliest date (01/15/2023), so the adjusted recommended date is the "
        + "earliest date", date("01/15/2023"), step.computeAdjustedRecommendedDate());
  }

  // =================== Table 7-13 FORECASTDT-6, the adjusted past due date

  /**
   * <strong>FORECASTDT-6, first bullet.</strong> "The later of the earliest date
   * of the patient series forecast and the unadjusted past due date of the
   * patient series forecast if there is an unadjusted past due date." Here the
   * unadjusted past due date (01/14/2025, from a latest recommended age of "10
   * years") is the later of the two, so it stands.
   */
  @Test
  public void forecastdtSixTheAdjustedPastDueDateIsTheLaterOfTheTwoDates() {
    latestRecommendedAge("10 years");

    build();

    assertEquals("FORECASTDT-6's first bullet: the later of the earliest date (01/15/2015) and "
        + "the unadjusted past due date (01/14/2025)", date("01/14/2025"),
        step.computeAdjustedPastDueDate());
  }

  /**
   * The other direction of FORECASTDT-6's first bullet: a dose the patient is
   * already past due for but cannot yet receive. The unadjusted past due date is
   * 01/14/2018 (latest recommended age "3 years") but the dose cannot be given
   * before the minimum age of "12 years" (01/15/2027), so the adjusted past due
   * date is the earliest date.
   */
  @Test
  public void forecastdtSixTheAdjustedPastDueDateIsPushedToTheEarliestDate() {
    minimumAge("12 years");
    latestRecommendedAge("3 years");

    build();

    assertEquals("FORECASTDT-6's first bullet: the earliest date (01/15/2027) is later than the "
        + "unadjusted past due date (01/14/2018)", date("01/15/2027"),
        step.computeAdjustedPastDueDate());
  }

  /**
   * <strong>FORECASTDT-6, second bullet.</strong> "Blank if there is no
   * unadjusted past due date." A dose that never becomes past due (FORECASTDT-3
   * left the unadjusted date blank) must not acquire one by adjustment.
   */
  @Test
  public void forecastdtSixIsBlankWithoutAnUnadjustedPastDueDate() {
    build();

    assertNull("FORECASTDT-6's second bullet: no unadjusted past due date means a blank adjusted "
        + "past due date", step.computeAdjustedPastDueDate());
  }

  // ================ Table 7-13 FORECASTRECVAC-1, FORECASTDN-1, FORECASTGUIDANCE-1

  /**
   * <strong>FORECASTRECVAC-1.</strong> "A series dose vaccine must be considered
   * a recommended series dose vaccine for a patient series forecast if all the
   * following are true: the series dose vaccine is a preferable vaccine; the
   * forecast vaccine type flag of the series dose vaccine is 'Y'; there is no
   * vaccine contraindication involving the vaccine type ...; [and the earliest
   * date or the adjusted recommended date] is on or after the preferable vaccine
   * type begin age date and before the preferable vaccine type end age date."
   *
   * <p>
   * This is half of what the section's own title promises - "and Recommended
   * Vaccines" - and the reason Table 7-12 carries the Vaccine Type (CVX) and
   * Forecast Vaccine Type Flag attributes at all. The fixture gives the dose
   * being forecast two preferable vaccines, one forecastable and in the age
   * window and one not, so a correct implementation has something to choose
   * between; the assertion is only that the forecast ends up carrying a
   * recommended-vaccine list at all, since the four bullets cannot be tested
   * individually until something produces one.
   */
  @Test
  public void forecastrecvacOneIdentifiesTheRecommendedSeriesDoseVaccines() {
    preferableVaccine("03", YesNo.YES, "12 months", "13 years");
    preferableVaccine("94", YesNo.NO, "12 months", "13 years");

    build();

    assertNotNull("FORECASTRECVAC-1: 7.5 must identify the recommended series dose vaccines for "
        + "the forecast, but neither Forecast nor the step exposes any recommended-vaccine list",
        accessorMatching("(?i)get.*recommended.*vaccine.*|get.*vaccine.*recommend.*"));
  }

  /**
   * <strong>FORECASTDN-1.</strong> "The forecast dose number for a patient series
   * forecast must be calculated as ... the count of all target doses plus 1 where
   * ... the target dose is part of the relevant patient series [and] the target
   * dose has a target dose status of 'Satisfied'."
   *
   * <p>
   * It is the number the patient is actually told ("you are due for dose 3"), and
   * it is deliberately not the same as the series dose number of the target dose
   * being forecast: a series dose can be skipped, substituted or satisfied out of
   * order, so the count of satisfied target doses and the position of the dose in
   * the series can diverge. The fixture's series has one satisfied target dose,
   * so the forecast dose number is 2.
   */
  @Test
  public void forecastdnOneIsTheCountOfSatisfiedTargetDosesPlusOne() {
    assertNotNull("FORECASTDN-1: a patient series forecast must carry a forecast dose number, but "
        + "Forecast has no dose number at all",
        accessorMatching("(?i)get.*dose.*number.*"));
  }

  /**
   * <strong>FORECASTGUIDANCE-1.</strong> "Administrative guidance included in a
   * forecast made for a patient must include all the following: administrative
   * guidance pertaining to any antigen series that defines the regimen for a
   * recommended antigen; administrative guidance pertaining to any indication for
   * which there is an active patient observation for the patient; administrative
   * guidance pertaining to any contraindication for which there is an active
   * patient observation for the patient."
   *
   * <p>
   * The section's Purpose names it too - "additional detail, such as
   * administrative guidance for providers may also be included" - so a forecast
   * has to have somewhere to put it.
   */
  @Test
  public void forecastguidanceOneIncludesAdministrativeGuidance() {
    assertNotNull("FORECASTGUIDANCE-1: a forecast must be able to carry the administrative "
        + "guidance the rule requires, but Forecast has no guidance field",
        accessorMatching("(?i)get.*guidance.*"));
  }

  // ============================================================ State changes

  /**
   * The step package's State Changes: {@code computeDates(forecast)} assigns the
   * results of the six FORECASTDT rules onto the {@link Forecast}. These four are
   * the dates a forecast reports - earliest, adjusted recommended, adjusted past
   * due and latest - for a dose with a minimum age of "12 months", an earliest
   * recommended age of "4 years", a latest recommended age of "6 years" and a
   * maximum age of "18 years".
   */
  @Test
  public void computeDatesAssignsTheReportedDatesOntoTheForecast() {
    minimumAge("12 months");
    earliestRecommendedAge("4 years");
    latestRecommendedAge("6 years");
    maximumAge("18 years");

    build();
    computeDates(forecast);

    assertEquals("FORECASTDT-1 on the forecast: the earliest date", date("01/15/2016"),
        forecast.getEarliestDate());
    assertEquals("FORECASTDT-5 on the forecast: the adjusted recommended date",
        date("01/15/2019"), forecast.getAdjustedRecommendedDate());
    assertEquals("FORECASTDT-6 on the forecast: the adjusted past due date", date("01/14/2021"),
        forecast.getAdjustedPastDueDate());
    assertEquals("FORECASTDT-4 on the forecast: the latest date", date("01/14/2033"),
        forecast.getLatestDate());
  }

  /**
   * FORECASTDT-2 and FORECASTDT-3 define the unadjusted recommended date and the
   * unadjusted past due date as dates "of a patient series forecast" in their own
   * right, not as intermediate values - Figure 7-7's timeline prints both, and
   * {@link Forecast} carries a field and accessor for each. A forecast that
   * records only the adjusted pair loses the distinction the two rules exist to
   * draw: whether the recommended date the patient is given is the schedule's own
   * date or one pulled forward because they are behind.
   */
  @Test
  public void computeDatesAlsoRecordsTheUnadjustedDates() {
    minimumAge("12 years");
    earliestRecommendedAge("4 years");
    latestRecommendedAge("6 years");

    build();
    computeDates(forecast);

    assertEquals("FORECASTDT-2 on the forecast: the unadjusted recommended date (01/15/2019), "
        + "which the adjustment moved to 01/15/2027", date("01/15/2019"),
        forecast.getUnadjustedRecommendedDate());
    assertEquals("FORECASTDT-3 on the forecast: the unadjusted past due date (01/14/2021)",
        date("01/14/2021"), forecast.getUnadjustedPastDueDate());
  }

  /**
   * The step package's other two State Changes: {@code process()} adds the
   * completed forecast to {@code dataModel.getForecastList()} and hands it to the
   * patient series currently being forecast, which is how 7.6 and Chapter 8 find
   * it.
   */
  @Test
  public void processPublishesTheForecastToTheDataModelAndThePatientSeries() throws Exception {
    minimumAge("12 months");

    build();
    step.process();

    List<Forecast> forecastList = dataModel.getForecastList();
    assertTrue("7.5 adds the forecast it built to the data model's forecast list",
        forecastList.contains(forecast));
    assertSame("7.5 sets the forecast on the patient series being forecast", forecast,
        patientSeries.getForecast());
    assertEquals("the published forecast carries FORECASTDT-1's earliest date",
        date("01/15/2016"), forecast.getEarliestDate());
  }

  /**
   * The step package's Next Steps: 7.5 transitions unconditionally to 7.6
   * Validate Recommendation. The specification states no transition rule for this
   * section, so what is asserted is the documented implementation behaviour - one
   * outgoing edge, taken every time, with no condition on it.
   */
  @Test
  public void theStepIsUnconditionallyFollowedBySevenSix() throws Exception {
    build();
    step.process();

    assertEquals("7.5 transitions unconditionally to 7.6 Validate Recommendation",
        LogicStepType.VALIDATE_RECOMMENDATION, step.getNextLogicStepType());
    assertEquals("7.6 is Validate Recommendation", "7.6",
        LogicStepType.VALIDATE_RECOMMENDATION.getChapter());
  }
}
