package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Section 6.9 "Evaluate for Allowable Vaccine" (Logic Specification for ACIP
 * Recommendations v4.6, pages 66-69, Figures 6-20/6-21, Figure 6-22, Table 6-28,
 * Table 6-29, Table 6-30) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/06-09-evaluate-for-allowable-vaccine/index.md}.
 *
 * <p>
 * "Evaluate for allowable vaccine validates the vaccine of a vaccine dose
 * administered against the list of allowable vaccines." It is the fallback 6.8
 * routes to when no preferable vaccine matched, and it is the simpler sibling:
 * Table 6-29 asks two questions rather than 6.8's four - vaccine type, then age
 * window - with no trade name and no volume.
 *
 * <pre>
 * Table 6-29 Was the Vaccine Dose Admininstered an Allowable Vaccine for the
 *            Target Dose?   [the specification's own spelling]
 *
 *   Condition                                            R1    R2    R3
 *   Is the vaccine type of the vaccine dose administered
 *     the same as the vaccine type of an allowable
 *     vaccine for the target dose?                       Yes   No    Yes
 *   Is the allowable vaccine type begin age date
 *     &#8804; date administered &lt; allowable vaccine type
 *     end age date?                                      Yes   -     No
 *
 *   Outcome                                              Yes,  No,   No, out of the
 *                                                        allow-not   recommended
 *                                                        able  allow-age range for
 *                                                              able  the allowable
 *                                                                    vaccine
 * </pre>
 *
 * <p>
 * Table 6-30 supplies the two calculated dates the second condition needs:
 *
 * <pre>
 * CALCDTALLOW-1  A patient's allowable vaccine type begin age date must be
 *                calculated as the patient's date of birth plus the vaccine type
 *                begin age of an allowable vaccine.
 * CALCDTALLOW-2  A patient's allowable vaccine type end age date must be
 *                calculated as the patient's date of birth plus the vaccine type
 *                end age of an allowable vaccine.
 * </pre>
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads: a
 * {@code Patient} with a date of birth (both business rules measure from it), the
 * {@code AntigenAdministeredRecord} 4.4 has made current, and the current
 * {@code TargetDose} whose {@code SeriesDose} holds the Supporting Data
 * {@code AllowableVaccine} records.
 *
 * <p>
 * Vaccine types are handed out through {@code DataModel}'s own {@code cvxMap},
 * the way {@code DataModelLoader.readVaccine} and {@code GatherNecessaryData} both
 * obtain theirs, so a CVX code always yields the one shared {@link VaccineType}
 * instance. That detail matters here: Table 6-29's first condition is implemented
 * as a reference comparison rather than a CVX comparison (see
 * {@link #sameVaccineTypeMeansTheSameCvxCode()}).
 *
 * <p>
 * The fixtures are the bundled Supporting Data release's own allowable vaccine
 * shapes, verbatim:
 *
 * <ul>
 * <li>Diphtheria Dose 1's DTaP entry - CVX 20, begin age "6 weeks - 4 days",
 * empty end age - as the standard allowable vaccine most rule-by-rule tests
 * measure against. A patient born 01/01/2015 gets a begin age date of 02/08/2015
 * and, from the empty end age, Table 6-28's assumed 12/31/2999.</li>
 * <li>HepB Dose 1's "Hep B, Adol/peds" entry - CVX 08, begin age "0 days", end age
 * "20 years" - for the birth-dose boundary and for the only bounded age window of
 * the three.</li>
 * <li>RSV Dose 1's "Respiratory syncytial virus (RSV), unspecified" entry - CVX
 * 304, empty begin age <em>and</em> empty end age - for Table 6-28's two assumed
 * values. It is one of exactly three entries in the release with an empty
 * {@code <beginAge/>}, all three of them RSV Dose 1.</li>
 * </ul>
 *
 * <p>
 * 6.9 publishes Table 6-28's five attributes per allowable vaccine into the plain
 * {@code getConditionAttributeList()} (unlike 6.8, which groups them in
 * {@code getConditionAttributesAdditionalMap()}), so that flat list is what these
 * tests read, five entries at a time. Its decision table is a {@code private}
 * inner class, so it is read through the public {@code getLogicTableList()} as a
 * plain {@link LogicTable}, and its "was this an allowable vaccine" answer is read
 * from the step's own public conclusion. Unlike 6.8, that conclusion is <em>not</em>
 * the next step - {@code transitions.yaml} records 6.9 as unconditional to 6.10
 * either way - it is the target dose's {@code statusCause}, to which
 * {@code process()} appends "Vaccine" when no allowable vaccine matched and which
 * 6.10 Satisfy Target Dose reads back.
 *
 * <h2>What is deliberately not asserted</h2>
 *
 * <p>
 * Figure 6-22's loop exit, for the same reason 6.8 left its own alone:
 * {@code process()} evaluates every allowable vaccine's table and only then
 * aggregates, while the process model leaves the loop on the first match. The
 * difference is observable only when one dose can match two of a target dose's
 * allowable vaccines with different outcomes, and it cannot on the bundled
 * release - not one of its 484 series doses lists the same CVX twice among its
 * 3709 allowable vaccine entries.
 *
 * <p>
 * Whether Table 6-29's Rule 3 ought to record an evaluation reason for "out of the
 * recommended age range". The step package's own review findings raise it as a
 * low-confidence reading of the outcome <em>text</em> - Table 6-29 names no state
 * change, and 6.8's equivalent rule sets no reason either. Pinning an
 * interpretation the specification does not state would be guessing;
 * {@link #ruleThreeRecordsNoEvaluationReason()} pins only what the tables actually
 * say, which is that 6.9 leaves the evaluation untouched.
 *
 * <p>
 * The evaluation <em>status</em>. Like 6.5 through 6.8, 6.9 records only a status
 * cause; section 6.10 Satisfy Target Dose is where a status is decided.
 *
 * <p>
 * Figures 6-20 and 6-21, which illustrate two patients rather than adding a rule.
 *
 * <p>
 * The structured log events, for the same reason 6.3 through 6.8 left theirs
 * alone.
 */
public class EvaluateForAllowableVaccineTest {

  /** Table 6-28 row 1. */
  private static final String DATE_ADMINISTERED = "Date Administered";
  /** Table 6-28 row 2. */
  private static final String VACCINE_TYPE = "Vaccine Type";
  /** Table 6-28 row 3. */
  private static final String ALLOWABLE_VACCINE_ELEMENTS = "Allowable Vaccine elements";
  /** Table 6-28 row 4, CALCDTALLOW-1. */
  private static final String BEGIN_AGE_DATE = "Allowable Vaccine Type Begin Age Date";
  /** Table 6-28 row 5, CALCDTALLOW-2. */
  private static final String END_AGE_DATE = "Allowable Vaccine Type End Age Date";

  /** Table 6-28's assumed value if the CALCDTALLOW-1 date is empty. */
  private static final String ASSUMED_BEGIN_AGE_DATE = "01/01/1900";
  /** Table 6-28's assumed value if the CALCDTALLOW-2 date is empty. */
  private static final String ASSUMED_END_AGE_DATE = "12/31/2999";

  /** CVX 20, DTaP - the standard allowable vaccine in the fixtures below. */
  private static final String DTAP = "20";
  /** CVX 08, "Hep B, Adol/peds" - the release's "0 days" begin age shape. */
  private static final String HEPB_PEDS = "08";
  /** CVX 304, "Respiratory syncytial virus (RSV), unspecified" - no age window. */
  private static final String RSV_UNSPECIFIED = "304";
  /** CVX 21, Varicella - not an allowable vaccine for any fixture below. */
  private static final String VARICELLA = "21";

  private static final String DATE_OF_BIRTH = "01/01/2015";
  /** {@value #DATE_OF_BIRTH} plus the DTaP fixture's "6 weeks - 4 days" begin age. */
  private static final String BEGIN_AGE_DATE_VALUE = "02/08/2015";
  /** {@value #DATE_OF_BIRTH} plus the HepB fixture's "20 years" end age. */
  private static final String HEPB_END_AGE_DATE_VALUE = "01/01/2035";
  /** Squarely inside the standard fixture's age window. */
  private static final String INSIDE_THE_AGE_WINDOW = "06/01/2016";

  /** What {@code process()} appends to the target dose's status cause on a miss. */
  private static final String MISSED = "Vaccine";

  /**
   * Table 6-29's grid, transcribed from the specification. Condition rows in Table
   * 6-28 order, rule columns 1 to 3, with a dash written as
   * {@link LogicResult#ANY}.
   */
  private static final LogicResult[][] TABLE_SIX_TWENTY_NINE = {
      { LogicResult.YES, LogicResult.NO, LogicResult.YES },
      { LogicResult.YES, LogicResult.ANY, LogicResult.NO },
  };

  private DataModel dataModel;
  private SeriesDose seriesDose;
  private TargetDose targetDose;
  private EvaluateForAllowableVaccine step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    Patient patient = new Patient();
    patient.setDateOfBirth(date(DATE_OF_BIRTH));
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date("06/01/2021"));
    dataModel.setTargetDoseList(new ArrayList<TargetDose>());

    seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("1");
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    // 6.4 always runs first and always records an evaluation. 6.9 records no
    // evaluation reason of its own, so this is here to prove it leaves it alone.
    Evaluation evaluation = new Evaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.VALID);
    targetDose.setEvaluation(evaluation);

    // The default fixture: the release's own DTaP allowable vaccine, and a matching
    // dose administered inside its age window.
    theStandardAllowableVaccine();
    administered(INSIDE_THE_AGE_WINDOW, DTAP);

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
   * The one shared {@link VaccineType} instance for a CVX code, handed out of
   * {@code DataModel}'s own {@code cvxMap} exactly as {@code DataModelLoader} and
   * {@code GatherNecessaryData} do it in the real pipeline.
   */
  private VaccineType cvx(String cvxCode) {
    VaccineType vaccineType = dataModel.getCvxMap().get(cvxCode);
    if (vaccineType == null) {
      vaccineType = new VaccineType();
      vaccineType.setCvxCode(cvxCode);
      vaccineType.setShortDescription("CVX " + cvxCode);
      dataModel.getCvxMap().put(cvxCode, vaccineType);
    }
    return vaccineType;
  }

  /** Stands in for 4.4 having made this vaccine dose administered the current one. */
  private AntigenAdministeredRecord administered(String monthDayYear, String cvxCode) {
    VaccineType type = cvx(cvxCode);

    Vaccine vaccine = new Vaccine();
    vaccine.setVaccineType(type);

    VaccineDoseAdministered vaccineDoseAdministered = new VaccineDoseAdministered();
    vaccineDoseAdministered.setVaccine(vaccine);
    vaccineDoseAdministered.setDateAdministered(date(monthDayYear));

    AntigenAdministeredRecord aar = new AntigenAdministeredRecord();
    aar.setDateAdministered(date(monthDayYear));
    aar.setVaccineType(type);
    aar.setVaccineDoseAdministered(vaccineDoseAdministered);

    dataModel.setAntigenAdministeredRecord(aar);
    return aar;
  }

  /**
   * A Supporting Data allowable vaccine, written the way
   * {@code DataModelLoader.readVaccine} writes one onto a series dose.
   */
  private AllowableVaccine allowableVaccine(String cvxCode, String beginAge, String endAge) {
    AllowableVaccine allowableVaccine = new AllowableVaccine();
    allowableVaccine.setSeriesDose(seriesDose);
    allowableVaccine.setVaccineType(cvx(cvxCode));
    allowableVaccine.setVaccineTypeBeginAge(new TimePeriod(beginAge));
    allowableVaccine.setVaccineTypeEndAge(new TimePeriod(endAge));
    seriesDose.getAllowableVaccineList().add(allowableVaccine);
    return allowableVaccine;
  }

  /**
   * The bundled release's Diphtheria Dose 1 DTaP allowable vaccine, verbatim: CVX
   * 20, begin age "6 weeks - 4 days", empty end age. For a patient born
   * {@value #DATE_OF_BIRTH} that is an age window from
   * {@value #BEGIN_AGE_DATE_VALUE} to Table 6-28's assumed
   * {@value #ASSUMED_END_AGE_DATE}.
   */
  private AllowableVaccine theStandardAllowableVaccine() {
    return allowableVaccine(DTAP, "6 weeks - 4 days", "");
  }

  /**
   * The bundled release's HepB Dose 1 "Hep B, Adol/peds" allowable vaccine,
   * verbatim: CVX 08, begin age "0 days", end age "20 years".
   */
  private AllowableVaccine theHepBAllowableVaccine() {
    return allowableVaccine(HEPB_PEDS, "0 days", "20 years");
  }

  private void noAllowableVaccines() {
    seriesDose.getAllowableVaccineList().clear();
  }

  private EvaluateForAllowableVaccine construct() {
    step = new EvaluateForAllowableVaccine(dataModel);
    return step;
  }

  private LogicStep run() throws Exception {
    construct();
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  /**
   * Table 6-28's five rows for the {@code index}th allowable vaccine. 6.9 appends
   * them to the plain condition attribute list, one block of five per allowable
   * vaccine.
   */
  private ConditionAttribute<?> attribute(int allowableVaccineIndex, int row) {
    List<ConditionAttribute<?>> attributes = step.getConditionAttributeList();
    int at = allowableVaccineIndex * 5 + row;
    assertTrue("Table 6-28 row " + (row + 1) + " is not registered", attributes.size() > at);
    return attributes.get(at);
  }

  /** Table 6-28's rows for the single allowable vaccine most fixtures define. */
  private ConditionAttribute<?> attribute(int row) {
    return attribute(0, row);
  }

  private LogicTable tableSixTwentyNine() {
    assertEquals("one allowable vaccine means one decision table",
        1, step.getLogicTableList().size());
    return step.getLogicTableList().get(0);
  }

  private LogicResult conditionResult(int condition) {
    return tableSixTwentyNine().getLogicConditions()[condition].getLogicResult();
  }

  /**
   * 6.9's own public conclusion. {@code transitions.yaml}: the transition to 6.10
   * is unconditional, and "a miss is recorded (statusCause += \"Vaccine\") for
   * 6.10, not branched on here" - so the status cause <em>is</em> the "was the
   * vaccine dose administered an allowable vaccine" answer.
   */
  private void assertAllowable(String why) {
    assertEquals(why + " - the target dose's status cause should be untouched",
        "", targetDose.getStatusCause());
  }

  private void assertNotAllowable(String why) {
    assertEquals(why + " - 6.9 should record the miss for 6.10",
        MISSED, targetDose.getStatusCause());
  }

  /**
   * Labels are compared with whitespace, case and punctuation removed -
   * transcription differences, not behavioural ones.
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

  // =================================================== Entry: what 6.9's class is

  /** 6.9 identifies itself as {@code EVALUATE_FOR_ALLOWABLE_VACCINE}. */
  @Test
  public void theStepIsSixNine() throws Exception {
    run();

    assertEquals(LogicStepType.EVALUATE_FOR_ALLOWABLE_VACCINE, step.getLogicStepType());
    assertEquals("6.9", LogicStepType.EVALUATE_FOR_ALLOWABLE_VACCINE.getChapter());
  }

  // ============================================ Figure 6-22: the loop's shape

  /**
   * Figure 6-22's loop is "for each allowable vaccine for the target dose", and
   * each pass applies Table 6-29 to that one allowable vaccine, so the step builds
   * one decision table - and one block of Table 6-28 attributes - per allowable
   * vaccine defined on the target dose. The bundled release runs from 1 to 13
   * allowable vaccines per series dose; three is the release's most common count.
   */
  @Test
  public void oneDecisionTableIsBuiltForEachAllowableVaccine() throws Exception {
    noAllowableVaccines();
    theStandardAllowableVaccine();
    theHepBAllowableVaccine();
    allowableVaccine(RSV_UNSPECIFIED, "", "");

    run();

    assertEquals("three allowable vaccines means three passes round Figure 6-22's loop",
        3, step.getLogicTableList().size());
    assertEquals("and three blocks of Table 6-28's five attributes",
        15, step.getConditionAttributeList().size());
  }

  /** A target dose with no allowable vaccines never enters Figure 6-22's loop. */
  @Test
  public void noAllowableVaccinesMeansNoDecisionTable() throws Exception {
    noAllowableVaccines();

    run();

    assertEquals(0, step.getLogicTableList().size());
    assertEquals(0, step.getConditionAttributeList().size());
  }

  // ============================================ Table 6-28: the attribute table

  /** Table 6-28 "Allowable Vaccine Attributes" has five rows per allowable vaccine. */
  @Test
  public void tableSixTwentyEightPublishesFiveAttributesPerAllowableVaccine() throws Exception {
    run();

    assertEquals("Table 6-28 has five rows", 5, step.getConditionAttributeList().size());
    assertTrue("the attribute table should identify itself as Table 6-28 but was '"
        + step.getConditionTableName() + "'",
        normalized(step.getConditionTableName()).contains("table628"));
  }

  /**
   * Table 6-28 rows 1 and 2: the date administered and the vaccine type of the
   * vaccine dose administered.
   */
  @Test
  public void tableSixTwentyEightCarriesTheVaccineDoseAdministeredAttributes() throws Exception {
    administered(INSIDE_THE_AGE_WINDOW, DTAP);

    run();

    assertLabelIs("Vaccine dose administered", attribute(0).getAttributeType());
    assertLabelIs(DATE_ADMINISTERED, attribute(0).getAttributeName());
    assertEquals(date(INSIDE_THE_AGE_WINDOW), attribute(0).getFinalValue());

    assertLabelIs("Vaccine dose administered", attribute(1).getAttributeType());
    assertLabelIs(VACCINE_TYPE, attribute(1).getAttributeName());
    assertSame("Table 6-28 row 2 carries the administered vaccine's type",
        cvx(DTAP), attribute(1).getFinalValue());
  }

  /**
   * Table 6-28 row 3: the Supporting Data allowable vaccine this pass round Figure
   * 6-22's loop is evaluating.
   */
  @Test
  public void tableSixTwentyEightCarriesTheAllowableVaccineElements() throws Exception {
    noAllowableVaccines();
    AllowableVaccine allowableVaccine = theStandardAllowableVaccine();

    run();

    assertLabelIs("Supporting data", attribute(2).getAttributeType());
    assertLabelIs(ALLOWABLE_VACCINE_ELEMENTS, attribute(2).getAttributeName());
    assertSame("Table 6-28 row 3 carries the allowable vaccine being evaluated",
        allowableVaccine, attribute(2).getFinalValue());
  }

  /** Table 6-28 rows 4 and 5 are named for the dates CALCDTALLOW-1/2 calculate. */
  @Test
  public void tableSixTwentyEightNamesTheCalculatedDateRows() throws Exception {
    run();

    assertLabelIs(BEGIN_AGE_DATE, attribute(3).getAttributeName());
    assertLabelIs(END_AGE_DATE, attribute(4).getAttributeName());
  }

  /**
   * Table 6-28's last two rows carry the attribute type "Calculated date
   * (CALCDTALLOW-1)" and "Calculated date (CALCDTALLOW-2)" - the same shape 6.5,
   * 6.6 and 6.8 use for their own calculated dates.
   */
  @Test
  public void tableSixTwentyEightLabelsTheCalculatedDateRowsAsTheSpecificationDoes()
      throws Exception {
    run();

    assertLabelIs("Calculated date (CALCDTALLOW-1)", attribute(3).getAttributeType());
    assertLabelIs("Calculated date (CALCDTALLOW-2)", attribute(4).getAttributeType());
  }

  /**
   * Table 6-28 row 4's assumed value if empty is 01/01/1900. An allowable vaccine
   * whose {@code <beginAge/>} element is empty has no begin age to calculate
   * CALCDTALLOW-1 from, so the calculated date falls back to the assumed value.
   * Three of the release's 3709 allowable vaccine entries are shaped this way, all
   * three of them RSV Dose 1.
   */
  @Test
  public void tableSixTwentyEightAssumesTheBeginAgeDateIsJanuaryFirstNineteenHundred()
      throws Exception {
    noAllowableVaccines();
    allowableVaccine(RSV_UNSPECIFIED, "", "");
    administered(INSIDE_THE_AGE_WINDOW, RSV_UNSPECIFIED);

    run();

    assertEquals("Table 6-28 row 4's assumed value if empty",
        date(ASSUMED_BEGIN_AGE_DATE), attribute(3).getFinalValue());
  }

  /**
   * Table 6-28 row 5's assumed value if empty is 12/31/2999. 3159 of the bundled
   * release's 3709 allowable vaccine entries have an empty {@code <endAge/>}, so
   * this is the common case rather than the exception - the standard DTaP fixture
   * is one of them.
   */
  @Test
  public void tableSixTwentyEightAssumesTheEndAgeDateIsDecemberThirtyFirstTwentyNineNinetyNine()
      throws Exception {
    run();

    assertEquals("Table 6-28 row 5's assumed value if empty",
        date(ASSUMED_END_AGE_DATE), attribute(4).getFinalValue());
  }

  // ====================================== Table 6-30 CALCDTALLOW-1

  /**
   * CALCDTALLOW-1: "A patient's allowable vaccine type begin age date must be
   * calculated as the patient's date of birth plus the vaccine type begin age of an
   * allowable vaccine."
   *
   * <p>
   * A patient born {@value #DATE_OF_BIRTH} plus the DTaP fixture's "6 weeks - 4
   * days" begin age is {@value #BEGIN_AGE_DATE_VALUE}. "6 weeks - 4 days" is the
   * release's most common allowable vaccine begin age - 1623 of its 3709 entries.
   */
  @Test
  public void calcdtallowOneCalculatesTheAllowableVaccineTypeBeginAgeDate() throws Exception {
    run();

    assertEquals("date of birth " + DATE_OF_BIRTH + " plus a begin age of 6 weeks - 4 days",
        date(BEGIN_AGE_DATE_VALUE), attribute(3).getFinalValue());
  }

  // ====================================== Table 6-30 CALCDTALLOW-2

  /**
   * CALCDTALLOW-2: "A patient's allowable vaccine type end age date must be
   * calculated as the patient's date of birth plus the vaccine type end age of an
   * allowable vaccine."
   *
   * <p>
   * A patient born {@value #DATE_OF_BIRTH} plus the HepB fixture's "20 years" end
   * age is {@value #HEPB_END_AGE_DATE_VALUE}. "20 years" is the release's most
   * common non-empty allowable vaccine end age - 203 of its 550 valued ones.
   */
  @Test
  public void calcdtallowTwoCalculatesTheAllowableVaccineTypeEndAgeDate() throws Exception {
    noAllowableVaccines();
    theHepBAllowableVaccine();
    administered(INSIDE_THE_AGE_WINDOW, HEPB_PEDS);

    run();

    assertEquals("date of birth " + DATE_OF_BIRTH + " plus an end age of 20 years",
        date(HEPB_END_AGE_DATE_VALUE), attribute(4).getFinalValue());
  }

  // ============================================ Table 6-29: the decision table

  /**
   * Table 6-29 "Was the vaccine dose admininstered an allowable vaccine for the
   * target dose?" has two conditions and three rules.
   */
  @Test
  public void theDecisionTableIsTableSixTwentyNine() throws Exception {
    run();

    LogicTable table = tableSixTwentyNine();
    assertTrue("the decision table should identify itself as Table 6-29 but was '"
        + table.getLabel() + "'", normalized(table.getLabel()).contains("table629"));
    assertEquals("Table 6-29 has two conditions", 2, table.getLogicConditions().length);
    assertEquals("Table 6-29 has three rules", 3, table.getLogicOutcomes().length);
  }

  /** Table 6-29's grid, condition by condition and rule by rule. */
  @Test
  public void theDecisionTableGridMatchesTableSixTwentyNine() throws Exception {
    run();

    assertArrayEquals("Table 6-29's condition/rule grid",
        TABLE_SIX_TWENTY_NINE, tableSixTwentyNine().getLogicResultTable());
  }

  // ============================================= Table 6-29 Rule 1

  /**
   * Table 6-29 Rule 1: the same vaccine type as an allowable vaccine, administered
   * inside that allowable vaccine's age window - "Yes. The vaccine dose
   * administered was an allowable vaccine for the target dose."
   */
  @Test
  public void ruleOneReportsAnAllowableVaccine() throws Exception {
    run();

    assertEquals("Table 6-29's first condition", LogicResult.YES, conditionResult(0));
    assertEquals("Table 6-29's second condition", LogicResult.YES, conditionResult(1));
    assertAllowable("a DTaP dose administered inside the DTaP allowable vaccine's age window "
        + "was an allowable vaccine for the target dose");
  }

  /**
   * Table 6-29's second condition is inclusive at its lower bound - "Is the
   * allowable vaccine type begin age date &#8804; date administered &lt; allowable
   * vaccine type end age date?" - so a dose administered on the begin age date
   * itself is inside the age window and the answer is Yes.
   *
   * <p>
   * The DTaP fixture's begin age of "6 weeks - 4 days" gives a begin age date of
   * {@value #BEGIN_AGE_DATE_VALUE} for a patient born {@value #DATE_OF_BIRTH}. This
   * boundary applies to every one of the release's 3709 allowable vaccine entries
   * bar the six that carry no usable begin age.
   */
  @Test
  public void ruleOneIncludesTheBeginAgeDateItself() throws Exception {
    administered(BEGIN_AGE_DATE_VALUE, DTAP);

    run();

    assertEquals("Table 6-29's second condition", LogicResult.YES, conditionResult(1));
    assertAllowable("the allowable vaccine type begin age date itself is inside the window");
  }

  /**
   * The same inclusive lower bound at the boundary that matters most in the bundled
   * release: an allowable vaccine whose begin age is "0 days" puts its begin age
   * date on the patient's date of birth, so a birth dose is inside its age window.
   * 1035 of the release's 3709 allowable vaccine entries have a "0 days" begin age -
   * the second most common value after "6 weeks - 4 days" - and every one of HepB
   * Dose 1's fourteen is one of them, which is where the routine birth dose lands.
   */
  @Test
  public void aDoseAdministeredOnTheDateOfBirthIsInsideAZeroDaysBeginAgeWindow() throws Exception {
    noAllowableVaccines();
    theHepBAllowableVaccine();
    administered(DATE_OF_BIRTH, HEPB_PEDS);

    run();

    assertEquals("Table 6-29's second condition", LogicResult.YES, conditionResult(1));
    assertAllowable("a Hep B birth dose is inside a '0 days' begin age window");
  }

  /**
   * And the upper bound is exclusive, so the day before the end age date is still
   * inside the window. The HepB fixture's end age date is
   * {@value #HEPB_END_AGE_DATE_VALUE}.
   */
  @Test
  public void ruleOneIncludesTheDayBeforeTheEndAgeDate() throws Exception {
    noAllowableVaccines();
    theHepBAllowableVaccine();
    administered("12/31/2034", HEPB_PEDS);

    run();

    assertEquals("Table 6-29's second condition", LogicResult.YES, conditionResult(1));
    assertAllowable("the day before the allowable vaccine type end age date is inside the window");
  }

  /**
   * An allowable vaccine with neither a begin age nor an end age falls back to Table
   * 6-28's two assumed values, which between them span every date a dose can carry -
   * so the age window never excludes it. That is the shape of RSV Dose 1's three
   * unspecified-RSV entries.
   */
  @Test
  public void anAllowableVaccineWithNoAgeWindowNeverFailsTheAgeCondition() throws Exception {
    noAllowableVaccines();
    allowableVaccine(RSV_UNSPECIFIED, "", "");
    administered(DATE_OF_BIRTH, RSV_UNSPECIFIED);

    run();

    assertEquals("Table 6-29's second condition", LogicResult.YES, conditionResult(1));
    assertAllowable("an allowable vaccine with no age window admits any date administered");
  }

  // ============================================= Table 6-29 Rule 2

  /**
   * Table 6-29 Rule 2: the vaccine type of the vaccine dose administered is not the
   * vaccine type of this allowable vaccine, so the age window is not asked ("-") -
   * "No. The vaccine dose administered was not an allowable vaccine for the target
   * dose."
   */
  @Test
  public void ruleTwoReportsNotAllowableWhenTheVaccineTypeDiffers() throws Exception {
    administered(INSIDE_THE_AGE_WINDOW, VARICELLA);

    run();

    assertEquals("Table 6-29's first condition", LogicResult.NO, conditionResult(0));
    assertNotAllowable("a Varicella dose is not the DTaP allowable vaccine");
  }

  /**
   * Table 6-29's first condition asks whether the two vaccine types are "the same".
   * {@link VaccineType} defines that itself: two vaccine types are equal when their
   * CVX codes are equal.
   *
   * <p>
   * The condition is implemented as a Java reference comparison
   * ({@code vt == av.getVaccineType()}) rather than the CVX comparison 6.8's
   * equivalent condition uses, so two distinct instances of CVX 20 are not "the
   * same vaccine type" to 6.9. Against the bundled Supporting Data this changes
   * nothing today - {@code DataModelLoader} and {@code GatherNecessaryData} both
   * take every {@link VaccineType} out of the shared {@code cvxMap}, so all 3709
   * allowable vaccine entries and every administered dose already share one
   * instance per CVX code. It is a latent divergence from the domain model's own
   * definition of "same vaccine type", not an observable one.
   */
  @Test
  public void sameVaccineTypeMeansTheSameCvxCode() throws Exception {
    noAllowableVaccines();
    VaccineType anotherDtap = new VaccineType();
    anotherDtap.setCvxCode(DTAP);
    anotherDtap.setShortDescription("DTaP");
    AllowableVaccine allowableVaccine = allowableVaccine(DTAP, "6 weeks - 4 days", "");
    allowableVaccine.setVaccineType(anotherDtap);
    administered(INSIDE_THE_AGE_WINDOW, DTAP);

    run();

    assertEquals("two vaccine types with the same CVX code are the same vaccine type",
        LogicResult.YES, conditionResult(0));
    assertAllowable("a CVX 20 dose is the CVX 20 allowable vaccine");
  }

  /**
   * An entirely empty {@code <allowableVaccine/>} element carries no CVX, no begin
   * age and no end age, so it can never be the vaccine type of the dose
   * administered - Rule 2 - and asking it must not fail. Three of the release's
   * 3709 entries are empty this way: HepB Dose 1, HepB Dose 2 and Pertussis Dose 1.
   */
  @Test
  public void anEmptyAllowableVaccineElementNeverMatches() throws Exception {
    noAllowableVaccines();
    AllowableVaccine empty = new AllowableVaccine();
    empty.setSeriesDose(seriesDose);
    seriesDose.getAllowableVaccineList().add(empty);

    run();

    assertEquals("Table 6-29's first condition", LogicResult.NO, conditionResult(0));
    assertNotAllowable("an empty allowable vaccine element is not any vaccine type");
  }

  /**
   * A target dose that defines no allowable vaccines at all cannot have received
   * one, so the miss is recorded for 6.10 just as Rule 2's is.
   */
  @Test
  public void aTargetDoseWithNoAllowableVaccinesIsNotAllowable() throws Exception {
    noAllowableVaccines();

    run();

    assertNotAllowable("a target dose with no allowable vaccines never received one");
  }

  // ============================================= Table 6-29 Rule 3

  /**
   * Table 6-29 Rule 3: the right vaccine type, administered before the allowable
   * vaccine's begin age date - "No. The vaccine dose administered was not an
   * allowable vaccine for the target dose. It was administered out of the
   * recommended age range for the allowable vaccine." Here the dose comes the day
   * before the {@value #BEGIN_AGE_DATE_VALUE} begin age date.
   */
  @Test
  public void ruleThreeReportsNotAllowableBeforeTheBeginAgeDate() throws Exception {
    administered("02/07/2015", DTAP);

    run();

    assertEquals("Table 6-29's first condition", LogicResult.YES, conditionResult(0));
    assertEquals("Table 6-29's second condition", LogicResult.NO, conditionResult(1));
    assertNotAllowable("a dose administered before the allowable vaccine type begin age date "
        + "is out of the recommended age range");
  }

  /**
   * The second condition's upper bound is exclusive - "date administered &lt;
   * allowable vaccine type end age date" - so a dose administered on the end age
   * date itself is out of the age window. The HepB fixture's end age date is
   * {@value #HEPB_END_AGE_DATE_VALUE}.
   */
  @Test
  public void ruleThreeReportsNotAllowableOnTheEndAgeDate() throws Exception {
    noAllowableVaccines();
    theHepBAllowableVaccine();
    administered(HEPB_END_AGE_DATE_VALUE, HEPB_PEDS);

    run();

    assertEquals("Table 6-29's second condition", LogicResult.NO, conditionResult(1));
    assertNotAllowable("the allowable vaccine type end age date itself is outside the window");
  }

  /**
   * Table 6-29 names no state change for any of its three rules, and 6.8's
   * equivalent out-of-age-range rule records none either. 6.9 leaves the evaluation
   * 6.4 recorded exactly as it found it; 6.10 Satisfy Target Dose decides the
   * status.
   */
  @Test
  public void ruleThreeRecordsNoEvaluationReason() throws Exception {
    administered("02/07/2015", DTAP);

    run();

    assertNull("Table 6-29 records no evaluation reason",
        targetDose.getEvaluation().getEvaluationReason());
    assertEquals("nor an evaluation status", EvaluationStatus.VALID,
        targetDose.getEvaluation().getEvaluationStatus());
  }

  // ================================================== The roll-up across the loop

  /**
   * {@code process()} aggregates across every allowable vaccine, so one match among
   * several is enough: the dose received an allowable vaccine and nothing is
   * recorded for 6.10. Here the first two allowable vaccines are the wrong vaccine
   * type and the third is the right one.
   */
  @Test
  public void oneMatchAmongSeveralAllowableVaccinesIsEnough() throws Exception {
    noAllowableVaccines();
    allowableVaccine(VARICELLA, "12 months", "");
    theHepBAllowableVaccine();
    theStandardAllowableVaccine();

    run();

    assertAllowable("one of the three allowable vaccines matched");
  }

  /**
   * And the miss is recorded once, not once per allowable vaccine that failed:
   * three allowable vaccines, none of them the vaccine type administered, still
   * leaves 6.10 a single "Vaccine".
   */
  @Test
  public void aMissIsRecordedOnceNotOncePerAllowableVaccine() throws Exception {
    noAllowableVaccines();
    theStandardAllowableVaccine();
    theHepBAllowableVaccine();
    allowableVaccine(RSV_UNSPECIFIED, "", "");
    administered(INSIDE_THE_AGE_WINDOW, VARICELLA);

    run();

    assertNotAllowable("three allowable vaccines missed, one status cause");
  }

  // ================================================================ Next step

  /**
   * The step package's {@code transitions.yaml}: 6.9 continues to 6.10 Satisfy
   * Target Dose unconditionally, "regardless of whether an allowable vaccine
   * matched".
   */
  @Test
  public void theNextStepIsAlwaysSatisfyTargetDose() throws Exception {
    run();
    assertEquals("a matched allowable vaccine continues to 6.10",
        LogicStepType.SATISFY_TARGET_DOSE, step.getNextLogicStepType());

    setUp();
    administered(INSIDE_THE_AGE_WINDOW, VARICELLA);
    run();
    assertEquals("and so does a miss",
        LogicStepType.SATISFY_TARGET_DOSE, step.getNextLogicStepType());
  }

  // ============================================ Constructing the step

  /**
   * Constructing a step must not change the shared state a step records into - that
   * is what {@code process()} is for. The status cause is 6.9's only state change,
   * so this pins that the constructor does not reach it.
   */
  @Test
  public void constructingTheStepRecordsNothing() throws Exception {
    administered(INSIDE_THE_AGE_WINDOW, VARICELLA);

    construct();

    assertEquals("constructing the step must not record a status cause",
        "", targetDose.getStatusCause());
    assertNull("constructing the step must not decide the next step",
        step.getNextLogicStepType());
  }

  // ============= Table 6-28's Supporting Data row against the real release

  /**
   * Confirms Table 6-28's "Allowable Vaccine elements" row can actually be filled
   * from the bundled release, and that every attribute Table 6-29's two conditions
   * consult arrives on the domain object: the release's own Diphtheria Dose 1 DTaP
   * markup, verbatim, read through {@code DataModelLoader.readVaccine}.
   */
  @Test
  public void theSupportingDataAllowableVaccineMarkupCarriesEveryAttributeTableSixTwentyNineNeeds()
      throws Exception {
    cvx(DTAP);

    AllowableVaccine loaded = new AllowableVaccine();
    readVaccine(loaded, ""
        + "<allowableVaccine>"
        + "<vaccineType>DTaP</vaccineType><cvx>20</cvx>"
        + "<beginAge>6 weeks - 4 days</beginAge><endAge/>"
        + "</allowableVaccine>");

    assertNotNull("Table 6-29's first condition needs the allowable vaccine's vaccine type",
        loaded.getVaccineType());
    assertEquals(DTAP, loaded.getVaccineType().getCvxCode());

    Date birthDate = date(DATE_OF_BIRTH);
    assertNotNull("CALCDTALLOW-1 needs the vaccine type begin age",
        loaded.getVaccineTypeBeginAge());
    assertEquals("date of birth " + DATE_OF_BIRTH + " plus a begin age of 6 weeks - 4 days",
        date(BEGIN_AGE_DATE_VALUE), loaded.getVaccineTypeBeginAge().getDateFrom(birthDate));
    assertNotNull("CALCDTALLOW-2 needs the vaccine type end age",
        loaded.getVaccineTypeEndAge());
    assertFalse("an empty <endAge/> leaves Table 6-28 row 5 to its assumed value",
        loaded.getVaccineTypeEndAge().isValued());
  }

  /**
   * Invokes {@code DataModelLoader.readVaccine} - private, like the loader's other
   * per-element readers - on one {@code <allowableVaccine>} element.
   */
  private void readVaccine(Vaccine target, String allowableVaccineXml) throws Exception {
    DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = documentBuilder.parse(
        new ByteArrayInputStream(allowableVaccineXml.getBytes(Charset.forName("UTF-8"))));
    Node node = document.getDocumentElement();

    Method readVaccine = DataModelLoader.class.getDeclaredMethod("readVaccine",
        DataModel.class, Node.class, Vaccine.class);
    readVaccine.setAccessible(true);
    try {
      readVaccine.invoke(null, dataModel, node, target);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof Exception) {
        throw (Exception) ite.getCause();
      }
      throw ite;
    }
  }
}
