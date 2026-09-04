package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenContraindication;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.ObservationCode;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientObservation;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineContraindication;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.w3c.dom.Document;

/**
 * Section 7.3 "Determine Contraindications" (Logic Specification for ACIP
 * Recommendations v4.6, pages 74-77) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/07-03-determine-contraindications/index.md}.
 *
 * <p>
 * 7.3 is made of Table 7-4 (Determine Contraindication Attributes), three
 * decision tables - Table 7-5 (does an <em>antigen</em> contraindication apply
 * to the patient?), Table 7-6 (does a <em>vaccine</em> contraindication apply to
 * the patient?) and Table 7-7 (combining both into "is the relevant patient
 * series a contraindicated patient series?") - and Table 7-8's two date business
 * rules, CALCDTCI-1 and CALCDTCI-2. Its one state change is the patient series
 * status becoming Contraindicated, which 7.4's own "is the relevant patient
 * series a contraindicated patient series?" condition reads back.
 *
 * <h2>The two levels the section distinguishes</h2>
 *
 * <p>
 * "Contraindications may be applied at either the antigen or vaccine level." An
 * antigen contraindication "prevents all relevant patient series for that
 * antigen from recommending further vaccination for the patient"; a vaccine
 * contraindication "eliminates a specific vaccine from being forecast". Table
 * 7-7 puts them together: the series is contraindicated if any antigen
 * contraindication applies, <em>or</em> if every preferable vaccine for the
 * series has at least one applying vaccine contraindication.
 *
 * <p>
 * The section also states an explicit conservative default: "in the case where a
 * contraindication cannot be definitively determined to be relevant for a
 * patient, the contraindication will not be applied, but a notification should
 * be made to a clinician". That is Table 7-5's Rule 4 and Table 7-6's Rule 7,
 * whose outcomes name the Contraindication Text Description as the thing the
 * clinician is shown.
 *
 * <h2>Fixtures</h2>
 *
 * <p>
 * Where a contraindication is needed, it is the one the bundled 4.65-508 release
 * actually ships. The antigen-level fixture is RSV's observation 278 "Birth
 * mother received RSV vaccine during pregnancy" - the only one of the release's
 * 250 antigen-level contraindications that carries a real age window (begin age
 * "0 days", end age "8 months"), so it is the one case where CALCDTCI-1 and
 * CALCDTCI-2 have anything to compute from. The vaccine-level fixture is
 * Influenza's observation 003 "Immunocompromised", which contraindicates CVX 111
 * and CVX 333 (live attenuated intranasal influenza).
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads. 7.3's
 * constructor reads only the patient's medical history and the assessment date;
 * {@code process()} additionally ends by constructing 7.4
 * {@code DetermineForecastNeed}, whose own constructor dereferences the target
 * dose's series dose ages, hence the target dose and its {@code Age} in
 * {@code setUp()}. Tests that only need to read attributes or decision tables
 * construct the step without calling {@code process()}.
 */
public class DetermineContraindicationsTest {

  // ---- Table 7-4's two assumed values, as the specification prints them.

  /** Table 7-4: Contraindication Begin Age Date, "Assumed Value if Empty". */
  private static final String ASSUMED_BEGIN_AGE_DATE = "01/01/1900";
  /** Table 7-4: Contraindication End Age Date, "Assumed Value if Empty". */
  private static final String ASSUMED_END_AGE_DATE = "12/31/2999";

  // ---- The RSV antigen-level contraindication, as the 4.65-508 release ships it.

  private static final String RSV_OBSERVATION_CODE = "278";
  private static final String RSV_OBSERVATION_TITLE =
      "Birth mother received RSV vaccine during pregnancy";
  private static final String RSV_CONTRAINDICATION_TEXT =
      "Do not vaccinate if the birth mother received RSV vaccine during pregnancy.";
  private static final String RSV_BEGIN_AGE = "0 days";
  private static final String RSV_END_AGE = "8 months";

  // ---- The Influenza vaccine-level contraindication, as the release ships it.

  private static final String FLU_OBSERVATION_CODE = "003";
  private static final String FLU_OBSERVATION_TITLE = "Immunocompromised";
  private static final String LAIV_CVX = "111";
  private static final String LAIV_TYPE = "influenza, live, trivalent, intranasal";

  private static final String DATE_OF_BIRTH = "01/15/2025";
  private static final String ASSESSMENT_DATE = "06/15/2025";

  private DataModel dataModel;
  private Patient patient;
  private Antigen rsv;
  private PatientSeries patientSeries;
  private SeriesDose seriesDose;
  private DetermineContraindications step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    patient = new Patient();
    patient.setDateOfBirth(date(DATE_OF_BIRTH));
    patient.getMedicalHistory().setImmunizationHistory(new ImmunizationHistory());
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date(ASSESSMENT_DATE));

    rsv = new Antigen();
    rsv.setName("RSV");
    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName("RSV 1 dose series");
    antigenSeries.setTargetDisease(rsv);

    seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("1");
    seriesDose.setAntigenSeries(antigenSeries);
    Age age = new Age();
    age.setSeriesDose(seriesDose);
    age.setMinimugeAge(new TimePeriod("0 days"));
    age.setMaximumAge(new TimePeriod("8 months"));
    seriesDose.getAgeList().add(age);
    antigenSeries.getSeriesDoseList().add(seriesDose);

    dataModel.setTargetDose(new TargetDose(seriesDose));

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
   * RSV's observation 278 contraindication as the release defines it, with the
   * age window CALCDTCI-1/CALCDTCI-2 are supposed to turn into dates.
   */
  private static Contraindication rsvContraindication() {
    Contraindication contraindication = new Contraindication();
    contraindication.setObservationCode(RSV_OBSERVATION_CODE);
    contraindication.setObservationTitle(RSV_OBSERVATION_TITLE);
    contraindication.setContraindicationTextDescription(RSV_CONTRAINDICATION_TEXT);
    contraindication.setContraindicationBeginAge(new TimePeriod(RSV_BEGIN_AGE));
    contraindication.setContraindicationEndAge(new TimePeriod(RSV_END_AGE));
    return contraindication;
  }

  /**
   * Puts a contraindication where the loader puts one - on a {@link Schedule} in
   * the data model's schedule list - so the step has Supporting Data to read.
   */
  private void supportingDataContraindication(Contraindication contraindication) {
    Schedule schedule = new Schedule();
    schedule.setScheduleName(rsv.getName());
    schedule.getContraindicationList().add(contraindication);
    dataModel.getScheduleList().add(schedule);
  }

  /** Records an active observation on the patient - e.g. observation 278. */
  private void patientObservation(String code, String text) {
    ObservationCode observationCode = new ObservationCode();
    observationCode.setCode(code);
    observationCode.setText(text);
    PatientObservation patientObservation = new PatientObservation();
    patientObservation.setObservationCode(observationCode);
    patientObservation.setObservationDate(dataModel.getAssessmentDate());
    patient.getMedicalHistory().getPatientObservationList().add(patientObservation);
  }

  /** Gives the series dose a single preferable vaccine of the named CVX. */
  private PreferrableVaccine preferableVaccine(String cvxCode, String shortDescription) {
    VaccineType vaccineType = new VaccineType();
    vaccineType.setCvxCode(cvxCode);
    vaccineType.setShortDescription(shortDescription);
    PreferrableVaccine preferrableVaccine = new PreferrableVaccine();
    preferrableVaccine.setSeriesDose(seriesDose);
    preferrableVaccine.setVaccineType(vaccineType);
    seriesDose.getPreferrableVaccineList().add(preferrableVaccine);
    return preferrableVaccine;
  }

  private DetermineContraindications build() {
    step = new DetermineContraindications(dataModel);
    return step;
  }

  private LogicStep run() throws Exception {
    build();
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

  private Object finalValueOf(String attributeName) {
    ConditionAttribute<?> conditionAttribute = attribute(attributeName);
    assertNotNull("Table 7-4 lists an attribute named '" + attributeName + "'", conditionAttribute);
    return conditionAttribute.getFinalValue();
  }

  /** The step's decision table whose label names {@code tableNumber}. */
  private LogicTable table(String tableNumber) {
    for (LogicTable logicTable : step.getLogicTableList()) {
      if (logicTable.getLabel() != null
          && normalized(logicTable.getLabel()).contains(normalized(tableNumber))) {
        return logicTable;
      }
    }
    fail("7.3 must build " + tableNumber + ", but the step has "
        + step.getLogicTableList().size() + " decision table(s): " + labels());
    return null;
  }

  private String labels() {
    StringBuilder sb = new StringBuilder();
    for (LogicTable logicTable : step.getLogicTableList()) {
      sb.append(sb.length() == 0 ? "" : ", ").append(logicTable.getLabel());
    }
    return sb.toString();
  }

  private static String normalized(String label) {
    if (label == null) {
      return null;
    }
    return label.replaceAll("\\s+", "").replace(".", "").replace("-", "")
        .replace("?", "").toLowerCase();
  }

  private static void assertLabelIs(String expected, String actual) {
    assertEquals("expected label '" + expected + "' but was '" + actual + "'",
        normalized(expected), normalized(actual));
  }

  private void assertContraindicated(String because) {
    assertEquals(because + " (patient series status was "
        + patientSeries.getPatientSeriesStatus() + ")", PatientSeriesStatus.CONTRAINDICATED,
        patientSeries.getPatientSeriesStatus());
  }

  // ================================================= What 7.3's own class is

  /**
   * 7.3's identity: {@code LogicStepFactory} is how the engine reaches it (7.2
   * hands it {@code DETERMINE_CONTRAINDICATIONS}), it publishes chapter "7.3",
   * and it names Table 7-4 as its attribute table. Both factory overloads are
   * checked, since the pipeline's dispatch and the web renderer's use different
   * ones.
   */
  @Test
  public void theFactoryBuildsThisClassForStepSevenThree() {
    LogicStep built = LogicStepFactory.createLogicStep(
        LogicStepType.DETERMINE_CONTRAINDICATIONS, dataModel);
    LogicStep builtAgain = LogicStepFactory.createLogicStep(
        LogicStepType.DETERMINE_CONTRAINDICATIONS, dataModel, true);

    assertTrue("7.3 is built as DetermineContraindications",
        built instanceof DetermineContraindications);
    assertTrue("7.3 is built as DetermineContraindications",
        builtAgain instanceof DetermineContraindications);
    assertEquals("7.3", built.getLogicStepType().getChapter());
    assertLabelIs("Table 7-4 Determine Contraindication Attributes",
        built.getConditionTableName());
  }

  /**
   * Table 7-4 Determine Contraindication Attributes prints six attributes: the
   * patient's Active Patient Observations and Adverse Reactions, the Supporting
   * Data Contraindication elements, the Processing Data Assessment Date, and the
   * two calculated dates CALCDTCI-1 and CALCDTCI-2 produce. All six are inputs to
   * Tables 7-5, 7-6 and 7-7, so all six belong on the step's attribute list -
   * that list is the printed table.
   */
  @Test
  public void tableSevenFourRegistersEveryAttributeItPrints() {
    build();

    assertNotNull("Table 7-4: Patient / Active Patient Observations",
        attribute("Active Patient Observations"));
    assertNotNull("Table 7-4: Patient / Adverse Reactions", attribute("Adverse Reactions"));
    assertNotNull("Table 7-4: Supporting Data / Contraindication elements",
        attribute("Contraindication Elements"));
    assertNotNull("Table 7-4: Processing data / Assessment Date", attribute("Assessment Date"));
    assertNotNull("Table 7-4: Calculated date (CALCDTCI-1) / Contraindication Begin Age Date",
        attribute("Contraindication Begin Age Date"));
    assertNotNull("Table 7-4: Calculated date (CALCDTCI-2) / Contraindication End Age Date",
        attribute("Contraindication End Age Date"));
  }

  /**
   * Table 7-4 gives the Assessment Date the assumed value "current date" and the
   * step is handed a real one, so the attribute must carry the assessment date
   * under which this patient is being evaluated.
   */
  @Test
  public void tableSevenFoursAssessmentDateAttributeIsTheAssessmentDate() {
    build();

    assertEquals("Table 7-4: Processing data / Assessment Date", date(ASSESSMENT_DATE),
        finalValueOf("Assessment Date"));
  }

  /**
   * Table 7-4 gives the Supporting Data Contraindication elements attribute no
   * assumed value ("-"), which means it has to be filled from the Supporting
   * Data: there is nothing to fall back on, and every condition in Tables 7-5 and
   * 7-6 is a question <em>about a contraindication</em>. Here the release's RSV
   * contraindication is in the data model, exactly where the loader leaves one.
   */
  @Test
  public void tableSevenFoursContraindicationElementsAttributeIsFilledFromSupportingData() {
    supportingDataContraindication(rsvContraindication());

    build();

    assertNotNull("Table 7-4 gives 'Contraindication elements' no assumed value, so the step "
        + "must take its value from the Supporting Data",
        finalValueOf("Contraindication Elements"));
  }

  /**
   * Table 7-4: the Contraindication Begin Age Date's "Assumed Value if Empty" is
   * <strong>01/01/1900</strong> - a date in the past, so that a contraindication
   * defining no begin age is already in force.
   */
  @Test
  public void tableSevenFoursAssumedContraindicationBeginAgeDateIsInThePast() {
    build();

    assertEquals("Table 7-4: Contraindication Begin Age Date, Assumed Value if Empty",
        date(ASSUMED_BEGIN_AGE_DATE),
        attribute("Contraindication Begin Age Date").getAssumedValue());
  }

  /**
   * Table 7-4: the Contraindication End Age Date's "Assumed Value if Empty" is
   * <strong>12/31/2999</strong> - a date in the future, so that a contraindication
   * defining no end age never expires.
   */
  @Test
  public void tableSevenFoursAssumedContraindicationEndAgeDateIsInTheFuture() {
    build();

    assertEquals("Table 7-4: Contraindication End Age Date, Assumed Value if Empty",
        date(ASSUMED_END_AGE_DATE),
        attribute("Contraindication End Age Date").getAssumedValue());
  }

  /**
   * The consequence of the two assumed values above, and the reason they are not
   * a cosmetic detail. Tables 7-5 and 7-6 both ask "is the contraindication begin
   * age date &le; assessment date &lt; contraindication end age date?", and Table
   * 7-5's Rule 5 / Table 7-6's Rule 6 make a No there enough on its own for the
   * contraindication not to apply. With Table 7-4's assumed values - 01/01/1900
   * to 12/31/2999 - that condition is Yes for any patient assessed at any
   * plausible date, which is the point: an age-less contraindication is always in
   * its window.
   *
   * <p>
   * This is the ordinary case in the bundled 4.65-508 release rather than an edge
   * case: of its 392 contraindications (250 antigen-level, 142 vaccine-level),
   * only 5 carry an age at all - 1 antigen-level (RSV 278, "0 days" to "8
   * months") and 4 {@code <contraindicatedVaccine>} entries under 2 Influenza
   * contraindications. The remaining 387 rely entirely on these two assumed
   * values, so if the window they describe is empty rather than universal, every
   * one of them fails the third condition and no contraindication in the release
   * can ever apply.
   */
  @Test
  public void theAssumedContraindicationAgeWindowContainsTheAssessmentDate() {
    build();

    Date beginAgeDate = (Date) finalValueOf("Contraindication Begin Age Date");
    Date endAgeDate = (Date) finalValueOf("Contraindication End Age Date");
    Date assessmentDate = dataModel.getAssessmentDate();

    assertFalse("Tables 7-5/7-6 condition 3: contraindication begin age date (" + beginAgeDate
        + ") must be <= the assessment date (" + assessmentDate + ")",
        beginAgeDate.after(assessmentDate));
    assertTrue("Tables 7-5/7-6 condition 3: the assessment date (" + assessmentDate
        + ") must be < the contraindication end age date (" + endAgeDate + ")",
        assessmentDate.before(endAgeDate));
  }

  // ============================================ Table 7-8, the business rules

  /**
   * <strong>CALCDTCI-1.</strong> "A patient's contraindication begin age date
   * must be calculated as the patient's date of birth plus the contraindication
   * begin age of a contraindication." RSV 278's begin age is "0 days", so for a
   * patient born 01/15/2025 the begin age date is 01/15/2025.
   */
  @Test
  public void calcdtciOneIsTheDateOfBirthPlusTheContraindicationBeginAge() {
    Date beginAgeDate =
        DateRules.CALCDTCI_1.evaluate(dataModel, build(), rsvContraindication());

    assertEquals("CALCDTCI-1: date of birth 01/15/2025 plus a begin age of '0 days'",
        date("01/15/2025"), beginAgeDate);
  }

  /**
   * <strong>CALCDTCI-2.</strong> "A patient's contraindication end age date must
   * be calculated as the patient's date of birth plus the contraindication end
   * age of a contraindication." RSV 278's end age is "8 months", so for a patient
   * born 01/15/2025 the end age date is 09/15/2025.
   */
  @Test
  public void calcdtciTwoIsTheDateOfBirthPlusTheContraindicationEndAge() {
    Date endAgeDate =
        DateRules.CALCDTCI_2.evaluate(dataModel, build(), rsvContraindication());

    assertEquals("CALCDTCI-2: date of birth 01/15/2025 plus an end age of '8 months'",
        date("09/15/2025"), endAgeDate);
  }

  /**
   * Table 7-4 sources the two calculated dates from CALCDTCI-1/CALCDTCI-2, and
   * both rules are defined against "a contraindication" - so the dates the step
   * publishes have to be the ones computed from the contraindication actually in
   * hand, not the assumed values. With the release's RSV contraindication loaded,
   * the begin age date is the patient's date of birth plus "0 days".
   */
  @Test
  public void theStepsContraindicationBeginAgeDateIsComputedFromTheContraindicationInHand() {
    supportingDataContraindication(rsvContraindication());

    build();

    assertEquals("CALCDTCI-1 applied to RSV 278's begin age of '0 days' for a patient born "
        + "01/15/2025", date("01/15/2025"), finalValueOf("Contraindication Begin Age Date"));
  }

  /**
   * The same for CALCDTCI-2: with the release's RSV contraindication loaded, the
   * end age date is the patient's date of birth plus "8 months". This is the
   * whole reason the age window matters clinically - RSV 278 is a
   * contraindication that expires when the infant turns eight months old.
   */
  @Test
  public void theStepsContraindicationEndAgeDateIsComputedFromTheContraindicationInHand() {
    supportingDataContraindication(rsvContraindication());

    build();

    assertEquals("CALCDTCI-2 applied to RSV 278's end age of '8 months' for a patient born "
        + "01/15/2025", date("09/15/2025"), finalValueOf("Contraindication End Age Date"));
  }

  // ==================================== The three decision tables, as printed

  /**
   * Table 7-5 "Does the Antigen Contraindication Apply to the Patient?", three
   * conditions by five rules, exactly as printed:
   *
   * <pre>
   * describes any active patient observations?   Yes  No   No   Unk  -
   * describes any adverse reactions?             No   Yes  No   Unk  -
   * begin age date &le; assessment &lt; end age date   Yes  Yes  Yes  Yes  No
   * outcome                                      App  App  Not  Not* Not
   * </pre>
   *
   * (* Rule 4 is the conservative default: "it could not be determined ...
   * however, the Contraindication Text Description should be made available to
   * the clinician for manual determination.")
   */
  @Test
  public void tableSevenFiveIsEncodedExactlyAsTheSpecificationPrintsIt() {
    build();

    LogicTable tableSevenFive = table("Table 7-5");
    assertLabelIs("Table 7-5 Does the antigen contraindication apply to the patient?",
        tableSevenFive.getLabel());
    assertEquals("Table 7-5 has three conditions", 3,
        tableSevenFive.getLogicConditions().length);
    assertEquals("Table 7-5 has five rules", 5, tableSevenFive.getLogicOutcomes().length);

    LogicResult[][] grid = tableSevenFive.getLogicResultTable();
    assertArrayEquals("Table 7-5 condition 1: does the antigen contraindication describe any "
        + "active patient observations?",
        new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.UNKNOWN,
            LogicResult.ANY },
        grid[0]);
    assertArrayEquals("Table 7-5 condition 2: does the antigen contraindication describe any "
        + "adverse reactions?",
        new LogicResult[] { LogicResult.NO, LogicResult.YES, LogicResult.NO, LogicResult.UNKNOWN,
            LogicResult.ANY },
        grid[1]);
    assertArrayEquals("Table 7-5 condition 3: is the contraindication begin age date <= "
        + "assessment date < contraindication end age date?",
        new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.YES, LogicResult.YES,
            LogicResult.NO },
        grid[2]);
  }

  /**
   * Table 7-6 "Does the Vaccine Contraindication Apply to the Patient?", four
   * conditions by seven rules, exactly as printed. It is Table 7-5 plus a fourth
   * condition - "is the vaccine type of the preferable vaccine one of the
   * contraindicated vaccine types for the contraindication?" - which is what
   * makes a vaccine contraindication remove one vaccine rather than the whole
   * antigen.
   *
   * <pre>
   * describes any active patient observations?   Yes  Yes  No   No   No   -    Unk
   * describes any adverse reactions?             No   No   Yes  Yes  No   -    Unk
   * begin age date &le; assessment &lt; end age date   Yes  Yes  Yes  Yes  -    No   Yes
   * preferable vaccine type is contraindicated?  Yes  No   Yes  No   -    -    Yes
   * outcome                                      App  Not  App  Not  Not  Not  Not*
   * </pre>
   */
  @Test
  public void tableSevenSixIsEncodedExactlyAsTheSpecificationPrintsIt() {
    build();

    LogicTable tableSevenSix = table("Table 7-6");
    assertLabelIs("Table 7-6 Does the vaccine contraindication apply to the patient?",
        tableSevenSix.getLabel());
    assertEquals("Table 7-6 has four conditions", 4, tableSevenSix.getLogicConditions().length);
    assertEquals("Table 7-6 has seven rules", 7, tableSevenSix.getLogicOutcomes().length);

    LogicResult[][] grid = tableSevenSix.getLogicResultTable();
    assertArrayEquals("Table 7-6 condition 1: does the vaccine contraindication describe any "
        + "active patient observations?",
        new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.NO, LogicResult.NO,
            LogicResult.NO, LogicResult.ANY, LogicResult.UNKNOWN },
        grid[0]);
    assertArrayEquals("Table 7-6 condition 2: does the vaccine contraindication describe any "
        + "adverse reactions?",
        new LogicResult[] { LogicResult.NO, LogicResult.NO, LogicResult.YES, LogicResult.YES,
            LogicResult.NO, LogicResult.ANY, LogicResult.UNKNOWN },
        grid[1]);
    assertArrayEquals("Table 7-6 condition 3: is the contraindication begin age date <= "
        + "assessment date < contraindication end age date?",
        new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.YES, LogicResult.YES,
            LogicResult.ANY, LogicResult.NO, LogicResult.YES },
        grid[2]);
    assertArrayEquals("Table 7-6 condition 4: is the vaccine type of the preferable vaccine one "
        + "of the contraindicated vaccine types for the contraindication?",
        new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.YES, LogicResult.NO,
            LogicResult.ANY, LogicResult.ANY, LogicResult.YES },
        grid[3]);
  }

  /**
   * Table 7-7 "Is the Relevant Patient Series a Contraindicated Patient Series?",
   * two conditions by three rules, exactly as printed. This is the table that
   * produces 7.3's only state change.
   *
   * <pre>
   * any antigen contraindications apply?           Yes  No   No
   * all preferable vaccines contraindicated?       -    Yes  No
   * outcome                                        Con  Con  Not
   * </pre>
   */
  @Test
  public void tableSevenSevenIsEncodedExactlyAsTheSpecificationPrintsIt() {
    build();

    LogicTable tableSevenSeven = table("Table 7-7");
    assertLabelIs("Table 7-7 Is the relevant patient series a contraindicated patient series?",
        tableSevenSeven.getLabel());
    assertEquals("Table 7-7 has two conditions", 2,
        tableSevenSeven.getLogicConditions().length);
    assertEquals("Table 7-7 has three rules", 3, tableSevenSeven.getLogicOutcomes().length);

    LogicResult[][] grid = tableSevenSeven.getLogicResultTable();
    assertArrayEquals("Table 7-7 condition 1: are there any antigen contraindications that apply "
        + "to the patient?",
        new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.NO },
        grid[0]);
    assertArrayEquals("Table 7-7 condition 2: do all preferable vaccines for the relevant patient "
        + "series have at least one vaccine contraindication that applies to the patient?",
        new LogicResult[] { LogicResult.ANY, LogicResult.YES, LogicResult.NO },
        grid[1]);
  }

  // ============================== Table 7-7's outcomes - 7.3's one state change

  /**
   * <strong>Table 7-7 Rule 1 - Contraindicated.</strong> An antigen
   * contraindication that applies is on its own enough; the preferable-vaccine
   * condition is "-". "An antigen contraindication prevents all relevant patient
   * series for that antigen from recommending further vaccination for the
   * patient. ... The patient series status will be contraindicated for each
   * relevant patient series."
   *
   * <p>
   * The patient here is a five-month-old whose birth mother received RSV vaccine
   * during pregnancy - observation 278, the release's own RSV antigen
   * contraindication - assessed on 06/15/2025, inside its 0-days-to-8-months
   * window. All three of Table 7-5's Rule 1 conditions are Yes for them.
   */
  @Test
  public void tableSevenSevenRuleOneAnApplyingAntigenContraindicationContraindicatesTheSeries()
      throws Exception {
    supportingDataContraindication(rsvContraindication());
    patientObservation(RSV_OBSERVATION_CODE, RSV_OBSERVATION_TITLE);

    run();

    assertContraindicated("Table 7-7 Rule 1: an antigen contraindication that applies to the "
        + "patient makes the relevant patient series a contraindicated patient series");
  }

  /**
   * <strong>Table 7-7 Rule 2 - Contraindicated.</strong> No antigen
   * contraindication applies, but every preferable vaccine for the series has at
   * least one applying vaccine contraindication: "A relevant patient series
   * should not be forecast if either an antigen contraindication exists, or if
   * all preferable vaccines are contraindicated."
   *
   * <p>
   * The series here has exactly one preferable vaccine, CVX 111 (live attenuated
   * intranasal influenza), and the patient is immunocompromised - observation
   * 003, the release's own Influenza vaccine contraindication, which names CVX
   * 111 among its contraindicated vaccines. With the single preferable vaccine
   * contraindicated, "all" of them are.
   */
  @Test
  public void tableSevenSevenRuleTwoAllPreferableVaccinesContraindicatedContraindicatesTheSeries()
      throws Exception {
    VaccineContraindication vaccineContraindication = new VaccineContraindication();
    vaccineContraindication.setObservationCode(FLU_OBSERVATION_CODE);
    vaccineContraindication.setObservationTitle(FLU_OBSERVATION_TITLE);
    supportingDataContraindication(vaccineContraindication);
    preferableVaccine(LAIV_CVX, LAIV_TYPE);
    patientObservation(FLU_OBSERVATION_CODE, FLU_OBSERVATION_TITLE);

    run();

    assertContraindicated("Table 7-7 Rule 2: every preferable vaccine for the series has an "
        + "applying vaccine contraindication, so the series is a contraindicated patient series");
  }

  /**
   * <strong>Table 7-7 Rule 3 - Not contraindicated.</strong> No antigen
   * contraindication applies and not all preferable vaccines are contraindicated,
   * so the series is not a contraindicated patient series and goes on to be
   * forecast normally. This is the ordinary answer for most patients: here the
   * patient has no recorded observations at all, so no contraindication in the
   * release could describe them.
   */
  @Test
  public void tableSevenSevenRuleThreeNoApplyingContraindicationLeavesTheSeriesForecastable()
      throws Exception {
    supportingDataContraindication(rsvContraindication());
    preferableVaccine(LAIV_CVX, LAIV_TYPE);

    run();

    assertFalse("Table 7-7 Rule 3: with no applying contraindication the relevant patient series "
        + "is not a contraindicated patient series",
        PatientSeriesStatus.CONTRAINDICATED.equals(patientSeries.getPatientSeriesStatus()));
  }

  /**
   * 7.3 does not branch. Table 7-7's three rules set patient-series status; the
   * specification's Table 7-1 puts 7.4 next either way, and it is 7.4's own
   * condition 3 ("is the relevant patient series a contraindicated patient
   * series?") that reads the outcome back. Both a contraindicated outcome and a
   * not-contraindicated one are run here.
   */
  @Test
  public void everyOutcomeOfTableSevenSevenContinuesToSevenFour() throws Exception {
    supportingDataContraindication(rsvContraindication());
    run();
    assertEquals("a not-contraindicated outcome continues to 7.4 Determine Forecast Need",
        LogicStepType.DETERMINE_FORECAST_NEED, step.getNextLogicStepType());

    setUp();
    supportingDataContraindication(rsvContraindication());
    patientObservation(RSV_OBSERVATION_CODE, RSV_OBSERVATION_TITLE);
    run();
    assertEquals("a contraindicated outcome continues to 7.4 as well - 7.3 is not a branch point",
        LogicStepType.DETERMINE_FORECAST_NEED, step.getNextLogicStepType());
  }

  // ========================== Where 7.3's Supporting Data actually comes from

  /**
   * The release really does ship the contraindication elements 7.3 needs, and
   * {@code DataModelLoader} really does read them. This is the
   * {@code <contraindications>} element of
   * {@code AntigenSupportingData- RSV-508.xml} in the bundled 4.65-508 release,
   * cut down to its observation-278 entry and fed to the loader's own private
   * per-element reader. Across the release there are 392 such entries - 250
   * antigen-level under {@code <vaccineGroup>} and 142 vaccine-level under
   * {@code <vaccine>}, in all 30 antigen files.
   */
  @Test
  public void theReleasesContraindicationElementsAreParsedByTheLoader() throws Exception {
    Schedule schedule = new Schedule();
    readContraindicationsInto(schedule, RSV_CONTRAINDICATIONS_XML);

    List<Contraindication> parsed = schedule.getContraindicationList();
    assertEquals("the RSV contraindications element defines one contraindication", 1,
        parsed.size());
    assertEquals(RSV_OBSERVATION_CODE, parsed.get(0).getObservationCode());
    assertEquals(RSV_OBSERVATION_TITLE, parsed.get(0).getObservationTitle());
  }

  /**
   * Parsing it is not enough: Table 7-4's "Supporting Data Contraindication
   * elements" has to arrive somewhere 7.3 can read it. This is the same question
   * 7.2's {@code theParsedImmunityElementReachesWhereSevenTwoLooksForIt} asks of
   * the immunity element, from the contraindication side.
   *
   * <p>
   * 7.3's own source names the place it would look -
   * {@code dataModel.getContraindicationList()} - in the commented-out line that
   * would set the attribute's initial value. Every one of the release's 392
   * contraindications goes through the same loader path, so whatever this test
   * says about RSV holds for all of them.
   */
  @Test
  public void theParsedContraindicationsReachWhereSevenThreeLooksForThem() throws Exception {
    Schedule schedule = new Schedule();
    readContraindicationsInto(schedule, RSV_CONTRAINDICATIONS_XML);

    assertFalse("a release that ships contraindication elements must leave them where 7.3's "
        + "Table 7-4 attribute reads them", dataModel.getContraindicationList().isEmpty());
  }

  /**
   * Table 7-5's Rule 4 and Table 7-6's Rule 7 - the section's stated conservative
   * default - both end "the Contraindication Text Description should be made
   * available to the clinician for manual determination". A description that was
   * never read out of the Supporting Data cannot be shown to anyone, so the
   * loader has to keep it. The release carries a
   * {@code <contraindicationText>} on all 392 of its contraindications.
   */
  @Test
  public void aParsedContraindicationCarriesTheTextDescriptionShownToTheClinician()
      throws Exception {
    Schedule schedule = new Schedule();
    readContraindicationsInto(schedule, RSV_CONTRAINDICATIONS_XML);

    assertEquals("Tables 7-5/7-6's undetermined outcomes hand the Contraindication Text "
        + "Description to the clinician", RSV_CONTRAINDICATION_TEXT,
        schedule.getContraindicationList().get(0).getContraindicationTextDescription());
  }

  /**
   * CALCDTCI-1 and CALCDTCI-2 compute their dates from "the contraindication
   * begin age" and "the contraindication end age" of a contraindication - values
   * that live in the Supporting Data's {@code <beginAge>}/{@code <endAge>}
   * elements. RSV 278 is the release's only antigen-level contraindication that
   * defines them ("0 days" to "8 months"), and it is exactly the case the age
   * window exists for: a contraindication that stops applying when the infant
   * turns eight months old.
   */
  @Test
  public void aParsedAntigenContraindicationCarriesTheAgesCalcdtciNeeds() throws Exception {
    Schedule schedule = new Schedule();
    readContraindicationsInto(schedule, RSV_CONTRAINDICATIONS_XML);

    Contraindication parsed = schedule.getContraindicationList().get(0);
    assertNotNull("CALCDTCI-1 needs RSV 278's begin age of '0 days'",
        parsed.getContraindicationBeginAge());
    assertNotNull("CALCDTCI-2 needs RSV 278's end age of '8 months'",
        parsed.getContraindicationEndAge());
    assertEquals("CALCDTCI-2: date of birth plus the contraindication end age",
        date("09/15/2025"), parsed.getContraindicationEndAge().getDateFrom(date(DATE_OF_BIRTH)));
  }

  /**
   * Table 7-6's fourth condition is "is the vaccine type of the preferable
   * vaccine one of the contraindicated vaccine types for the contraindication?" -
   * the condition that makes a vaccine contraindication remove one vaccine rather
   * than the whole antigen. It can only be answered if the contraindicated
   * vaccine types were read out of the Supporting Data.
   *
   * <p>
   * This is Influenza's observation 003 "Immunocompromised" as the release ships
   * it, naming CVX 111 and CVX 333. Release-wide, the 142 vaccine-level
   * contraindications carry 329 {@code <contraindicatedVaccine>} entries, every
   * one of them with a {@code <cvx>} - so this condition has real data behind it
   * for the entire vaccine half of the section.
   */
  @Test
  public void aParsedVaccineContraindicationCarriesItsContraindicatedVaccineTypes()
      throws Exception {
    Schedule schedule = new Schedule();
    readContraindicationsInto(schedule, INFLUENZA_CONTRAINDICATIONS_XML);

    assertEquals("the Influenza fixture defines one vaccine-level contraindication", 1,
        schedule.getContraindicationList().size());
    Contraindication parsed = schedule.getContraindicationList().get(0);
    assertTrue("Table 7-6 condition 4 needs the contraindicated vaccine types (CVX 111 and CVX "
        + "333) of Influenza's observation 003, but the parsed contraindication is a "
        + parsed.getClass().getSimpleName() + " with no vaccine types on it",
        parsed instanceof VaccineContraindication);
  }

  /**
   * Table 7-7 asks two separate questions - "are there any <em>antigen</em>
   * contraindications that apply?" and "do all preferable vaccines have an
   * applying <em>vaccine</em> contraindication?" - so the two levels have to stay
   * distinguishable after loading. The Supporting Data keeps them apart
   * structurally, under {@code <vaccineGroup>} and {@code <vaccine>}
   * respectively, and the domain model has an
   * {@link AntigenContraindication}/{@link VaccineContraindication} pair for
   * precisely this distinction.
   *
   * <p>
   * The fixture here is one antigen-level and one vaccine-level contraindication
   * in a single {@code <contraindications>} element, the shape 24 of the
   * release's 30 antigen files actually use.
   */
  @Test
  public void antigenAndVaccineContraindicationsStayDistinguishableAfterLoading()
      throws Exception {
    Schedule schedule = new Schedule();
    readContraindicationsInto(schedule, BOTH_LEVELS_CONTRAINDICATIONS_XML);

    assertEquals("the fixture defines one antigen-level and one vaccine-level contraindication",
        2, schedule.getContraindicationList().size());
    int antigenLevel = 0;
    int vaccineLevel = 0;
    for (Contraindication contraindication : schedule.getContraindicationList()) {
      if (contraindication instanceof AntigenContraindication) {
        antigenLevel++;
      } else if (contraindication instanceof VaccineContraindication) {
        vaccineLevel++;
      }
    }
    assertEquals("Table 7-7 condition 1 counts antigen contraindications", 1, antigenLevel);
    assertEquals("Table 7-7 condition 2 counts vaccine contraindications", 1, vaccineLevel);
  }

  // ------------------------------------------------- real Supporting Data markup

  /**
   * The observation-278 entry of {@code AntigenSupportingData- RSV-508.xml}'s
   * {@code <contraindications>} element - the release's only antigen-level
   * contraindication with an age window.
   */
  private static final String RSV_CONTRAINDICATIONS_XML = ""
      + "<antigenSupportingData>"
      + "  <contraindications>"
      + "    <vaccineGroup>"
      + "      <contraindication>"
      + "        <observationCode>278</observationCode>"
      + "        <observationTitle>Birth mother received RSV vaccine during pregnancy"
      + "</observationTitle>"
      + "        <contraindicationText>Do not vaccinate if the birth mother received RSV vaccine"
      + " during pregnancy.</contraindicationText>"
      + "        <contraindicationGuidance/>"
      + "        <beginAge>0 days</beginAge>"
      + "        <endAge>8 months</endAge>"
      + "      </contraindication>"
      + "    </vaccineGroup>"
      + "  </contraindications>"
      + "</antigenSupportingData>";

  /**
   * The observation-003 entry of {@code AntigenSupportingData- Influenza-508.xml}'s
   * {@code <contraindications>/<vaccine>} element.
   */
  private static final String INFLUENZA_CONTRAINDICATIONS_XML = ""
      + "<antigenSupportingData>"
      + "  <contraindications>"
      + "    <vaccine>"
      + "      <contraindication>"
      + "        <observationCode>003</observationCode>"
      + "        <observationTitle>Immunocompromised</observationTitle>"
      + "        <contraindicationText>Do not vaccinate with live attenuated influenza virus"
      + " (LAIV) if the patient is immunocompromised.</contraindicationText>"
      + "        <contraindicationGuidance/>"
      + "        <contraindicatedVaccine>"
      + "          <vaccineType>influenza, live, trivalent, intranasal</vaccineType>"
      + "          <cvx>111</cvx>"
      + "          <beginAge/>"
      + "          <endAge/>"
      + "        </contraindicatedVaccine>"
      + "        <contraindicatedVaccine>"
      + "          <vaccineType>Influenza, live, trivalent, intranasal, self/caregiver admin, PF"
      + "</vaccineType>"
      + "          <cvx>333</cvx>"
      + "          <beginAge/>"
      + "          <endAge/>"
      + "        </contraindicatedVaccine>"
      + "      </contraindication>"
      + "    </vaccine>"
      + "  </contraindications>"
      + "</antigenSupportingData>";

  /**
   * One antigen-level and one vaccine-level contraindication in the same
   * {@code <contraindications>} element - the shape most of the release's antigen
   * files use, here with Influenza's own observation 085 and observation 003.
   */
  private static final String BOTH_LEVELS_CONTRAINDICATIONS_XML = ""
      + "<antigenSupportingData>"
      + "  <contraindications>"
      + "    <vaccineGroup>"
      + "      <contraindication>"
      + "        <observationCode>085</observationCode>"
      + "        <observationTitle>Severe allergic reaction after previous dose of Influenza"
      + "</observationTitle>"
      + "        <contraindicationText>Do not vaccinate if the patient has had a severe allergic"
      + " reaction after a previous dose of Influenza vaccine.</contraindicationText>"
      + "        <contraindicationGuidance/>"
      + "        <beginAge/>"
      + "        <endAge/>"
      + "      </contraindication>"
      + "    </vaccineGroup>"
      + "    <vaccine>"
      + "      <contraindication>"
      + "        <observationCode>003</observationCode>"
      + "        <observationTitle>Immunocompromised</observationTitle>"
      + "        <contraindicationText>Do not vaccinate with live attenuated influenza virus"
      + " (LAIV) if the patient is immunocompromised.</contraindicationText>"
      + "        <contraindicationGuidance/>"
      + "        <contraindicatedVaccine>"
      + "          <vaccineType>influenza, live, trivalent, intranasal</vaccineType>"
      + "          <cvx>111</cvx>"
      + "          <beginAge/>"
      + "          <endAge/>"
      + "        </contraindicatedVaccine>"
      + "      </contraindication>"
      + "    </vaccine>"
      + "  </contraindications>"
      + "</antigenSupportingData>";

  /**
   * Invokes {@code DataModelLoader}'s private per-element reader for
   * {@code <contraindications>} directly, so the loader's own parsing is
   * exercised without a whole release having to be loaded.
   */
  private void readContraindicationsInto(Schedule schedule, String xml) throws Exception {
    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    document.getDocumentElement().normalize();

    Method readContraindications = DataModelLoader.class.getDeclaredMethod("readContraindications",
        Schedule.class, DataModel.class, Document.class);
    readContraindications.setAccessible(true);
    readContraindications.invoke(null, schedule, dataModel, document);
  }
}
