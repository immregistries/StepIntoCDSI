package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Section 6.3 "Evaluate for Inadvertent Vaccine" (Logic Specification for ACIP
 * Recommendations v4.6, page 52, Figure 6-4, Table 6-12, Table 6-13) as
 * documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/06-03-evaluate-for-inadvertent-vaccine/index.md}.
 *
 * <p>
 * 6.3 is a single-condition step. It runs after 6.2 has determined the target
 * dose is not skipped, and asks one question:
 *
 * <pre>
 * Table 6-13 Was the Vaccine Dose Administered an Inadvertent Administration
 *            for the Target Dose?
 *
 *   Condition                                          Rule 1     Rule 2
 *   Is the vaccine type of the dose administered one    Yes        No
 *   of the target dose's inadvertent vaccine types?
 *   Outcome                                            inadvert-  not
 *                                                      ent        inadvertent
 * </pre>
 *
 * <p>
 * Rule 1 sets exactly three values - target dose status 'Not Satisfied',
 * evaluation status 'Not Valid', evaluation reason 'Inadvertent Administration'
 * - and hands control back to 4.4 with the dose rejected. Rule 2 changes no
 * state and proceeds to 6.4 Evaluate Age. The specification states no business
 * rules for this section.
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads: the
 * {@code AntigenAdministeredRecord} 4.4 has made current (whose
 * {@code VaccineDoseAdministered} carries the vaccine type under test) and the
 * current {@code TargetDose} with the {@code SeriesDose} whose inadvertent
 * vaccine list is Table 6-12's second attribute. {@code process()} is called
 * directly; it ends by constructing its chosen next step, so the fixture also
 * supplies the patient date of birth that 6.4's constructor dereferences. No
 * Supporting Data release is loaded and {@code process()} is never called on the
 * returned step.
 *
 * <p>
 * The step's single decision table is a {@code private} inner class, so it is
 * read here through the public {@code getLogicTableList()} as a plain
 * {@link LogicTable}; its two condition attributes are read through the public
 * {@code getConditionAttributeList()}. Both attributes are named "Vaccine Type",
 * so they are looked up by attribute <em>type</em> rather than by name.
 *
 * <h2>What is deliberately not asserted</h2>
 *
 * <p>
 * The wording of Table 6-13's single condition. The specification's table cell
 * and the implementation's condition label ask the same question in different
 * words ("... one of the target dose's inadvertent vaccine types?" against
 * "... one of the vaccine types of an inadvertent vaccine for the target
 * dose?"), and neither is a transcription of the other, so pinning either string
 * would assert a paraphrase rather than a behaviour. The table's own title,
 * which the step write-up quotes verbatim, is pinned instead.
 */
public class EvaluateForInadvertentVaccineTest {

  /** Table 6-12 row 1: the vaccine dose administered's vaccine type. */
  private static final String VACCINE_DOSE_ADMINISTERED = "Vaccine dose administered";
  /** Table 6-12 row 2: the target dose's Supporting-Data-defined inadvertent vaccine types. */
  private static final String SUPPORTING_DATA = "Supporting Data (inadvertent vaccine)";

  private DataModel dataModel;
  private Patient patient;
  private AntigenAdministeredRecord antigenAdministeredRecord;
  private VaccineDoseAdministered vaccineDoseAdministered;
  private SeriesDose seriesDose;
  private TargetDose targetDose;
  private EvaluateForInadvertentVaccine step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    patient = new Patient();
    patient.setDateOfBirth(date("01/01/2015"));
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date("06/01/2021"));

    seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("1");
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    vaccineDoseAdministered = doseOf("10", "06/01/2016"); // IPV
    antigenAdministeredRecord = new AntigenAdministeredRecord();
    antigenAdministeredRecord.setDateAdministered(date("06/01/2016"));
    antigenAdministeredRecord.setVaccineType(vaccineDoseAdministered.getVaccine().getVaccineType());
    antigenAdministeredRecord.setVaccineDoseAdministered(vaccineDoseAdministered);
    dataModel.setAntigenAdministeredRecord(antigenAdministeredRecord);

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

  private static VaccineDoseAdministered doseOf(String cvxCode, String monthDayYear) {
    Vaccine vaccine = new Vaccine();
    vaccine.setVaccineType(vaccineType(cvxCode));

    VaccineDoseAdministered dose = new VaccineDoseAdministered();
    dose.setVaccine(vaccine);
    dose.setDateAdministered(date(monthDayYear));
    return dose;
  }

  /** Stands in for 4.4 having made this vaccine dose administered the current one. */
  private void administered(String cvxCode) {
    vaccineDoseAdministered = doseOf(cvxCode, "06/01/2016");
    antigenAdministeredRecord.setVaccineType(vaccineDoseAdministered.getVaccine().getVaccineType());
    antigenAdministeredRecord.setVaccineDoseAdministered(vaccineDoseAdministered);
  }

  /** The Supporting Data's inadvertent vaccine types for the target dose under evaluation. */
  private void inadvertentVaccineTypes(String... cvxCodes) {
    for (String cvxCode : cvxCodes) {
      seriesDose.getInadvertentVaccineList().add(vaccineType(cvxCode));
    }
  }

  /**
   * Attaches an {@code Evaluation} to the current target dose, so Rule 1's
   * outcome has somewhere to record its evaluation status and reason. Whether
   * the running engine actually hands 6.3 a target dose in that state is the
   * separate question
   * {@link #ruleOneRecordsItsEvaluationOnATargetDoseThatHasNoEvaluationYet}
   * asks.
   */
  private Evaluation withEvaluation() {
    Evaluation evaluation = new Evaluation();
    targetDose.setEvaluation(evaluation);
    return evaluation;
  }

  private LogicStep run() throws Exception {
    step = new EvaluateForInadvertentVaccine(dataModel);
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  private ConditionAttribute<?> attributeOfType(String attributeType) {
    for (ConditionAttribute<?> conditionAttribute : step.getConditionAttributeList()) {
      if (conditionAttribute != null && attributeType.equals(conditionAttribute.getAttributeType())) {
        return conditionAttribute;
      }
    }
    fail("Table 6-12 attribute of type '" + attributeType + "' is not registered by the step");
    return null;
  }

  private LogicTable tableSixThirteen() {
    assertEquals("6.3 builds exactly one decision table", 1, step.getLogicTableList().size());
    return step.getLogicTableList().get(0);
  }

  /**
   * Labels are compared with whitespace, case and the punctuation the two
   * documents disagree about removed - transcription differences, not
   * behavioural ones. A different table <em>number</em> is not.
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

  // ==================================================== Entry: what 6.3's class is

  /**
   * 6.3 identifies itself as {@code EVALUATE_FOR_INADVERTENT_VACCINE} and names
   * Table 6-12 as its attribute table.
   */
  @Test
  public void theStepIsSixThreeAndNamesTableSixTwelveAsItsAttributeTable() throws Exception {
    run();

    assertEquals(LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE, step.getLogicStepType());
    assertEquals("6.3", LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE.getChapter());
    assertLabelIs("Table 6-12 Inadvertent Vaccine Attributes", step.getConditionTableName());
  }

  // ============================================== Table 6-12: the two attributes

  /**
   * Table 6-12 lists two attributes: the vaccine type of the vaccine dose
   * administered, and the vaccine type of the target dose's Supporting Data
   * inadvertent vaccine. Both are registered, and the first carries the vaccine
   * dose administered 4.4 has made current.
   */
  @Test
  public void tableSixTwelveRegistersBothAttributesAndCarriesTheAdministeredDose() throws Exception {
    administered("178"); // OPV bivalent

    run();

    ConditionAttribute<?> administered = attributeOfType(VACCINE_DOSE_ADMINISTERED);
    assertEquals("Vaccine Type", administered.getAttributeName());
    assertSame("Table 6-12's first attribute is the current vaccine dose administered",
        vaccineDoseAdministered, administered.getFinalValue());

    assertEquals("Vaccine Type", attributeOfType(SUPPORTING_DATA).getAttributeName());
    assertEquals("Table 6-12 lists exactly two attributes", 2, step.getConditionAttributeList().size());
  }

  /**
   * Table 6-12's second attribute is the target dose's Supporting-Data-defined
   * inadvertent vaccine type. The step declares it ({@code caInadvertentVaccine})
   * and registers it, but never gives it a value: the decision table reaches past
   * it to {@code dataModel.getTargetDose().getTrackedSeriesDose()
   * .getInadvertentVaccineList()} directly, and the {@code caInadvertentVaccineList}
   * field that would hold the list is initialised empty and never read or
   * written. So the attribute Table 6-12 names is blank whatever the Supporting
   * Data says - including on the step's rendered attribute table.
   */
  @Test
  public void tableSixTwelveSupportingDataAttributeCarriesTheInadvertentVaccineType() throws Exception {
    inadvertentVaccineTypes("178", "179", "182");
    administered("10");

    run();

    assertNotNull("Table 6-12's Supporting Data attribute must carry the target dose's "
        + "inadvertent vaccine type, but the step never assigns it",
        attributeOfType(SUPPORTING_DATA).getFinalValue());
  }

  // ================================================ Table 6-13: shape of the table

  /**
   * Table 6-13's single condition and its two-rule grid, as the specification
   * writes them.
   */
  @Test
  public void tableSixThirteenIsEncodedWithOneConditionAndTwoRules() throws Exception {
    run();
    LogicTable table = tableSixThirteen();

    assertLabelIs("Table 6-13 Was the Vaccine Dose Administered an Inadvertent Administration "
        + "for the Target Dose?", table.getLabel());
    assertEquals("Table 6-13 has one condition", 1, table.getLogicConditions().length);
    assertEquals("Table 6-13 has two rules", 2, table.getLogicOutcomes().length);
    assertArrayEquals("Table 6-13: Yes / No",
        new LogicResult[] { LogicResult.YES, LogicResult.NO }, table.getLogicResultTable()[0]);
  }

  // ======================================================== Table 6-13 Rule 1

  /**
   * Table 6-13 Rule 1: the vaccine type of the dose administered is one of the
   * target dose's inadvertent vaccine types, so the administration was
   * inadvertent - target dose status 'Not Satisfied', evaluation status 'Not
   * Valid', evaluation reason 'Inadvertent Administration' - and Figure 6-4
   * returns control to 4.4 with the dose rejected.
   */
  @Test
  public void ruleOneRejectsAnInadvertentAdministrationAndReturnsToFourFour() throws Exception {
    inadvertentVaccineTypes("178"); // OPV bivalent is inadvertent for an IPV target dose
    administered("178");
    Evaluation evaluation = withEvaluation();
    targetDose.setTargetDoseStatus(TargetDoseStatus.SATISFIED);

    run();

    assertEquals(LogicResult.YES, tableSixThirteen().getLogicConditions()[0].getLogicResult());
    assertEquals("Rule 1: target dose status 'Not Satisfied'",
        TargetDoseStatus.NOT_SATISFIED, targetDose.getTargetDoseStatus());
    assertEquals("Rule 1: evaluation status 'Not Valid'",
        EvaluationStatus.NOT_VALID, evaluation.getEvaluationStatus());
    assertEquals("Rule 1: evaluation reason 'Inadvertent Administration'",
        EvaluationReason.INADVERTENT_ADMINISTRATION, evaluation.getEvaluationReason());
    assertEquals("an inadvertent dose returns to 4.4 Evaluate and Forecast All Patient Series",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());
    assertEquals("Rule 1 adds no second evaluation", 1, targetDose.getEvaluationList().size());
  }

  /**
   * Table 6-13's condition asks whether the vaccine type administered is
   * <em>one of</em> the target dose's inadvertent vaccine types - the Supporting
   * Data commonly defines several per target dose (Polio's target doses each name
   * OPV bivalent 178, OPV monovalent 179 and OPV unspecified 182). Any one of
   * them matching answers Yes.
   */
  @Test
  public void ruleOneMatchesAnyOneOfSeveralInadvertentVaccineTypes() throws Exception {
    String[] inadvertent = { "178", "179", "182" };
    for (String cvxCode : inadvertent) {
      setUp();
      inadvertentVaccineTypes(inadvertent);
      administered(cvxCode);
      withEvaluation();

      run();

      assertEquals("CVX " + cvxCode + " is one of the target dose's inadvertent vaccine types",
          LogicResult.YES, tableSixThirteen().getLogicConditions()[0].getLogicResult());
      assertEquals(TargetDoseStatus.NOT_SATISFIED, targetDose.getTargetDoseStatus());
    }
  }

  /**
   * The comparison Table 6-13 makes is between vaccine <em>types</em>, not
   * between object identities: the type on the patient's dose and the type in the
   * target dose's Supporting Data are separate objects that denote the same
   * vaccine when they share a CVX code.
   */
  @Test
  public void theVaccineTypesAreComparedByCvxCodeNotByIdentity() throws Exception {
    inadvertentVaccineTypes("178");
    administered("178");
    withEvaluation();

    assertTrue("the fixture supplies two distinct VaccineType objects sharing CVX 178",
        seriesDose.getInadvertentVaccineList().get(0) != vaccineDoseAdministered.getVaccine()
            .getVaccineType());

    run();

    assertEquals(LogicResult.YES, tableSixThirteen().getLogicConditions()[0].getLogicResult());
    assertEquals(TargetDoseStatus.NOT_SATISFIED, targetDose.getTargetDoseStatus());
  }

  /**
   * Rule 1's outcome writes an evaluation status and an evaluation reason onto
   * {@code dataModel.getTargetDose().getEvaluation()}. In the running engine that
   * evaluation does not exist yet: {@code TargetDose}'s evaluation list starts
   * empty, 4.4 dispatches to 6.1 without creating one, and neither 6.1's Rule 3
   * ("the vaccine dose administered can be evaluated") nor 6.2's "cannot be
   * skipped" outcome records anything - the first step that creates an evaluation
   * is 6.4, through {@code DataModel.setEvaluationForCurrentTargetDose(...)},
   * which runs after 6.3. So on the path that reaches 6.3 at all, the target dose
   * carries no evaluation and Rule 1's outcome dereferences null.
   *
   * <p>
   * This is the same shape as the gap 6.1's unit found in Table 6-3 Rules 1 and
   * 2, and it is recorded here for 6.3's own decision table rather than assumed
   * from it.
   */
  @Test
  public void ruleOneRecordsItsEvaluationOnATargetDoseThatHasNoEvaluationYet() throws Exception {
    inadvertentVaccineTypes("178");
    administered("178");

    assertEquals("4.4/6.1/6.2 hand 6.3 a target dose with no evaluation recorded yet",
        0, targetDose.getEvaluationList().size());

    try {
      run();
    } catch (NullPointerException npe) {
      fail("Table 6-13 Rule 1 requires evaluation status 'Not Valid' and evaluation reason "
          + "'Inadvertent Administration', but the step threw NullPointerException - "
          + "dataModel.getTargetDose().getEvaluation() is null on the target dose 6.2 hands it");
    }

    assertNotNull("Rule 1 records an evaluation status and reason, so an evaluation must exist",
        targetDose.getEvaluation());
    assertEquals(EvaluationStatus.NOT_VALID, targetDose.getEvaluation().getEvaluationStatus());
    assertEquals(EvaluationReason.INADVERTENT_ADMINISTRATION,
        targetDose.getEvaluation().getEvaluationReason());
  }

  // ======================================================== Table 6-13 Rule 2

  /**
   * Table 6-13 Rule 2: the vaccine type administered is not one of the target
   * dose's inadvertent vaccine types. The outcome is "not inadvertent" - no state
   * change at all - and evaluation continues at 6.4 Evaluate Age.
   */
  @Test
  public void ruleTwoLeavesANonInadvertentDoseAloneAndContinuesToSixFour() throws Exception {
    inadvertentVaccineTypes("178", "179", "182");
    administered("10"); // IPV: allowable here, not inadvertent
    Evaluation evaluation = withEvaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.VALID);
    targetDose.setTargetDoseStatus(TargetDoseStatus.SATISFIED);

    run();

    assertEquals(LogicResult.NO, tableSixThirteen().getLogicConditions()[0].getLogicResult());
    assertEquals("Rule 2 records no target dose status",
        TargetDoseStatus.SATISFIED, targetDose.getTargetDoseStatus());
    assertEquals("Rule 2 records no evaluation status",
        EvaluationStatus.VALID, evaluation.getEvaluationStatus());
    assertEquals("Rule 2 records no evaluation reason", null, evaluation.getEvaluationReason());
    assertEquals("Rule 2 adds no evaluation", 1, targetDose.getEvaluationList().size());
    assertEquals("a dose that is not inadvertent proceeds to 6.4 Evaluate Age",
        LogicStepType.EVALUATE_AGE, step.getNextLogicStepType());
  }

  /**
   * A target dose whose Supporting Data names no inadvertent vaccine types can
   * never take Rule 1 - the condition has nothing to match against, so Rule 2
   * applies and the dose proceeds to 6.4 untouched.
   */
  @Test
  public void aTargetDoseWithNoInadvertentVaccineTypesIsNeverAnInadvertentAdministration()
      throws Exception {
    administered("178");
    withEvaluation();

    assertTrue("the fixture's series dose names no inadvertent vaccine types",
        seriesDose.getInadvertentVaccineList().isEmpty());

    run();

    assertEquals(LogicResult.NO, tableSixThirteen().getLogicConditions()[0].getLogicResult());
    assertEquals(LogicStepType.EVALUATE_AGE, step.getNextLogicStepType());
  }

  // ============================== Table 6-12's Supporting Data, as actually loaded

  /**
   * Table 6-12's second attribute is sourced from the Supporting Data, and
   * Table 6-13's condition reads it as
   * {@code targetDose.getTrackedSeriesDose().getInadvertentVaccineList()}. Every
   * other test above populates that list by hand; this one asks whether the
   * Supporting Data's own definition can reach it.
   *
   * <p>
   * It cannot. {@code AntigenSupportingData.xsd} declares
   * {@code inadvertentVaccine} (vaccineType, cvx) as a repeating child of
   * {@code seriesDose}, but {@code DataModelLoader.readSeriesDose} has no branch
   * for that element name - it handles doseNumber, age, interval,
   * allowableInterval, preferableVaccine, allowableVaccine, conditionalSkip,
   * recurringDose, conditionalNeed, seasonalRecommendation, substituteDose and
   * requiredGender, and silently drops every other child element.
   * {@code SeriesDose.setInadvertentVaccineList(...)} is called from nowhere in
   * the engine, so the list every {@code SeriesDose} exposes is the empty one it
   * was constructed with.
   *
   * <p>
   * That makes Table 6-13 Rule 1 unreachable in the running engine, and it is not
   * a hypothetical corner: the Supporting Data release bundled with
   * {@code cdsi-engine} defines 307 {@code <inadvertentVaccine>} entries with a
   * CVX code, over 26 distinct vaccine types, on 115 of its 484 series doses,
   * across 8 antigens - COVID-19 (90 entries on 37 series doses), Polio (78 on
   * 26, every series dose it defines), Tetanus (36 on 6), Diphtheria (36 on 6),
   * RSV (34 on 7, every one), Pneumococcal (15 on 15), HPV (12 on 12) and
   * Pertussis (6 on 6). All 307 are discarded at load time.
   *
   * <p>
   * The gap is in the loader rather than in
   * {@code EvaluateForInadvertentVaccine} itself, so this test asks the smallest
   * question that can be asked about 6.3's own input: given the Supporting
   * Data's own markup for a series dose - three inadvertent vaccine types, copied
   * from Polio's Dose 1 - does the target dose 6.3 reads end up carrying them?
   */
  @Test
  public void theSupportingDatasInadvertentVaccineTypesReachTheTargetDose() throws Exception {
    String[] cvxCodes = { "178", "179", "182" };
    for (String cvxCode : cvxCodes) {
      dataModel.getCvxMap().put(cvxCode, vaccineType(cvxCode));
    }

    SeriesDose loaded = new SeriesDose();
    readSeriesDose(loaded, ""
        + "<seriesDose>"
        + "<doseNumber>Dose 1</doseNumber>"
        + "<inadvertentVaccine><vaccineType>OPV bivalent</vaccineType><cvx>178</cvx></inadvertentVaccine>"
        + "<inadvertentVaccine><vaccineType>OPV, monovalent, unspecified</vaccineType>"
        + "<cvx>179</cvx></inadvertentVaccine>"
        + "<inadvertentVaccine><vaccineType>OPV, Unspecified</vaccineType><cvx>182</cvx></inadvertentVaccine>"
        + "</seriesDose>");

    assertEquals("the loader read the series dose", "1", loaded.getDoseNumber());
    assertEquals("Table 6-12's inadvertent vaccine types, as the Supporting Data defines them",
        3, loaded.getInadvertentVaccineList().size());
    assertEquals(vaccineType("178"), loaded.getInadvertentVaccineList().get(0));
    assertEquals(vaccineType("179"), loaded.getInadvertentVaccineList().get(1));
    assertEquals(vaccineType("182"), loaded.getInadvertentVaccineList().get(2));
  }

  /**
   * Invokes {@code DataModelLoader.readSeriesDose} - private, like the loader's
   * other per-element readers - on one {@code <seriesDose>} element.
   */
  private void readSeriesDose(SeriesDose target, String seriesDoseXml) throws Exception {
    DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = documentBuilder.parse(
        new ByteArrayInputStream(seriesDoseXml.getBytes(Charset.forName("UTF-8"))));
    Node node = document.getDocumentElement();

    Method readSeriesDose = DataModelLoader.class.getDeclaredMethod("readSeriesDose",
        SeriesDose.class, Map.class, DataModel.class, Node.class);
    readSeriesDose.setAccessible(true);
    try {
      readSeriesDose.invoke(null, target, new HashMap<String, SeriesDose>(), dataModel, node);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof Exception) {
        throw (Exception) ite.getCause();
      }
      throw ite;
    }
  }
}
