package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.BirthDateImmunity;
import org.openimmunizationsoftware.cdsi.core.domain.ClinicalHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Exclusion;
import org.openimmunizationsoftware.cdsi.core.domain.Immunity;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.ObservationCode;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientObservation;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.w3c.dom.Document;

/**
 * Section 7.2 "Determine Evidence of Immunity" (Logic Specification for ACIP
 * Recommendations v4.6, page 73) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/07-02-determine-evidence-of-immunity/index.md}.
 *
 * <p>
 * 7.2 has no business rules of its own. Its whole content is Table 7-2
 * (Immunity Attributes) and Table 7-3 (Does the Patient have Evidence of
 * Immunity?), a five-rule decision table, plus the state change the two "Immune"
 * rules make: the patient series' status becomes {@code IMMUNE} and the forecast
 * carries the reason "Patient has evidence of immunity". Control flow is
 * unconditional - all five rules continue to 7.3 - so the outcome is state
 * carried forward, read later by 7.4's own "does the patient have evidence of
 * immunity?" condition.
 *
 * <h2>The two immunity paths the specification describes</h2>
 *
 * <p>
 * "A patient may be considered immune due to their clinical history or if they
 * were born before a defined date for the given target disease." The
 * specification's worked example is measles: immune with a clinical finding of
 * "Measles immune", <em>or</em> born before 01/01/1957. Table 7-3 Rule 1 is the
 * clinical-history path; Rules 2, 3 and 4 are the birth-date path, qualified by
 * an exclusion condition and a country of birth; Rule 5 is the plain "born on or
 * after the cutoff" answer.
 *
 * <p>
 * Every fixture below is built from the immunity element the bundled 4.65-508
 * release actually ships for Measles, so the dates and codes here are real:
 * clinical history guideline 020 "Laboratory Evidence of Immunity for Measles",
 * immunity birth date 01/01/1957, an empty {@code <birthCountry/>}, and one
 * exclusion, 055 "Health care personnel".
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads and calls
 * {@code process()} directly. {@code process()} ends by constructing 7.3
 * {@code DetermineContraindications}, which is inert here (no Supporting Data
 * release is loaded and {@code process()} is never called on it), but which does
 * read the patient's medical history in its constructor - hence the medical
 * history in {@code setUp()}. The step's decision table is a {@code private}
 * inner class, so it is read through the {@link LogicTable} base type from
 * {@code getLogicTableList()}.
 */
public class DetermineEvidenceOfImmunityTest {

  // ---- The Measles immunity element as the 4.65-508 release actually ships it.

  /** {@code <immunityBirthDate>01/01/1957</immunityBirthDate>}. */
  private static final String MEASLES_IMMUNITY_BIRTH_DATE = "01/01/1957";
  /** {@code <birthCountry/>} - Measles ships an empty birth country. */
  private static final String MEASLES_IMMUNITY_COUNTRY = "";
  /** {@code <guidelineCode>020</guidelineCode>}. */
  private static final String MEASLES_GUIDELINE_CODE = "020";
  private static final String MEASLES_GUIDELINE_TITLE = "Laboratory Evidence of Immunity for Measles";
  /** {@code <exclusionCode>055</exclusionCode>}. */
  private static final String HEALTH_CARE_PERSONNEL = "055";

  /** Before the measles immunity birth date. */
  private static final String BORN_BEFORE_CUTOFF = "06/15/1950";
  /** On or after the measles immunity birth date. */
  private static final String BORN_AFTER_CUTOFF = "06/15/1990";

  private static final String IMMUNE_FORECAST_REASON = "Patient has evidence of immunity";

  private DataModel dataModel;
  private Patient patient;
  private Antigen measles;
  private PatientSeries patientSeries;
  private TargetDose targetDose;
  private DetermineEvidenceOfImmunity step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    patient = new Patient();
    patient.setDateOfBirth(date(BORN_AFTER_CUTOFF));
    patient.setCountryOfBirth(MEASLES_IMMUNITY_COUNTRY);
    patient.getMedicalHistory().setImmunizationHistory(new ImmunizationHistory());
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date("06/15/2025"));

    SeriesDose seriesDose = new SeriesDose();
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    // 7.2's constructor reads the current patient series' target disease, so the
    // step cannot exist without one. The specification's own worked example is
    // measles, so that is the antigen throughout.
    measles = new Antigen();
    measles.setName("Measles");
    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName("Measles 2 dose series");
    antigenSeries.setTargetDisease(measles);
    patientSeries = new PatientSeries(antigenSeries);
    dataModel.getPatientSeriesStepper().add(patientSeries);
    dataModel.getPatientSeriesStepper().increment();

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

  /**
   * The Measles immunity element from the bundled release, built by hand:
   * clinical history 020, birth date 01/01/1957, empty birth country, and - only
   * when {@code withExclusion} - exclusion 055 "Health care personnel".
   */
  private static Immunity measlesImmunity(boolean withExclusion) {
    return immunity(MEASLES_IMMUNITY_BIRTH_DATE, MEASLES_IMMUNITY_COUNTRY, withExclusion);
  }

  private static Immunity immunity(String immunityBirthDate, String countryOfBirth,
      boolean withExclusion) {
    Immunity immunity = new Immunity();

    ClinicalHistory clinicalHistory = new ClinicalHistory();
    clinicalHistory.setImmunityGuidelineCode(MEASLES_GUIDELINE_CODE);
    clinicalHistory.setImmunityGuidelineTitle(MEASLES_GUIDELINE_TITLE);
    immunity.getClinicalHistoryList().add(clinicalHistory);

    BirthDateImmunity birthDateImmunity = new BirthDateImmunity();
    birthDateImmunity.setImmunityBirthDate(date(immunityBirthDate));
    birthDateImmunity.setCountryOfBirth(countryOfBirth);
    if (withExclusion) {
      Exclusion exclusion = new Exclusion();
      exclusion.setExclusionCode(HEALTH_CARE_PERSONNEL);
      exclusion.setExclusionTitle("Health care personnel");
      birthDateImmunity.getExclusionList().add(exclusion);
    }
    immunity.getBirthDateImmunityList().add(birthDateImmunity);

    return immunity;
  }

  /** Puts an immunity element where 7.2's decision table looks for one. */
  private void supportingDataImmunity(Immunity immunity) {
    List<Immunity> immunityList = new ArrayList<Immunity>();
    immunityList.add(immunity);
    dataModel.setImmunityList(immunityList);
  }

  /** Records an observation on the patient - e.g. "Health care personnel". */
  private void patientObservation(String code, String text) {
    ObservationCode observationCode = new ObservationCode();
    observationCode.setCode(code);
    observationCode.setText(text);
    PatientObservation patientObservation = new PatientObservation();
    patientObservation.setObservationCode(observationCode);
    patientObservation.setObservationDate(dataModel.getAssessmentDate());
    patient.getMedicalHistory().getPatientObservationList().add(patientObservation);
  }

  private LogicStep run() throws Exception {
    step = new DetermineEvidenceOfImmunity(dataModel);
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  private LogicTable tableSevenThree() {
    assertEquals("7.2 builds exactly one decision table", 1, step.getLogicTableList().size());
    return step.getLogicTableList().get(0);
  }

  /** The result Table 7-3's condition at {@code row} answered for this fixture. */
  private LogicResult conditionResult(int row) {
    return tableSevenThree().getLogicConditions()[row].getLogicResult();
  }

  private ConditionAttribute<?> attribute(String attributeName) {
    for (ConditionAttribute<?> conditionAttribute : step.getConditionAttributeList()) {
      if (conditionAttribute != null
          && attributeName.equalsIgnoreCase(conditionAttribute.getAttributeName())) {
        return conditionAttribute;
      }
    }
    return null;
  }

  private void assertImmune(String because) {
    assertEquals(because, PatientSeriesStatus.IMMUNE, patientSeries.getPatientSeriesStatus());
    assertEquals("an immune outcome sets the forecast reason", IMMUNE_FORECAST_REASON,
        dataModel.getForecast().getForecastReason());
  }

  private void assertNotImmune(String because) {
    assertFalse(because + " (patient series status was " + patientSeries.getPatientSeriesStatus()
        + ")", PatientSeriesStatus.IMMUNE.equals(patientSeries.getPatientSeriesStatus()));
    assertFalse("a not-immune outcome records no evidence-of-immunity forecast reason",
        IMMUNE_FORECAST_REASON.equalsIgnoreCase(
            String.valueOf(dataModel.getForecast().getForecastReason())));
  }

  /**
   * Labels are compared with whitespace and the punctuation the specification and
   * the implementation disagree about removed - "Table 7-2" against "Table 7.2"
   * is a transcription difference, not a behavioural one.
   */
  private static void assertLabelIs(String expected, String actual) {
    assertEquals("expected label '" + expected + "' but was '" + actual + "'",
        normalized(expected), normalized(actual));
  }

  private static String normalized(String label) {
    if (label == null) {
      return null;
    }
    return label.replaceAll("\\s+", "").replace(".", "").replace("-", "")
        .replace("?", "").toLowerCase();
  }

  // ================================================= What 7.2's own class is

  /**
   * 7.2's identity: {@code LogicStepFactory} is how the engine reaches it (7.1
   * hands it {@code DETERMINE_EVIDENCE_OF_IMMUNITY}), it publishes chapter "7.2",
   * and it names Table 7-2 as its attribute table. Both factory overloads are
   * checked, since the pipeline's dispatch and the web renderer's use different
   * ones.
   */
  @Test
  public void theFactoryBuildsThisClassForStepSevenTwo() {
    LogicStep built = LogicStepFactory.createLogicStep(
        LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY, dataModel);
    LogicStep builtAgain = LogicStepFactory.createLogicStep(
        LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY, dataModel, true);

    assertTrue("7.2 is built as DetermineEvidenceOfImmunity",
        built instanceof DetermineEvidenceOfImmunity);
    assertTrue("7.2 is built as DetermineEvidenceOfImmunity",
        builtAgain instanceof DetermineEvidenceOfImmunity);
    assertEquals("7.2", built.getLogicStepType().getChapter());
    assertLabelIs("Table 7-2 Immunity Attributes", built.getConditionTableName());
  }

  /**
   * Table 7-2 Immunity Attributes prints four attributes: the patient's Date of
   * Birth, the patient's Country of Birth, the patient's Evidence of Immunity,
   * and the Supporting Data Immunity elements. All four are what the section
   * takes as input, so all four belong on the step's attribute list - that list
   * is the printed table.
   */
  @Test
  public void tableSevenTwoRegistersEveryAttributeItPrints() throws Exception {
    supportingDataImmunity(measlesImmunity(false));

    run();

    assertNotNull("Table 7-2: Patient / Date of Birth", attribute("Date of Birth"));
    assertNotNull("Table 7-2: Patient / Country of Birth", attribute("Country of Birth"));
    assertNotNull("Table 7-2: Patient / Evidence of Immunity", attribute("Evidence of Immunity"));
    assertNotNull("Table 7-2: Supporting Data / Immunity Elements",
        attribute("Immunity Elements"));
  }

  /**
   * The Supporting Data attribute Table 7-2 names is the immunity element "for
   * the given target disease" - the antigen this patient series tracks, not a
   * schedule-wide one. The step's constructor sources it that way, from the
   * forecast's antigen.
   */
  @Test
  public void theImmunityElementsAttributeIsSourcedFromThisSeriesTargetDisease() throws Exception {
    List<Immunity> antigenImmunity = new ArrayList<Immunity>();
    antigenImmunity.add(measlesImmunity(false));
    measles.setImmunityList(antigenImmunity);

    run();

    assertSame("the immunity elements attribute is the target disease's own list",
        measles.getImmunityList(), dataModel.getForecast().getAntigen().getImmunityList());
  }

  /**
   * 7.2 is where Chapter 7's {@code Forecast} object is created and attached to
   * the current patient series - it is the object 7.2's own immune outcome writes
   * its reason into, and the one 7.4 and 7.5 go on to fill in. The antigen it
   * carries is the current patient series' target disease and the target dose is
   * the one under consideration.
   */
  @Test
  public void theStepCreatesTheForecastForTheCurrentPatientSeries() throws Exception {
    run();

    assertNotNull("7.2 creates the forecast", dataModel.getForecast());
    assertSame("the forecast is attached to the current patient series",
        dataModel.getForecast(), patientSeries.getForecast());
    assertEquals("the forecast's antigen is this series' target disease",
        measles, dataModel.getForecast().getAntigen());
    assertSame("the forecast carries the target dose under consideration",
        targetDose, dataModel.getForecast().getTargetDose());
  }

  // ========================================== Table 7-3, as the specification
  // ========================================== prints it

  /**
   * Table 7-3 "Does the Patient have Evidence of Immunity?", four conditions by
   * five rules, exactly as printed:
   *
   * <pre>
   * history contains an immunity guideline   Yes  No   No   No   No
   * date of birth &lt; immunity birth date      -    Yes  Yes  Yes  No
   * has an immunity exclusion condition      -    Yes  No   No   -
   * country of birth == immunity country     -    -    Yes  No   -
   * outcome                                  Imm  Not  Imm  Not  Not
   * </pre>
   */
  @Test
  public void tableSevenThreeIsEncodedExactlyAsTheSpecificationPrintsIt() throws Exception {
    run();

    LogicTable table = tableSevenThree();
    assertLabelIs("Table 7-3 Does the patient have evidence of immunity?", table.getLabel());
    assertEquals("Table 7-3 has four conditions", 4, table.getLogicConditions().length);
    assertEquals("Table 7-3 has five rules", 5, table.getLogicOutcomes().length);

    LogicResult[][] grid = table.getLogicResultTable();
    assertArrayEquals("Table 7-3 condition 1: does the patient history contain one of the "
        + "immunity guidelines?",
        new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.NO,
            LogicResult.NO },
        grid[0]);
    assertArrayEquals("Table 7-3 condition 2: is the patient's date of birth < the immunity "
        + "birth date?",
        new LogicResult[] { LogicResult.ANY, LogicResult.YES, LogicResult.YES, LogicResult.YES,
            LogicResult.NO },
        grid[1]);
    assertArrayEquals("Table 7-3 condition 3: does this patient have an immunity exclusion "
        + "condition?",
        new LogicResult[] { LogicResult.ANY, LogicResult.YES, LogicResult.NO, LogicResult.NO,
            LogicResult.ANY },
        grid[2]);
    assertArrayEquals("Table 7-3 condition 4: is the patient's country of birth the same as the "
        + "immunity country of birth?",
        new LogicResult[] { LogicResult.ANY, LogicResult.ANY, LogicResult.YES, LogicResult.NO,
            LogicResult.ANY },
        grid[3]);
  }

  // ================================================ Table 7-3, rule by rule

  /**
   * <strong>Rule 1 - Immune.</strong> "Does the patient history contain one of
   * the immunity guidelines? Yes" is on its own enough: the remaining three
   * conditions are "-", so a patient with the clinical finding is immune whatever
   * their date or country of birth. This is the specification's own worked
   * example - measles, "a clinical finding of 'Measles immune'" - and the patient
   * here is deliberately born in 1990, well after the 01/01/1957 birth-date
   * cutoff, so the birth-date rules cannot reach the answer instead.
   *
   * <p>
   * The patient's finding is recorded here as an observation carrying the
   * immunity element's own guideline code, 020, which is the only representation
   * the domain model offers. The choice of representation does not change the
   * outcome: the condition's {@code evaluateInternal} is a placeholder that
   * returns {@code NO} unconditionally, so no patient history of any shape can
   * make Rule 1 fire.
   */
  @Test
  public void ruleOneAPatientWhoseHistoryContainsAnImmunityGuidelineIsImmune() throws Exception {
    supportingDataImmunity(measlesImmunity(true));
    patientObservation(MEASLES_GUIDELINE_CODE, MEASLES_GUIDELINE_TITLE);

    run();

    assertEquals("Table 7-3 condition 1 must be able to answer Yes for a patient whose history "
        + "contains one of the immunity guidelines", LogicResult.YES, conditionResult(0));
    assertImmune("Rule 1: a clinical history of immunity is on its own evidence of immunity");
  }

  /**
   * <strong>Rule 2 - Not immune.</strong> Born before the immunity birth date,
   * but the patient has an immunity exclusion condition, so the birth-date path
   * does not apply to them. Here the patient is health care personnel - exclusion
   * 055, the exclusion the Measles element actually ships - and was born in 1950,
   * before 01/01/1957.
   */
  @Test
  public void ruleTwoAPatientWithAnImmunityExclusionConditionIsNotImmune() throws Exception {
    patient.setDateOfBirth(date(BORN_BEFORE_CUTOFF));
    supportingDataImmunity(measlesImmunity(true));
    patientObservation(HEALTH_CARE_PERSONNEL, "Health care personnel");

    run();

    assertEquals(LogicResult.NO, conditionResult(0));
    assertEquals("06/15/1950 is before 01/01/1957", LogicResult.YES, conditionResult(1));
    assertEquals("the patient is health care personnel", LogicResult.YES, conditionResult(2));
    assertNotImmune("Rule 2: an immunity exclusion condition takes the patient off the "
        + "birth-date path");
  }

  /**
   * <strong>Rule 3 - Immune.</strong> The birth-date path proper: born before the
   * immunity birth date, no immunity exclusion condition, and the patient's
   * country of birth is the immunity element's country of birth. The measles
   * element ships an empty {@code <birthCountry/>}, which the patient fixture
   * matches.
   */
  @Test
  public void ruleThreeAPatientBornBeforeTheImmunityBirthDateInThatCountryIsImmune()
      throws Exception {
    patient.setDateOfBirth(date(BORN_BEFORE_CUTOFF));
    supportingDataImmunity(measlesImmunity(false));

    run();

    assertEquals(LogicResult.NO, conditionResult(0));
    assertEquals("06/15/1950 is before 01/01/1957", LogicResult.YES, conditionResult(1));
    assertEquals("the patient has no immunity exclusion condition", LogicResult.NO,
        conditionResult(2));
    assertEquals("the patient's country of birth is the immunity country of birth",
        LogicResult.YES, conditionResult(3));
    assertImmune("Rule 3: born before the immunity birth date in the immunity country of birth");
  }

  /**
   * <strong>Rule 4 - Not immune.</strong> Born before the immunity birth date and
   * with no exclusion condition, but born in a different country from the one the
   * immunity element names - the age-based rule is a statement about a particular
   * country's disease history, so it does not carry over.
   */
  @Test
  public void ruleFourAPatientBornInADifferentCountryIsNotImmune() throws Exception {
    patient.setDateOfBirth(date(BORN_BEFORE_CUTOFF));
    patient.setCountryOfBirth("Canada");
    supportingDataImmunity(measlesImmunity(false));

    run();

    assertEquals(LogicResult.NO, conditionResult(0));
    assertEquals("06/15/1950 is before 01/01/1957", LogicResult.YES, conditionResult(1));
    assertEquals(LogicResult.NO, conditionResult(2));
    assertEquals("'Canada' is not the immunity country of birth", LogicResult.NO,
        conditionResult(3));
    assertNotImmune("Rule 4: the birth-date rule belongs to the immunity country of birth");
  }

  /**
   * <strong>Rule 5 - Not immune.</strong> No clinical history of immunity and
   * born on or after the immunity birth date: neither path applies, and the
   * exclusion condition and country of birth are "-" because they only ever
   * qualify the birth-date path. This is the ordinary answer for almost every
   * patient.
   */
  @Test
  public void ruleFiveAPatientBornOnOrAfterTheImmunityBirthDateIsNotImmune() throws Exception {
    supportingDataImmunity(measlesImmunity(true));

    run();

    assertEquals(LogicResult.NO, conditionResult(0));
    assertEquals("06/15/1990 is not before 01/01/1957", LogicResult.NO, conditionResult(1));
    assertNotImmune("Rule 5: no clinical history and born after the cutoff");
  }

  // ============================== Whose facts each Table 7-3 condition is about

  /**
   * Table 7-3's third condition is "Does <strong>this patient</strong> have an
   * immunity exclusion condition?" - a question about the patient, answered
   * against the exclusion list the immunity element defines. A patient who is not
   * health care personnel does not have the measles element's exclusion
   * condition, so Rule 2 does not apply to them and Rule 3 does: born 06/15/1950,
   * before 01/01/1957, in the immunity country of birth, they are immune.
   *
   * <p>
   * This is the same fixture as the Rule 3 test with one difference - the
   * immunity element carries its real exclusion (055 "Health care personnel")
   * rather than none - and the patient still has no observation recording that
   * condition. It matters against the bundled release rather than only in
   * principle: all four antigens that ship a birth-date immunity element in
   * 4.65-508 (Measles, Mumps and Rubella at 01/01/1957, Varicella at 01/01/1980)
   * define at least one exclusion - Measles/Mumps/Rubella one each, Varicella
   * three (055 Health care personnel, 007 Pregnant, 003 Immunocompromised). If
   * the presence of an exclusion in the Supporting Data answers this condition
   * Yes on its own, then Rule 3 is unreachable for every antigen in the release
   * and the birth-date path can never make anyone immune.
   */
  @Test
  public void theExclusionConditionIsAskedOfThePatientNotOfTheSupportingData() throws Exception {
    patient.setDateOfBirth(date(BORN_BEFORE_CUTOFF));
    supportingDataImmunity(measlesImmunity(true));

    run();

    assertEquals("the patient's history records no exclusion condition, so Table 7-3's third "
        + "condition is No for them even though the immunity element defines one",
        LogicResult.NO, conditionResult(2));
    assertImmune("Rule 3: a patient without the exclusion condition keeps the birth-date path");
  }

  /**
   * The immunity element Table 7-3 is answered against is the one for "the given
   * target disease" - Table 7-2 sources it from the Supporting Data for this
   * patient series' antigen, and the specification's example is explicitly per
   * disease ("for measles ... born before 01/01/1957"). Different antigens ship
   * different cutoffs in the same release: 01/01/1957 for Measles, Mumps and
   * Rubella, 01/01/1980 for Varicella.
   *
   * <p>
   * Here the measles antigen carries its own immunity element and nothing else
   * does, so a patient born 06/15/1950 is immune to measles. The converse case
   * has the same cause and is not asserted separately: were the element taken
   * from somewhere other than the target disease, a Varicella element (01/01/1980)
   * would make a patient born in 1970 "immune" to measles, for which the cutoff
   * is 1957.
   */
  @Test
  public void theImmunityElementUsedIsTheOneForThisPatientSeriesTargetDisease() throws Exception {
    patient.setDateOfBirth(date(BORN_BEFORE_CUTOFF));
    List<Immunity> antigenImmunity = new ArrayList<Immunity>();
    antigenImmunity.add(measlesImmunity(false));
    measles.setImmunityList(antigenImmunity);

    run();

    assertEquals("the measles immunity birth date is 01/01/1957 and the patient was born "
        + "06/15/1950", LogicResult.YES, conditionResult(1));
    assertImmune("Rule 3, answered against the target disease's own immunity element");
  }

  /**
   * Table 7-2 lists the patient's Country of Birth as an input, not as a required
   * one, and the fourth condition's own body opens with a null guard for it - a
   * guard placed after the value has already been dereferenced, so it can never
   * run. A patient whose country of birth is simply not known must still get an
   * answer out of 7.2; here they are born on 06/15/1990, after the cutoff, so
   * Rule 5 applies and the country of birth is "-" in any case.
   */
  @Test
  public void aPatientWithNoRecordedCountryOfBirthStillGetsAnAnswer() throws Exception {
    patient.setCountryOfBirth(null);
    supportingDataImmunity(measlesImmunity(true));

    try {
      run();
    } catch (NullPointerException npe) {
      fail("7.2 must tolerate a patient with no recorded country of birth, but Table 7-3's "
          + "fourth condition threw " + npe);
    }

    assertNotImmune("Rule 5: born after the cutoff, with or without a country of birth");
  }

  // ============================ Where 7.2's Supporting Data actually comes from

  /**
   * The release really does ship the immunity element 7.2 needs, and
   * {@code DataModelLoader} really does parse it. This is the Measles
   * {@code <immunity>} element from {@code AntigenSupportingData- Measles-508.xml}
   * in the bundled 4.65-508 release, copied verbatim, fed to the loader's own
   * private per-element reader.
   */
  @Test
  public void theReleasesImmunityElementIsParsedByTheLoader() throws Exception {
    Schedule schedule = new Schedule();
    readImmunityInto(schedule, MEASLES_IMMUNITY_XML);

    Immunity immunity = schedule.getImmunity();
    assertNotNull("the Measles immunity element is parsed", immunity);
    assertEquals(1, immunity.getClinicalHistoryList().size());
    assertEquals(MEASLES_GUIDELINE_CODE,
        immunity.getClinicalHistoryList().get(0).getImmunityGuidelineCode());
    assertEquals(1, immunity.getBirthDateImmunityList().size());
    BirthDateImmunity birthDateImmunity = immunity.getBirthDateImmunityList().get(0);
    assertEquals(date(MEASLES_IMMUNITY_BIRTH_DATE), birthDateImmunity.getImmunityBirthDate());
    assertEquals(MEASLES_IMMUNITY_COUNTRY, birthDateImmunity.getCountryOfBirth());
    assertEquals(1, birthDateImmunity.getExclusionList().size());
    assertEquals(HEALTH_CARE_PERSONNEL,
        birthDateImmunity.getExclusionList().get(0).getExclusionCode());
  }

  /**
   * Parsing it is not enough: Table 7-2's "Supporting Data Immunity elements" has
   * to arrive somewhere 7.2 can read it. Every immunity-bearing antigen in the
   * bundled 4.65-508 release goes through this same path - six of them ship a
   * populated {@code <immunity>} element (HepA and HepB with clinical history
   * only; Measles, Mumps, Rubella and Varicella with a birth date as well) - so
   * whatever this test says about Measles holds for the whole release.
   *
   * <p>
   * The question asked here is the smallest one that can be asked of 7.2 in
   * isolation: after the loader has read the element, is it reachable from the
   * two places 7.2's decision table and Table 7-2 attribute look - the data
   * model's own immunity list, and the target disease antigen's?
   */
  @Test
  public void theParsedImmunityElementReachesWhereSevenTwoLooksForIt() throws Exception {
    Schedule schedule = new Schedule();
    readImmunityInto(schedule, MEASLES_IMMUNITY_XML);

    assertFalse("Table 7-3's conditions read dataModel.getImmunityList(); a release that ships "
        + "an immunity element must leave one there", dataModel.getImmunityList().isEmpty());
  }

  /** The {@code <immunity>} element of {@code AntigenSupportingData- Measles-508.xml}. */
  private static final String MEASLES_IMMUNITY_XML = ""
      + "<antigenSupportingData>"
      + "  <immunity>"
      + "    <clinicalHistory>"
      + "      <guidelineCode>020</guidelineCode>"
      + "      <guidelineTitle>Laboratory Evidence of Immunity for Measles</guidelineTitle>"
      + "    </clinicalHistory>"
      + "    <dateOfBirth>"
      + "      <immunityBirthDate>01/01/1957</immunityBirthDate>"
      + "      <birthCountry/>"
      + "      <exclusion>"
      + "        <exclusionCode>055</exclusionCode>"
      + "        <exclusionTitle>Health care personnel</exclusionTitle>"
      + "      </exclusion>"
      + "    </dateOfBirth>"
      + "  </immunity>"
      + "</antigenSupportingData>";

  /**
   * Invokes {@code DataModelLoader}'s private per-element reader for
   * {@code <immunity>} directly, so the loader's own parsing is exercised without
   * a whole release having to be loaded.
   */
  private void readImmunityInto(Schedule schedule, String xml) throws Exception {
    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    document.getDocumentElement().normalize();

    Method readImmunity = DataModelLoader.class.getDeclaredMethod("readImmunity", Schedule.class,
        DataModel.class, Document.class);
    readImmunity.setAccessible(true);
    readImmunity.invoke(null, schedule, dataModel, document);
  }

  // ================================================== 7.2's single destination

  /**
   * 7.2 does not branch. Table 7-3's five rules set patient-series status and
   * forecast-reason state; control continues to 7.3 Determine Contraindications
   * either way, and it is 7.4 that later reads the {@code IMMUNE} status back.
   * Both an immune outcome and a not-immune one are run here.
   */
  @Test
  public void everyOutcomeOfTableSevenThreeContinuesToSevenThree() throws Exception {
    supportingDataImmunity(measlesImmunity(true));
    run();
    assertNotImmune("Rule 5");
    assertEquals("a not-immune outcome continues to 7.3 Determine Contraindications",
        LogicStepType.DETERMINE_CONTRAINDICATIONS, step.getNextLogicStepType());

    setUp();
    patient.setDateOfBirth(date(BORN_BEFORE_CUTOFF));
    supportingDataImmunity(measlesImmunity(false));
    run();
    assertImmune("Rule 3");
    assertEquals("an immune outcome continues to 7.3 as well - 7.2 is not a branch point",
        LogicStepType.DETERMINE_CONTRAINDICATIONS, step.getNextLogicStepType());
  }
}
