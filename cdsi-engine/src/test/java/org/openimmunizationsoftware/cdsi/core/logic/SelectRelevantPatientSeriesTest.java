package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Indication;
import org.openimmunizationsoftware.cdsi.core.domain.MedicalHistory;
import org.openimmunizationsoftware.cdsi.core.domain.ObservationCode;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientObservation;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * Section 5.1 "Select Relevant Patient Series" (Logic Specification for ACIP
 * Recommendations v4.6, pages 41-44, Figures 5-2 and 5-3) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/05-01-select-relevant-patient-series/index.md}.
 *
 * <p>
 * Unlike the Chapter 4 loop drivers, 5.1 is where relevance is actually
 * decided. It carries Table 5-2 (nine input attributes with assumed values),
 * Table 5-3 (business rules CALCDTIND-1 and CALCDTIND-2), and two nested
 * decision tables:
 *
 * <ul>
 * <li><b>Table 5-4 "Does the indication apply to the patient?"</b> - asked once
 * per {@code Indication} of an antigen series, over two conditions: does the
 * indication describe any active patient observations (Yes / No / Unknown), and
 * is the indication begin age date &le; assessment date &lt; indication end age
 * date.</li>
 * <li><b>Table 5-5 "Is an antigen series a relevant patient series for a
 * patient?"</b> - asked once per antigen series, over three conditions: is the
 * patient gender one of the series' required genders, is the series type
 * 'Standard' or 'Evaluation Only', and did at least one of the series'
 * indications apply (the aggregate of that series' Table 5-4 answers).</li>
 * </ul>
 *
 * <p>
 * The step builds those tables in its constructor and evaluates them in
 * {@code process()}, so each test hand-builds the minimal
 * {@code DataModel} - a patient, an assessment date, the antigen 4.3's loop has
 * made current, and one or more {@code AntigenSeries} - drives
 * {@code process()} directly, and then inspects either the decision tables the
 * step exposes through {@code getLogicTableList()} or the
 * {@code PatientSeries} objects it left on the patient series stepper. No
 * Supporting Data release is loaded and no other engine step is run;
 * constructing the returned next step (4.3's
 * {@code CreateRelevantPatientSeries}) is inert, and {@code process()} is never
 * called on it here.
 *
 * <p>
 * The step's own inner tables ({@code LTInnerSet}, {@code LT55}) are
 * package-visible, so their condition attributes, their per-indication
 * "applies" flag and their encoded result grids are read directly rather than
 * reflectively.
 *
 * <p>
 * Gender values here use the Supporting Data vocabulary ("Female", "Male")
 * rather than the {@code ForecastInput} vocabulary ("F", "M"), because Table
 * 5-5's first condition compares the patient's gender against the series'
 * required genders by string equality and this unit is not where that
 * normalization would belong.
 */
public class SelectRelevantPatientSeriesTest {

  private static final String HEPB = "HepB";
  private static final String POLIO = "Polio";

  private static final String FEMALE = "Female";
  private static final String MALE = "Male";
  private static final String UNKNOWN_GENDER = "Unknown";

  /** Chronic liver disease, one of HepB's real Risk-series indications. */
  private static final String CHRONIC_LIVER_DISEASE = "015";
  private static final String DIABETES = "014";

  private static final String APPLIES = "Yes. The Indication applies to the patient.";
  private static final String DOES_NOT_APPLY = "No. The Indication does not apply to the patient.";
  private static final String FLAG_FOR_CLINICIAN = "No. The Indication does not apply to the patient; however, "
      + "the Indication Text Description should be made available to the clinician for manual determination.";
  private static final String RELEVANT = "Yes. The antigen series is a relevant patient series for the patient.";
  private static final String NOT_RELEVANT = "No. The antigen series is not a relevant patient series for the patient.";

  private DataModel dataModel;
  private Patient patient;
  private SelectRelevantPatientSeries step;

  @Before
  public void setUp() {
    dataModel = new DataModel();
    patient = new Patient();
    patient.setDateOfBirth(date(2011, 1, 1));
    patient.setGender(FEMALE);
    patient.setMedicalHistory(new MedicalHistory());
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date(2021, 1, 1));
    currentAntigen(antigen(HEPB));
  }

  // ---------------------------------------------------------------- fixtures

  private static Date date(int year, int month, int day) {
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(year, month - 1, day);
    return calendar.getTime();
  }

  private static Date date(String monthDayYear) {
    try {
      return new SimpleDateFormat("MM/dd/yyyy").parse(monthDayYear);
    } catch (java.text.ParseException pe) {
      throw new IllegalArgumentException(pe);
    }
  }

  private Antigen antigen(String name) {
    return dataModel.getOrCreateAntigen(name);
  }

  /** Stands in for 4.3's loop having made {@code antigen} the current one. */
  private void currentAntigen(Antigen antigen) {
    List<Antigen> antigenSelectedList = new ArrayList<Antigen>();
    antigenSelectedList.add(antigen);
    dataModel.setAntigenSelectedList(antigenSelectedList);
    dataModel.setAntigenSelectedPos(0);
  }

  private AntigenSeries series(String seriesName, String antigenName, SeriesType seriesType,
      Indication... indications) {
    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setTargetDisease(antigen(antigenName));
    antigenSeries.setSeriesType(seriesType);
    for (Indication indication : indications) {
      antigenSeries.getIndicationList().add(indication);
    }
    dataModel.getAntigenSeriesList().add(antigenSeries);
    return antigenSeries;
  }

  private AntigenSeries series(String seriesName, SeriesType seriesType, Indication... indications) {
    return series(seriesName, HEPB, seriesType, indications);
  }

  private static AntigenSeries requiring(AntigenSeries antigenSeries, String... requiredGenders) {
    antigenSeries.setRequiredGenderList(new ArrayList<String>(Arrays.asList(requiredGenders)));
    return antigenSeries;
  }

  /**
   * One Supporting Data indication. A {@code null} age is an absent element; an
   * empty string is the {@code <endAge/>} shape the real releases use, which
   * {@link TimePeriod} treats as unvalued.
   */
  private static Indication indication(String observationCode, String beginAge, String endAge) {
    Indication indication = new Indication();
    indication.setObservationCode(observationCode(observationCode));
    indication.setDescription("Administer to persons with observation " + observationCode + ".");
    if (beginAge != null) {
      indication.setBeginAge(new TimePeriod(beginAge));
    }
    if (endAge != null) {
      indication.setEndAge(new TimePeriod(endAge));
    }
    return indication;
  }

  private static ObservationCode observationCode(String code) {
    ObservationCode observationCode = new ObservationCode();
    observationCode.setCode(code);
    observationCode.setText("Observation " + code);
    return observationCode;
  }

  /** Records an active patient observation in the patient's medical history. */
  private void activeObservation(String code, Date observedOn) {
    PatientObservation patientObservation = new PatientObservation();
    patientObservation.setObservationCode(observationCode(code));
    patientObservation.setObservationDate(observedOn);
    patient.getMedicalHistory().getPatientObservationList().add(patientObservation);
  }

  private LogicStep run() throws Exception {
    step = new SelectRelevantPatientSeries(dataModel);
    return step.process();
  }

  // ------------------------------------------------------ reading the tables

  /** Every Table 5-4 the step built, in construction order. */
  private List<SelectRelevantPatientSeries.LTInnerSet> tablesFiveFour() {
    List<SelectRelevantPatientSeries.LTInnerSet> tables = new ArrayList<SelectRelevantPatientSeries.LTInnerSet>();
    for (LogicTable logicTable : step.getLogicTableList()) {
      if (!(logicTable instanceof SelectRelevantPatientSeries.LT55)) {
        tables.add((SelectRelevantPatientSeries.LTInnerSet) logicTable);
      }
    }
    return tables;
  }

  /** Every Table 5-5 the step built, in construction order. */
  private List<SelectRelevantPatientSeries.LT55> tablesFiveFive() {
    List<SelectRelevantPatientSeries.LT55> tables = new ArrayList<SelectRelevantPatientSeries.LT55>();
    for (LogicTable logicTable : step.getLogicTableList()) {
      if (logicTable instanceof SelectRelevantPatientSeries.LT55) {
        tables.add((SelectRelevantPatientSeries.LT55) logicTable);
      }
    }
    return tables;
  }

  private SelectRelevantPatientSeries.LTInnerSet onlyTableFiveFour() {
    List<SelectRelevantPatientSeries.LTInnerSet> tables = tablesFiveFour();
    assertEquals("expected exactly one Table 5-4", 1, tables.size());
    return tables.get(0);
  }

  private SelectRelevantPatientSeries.LT55 onlyTableFiveFive() {
    List<SelectRelevantPatientSeries.LT55> tables = tablesFiveFive();
    assertEquals("expected exactly one Table 5-5", 1, tables.size());
    return tables.get(0);
  }

  private static LogicResult conditionResult(LogicTable logicTable, int condition) {
    return logicTable.getLogicConditions()[condition].getLogicResult();
  }

  /** The series names the step decided were relevant, in the order it added them. */
  private List<String> relevantSeriesNames() {
    List<String> names = new ArrayList<String>();
    for (PatientSeries patientSeries : dataModel.getPatientSeriesStepper().getList()) {
      names.add(patientSeries.getTrackedAntigenSeries().getSeriesName());
    }
    return names;
  }

  private int timesLogged(String message) {
    int count = 0;
    for (String logged : step.getLogList()) {
      if (message.equals(logged)) {
        count++;
      }
    }
    return count;
  }

  // ------------------------------------- Table 5-2: attributes and assumptions

  /**
   * Table 5-2 "Select Relevant Patient Series Attributes": nine attributes, each
   * with its attribute type, attribute name and assumed-value-if-empty. The
   * attribute names are compared case-insensitively - the specification writes
   * "Date of Birth" where the implementation writes "Date Of Birth", which is a
   * transcription difference, not a behavioural one.
   */
  @Test
  public void tableFiveTwoAttributesCarryTheSpecifiedTypesNamesAndAssumedValues() throws Exception {
    Indication indication = indication(CHRONIC_LIVER_DISEASE, "2 years", "5 years");
    AntigenSeries antigenSeries = requiring(series("HepB risk series", SeriesType.RISK, indication), FEMALE);

    run();
    SelectRelevantPatientSeries.LTInnerSet table = onlyTableFiveFour();

    assertAttribute(table.caGender, "Patient", "Gender");
    assertEquals("Table 5-2: Gender is assumed 'Unknown' when empty", UNKNOWN_GENDER,
        table.caGender.getAssumedValue());
    assertEquals(FEMALE, table.caGender.getInitialValue());

    assertAttribute(table.caDateOfBirth, "Patient", "Date of Birth");
    assertEquals(date(2011, 1, 1), table.caDateOfBirth.getInitialValue());

    assertAttribute(table.caActivePatientObservations, "Patient history", "Active Patient Observation(s)");
    assertSame("Active patient observations come from the patient's medical history",
        patient.getMedicalHistory(), table.caActivePatientObservations.getInitialValue());

    assertAttribute(table.caRequiredGender, "Supporting Data (Gender)", "Required Gender");
    assertEquals("Table 5-2: an empty Required Gender is assumed to be the patient's own gender",
        Arrays.asList(FEMALE), table.caRequiredGender.getAssumedValue());
    assertSame(antigenSeries.getRequiredGenderList(), table.caRequiredGender.getInitialValue());

    assertAttribute(table.caSeriesType, "Supporting Data (Series Type)", "Series Type");

    assertAttribute(table.caObservationCode, "Supporting Data (Indication)", "Observation Code");
    assertSame(indication.getObservationCode(), table.caObservationCode.getInitialValue());

    assertAttribute(table.caAssessmentDate, "Runtime data", "Assessment Date");
    assertNotNull("Table 5-2: Assessment Date is assumed to be the current date when empty",
        table.caAssessmentDate.getAssumedValue());
    assertEquals(date(2021, 1, 1), table.caAssessmentDate.getInitialValue());

    assertAttribute(table.caIndicationBeginAgeDate, "Calculated date (CALCDTIND-1)", "Indication Begin Age Date");
    assertEquals("Table 5-2: Indication Begin Age Date is assumed 01/01/1900 when empty",
        date("01/01/1900"), table.caIndicationBeginAgeDate.getAssumedValue());

    assertAttribute(table.caIndicationEndAgeDate, "Calculated date (CALCDTIND-2)", "Indication End Age Date");
    assertEquals("Table 5-2: Indication End Age Date is assumed 12/31/2999 when empty",
        date("12/31/2999"), table.caIndicationEndAgeDate.getAssumedValue());
  }

  private static void assertAttribute(
      org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute<?> attribute,
      String attributeType, String attributeName) {
    assertNotNull("Table 5-2 attribute '" + attributeName + "' is missing", attribute);
    assertEquals(attributeType, attribute.getAttributeType());
    assertTrue("Table 5-2 attribute name: expected '" + attributeName + "' but was '"
        + attribute.getAttributeName() + "'", attributeName.equalsIgnoreCase(attribute.getAttributeName()));
  }

  /**
   * Table 5-2 names the sixth attribute "Series type", sourced from Supporting
   * Data (Series Type) - the antigen series' Standard / Evaluation Only / Risk
   * classification, the same value Table 5-5's second condition asks about. The
   * attribute is populated from {@code getSeriesName()} instead, so it carries
   * the series' name.
   *
   * <p>
   * Table 5-5's second condition reads {@code antigenSeries.getSeriesType()}
   * directly rather than going through this attribute, so nothing in 5.1's
   * decisions turns on it today; what it changes is the attribute value the
   * step publishes for its own Table 5-2 rendering.
   */
  @Test
  public void theSeriesTypeAttributeCarriesTheSeriesTypeNotTheSeriesName() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", null));

    run();

    assertEquals("Table 5-2: the Series Type attribute holds the series type", SeriesType.RISK.toString(),
        onlyTableFiveFour().caSeriesType.getFinalValue());
  }

  // ---------------------------------- Table 5-3: CALCDTIND-1 and CALCDTIND-2

  /**
   * CALCDTIND-1: "A patient's indication begin age date must be calculated as
   * the patient's date of birth plus the indication begin age of an
   * indication."
   */
  @Test
  public void calcdtind1SetsTheIndicationBeginAgeDateToDateOfBirthPlusTheIndicationBeginAge() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", "5 years"));

    run();

    assertEquals("CALCDTIND-1: 01/01/2011 + 2 years", date(2013, 1, 1),
        onlyTableFiveFour().caIndicationBeginAgeDate.getFinalValue());
  }

  /**
   * CALCDTIND-2: "A patient's indication end age date must be calculated as the
   * patient's date of birth plus the indication end age of an indication."
   */
  @Test
  public void calcdtind2SetsTheIndicationEndAgeDateToDateOfBirthPlusTheIndicationEndAge() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", "5 years"));

    run();

    assertEquals("CALCDTIND-2: 01/01/2011 + 5 years", date(2016, 1, 1),
        onlyTableFiveFour().caIndicationEndAgeDate.getFinalValue());
  }

  /**
   * Table 5-2's assumed values for the two calculated dates. An indication with
   * no begin age and an empty end age - the {@code <endAge/>} shape the CDC
   * releases actually use - leaves both business rules with nothing to compute,
   * so the window opens at 01/01/1900 and closes at 12/31/2999, i.e. it is
   * always open.
   */
  @Test
  public void anIndicationWithNoAgesFallsBackToTableFiveTwosAssumedDates() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, null, ""));

    run();
    SelectRelevantPatientSeries.LTInnerSet table = onlyTableFiveFour();

    assertEquals(date("01/01/1900"), table.caIndicationBeginAgeDate.getFinalValue());
    assertEquals(date("12/31/2999"), table.caIndicationEndAgeDate.getFinalValue());
    assertEquals("An always-open window satisfies Table 5-4's second condition", LogicResult.YES,
        conditionResult(table, 1));
  }

  // ------------------------------------------------------------- Table 5-4

  /**
   * Table 5-4's first condition, "Does the indication describe any active
   * patient observations?", asked of a patient who has exactly the observation
   * the indication describes. The condition is stubbed out - its body is a
   * {@code return LogicResult.NO} under a "logic condition not yet implemented"
   * comment - so it answers No for every patient, and
   * {@code caActivePatientObservations}, though populated from the patient's
   * medical history, is never read.
   */
  @Test
  public void tableFiveFourConditionOneAnswersYesWhenTheIndicationDescribesAnActivePatientObservation()
      throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", null));
    activeObservation(CHRONIC_LIVER_DISEASE, date(2019, 6, 1));

    run();

    assertEquals("Table 5-4 condition 1: the indication describes an active patient observation",
        LogicResult.YES, conditionResult(onlyTableFiveFour(), 0));
  }

  /**
   * Table 5-4 Rule 1: the indication describes an active patient observation
   * and the indication begin age date &le; assessment date &lt; indication end
   * age date, so "Yes. The Indication applies to the patient."
   */
  @Test
  public void ruleOneAnIndicationDescribingAnActiveObservationInsideTheAgeWindowApplies() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", "20 years"));
    activeObservation(CHRONIC_LIVER_DISEASE, date(2019, 6, 1));

    run();
    SelectRelevantPatientSeries.LTInnerSet table = onlyTableFiveFour();

    assertEquals("Table 5-4 condition 2: 01/01/2013 <= 01/01/2021 < 01/01/2031", LogicResult.YES,
        conditionResult(table, 1));
    assertTrue("Table 5-4 Rule 1: the indication applies to the patient", table.isApplies());
    assertEquals(1, timesLogged(APPLIES));
  }

  /**
   * Table 5-4 Rule 2: the indication does not describe any active patient
   * observation - this patient's only observation is a different one - even
   * though the age window is open, so "No. The Indication does not apply to the
   * patient."
   */
  @Test
  public void ruleTwoAnIndicationDescribingNoActivePatientObservationDoesNotApply() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", "20 years"));
    activeObservation(DIABETES, date(2019, 6, 1));

    run();
    SelectRelevantPatientSeries.LTInnerSet table = onlyTableFiveFour();

    assertEquals(LogicResult.NO, conditionResult(table, 0));
    assertEquals(LogicResult.YES, conditionResult(table, 1));
    assertFalse("Table 5-4 Rule 2: the indication does not apply to the patient", table.isApplies());
    assertEquals(1, timesLogged(DOES_NOT_APPLY));
  }

  /**
   * Table 5-4 Rule 4: the assessment date is outside the indication's age
   * window, so the indication does not apply whatever the observation answer
   * would have been - the patient here does have the observation the indication
   * describes.
   */
  @Test
  public void ruleFourAnIndicationOutsideItsAgeWindowDoesNotApply() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", "5 years"));
    activeObservation(CHRONIC_LIVER_DISEASE, date(2019, 6, 1));

    run();
    SelectRelevantPatientSeries.LTInnerSet table = onlyTableFiveFour();

    assertEquals("01/01/2021 is past the 01/01/2016 end age date", LogicResult.NO, conditionResult(table, 1));
    assertFalse("Table 5-4 Rule 4: the indication does not apply to the patient", table.isApplies());
    assertEquals(1, timesLogged(DOES_NOT_APPLY));
  }

  /**
   * Table 5-4's second condition is written "Is the indication begin age date
   * &le; assessment date &lt; indication end age date?" - closed at the begin
   * age date, open at the end age date. For an indication running from age 2 to
   * age 5 of a patient born 01/01/2011 that is the window
   * [01/01/2013, 01/01/2016).
   */
  @Test
  public void theIndicationAgeWindowIsInclusiveOfItsBeginDateAndExclusiveOfItsEndDate() throws Exception {
    assertEquals("the day before the begin age date is outside the window", LogicResult.NO,
        ageWindowAnswerOn(date(2012, 12, 31)));
    assertEquals("the begin age date itself is inside the window", LogicResult.YES,
        ageWindowAnswerOn(date(2013, 1, 1)));
    assertEquals("the day before the end age date is inside the window", LogicResult.YES,
        ageWindowAnswerOn(date(2015, 12, 31)));
    assertEquals("the end age date itself is outside the window", LogicResult.NO,
        ageWindowAnswerOn(date(2016, 1, 1)));
  }

  private LogicResult ageWindowAnswerOn(Date assessmentDate) throws Exception {
    setUp();
    dataModel.setAssessmentDate(assessmentDate);
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", "5 years"));
    run();
    return conditionResult(onlyTableFiveFour(), 1);
  }

  /**
   * Table 5-4 Rule 3 is the "Unknown" column: it is selected when the
   * observation question cannot be answered - not whenever it is answered No.
   * The implementation encodes Rule 3's first condition as {@code ANY} rather
   * than {@code UNKNOWN}, which makes Rule 3 a strictly weaker copy of Rule 2.
   */
  @Test
  public void tableFiveFourRuleThreeIsGuardedByAnUnknownObservationAnswer() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", null));

    run();

    assertArrayEquals("Table 5-4, first condition across rules 1-4: Yes / No / Unknown / -",
        new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.UNKNOWN, LogicResult.ANY },
        onlyTableFiveFour().getLogicResultTable()[0]);
  }

  /**
   * The observable consequence of the previous test. The Purpose reserves the
   * clinician notification for indications whose relevance "could not be
   * resolved" - Rule 3, the Unknown column. Because Rule 3's first condition is
   * encoded as {@code ANY}, an ordinary Rule 2 indication (observation answer
   * No, age window open) matches both columns, so this step performs Rule 2's
   * and Rule 3's outcomes together and raises the clinician flag for an
   * indication that was resolved perfectly well.
   */
  @Test
  public void anIndicationThatSimplyDoesNotApplyIsNotAlsoFlaggedForClinicianReview() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", "20 years"));

    run();

    assertEquals("Rule 2 was selected", 1, timesLogged(DOES_NOT_APPLY));
    assertEquals("Rule 3's clinician flag belongs to an unresolved indication only", 0,
        timesLogged(FLAG_FOR_CLINICIAN));
  }

  // ------------------------------------------------------------- Table 5-5

  /**
   * Table 5-5 Rule 1: the patient's gender is one of the antigen series'
   * required genders and the series type is 'Standard', so the series is a
   * relevant patient series whether or not any indication applies. A relevant
   * series is instantiated as a {@code PatientSeries} and added to the
   * patient's tracked series (Figure 5-2).
   */
  @Test
  public void ruleOneAStandardSeriesForAPatientOfARequiredGenderIsRelevant() throws Exception {
    AntigenSeries antigenSeries = requiring(series("HepB 3 dose series", SeriesType.STANDARD), FEMALE,
        UNKNOWN_GENDER);

    run();
    SelectRelevantPatientSeries.LT55 table = onlyTableFiveFive();

    assertEquals(LogicResult.YES, conditionResult(table, 0));
    assertEquals(LogicResult.YES, conditionResult(table, 1));
    assertEquals(Arrays.asList("HepB 3 dose series"), relevantSeriesNames());
    assertSame(antigenSeries, dataModel.getPatientSeriesStepper().getList().get(0).getTrackedAntigenSeries());
    assertEquals(1, timesLogged(RELEVANT));
  }

  /**
   * Table 5-5 Rule 1 again, for the other series type it covers: "Antigen
   * series with a Series Type of 'Standard' or 'Evaluation Only' are relevant
   * for all patients of the appropriate gender."
   */
  @Test
  public void ruleOneAlsoCoversAnEvaluationOnlySeries() throws Exception {
    series("HepB evaluation only series", SeriesType.EVALUATION_ONLY);

    run();

    assertEquals(LogicResult.YES, conditionResult(onlyTableFiveFive(), 1));
    assertEquals(Arrays.asList("HepB evaluation only series"), relevantSeriesNames());
  }

  /**
   * Table 5-5 Rule 2: the patient's gender is not one of the antigen series'
   * required genders, so the series is not relevant whatever its series type or
   * indications say.
   *
   * <p>
   * The step builds Table 5-2's {@code caGender} and {@code caRequiredGender}
   * attributes on each Table 5-4 it creates, but never on the Table 5-5 that
   * asks this question, so {@code LT55}'s own copies of both stay null and the
   * condition takes its "no required genders were supplied" early return - Yes,
   * for every patient. Rule 2 is therefore unreachable.
   *
   * <p>
   * Gender-restricted series are not hypothetical: the bundled CDC Supporting
   * Data release 4.65 carries series-level {@code <requiredGender>} elements in
   * every one of its 30 antigen files, including HPV series restricted to
   * Female (plus Unknown) and separate ones restricted to Male.
   */
  @Test
  public void ruleTwoASeriesWhoseRequiredGendersExcludeThePatientIsNotRelevant() throws Exception {
    requiring(series("HPV 2 dose series, male", SeriesType.STANDARD), MALE);

    run();

    assertEquals("Table 5-5 condition 1: a Female patient is not one of the required genders",
        LogicResult.NO, conditionResult(onlyTableFiveFive(), 0));
    assertEquals("Table 5-5 Rule 2: the antigen series is not a relevant patient series",
        new ArrayList<String>(), relevantSeriesNames());
    assertEquals(1, timesLogged(NOT_RELEVANT));
  }

  /**
   * Table 5-5 Rule 3: a Risk series - not 'Standard' or 'Evaluation Only' - is
   * still a relevant patient series when at least one of the indications that
   * drives the need for it applies to the patient. Here the patient has exactly
   * the observation the series' indication describes, inside its age window.
   *
   * <p>
   * The third condition correctly aggregates the series' Table 5-4 answers, but
   * no Table 5-4 can ever answer "applies" while its first condition is
   * hard-wired to No (see
   * {@link #tableFiveFourConditionOneAnswersYesWhenTheIndicationDescribesAnActivePatientObservation}),
   * so Rule 3 is unreachable and every Risk series falls through to Rule 4.
   * Release 4.65 defines 84 Risk series across 25 antigens, driven by roughly
   * 730 indications.
   */
  @Test
  public void ruleThreeARiskSeriesWithAnApplyingIndicationIsRelevant() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", "20 years"));
    activeObservation(CHRONIC_LIVER_DISEASE, date(2019, 6, 1));

    run();
    SelectRelevantPatientSeries.LT55 table = onlyTableFiveFive();

    assertEquals("Table 5-5 condition 3: at least one indication applies to the patient", LogicResult.YES,
        conditionResult(table, 2));
    assertEquals("Table 5-5 Rule 3: the antigen series is a relevant patient series",
        Arrays.asList("HepB risk series"), relevantSeriesNames());
  }

  /**
   * Table 5-5 Rule 4: a Risk series none of whose indications apply to the
   * patient - this patient has no observations at all - is not a relevant
   * patient series, and so is excluded from further processing.
   */
  @Test
  public void ruleFourARiskSeriesWithNoApplyingIndicationIsNotRelevant() throws Exception {
    series("HepB risk series", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", "20 years"));

    run();
    SelectRelevantPatientSeries.LT55 table = onlyTableFiveFive();

    assertEquals(LogicResult.NO, conditionResult(table, 1));
    assertEquals(LogicResult.NO, conditionResult(table, 2));
    assertEquals(new ArrayList<String>(), relevantSeriesNames());
    assertEquals(1, timesLogged(NOT_RELEVANT));
  }

  /**
   * Table 5-5 Rule 4, degenerate case: a Risk series with no indications at all
   * has nothing that could drive the need for it, so the third condition has no
   * Table 5-4 to aggregate and the series is not relevant.
   */
  @Test
  public void aRiskSeriesWithNoIndicationsAtAllIsNotRelevant() throws Exception {
    series("HepB risk series", SeriesType.RISK);

    run();

    assertEquals("no indications means no Table 5-4", 0, tablesFiveFour().size());
    assertEquals(LogicResult.NO, conditionResult(onlyTableFiveFive(), 2));
    assertEquals(new ArrayList<String>(), relevantSeriesNames());
  }

  /**
   * Table 5-2: "Required Gender - Assumed Value if Empty: Gender of the
   * patient." A series that names no required genders is therefore relevant for
   * a patient of any gender, including one whose own gender is unknown.
   *
   * <p>
   * This passes today for a weaker reason than the specification gives - the
   * condition returns Yes for every patient regardless of the attribute, see
   * {@link #ruleTwoASeriesWhoseRequiredGendersExcludeThePatientIsNotRelevant} -
   * so it is pinned as the positive counterpart of that test rather than as
   * independent evidence the assumed value works.
   */
  @Test
  public void aSeriesWithNoRequiredGendersIsRelevantForAPatientOfAnyGender() throws Exception {
    patient.setGender(UNKNOWN_GENDER);
    series("HepB 3 dose series", SeriesType.STANDARD);

    run();

    assertEquals(LogicResult.YES, conditionResult(onlyTableFiveFive(), 0));
    assertEquals(Arrays.asList("HepB 3 dose series"), relevantSeriesNames());
  }

  // ------------------------------------------- scope, structure and next step

  /**
   * Entry Conditions: 5.1 "runs once per antigen, driven by 4.3's loop". Only
   * the antigen series whose target disease is the antigen currently being
   * processed are considered - a series for a different antigen gets no
   * decision table and cannot become a patient series on this pass.
   */
  @Test
  public void onlyAntigenSeriesForTheAntigenCurrentlyBeingProcessedAreConsidered() throws Exception {
    series("HepB 3 dose series", HEPB, SeriesType.STANDARD);
    series("Polio 4 dose series", POLIO, SeriesType.STANDARD);

    run();

    assertEquals("one Table 5-5, for the HepB series only", 1, tablesFiveFive().size());
    assertEquals(Arrays.asList("HepB 3 dose series"), relevantSeriesNames());
  }

  /**
   * The nesting the two tables describe: one Table 5-4 per indication of an
   * antigen series, and one Table 5-5 per antigen series, each Table 5-5
   * labelled with the series it is deciding about.
   */
  @Test
  public void oneTableFiveFourIsBuiltPerIndicationAndOneTableFiveFivePerAntigenSeries() throws Exception {
    series("HepB risk series A", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", null),
        indication(DIABETES, "2 years", null));
    series("HepB risk series B", SeriesType.RISK, indication(CHRONIC_LIVER_DISEASE, "2 years", null));

    run();

    assertEquals("three indications across the two series", 3, tablesFiveFour().size());
    assertEquals(2, tablesFiveFive().size());
    assertTrue(tablesFiveFive().get(0).getLabel().contains("HepB risk series A"));
    assertTrue(tablesFiveFive().get(1).getLabel().contains("HepB risk series B"));
    for (SelectRelevantPatientSeries.LTInnerSet table : tablesFiveFour()) {
      assertEquals("TABLE 5-4 DOES THE INDICATION APPLY TO THE PATIENT?", table.getLabel());
    }
  }

  /**
   * State Changes: every antigen series found relevant becomes its own
   * {@code PatientSeries} on the patient series stepper, in the order the
   * antigen series were considered.
   */
  @Test
  public void everyRelevantSeriesBecomesItsOwnPatientSeriesInAntigenSeriesOrder() throws Exception {
    series("HepB 3 dose series", SeriesType.STANDARD);
    series("HepB 2 dose adolescent series", SeriesType.STANDARD);
    series("HepB evaluation only series", SeriesType.EVALUATION_ONLY);

    run();

    assertEquals(Arrays.asList("HepB 3 dose series", "HepB 2 dose adolescent series", "HepB evaluation only series"),
        relevantSeriesNames());
    assertEquals(3, timesLogged(RELEVANT));
  }

  /**
   * Next Steps: an unconditional return to 4.3, the loop driver, regardless of
   * how many antigen series were found relevant for the current antigen.
   */
  @Test
  public void theStepAlwaysReturnsToCreateRelevantPatientSeries() throws Exception {
    series("HepB 3 dose series", SeriesType.STANDARD);
    assertEquals(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES, run().getLogicStepType());
    assertEquals(1, relevantSeriesNames().size());

    setUp();
    series("HepB risk series", SeriesType.RISK);
    assertEquals("still returns to 4.3 when nothing was found relevant",
        LogicStepType.CREATE_RELEVANT_PATIENT_SERIES, run().getLogicStepType());
    assertEquals(0, relevantSeriesNames().size());

    setUp();
    assertEquals("still returns to 4.3 when the antigen has no series at all",
        LogicStepType.CREATE_RELEVANT_PATIENT_SERIES, run().getLogicStepType());
  }
}
