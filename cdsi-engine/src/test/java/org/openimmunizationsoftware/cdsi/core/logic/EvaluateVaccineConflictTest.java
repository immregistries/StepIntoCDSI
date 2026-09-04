package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Section 6.7 "Evaluate Vaccine Conflict" (Logic Specification for ACIP
 * Recommendations v4.6, pages 61-62, Figure 6-15, Figure 6-16, Table 6-23,
 * Table 6-24) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/06-07-evaluate-vaccine-conflict/index.md}.
 *
 * <p>
 * 6.7 is the one chapter-6 evaluation step with <em>no</em> decision table.
 * Version 4.4 of the specification deliberately restructured it "to be less
 * process driven and more business rule based", so Table 6-24's three business
 * rules are the whole of what 6.7 normatively says:
 *
 * <pre>
 * CALCDTCONFLICT-1  conflict begin interval date
 *                   = previous dose's date administered + the conflict begin
 *                     interval of a vaccine type conflict, where the current
 *                     dose's vaccine type is an impacted vaccine type and the
 *                     previous dose's vaccine type is a conflicting vaccine
 *                     type for it.
 *
 * CALCDTCONFLICT-2  conflict end interval date
 *                   = previous dose's date administered + the *minimum*
 *                     conflict end interval, when the previous dose has an
 *                     evaluation status of 'Valid' or no evaluation status;
 *                   = previous dose's date administered + the conflict end
 *                     interval, when the previous dose has an evaluation
 *                     status that is not 'Valid'.
 *
 * CONFLICT-3        the current dose is an impacted vaccine dose administered
 *                   when its date administered is on or after the conflict
 *                   begin interval date and before the conflict end interval
 *                   date.
 * </pre>
 *
 * <p>
 * The Implementer Note supplies the vocabulary bridge these tests need: the
 * specification's "Impacted Vaccine Type" is the Supporting Data's
 * {@code <current>} vaccine type, and the specification's "Conflicting Vaccine
 * Type" is the Supporting Data's {@code <previous>} vaccine type. Both the
 * domain class and the implementation use the older Supporting Data words.
 *
 * <p>
 * Section 6.7's own text adds the negative case: "if no vaccine Supporting Data
 * exists for the vaccine type of the vaccine dose administered being evaluated,
 * the vaccine dose administered is not in conflict with any other vaccine dose
 * administered."
 *
 * <h2>Scaffolding</h2>
 *
 * <p>
 * Each test hand-builds the minimal {@code DataModel} the step reads: an
 * immunization history of {@code AntigenAdministeredRecord}s in date order (4.2
 * sorts them ascending by date administered within an antigen), the index 4.4
 * has stepped to, the current target dose the step records onto, and the
 * {@code LiveVirusConflict} Supporting Data.
 *
 * <p>
 * The conflict fixture is the bundled release's own MMR-after-MMR entry,
 * verbatim: previous CVX 03, current CVX 03, conflict begin interval "1 day",
 * minimum conflict end interval "24 days", conflict end interval "28 days". A
 * previous dose on 01/01/2016 therefore gives a conflict begin interval date of
 * 01/02/2016 and a conflict end interval date of either 01/25/2016
 * (CALCDTCONFLICT-2's first branch) or 01/29/2016 (its second). The default
 * fixture puts the current dose on 01/15/2016, squarely inside that window.
 *
 * <p>
 * The step declares its calculated-date attributes ad hoc, per candidate
 * previous dose, inside the constructor rather than once at class level, so
 * these tests find them by the Table 6-23 attribute name rather than by index.
 *
 * <h2>What is deliberately not asserted</h2>
 *
 * <p>
 * Any decision table number, label or grid. Section 6.7 defines none - the
 * specification lists no table for it beyond the attribute table and the
 * business rule table. The implementation still carries three internal
 * {@code LogicTable}s labelled "Table 4-20/4-21/4-22", stale leftovers from a
 * pre-4.6 chapter numbering; pinning those labels would assert a transcription
 * the current specification does not contain, and the step package already
 * records them as a documentation-only finding.
 *
 * <p>
 * Table 6-23's "Supporting Data / Live Virus Conflicts" row. The step reads the
 * conflict list straight off the {@code DataModel} instead of publishing it as
 * a condition attribute. That is the same structural difference the step
 * package already notes for the two calculated dates, and asserting it
 * separately would restate a presentation gap rather than a behaviour.
 *
 * <p>
 * Non-live-virus vaccine conflicts. Section 6.7's Purpose says it "covers live
 * virus vaccine conflicts as well as non-live virus vaccine conflicts", but the
 * only conflict Supporting Data the specification defines - and the only kind
 * the loader reads - is {@code <liveVirusConflicts>}, so there is no separate
 * behaviour to isolate.
 *
 * <p>
 * The evaluation status. Like 6.5 and 6.6, 6.7 records a marker on the target
 * dose's status cause; section 6.10 Evaluate Target Dose is where a status is
 * decided from it.
 *
 * <p>
 * The structured log events, for the same reason 6.3 through 6.6 left theirs
 * alone.
 */
public class EvaluateVaccineConflictTest {

  /** Table 6-23 row 1. */
  private static final String DATE_ADMINISTERED = "Date Administered";
  /** Table 6-23 row 2. */
  private static final String VACCINE_TYPE = "Vaccine Type";
  /** Table 6-23 row 4, CALCDTCONFLICT-1. */
  private static final String CONFLICT_BEGIN_INTERVAL_DATE = "Conflict Begin Interval Date";
  /** Table 6-23 row 5, CALCDTCONFLICT-2. */
  private static final String CONFLICT_END_INTERVAL_DATE = "Conflict End Interval Date";

  /**
   * The marker 6.7 appends to the target dose's status cause when a conflict is
   * found, for 6.10 Evaluate Target Dose to read later.
   */
  private static final String CONFLICT_STATUS_CAUSE = "VirusConflict";

  /** CVX 03, MMR - both the impacted and the conflicting type in the fixture. */
  private static final String MMR = "03";
  /** CVX 21, Varicella - in no conflict with MMR in the fixture. */
  private static final String VARICELLA = "21";

  private static final String PREVIOUS_DOSE = "01/01/2016";
  private static final String CURRENT_DOSE = "01/15/2016";

  private DataModel dataModel;
  private SeriesDose seriesDose;
  private TargetDose targetDose;
  private EvaluateVaccineConflict step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    Patient patient = new Patient();
    patient.setDateOfBirth(date("01/01/2015"));
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date("06/01/2021"));
    dataModel.setTargetDoseList(new ArrayList<TargetDose>());

    seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("2");
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    // 6.4 always runs first and always records an evaluation.
    Evaluation evaluation = new Evaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.VALID);
    targetDose.setEvaluation(evaluation);

    theBundledMmrAfterMmrConflict();

    // The default history: one conflicting MMR dose administered before the
    // current MMR dose, with the current dose inside the conflict window.
    immunizationHistory(1, administeredRecord(PREVIOUS_DOSE, MMR),
        administeredRecord(CURRENT_DOSE, MMR));

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

  private static AntigenAdministeredRecord administeredRecord(String monthDayYear, String cvxCode) {
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
    return aar;
  }

  /**
   * Stands in for 4.2 having sorted the immunization history ascending by date
   * administered and 4.4 having stepped to the record at {@code currentPos}.
   */
  private void immunizationHistory(int currentPos, AntigenAdministeredRecord... records) {
    List<AntigenAdministeredRecord> list = new ArrayList<AntigenAdministeredRecord>(
        Arrays.asList(records));
    dataModel.setAntigenAdministeredRecordList(list);
    dataModel.setSelectedAntigenAdministeredRecordList(list);
    dataModel.setSelectedAntigenAdministeredRecordPos(currentPos);
    dataModel.setAntigenAdministeredRecord(list.get(currentPos));
  }

  /**
   * The bundled Supporting Data release's own MMR-after-MMR live virus conflict:
   * conflict begin interval "1 day", minimum conflict end interval "24 days",
   * conflict end interval "28 days". Its three intervals are all different, which
   * is what lets CALCDTCONFLICT-1 and both branches of CALCDTCONFLICT-2 be told
   * apart.
   */
  private LiveVirusConflict theBundledMmrAfterMmrConflict() {
    return liveVirusConflict(MMR, MMR, "1 day", "24 days", "28 days");
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

  /**
   * Stands in for an earlier trip round 4.4's target dose loop having evaluated
   * the previous vaccine dose administered: a satisfied target dose carrying the
   * evaluation status CALCDTCONFLICT-2's two branches turn on.
   */
  private void previousDoseWasEvaluated(AntigenAdministeredRecord previous,
      EvaluationStatus status) {
    SeriesDose previousSeriesDose = new SeriesDose();
    previousSeriesDose.setDoseNumber("1");
    TargetDose previousTargetDose = new TargetDose(previousSeriesDose);
    previousTargetDose.setSatisfiedByVaccineDoseAdministered(previous.getVaccineDoseAdministered());

    Evaluation previousEvaluation = new Evaluation();
    previousEvaluation.setEvaluationStatus(status);
    previousTargetDose.setEvaluation(previousEvaluation);

    dataModel.getTargetDoseList().add(previousTargetDose);
    dataModel.setPreviousTargetDose(previousTargetDose);
    dataModel.setPreviousAntigenAdministeredRecord(previous);
  }

  private EvaluateVaccineConflict construct() {
    step = new EvaluateVaccineConflict(dataModel);
    return step;
  }

  private LogicStep run() throws Exception {
    construct();
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  /**
   * Table 6-23's rows are published as {@code ConditionAttribute}s, but the two
   * calculated dates are created per candidate previous dose inside the
   * constructor rather than once at class level, so a row is found by its name
   * rather than by a fixed index.
   */
  private ConditionAttribute<?> attributeNamed(String attributeName) {
    for (ConditionAttribute<?> attribute : step.getConditionAttributeList()) {
      if (normalized(attributeName).equals(normalized(attribute.getAttributeName()))) {
        return attribute;
      }
    }
    return null;
  }

  private ConditionAttribute<?> attribute(int row) {
    List<ConditionAttribute<?>> attributes = step.getConditionAttributeList();
    assertTrue("Table 6-23 row " + (row + 1) + " is not registered", attributes.size() > row);
    return attributes.get(row);
  }

  private void assertConflictRecorded(String why) {
    assertTrue(why + ", but the status cause was '" + targetDose.getStatusCause() + "'",
        targetDose.getStatusCause().contains(CONFLICT_STATUS_CAUSE));
  }

  private void assertNoConflictRecorded(String why) {
    assertTrue(why + ", but the status cause was '" + targetDose.getStatusCause() + "'",
        !targetDose.getStatusCause().contains(CONFLICT_STATUS_CAUSE));
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

  // ==================================================== Entry: what 6.7's class is

  /** 6.7 identifies itself as {@code EVALUATE_VACCINE_CONFLICT}. */
  @Test
  public void theStepIsSixSeven() throws Exception {
    run();

    assertEquals(LogicStepType.EVALUATE_VACCINE_CONFLICT, step.getLogicStepType());
    assertEquals("6.7", LogicStepType.EVALUATE_VACCINE_CONFLICT.getChapter());
  }

  // ============================================ Table 6-23: the attribute table

  /**
   * Table 6-23 row 1: the date administered of the vaccine dose administered 4.4
   * has made current.
   */
  @Test
  public void tableSixTwentyThreeCarriesTheCurrentDosesDateAdministered() throws Exception {
    run();

    ConditionAttribute<?> dateAdministered = attributeNamed(DATE_ADMINISTERED);
    assertNotNull("Table 6-23 row 1 '" + DATE_ADMINISTERED + "' is not registered",
        dateAdministered);
    assertEquals(date(CURRENT_DOSE), dateAdministered.getFinalValue());
  }

  /**
   * Table 6-23 row 2: the vaccine type of the vaccine dose administered - the
   * Implementer Note's "impacted vaccine type", carried under the Supporting
   * Data's older "current vaccine type" wording.
   */
  @Test
  public void tableSixTwentyThreeCarriesTheCurrentDosesVaccineType() throws Exception {
    run();

    assertEquals("Table 6-23 row 2 carries the vaccine type of the vaccine dose administered",
        vaccineType(MMR), attribute(1).getFinalValue());
  }

  /**
   * Table 6-23's first two rows are both "Vaccine dose administered" attributes -
   * the date administered and the vaccine type of the dose being evaluated. The
   * Implementer Note explains the terminology migration for the <em>value</em>
   * ("Impacted Vaccine Type" is the Supporting Data's "Current Vaccine Type"), but
   * Table 6-23 still sources this row from the vaccine dose administered, not from
   * Supporting Data.
   */
  @Test
  public void tableSixTwentyThreeNamesItsAttributesAsTheSpecificationDoes() throws Exception {
    run();

    assertLabelIs("Vaccine dose administered", attribute(0).getAttributeType());
    assertLabelIs(DATE_ADMINISTERED, attribute(0).getAttributeName());
    assertLabelIs("Vaccine dose administered", attribute(1).getAttributeType());
    assertLabelIs(VACCINE_TYPE, attribute(1).getAttributeName());
  }

  // ====================================== Table 6-24 CALCDTCONFLICT-1

  /**
   * CALCDTCONFLICT-1: "The conflict begin interval date for a previous vaccine
   * dose administered must be calculated as the date administered of the previous
   * vaccine dose administered plus the conflict begin interval of a vaccine type
   * conflict where ... the vaccine type of the current vaccine dose administered
   * is an impacted vaccine type [and] the vaccine type of the previous vaccine
   * dose administered is a conflicting vaccine type for the impacted vaccine
   * type."
   *
   * <p>
   * Previous MMR dose 01/01/2016 plus the bundled conflict's "1 day" begin
   * interval is 01/02/2016.
   */
  @Test
  public void calcdtconflictOneCalculatesTheConflictBeginIntervalDate() throws Exception {
    run();

    ConditionAttribute<?> beginIntervalDate = attributeNamed(CONFLICT_BEGIN_INTERVAL_DATE);
    assertNotNull("CALCDTCONFLICT-1 must produce a '" + CONFLICT_BEGIN_INTERVAL_DATE
        + "' for the previous vaccine dose administered", beginIntervalDate);
    assertEquals("previous dose " + PREVIOUS_DOSE + " plus the conflict begin interval of 1 day",
        date("01/02/2016"), beginIntervalDate.getFinalValue());
  }

  // ====================================== Table 6-24 CALCDTCONFLICT-2

  /**
   * CALCDTCONFLICT-2's first branch: the conflict end interval date is "the date
   * administered of the previous vaccine dose administered plus the minimum
   * conflict end interval of a vaccine type conflict if ... the previous vaccine
   * dose administered has ... no evaluation status."
   *
   * <p>
   * Previous MMR dose 01/01/2016 plus the bundled conflict's "24 days" minimum
   * conflict end interval is 01/25/2016.
   */
  @Test
  public void calcdtconflictTwoUsesTheMinimumEndIntervalWhenThePreviousDoseHasNoEvaluationStatus()
      throws Exception {
    run();

    ConditionAttribute<?> endIntervalDate = attributeNamed(CONFLICT_END_INTERVAL_DATE);
    assertNotNull("CALCDTCONFLICT-2 must produce a '" + CONFLICT_END_INTERVAL_DATE
        + "' for the previous vaccine dose administered", endIntervalDate);
    assertEquals("an unevaluated previous dose takes the minimum conflict end interval "
        + "of 24 days from " + PREVIOUS_DOSE,
        date("01/25/2016"), endIntervalDate.getFinalValue());
  }

  /**
   * CALCDTCONFLICT-2's first branch again, for the other half of its condition: a
   * previous vaccine dose administered with "an evaluation status of 'Valid'"
   * takes the same minimum conflict end interval as one with no status at all.
   */
  @Test
  public void calcdtconflictTwoUsesTheMinimumEndIntervalWhenThePreviousDoseIsValid()
      throws Exception {
    AntigenAdministeredRecord previous = administeredRecord(PREVIOUS_DOSE, MMR);
    immunizationHistory(1, previous, administeredRecord(CURRENT_DOSE, MMR));
    previousDoseWasEvaluated(previous, EvaluationStatus.VALID);

    run();

    ConditionAttribute<?> endIntervalDate = attributeNamed(CONFLICT_END_INTERVAL_DATE);
    assertNotNull("CALCDTCONFLICT-2 must produce a '" + CONFLICT_END_INTERVAL_DATE
        + "' for the previous vaccine dose administered", endIntervalDate);
    assertEquals("a 'Valid' previous dose takes the minimum conflict end interval of 24 days",
        date("01/25/2016"), endIntervalDate.getFinalValue());
  }

  /**
   * CALCDTCONFLICT-2's second branch: the conflict end interval date is "the date
   * administered of the previous vaccine dose administered plus the conflict end
   * interval of a vaccine type conflict if ... the previous vaccine dose
   * administered has an evaluation status [and] does not have an evaluation status
   * of 'Valid'."
   *
   * <p>
   * Previous MMR dose 01/01/2016 plus the bundled conflict's "28 days" conflict
   * end interval is 01/29/2016 - four days later than the 'Valid' branch, so the
   * two branches are distinguishable on the release's own data.
   */
  @Test
  public void calcdtconflictTwoUsesTheConflictEndIntervalWhenThePreviousDoseIsNotValid()
      throws Exception {
    AntigenAdministeredRecord previous = administeredRecord(PREVIOUS_DOSE, MMR);
    immunizationHistory(1, previous, administeredRecord(CURRENT_DOSE, MMR));
    previousDoseWasEvaluated(previous, EvaluationStatus.NOT_VALID);

    run();

    ConditionAttribute<?> endIntervalDate = attributeNamed(CONFLICT_END_INTERVAL_DATE);
    assertNotNull("CALCDTCONFLICT-2 must produce a '" + CONFLICT_END_INTERVAL_DATE
        + "' for the previous vaccine dose administered", endIntervalDate);
    assertEquals("a previous dose that is not 'Valid' takes the conflict end interval "
        + "of 28 days from " + PREVIOUS_DOSE,
        date("01/29/2016"), endIntervalDate.getFinalValue());
  }

  /**
   * The structural claim underneath both calculated dates, asserted without
   * depending on <em>which</em> recorded dose the step treats as the previous one.
   * CALCDTCONFLICT-1 names the conflict <em>begin</em> interval and
   * CALCDTCONFLICT-2 names one of the two conflict <em>end</em> intervals, so for
   * any real vaccine type conflict the two calculated dates must be different -
   * otherwise CONFLICT-3's window is empty and no date administered can ever fall
   * inside it.
   *
   * <p>
   * The fixture puts a conflicting MMR dose on both sides of the current dose so
   * that both dates get published whichever direction the step searches.
   *
   * <p>
   * Every one of the 625 {@code <liveVirusConflict>} entries in the Supporting
   * Data release bundled in {@code cdsi-engine/src/main/resources} has a conflict
   * begin interval of "1 day" and end intervals between "24 days" and "30 days",
   * so this holds for all 625 of them.
   */
  @Test
  public void theConflictEndIntervalDateIsLaterThanTheConflictBeginIntervalDate() throws Exception {
    immunizationHistory(1,
        administeredRecord(PREVIOUS_DOSE, MMR),
        administeredRecord(CURRENT_DOSE, MMR),
        administeredRecord("02/01/2016", MMR));

    run();

    ConditionAttribute<?> beginIntervalDate = attributeNamed(CONFLICT_BEGIN_INTERVAL_DATE);
    ConditionAttribute<?> endIntervalDate = attributeNamed(CONFLICT_END_INTERVAL_DATE);
    assertNotNull(beginIntervalDate);
    assertNotNull(endIntervalDate);
    assertTrue("CALCDTCONFLICT-1's begin interval (1 day) and CALCDTCONFLICT-2's end interval "
        + "(24 or 28 days) are different intervals, so the conflict end interval date ("
        + endIntervalDate.getFinalValue() + ") must be later than the conflict begin interval "
        + "date (" + beginIntervalDate.getFinalValue() + ")",
        ((Date) endIntervalDate.getFinalValue()).after((Date) beginIntervalDate.getFinalValue()));
  }

  // ============================================= Table 6-24 CONFLICT-3

  /**
   * CONFLICT-3: "A current vaccine dose administered must be considered an
   * impacted vaccine dose administered if all the following are true for the date
   * administered of the current vaccine dose administered: it is on or after the
   * conflict begin interval date; it is before the conflict end interval date."
   *
   * <p>
   * The default fixture: a previous MMR dose on 01/01/2016 and the current MMR
   * dose on 01/15/2016, inside the 01/02/2016 - 01/25/2016 window. The conflict is
   * recorded on the target dose's status cause for 6.10 Evaluate Target Dose to
   * read later.
   */
  @Test
  public void conflictThreeReportsADoseInsideTheConflictWindow() throws Exception {
    run();

    assertConflictRecorded("a dose administered inside the conflict window is in conflict");
  }

  /**
   * CONFLICT-3's first bullet is "on or after the conflict begin interval date",
   * so the conflict begin interval date itself is inside the window. Previous MMR
   * dose 01/01/2016 gives a begin interval date of 01/02/2016; a current dose on
   * exactly that day is in conflict.
   */
  @Test
  public void conflictThreeIncludesTheConflictBeginIntervalDateItself() throws Exception {
    immunizationHistory(1, administeredRecord(PREVIOUS_DOSE, MMR),
        administeredRecord("01/02/2016", MMR));

    run();

    assertConflictRecorded("the conflict begin interval date itself is inside the window");
  }

  /**
   * The day before the conflict begin interval date is outside the window. Here
   * the two doses are given on the same day, 01/01/2016, which is before the
   * 01/02/2016 begin interval date.
   *
   * <p>
   * This currently passes for the wrong reason - the implementation records no
   * conflict for any arrangement of this fixture - but it pins the lower boundary
   * for whoever brings the step in line.
   */
  @Test
  public void conflictThreeExcludesADoseBeforeTheConflictBeginIntervalDate() throws Exception {
    immunizationHistory(1, administeredRecord(PREVIOUS_DOSE, MMR),
        administeredRecord(PREVIOUS_DOSE, MMR));

    run();

    assertNoConflictRecorded("a dose administered before the conflict begin interval date "
        + "is outside the window");
  }

  /**
   * CONFLICT-3's second bullet is "before the conflict end interval date", so the
   * conflict end interval date itself is outside the window. Previous MMR dose
   * 01/01/2016 with no evaluation status gives a conflict end interval date of
   * 01/25/2016; a current dose on exactly that day is not in conflict.
   *
   * <p>
   * Like the lower-boundary test above, this currently passes for the wrong
   * reason.
   */
  @Test
  public void conflictThreeExcludesTheConflictEndIntervalDateItself() throws Exception {
    immunizationHistory(1, administeredRecord(PREVIOUS_DOSE, MMR),
        administeredRecord("01/25/2016", MMR));

    run();

    assertNoConflictRecorded("the conflict end interval date itself is outside the window");
  }

  /**
   * A dose administered well after the conflict end interval date is outside the
   * window. 01/01/2016 plus 28 days - the longest of the bundled conflict's three
   * intervals - is 01/29/2016, so a dose on 06/01/2016 is clear of it either way.
   */
  @Test
  public void conflictThreeExcludesADoseAfterTheConflictEndIntervalDate() throws Exception {
    immunizationHistory(1, administeredRecord(PREVIOUS_DOSE, MMR),
        administeredRecord("06/01/2016", MMR));

    run();

    assertNoConflictRecorded("a dose administered after the conflict end interval date "
        + "is outside the window");
  }

  // ============================== Section 6.7: when there is no conflict at all

  /**
   * Section 6.7: "Many vaccines do not have any conflict with each other.
   * Therefore, if no vaccine Supporting Data exists for the vaccine type of the
   * vaccine dose administered being evaluated, the vaccine dose administered is
   * not in conflict with any other vaccine dose administered." Here the current
   * dose is Varicella, which the fixture's only conflict does not name as an
   * impacted vaccine type.
   */
  @Test
  public void aVaccineTypeThatIsNotAnImpactedTypeIsNeverInConflict() throws Exception {
    immunizationHistory(1, administeredRecord(PREVIOUS_DOSE, MMR),
        administeredRecord(CURRENT_DOSE, VARICELLA));

    run();

    assertNoConflictRecorded("Varicella is not an impacted vaccine type in the fixture's "
        + "conflict Supporting Data");
  }

  /** The same rule with no conflict Supporting Data loaded at all. */
  @Test
  public void noConflictSupportingDataMeansNoConflict() throws Exception {
    dataModel.getLiveVirusConflictList().clear();

    run();

    assertNoConflictRecorded("no vaccine conflict Supporting Data means no conflict");
  }

  /**
   * CALCDTCONFLICT-1 and -2 both require that "the vaccine type of the previous
   * vaccine dose administered is a conflicting vaccine type for the impacted
   * vaccine type" - it is the <em>pairing</em> that has to be defined, not either
   * type on its own. Here the current MMR dose is an impacted type, but the
   * previous Varicella dose is not a conflicting type for it.
   */
  @Test
  public void aPreviousDoseThatIsNotAConflictingTypeForTheCurrentTypeIsNotAConflict()
      throws Exception {
    immunizationHistory(1, administeredRecord(PREVIOUS_DOSE, VARICELLA),
        administeredRecord(CURRENT_DOSE, MMR));

    run();

    assertNoConflictRecorded("Varicella is not a conflicting vaccine type for MMR in the "
        + "fixture's conflict Supporting Data");
  }

  /**
   * Section 6.7 validates the current dose "against previous administered
   * vaccines". With nothing administered on or before it there is nothing for it
   * to conflict with, so no conflict can be recorded.
   *
   * <p>
   * A full pipeline run never reaches this state - the current vaccine dose
   * administered is itself a member of the immunization history, so there is
   * always at least one dose administered on or before it - which makes this a
   * latent defect rather than a live one. It is reachable from any caller that
   * constructs the step against a {@code DataModel} whose immunization history has
   * not been populated.
   */
  @Test
  public void noVaccineDoseAdministeredOnOrBeforeTheCurrentOneMeansNoConflict() throws Exception {
    AntigenAdministeredRecord current = administeredRecord(CURRENT_DOSE, MMR);
    dataModel.setAntigenAdministeredRecordList(new ArrayList<AntigenAdministeredRecord>());
    dataModel.setSelectedAntigenAdministeredRecordList(
        new ArrayList<AntigenAdministeredRecord>(Arrays.asList(current)));
    dataModel.setSelectedAntigenAdministeredRecordPos(0);
    dataModel.setAntigenAdministeredRecord(current);

    run();

    assertNoConflictRecorded("with no vaccine dose administered on or before the current one "
        + "there is nothing to be in conflict with");
  }

  // ======================================================== The state change

  /**
   * The step package's State Changes: a detected conflict appends its marker to
   * whatever the earlier chapter-6 steps have already recorded rather than
   * replacing it, so a dose that failed both age and vaccine conflict carries both
   * markers into 6.10.
   */
  @Test
  public void aDetectedConflictIsAppendedToTheExistingStatusCause() throws Exception {
    targetDose.setStatusCause("Age");

    run();

    assertEquals("the conflict marker is appended to the existing status cause rather than "
        + "replacing it", "Age" + CONFLICT_STATUS_CAUSE, targetDose.getStatusCause());
  }

  /**
   * No conflict means no marker - the status cause the earlier chapter-6 steps
   * recorded is left exactly as it was.
   */
  @Test
  public void noConflictLeavesTheStatusCauseUntouched() throws Exception {
    targetDose.setStatusCause("Age");
    dataModel.getLiveVirusConflictList().clear();

    run();

    assertEquals("Age", targetDose.getStatusCause());
  }

  // ============================================ Constructing the step

  /**
   * Constructing a step must not change the shared state a step records into -
   * that is what {@code process()} is for. Every other chapter-6 step's
   * constructor only reads the {@code DataModel} and builds its condition
   * attributes; this one evaluates a decision table straight away, which is why
   * this is worth pinning explicitly.
   *
   * <p>
   * This is the ordinary case, with an immunization history populated the way 4.2
   * leaves it.
   */
  @Test
  public void constructingTheStepDoesNotRecordAnythingWhenTheHistoryIsPopulated()
      throws Exception {
    targetDose.setStatusCause("Age");

    construct();

    assertEquals("constructing the step must not change the target dose's status cause",
        "Age", targetDose.getStatusCause());
  }

  /**
   * The same rule for the case where nothing has been administered on or before
   * the current dose. Constructing the step is not evaluating it, so whatever the
   * step decides about this history, it must not be recorded before
   * {@code process()} runs.
   */
  @Test
  public void constructingTheStepDoesNotRecordAnythingWhenThereIsNoPreviousDose()
      throws Exception {
    AntigenAdministeredRecord current = administeredRecord(CURRENT_DOSE, MMR);
    dataModel.setAntigenAdministeredRecordList(new ArrayList<AntigenAdministeredRecord>());
    dataModel.setSelectedAntigenAdministeredRecordList(
        new ArrayList<AntigenAdministeredRecord>(Arrays.asList(current)));
    dataModel.setSelectedAntigenAdministeredRecordPos(0);
    dataModel.setAntigenAdministeredRecord(current);
    targetDose.setStatusCause("Age");

    construct();

    assertEquals("constructing the step must not change the target dose's status cause",
        "Age", targetDose.getStatusCause());
  }

  // ================================================================ Next step

  /**
   * The step package's {@code transitions.yaml}: unconditional to 6.8 Evaluate for
   * Preferable Vaccine. A detected conflict is recorded for 6.10, not branched on
   * here.
   */
  @Test
  public void aDetectedConflictContinuesToSixEight() throws Exception {
    run();

    assertEquals(LogicStepType.EVALUATE_FOR_PREFERABLE_VACCINE, step.getNextLogicStepType());
  }

  /** The same transition when no conflict is found. */
  @Test
  public void noConflictAlsoContinuesToSixEight() throws Exception {
    dataModel.getLiveVirusConflictList().clear();

    run();

    assertEquals(LogicStepType.EVALUATE_FOR_PREFERABLE_VACCINE, step.getNextLogicStepType());
  }

  // ================== Table 6-23's Supporting Data row against the real release

  /**
   * Confirms Table 6-23's "Live Virus Conflicts" Supporting Data row can actually
   * be filled from the bundled release, and that all three intervals
   * CALCDTCONFLICT-1 and CALCDTCONFLICT-2 name arrive distinct: the release's own
   * MMR-after-MMR markup, verbatim, read through
   * {@code DataModelLoader.readLiveVirusConfict}.
   *
   * <p>
   * The loader is not the gap here - it reads {@code conflictBeginInterval},
   * {@code minConflictEndInterval} and {@code conflictEndInterval} onto three
   * separate fields of {@code LiveVirusConflict}, which is what makes
   * CALCDTCONFLICT-2's two branches representable at all.
   */
  @Test
  public void theSupportingDatasLiveVirusConflictMarkupCarriesThreeDistinctIntervals()
      throws Exception {
    LiveVirusConflict loaded = new LiveVirusConflict();
    readLiveVirusConflict(loaded, ""
        + "<liveVirusConflict>"
        + "<previous><vaccineType>MMR</vaccineType><cvx>03</cvx></previous>"
        + "<current><vaccineType>MMR</vaccineType><cvx>03</cvx></current>"
        + "<conflictBeginInterval>1 day</conflictBeginInterval>"
        + "<minConflictEndInterval>24 days</minConflictEndInterval>"
        + "<conflictEndInterval>28 days</conflictEndInterval>"
        + "</liveVirusConflict>");

    assertEquals("the conflicting ('previous') vaccine type", "03",
        loaded.getPreviousVaccineType().getCvxCode());
    assertEquals("the impacted ('current') vaccine type", "03",
        loaded.getCurrentVaccineType().getCvxCode());

    Date previousDose = date(PREVIOUS_DOSE);
    assertNotNull("CALCDTCONFLICT-1 needs the conflict begin interval",
        loaded.getConflictBeginInterval());
    assertEquals(date("01/02/2016"), loaded.getConflictBeginInterval().getDateFrom(previousDose));
    assertNotNull("CALCDTCONFLICT-2's first branch needs the minimum conflict end interval",
        loaded.getMinimalConflictEndInterval());
    assertEquals(date("01/25/2016"),
        loaded.getMinimalConflictEndInterval().getDateFrom(previousDose));
    assertNotNull("CALCDTCONFLICT-2's second branch needs the conflict end interval",
        loaded.getConflictEndInterval());
    assertEquals(date("01/29/2016"), loaded.getConflictEndInterval().getDateFrom(previousDose));
  }

  /**
   * Invokes {@code DataModelLoader.readLiveVirusConfict} - private, like the
   * loader's other per-element readers - on one {@code <liveVirusConflict>}
   * element. (The method's name is misspelled in the loader; it is spelled here as
   * it is declared there.)
   */
  private void readLiveVirusConflict(LiveVirusConflict target, String liveVirusConflictXml)
      throws Exception {
    DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = documentBuilder.parse(
        new ByteArrayInputStream(liveVirusConflictXml.getBytes(Charset.forName("UTF-8"))));
    Node node = document.getDocumentElement();

    Method readLiveVirusConfict = DataModelLoader.class.getDeclaredMethod("readLiveVirusConfict",
        DataModel.class, LiveVirusConflict.class, Node.class);
    readLiveVirusConfict.setAccessible(true);
    try {
      readLiveVirusConfict.invoke(null, dataModel, target, node);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof Exception) {
        throw (Exception) ite.getCause();
      }
      throw ite;
    }
  }
}
