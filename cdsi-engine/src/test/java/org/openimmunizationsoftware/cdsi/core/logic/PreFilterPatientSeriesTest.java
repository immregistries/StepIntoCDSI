package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

/**
 * Section 8.1 "Pre-filter Patient Series" (Logic Specification for ACIP
 * Recommendations v4.6, page 87, Table 8-2 "Pre-Filter Patient Series Business
 * Rules") as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/08-01-pre-filter-patient-series/index.md}.
 *
 * <p>
 * 8.1 has no figure and no decision table of its own. Table 8-2's two business
 * rules - SELECTB-24 (which relevant patient series are <i>candidate</i>
 * scorable patient series) and SELECTSCORE-2 (which candidates become
 * <i>scorable</i> patient series) - are the whole of the section, so the tests
 * below are organised one per rule clause: SELECTB-24's two bullets, then each
 * of SELECTSCORE-2's four bullets and their sub-clauses, then the section's
 * Purpose sentence about series-group scope, then the documented State Changes
 * and Next Steps.
 *
 * <p>
 * The step is driven through its public {@code process()} with a hand-built
 * {@code DataModel}. {@link PreFilterPatientSeries} reads exactly three things -
 * {@code dataModel.getPatientSeriesStepper().getList()}, each patient series'
 * {@code patientSeriesStatus} / {@code targetDoseList}, and each tracked
 * {@code AntigenSeries}' {@code seriesType} plus its
 * {@code SelectPatientSeries} (series priority, default series flag) - so no
 * Supporting Data release, no loader and no upstream step is needed. The step
 * it returns ({@code IdentifyOnePrioritizedPatientSeries}) only builds an inert
 * {@code LogicTable} in its constructor; its {@code process()} is never called
 * here, which keeps this an 8.1 test rather than an 8.2 test.
 *
 * <p>
 * <b>Series priority ordering.</b> SELECTSCORE-2's Risk bullet speaks of a
 * priority being "the same as or greater than" another. The bundled 4.65-508
 * release only ever uses the letters {@code A}, {@code B} and {@code C} (370,
 * 51 and 15 occurrences respectively), and {@code PreFilterPatientSeries}
 * itself treats the lexicographically smallest letter as the highest priority
 * (its {@code highestRiskPriority.compareTo(seriesPriority) > 0} test keeps the
 * smaller string). The tests below use that same ordering - {@code A} is the
 * highest priority - so that no test turns on independently resolving the
 * letter order, which the specification text never states.
 *
 * <p>
 * <b>Deliberately not covered.</b> (a) SELECTSCORE-2's Standard bullet says
 * "The <i>earliest</i> vaccine dose administered with an evaluation status of
 * 'Valid'" must predate the maximum age to start date; the implementation
 * checks no date at all, so a test distinguishing "earliest" from "any" would
 * pass or fail identically to
 * {@link #selectscoreTwoAValidDoseMustHaveBeenAdministeredBeforeTheMaximumAgeToStartDate()}
 * and is not written separately. (b) SELECTSCORE-2's Evaluation Only bullet is
 * the one bullet that does <i>not</i> require "It is a candidate scorable
 * patient series"; because {@code patientSeriesStatus} holds a single value, a
 * series cannot be both {@code CONTRAINDICATED} (excluded from candidates) and
 * {@code COMPLETE} (required by that bullet), so the omission is not observable
 * and no test asserts it. (c) The implementation additionally requires a target
 * dose to be {@code SATISFIED} before its {@code VALID} evaluation counts,
 * which Table 8-2 does not say; constructing a not-satisfied target dose that
 * nonetheless carries a {@code VALID} evaluation would be an unreachable state,
 * so this is recorded in the unit's notes rather than asserted here.
 */
public class PreFilterPatientSeriesTest {

  /** Series group names as they appear in the bundled Supporting Data. */
  private static final String STANDARD_GROUP = "Standard";
  private static final String INCREASED_RISK_GROUP = "Increased Risk";
  private static final String PEDIATRIC_TRAVEL_GROUP = "Increased Risk - Pediatric Travel";

  private static final String HIGHEST_PRIORITY = "A";
  private static final String LOWER_PRIORITY = "B";

  private DataModel dataModel;
  private Antigen hepB;

  @Before
  public void setUp() {
    dataModel = new DataModel();
    hepB = dataModel.getOrCreateAntigen("HepB");
    Patient patient = new Patient();
    patient.setDateOfBirth(date("01/01/2010"));
    dataModel.setPatient(patient);
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the minimal shape 8.1 actually reads.
  // ---------------------------------------------------------------------

  /**
   * Adds one relevant patient series to the patient series stepper, which is
   * where 5.1 {@code SelectRelevantPatientSeries} puts them and the only place
   * 8.1 looks.
   */
  private PatientSeries relevantPatientSeries(String seriesName, Antigen targetDisease, SeriesType seriesType,
      String seriesGroupName, String seriesPriority, PatientSeriesStatus status) {
    SelectPatientSeries selectPatientSeries = new SelectPatientSeries();
    selectPatientSeries.setSeriesGroupName(seriesGroupName);
    selectPatientSeries.setSeriesGroup(seriesGroupName);
    selectPatientSeries.setSeriesPriority(seriesPriority);
    selectPatientSeries.setDefaultSeries(YesNo.NO);

    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setSeriesType(seriesType);
    antigenSeries.setTargetDisease(targetDisease);
    antigenSeries.setSelectPatientSeries(selectPatientSeries);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setPatientSeriesStatus(status);
    patientSeries.setTargetDoseList(new ArrayList<TargetDose>());
    dataModel.getPatientSeriesStepper().add(patientSeries);
    return patientSeries;
  }

  /** A Standard series of the default "Standard" series group for HepB. */
  private PatientSeries standardSeries(String seriesName, PatientSeriesStatus status) {
    return relevantPatientSeries(seriesName, hepB, SeriesType.STANDARD, STANDARD_GROUP, HIGHEST_PRIORITY, status);
  }

  /** A Risk series of the "Increased Risk" series group for HepB. */
  private PatientSeries riskSeries(String seriesName, String seriesPriority, PatientSeriesStatus status) {
    return relevantPatientSeries(seriesName, hepB, SeriesType.RISK, INCREASED_RISK_GROUP, seriesPriority, status);
  }

  private static void markAsDefaultSeries(PatientSeries patientSeries) {
    patientSeries.getTrackedAntigenSeries().getSelectPatientSeries().setDefaultSeries(YesNo.YES);
  }

  private static void setMaximumAgeToStart(PatientSeries patientSeries, String timePeriod) {
    patientSeries.getTrackedAntigenSeries().getSelectPatientSeries().setMaxAgeToStart(new TimePeriod(timePeriod));
  }

  /**
   * Gives the patient series a satisfied target dose whose evaluation is
   * {@code VALID} - the state SELECTSCORE-2's Standard bullet calls "includes a
   * target dose evaluating at least one vaccine dose administered with an
   * evaluation status of 'Valid'".
   */
  private static TargetDose validDose(PatientSeries patientSeries, String dateAdministered) {
    VaccineDoseAdministered vaccineDoseAdministered = new VaccineDoseAdministered();
    vaccineDoseAdministered.setDateAdministered(date(dateAdministered));

    Evaluation evaluation = new Evaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.VALID);
    evaluation.setVaccineDoseAdministered(vaccineDoseAdministered);

    TargetDose targetDose = new TargetDose();
    targetDose.setTargetDoseStatus(TargetDoseStatus.SATISFIED);
    targetDose.setEvaluation(evaluation);
    patientSeries.getTargetDoseList().add(targetDose);
    return targetDose;
  }

  /** A target dose that was satisfied by a dose that did not evaluate Valid. */
  private static TargetDose notValidDose(PatientSeries patientSeries) {
    Evaluation evaluation = new Evaluation();
    evaluation.setEvaluationStatus(EvaluationStatus.NOT_VALID);

    TargetDose targetDose = new TargetDose();
    targetDose.setTargetDoseStatus(TargetDoseStatus.SATISFIED);
    targetDose.setEvaluation(evaluation);
    patientSeries.getTargetDoseList().add(targetDose);
    return targetDose;
  }

  private LogicStep process() throws Exception {
    return new PreFilterPatientSeries(dataModel).process();
  }

  private List<String> scorableSeriesNames() throws Exception {
    process();
    return seriesNamesOf(dataModel.getScorablePatientSeriesList());
  }

  private static List<String> seriesNamesOf(List<PatientSeries> patientSeriesList) {
    List<String> names = new ArrayList<String>();
    for (PatientSeries patientSeries : patientSeriesList) {
      names.add(patientSeries.getTrackedAntigenSeries().getSeriesName());
    }
    return names;
  }

  private static Set<String> seriesGroupsOf(List<PatientSeries> patientSeriesList) {
    Set<String> groups = new HashSet<String>();
    for (PatientSeries patientSeries : patientSeriesList) {
      groups.add(patientSeries.getTrackedAntigenSeries().getSelectPatientSeries().getSeriesGroupName());
    }
    return groups;
  }

  private static Date date(String monthDayYear) {
    try {
      return new SimpleDateFormat("MM/dd/yyyy").parse(monthDayYear);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  // ---------------------------------------------------------------------
  // SELECTB-24 - candidate scorable patient series
  // ---------------------------------------------------------------------

  /**
   * SELECTB-24, first bullet: a relevant patient series is a candidate scorable
   * patient series if "the patient series forecast does not have a patient
   * series status of 'Contraindicated.'" A Not Complete Standard series with a
   * valid dose therefore reaches SELECTSCORE-2 and is scored.
   */
  @Test
  public void selectbTwentyFourASeriesThatIsNotContraindicatedIsACandidateScorablePatientSeries() throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");

    assertEquals(Arrays.asList("HepB standard"), scorableSeriesNames());
  }

  /**
   * SELECTB-24, second bullet: a Contraindicated series is a candidate only if
   * "each relevant patient series ... made for the same series group has a
   * patient series status of 'Contraindicated'". With a non-contraindicated
   * sibling in the same series group, the contraindicated one is not a
   * candidate and so cannot become scorable, however well it would otherwise
   * score.
   */
  @Test
  public void selectbTwentyFourAContraindicatedSeriesIsNotACandidateWhenASiblingInItsGroupIsNot() throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");
    validDose(standardSeries("HepB contraindicated", PatientSeriesStatus.CONTRAINDICATED), "06/01/2010");

    assertEquals("A contraindicated series drops out while a sibling is still in play",
        Arrays.asList("HepB standard"), scorableSeriesNames());
  }

  /**
   * SELECTB-24, second bullet, the case it exists for: when every relevant
   * patient series in the series group is Contraindicated, all of them stay
   * candidates, so the group can still report something rather than coming up
   * empty.
   */
  @Test
  public void selectbTwentyFourContraindicatedSeriesStayCandidatesWhenEverySeriesInTheGroupIsContraindicated()
      throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.CONTRAINDICATED), "06/01/2010");
    validDose(standardSeries("HepB alternate", PatientSeriesStatus.CONTRAINDICATED), "06/01/2010");

    assertEquals(Arrays.asList("HepB standard", "HepB alternate"), scorableSeriesNames());
  }

  /**
   * SELECTB-24's all-contraindicated escape hatch is scoped to <i>one</i>
   * series group - "each relevant patient series ... made for <b>the same
   * series group</b>". A group whose every series is Contraindicated must
   * therefore keep its series as candidates regardless of what a different
   * series group of the same antigen looks like.
   */
  @Test
  public void selectbTwentyFourTheAllContraindicatedFallbackIsDecidedPerSeriesGroup() throws Exception {
    PatientSeries riskGroupOnlySeries = relevantPatientSeries("HepB risk", hepB, SeriesType.STANDARD,
        INCREASED_RISK_GROUP, HIGHEST_PRIORITY, PatientSeriesStatus.CONTRAINDICATED);
    validDose(riskGroupOnlySeries, "06/01/2010");
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");

    assertTrue("Every series of the Increased Risk group is Contraindicated, so its series stay candidates "
        + "- the Standard group's healthy series belongs to a different group and must not suppress it",
        scorableSeriesNames().contains("HepB risk"));
  }

  /**
   * SELECTB-24's first bullet is a negative test - "does not have a patient
   * series status of 'Contraindicated'". A patient series that carries no
   * status at all does not have that status, so it is a candidate. The
   * implementation instead requires a non-null status, which is a stricter
   * reading than the rule text.
   */
  @Test
  public void selectbTwentyFourASeriesWithNoPatientSeriesStatusIsStillNotContraindicated() throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");
    validDose(standardSeries("HepB unstatused", null), "06/01/2010");

    assertTrue("A null status is not the status 'Contraindicated', so SELECTB-24's first bullet holds",
        scorableSeriesNames().contains("HepB unstatused"));
  }

  // ---------------------------------------------------------------------
  // SELECTSCORE-2, bullet 1 - Risk series at the group's highest priority
  // ---------------------------------------------------------------------

  /**
   * SELECTSCORE-2, first bullet: a Risk series whose series priority "is the
   * same as or greater than the series priority of any relevant patient series
   * that tracks an antigen series that belongs to the same series group" is
   * scorable. The higher-priority of two Risk series in one group is kept.
   */
  @Test
  public void selectscoreTwoARiskSeriesAtItsGroupsHighestPriorityIsScorable() throws Exception {
    riskSeries("HepB risk A", HIGHEST_PRIORITY, PatientSeriesStatus.NOT_COMPLETE);
    riskSeries("HepB risk B", LOWER_PRIORITY, PatientSeriesStatus.NOT_COMPLETE);

    assertTrue(scorableSeriesNames().contains("HepB risk A"));
  }

  /**
   * The other half of the first bullet, and of the section's Purpose: "If a
   * Series Group contains relevant patient series of different priorities, only
   * the set of highest priority patient series should be considered." The
   * lower-priority Risk series is removed from consideration.
   */
  @Test
  public void selectscoreTwoARiskSeriesBelowItsGroupsHighestPriorityIsNotScorable() throws Exception {
    riskSeries("HepB risk A", HIGHEST_PRIORITY, PatientSeriesStatus.NOT_COMPLETE);
    riskSeries("HepB risk B", LOWER_PRIORITY, PatientSeriesStatus.NOT_COMPLETE);

    assertFalse("Only the set of highest priority patient series is considered",
        scorableSeriesNames().contains("HepB risk B"));
  }

  /**
   * SELECTSCORE-2's first bullet compares against "the series priority of
   * <b>any</b> relevant patient series that tracks an antigen series that
   * belongs to the same series group" - not only against the other Risk series.
   * A Risk series at priority B in a group that also holds a priority-A series
   * of another type therefore fails the comparison.
   *
   * <p>
   * The implementation computes its {@code highestRiskPriority} from the Risk
   * series alone, so the Risk series survives. Whether the specification really
   * means every series type or only the Risk ones is arguably ambiguous - the
   * rule text says "any relevant patient series", without qualification - so a
   * Role B session should classify this before changing anything.
   */
  @Test
  public void selectscoreTwoRiskPriorityIsComparedAgainstEverySeriesInTheGroupNotOnlyTheRiskOnes() throws Exception {
    relevantPatientSeries("HepB risk group standard", hepB, SeriesType.STANDARD, INCREASED_RISK_GROUP,
        HIGHEST_PRIORITY, PatientSeriesStatus.NOT_COMPLETE);
    riskSeries("HepB risk B", LOWER_PRIORITY, PatientSeriesStatus.NOT_COMPLETE);

    assertFalse("Priority B is not the same as or greater than the priority-A series sharing its series group",
        scorableSeriesNames().contains("HepB risk B"));
  }

  /**
   * The priority comparison is bounded by one series group ("belongs to the
   * same series group as the relevant patient series"). A Risk series that is
   * the highest-priority series of <i>its own</i> group must stay scorable even
   * though a different series group of the same antigen holds a series at a
   * higher priority. Fifteen of the thirty antigens in the bundled 4.65-508
   * release define more than one series group, so this is the normal shape of
   * the data, not a corner case.
   */
  @Test
  public void selectscoreTwoRiskPrioritiesAreComparedWithinOneSeriesGroupNotAcrossGroups() throws Exception {
    riskSeries("HepB risk A", HIGHEST_PRIORITY, PatientSeriesStatus.NOT_COMPLETE);
    relevantPatientSeries("HepB travel risk B", hepB, SeriesType.RISK, PEDIATRIC_TRAVEL_GROUP, LOWER_PRIORITY,
        PatientSeriesStatus.NOT_COMPLETE);

    assertTrue("Priority B is the highest priority within the Pediatric Travel group, which is the group that counts",
        scorableSeriesNames().contains("HepB travel risk B"));
  }

  /**
   * SELECTSCORE-2's first bullet also requires "It is a candidate scorable
   * patient series", i.e. it must have passed SELECTB-24 first. A
   * Contraindicated Risk series at the highest priority is still not scorable
   * while a non-contraindicated series remains in its group.
   */
  @Test
  public void selectscoreTwoARiskSeriesMustAlsoBeACandidateScorablePatientSeries() throws Exception {
    riskSeries("HepB risk contraindicated", HIGHEST_PRIORITY, PatientSeriesStatus.CONTRAINDICATED);
    riskSeries("HepB risk open", HIGHEST_PRIORITY, PatientSeriesStatus.NOT_COMPLETE);

    assertEquals(Arrays.asList("HepB risk open"), scorableSeriesNames());
  }

  // ---------------------------------------------------------------------
  // SELECTSCORE-2, bullet 2 - Standard series with a valid dose
  // ---------------------------------------------------------------------

  /**
   * SELECTSCORE-2, second bullet: a Standard series that "includes a target
   * dose evaluating at least one vaccine dose administered with an evaluation
   * status of 'Valid'" and is a candidate is scorable.
   */
  @Test
  public void selectscoreTwoAStandardSeriesWithAValidDoseIsScorable() throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");

    assertEquals(Arrays.asList("HepB standard"), scorableSeriesNames());
  }

  /**
   * The same bullet from the other side: a Standard series whose only target
   * dose was satisfied by a dose that did not evaluate Valid does not meet the
   * "at least one ... 'Valid'" clause, so this bullet does not make it
   * scorable. (Its group has a valid dose elsewhere, so the third bullet does
   * not apply either.)
   */
  @Test
  public void selectscoreTwoAStandardSeriesWhoseOnlyDoseIsNotValidIsNotScorableUnderThisBullet() throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");
    notValidDose(standardSeries("HepB alternate", PatientSeriesStatus.NOT_COMPLETE));

    assertEquals(Arrays.asList("HepB standard"), scorableSeriesNames());
  }

  /**
   * SELECTSCORE-2's second bullet has a third clause the other bullets do not:
   * "The earliest vaccine dose administered with an evaluation status of
   * 'Valid' associated with the relevant patient series has a date administered
   * <b>before the maximum age to start date</b>." Here the patient was born
   * 01/01/2010, the series' maximum age to start is 5 years (so 01/01/2015),
   * and the only valid dose was given 06/01/2020 - well past it. The bullet
   * does not hold and the series is not scorable through it.
   *
   * <p>
   * {@code PreFilterPatientSeries} reads no date and no
   * {@code maxAgeToStart} at all, so it scores the series anyway. This is the
   * gap 08-01's own Review Findings already flag as an unconfirmed observation.
   */
  @Test
  public void selectscoreTwoAValidDoseMustHaveBeenAdministeredBeforeTheMaximumAgeToStartDate() throws Exception {
    PatientSeries agedOutStart = standardSeries("HepB late start", PatientSeriesStatus.NOT_COMPLETE);
    setMaximumAgeToStart(agedOutStart, "5 years");
    validDose(agedOutStart, "06/01/2020");
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");

    assertFalse("The only valid dose was administered after the maximum age to start date",
        scorableSeriesNames().contains("HepB late start"));
  }

  // ---------------------------------------------------------------------
  // SELECTSCORE-2, bullet 3 - Standard series in a group with no valid doses
  // ---------------------------------------------------------------------

  /**
   * SELECTSCORE-2, third bullet: a Standard candidate is scorable when "the
   * number of valid doses is 0 for each relevant patient series in the series
   * group" and "there is no default patient series for the series group". A
   * patient with no history at all in a group that declares no default series
   * must therefore still produce scorable series - otherwise the group has
   * nothing to score and no best patient series can be chosen for it.
   *
   * <p>
   * The implementation has no equivalent of this bullet: its {@code STANDARD}
   * branch requires at least one valid dose, and its end-of-method fallback
   * only adds a series whose default-series flag is Yes - the opposite of this
   * bullet's condition - so the scorable list comes back empty.
   */
  @Test
  public void selectscoreTwoAStandardSeriesIsScorableWhenItsGroupHasZeroValidDosesAndNoDefaultSeries()
      throws Exception {
    standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE);
    standardSeries("HepB alternate", PatientSeriesStatus.NOT_COMPLETE);

    assertEquals("No valid doses anywhere in the group and no default series, so both Standard series are scorable",
        Arrays.asList("HepB standard", "HepB alternate"), scorableSeriesNames());
  }

  /**
   * The third bullet's "there is no default patient series for the series
   * group" clause: when the group <i>does</i> declare a default series, this
   * bullet does not apply, so a non-default Standard series with no valid doses
   * is not made scorable by it.
   */
  @Test
  public void selectscoreTwoTheZeroValidDoseBulletDoesNotApplyWhenTheGroupHasADefaultSeries() throws Exception {
    markAsDefaultSeries(standardSeries("HepB default", PatientSeriesStatus.NOT_COMPLETE));
    standardSeries("HepB alternate", PatientSeriesStatus.NOT_COMPLETE);

    assertFalse("A default series exists for the group, so the third bullet cannot make this series scorable",
        scorableSeriesNames().contains("HepB alternate"));
  }

  /**
   * The third bullet's "the number of valid doses is 0 for <b>each</b> relevant
   * patient series in the series group" clause is a property of the whole
   * group, not of the one series being judged: a sibling's valid dose disables
   * the bullet for every series in the group.
   */
  @Test
  public void selectscoreTwoTheZeroValidDoseBulletRequiresZeroValidDosesAcrossEverySeriesInTheGroup()
      throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");
    standardSeries("HepB doseless", PatientSeriesStatus.NOT_COMPLETE);

    assertFalse("A sibling in the group has a valid dose, so the group's valid dose count is not 0",
        scorableSeriesNames().contains("HepB doseless"));
  }

  // ---------------------------------------------------------------------
  // SELECTSCORE-2, bullet 4 - Evaluation Only series that is complete
  // ---------------------------------------------------------------------

  /**
   * SELECTSCORE-2, fourth bullet: an Evaluation Only series is scorable when
   * "the relevant patient series is a complete patient series".
   */
  @Test
  public void selectscoreTwoACompleteEvaluationOnlySeriesIsScorable() throws Exception {
    relevantPatientSeries("HepB evaluation only", hepB, SeriesType.EVALUATION_ONLY, STANDARD_GROUP, HIGHEST_PRIORITY,
        PatientSeriesStatus.COMPLETE);

    assertEquals(Arrays.asList("HepB evaluation only"), scorableSeriesNames());
  }

  /**
   * The fourth bullet from the other side: an Evaluation Only series that is
   * not complete meets no bullet of SELECTSCORE-2 and is not scorable. (The
   * group also holds a Standard series with a valid dose, so the third bullet
   * and the implementation's default-series fallback are both out of the way.)
   */
  @Test
  public void selectscoreTwoAnIncompleteEvaluationOnlySeriesIsNotScorable() throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");
    relevantPatientSeries("HepB evaluation only", hepB, SeriesType.EVALUATION_ONLY, STANDARD_GROUP, HIGHEST_PRIORITY,
        PatientSeriesStatus.NOT_COMPLETE);

    assertEquals(Arrays.asList("HepB standard"), scorableSeriesNames());
  }

  /**
   * Every bullet of SELECTSCORE-2 opens by naming a series type, so a series
   * whose tracked antigen series declares none satisfies no bullet. This pins
   * the implementation's {@code seriesType != null} guard against the rule
   * text rather than leaving it untested.
   */
  @Test
  public void selectscoreTwoASeriesWithNoSeriesTypeMatchesNoBulletAndIsNotScorable() throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");
    validDose(relevantPatientSeries("HepB untyped", hepB, null, STANDARD_GROUP, HIGHEST_PRIORITY,
        PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");

    assertEquals(Arrays.asList("HepB standard"), scorableSeriesNames());
  }

  /**
   * Characterization of the implementation's end-of-method fallback, which has
   * no counterpart in Table 8-2: when nothing else qualified and no valid dose
   * exists anywhere, {@code PreFilterPatientSeries} adds the group's default
   * Standard series.
   *
   * <p>
   * Recorded as behaviour rather than asserted as conformance because
   * SELECTSCORE-2 leaves a genuine gap here: its third bullet only makes a
   * Standard series scorable when there is <i>no</i> default series for the
   * group, and no other bullet mentions the default series at all, so on a
   * literal reading a group that declares a default series and has no valid
   * doses yields no scorable series whatsoever. That is very unlikely to be the
   * intent, and resolving it is a specification question, not something to
   * decide inside a test - see this unit's {@code status.yaml} notes.
   */
  @Test
  public void theDefaultSeriesFallbackAddsTheGroupsDefaultStandardSeriesWhenNothingElseQualified() throws Exception {
    markAsDefaultSeries(standardSeries("HepB default", PatientSeriesStatus.NOT_COMPLETE));

    assertEquals(Arrays.asList("HepB default"), scorableSeriesNames());
  }

  // ---------------------------------------------------------------------
  // Purpose - the scope 8.1 examines
  // ---------------------------------------------------------------------

  /**
   * Purpose: "Pre-filter patient series examines each of the patient series
   * <b>for a given Series Group</b> ... only the set of highest priority
   * patient series should be considered when determining the best patient
   * series <b>for the Series Group</b>." Chapter 8's overview says the same -
   * "Process steps 8.1 through 8.7 are repeated for each series group". So one
   * run of 8.1 must produce a scorable list drawn from a single series group.
   *
   * <p>
   * {@code PreFilterPatientSeries} reads the whole patient series stepper and
   * never looks at {@code SelectPatientSeries.getSeriesGroup()} - in fact
   * nothing in {@code cdsi-engine}'s logic package reads it - so one run mixes
   * every series group of the antigen into one list, and 8.2 through 8.7 then
   * score series from different groups against each other.
   */
  @Test
  public void theStepExaminesThePatientSeriesOfOnlyOneSeriesGroup() throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");
    riskSeries("HepB risk", HIGHEST_PRIORITY, PatientSeriesStatus.NOT_COMPLETE);

    process();

    assertEquals("One run of 8.1 pre-filters one series group, so its output cannot span two",
        1, seriesGroupsOf(dataModel.getScorablePatientSeriesList()).size());
  }

  /**
   * Chapter 8 runs once per antigen - 4.5 {@code SelectBestPatientSeries}
   * selects one antigen, narrows the relevant patient series to that antigen's
   * in {@code selectedPatientSeriesList}, and only then enters 8.1. A run of
   * 8.1 must therefore not consider another antigen's patient series.
   *
   * <p>
   * {@code PreFilterPatientSeries} reads
   * {@code getPatientSeriesStepper().getList()} - 5.1's unfiltered,
   * all-antigen list - rather than the per-antigen list 4.5 just built, so
   * Measles' series is pre-filtered alongside HepB's on the HepB pass.
   */
  @Test
  public void theStepExaminesOnlyThePatientSeriesOfTheAntigenBeingProcessed() throws Exception {
    Antigen measles = dataModel.getOrCreateAntigen("Measles");
    PatientSeries hepBSeries = standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE);
    validDose(hepBSeries, "06/01/2010");
    validDose(relevantPatientSeries("Measles standard", measles, SeriesType.STANDARD, STANDARD_GROUP,
        HIGHEST_PRIORITY, PatientSeriesStatus.NOT_COMPLETE), "06/01/2011");

    // What 4.5 has set up by the time it hands control to 8.1.
    dataModel.setAntigen(hepB);
    dataModel.setSelectedPatientSeriesList(new ArrayList<PatientSeries>(Arrays.asList(hepBSeries)));

    assertEquals("8.1 runs inside 4.5's per-antigen loop and must only see the current antigen's series",
        Arrays.asList("HepB standard"), scorableSeriesNames());
  }

  // ---------------------------------------------------------------------
  // State Changes and Next Steps
  // ---------------------------------------------------------------------

  /**
   * State Changes: 8.1 "builds {@code dataModel}'s
   * {@code scorablePatientSeriesList}". Next Steps: the transition to 8.2 is
   * unconditional (see {@code transitions.yaml}).
   */
  @Test
  public void theStepBuildsTheScorablePatientSeriesListAndTransitionsToEightTwo() throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");

    LogicStep next = process();

    assertNotNull("8.2 reads this list without creating it", dataModel.getScorablePatientSeriesList());
    assertEquals(LogicStepType.IDENTIFY_ONE_PRIORITIZED_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * 8.1 is re-entered once per series group (and, in this implementation, once
   * per antigen), and nothing between runs clears the scorable list, so the
   * step must replace it rather than append to it - otherwise one group's
   * pre-filtering would be scored together with the previous group's.
   */
  @Test
  public void theScorablePatientSeriesListIsRebuiltOnEachRunRatherThanAccumulated() throws Exception {
    validDose(standardSeries("HepB standard", PatientSeriesStatus.NOT_COMPLETE), "06/01/2010");

    process();
    List<PatientSeries> firstRun = dataModel.getScorablePatientSeriesList();
    process();

    assertNotSame("Each run starts a fresh scorable list", firstRun, dataModel.getScorablePatientSeriesList());
    assertEquals("A second run must not double up the same series",
        Arrays.asList("HepB standard"), seriesNamesOf(dataModel.getScorablePatientSeriesList()));
  }
}
