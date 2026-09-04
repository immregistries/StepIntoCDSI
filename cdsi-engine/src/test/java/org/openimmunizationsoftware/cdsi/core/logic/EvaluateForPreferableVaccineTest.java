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
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Section 6.8 "Evaluate for Preferable Vaccine" (Logic Specification for ACIP
 * Recommendations v4.6, pages 63-66, Figures 6-17/6-18, Figure 6-19, Table 6-25,
 * Table 6-26, Table 6-27) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/06-08-evaluate-for-preferable-vaccine/index.md}.
 *
 * <p>
 * 6.8 asks one question, once per preferable vaccine the target dose defines:
 * was the vaccine dose administered <em>this</em> preferable vaccine? Table 6-26
 * decides it on four conditions and five outcomes.
 *
 * <pre>
 * Table 6-26 Was the Vaccine Dose Administered a Preferable Vaccine for the
 *            Target Dose?
 *
 *   Condition                                        R1    R2    R3   R4   R5
 *   Is the vaccine type of the vaccine dose
 *     administered the same as the vaccine type of
 *     a preferable vaccine for the target dose?      Yes   Yes   No   Yes  Yes
 *   Is the preferable vaccine type begin age date
 *     &#8804; date administered &lt; preferable vaccine
 *     type end age date?                             Yes   Yes   -    No   Yes
 *   Is the trade name of the vaccine dose
 *     administered the same as the trade name of
 *     the preferable vaccine for the target dose?    Yes   Yes   -    -    No
 *   Is the volume of the vaccine dose administered
 *     &#8805; the volume of the preferable vaccine for
 *     the target dose?                               Yes   No    -    -    -
 *
 *   Outcome                                          Yes   Yes,  No   No,  No,
 *                                                          low        out  trade
 *                                                          volume     of   name
 *                                                          reason     age  differs
 * </pre>
 *
 * <p>
 * Table 6-27 supplies the two calculated dates the second condition needs:
 *
 * <pre>
 * CALCDTPREF-1  preferable vaccine type begin age date
 *               = the patient's date of birth + the vaccine type begin age of a
 *                 preferable vaccine.
 * CALCDTPREF-2  preferable vaccine type end age date
 *               = the patient's date of birth + the vaccine type end age of a
 *                 preferable vaccine.
 * </pre>
 *
 * <p>
 * The section text carries one rule the decision table does not spell out:
 * "volume is sparsely populated and tracked differently in most systems.
 * Therefore, volume will not be used to evaluate the validity of a vaccine dose
 * administered. However, it will be provided as an evaluation reason that less
 * than sufficient volume was administered." That is what makes Rule 2 a
 * <em>preferable</em> outcome carrying a reason rather than a rejection.
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads: a
 * {@code Patient} with a date of birth (both business rules measure from it), the
 * {@code AntigenAdministeredRecord} 4.4 has made current, and the current
 * {@code TargetDose} whose {@code SeriesDose} holds the Supporting Data
 * {@code PreferrableVaccine} records. The target dose is seeded with the
 * evaluation 6.4 leaves behind, because Rule 2 writes an evaluation reason into
 * it rather than creating one.
 *
 * <p>
 * The fixtures are the bundled Supporting Data release's own preferable vaccine
 * shapes, verbatim:
 *
 * <ul>
 * <li>the Diphtheria DTaP entry - CVX 20, begin age "6 weeks", end age "7 years",
 * volume "0.5", no trade name - as the standard preferable vaccine every
 * rule-by-rule test measures against. A patient born 01/01/2015 gets a begin age
 * date of 02/12/2015 and an end age date of 01/01/2022.</li>
 * <li>the HepB "Hep B, Adol/peds" entry - CVX 08, begin age "0 days", end age
 * "20 years", volume "0.5" - for the birth-dose boundary.</li>
 * <li>the HepB "Hep B, Adult" ENGERIX-B ADULT entry - CVX 43, begin age
 * "20 years", volume "2.0", trade name "ENGERIX-B ADULT" - for the trade name
 * condition. It shares CVX 43 with the release's RECOMBIVAX ADULT entry, so trade
 * name is the only attribute that tells the two apart.</li>
 * </ul>
 *
 * <p>
 * 6.8 publishes Table 6-25's attributes per preferable vaccine in
 * {@code getConditionAttributesAdditionalMap()} rather than once in
 * {@code getConditionAttributeList()}, so that map is what these tests read. Its
 * decision table is a {@code private} inner class, so it is read through the
 * public {@code getLogicTableList()} as a plain {@link LogicTable}, and its
 * "was this a preferable vaccine" answer is read from the step's own public
 * conclusion: {@code process()} routes to 6.10 Satisfy Target Dose when at least
 * one preferable vaccine matched and to 6.9 Evaluate for Allowable Vaccine when
 * none did.
 *
 * <h2>What is deliberately not asserted</h2>
 *
 * <p>
 * Figure 6-19's loop exit. The process model leaves the loop as soon as one
 * preferable vaccine matches ("Did the patient receive a preferable vaccine?" Yes
 * goes straight to the end node, bypassing "Are there more preferable
 * vaccines?"), while {@code process()} evaluates every preferable vaccine's table
 * and only then aggregates. The difference is observable only when a single dose
 * can match two of a target dose's preferable vaccines with different Rule
 * 1/Rule 2 outcomes, and it cannot on the bundled release: only 9 of its 484
 * series doses list the same CVX twice among their preferable vaccines, and in
 * every one of those the repeated entries either have disjoint age windows
 * (cholera CVX 174, JE CVX 134, and the rest) or identical volumes (the four
 * influenza CVX 140 brands), so no dose can reach two different outcomes.
 * Asserting an ordering nothing can observe would be a brittle test, not a
 * behaviour.
 *
 * <p>
 * The evaluation <em>status</em>. Like 6.5, 6.6 and 6.7, 6.8 records only a
 * reason and a routing decision; section 6.10 Satisfy Target Dose is where a
 * status is decided.
 *
 * <p>
 * The target dose's status cause. 6.8 never writes to it, and {@code process()}
 * ends by calling {@code next()}, which constructs the following step; asserting
 * that field here would test a neighbour.
 *
 * <p>
 * Whether a Rule 1 match should clear an evaluation reason an earlier chapter-6
 * step left behind. Table 6-26's outcomes name only what 6.8 records, never what
 * it preserves.
 *
 * <p>
 * The wording of Table 6-26's four condition rows and of its title question, for
 * the same reason 6.4, 6.5 and 6.6 left theirs alone - only the table number, its
 * shape and its result grid are pinned.
 *
 * <p>
 * {@code forecastVaccineType} and {@code mvx}, which the loader reads onto a
 * preferable vaccine but Table 6-25 does not list and Table 6-26 never consults.
 *
 * <p>
 * The structured log events, for the same reason 6.3 through 6.7 left theirs
 * alone.
 */
public class EvaluateForPreferableVaccineTest {

  /** Table 6-25 row 1. */
  private static final String DATE_ADMINISTERED = "Date Administered";
  /** Table 6-25 row 2. */
  private static final String VOLUME = "Volume";
  /** Table 6-25 row 3. */
  private static final String TRADE_NAME = "Trade Name";
  /** Table 6-25 row 4. */
  private static final String PREFERABLE_VACCINE_ELEMENTS = "Preferable Vaccine elements";
  /** Table 6-25 row 5, CALCDTPREF-1. */
  private static final String BEGIN_AGE_DATE = "Preferable Vaccine Type Begin Age Date";
  /** Table 6-25 row 6, CALCDTPREF-2. */
  private static final String END_AGE_DATE = "Preferable Vaccine Type End Age Date";

  /** Table 6-25's assumed value for the CALCDTPREF-1 date. */
  private static final String ASSUMED_BEGIN_AGE_DATE = "01/01/1900";
  /** Table 6-25's assumed value for the CALCDTPREF-2 date. */
  private static final String ASSUMED_END_AGE_DATE = "12/31/2999";

  /** CVX 20, DTaP - the standard preferable vaccine in the fixtures below. */
  private static final String DTAP = "20";
  /** CVX 08, Hep B Adol/peds - the release's "0 days" begin age entry. */
  private static final String HEPB_PEDS = "08";
  /** CVX 43, Hep B Adult - the release's two trade-named entries share it. */
  private static final String HEPB_ADULT = "43";
  /** CVX 21, Varicella - not a preferable vaccine for any fixture below. */
  private static final String VARICELLA = "21";

  private static final String DATE_OF_BIRTH = "01/01/2015";
  /** {@value #DATE_OF_BIRTH} plus the standard fixture's "6 weeks" begin age. */
  private static final String BEGIN_AGE_DATE_VALUE = "02/12/2015";
  /** {@value #DATE_OF_BIRTH} plus the standard fixture's "7 years" end age. */
  private static final String END_AGE_DATE_VALUE = "01/01/2022";
  /** Squarely inside the standard fixture's age window. */
  private static final String INSIDE_THE_AGE_WINDOW = "06/01/2016";

  /**
   * Table 6-26's grid, transcribed from the specification. Condition rows in
   * Table 6-25 order, rule columns 1 to 5, with a dash written as
   * {@link LogicResult#ANY}.
   */
  private static final LogicResult[][] TABLE_SIX_TWENTY_SIX = {
      { LogicResult.YES, LogicResult.YES, LogicResult.NO, LogicResult.YES, LogicResult.YES },
      { LogicResult.YES, LogicResult.YES, LogicResult.ANY, LogicResult.NO, LogicResult.YES },
      { LogicResult.YES, LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.NO },
      { LogicResult.YES, LogicResult.NO, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY },
  };

  private DataModel dataModel;
  private SeriesDose seriesDose;
  private TargetDose targetDose;
  private Evaluation evaluation;
  private EvaluateForPreferableVaccine step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    Patient patient = new Patient();
    patient.setDateOfBirth(date(DATE_OF_BIRTH));
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date("06/01/2021"));
    dataModel.setTargetDoseList(new ArrayList<TargetDose>());

    seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("2");
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    // 6.4 always runs first and always records an evaluation; Table 6-26's Rule 2
    // writes a reason into it rather than creating one.
    evaluation = new Evaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.VALID);
    targetDose.setEvaluation(evaluation);

    // The default fixture: the release's own DTaP preferable vaccine, and a
    // matching dose administered inside its age window.
    theStandardPreferableVaccine();
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

  private static VaccineType vaccineType(String cvxCode) {
    VaccineType vaccineType = new VaccineType();
    vaccineType.setCvxCode(cvxCode);
    vaccineType.setShortDescription("CVX " + cvxCode);
    return vaccineType;
  }

  /** Stands in for 4.4 having made this vaccine dose administered the current one. */
  private AntigenAdministeredRecord administered(String monthDayYear, String cvxCode) {
    VaccineType type = vaccineType(cvxCode);

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
   * A Supporting Data preferable vaccine, written the way
   * {@code DataModelLoader.readVaccine} writes one onto a series dose.
   */
  private PreferrableVaccine preferableVaccine(String cvxCode, String beginAge, String endAge,
      String tradeName, String volume) {
    PreferrableVaccine preferrableVaccine = new PreferrableVaccine();
    preferrableVaccine.setSeriesDose(seriesDose);
    preferrableVaccine.setVaccineType(vaccineType(cvxCode));
    preferrableVaccine.setVaccineTypeBeginAge(new TimePeriod(beginAge));
    preferrableVaccine.setVaccineTypeEndAge(new TimePeriod(endAge));
    preferrableVaccine.setTradeName(tradeName);
    preferrableVaccine.setVolume(volume);
    seriesDose.getPreferrableVaccineList().add(preferrableVaccine);
    return preferrableVaccine;
  }

  /**
   * The bundled release's Diphtheria DTaP preferable vaccine, verbatim: CVX 20,
   * begin age "6 weeks", end age "7 years", no trade name, volume "0.5". For a
   * patient born {@value #DATE_OF_BIRTH} that is an age window of
   * {@value #BEGIN_AGE_DATE_VALUE} to {@value #END_AGE_DATE_VALUE}.
   */
  private PreferrableVaccine theStandardPreferableVaccine() {
    return preferableVaccine(DTAP, "6 weeks", "7 years", "", "0.5");
  }

  private void noPreferableVaccines() {
    seriesDose.getPreferrableVaccineList().clear();
  }

  private EvaluateForPreferableVaccine construct() {
    step = new EvaluateForPreferableVaccine(dataModel);
    return step;
  }

  private LogicStep run() throws Exception {
    construct();
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  /**
   * Table 6-25's rows, for the one preferable vaccine the fixture defines. 6.8
   * publishes them per preferable vaccine in the additional map rather than once
   * in {@code getConditionAttributeList()}, which is empty for this step.
   */
  private List<ConditionAttribute<?>> tableSixTwentyFive() {
    Map<String, List<ConditionAttribute<?>>> groups = step.getConditionAttributesAdditionalMap();
    assertEquals("one preferable vaccine means one group of Table 6-25 attributes",
        1, groups.size());
    return groups.values().iterator().next();
  }

  private ConditionAttribute<?> attribute(int row) {
    List<ConditionAttribute<?>> attributes = tableSixTwentyFive();
    assertTrue("Table 6-25 row " + (row + 1) + " is not registered", attributes.size() > row);
    return attributes.get(row);
  }

  private LogicTable tableSixTwentySix() {
    assertEquals("one preferable vaccine means one decision table",
        1, step.getLogicTableList().size());
    return step.getLogicTableList().get(0);
  }

  private LogicResult conditionResult(int condition) {
    return tableSixTwentySix().getLogicConditions()[condition].getLogicResult();
  }

  private EvaluationReason evaluationReason() {
    return targetDose.getEvaluation().getEvaluationReason();
  }

  /**
   * 6.8's own public conclusion. {@code process()} sets 6.10 Satisfy Target Dose
   * when at least one preferable vaccine matched and 6.9 Evaluate for Allowable
   * Vaccine when none did, so the next step <em>is</em> the "was the vaccine dose
   * administered a preferable vaccine" answer.
   */
  private void assertPreferable(String why) {
    assertEquals(why, LogicStepType.SATISFY_TARGET_DOSE, step.getNextLogicStepType());
  }

  private void assertNotPreferable(String why) {
    assertEquals(why, LogicStepType.EVALUATE_FOR_ALLOWABLE_VACCINE, step.getNextLogicStepType());
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

  // ==================================================== Entry: what 6.8's class is

  /** 6.8 identifies itself as {@code EVALUATE_FOR_PREFERABLE_VACCINE}. */
  @Test
  public void theStepIsSixEight() throws Exception {
    run();

    assertEquals(LogicStepType.EVALUATE_FOR_PREFERABLE_VACCINE, step.getLogicStepType());
    assertEquals("6.8", LogicStepType.EVALUATE_FOR_PREFERABLE_VACCINE.getChapter());
  }

  // ============================================ Figure 6-19: the loop's shape

  /**
   * Figure 6-19's loop is "for each preferable vaccine for the target dose", and
   * each pass applies Table 6-26 to that one preferable vaccine, so the step
   * builds one decision table per preferable vaccine defined on the target dose.
   */
  @Test
  public void oneDecisionTableIsBuiltForEachPreferableVaccine() throws Exception {
    noPreferableVaccines();
    preferableVaccine(DTAP, "6 weeks", "7 years", "", "0.5");
    preferableVaccine(HEPB_PEDS, "0 days", "20 years", "", "0.5");
    preferableVaccine(HEPB_ADULT, "20 years", "", "ENGERIX-B ADULT", "2.0");

    run();

    assertEquals("three preferable vaccines means three passes round Figure 6-19's loop",
        3, step.getLogicTableList().size());
    assertEquals("and three groups of Table 6-25 attributes",
        3, step.getConditionAttributesAdditionalMap().size());
  }

  /** A target dose with no preferable vaccines never enters Figure 6-19's loop. */
  @Test
  public void noPreferableVaccinesMeansNoDecisionTable() throws Exception {
    noPreferableVaccines();

    run();

    assertEquals(0, step.getLogicTableList().size());
  }

  // ============================================ Table 6-26: the decision table

  /**
   * Table 6-26 "Was the vaccine dose administered a preferable vaccine for the
   * target dose?" has four conditions and five rules.
   */
  @Test
  public void theDecisionTableIsTableSixTwentySix() throws Exception {
    run();

    LogicTable table = tableSixTwentySix();
    assertTrue("the decision table should identify itself as Table 6-26 but was '"
        + table.getLabel() + "'", normalized(table.getLabel()).contains("table626"));
    assertEquals("Table 6-26 has four conditions", 4, table.getLogicConditions().length);
    assertEquals("Table 6-26 has five rules", 5, table.getLogicOutcomes().length);
  }

  /** Table 6-26's grid, condition by condition and rule by rule. */
  @Test
  public void theDecisionTableGridMatchesTableSixTwentySix() throws Exception {
    run();

    assertArrayEquals("Table 6-26's condition/rule grid",
        TABLE_SIX_TWENTY_SIX, tableSixTwentySix().getLogicResultTable());
  }

  // ============================================ Table 6-25: the attribute table

  /** Table 6-25 has six rows, published once per preferable vaccine. */
  @Test
  public void tableSixTwentyFivePublishesSixAttributesPerPreferableVaccine() throws Exception {
    run();

    assertEquals("Table 6-25 has six rows", 6, tableSixTwentyFive().size());
  }

  /**
   * Table 6-25 rows 1 to 3: the date administered, the volume and the trade name
   * of the vaccine dose administered.
   */
  @Test
  public void tableSixTwentyFiveCarriesTheVaccineDoseAdministeredAttributes() throws Exception {
    AntigenAdministeredRecord aar = administered(INSIDE_THE_AGE_WINDOW, DTAP);
    aar.setAmount("0.25");
    aar.setTradeName("RECOMBIVAX ADULT");

    run();

    assertLabelIs("Vaccine dose administered", attribute(0).getAttributeType());
    assertLabelIs(DATE_ADMINISTERED, attribute(0).getAttributeName());
    assertEquals(date(INSIDE_THE_AGE_WINDOW), attribute(0).getFinalValue());

    assertLabelIs("Vaccine dose administered", attribute(1).getAttributeType());
    assertLabelIs(VOLUME, attribute(1).getAttributeName());
    assertEquals("0.25", attribute(1).getFinalValue());

    assertLabelIs("Vaccine dose administered", attribute(2).getAttributeType());
    assertLabelIs(TRADE_NAME, attribute(2).getAttributeName());
    assertEquals("RECOMBIVAX ADULT", attribute(2).getFinalValue());
  }

  /**
   * Table 6-25 row 4: the Supporting Data preferable vaccine this pass round
   * Figure 6-19's loop is evaluating.
   */
  @Test
  public void tableSixTwentyFiveCarriesThePreferableVaccineElements() throws Exception {
    noPreferableVaccines();
    PreferrableVaccine preferrableVaccine = theStandardPreferableVaccine();

    run();

    assertLabelIs("Supporting Data", attribute(3).getAttributeType());
    assertLabelIs(PREFERABLE_VACCINE_ELEMENTS, attribute(3).getAttributeName());
    assertSame("Table 6-25 row 4 carries the preferable vaccine being evaluated",
        preferrableVaccine, attribute(3).getFinalValue());
  }

  /**
   * Table 6-25 row 5's assumed value if empty is 01/01/1900. A preferable vaccine
   * whose {@code <beginAge/>} element is empty has no begin age to calculate
   * CALCDTPREF-1 from, so the calculated date falls back to the assumed value.
   */
  @Test
  public void tableSixTwentyFiveAssumesTheBeginAgeDateIsJanuaryFirstNineteenHundred()
      throws Exception {
    noPreferableVaccines();
    preferableVaccine(DTAP, "", "7 years", "", "0.5");

    run();

    assertEquals("Table 6-25 row 5's assumed value if empty",
        date(ASSUMED_BEGIN_AGE_DATE), attribute(4).getFinalValue());
  }

  /**
   * Table 6-25 row 6's assumed value if empty is 12/31/2999. 627 of the bundled
   * release's 1089 preferable vaccine entries have an empty {@code <endAge/>}, so
   * this is the common case rather than the exception.
   */
  @Test
  public void tableSixTwentyFiveAssumesTheEndAgeDateIsDecemberThirtyFirstTwentyNineNinetyNine()
      throws Exception {
    noPreferableVaccines();
    preferableVaccine(DTAP, "6 weeks", "", "", "0.5");

    run();

    assertEquals("Table 6-25 row 6's assumed value if empty",
        date(ASSUMED_END_AGE_DATE), attribute(5).getFinalValue());
  }

  /**
   * Table 6-25's last two rows are attribute type "Calculated date (CALCDTPREF-1)"
   * / "Calculated date (CALCDTPREF-2)" with attribute names "Preferable Vaccine
   * Type Begin Age Date" and "Preferable Vaccine Type End Age Date" - the same
   * shape 6.5 and 6.6 use for their own calculated dates.
   */
  @Test
  public void tableSixTwentyFiveNamesTheCalculatedDatesAsTheSpecificationDoes() throws Exception {
    run();

    assertLabelIs("Calculated date (CALCDTPREF-1)", attribute(4).getAttributeType());
    assertLabelIs(BEGIN_AGE_DATE, attribute(4).getAttributeName());
    assertLabelIs("Calculated date (CALCDTPREF-2)", attribute(5).getAttributeType());
    assertLabelIs(END_AGE_DATE, attribute(5).getAttributeName());
  }

  // ====================================== Table 6-27 CALCDTPREF-1

  /**
   * CALCDTPREF-1: "A patient's preferable vaccine type begin age date must be
   * calculated as the patient's date of birth plus the vaccine type begin age of a
   * preferable vaccine."
   *
   * <p>
   * A patient born {@value #DATE_OF_BIRTH} plus the DTaP fixture's "6 weeks" begin
   * age is {@value #BEGIN_AGE_DATE_VALUE}.
   */
  @Test
  public void calcdtprefOneCalculatesThePreferableVaccineTypeBeginAgeDate() throws Exception {
    run();

    assertEquals("date of birth " + DATE_OF_BIRTH + " plus a begin age of 6 weeks",
        date(BEGIN_AGE_DATE_VALUE), attribute(4).getFinalValue());
  }

  // ====================================== Table 6-27 CALCDTPREF-2

  /**
   * CALCDTPREF-2: "A patient's preferable vaccine type end age date must be
   * calculated as the patient's date of birth plus the vaccine type end age of a
   * preferable vaccine."
   *
   * <p>
   * A patient born {@value #DATE_OF_BIRTH} plus the DTaP fixture's "7 years" end
   * age is {@value #END_AGE_DATE_VALUE}.
   */
  @Test
  public void calcdtprefTwoCalculatesThePreferableVaccineTypeEndAgeDate() throws Exception {
    run();

    assertEquals("date of birth " + DATE_OF_BIRTH + " plus an end age of 7 years",
        date(END_AGE_DATE_VALUE), attribute(5).getFinalValue());
  }

  // ============================================= Table 6-26 Rule 1

  /**
   * Table 6-26 Rule 1: same vaccine type, inside the age window, same trade name,
   * volume at least the preferable vaccine's - "Yes. The vaccine dose administered
   * was a preferable vaccine for the target dose", with no evaluation reason.
   */
  @Test
  public void ruleOneReportsAPreferableVaccine() throws Exception {
    run();

    assertPreferable("a DTaP dose administered inside the DTaP preferable vaccine's age window "
        + "was a preferable vaccine for the target dose");
    assertNull("Rule 1 records no evaluation reason", evaluationReason());
  }

  /**
   * Rule 1's fourth condition is "volume ... &#8805; the volume of the preferable
   * vaccine", so a dose administered at exactly the recommended volume is Rule 1,
   * not Rule 2. The DTaP fixture's volume is the release's most common, "0.5"
   * (837 of its 1089 preferable vaccine entries).
   */
  @Test
  public void ruleOneTreatsAnEqualVolumeAsSufficient() throws Exception {
    administered(INSIDE_THE_AGE_WINDOW, DTAP).setAmount("0.5");

    run();

    assertPreferable("a dose administered at exactly the recommended volume is Rule 1");
    assertNull("an equal volume is not less than the recommended volume", evaluationReason());
  }

  // ============================================= Table 6-26 Rule 2

  /**
   * Table 6-26 Rule 2: everything matches except the volume, which is below the
   * preferable vaccine's - "Evaluation Reason is 'Volume administered is less than
   * recommended volume'".
   */
  @Test
  public void ruleTwoRecordsTheLessThanRecommendedVolumeEvaluationReason() throws Exception {
    administered(INSIDE_THE_AGE_WINDOW, DTAP).setAmount("0.25");

    run();

    assertEquals("0.25 mL against the DTaP preferable vaccine's 0.5 mL",
        EvaluationReason.LESS_THAN_RECOMMENDED_VOLUME, evaluationReason());
  }

  /**
   * Section 6.8: "volume is sparsely populated and tracked differently in most
   * systems. Therefore, volume will not be used to evaluate the validity of a
   * vaccine dose administered." Rule 2's outcome still begins "Yes. The vaccine
   * dose administered was a preferable vaccine for the target dose" - the low
   * volume is a note, not a rejection.
   */
  @Test
  public void ruleTwoStillReportsAPreferableVaccine() throws Exception {
    administered(INSIDE_THE_AGE_WINDOW, DTAP).setAmount("0.25");

    run();

    assertPreferable("an under-volume dose is still a preferable vaccine");
  }

  // ============================================= Table 6-26 Rule 3

  /**
   * Table 6-26 Rule 3: the vaccine type of the vaccine dose administered is not
   * the vaccine type of this preferable vaccine, so nothing else is asked - "No.
   * The vaccine dose administered was not a preferable vaccine for the target
   * dose", with no evaluation reason.
   */
  @Test
  public void ruleThreeReportsNotPreferableWhenTheVaccineTypeDiffers() throws Exception {
    administered(INSIDE_THE_AGE_WINDOW, VARICELLA);

    run();

    assertEquals("Table 6-26's first condition", LogicResult.NO, conditionResult(0));
    assertNotPreferable("a Varicella dose is not the DTaP preferable vaccine");
    assertNull("Rule 3 records no evaluation reason", evaluationReason());
  }

  // ============================================= Table 6-26 Rule 4

  /**
   * Table 6-26 Rule 4: the right vaccine type, administered outside the preferable
   * vaccine's age window - "It was administered out of the recommended age range
   * for the preferable vaccine." Here the dose comes the day before the
   * {@value #BEGIN_AGE_DATE_VALUE} begin age date.
   */
  @Test
  public void ruleFourReportsNotPreferableBeforeTheBeginAgeDate() throws Exception {
    administered("02/11/2015", DTAP);

    run();

    assertEquals("Table 6-26's second condition", LogicResult.NO, conditionResult(1));
    assertNotPreferable("a dose administered before the preferable vaccine type begin age date "
        + "is out of the recommended age range");
  }

  /**
   * The second condition's upper bound is exclusive - "date administered &lt;
   * preferable vaccine type end age date" - so a dose administered on the end age
   * date itself is out of the age window. The DTaP fixture's end age date is
   * {@value #END_AGE_DATE_VALUE}.
   */
  @Test
  public void ruleFourReportsNotPreferableOnTheEndAgeDate() throws Exception {
    administered(END_AGE_DATE_VALUE, DTAP);

    run();

    assertEquals("Table 6-26's second condition", LogicResult.NO, conditionResult(1));
    assertNotPreferable("the preferable vaccine type end age date itself is outside the window");
  }

  /** The day before the end age date is still inside the window. */
  @Test
  public void ruleOneIncludesTheDayBeforeTheEndAgeDate() throws Exception {
    administered("12/31/2021", DTAP);

    run();

    assertEquals("Table 6-26's second condition", LogicResult.YES, conditionResult(1));
    assertPreferable("the day before the preferable vaccine type end age date is inside "
        + "the window");
  }

  /**
   * The second condition's lower bound is inclusive - "Is the preferable vaccine
   * type begin age date &#8804; date administered ...?" - so a dose administered on
   * the begin age date itself is inside the age window and the answer is Yes.
   *
   * <p>
   * The DTaP fixture's begin age of "6 weeks" gives a begin age date of
   * {@value #BEGIN_AGE_DATE_VALUE} for a patient born {@value #DATE_OF_BIRTH}.
   * "6 weeks" is the bundled release's most common begin age - 394 of its 1089
   * preferable vaccine entries - and every one of the 1089 defines a begin age, so
   * this boundary applies to all of them.
   */
  @Test
  public void ruleOneIncludesTheBeginAgeDateItself() throws Exception {
    administered(BEGIN_AGE_DATE_VALUE, DTAP);

    run();

    assertEquals("Table 6-26's second condition", LogicResult.YES, conditionResult(1));
    assertPreferable("the preferable vaccine type begin age date itself is inside the window");
  }

  /**
   * The same inclusive lower bound at the boundary that matters most in the
   * bundled release: a preferable vaccine whose begin age is "0 days" puts its
   * begin age date on the patient's date of birth, so a birth dose is inside its
   * age window. 53 of the release's 1089 preferable vaccine entries have a "0
   * days" begin age - 19 of them HepB, where the birth dose is routine.
   */
  @Test
  public void aDoseAdministeredOnTheDateOfBirthIsInsideAZeroDayBeginAgeWindow() throws Exception {
    noPreferableVaccines();
    preferableVaccine(HEPB_PEDS, "0 days", "20 years", "", "0.5");
    administered(DATE_OF_BIRTH, HEPB_PEDS);

    run();

    assertEquals("Table 6-26's second condition", LogicResult.YES, conditionResult(1));
    assertPreferable("a Hep B birth dose is inside a '0 days' begin age window");
  }

  // ============================================= Table 6-26 Rule 5

  /**
   * Table 6-26's third condition: "Is the trade name of the vaccine dose
   * administered the same as the trade name of the preferable vaccine for the
   * target dose?" A RECOMBIVAX ADULT dose measured against the release's
   * ENGERIX-B ADULT preferable vaccine is a No.
   *
   * <p>
   * Both entries are the bundled release's own, and both carry CVX 43, so trade
   * name is the only attribute that tells them apart. 16 of the release's 1089
   * preferable vaccine entries carry a trade name: ENGERIX-B ADULT (6),
   * RECOMBIVAX ADULT (2) and the four trivalent influenza brands under CVX 140 (2
   * each), and those four influenza entries share a begin age and a volume as well
   * as a CVX, so trade name is the <em>only</em> thing that distinguishes them.
   */
  @Test
  public void ruleFiveEvaluatesTheTradeNameOfTheVaccineDoseAdministered() throws Exception {
    theEngerixBAdultPreferableVaccine();
    AntigenAdministeredRecord aar = administered("06/01/2036", HEPB_ADULT);
    aar.setTradeName("RECOMBIVAX ADULT");
    aar.setAmount("2.0");

    run();

    assertEquals("Table 6-26's third condition", LogicResult.NO, conditionResult(2));
  }

  /**
   * Table 6-26 Rule 5: the right vaccine type, inside the age window, but a
   * different trade name - "No. The vaccine dose administered was not a preferable
   * vaccine for the target dose. The trade name of the vaccine dose administered
   * is not the same as the trade name of the preferable vaccine."
   */
  @Test
  public void ruleFiveReportsNotPreferableWhenTheTradeNameDiffers() throws Exception {
    theEngerixBAdultPreferableVaccine();
    AntigenAdministeredRecord aar = administered("06/01/2036", HEPB_ADULT);
    aar.setTradeName("RECOMBIVAX ADULT");
    aar.setAmount("2.0");

    run();

    assertNotPreferable("a RECOMBIVAX ADULT dose is not the ENGERIX-B ADULT preferable vaccine");
  }

  /** The same fixture with the trade names matching is Rule 1. */
  @Test
  public void aMatchingTradeNameIsStillPreferable() throws Exception {
    theEngerixBAdultPreferableVaccine();
    AntigenAdministeredRecord aar = administered("06/01/2036", HEPB_ADULT);
    aar.setTradeName("ENGERIX-B ADULT");
    aar.setAmount("2.0");

    run();

    assertEquals("Table 6-26's third condition", LogicResult.YES, conditionResult(2));
    assertPreferable("an ENGERIX-B ADULT dose is the ENGERIX-B ADULT preferable vaccine");
  }

  /**
   * The bundled release's HepB "Hep B, Adult" ENGERIX-B ADULT preferable vaccine,
   * verbatim: CVX 43, begin age "20 years", empty end age, trade name "ENGERIX-B
   * ADULT", volume "2.0".
   */
  private PreferrableVaccine theEngerixBAdultPreferableVaccine() {
    noPreferableVaccines();
    return preferableVaccine(HEPB_ADULT, "20 years", "", "ENGERIX-B ADULT", "2.0");
  }

  // ================================================================ Next step

  /**
   * The step package's {@code transitions.yaml}: no preferable vaccine matched -
   * including the case where the target dose defines none at all - continues to
   * 6.9 Evaluate for Allowable Vaccine.
   */
  @Test
  public void aTargetDoseWithNoPreferableVaccinesGoesToSixNine() throws Exception {
    noPreferableVaccines();

    run();

    assertNotPreferable("a target dose with no preferable vaccines sends the dose on to the "
        + "more permissive allowable vaccine list");
  }

  /**
   * And the other branch: {@code process()} aggregates across every preferable
   * vaccine, so one match among several is enough to continue to 6.10 Satisfy
   * Target Dose. Here the first preferable vaccine is the wrong vaccine type and
   * the second is the right one.
   */
  @Test
  public void oneMatchAmongSeveralPreferableVaccinesGoesToSixTen() throws Exception {
    noPreferableVaccines();
    preferableVaccine(VARICELLA, "12 months", "", "", "0.5");
    theStandardPreferableVaccine();

    run();

    assertPreferable("one preferable vaccine matched, so the dose satisfied the preferable "
        + "vaccine requirement");
  }

  // ============================================ Constructing the step

  /**
   * Constructing a step must not change the shared state a step records into -
   * that is what {@code process()} is for. Table 6-26's Rule 2 is 6.8's only state
   * change, so this pins that the constructor does not reach it.
   */
  @Test
  public void constructingTheStepRecordsNothing() throws Exception {
    administered(INSIDE_THE_AGE_WINDOW, DTAP).setAmount("0.25");

    construct();

    assertNull("constructing the step must not record an evaluation reason", evaluationReason());
    assertNull("constructing the step must not decide the next step",
        step.getNextLogicStepType());
  }

  // ============= Table 6-25's Supporting Data row against the real release

  /**
   * Confirms Table 6-25's "Preferable Vaccine elements" row can actually be filled
   * from the bundled release, and that every attribute Table 6-26's four
   * conditions consult arrives on the domain object: the release's own ENGERIX-B
   * ADULT markup, verbatim, read through {@code DataModelLoader.readVaccine}.
   *
   * <p>
   * The loader is not the gap for the trade name condition - it reads
   * {@code <tradeName>} onto {@code Vaccine.tradeName} alongside the CVX, the two
   * ages and the volume.
   */
  @Test
  public void theSupportingDataPreferableVaccineMarkupCarriesEveryAttributeTableSixTwentySixNeeds()
      throws Exception {
    dataModel.getCvxMap().put(HEPB_ADULT, vaccineType(HEPB_ADULT));

    PreferrableVaccine loaded = new PreferrableVaccine();
    readVaccine(loaded, ""
        + "<preferableVaccine>"
        + "<vaccineType>Hep B, Adult</vaccineType><cvx>43</cvx>"
        + "<beginAge>20 years</beginAge><endAge/>"
        + "<tradeName>ENGERIX-B ADULT</tradeName><mvx>SKB</mvx>"
        + "<volume>2.0</volume>"
        + "<forecastVaccineType>N</forecastVaccineType>"
        + "</preferableVaccine>");

    assertNotNull("Table 6-26's first condition needs the preferable vaccine's vaccine type",
        loaded.getVaccineType());
    assertEquals(HEPB_ADULT, loaded.getVaccineType().getCvxCode());

    Date birthDate = date(DATE_OF_BIRTH);
    assertNotNull("CALCDTPREF-1 needs the vaccine type begin age",
        loaded.getVaccineTypeBeginAge());
    assertEquals("date of birth " + DATE_OF_BIRTH + " plus a begin age of 20 years",
        date("01/01/2035"), loaded.getVaccineTypeBeginAge().getDateFrom(birthDate));
    assertNotNull("CALCDTPREF-2 needs the vaccine type end age",
        loaded.getVaccineTypeEndAge());
    assertFalse("an empty <endAge/> leaves Table 6-25 row 6 to its assumed value",
        loaded.getVaccineTypeEndAge().isValued());

    assertEquals("Table 6-26's third condition needs the preferable vaccine's trade name",
        "ENGERIX-B ADULT", loaded.getTradeName());
    assertEquals("Table 6-26's fourth condition needs the preferable vaccine's volume",
        "2.0", loaded.getVolume());
  }

  /**
   * Invokes {@code DataModelLoader.readVaccine} - private, like the loader's other
   * per-element readers - on one {@code <preferableVaccine>} element.
   */
  private void readVaccine(Vaccine target, String preferableVaccineXml) throws Exception {
    DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = documentBuilder.parse(
        new ByteArrayInputStream(preferableVaccineXml.getBytes(Charset.forName("UTF-8"))));
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
