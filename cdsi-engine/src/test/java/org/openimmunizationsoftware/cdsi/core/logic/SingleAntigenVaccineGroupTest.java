package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogEvent;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * Section 9.2 "Single Antigen Vaccine Group" (Logic Specification for ACIP
 * Recommendations v4.6, page 95; Table 9-3 "Single Antigen Vaccine Group
 * Business Rules") as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/09-02-single-antigen-vaccine-group/index.md}.
 *
 * <p>
 * <b>What the section says.</b> Table 9-3 names exactly two rules.
 * <b>SINGLEANTVG-1</b>: "The vaccine group status of a vaccine group forecast
 * made for a single antigen vaccine group must be the patient series status of
 * the patient series forecast contained in the vaccine group forecast."
 * <b>SINGLEANTVG-2</b>: "The earliest date of a vaccine group forecast made for
 * a single antigen vaccine group must be the earliest date of all the patient
 * series forecasts contained in the vaccine group forecast." The step is
 * entered from 9.1 when VACCINEGROUP-1 has classified the current vaccine group
 * as classifying exactly one antigen, and it returns unconditionally to the
 * chapter driver (section "9", {@code IdentifyAndEvaluateVaccineGroup}).
 *
 * <p>
 * <b>What the class does, and therefore what else is tested here.</b>
 * {@code SingleAntigenVaccineGroup} implements those two and then copies every
 * remaining forecast field from the one matching patient series forecast,
 * labelling the copies {@code SINGLEANTVG-3} through {@code SINGLEANTVG-10} in
 * code comments. Those higher labels are <i>not</i> in the specification's own
 * Table 9-3 (see this unit's Review Findings), but they are the code's own
 * statement of intent for one field each, and each of them corresponds to a
 * Table 9-2 {@code FORECASTVG-*} rule that 9.1 owns and delegates here, so they
 * are tested one method per label. Two of the ten are comments with no
 * statement under them - {@code SINGLEANTVG-9} (antigens needed, the
 * single-antigen twin of {@code MultipleAntigenVaccineGroup.MULTIANTVG_8()})
 * and {@code SINGLEANTVG-10} (recommended series dose vaccines, Table 9-2's
 * FORECASTVG-9) - and both of those tests are red for different reasons: the
 * first has a field to write to and does not write it, the second has no field
 * at all. See the 2026-09-07 entry in
 * {@code cdsi-reference/step-tests/cross-cutting-notes.md}.
 *
 * <p>
 * <b>Isolation.</b> {@code process()} is a single public method with no
 * decision table to drive (Table 9-3 is prose, and the {@code LogicTable} the
 * constructor registers is an empty 0x0 placeholder), so every test builds the
 * minimal {@code DataModel} the step actually reads - a current
 * {@code VaccineGroup} with its one classified {@code Antigen}, and a best
 * patient series list holding {@code PatientSeries} objects each carrying a
 * {@code Forecast} - and calls {@code process()} directly. Nothing is loaded
 * from a Supporting Data release: 9.2 reads no Supporting Data of its own, only
 * Chapter 8's output and 4.6's current vaccine group. {@code process()} ends in
 * {@code next()}, which constructs {@code IdentifyAndEvaluateVaccineGroup};
 * that class's constructor is trivial, so the handoff can be asserted directly
 * without running the chapter driver.
 */
public class SingleAntigenVaccineGroupTest {

  private static final String HEPB = "HepB";
  private static final String HEPATITIS_B = "Hepatitis B";
  private static final String MEASLES = "Measles";

  private static final SimpleDateFormat SDF = new SimpleDateFormat("MM/dd/yyyy");

  private DataModel dataModel;

  @Before
  public void setUp() {
    dataModel = new DataModel();
    dataModel.setBestPatientSeriesList(new ArrayList<PatientSeries>());
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the state 4.6 and Chapter 8 leave behind for 9.2.
  // ---------------------------------------------------------------------

  private static Date date(String mmddyyyy) {
    try {
      return SDF.parse(mmddyyyy);
    } catch (ParseException pe) {
      throw new IllegalArgumentException(pe);
    }
  }

  /**
   * The current vaccine group 9.1 has just classified as a single antigen
   * vaccine group - VACCINEGROUP-1, "classifies exactly one antigen".
   */
  private VaccineGroup singleAntigenVaccineGroup(String vaccineGroupName, String antigenName) {
    VaccineGroup vaccineGroup = dataModel.getOrCreateVaccineGroup(vaccineGroupName);
    vaccineGroup.getAntigenList().add(dataModel.getOrCreateAntigen(antigenName));
    dataModel.setVaccineGroup(vaccineGroup);
    return vaccineGroup;
  }

  /**
   * One entry of Chapter 8's output: a best patient series for {@code
   * antigenName}, carrying the patient series forecast 7.5 generated for it.
   */
  private PatientSeries bestPatientSeries(String seriesName, String antigenName, PatientSeriesStatus status,
      Date earliestDate) {
    Antigen antigen = dataModel.getOrCreateAntigen(antigenName);
    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setTargetDisease(antigen);

    Forecast forecast = new Forecast();
    forecast.setAntigen(antigen);
    forecast.setEarliestDate(earliestDate);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setPatientSeriesStatus(status);
    patientSeries.setForecast(forecast);
    patientSeries.setTargetDoseList(new ArrayList<org.openimmunizationsoftware.cdsi.core.domain.TargetDose>());

    dataModel.getBestPatientSeriesList().add(patientSeries);
    return patientSeries;
  }

  /**
   * The single-antigen fixture nearly every test starts from: a HepB vaccine
   * group classifying Hepatitis B, and one best patient series for it.
   */
  private PatientSeries hepBFixture(PatientSeriesStatus status, Date earliestDate) {
    singleAntigenVaccineGroup(HEPB, HEPATITIS_B);
    return bestPatientSeries("HepB standard", HEPATITIS_B, status, earliestDate);
  }

  // ---------------------------------------------------------------------
  // Driving the step.
  // ---------------------------------------------------------------------

  private SingleAntigenVaccineGroup runStep() throws Exception {
    SingleAntigenVaccineGroup step = new SingleAntigenVaccineGroup(dataModel);
    step.process();
    return step;
  }

  /** The one vaccine group forecast 9.2 is expected to have produced. */
  private VaccineGroupForecast theVaccineGroupForecast() throws Exception {
    runStep();
    List<VaccineGroupForecast> produced = dataModel.getVaccineGroupForecastList();
    assertFalse("9.2 must produce a vaccine group forecast for the single antigen vaccine group",
        produced.isEmpty());
    return produced.get(0);
  }

  private static boolean hasAlertContaining(LogicStep step, String fragment) {
    for (LogEvent event : step.getLogEventList()) {
      if (event.isAlert() && event.getMessage().contains(fragment)) {
        return true;
      }
    }
    return false;
  }

  /**
   * The "can the rule even be expressed?" probe used by 6.2, 7.1, 7.5, 7.6, 8.8
   * and 9.1 - the first no-argument accessor of {@code type} whose name matches,
   * or null.
   */
  private static Method accessorMatching(Class<?> type, String namePattern) {
    for (Method method : type.getMethods()) {
      if (method.getParameterTypes().length == 0 && method.getName().matches(namePattern)) {
        return method;
      }
    }
    return null;
  }

  private static List<String> accessorNames(Class<?> type) {
    List<String> names = new ArrayList<String>();
    for (Method method : type.getMethods()) {
      if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
        names.add(method.getName());
      }
    }
    return names;
  }

  // ---------------------------------------------------------------------
  // SINGLEANTVG-1 - "The vaccine group status of a vaccine group forecast made
  // for a single antigen vaccine group must be the patient series status of the
  // patient series forecast contained in the vaccine group forecast."
  // ---------------------------------------------------------------------

  /**
   * SINGLEANTVG-1 directly, for every patient series status the domain defines.
   * A single antigen vaccine group has exactly one contained forecast, so its
   * vaccine group status is that forecast's patient series status relabelled -
   * no aggregation, no precedence between statuses.
   */
  @Test
  public void singleantvgOneTheVaccineGroupStatusIsThePatientSeriesStatusOfTheContainedForecast() throws Exception {
    for (PatientSeriesStatus patientSeriesStatus : PatientSeriesStatus.values()) {
      setUp();
      hepBFixture(patientSeriesStatus, date("01/01/2024"));

      assertEquals("SINGLEANTVG-1: a patient series status of " + patientSeriesStatus
          + " makes the vaccine group status " + patientSeriesStatus,
          VaccineGroupStatus.valueOf(patientSeriesStatus.name()),
          theVaccineGroupForecast().getVaccineGroupStatus());
    }
  }

  /**
   * SINGLEANTVG-1's other half: the vaccine group forecast also records the
   * patient series status it was derived from, unchanged. The two fields exist
   * separately on {@code VaccineGroupForecast} - one typed
   * {@code VaccineGroupStatus}, one {@code PatientSeriesStatus} - and for a
   * single antigen vaccine group they must describe the same one contained
   * forecast.
   */
  @Test
  public void singleantvgOneTheContainedPatientSeriesStatusIsCarriedOnTheVaccineGroupForecastToo() throws Exception {
    hepBFixture(PatientSeriesStatus.CONTRAINDICATED, date("01/01/2024"));

    assertEquals("SINGLEANTVG-1: the contained patient series' own status is carried across unchanged",
        PatientSeriesStatus.CONTRAINDICATED, theVaccineGroupForecast().getPatientSeriesStatus());
  }

  /**
   * State Changes: "If the patient series status is unexpectedly {@code null},
   * logs an {@code ALERT.MISSING} and defaults to {@code NOT_COMPLETE} rather
   * than failing." Not a rule of Table 9-3 - the specification says nothing
   * about a missing patient series status - but it is this step's documented
   * defensive behaviour, and it is the safe direction (a group whose status is
   * unknown is reported as still needing doses rather than as complete).
   */
  @Test
  public void aMissingPatientSeriesStatusIsAlertedAndDefaultsTheVaccineGroupStatusToNotComplete() throws Exception {
    hepBFixture(null, date("01/01/2024"));

    SingleAntigenVaccineGroup step = new SingleAntigenVaccineGroup(dataModel);
    step.process();

    assertFalse("the step must still produce a vaccine group forecast rather than failing",
        dataModel.getVaccineGroupForecastList().isEmpty());
    assertEquals("State Changes: a null patient series status defaults to NOT_COMPLETE",
        VaccineGroupStatus.NOT_COMPLETE, dataModel.getVaccineGroupForecastList().get(0).getVaccineGroupStatus());
    assertTrue("State Changes: the missing status is alerted as ALERT.MISSING; log was " + step.getLogList(),
        hasAlertContaining(step, "ALERT.MISSING"));
  }

  /**
   * The {@code NOT_COMPLETE} default above is applied by
   * {@code VaccineGroupForecast.setVaccineGroupStatus(PatientSeriesStatus)},
   * which substitutes it silently; the sibling {@code setPatientSeriesStatus}
   * is a plain field assignment and takes the null. So one vaccine group
   * forecast ends up describing its own single contained forecast two ways at
   * once. SINGLEANTVG-1 makes both fields copies of the same value, so whatever
   * the fallback is it has to be the same on both.
   */
  @Test
  public void aMissingPatientSeriesStatusFallsBackConsistentlyAcrossBothStatusFields() throws Exception {
    hepBFixture(null, date("01/01/2024"));
    VaccineGroupForecast vaccineGroupForecast = theVaccineGroupForecast();

    assertEquals("Precondition: the vaccine group status falls back to NOT_COMPLETE",
        VaccineGroupStatus.NOT_COMPLETE, vaccineGroupForecast.getVaccineGroupStatus());
    assertEquals("SINGLEANTVG-1 makes both status fields copies of the one contained forecast's status, so the"
        + " documented NOT_COMPLETE fallback must reach both of them",
        PatientSeriesStatus.NOT_COMPLETE, vaccineGroupForecast.getPatientSeriesStatus());
  }

  // ---------------------------------------------------------------------
  // SINGLEANTVG-2 - "The earliest date of a vaccine group forecast made for a
  // single antigen vaccine group must be the earliest date of all the patient
  // series forecasts contained in the vaccine group forecast."
  // ---------------------------------------------------------------------

  /** SINGLEANTVG-2 in the ordinary case: one contained forecast, one date. */
  @Test
  public void singleantvgTwoTheEarliestDateIsTheContainedPatientSeriesForecastsEarliestDate() throws Exception {
    hepBFixture(PatientSeriesStatus.NOT_COMPLETE, date("03/15/2024"));

    assertEquals("SINGLEANTVG-2: the vaccine group forecast's earliest date is the contained forecast's",
        date("03/15/2024"), theVaccineGroupForecast().getEarliestDate());
  }

  /**
   * SINGLEANTVG-2 says "the earliest date of <i>all</i> the patient series
   * forecasts contained in the vaccine group forecast" - plural, and a minimum,
   * not a copy. A single antigen vaccine group can genuinely contain more than
   * one: 8.8 selects a best patient series per series group, and an antigen may
   * define several (HepB alone has Standard and Increased Risk groups in the
   * bundled release), so two best patient series for the one antigen both feed
   * the one group's forecast.
   *
   * <p>
   * The earlier of the two is put first in the list deliberately, so that a
   * "last one wins" implementation is distinguishable from a real minimum.
   */
  @Test
  public void singleantvgTwoTheEarliestDateIsTheEarliestOfAllContainedPatientSeriesForecasts() throws Exception {
    singleAntigenVaccineGroup(HEPB, HEPATITIS_B);
    bestPatientSeries("HepB standard", HEPATITIS_B, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    bestPatientSeries("HepB increased risk", HEPATITIS_B, PatientSeriesStatus.NOT_COMPLETE, date("06/01/2024"));

    assertEquals("SINGLEANTVG-2: the earliest date of all the contained patient series forecasts is 01/01/2024",
        date("01/01/2024"), theVaccineGroupForecast().getEarliestDate());
  }

  // ---------------------------------------------------------------------
  // SINGLEANTVG-3 through SINGLEANTVG-8 - the remaining field copies. Not named
  // in Table 9-3; each is the single-antigen case of a Table 9-2 FORECASTVG-*
  // rule (9.1's unit), and each is a labelled comment in this class.
  // ---------------------------------------------------------------------

  /** SINGLEANTVG-3 / FORECASTVG-2: adjusted recommended date. */
  @Test
  public void singleantvgThreeTheAdjustedRecommendedDateIsTheContainedForecasts() throws Exception {
    PatientSeries patientSeries = hepBFixture(PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    patientSeries.getForecast().setAdjustedRecommendedDate(date("02/01/2024"));

    assertEquals("SINGLEANTVG-3: the adjusted recommended date is the contained forecast's",
        date("02/01/2024"), theVaccineGroupForecast().getAdjustedRecommendedDate());
  }

  /** SINGLEANTVG-4 / FORECASTVG-3: adjusted past due date. */
  @Test
  public void singleantvgFourTheAdjustedPastDueDateIsTheContainedForecasts() throws Exception {
    PatientSeries patientSeries = hepBFixture(PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    patientSeries.getForecast().setAdjustedPastDueDate(date("03/01/2024"));

    assertEquals("SINGLEANTVG-4: the adjusted past due date is the contained forecast's",
        date("03/01/2024"), theVaccineGroupForecast().getAdjustedPastDueDate());
  }

  /** SINGLEANTVG-5 / FORECASTVG-4: latest date. */
  @Test
  public void singleantvgFiveTheLatestDateIsTheContainedForecasts() throws Exception {
    PatientSeries patientSeries = hepBFixture(PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    patientSeries.getForecast().setLatestDate(date("12/31/2024"));

    assertEquals("SINGLEANTVG-5: the latest date is the contained forecast's",
        date("12/31/2024"), theVaccineGroupForecast().getLatestDate());
  }

  /** SINGLEANTVG-6 / FORECASTVG-5: unadjusted recommended date. */
  @Test
  public void singleantvgSixTheUnadjustedRecommendedDateIsTheContainedForecasts() throws Exception {
    PatientSeries patientSeries = hepBFixture(PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    patientSeries.getForecast().setUnadjustedRecommendedDate(date("02/15/2024"));

    assertEquals("SINGLEANTVG-6: the unadjusted recommended date is the contained forecast's",
        date("02/15/2024"), theVaccineGroupForecast().getUnadjustedRecommendedDate());
  }

  /** SINGLEANTVG-7 / FORECASTVG-6: unadjusted past due date. */
  @Test
  public void singleantvgSevenTheUnadjustedPastDueDateIsTheContainedForecasts() throws Exception {
    PatientSeries patientSeries = hepBFixture(PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    patientSeries.getForecast().setUnadjustedPastDueDate(date("03/15/2024"));

    assertEquals("SINGLEANTVG-7: the unadjusted past due date is the contained forecast's",
        date("03/15/2024"), theVaccineGroupForecast().getUnadjustedPastDueDate());
  }

  /** SINGLEANTVG-8 / FORECASTVG-7: forecast reason. */
  @Test
  public void singleantvgEightTheForecastReasonIsTheContainedForecasts() throws Exception {
    PatientSeries patientSeries = hepBFixture(PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    patientSeries.getForecast().setForecastReason("Due Now");

    assertEquals("SINGLEANTVG-8: the forecast reason is the contained forecast's",
        "Due Now", theVaccineGroupForecast().getForecastReason());
  }

  /**
   * SINGLEANTVG-9 / FORECASTVG-8: "The vaccine group forecast antigens needed
   * for a single antigen vaccine group must be the best patient series target
   * disease" (the code's own comment; FORECASTVG-8 phrases the general rule as
   * "an antigen is a recommended antigen if its best patient series is the
   * basis of a contained forecast with status 'Not Complete'"). Unlike
   * SINGLEANTVG-10 below, this one has somewhere to go -
   * {@code VaccineGroupForecast.setAntigensNeededList} exists and
   * {@code MultipleAntigenVaccineGroup.MULTIANTVG_8()} calls it for the
   * multiple antigen branch - but 9.2's call is commented out.
   */
  @Test
  public void singleantvgNineTheAntigensNeededAreTheContainedPatientSeriesTargetDisease() throws Exception {
    hepBFixture(PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    assertEquals("SINGLEANTVG-9: a Not Complete single antigen vaccine group needs its one antigen",
        Arrays.asList(dataModel.getOrCreateAntigen(HEPATITIS_B)),
        theVaccineGroupForecast().getAntigensNeededList());
  }

  /**
   * SINGLEANTVG-10 / FORECASTVG-9: "The vaccine group forecast recommended
   * vaccines for a single antigen vaccine group must be the best patient series
   * forecast recommended vaccines." The comment is in the class with no
   * statement under it, because there is nothing to write to: neither
   * {@code VaccineGroupForecast} nor the {@code Forecast} it extends carries a
   * recommended-vaccine list, and 7.5's FORECASTRECVAC-1 test is red for the
   * missing field on the patient series side. Same probe, and the same result,
   * as 9.1's {@code forecastvgNineAVaccineGroupForecastCanCarryItsRecommended
   * SeriesDoseVaccines}; recorded here too because 9.2 is a second consumer of
   * the same gap.
   */
  @Test
  public void singleantvgTenTheVaccineGroupForecastCanCarryItsRecommendedSeriesDoseVaccines() {
    assertNotNull("SINGLEANTVG-10: a single antigen vaccine group's forecast must carry the contained patient"
        + " series forecast's recommended vaccines, but VaccineGroupForecast has no recommended-vaccine list;"
        + " its accessors are " + accessorNames(VaccineGroupForecast.class),
        accessorMatching(VaccineGroupForecast.class,
            "(?i)get.*recommend\\w*.*vaccine.*|get.*vaccine.*recommend\\w*.*"));
  }

  // ---------------------------------------------------------------------
  // Both of Table 9-3's rules are phrased over "the patient series forecast(s)
  // contained in the vaccine group forecast" - Table 9-2's FORECASTVG-1.
  // ---------------------------------------------------------------------

  /**
   * SINGLEANTVG-1 and SINGLEANTVG-2 both name their input as "the patient
   * series forecast[s] <i>contained in</i> the vaccine group forecast", which
   * FORECASTVG-1 defines. {@code VaccineGroupForecast} has a
   * {@code forecastList} for exactly that, so containment is expressible; 9.2
   * reads the best patient series list to find its one contributor and never
   * records which forecast it used.
   */
  @Test
  public void theContainedPatientSeriesForecastIsRecordedOnTheVaccineGroupForecast() throws Exception {
    PatientSeries patientSeries = hepBFixture(PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    assertEquals("FORECASTVG-1, which SINGLEANTVG-1/2 both refer to: the patient series forecast the vaccine"
        + " group forecast was made from is contained in it",
        Arrays.asList(patientSeries.getForecast()), theVaccineGroupForecast().getForecastList());
  }

  // ---------------------------------------------------------------------
  // Which patient series forecast is the contained one, and how many vaccine
  // group forecasts one run produces.
  // ---------------------------------------------------------------------

  /**
   * Entry Conditions: the vaccine group forecast is made for the vaccine group
   * 4.6 has made current and 9.1 has classified, so it must be attributed to
   * that group and to its one classified antigen.
   */
  @Test
  public void theVaccineGroupForecastIsAttributedToTheVaccineGroupAndItsOneAntigen() throws Exception {
    VaccineGroup vaccineGroup = singleAntigenVaccineGroup(HEPB, HEPATITIS_B);
    bestPatientSeries("HepB standard", HEPATITIS_B, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    VaccineGroupForecast vaccineGroupForecast = theVaccineGroupForecast();

    assertSame("the forecast is made for the vaccine group being processed", vaccineGroup,
        vaccineGroupForecast.getVaccineGroup());
    assertEquals("... and for the one antigen that group classifies", dataModel.getOrCreateAntigen(HEPATITIS_B),
        vaccineGroupForecast.getAntigen());
  }

  /**
   * FORECASTVG-1 requires the contained forecast's antigen series to define a
   * regimen the vaccine group classifies, so a best patient series for a
   * different antigen contributes nothing - not its status, and not its dates,
   * even when its earliest date is the earlier of the two.
   */
  @Test
  public void aBestPatientSeriesForAnotherAntigenIsNotContainedInThisVaccineGroupsForecast() throws Exception {
    singleAntigenVaccineGroup(HEPB, HEPATITIS_B);
    bestPatientSeries("Measles standard", MEASLES, PatientSeriesStatus.COMPLETE, date("01/01/2020"));
    bestPatientSeries("HepB standard", HEPATITIS_B, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    VaccineGroupForecast vaccineGroupForecast = theVaccineGroupForecast();

    assertEquals("only HepB's best patient series is contained in the HepB vaccine group's forecast", 1,
        dataModel.getVaccineGroupForecastList().size());
    assertEquals("the Measles series' Complete status must not become the HepB group's status",
        VaccineGroupStatus.NOT_COMPLETE, vaccineGroupForecast.getVaccineGroupStatus());
    assertEquals("nor its earlier earliest date", date("01/01/2024"), vaccineGroupForecast.getEarliestDate());
  }

  /**
   * A vaccine group forecast is made <i>for a vaccine group</i>, and 4.6 runs
   * 9.2 once per vaccine group - so one run produces at most one, however many
   * patient series forecasts it contains. Table 9-3's own wording assumes this:
   * SINGLEANTVG-2 takes the earliest date "of all the patient series forecasts
   * contained in <i>the</i> vaccine group forecast", singular.
   */
  @Test
  public void oneRunProducesAtMostOneVaccineGroupForecastForTheVaccineGroup() throws Exception {
    singleAntigenVaccineGroup(HEPB, HEPATITIS_B);
    bestPatientSeries("HepB standard", HEPATITIS_B, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    bestPatientSeries("HepB increased risk", HEPATITIS_B, PatientSeriesStatus.NOT_COMPLETE, date("06/01/2024"));

    runStep();

    assertEquals("one vaccine group means one vaccine group forecast, containing both patient series forecasts",
        1, dataModel.getVaccineGroupForecastList().size());
  }

  // ---------------------------------------------------------------------
  // State Changes - the two documented defensive fallbacks.
  // ---------------------------------------------------------------------

  /**
   * State Changes: "If no best patient series' forecast antigen matches the
   * vaccine group's antigen at all, logs an {@code ALERT.SPECGAP} and the
   * vaccine group forecast list simply doesn't gain an entry for this group
   * (rather than throwing)." Not spec-defined - Table 9-3 says nothing about a
   * vaccine group with no contained forecast - but it is what the step does,
   * and pinning it makes the gap visible.
   */
  @Test
  public void aVaccineGroupWithNoMatchingBestPatientSeriesIsAlertedAndProducesNoForecast() throws Exception {
    singleAntigenVaccineGroup(HEPB, HEPATITIS_B);
    bestPatientSeries("Measles standard", MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    SingleAntigenVaccineGroup step = new SingleAntigenVaccineGroup(dataModel);
    step.process();

    assertTrue("no contained patient series forecast means no vaccine group forecast for this group",
        dataModel.getVaccineGroupForecastList().isEmpty());
    assertTrue("State Changes: the gap is alerted as ALERT.SPECGAP; log was " + step.getLogList(),
        hasAlertContaining(step, "ALERT.SPECGAP"));
  }

  /**
   * State Changes, the other fallback: a vaccine group that classifies no
   * antigen cannot be a single antigen vaccine group at all (VACCINEGROUP-1
   * requires exactly one), yet 9.1 sends an empty group down the multiple
   * antigen branch and 4.6 hands over every vaccine group without checking, so
   * the guard is reachable in principle. It alerts {@code ALERT.MISSING},
   * produces no forecast and still returns to the chapter driver.
   */
  @Test
  public void aVaccineGroupThatClassifiesNoAntigenIsAlertedAndProducesNoForecast() throws Exception {
    dataModel.setVaccineGroup(dataModel.getOrCreateVaccineGroup("Empty group"));
    bestPatientSeries("HepB standard", HEPATITIS_B, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    SingleAntigenVaccineGroup step = new SingleAntigenVaccineGroup(dataModel);
    LogicStep next = step.process();

    assertTrue("a group with no classified antigen has no forecast to make",
        dataModel.getVaccineGroupForecastList().isEmpty());
    assertTrue("State Changes: the missing antigen is alerted as ALERT.MISSING; log was " + step.getLogList(),
        hasAlertContaining(step, "ALERT.MISSING"));
    assertEquals("and the early exit still returns to the chapter driver",
        LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, next.getLogicStepType());
  }

  // ---------------------------------------------------------------------
  // Next Steps, and the shape of the section.
  // ---------------------------------------------------------------------

  /**
   * Next Steps / {@code transitions.yaml}: "Unconditional return to section
   * '9'" - the chapter's vaccine group loop driver - "whether or not a match
   * was found". Checked on all three paths through {@code process()}: a match,
   * no match, and the missing-antigen early exit.
   */
  @Test
  public void theStepAlwaysReturnsToTheChapterDriver() throws Exception {
    hepBFixture(PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    LogicStep afterMatch = new SingleAntigenVaccineGroup(dataModel).process();
    assertEquals(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, afterMatch.getLogicStepType());
    assertTrue("9.2 must return to section 9's own class, got " + afterMatch.getClass().getName(),
        afterMatch instanceof IdentifyAndEvaluateVaccineGroup);

    setUp();
    singleAntigenVaccineGroup(HEPB, HEPATITIS_B);
    LogicStep afterNoMatch = new SingleAntigenVaccineGroup(dataModel).process();
    assertEquals("... including when no best patient series matched",
        LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, afterNoMatch.getLogicStepType());

    setUp();
    dataModel.setVaccineGroup(dataModel.getOrCreateVaccineGroup("Empty group"));
    LogicStep afterEarlyExit = new SingleAntigenVaccineGroup(dataModel).process();
    assertEquals("... and on the missing-antigen early exit",
        LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, afterEarlyExit.getLogicStepType());
  }

  /**
   * Decision Tables: "None - this section applies rules directly rather than
   * through a Yes/No decision grid." Pinned as behaviour: the constructor still
   * registers one {@code LogicTable}, but it is an empty 0x0 placeholder
   * labelled "Table ?-?" with every condition and outcome commented out, and
   * {@code process()} never evaluates it. Worth pinning because the step viewer
   * renders the registered tables, so 9.2 displays an empty table headed with a
   * placeholder rather than citing Table 9-3.
   */
  @Test
  public void theSectionAppliesItsRulesDirectlyRatherThanThroughADecisionTable() {
    singleAntigenVaccineGroup(HEPB, HEPATITIS_B);
    SingleAntigenVaccineGroup step = new SingleAntigenVaccineGroup(dataModel);

    assertEquals("9.2 registers one logic table", 1, step.getLogicTableList().size());
    LogicTable logicTable = step.getLogicTableList().get(0);
    assertEquals("Actual behaviour, pinned: Table 9-3 is prose, so the registered table asks no questions",
        0, logicTable.getLogicConditions().length);
    assertEquals("... and offers no outcomes", 0, logicTable.getLogicOutcomes().length);
  }
}
