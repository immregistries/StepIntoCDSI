package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
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
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * Section 6.10 "Satisfy Target Dose" (Logic Specification for ACIP
 * Recommendations v4.6, pages 69-70, Figure 6-23, Table 6-31) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/06-10-satisfy-target-dose/index.md}.
 *
 * <p>
 * "Satisfy target dose uses the results from the previous evaluation sections as
 * conditions to determine if the target dose is satisfied." It is Chapter 6's
 * final, combining step: it recomputes nothing, it only reads what 6.4 through
 * 6.9 left behind and turns it into one answer.
 *
 * <pre>
 * Table 6-31 Was the Target Dose Satisfied?
 *
 *   Condition                                 R1    R2     R3   R4   R5   R6
 *   Valid age for the target dose?            Yes   Extr.  No   -    -    -
 *   Satisfied all preferable/allowable
 *     intervals?                              Yes   -      -    No   -    -
 *   Is this an impacted (conflicting) dose?   No    -      -    -    Yes  -
 *   Preferable or allowable vaccine?          Yes   -      -    -    -    No
 *
 *   Outcome                                   Sat-  Not    Not  Not  Not  Not
 *                                             is-   Satis- Sat- Sat- Sat- Sat-
 *                                             fied  fied   is-  is-  is-  is-
 *                                             (Va-  (Extr- fied fied fied fied
 *                                             lid)  aneous)(Not (Not (Not (Not
 *                                                          Va-  Va-  Va-  Va-
 *                                                          lid) lid) lid) lid)
 * </pre>
 *
 * <h2>What the four conditions actually read</h2>
 *
 * <p>
 * The step package's Inputs and Attributes section records that Table 6-31's
 * four conditions are summaries of prior steps' outcomes rather than raw data
 * values, and the implementation reads them from exactly two places:
 *
 * <ul>
 * <li>Condition 1 reads {@code targetDose.getEvaluation().getEvaluationStatus()},
 * which 6.4 Evaluate Age records. {@code VALID} is Yes,
 * {@code EXTRANEOUS} is Extraneous, everything else is No.</li>
 * <li>Conditions 2, 3 and 4 are substring tests against one shared
 * {@code targetDose.getStatusCause()} string that earlier steps append markers
 * to: {@code "Interval"}, {@code "VirusConflict"} and {@code "Vaccine"}.</li>
 * </ul>
 *
 * <p>
 * Which steps actually write those markers is worth stating exactly, because it
 * is narrower than the step package's prose suggests. Across the whole of
 * {@code cdsi-engine} there are exactly four writers of {@code statusCause}:
 * {@code EvaluateAllowableInterval} (6.6) appends {@code "Interval"},
 * {@code EvaluateVaccineConflict} (6.7) appends {@code "VirusConflict"},
 * {@code EvaluateForAllowableVaccine} (6.9) appends {@code "Vaccine"}, and
 * {@code EvaluateGender} appends {@code "Gender"}. 6.5 Evaluate Preferable
 * Interval and 6.8 Evaluate for Preferable Vaccine write nothing at all - a
 * preferable-interval miss routes on to 6.6 and a preferable-vaccine miss routes
 * on to 6.9, so only the allowable fallback failing records anything. That is
 * exactly what Table 6-31's second and fourth conditions ask for ("all
 * preferable intervals <em>or</em> all allowable intervals", "a preferable
 * vaccine <em>or</em> an allowable vaccine"), so the narrower marker set is
 * correct rather than a gap. These tests therefore write the markers the way the
 * upstream steps write them - by appending to whatever is already there - rather
 * than assuming a marker per step.
 *
 * <p>
 * Because all three conditions are {@code String.contains} tests on one
 * concatenated string, they are order-independent and duplicate-tolerant.
 * {@link #markerOrderDoesNotChangeTheOutcome()} and
 * {@link #aDuplicatedVirusConflictMarkerIsStillReadOnce()} pin both, the latter
 * because 6.7 can append its own marker twice in one pass.
 * {@link #theGenderMarkerIsNotOneOfTableSixThirtyOnesConditions()} pins that the
 * fourth writer's marker is ignored, which is what Table 6-31 - which has no
 * gender condition - requires.
 *
 * <h2>What is deliberately not asserted</h2>
 *
 * <p>
 * Figure 6-23's process model, which draws the same four questions Table 6-31
 * tabulates and adds no rule of its own.
 *
 * <p>
 * The condition labels. Table 6-31's condition column is transcribed in the step
 * package in abbreviated form ("Valid age for the target dose?"), while the
 * implementation carries the specification's longer prose ("Was the vaccine dose
 * administered at a valid age for the target dose?"). Asserting either against
 * the other would pin a transcription difference, not a behaviour, so these
 * tests pin the grid and the outcomes instead.
 *
 * <p>
 * A null {@code Evaluation}. Condition 1 handles it (returning No), but Rules 3
 * to 6's outcomes all call {@code getEvaluation().setEvaluationStatus(...)}
 * unguarded, so a null evaluation would throw rather than produce a Table 6-31
 * outcome. It is unreachable in the real pipeline - 6.4 {@code process()} throws
 * a {@code NullPointerException} of its own if it has not recorded an evaluation
 * by the time it hands off - and the specification says nothing about the case,
 * so pinning an interpretation of it would be guessing.
 *
 * <p>
 * The {@code SKIPPED} branch of {@code process()}'s control log. Table 6-31 has
 * no skipped outcome and no outcome in the table sets {@code SKIPPED}; the
 * branch reports a status some other step set, so there is nothing of 6.10's own
 * to pin.
 *
 * <p>
 * The structured log events, for the same reason 6.3 through 6.9 left theirs
 * alone.
 */
public class SatisfyTargetDoseTest {

  /** What 6.6 Evaluate Allowable Interval appends when no interval was satisfied. */
  private static final String INTERVAL = "Interval";
  /** What 6.7 Evaluate Vaccine Conflict appends for an impacted dose. */
  private static final String VIRUS_CONFLICT = "VirusConflict";
  /** What 6.9 Evaluate for Allowable Vaccine appends when no allowable vaccine matched. */
  private static final String VACCINE = "Vaccine";
  /** What {@code EvaluateGender} appends. Table 6-31 has no condition for it. */
  private static final String GENDER = "Gender";

  /**
   * Table 6-31's grid, transcribed from the specification. Condition rows in the
   * table's own order, rule columns 1 to 6, with a dash written as
   * {@link LogicResult#ANY}.
   */
  private static final LogicResult[][] TABLE_SIX_THIRTY_ONE = {
      { LogicResult.YES, LogicResult.EXTRANEOUS, LogicResult.NO,
          LogicResult.ANY, LogicResult.ANY, LogicResult.ANY },
      { LogicResult.YES, LogicResult.ANY, LogicResult.ANY,
          LogicResult.NO, LogicResult.ANY, LogicResult.ANY },
      { LogicResult.NO, LogicResult.ANY, LogicResult.ANY,
          LogicResult.ANY, LogicResult.YES, LogicResult.ANY },
      { LogicResult.YES, LogicResult.ANY, LogicResult.ANY,
          LogicResult.ANY, LogicResult.ANY, LogicResult.NO },
  };

  private DataModel dataModel;
  private TargetDose targetDose;
  private Evaluation evaluation;
  private VaccineDoseAdministered vaccineDoseAdministered;
  private SatisfyTargetDose step;

  @Before
  public void setUp() {
    dataModel = new DataModel();

    Patient patient = new Patient();
    patient.setDateOfBirth(date("01/01/2015"));
    dataModel.setPatient(patient);
    dataModel.setAssessmentDate(date("06/01/2021"));
    dataModel.setTargetDoseList(new ArrayList<TargetDose>());

    SeriesDose seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("1");
    targetDose = new TargetDose(seriesDose);
    dataModel.setTargetDose(targetDose);

    // Stands in for 4.4 having made this vaccine dose administered the current one.
    VaccineType vaccineType = new VaccineType();
    vaccineType.setCvxCode("20");
    vaccineType.setShortDescription("DTaP");

    Vaccine vaccine = new Vaccine();
    vaccine.setVaccineType(vaccineType);

    vaccineDoseAdministered = new VaccineDoseAdministered();
    vaccineDoseAdministered.setVaccine(vaccine);
    vaccineDoseAdministered.setDateAdministered(date("06/01/2016"));

    AntigenAdministeredRecord aar = new AntigenAdministeredRecord();
    aar.setDateAdministered(date("06/01/2016"));
    aar.setVaccineType(vaccineType);
    aar.setVaccineDoseAdministered(vaccineDoseAdministered);
    dataModel.setAntigenAdministeredRecord(aar);

    // 6.4 Evaluate Age always records an evaluation before 6.10 can run, and
    // Table 6-31's first condition is the status it recorded. The default fixture
    // is the Rule 1 shape: a valid age and an empty status cause.
    evaluation = new Evaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.VALID);
    evaluation.setVaccineDoseAdministered(vaccineDoseAdministered);
    targetDose.setEvaluation(evaluation);

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

  /** Stands in for 6.4 Evaluate Age having recorded this evaluation status. */
  private void ageEvaluatedAs(EvaluationStatus evaluationStatus, EvaluationReason reason) {
    evaluation.setEvaluationStatus(evaluationStatus);
    evaluation.setEvaluationReason(reason);
  }

  /**
   * Appends a marker to the target dose's status cause exactly the way 6.6, 6.7
   * and 6.9 do it - {@code setStatusCause(getStatusCause() + marker)} - so that
   * several markers accumulate the same way they do in the pipeline.
   */
  private void upstreamRecorded(String marker) {
    targetDose.setStatusCause(targetDose.getStatusCause() + marker);
  }

  private SatisfyTargetDose construct() {
    step = new SatisfyTargetDose(dataModel);
    return step;
  }

  private LogicStep run() throws Exception {
    construct();
    return step.process();
  }

  // ------------------------------------------------------- reading the step

  private LogicTable tableSixThirtyOne() {
    assertEquals("6.10 has one decision table", 1, step.getLogicTableList().size());
    return step.getLogicTableList().get(0);
  }

  private LogicResult conditionResult(int condition) {
    return tableSixThirtyOne().getLogicConditions()[condition].getLogicResult();
  }

  /**
   * Table 6-31's four condition answers for the rule under test, in the table's
   * own row order. A dash is written as {@code null} - the condition is still
   * evaluated, the rule just does not care what it answered.
   */
  private void assertConditionsWere(LogicResult age, LogicResult interval,
      LogicResult conflict, LogicResult vaccine) {
    if (age != null) {
      assertEquals("Table 6-31 condition 1, valid age for the target dose",
          age, conditionResult(0));
    }
    if (interval != null) {
      assertEquals("Table 6-31 condition 2, satisfied all preferable or allowable intervals",
          interval, conditionResult(1));
    }
    if (conflict != null) {
      assertEquals("Table 6-31 condition 3, is this an impacted vaccine dose administered",
          conflict, conditionResult(2));
    }
    if (vaccine != null) {
      assertEquals("Table 6-31 condition 4, a preferable or an allowable vaccine",
          vaccine, conditionResult(3));
    }
  }

  /** Table 6-31's outcome row: a target dose status and an evaluation status. */
  private void assertOutcomeWas(String rule, TargetDoseStatus targetDoseStatus,
      EvaluationStatus evaluationStatus) {
    assertEquals(rule + " - the target dose status",
        targetDoseStatus, targetDose.getTargetDoseStatus());
    assertEquals(rule + " - the evaluation status",
        evaluationStatus, targetDose.getEvaluation().getEvaluationStatus());
  }

  /**
   * Only Rule 1 records the "this vaccine dose administered satisfies this target
   * dose" linkage, so its absence is how the five Not Satisfied rules are told
   * apart from Rule 1 even though four of them share an evaluation status.
   */
  private void assertNotLinked(String why) {
    assertNull(why + " - no vaccine dose administered should satisfy the target dose",
        targetDose.getSatisfiedByVaccineDoseAdministered());
    assertNull(why + " - and the dose should not point back at the target dose",
        vaccineDoseAdministered.getTargetDose());
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

  // =================================================== Entry: what 6.10's class is

  /** 6.10 identifies itself as {@code SATISFY_TARGET_DOSE}. */
  @Test
  public void theStepIsSixTen() throws Exception {
    run();

    assertEquals(LogicStepType.SATISFY_TARGET_DOSE, step.getLogicStepType());
    assertEquals("6.10", LogicStepType.SATISFY_TARGET_DOSE.getChapter());
  }

  /**
   * "No separate attribute table for this section - Table 6-31's four conditions
   * are themselves the inputs, each a summary of a prior step's outcome rather
   * than a raw data value." So 6.10 publishes no condition attributes.
   */
  @Test
  public void thereIsNoAttributeTable() throws Exception {
    run();

    assertEquals("6.10 has no attribute table to publish",
        0, step.getConditionAttributeList().size());
  }

  /** The specification records no business rules for this section. */
  @Test
  public void thereAreNoBusinessRules() throws Exception {
    run();

    assertEquals("6.10 has no business rules", 0, step.getBusinessRuleList().size());
  }

  // ============================================ Table 6-31: the decision table

  /**
   * Table 6-31 "Was the Target Dose Satisfied?" has four conditions and six
   * rules.
   */
  @Test
  public void theDecisionTableIsTableSixThirtyOne() throws Exception {
    run();

    LogicTable table = tableSixThirtyOne();
    assertTrue("the decision table should identify itself as Table 6-31 but was '"
        + table.getLabel() + "'", normalized(table.getLabel()).contains("table631"));
    assertEquals("Table 6-31 has four conditions", 4, table.getLogicConditions().length);
    assertEquals("Table 6-31 has six rules", 6, table.getLogicOutcomes().length);
  }

  /** Table 6-31's grid, condition by condition and rule by rule. */
  @Test
  public void theDecisionTableGridMatchesTableSixThirtyOne() throws Exception {
    run();

    assertArrayEquals("Table 6-31's condition/rule grid",
        TABLE_SIX_THIRTY_ONE, tableSixThirtyOne().getLogicResultTable());
  }

  // ============================================= Table 6-31 Rule 1

  /**
   * Table 6-31 Rule 1: a valid age, every preferable or allowable interval
   * satisfied, no conflict, and a preferable or allowable vaccine - "Satisfied",
   * with an evaluation status of "Valid".
   */
  @Test
  public void ruleOneSatisfiesTheTargetDose() throws Exception {
    run();

    assertConditionsWere(LogicResult.YES, LogicResult.YES, LogicResult.NO, LogicResult.YES);
    assertOutcomeWas("Table 6-31 Rule 1",
        TargetDoseStatus.SATISFIED, EvaluationStatus.VALID);
  }

  /**
   * Rule 1 is the only rule that records the linkage the rest of the engine
   * relies on: the target dose remembers which vaccine dose administered
   * satisfied it, and that dose remembers which target dose it satisfied.
   */
  @Test
  public void ruleOneLinksTheVaccineDoseAdministeredToTheTargetDose() throws Exception {
    run();

    assertSame("Rule 1 records which vaccine dose administered satisfied the target dose",
        vaccineDoseAdministered, targetDose.getSatisfiedByVaccineDoseAdministered());
    assertSame("and links the vaccine dose administered back to this target dose",
        targetDose, vaccineDoseAdministered.getTargetDose());
  }

  /**
   * Table 6-31's outcome row names an evaluation status, not an evaluation
   * reason. 6.4 Evaluate Age's own Table 6-15 can record a valid age <em>with</em>
   * a grace period reason, and Rule 1 leaves that reason where it found it.
   */
  @Test
  public void ruleOnePreservesTheEvaluationReasonSixFourRecorded()
      throws Exception {
    ageEvaluatedAs(EvaluationStatus.VALID, EvaluationReason.GRACE_PERIOD);

    run();

    assertOutcomeWas("Table 6-31 Rule 1 with a grace period",
        TargetDoseStatus.SATISFIED, EvaluationStatus.VALID);
    assertEquals("Rule 1 does not disturb the evaluation reason 6.4 recorded",
        EvaluationReason.GRACE_PERIOD, targetDose.getEvaluation().getEvaluationReason());
  }

  // ============================================= Table 6-31 Rule 2

  /**
   * Table 6-31 Rule 2: the vaccine dose administered was not at a valid age
   * because it was extraneous - "Not Satisfied", with an evaluation status of
   * "Extraneous". The other three conditions are dashes.
   */
  @Test
  public void ruleTwoReportsExtraneous() throws Exception {
    ageEvaluatedAs(EvaluationStatus.EXTRANEOUS, EvaluationReason.TOO_OLD);

    run();

    assertConditionsWere(LogicResult.EXTRANEOUS, null, null, null);
    assertOutcomeWas("Table 6-31 Rule 2",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.EXTRANEOUS);
    assertNotLinked("an extraneous dose does not satisfy the target dose");
  }

  // ============================================= Table 6-31 Rule 3

  /**
   * Table 6-31 Rule 3: the vaccine dose administered was not at a valid age for
   * the target dose - "Not Satisfied", with an evaluation status of "Not Valid".
   * 6.4's too-young outcome is what lands here.
   */
  @Test
  public void ruleThreeReportsNotValidWhenTheAgeWasNotValid() throws Exception {
    ageEvaluatedAs(EvaluationStatus.NOT_VALID, EvaluationReason.TOO_YOUNG);

    run();

    assertConditionsWere(LogicResult.NO, null, null, null);
    assertOutcomeWas("Table 6-31 Rule 3",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.NOT_VALID);
    assertNotLinked("a dose administered at an invalid age does not satisfy the target dose");
  }

  /**
   * Table 6-31's first condition has exactly three answers - Yes, Extraneous and
   * No - so any evaluation status that is neither Valid nor Extraneous is a No
   * and lands on Rule 3. {@code SUB_STANDARD} is the fourth
   * {@code EvaluationStatus} the domain model defines.
   */
  @Test
  public void anySkippedOrSubStandardEvaluationStatusIsAnAgeAnswerOfNo() throws Exception {
    ageEvaluatedAs(EvaluationStatus.SUB_STANDARD, null);

    run();

    assertConditionsWere(LogicResult.NO, null, null, null);
    assertOutcomeWas("Table 6-31 Rule 3",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.NOT_VALID);
  }

  // ============================================= Table 6-31 Rule 4

  /**
   * Table 6-31 Rule 4: the vaccine dose administered did not satisfy all
   * preferable intervals or all allowable intervals - "Not Satisfied", with an
   * evaluation status of "Not Valid". The marker is the one 6.6 Evaluate
   * Allowable Interval appends, which is only reached when 6.5's preferable
   * interval already missed.
   */
  @Test
  public void ruleFourReportsNotValidWhenNoIntervalWasSatisfied() throws Exception {
    upstreamRecorded(INTERVAL);

    run();

    assertConditionsWere(LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.YES);
    assertOutcomeWas("Table 6-31 Rule 4",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.NOT_VALID);
    assertNotLinked("a dose that missed every interval does not satisfy the target dose");
  }

  // ============================================= Table 6-31 Rule 5

  /**
   * Table 6-31 Rule 5: the vaccine dose administered is an impacted (conflicting)
   * vaccine dose administered - "Not Satisfied", with an evaluation status of
   * "Not Valid". The marker is the one 6.7 Evaluate Vaccine Conflict appends.
   */
  @Test
  public void ruleFiveReportsNotValidForAnImpactedDose() throws Exception {
    upstreamRecorded(VIRUS_CONFLICT);

    run();

    assertConditionsWere(LogicResult.YES, LogicResult.YES, LogicResult.YES, LogicResult.YES);
    assertOutcomeWas("Table 6-31 Rule 5",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.NOT_VALID);
    assertNotLinked("an impacted dose does not satisfy the target dose");
  }

  /**
   * 6.7 can append {@code "VirusConflict"} twice in one pass - once from Table
   * 6-20's own outcome and once from {@code process()}. Table 6-31's third
   * condition is a substring test, so a doubled marker is still one impacted
   * dose and Rule 5 is still the outcome. This pins that 6.7's duplication cannot
   * change what 6.10 decides.
   */
  @Test
  public void aDuplicatedVirusConflictMarkerIsStillReadOnce() throws Exception {
    upstreamRecorded(VIRUS_CONFLICT);
    upstreamRecorded(VIRUS_CONFLICT);

    run();

    assertConditionsWere(null, null, LogicResult.YES, null);
    assertOutcomeWas("Table 6-31 Rule 5 with a doubled marker",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.NOT_VALID);
  }

  // ============================================= Table 6-31 Rule 6

  /**
   * Table 6-31 Rule 6: the vaccine dose administered was neither a preferable nor
   * an allowable vaccine for the target dose - "Not Satisfied", with an
   * evaluation status of "Not Valid". The marker is the one 6.9 Evaluate for
   * Allowable Vaccine appends, which is only reached when 6.8's preferable
   * vaccine already missed.
   */
  @Test
  public void ruleSixReportsNotValidWhenTheVaccineWasNeitherPreferableNorAllowable()
      throws Exception {
    upstreamRecorded(VACCINE);

    run();

    assertConditionsWere(LogicResult.YES, LogicResult.YES, LogicResult.NO, LogicResult.NO);
    assertOutcomeWas("Table 6-31 Rule 6",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.NOT_VALID);
    assertNotLinked("the wrong vaccine does not satisfy the target dose");
  }

  // ================================== How the accumulated status cause is read

  /**
   * The three status cause conditions are substring tests on one concatenated
   * string, so the order the upstream steps happened to append their markers in
   * cannot change Table 6-31's answers. 6.6 runs before 6.7 which runs before
   * 6.9, but nothing in Table 6-31 depends on that.
   */
  @Test
  public void markerOrderDoesNotChangeTheOutcome() throws Exception {
    upstreamRecorded(VACCINE);
    upstreamRecorded(INTERVAL);

    run();

    assertConditionsWere(LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.NO);
    assertOutcomeWas("markers appended in either order",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.NOT_VALID);
  }

  /**
   * A dose can fail several of Chapter 6's checks at once - every marker the
   * upstream steps can write, on one target dose. Rules 4, 5 and 6 all report the
   * same "Not Satisfied (Not Valid)" outcome, so there is nothing for the rules
   * to disagree about and the answer is that one outcome.
   */
  @Test
  public void everyStatusCauseMarkerAtOnceStillReportsNotValid() throws Exception {
    upstreamRecorded(INTERVAL);
    upstreamRecorded(VIRUS_CONFLICT);
    upstreamRecorded(VACCINE);

    run();

    assertConditionsWere(LogicResult.YES, LogicResult.NO, LogicResult.YES, LogicResult.NO);
    assertOutcomeWas("Rules 4, 5 and 6 together",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.NOT_VALID);
    assertNotLinked("a dose that failed everything does not satisfy the target dose");
  }

  /**
   * {@code EvaluateGender} appends {@code "Gender"} to the same shared status
   * cause string, but Table 6-31 has no gender condition and none of its three
   * status cause markers is a substring of it. A gender failure therefore cannot
   * stop a target dose being satisfied, which is what the specification's four
   * conditions require.
   */
  @Test
  public void theGenderMarkerIsNotOneOfTableSixThirtyOnesConditions() throws Exception {
    upstreamRecorded(GENDER);

    run();

    assertConditionsWere(LogicResult.YES, LogicResult.YES, LogicResult.NO, LogicResult.YES);
    assertOutcomeWas("a gender marker is not one of Table 6-31's conditions",
        TargetDoseStatus.SATISFIED, EvaluationStatus.VALID);
  }

  // ============ Table 6-31's rule ordering when more than one rule could apply

  /**
   * Table 6-31 Rule 2's outcome is "Not Satisfied (Extraneous)", and its three
   * remaining conditions are dashes - an extraneous dose is extraneous whatever
   * the interval, conflict and vaccine checks said. The step package states the
   * ordering explicitly: "Table 6-31 picks the single most relevant reason to
   * report even though several conditions could theoretically overlap (the rule
   * ordering effectively prioritizes age validity first, then interval, then
   * conflict, then vaccine type)."
   *
   * <p>
   * The overlap is reachable, not theoretical, on two counts. In the pipeline,
   * 6.4 Evaluate Age routes unconditionally on to 6.5 whatever it decided
   * ({@code EvaluateAge.process} sets {@code EVALUATE_PREFERABLE_INTERVAL} before
   * evaluating its own table and no outcome overrides it), so an extraneous (too
   * old) dose still runs through 6.5/6.6, 6.7 and 6.8/6.9 and can pick up any of
   * their markers before 6.10 reads them. And in the bundled Supporting Data
   * release (4.65-508, the one {@code DefaultSupportingDataSet.resolve()}
   * returns) the data supports it: of its 506 series doses, 92 carry a non-empty
   * {@code <maxAge/>} and so can make 6.4 answer Extraneous at all; 91 of those
   * 92 also define at least one {@code <allowableVaccine>}, and 59 of them also
   * define an interval or an allowable interval. Every one of the 92 can
   * therefore reach 6.10 with an Extraneous evaluation and one of Rules 4 to 6's
   * markers at the same time.
   *
   * <p>
   * The FITS conformance suite cannot see the difference: its 4,896 cases record
   * 11,126 administered vaccinations and not one expected per-dose evaluation
   * status, only expected forecasts.
   *
   * <p>
   * Here the dose is extraneous <em>and</em> missed every interval, so Rule 2 and
   * Rule 4 both match. Rule 2 is the earlier rule, so "Extraneous" is the
   * evaluation status Table 6-31 calls for.
   */
  @Test
  public void anExtraneousDoseThatAlsoMissedEveryIntervalIsStillExtraneous() throws Exception {
    ageEvaluatedAs(EvaluationStatus.EXTRANEOUS, EvaluationReason.TOO_OLD);
    upstreamRecorded(INTERVAL);

    run();

    assertConditionsWere(LogicResult.EXTRANEOUS, LogicResult.NO, null, null);
    assertOutcomeWas("Table 6-31 Rule 2 comes before Rule 4",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.EXTRANEOUS);
  }

  /**
   * The same overlap between Rule 2 and Rule 5: an extraneous dose that is also
   * an impacted dose. Rule 2 is the earlier rule, so the evaluation status is
   * "Extraneous".
   */
  @Test
  public void anExtraneousDoseThatIsAlsoImpactedIsStillExtraneous() throws Exception {
    ageEvaluatedAs(EvaluationStatus.EXTRANEOUS, EvaluationReason.TOO_OLD);
    upstreamRecorded(VIRUS_CONFLICT);

    run();

    assertConditionsWere(LogicResult.EXTRANEOUS, null, LogicResult.YES, null);
    assertOutcomeWas("Table 6-31 Rule 2 comes before Rule 5",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.EXTRANEOUS);
  }

  /**
   * And the same overlap between Rule 2 and Rule 6: an extraneous dose that was
   * also neither a preferable nor an allowable vaccine. Rule 2 is the earlier
   * rule, so the evaluation status is "Extraneous".
   */
  @Test
  public void anExtraneousDoseThatWasAlsoTheWrongVaccineIsStillExtraneous() throws Exception {
    ageEvaluatedAs(EvaluationStatus.EXTRANEOUS, EvaluationReason.TOO_OLD);
    upstreamRecorded(VACCINE);

    run();

    assertConditionsWere(LogicResult.EXTRANEOUS, null, null, LogicResult.NO);
    assertOutcomeWas("Table 6-31 Rule 2 comes before Rule 6",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.EXTRANEOUS);
  }

  // ================================================================ State changes

  /**
   * {@code process()} clears the status cause back to "" once Table 6-31 has read
   * it, "so it doesn't leak into the next target dose's evaluation" - the shared
   * string is per-target-dose state, and 6.10 is the only step that resets it.
   */
  @Test
  public void processClearsTheStatusCauseForTheNextTargetDose() throws Exception {
    upstreamRecorded(INTERVAL);
    upstreamRecorded(VIRUS_CONFLICT);
    upstreamRecorded(VACCINE);

    run();

    assertEquals("6.10 clears the accumulated status cause after reading it",
        "", targetDose.getStatusCause());
  }

  /**
   * The clear happens after Table 6-31 has been evaluated, not before - otherwise
   * every dose would look like Rule 1. Running the step twice shows both halves:
   * the first pass reads the marker and reports Rule 6, and a second pass that
   * differs only in having had the status cause cleared reports Rule 1.
   *
   * <p>
   * The evaluation status is reset to Valid between the two passes because the
   * first pass's Rule 6 outcome wrote Not Valid into it. In the pipeline that
   * reset is what actually happens - the next target dose round the 4.4 loop
   * arrives with a fresh evaluation from 6.4 - so this isolates the status
   * cause, which is the only thing 6.10 itself carries forward.
   */
  @Test
  public void theStatusCauseIsClearedOnlyAfterTableSixThirtyOneHasReadIt() throws Exception {
    upstreamRecorded(VACCINE);

    run();
    assertOutcomeWas("the first pass still sees the marker",
        TargetDoseStatus.NOT_SATISFIED, EvaluationStatus.NOT_VALID);

    ageEvaluatedAs(EvaluationStatus.VALID, null);
    run();
    assertOutcomeWas("the second pass sees a cleared status cause",
        TargetDoseStatus.SATISFIED, EvaluationStatus.VALID);
  }

  // ================================================================ Next step

  /**
   * {@code transitions.yaml}: 6.10 loops back to 4.4 Evaluate and Forecast All
   * Patient Series "always, regardless of whether the target dose was satisfied,
   * extraneous, or not satisfied". None of Table 6-31's six outcomes calls
   * {@code setNextLogicStepType}.
   */
  @Test
  public void theNextStepIsAlwaysEvaluateAndForecastAllPatientSeries() throws Exception {
    run();
    assertEquals("a satisfied target dose returns to 4.4",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());

    setUp();
    ageEvaluatedAs(EvaluationStatus.EXTRANEOUS, EvaluationReason.TOO_OLD);
    run();
    assertEquals("an extraneous target dose returns to 4.4",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());

    setUp();
    upstreamRecorded(INTERVAL);
    run();
    assertEquals("a not satisfied target dose returns to 4.4",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, step.getNextLogicStepType());
  }

  // ============================================ Constructing the step

  /**
   * Constructing a step must not decide anything - that is what {@code process()}
   * is for. 6.10's state changes are the target dose status, the evaluation
   * status, the satisfying-dose linkage and the cleared status cause, so this
   * pins that the constructor reaches none of them.
   */
  @Test
  public void constructingTheStepDecidesNothing() throws Exception {
    ageEvaluatedAs(EvaluationStatus.EXTRANEOUS, EvaluationReason.TOO_OLD);
    upstreamRecorded(INTERVAL);

    construct();

    assertEquals("constructing the step must not read the status cause away",
        INTERVAL, targetDose.getStatusCause());
    assertEquals("constructing the step must not set an evaluation status",
        EvaluationStatus.EXTRANEOUS, targetDose.getEvaluation().getEvaluationStatus());
    assertNotLinked("constructing the step must not satisfy the target dose");
    assertNull("constructing the step must not decide the next step",
        step.getNextLogicStepType());
    assertNull("and Table 6-31's conditions must not have been evaluated yet",
        conditionResult(0));
  }
}
