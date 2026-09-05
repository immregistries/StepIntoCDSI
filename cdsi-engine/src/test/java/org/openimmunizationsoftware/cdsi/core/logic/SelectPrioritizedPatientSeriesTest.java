package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

/**
 * Section 8.7 "Select Prioritized Patient Series" (Logic Specification for ACIP
 * Recommendations v4.6, page 91; Table 8-13 business rules SELECTBEST-1 and
 * SELECTBEST-2) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/08-07-select-prioritized-patient-series/index.md}.
 *
 * <p>
 * 8.7 is the consumer end of the 8.4/8.5/8.6 scoring family. Those three steps
 * award points onto one running integer per patient series,
 * {@code PatientSeries.getScorePatientSeries()}; 8.7 reads that integer to pick
 * the winner and puts it on {@code dataModel.getPrioritizedPatientSeriesList()}
 * for 8.8. It has no {@code LogicTable} of its own - Table 8-13 is a plain
 * two-column business-rule table, not a decision grid:
 *
 * <pre>
 * SELECTBEST-1  The scorable patient series score is the sum of all points awarded to it.
 * SELECTBEST-2  The prioritized patient series is the one with the highest score,
 *               or - if tied - the one with the best-ranked series preference.
 * </pre>
 *
 * <p>
 * <b>Isolation.</b> All of 8.7's logic lives in one private method,
 * {@code selectPrioritizedPatientSeries()}, so the selection tests invoke that
 * reflectively (the precedent {@link NoValidDosesCompletableTest} and
 * {@link NoValidDosesTest} set) and read the public
 * {@code getPrioritizedPatientSeries()}. Only the State Changes and Next Steps
 * tests at the bottom drive the public {@code process()}. Either way the fixture
 * is hand-built: no Supporting Data release, no loader and no upstream step is
 * involved. Note that {@code SelectPrioritizedPatientSeries} captures
 * {@code dataModel.getSelectedPatientSeriesList()} in a field initializer, i.e.
 * at construction time, so every helper below builds the fixture first and
 * constructs the step last.
 *
 * <p>
 * <b>On "best-ranked series preference".</b> The specification does not say
 * which direction ranks better, and the Supporting Data's
 * {@code <seriesPreference>} values are bare integers (1 through 10 in the
 * bundled 4.65-508 release, 72 of its 131 populated values being "1"). The
 * implementation reads
 * lower as better, which is the conventional reading of a preference rank and
 * the only one these tests adopt; where a test turns on the direction it says
 * so.
 *
 * <p>
 * <b>On the score never being reset.</b> Nothing in {@code cdsi-engine} ever
 * resets {@code scorePatientSeries} between selections - see the 2026-09-02
 * entry in {@code cdsi-reference/step-tests/cross-cutting-notes.md}. 8.7 is the
 * only reader of the field in the whole engine (verified by grep:
 * {@code getScorePatientSeries()} appears exactly twice, both in this class),
 * which is why that entry named 8.7's Role A pass as the place to settle whether
 * the accumulation actually matters. The three SELECTBEST-1 tests below are what
 * settles it; the reasoning is recorded in that entry and in this unit's
 * {@code status.yaml} notes.
 */
public class SelectPrioritizedPatientSeriesTest {

  /** Series group names as they appear in the bundled Supporting Data. */
  private static final String STANDARD_GROUP = "Standard";
  private static final String INCREASED_RISK_GROUP = "Increased Risk";

  /** The domain model's own default - a Java string literal, therefore interned. */
  private static final String NO_SERIES_PREFERENCE_DECLARED = "";

  private DataModel dataModel;
  private Antigen hepB;
  private Antigen measles;
  /** The list 8.7 actually reads - what 4.5 leaves behind for one antigen pass. */
  private List<PatientSeries> selectedPatientSeriesList;

  @Before
  public void setUp() {
    dataModel = new DataModel();
    dataModel.setPatient(new Patient());

    hepB = dataModel.getOrCreateAntigen("HepB");
    measles = dataModel.getOrCreateAntigen("Measles");
    // The state 4.5 leaves behind at the top of one antigen pass.
    dataModel.setAntigen(hepB);
    dataModel.setSelectedPatientSeriesList(new ArrayList<PatientSeries>());
    dataModel.setScorablePatientSeriesList(new ArrayList<PatientSeries>());
    selectedPatientSeriesList = dataModel.getSelectedPatientSeriesList();
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the minimal shape 8.7 actually reads.
  // ---------------------------------------------------------------------

  /**
   * A patient series of a named antigen and series group, with a declared
   * series preference, registered both on the all-antigen patient series
   * stepper (as 5.1 leaves it) and on the current antigen's selected list (as
   * 4.5 leaves it), which is the list 8.7 reads.
   */
  private PatientSeries series(String seriesName, Antigen targetDisease, String seriesGroupName,
      String seriesPreference) {
    PatientSeries patientSeries = unselectedSeries(seriesName, targetDisease, seriesGroupName, seriesPreference);
    selectedPatientSeriesList.add(patientSeries);
    return patientSeries;
  }

  /**
   * The same, but only on the stepper - a patient series that belongs to some
   * other antigen's pass, so 4.5 did not put it on this pass's selected list.
   */
  private PatientSeries unselectedSeries(String seriesName, Antigen targetDisease, String seriesGroupName,
      String seriesPreference) {
    SelectPatientSeries selectPatientSeries = new SelectPatientSeries();
    selectPatientSeries.setSeriesGroup(seriesGroupName);
    selectPatientSeries.setSeriesGroupName(seriesGroupName);
    selectPatientSeries.setSeriesPreference(seriesPreference);

    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setSeriesType(SeriesType.STANDARD);
    antigenSeries.setTargetDisease(targetDisease);
    antigenSeries.setSelectPatientSeries(selectPatientSeries);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
    patientSeries.setTargetDoseList(new ArrayList<TargetDose>());
    dataModel.getPatientSeriesStepper().getList().add(patientSeries);
    return patientSeries;
  }

  /** A HepB Standard-group patient series with no declared series preference. */
  private PatientSeries series(String seriesName) {
    return series(seriesName, hepB, STANDARD_GROUP, NO_SERIES_PREFERENCE_DECLARED);
  }

  /** A HepB Standard-group patient series with a declared series preference. */
  private PatientSeries seriesPreferred(String seriesName, String seriesPreference) {
    return series(seriesName, hepB, STANDARD_GROUP, seriesPreference);
  }

  /**
   * Awards {@code points} to a patient series the way 8.4/8.5/8.6 do - one
   * {@code incPatientScoreSeries()} or {@code descPatientScoreSeries()} call per
   * point, never a set - so the fixture reaches the score by the same route the
   * production steps do.
   */
  private static PatientSeries award(PatientSeries patientSeries, int points) {
    for (int i = 0; i < points; i++) {
      patientSeries.incPatientScoreSeries();
    }
    for (int i = 0; i > points; i--) {
      patientSeries.descPatientScoreSeries();
    }
    return patientSeries;
  }

  /** A target dose of the series that some administered dose satisfied. */
  private static void validDose(PatientSeries patientSeries) {
    SeriesDose seriesDose = new SeriesDose();
    seriesDose.setAntigenSeries(patientSeries.getTrackedAntigenSeries());
    seriesDose.setDoseNumber(String.valueOf(patientSeries.getTargetDoseList().size() + 1));

    TargetDose targetDose = new TargetDose(seriesDose);
    targetDose.setTargetDoseStatus(TargetDoseStatus.SATISFIED);
    patientSeries.getTargetDoseList().add(targetDose);
  }

  // ---------------------------------------------------------------------
  // Driving the step.
  // ---------------------------------------------------------------------

  /** Runs 8.7's whole selection and reports the patient series it picked. */
  private PatientSeries select() throws Exception {
    SelectPrioritizedPatientSeries step = new SelectPrioritizedPatientSeries(dataModel);
    Method method = SelectPrioritizedPatientSeries.class.getDeclaredMethod("selectPrioritizedPatientSeries");
    method.setAccessible(true);
    try {
      method.invoke(step);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception) {
        throw (Exception) e.getCause();
      }
      throw e;
    }
    return step.getPrioritizedPatientSeries();
  }

  /** Runs the whole step and reports the step control is handed to. */
  private LogicStepType selectWholeStep() throws Exception {
    return new SelectPrioritizedPatientSeries(dataModel).process().getLogicStepType();
  }

  private static String nameOf(PatientSeries patientSeries) {
    return patientSeries == null ? "(none)" : patientSeries.getTrackedAntigenSeries().getSeriesName();
  }

  private List<String> prioritizedNames() {
    List<String> names = new ArrayList<String>();
    for (PatientSeries patientSeries : dataModel.getPrioritizedPatientSeriesList()) {
      names.add(nameOf(patientSeries));
    }
    return names;
  }

  // ---------------------------------------------------------------------
  // SELECTBEST-1 - "The scorable patient series score is the sum of all points
  // awarded to it."
  // ---------------------------------------------------------------------

  /**
   * SELECTBEST-1's straightforward half: the number 8.7 compares is the running
   * sum of the increments and decrements the scoring step awarded, not the count
   * of positive awards. A series awarded +1, +1, -1 sums to 1 and beats one
   * awarded +1, -1, -1, which sums to -1, even though both were awarded a
   * positive point twice and once respectively.
   */
  @Test
  public void selectbestOneTheSeriesWithTheLargerSumOfAwardedPointsIsSelected() throws Exception {
    PatientSeries netPositive = series("HepB net +1");
    netPositive.incPatientScoreSeries();
    netPositive.incPatientScoreSeries();
    netPositive.descPatientScoreSeries();

    PatientSeries netNegative = series("HepB net -1");
    netNegative.incPatientScoreSeries();
    netNegative.descPatientScoreSeries();
    netNegative.descPatientScoreSeries();

    assertEquals("the fixture's own arithmetic", 1, netPositive.getScorePatientSeries());
    assertEquals("the fixture's own arithmetic", -1, netNegative.getScorePatientSeries());
    assertSame("SELECTBEST-1 sums the points awarded; +1+1-1 beats +1-1-1", netPositive, select());
  }

  /**
   * SELECTBEST-1's other half, and the reason the 2026-09-02 cross-cutting entry
   * asked for this unit: the score is "the sum of all points awarded to it" -
   * awarded to it <i>in this selection</i>. Nothing in {@code cdsi-engine} ever
   * resets {@code scorePatientSeries}, and 8.7 compares the raw accumulated
   * integers with {@code ==} and {@code >} with no baseline of any kind, so
   * points a candidate happens to be carrying in from an earlier selection count
   * exactly as much as the ones this selection awarded.
   *
   * <p>
   * Here the series this selection actually favours is awarded +1 and the other
   * is awarded nothing at all, but the other walked in carrying 5 points, so it
   * wins. Whether that can happen in a real assessment is a separate question
   * from whether 8.7 would be fooled by it if it did - this test settles only the
   * second, and the first is answered by
   * {@link #selectbestOneAnAccumulationScaledEquallyAcrossCandidatesDoesNotChangeTheWinner()}
   * below and in this unit's {@code status.yaml} notes.
   */
  @Test
  public void selectbestOneTheScoreIsThePointsAwardedInThisSelectionNotOnesCarriedIn() throws Exception {
    PatientSeries carryingPointsFromAnEarlierSelection = series("HepB carrying 5");
    award(carryingPointsFromAnEarlierSelection, 5);
    // ... and this selection awards it nothing at all.

    PatientSeries awardedThePointThisSelection = series("HepB awarded +1 now");
    award(awardedThePointThisSelection, 1);

    assertSame("SELECTBEST-1's sum is over the points awarded in this selection, so the series awarded +1"
        + " here is the prioritized one; the other was awarded nothing this time and is only ahead on"
        + " points nothing ever reset", awardedThePointThisSelection, select());
  }

  /**
   * The companion to the test above, and the half that settles materiality:
   * an accumulation that is <i>equal across every candidate</i> cannot change
   * which series 8.7 picks, because multiplying every candidate's per-selection
   * delta by the same positive count preserves both the maximum and the tie set.
   *
   * <p>
   * The first assertion scales a three-way ranking by 7 and gets the same winner;
   * the second scales a genuine two-way tie by 7 and shows it is still a tie, so
   * SELECTBEST-2's series preference tie-break still runs. That is exactly the
   * situation the engine is in today: 8.5 and 8.6 read the never-re-scoped
   * all-antigen patient series stepper, so every antigen pass presents them
   * identical input and awards every series in it an identical delta. This test
   * pins the property the non-materiality argument rests on, so that a later
   * change which breaks it - candidates of one selection being scored a different
   * number of times as each other - is visible here rather than silently.
   */
  @Test
  public void selectbestOneAnAccumulationScaledEquallyAcrossCandidatesDoesNotChangeTheWinner() throws Exception {
    int passes = 7;

    PatientSeries best = award(seriesPreferred("HepB +3 per pass", "2"), 3 * passes);
    award(seriesPreferred("HepB +1 per pass", "3"), 1 * passes);
    award(seriesPreferred("HepB -1 per pass", "1"), -1 * passes);

    assertSame("scaling every candidate's per-selection delta by the same positive count leaves the highest"
        + " scorer highest", best, select());

    setUp();
    PatientSeries tiedBetterRanked = award(seriesPreferred("HepB tied, preference 1", "1"), 2 * passes);
    award(seriesPreferred("HepB tied, preference 4", "4"), 2 * passes);

    assertSame("a tie scaled equally is still a tie, so SELECTBEST-2's series preference still decides it",
        tiedBetterRanked, select());
  }

  /**
   * SELECTBEST-1 makes the score the only input to the selection.
   * {@code SelectPrioritizedPatientSeries} carries a public
   * {@code numberOfValidDoses(PatientSeries)} helper - a copy of the one 8.4/8.5
   * use for their own scoring rows - which nothing in the class or anywhere else
   * in the engine calls. This pins that it stays dead: a series with fewer valid
   * doses but the higher score is the prioritized one.
   */
  @Test
  public void selectbestOneTheSelectionReadsTheScoreNotTheNumberOfValidDoses() throws Exception {
    PatientSeries fewDosesHighScore = award(series("HepB 1 valid dose, score 3"), 3);
    validDose(fewDosesHighScore);

    PatientSeries manyDosesLowScore = award(series("HepB 4 valid doses, score 1"), 1);
    validDose(manyDosesLowScore);
    validDose(manyDosesLowScore);
    validDose(manyDosesLowScore);
    validDose(manyDosesLowScore);

    assertSame("SELECTBEST-1/2 select on the score alone; the valid dose count was already spent by 8.4-8.6",
        fewDosesHighScore, select());
  }

  // ---------------------------------------------------------------------
  // SELECTBEST-2 - "The prioritized patient series is the one with the highest
  // score, or - if tied - the one with the best-ranked series preference."
  // ---------------------------------------------------------------------

  /** SELECTBEST-2's first clause with no tie in sight: highest score wins. */
  @Test
  public void selectbestTwoThePrioritizedPatientSeriesIsTheOneWithTheHighestScore() throws Exception {
    award(series("HepB score -2"), -2);
    PatientSeries highest = award(series("HepB score 4"), 4);
    award(series("HepB score 1"), 1);

    assertSame("SELECTBEST-2: the prioritized patient series is the one with the highest score", highest,
        select());
  }

  /**
   * The same three scores in all three rotations. The specification names no
   * ordering over the patient series, so which one happens to sit first in the
   * list must not decide the outcome - and 8.7's loop seeds its running best
   * from {@code patientSeriesList.get(0)}, which is exactly the shape that goes
   * wrong in 8.5's and 8.6's "can start/finish earliest" rows.
   */
  @Test
  public void selectbestTwoTheHighestScoringSeriesIsSelectedWhereverItSitsInTheList() throws Exception {
    for (int winnerPosition = 0; winnerPosition < 3; winnerPosition++) {
      setUp();
      PatientSeries winner = null;
      for (int position = 0; position < 3; position++) {
        PatientSeries patientSeries = award(series("HepB position " + position), position == winnerPosition ? 4 : 1);
        if (position == winnerPosition) {
          winner = patientSeries;
        }
      }
      assertSame("the highest scorer is selected with the winner at position " + winnerPosition, winner, select());
    }
  }

  /** A series group of one: the only candidate is the prioritized patient series. */
  @Test
  public void selectbestTwoASoleCandidateIsThePrioritizedPatientSeries() throws Exception {
    PatientSeries only = award(series("HepB only candidate"), -3);

    assertSame("with one candidate there is nothing to compare it against, and it is still the prioritized"
        + " patient series even on a negative score", only, select());
  }

  /**
   * SELECTBEST-2's second clause: two series tied on score are separated by the
   * series preference their antigen series declares in Supporting Data, lower
   * ranking better.
   */
  @Test
  public void selectbestTwoATieOnScoreIsBrokenByTheBestRankedSeriesPreference() throws Exception {
    award(seriesPreferred("HepB preference 2", "2"), 3);
    PatientSeries bestRanked = award(seriesPreferred("HepB preference 1", "1"), 3);

    assertSame("SELECTBEST-2 breaks a tie on the best-ranked series preference", bestRanked, select());
  }

  /**
   * The tie-break must not depend on which of the tied series the list happens
   * to present first, for the same reason as
   * {@link #selectbestTwoTheHighestScoringSeriesIsSelectedWhereverItSitsInTheList()}.
   */
  @Test
  public void selectbestTwoTheSeriesPreferenceTieBreakIsIndependentOfListOrder() throws Exception {
    PatientSeries bestRankedFirst = award(seriesPreferred("HepB preference 1", "1"), 3);
    award(seriesPreferred("HepB preference 5", "5"), 3);
    assertSame("best-ranked series preference first in the list", bestRankedFirst, select());

    setUp();
    award(seriesPreferred("HepB preference 5", "5"), 3);
    PatientSeries bestRankedLast = award(seriesPreferred("HepB preference 1", "1"), 3);
    assertSame("best-ranked series preference last in the list", bestRankedLast, select());
  }

  /**
   * SELECTBEST-2 applies the tie-break only among the series that are actually
   * tied for the highest score. A lower-scoring series with the best series
   * preference of all does not win - the score clause comes first.
   */
  @Test
  public void selectbestTwoTheTieBreakAppliesOnlyAmongTheHighestScoringSeries() throws Exception {
    award(seriesPreferred("HepB score 4, preference 9", "9"), 4);
    award(seriesPreferred("HepB score 0, preference 1", "1"), 0);
    PatientSeries winner = award(seriesPreferred("HepB score 4, preference 4", "4"), 4);

    assertSame("the best-ranked series preference in the whole group belongs to a series that is not tied for"
        + " the highest score, so SELECTBEST-2's first clause settles it before the second is reached",
        winner, select());
  }

  /** A three-way tie is won by the best-ranked series preference of all three. */
  @Test
  public void selectbestTwoAThreeWayTieIsWonByTheBestRankedPreferenceOfAllOfThem() throws Exception {
    award(seriesPreferred("HepB preference 3", "3"), 2);
    PatientSeries bestRanked = award(seriesPreferred("HepB preference 1", "1"), 2);
    award(seriesPreferred("HepB preference 2", "2"), 2);

    assertSame("the tie-break ranges over every series tied for the highest score", bestRanked, select());
  }

  /**
   * A tie among series that declare no series preference at all. All 143 series
   * in the bundled 4.65-508 release carry a {@code <seriesPreference>} element,
   * but 12 of them are self-closing and so declare no preference; the other 131
   * carry a bare integer between 1 and 10, 72 of them "1".
   *
   * <p>
   * The specification offers no second tie-break, so all this can require is
   * that a prioritized patient series is still selected - SELECTBEST-2 says the
   * prioritized patient series <i>is</i> one of them, and 8.8 has nothing to
   * evaluate otherwise. It passes today because
   * {@code SelectPatientSeries.seriesPreference} is initialised to the string
   * literal {@code ""}, which is interned, so 8.7's reference comparison
   * {@code currentSeriesPreference != ""} correctly answers "no preference
   * declared" and skips the tie-break. See the test below for the other half of
   * that.
   */
  @Test
  public void selectbestTwoATieAmongSeriesThatDeclareNoPreferenceStillSelectsOne() throws Exception {
    award(series("HepB no preference A"), 2);
    award(series("HepB no preference B"), 2);

    assertNotNull("SELECTBEST-2 still names a prioritized patient series when the tie-break has nothing to"
        + " rank on", select());
  }

  /**
   * The same tie, with the empty series preference arriving as a string that is
   * <i>not</i> the interned literal - which is what
   * {@code cdsi-reference/logic-spec/versions/4.6/steps/08-07-select-prioritized-patient-series/index.md}'s
   * Review Findings left open ("this may work correctly in practice depending on
   * where {@code getSeriesPreference()}'s value originates; this pass did not
   * trace that far").
   *
   * <p>
   * Traced: {@code DataModelLoader} sets the field from
   * {@code DomUtils.getInternalValue()}, which returns its own {@code ""}
   * literal when the element has no child node, and {@code String.trim()}
   * returns the receiver unchanged when there is nothing to trim - so all 12
   * self-closing {@code <seriesPreference/>} elements in the bundled release
   * still yield the interned literal and are handled correctly. A
   * whitespace-only element would not: {@code trim()} would return a fresh
   * empty {@code String}, {@code != ""} would answer "a preference is declared",
   * and {@code Integer.parseInt("")} would throw. The bundled release contains
   * no such element, so this is a latent defect rather than a live one, but it
   * is a defect of the guard rather than of the data: the guard tests reference
   * identity where it means to test emptiness.
   */
  @Test
  public void selectbestTwoAnEmptySeriesPreferenceIsNoPreferenceHoweverItWasBuilt() throws Exception {
    // Not the interned literal - the same value any runtime-built empty string has.
    String emptyButNotInterned = new String("  ".trim());
    award(series("HepB empty preference", hepB, STANDARD_GROUP, emptyButNotInterned), 2);
    award(seriesPreferred("HepB preference 1", "1"), 2);

    try {
      assertNotNull("an empty series preference means no preference is declared, whichever String instance"
          + " carries it, so SELECTBEST-2 still names a prioritized patient series", select());
    } catch (NumberFormatException e) {
      fail("SELECTBEST-2 must still select a prioritized patient series when a series declares an empty"
          + " series preference, but the tie-break guard compares the value to \"\" by reference rather"
          + " than by equality, so a non-interned empty string reaches Integer.parseInt: " + e);
    }
  }

  // ---------------------------------------------------------------------
  // Purpose - "the prioritized patient series for the series group", applied to
  // "the scored patient series".
  // ---------------------------------------------------------------------

  /**
   * 8.7's antigen scope, which - unlike four of the eight Chapter 8 steps - is
   * correct. The step reads {@code dataModel.getSelectedPatientSeriesList()},
   * which 4.5 rebuilds per antigen pass, so a Measles series sitting on the
   * all-antigen patient series stepper with a far higher score cannot take the
   * HepB pass's selection.
   */
  @Test
  public void theSelectionIsMadeOverThePatientSeriesOfTheAntigenBeingProcessed() throws Exception {
    PatientSeries hepBWinner = award(series("HepB score 1"), 1);
    award(series("HepB score 0"), 0);
    // On the stepper, as 5.1 leaves it, but not on this antigen pass's list.
    award(unselectedSeries("Measles score 9", measles, STANDARD_GROUP, "1"), 9);

    assertSame("8.7 selects among the patient series of the antigen 4.5 is currently processing", hepBWinner,
        select());
  }

  /**
   * 8.7's pipeline stage, which is not correct. Table 8-13's rules are phrased
   * over the <i>scorable</i> patient series and 8.7's own Purpose says the rules
   * are "applied to the scored patient series" - the list 8.1 produces. 8.7
   * reads 4.5's pre-8.1 {@code selectedPatientSeriesList} instead, so a series
   * 8.1 deliberately dropped from consideration is still a candidate to win the
   * selection outright.
   *
   * <p>
   * Here two Risk series of one group: 8.1's SELECTSCORE-2 keeps only the
   * highest-priority one, so the priority-B series is not on
   * {@code scorablePatientSeriesList} at all and was never scored by 8.4/8.5/8.6
   * either - yet it is the series 8.7 names as prioritized. This is the same
   * cross-cutting entry 8.4 confirmed from the other direction, and it is
   * sharper here than anywhere else in the chapter: in 8.4 a stray series
   * distorted a comparison between real candidates, in 8.7 it becomes the
   * answer.
   */
  @Test
  public void theSelectionIsMadeOverTheScorablePatientSeriesEightOneProduced() throws Exception {
    PatientSeries keptByPreFilter = award(series("HepB Risk priority A"), 1);
    keptByPreFilter.getTrackedAntigenSeries().setSeriesType(SeriesType.RISK);
    keptByPreFilter.getTrackedAntigenSeries().getSelectPatientSeries().setSeriesPriority("A");

    PatientSeries droppedByPreFilter = award(series("HepB Risk priority B"), 5);
    droppedByPreFilter.getTrackedAntigenSeries().setSeriesType(SeriesType.RISK);
    droppedByPreFilter.getTrackedAntigenSeries().getSelectPatientSeries().setSeriesPriority("B");

    // What 8.1 leaves behind: only the highest-priority Risk series of the group.
    dataModel.getScorablePatientSeriesList().add(keptByPreFilter);

    assertSame("8.7 selects among the scorable patient series 8.1 produced; the priority-B Risk series 8.1"
        + " dropped is not a candidate and was never scored", keptByPreFilter, select());
  }

  /**
   * 8.7's Purpose names its output scope explicitly - the rules "result in the
   * prioritized patient series <i>for the series group</i>" - and Chapter 8's
   * overview repeats it ("Process steps 8.1 through 8.7 are repeated for each
   * series group to identify one prioritized patient series per series group").
   * Two series groups of one antigen must therefore leave two prioritized
   * patient series behind for 8.8, one each.
   *
   * <p>
   * Nothing in the engine loops over series groups, so 8.7 runs once per antigen
   * and produces one prioritized patient series for the whole antigen. See the
   * 2026-09-05 "Chapter 8 has no series group" cross-cutting entry.
   */
  @Test
  public void theStepProducesOnePrioritizedPatientSeriesPerSeriesGroup() throws Exception {
    award(series("HepB Standard winner", hepB, STANDARD_GROUP, "1"), 3);
    award(series("HepB Standard runner-up", hepB, STANDARD_GROUP, "2"), 1);
    award(series("HepB Increased Risk winner", hepB, INCREASED_RISK_GROUP, "1"), 5);
    award(series("HepB Increased Risk runner-up", hepB, INCREASED_RISK_GROUP, "2"), 4);

    selectWholeStep();

    assertEquals("one prioritized patient series per series group, not per antigen; got " + prioritizedNames(),
        2, dataModel.getPrioritizedPatientSeriesList().size());
  }

  /**
   * The consequence of the same gap on the comparison rather than on the output
   * count: SELECTBEST-2's "highest score" is a comparison within one series
   * group, so the Standard group's own winner must be the Standard group's
   * prioritized patient series even though an Increased Risk series of the same
   * antigen scored higher. Scores are not comparable across groups in the first
   * place - 8.1's SELECTSCORE-2 and 8.4/8.5/8.6's rows are all phrased within a
   * series group.
   */
  @Test
  public void theSelectionComparesScoresWithinOneSeriesGroupNotAcrossGroups() throws Exception {
    PatientSeries standardWinner = award(series("HepB Standard winner", hepB, STANDARD_GROUP, "1"), 1);
    award(series("HepB Standard runner-up", hepB, STANDARD_GROUP, "2"), 0);
    award(series("HepB Increased Risk", hepB, INCREASED_RISK_GROUP, "1"), 5);

    selectWholeStep();

    assertTrue("the Standard series group's prioritized patient series is its own winner, not whichever"
        + " series of another group of the same antigen happens to score highest; got " + prioritizedNames(),
        dataModel.getPrioritizedPatientSeriesList().contains(standardWinner));
  }

  // ---------------------------------------------------------------------
  // State Changes - "Adds the selected series to
  // dataModel.getPrioritizedPatientSeriesList()".
  // ---------------------------------------------------------------------

  /** The step's only state change: the winner is recorded for 8.8 to evaluate. */
  @Test
  public void theSelectedSeriesIsAddedToThePrioritizedPatientSeriesList() throws Exception {
    award(series("HepB runner-up"), 1);
    PatientSeries winner = award(series("HepB winner"), 4);

    selectWholeStep();

    assertEquals("exactly the selected series is handed on to 8.8", Arrays.asList(nameOf(winner)),
        prioritizedNames());
  }

  /**
   * With nothing to select from there is no prioritized patient series to
   * record. 8.2's Table 8-3 can route a group here having found no scorable
   * patient series at all, so this is reachable, not defensive.
   */
  @Test
  public void noPrioritizedPatientSeriesIsRecordedWhenThereAreNoCandidates() throws Exception {
    selectWholeStep();

    assertTrue("nothing is selected and nothing is recorded when the selection has no candidates",
        dataModel.getPrioritizedPatientSeriesList().isEmpty());
  }

  // ---------------------------------------------------------------------
  // Next Steps - unconditional to 8.8.
  // ---------------------------------------------------------------------

  /** 8.7 always hands control to 8.8, whatever it selected. */
  @Test
  public void theStepAlwaysHandsOffToDetermineBestPatientSeries() throws Exception {
    award(series("HepB winner"), 2);

    assertEquals("8.7 is unconditional to 8.8", LogicStepType.DETERMINE_BEST_PATIENT_SERIES, selectWholeStep());
  }

  /** ... including when it selected nothing at all. */
  @Test
  public void theStepHandsOffToDetermineBestPatientSeriesEvenWithNoCandidates() throws Exception {
    assertEquals("8.7 is unconditional to 8.8 even with an empty candidate list",
        LogicStepType.DETERMINE_BEST_PATIENT_SERIES, selectWholeStep());
  }
}
