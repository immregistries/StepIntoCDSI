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
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.IntervalPriority;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * Section 9.3 "Multiple Antigen Vaccine Group" (Logic Specification for ACIP
 * Recommendations v4.6, pages 95-96; Table 9-4 "What is the Vaccine Group Status
 * of a Vaccine Group Forecast for a Multiple Antigen Vaccine Group?" and Table
 * 9-5 "Multiple Antigen Vaccine Group Business Rules") as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/09-03-multiple-antigen-vaccine-group/index.md}.
 *
 * <p>
 * <b>What the section says.</b> The section owns exactly two named business
 * rules and one decision table.
 *
 * <ul>
 * <li><b>Table 9-4</b> - a six-condition cascade, each condition asked only if
 * every prior one answered No: any contained forecast 'Contraindicated' →
 * Contraindicated; else any 'Aged Out' → Aged Out; else any 'Not Recommended' →
 * Not Recommended; else any 'Not Complete' → Not Complete; else all 'Immune' →
 * Immune; else all 'Complete' or 'Immune' → Complete.</li>
 * <li><b>MULTIANTVG-1</b> - "The earliest date of a vaccine group forecast for a
 * multiple antigen vaccine group must be one of the following: the later of (the
 * earliest date of all contained patient series forecasts; the latest date
 * administered of any vaccine dose administered belonging to the vaccine group)
 * if any contained forecast is a priority patient series forecast; otherwise,
 * the latest earliest date of all contained patient series forecasts."</li>
 * <li><b>FORECASTPRIORITY-1</b> - "A patient series forecast is a priority
 * patient series forecast if its target dose has at least one preferable
 * interval, and every preferable interval for that target dose has an interval
 * priority flag of 'Y'."</li>
 * </ul>
 *
 * <p>
 * <b>What else is tested here, and why.</b> 9.3's State Changes record that this
 * class is also "where the behavior described by 9.1's FORECASTVG-2 through
 * FORECASTVG-9 actually runs" - Table 9-2 belongs to unit 9.1, whose own class
 * implements none of it. The class labels those aggregations
 * {@code MULTIANTVG_2()} through {@code MULTIANTVG_9}, labels the specification
 * itself does not define (Table 9-5 names only MULTIANTVG-1), so each is tested
 * against the Table 9-2 rule whose behaviour it carries, named by its
 * {@code FORECASTVG-*} id. FORECASTVG-1 (containment), FORECASTVG-9 (recommended
 * series dose vaccines) and FORECASTDN-2 (forecast dose number) are covered by
 * the same "can the rule even be expressed?" probes 6.2, 7.1, 7.5, 7.6, 8.8, 9.1
 * and 9.2 use; see the 2026-09-07 entry in
 * {@code cdsi-reference/step-tests/cross-cutting-notes.md}, which this unit
 * closes out.
 *
 * <p>
 * <b>Isolation.</b> Every test hand-builds the minimal {@code DataModel} the
 * step actually reads - a current {@code VaccineGroup} classifying more than one
 * {@code Antigen} (VACCINEGROUP-2, which 9.1 has already decided), and a best
 * patient series list holding {@code PatientSeries} objects each carrying the
 * {@code Forecast} 7.5 generated for it - and calls the constructor and
 * {@code process()} directly. Nothing is loaded from a Supporting Data release:
 * 9.3 reads no Supporting Data of its own, only Chapter 8's output and 4.6's
 * current vaccine group. The one exception is the interval priority flag, whose
 * release representation ("override", not the specification's 'Y' - 90 of the
 * 1492 {@code <intervalPriority>} elements in the bundled 4.65-508 release carry
 * it) is built directly as {@code IntervalPriority.OVERRIDE}, the single value
 * the enum and the loader define.
 *
 * <p>
 * MMR is used throughout as the concrete multiple antigen vaccine group: it is
 * one of the two groups VACCINEGROUP-2 classifies as multiple antigen in the
 * bundled release, and it classifies three antigens, so a cascade of six
 * conditions over more than two contained forecasts can be exercised.
 */
public class MultipleAntigenVaccineGroupTest {

  private static final String MMR = "MMR";
  private static final String MEASLES = "Measles";
  private static final String MUMPS = "Mumps";
  private static final String RUBELLA = "Rubella";
  private static final String HEPB = "HepB";
  private static final String HEPATITIS_B = "Hepatitis B";

  private static final SimpleDateFormat SDF = new SimpleDateFormat("MM/dd/yyyy");

  private DataModel dataModel;

  @Before
  public void setUp() {
    dataModel = new DataModel();
    dataModel.setBestPatientSeriesList(new ArrayList<PatientSeries>());
    dataModel.setVaccineGroupForecastList(new ArrayList<VaccineGroupForecast>());
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the state 4.6, 9.1 and Chapter 8 leave behind for 9.3.
  // ---------------------------------------------------------------------

  private static Date date(String mmddyyyy) {
    try {
      return SDF.parse(mmddyyyy);
    } catch (ParseException pe) {
      throw new IllegalArgumentException(pe);
    }
  }

  /**
   * The current vaccine group 9.1 has just classified as a multiple antigen
   * vaccine group - VACCINEGROUP-2, "classifies more than one antigen".
   */
  private VaccineGroup multipleAntigenVaccineGroup(String vaccineGroupName, String... antigenNames) {
    VaccineGroup vaccineGroup = dataModel.getOrCreateVaccineGroup(vaccineGroupName);
    for (String antigenName : antigenNames) {
      vaccineGroup.getAntigenList().add(dataModel.getOrCreateAntigen(antigenName));
    }
    dataModel.setVaccineGroup(vaccineGroup);
    return vaccineGroup;
  }

  /** MMR - Measles, Mumps and Rubella - the running multiple antigen example. */
  private VaccineGroup mmrVaccineGroup() {
    return multipleAntigenVaccineGroup(MMR, MEASLES, MUMPS, RUBELLA);
  }

  /**
   * One entry of Chapter 8's output: a best patient series for
   * {@code antigenName}, carrying the patient series forecast 7.5 generated for
   * it. Added to the best patient series list in call order, so that tests can
   * distinguish an order-dependent implementation from a real minimum or
   * maximum.
   */
  private PatientSeries bestPatientSeries(String antigenName, PatientSeriesStatus status, Date earliestDate) {
    Antigen antigen = dataModel.getOrCreateAntigen(antigenName);
    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(antigenName + " standard");
    antigenSeries.setTargetDisease(antigen);

    Forecast forecast = new Forecast();
    forecast.setAntigen(antigen);
    forecast.setEarliestDate(earliestDate);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setPatientSeriesStatus(status);
    patientSeries.setForecast(forecast);
    patientSeries.setTargetDoseList(new ArrayList<TargetDose>());

    dataModel.getBestPatientSeriesList().add(patientSeries);
    return patientSeries;
  }

  /**
   * A target dose with one preferable interval per supplied priority - the input
   * FORECASTPRIORITY-1 is defined over. A {@code null} entry is a preferable
   * interval whose interval priority flag is not 'Y'.
   */
  private static TargetDose targetDoseWithPreferableIntervals(IntervalPriority... intervalPriorities) {
    SeriesDose seriesDose = new SeriesDose();
    seriesDose.setDoseNumber("1");
    for (IntervalPriority intervalPriority : intervalPriorities) {
      Interval interval = new Interval();
      interval.setSeriesDose(seriesDose);
      interval.setIntervalPriority(intervalPriority);
      seriesDose.getIntervalList().add(interval);
    }
    return new TargetDose(seriesDose);
  }

  /**
   * Makes {@code patientSeries}' forecast a priority patient series forecast in
   * a way no reading of FORECASTPRIORITY-1 can disagree about: its target dose
   * has exactly one preferable interval and that interval carries the priority
   * flag, <i>and</i> the same interval is hung on the forecast itself. Used by
   * the MULTIANTVG-1 tests, which are about which branch the rule takes rather
   * than about how the branch is detected; the FORECASTPRIORITY-1 tests below
   * deliberately do not set both.
   */
  private static void makePriorityPatientSeriesForecast(PatientSeries patientSeries) {
    TargetDose targetDose = targetDoseWithPreferableIntervals(IntervalPriority.OVERRIDE);
    patientSeries.getForecast().setTargetDose(targetDose);
    patientSeries.getForecast().setInterval(targetDose.getTrackedSeriesDose().getIntervalList().get(0));
  }

  /**
   * A vaccine dose administered belonging to {@code vaccineGroup} - the second
   * term of MULTIANTVG-1's priority branch. The dose is of a vaccine listed by
   * the group itself, so its membership needs no CVX-to-antigen inference.
   */
  private void vaccineDoseAdministeredInGroup(VaccineGroup vaccineGroup, String tradeName, Date dateAdministered) {
    Vaccine vaccine = new Vaccine();
    vaccine.setTradeName(tradeName);
    vaccineGroup.getVaccineList().add(vaccine);

    VaccineDoseAdministered vaccineDoseAdministered = new VaccineDoseAdministered();
    vaccineDoseAdministered.setVaccine(vaccine);
    vaccineDoseAdministered.setDateAdministered(dateAdministered);

    if (dataModel.getImmunizationHistory() == null) {
      dataModel.setImmunizationHistory(new ImmunizationHistory());
    }
    dataModel.getImmunizationHistory().getVaccineDoseAdministeredList().add(vaccineDoseAdministered);
  }

  // ---------------------------------------------------------------------
  // Driving the step.
  // ---------------------------------------------------------------------

  private MultipleAntigenVaccineGroup runStep() throws Exception {
    MultipleAntigenVaccineGroup step = new MultipleAntigenVaccineGroup(dataModel);
    step.process();
    return step;
  }

  /** The one vaccine group forecast 9.3 is expected to have produced. */
  private VaccineGroupForecast theVaccineGroupForecast() throws Exception {
    runStep();
    List<VaccineGroupForecast> produced = dataModel.getVaccineGroupForecastList();
    assertFalse("9.3 must produce a vaccine group forecast for the multiple antigen vaccine group",
        produced.isEmpty());
    return produced.get(0);
  }

  /**
   * The "can the rule even be expressed?" probe used by 6.2, 7.1, 7.5, 7.6, 8.8,
   * 9.1 and 9.2 - the first no-argument accessor of {@code type} whose name
   * matches, or null.
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

  // =====================================================================
  // Table 9-4 - What is the Vaccine Group Status of a Vaccine Group Forecast
  // for a Multiple Antigen Vaccine Group? One test per rule column.
  // =====================================================================

  /**
   * Table 9-4 Rule 1: "Is there a patient series forecast contained in the
   * vaccine group forecast with a patient series status of 'Contraindicated'?" -
   * Yes → Contraindicated. Only one of MMR's three antigens is contraindicated,
   * and the rule is an "any", not an "all".
   *
   * <p>
   * This is also the direct confirmation that {@code MultipleAntigenVaccineGroup}
   * really is a downstream consumer of {@code PatientSeriesStatus.CONTRAINDICATED}
   * alongside 8.1, as the 2026-09-04 "Schedule-level Supporting Data" entry in
   * {@code cross-cutting-notes.md} says.
   */
  @Test
  public void tableNineFourRuleOneAnyContainedForecastContraindicatedMakesTheGroupContraindicated() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.CONTRAINDICATED, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    assertEquals("Table 9-4 Rule 1: one contraindicated contained forecast makes the whole group Contraindicated",
        VaccineGroupStatus.CONTRAINDICATED, theVaccineGroupForecast().getVaccineGroupStatus());
  }

  /**
   * Table 9-4 Rule 2: no contained forecast is 'Contraindicated', but one is
   * 'Aged Out' → Aged Out.
   */
  @Test
  public void tableNineFourRuleTwoAnyContainedForecastAgedOutMakesTheGroupAgedOut() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.AGED_OUT, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    assertEquals("Table 9-4 Rule 2: with nothing contraindicated, one aged out contained forecast makes the"
        + " group Aged Out", VaccineGroupStatus.AGED_OUT, theVaccineGroupForecast().getVaccineGroupStatus());
  }

  /**
   * Table 9-4 Rule 3: nothing 'Contraindicated' or 'Aged Out', but one contained
   * forecast is 'Not Recommended' → Not Recommended.
   */
  @Test
  public void tableNineFourRuleThreeAnyContainedForecastNotRecommendedMakesTheGroupNotRecommended() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_RECOMMENDED, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    assertEquals("Table 9-4 Rule 3: one not recommended contained forecast makes the group Not Recommended",
        VaccineGroupStatus.NOT_RECOMMENDED, theVaccineGroupForecast().getVaccineGroupStatus());
  }

  /**
   * Table 9-4 Rule 4: nothing higher in the cascade, but one contained forecast
   * is 'Not Complete' → Not Complete. This is the group that still needs doses,
   * and the only outcome for which the class runs its date aggregation.
   */
  @Test
  public void tableNineFourRuleFourAnyContainedForecastNotCompleteMakesTheGroupNotComplete() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.IMMUNE, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    assertEquals("Table 9-4 Rule 4: one not complete contained forecast makes the group Not Complete",
        VaccineGroupStatus.NOT_COMPLETE, theVaccineGroupForecast().getVaccineGroupStatus());
  }

  /**
   * Table 9-4 Rule 5: <i>all</i> contained forecasts are 'Immune' → Immune. The
   * fifth and sixth conditions are the only two of the six phrased as "all"
   * rather than "any".
   */
  @Test
  public void tableNineFourRuleFiveAllContainedForecastsImmuneMakesTheGroupImmune() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.IMMUNE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.IMMUNE, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.IMMUNE, date("01/01/2024"));

    assertEquals("Table 9-4 Rule 5: every contained forecast immune makes the group Immune",
        VaccineGroupStatus.IMMUNE, theVaccineGroupForecast().getVaccineGroupStatus());
  }

  /**
   * Table 9-4 Rule 6: not all 'Immune', but all 'Complete' or 'Immune' →
   * Complete. Rule 5 is asked first, so a group that is entirely immune is
   * Immune, and a mixture is Complete.
   */
  @Test
  public void tableNineFourRuleSixAllContainedForecastsCompleteOrImmuneMakesTheGroupComplete() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.IMMUNE, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.COMPLETE, date("01/01/2024"));

    assertEquals("Table 9-4 Rule 6: a mixture of complete and immune contained forecasts makes the group Complete",
        VaccineGroupStatus.COMPLETE, theVaccineGroupForecast().getVaccineGroupStatus());
  }

  /**
   * Table 9-4 is a cascade, not six independent questions: each condition is
   * asked only if every condition above it answered No, so the first status
   * present in the table's own order wins however many other statuses the group
   * contains. Checked at each of the four "any" boundaries in turn.
   */
  @Test
  public void tableNineFourIsACascadeSoTheHighestStatusPresentWins() throws Exception {
    setUp();
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.AGED_OUT, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.CONTRAINDICATED, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    assertEquals("Contraindicated outranks Aged Out and Not Complete", VaccineGroupStatus.CONTRAINDICATED,
        theVaccineGroupForecast().getVaccineGroupStatus());

    setUp();
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_RECOMMENDED, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.AGED_OUT, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    assertEquals("Aged Out outranks Not Recommended and Not Complete", VaccineGroupStatus.AGED_OUT,
        theVaccineGroupForecast().getVaccineGroupStatus());

    setUp();
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_RECOMMENDED, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.COMPLETE, date("01/01/2024"));
    assertEquals("Not Recommended outranks Not Complete", VaccineGroupStatus.NOT_RECOMMENDED,
        theVaccineGroupForecast().getVaccineGroupStatus());

    setUp();
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.IMMUNE, date("01/01/2024"));
    assertEquals("Not Complete outranks the two 'all' rules below it", VaccineGroupStatus.NOT_COMPLETE,
        theVaccineGroupForecast().getVaccineGroupStatus());
  }

  /**
   * Table 9-4's State Changes: each of the six outcomes writes the group's
   * status to <i>both</i> of {@code VaccineGroupForecast}'s status fields - the
   * {@code VaccineGroupStatus} the table names, and the {@code PatientSeriesStatus}
   * the class also carries. They describe the same one decision, so they must
   * agree for every rule of the table.
   */
  @Test
  public void tableNineFoursOutcomeIsCarriedOnBothOfTheVaccineGroupForecastsStatusFields() throws Exception {
    PatientSeriesStatus[] statuses = { PatientSeriesStatus.CONTRAINDICATED, PatientSeriesStatus.AGED_OUT,
        PatientSeriesStatus.NOT_RECOMMENDED, PatientSeriesStatus.NOT_COMPLETE, PatientSeriesStatus.IMMUNE,
        PatientSeriesStatus.COMPLETE };
    for (PatientSeriesStatus status : statuses) {
      setUp();
      mmrVaccineGroup();
      bestPatientSeries(MEASLES, status, date("01/01/2024"));
      bestPatientSeries(MUMPS, status, date("01/01/2024"));
      bestPatientSeries(RUBELLA, status, date("01/01/2024"));

      VaccineGroupForecast vaccineGroupForecast = theVaccineGroupForecast();
      assertEquals("Table 9-4: a group whose contained forecasts are all " + status + " has that vaccine group"
          + " status", VaccineGroupStatus.valueOf(status.name()), vaccineGroupForecast.getVaccineGroupStatus());
      assertEquals("... and the same value on the patient series status field", status,
          vaccineGroupForecast.getPatientSeriesStatus());
    }
  }

  /**
   * Table 9-4 has six conditions and six outcome columns; pinned structurally so
   * that a future change that drops or reorders a condition is visible as more
   * than a changed status. (The class's own label for the table reads "Table 9 -
   * 3 WHAT IS THE VACCINE GROUP STATUS OF A MULTIPLE VACCINE GROUP?" - the
   * specification's number for it is 9-4; that is a traceability nit recorded in
   * this unit's notes, not asserted here.)
   */
  @Test
  public void theStepImplementsTableNineFoursSixConditionCascade() {
    mmrVaccineGroup();
    MultipleAntigenVaccineGroup step = new MultipleAntigenVaccineGroup(dataModel);

    assertEquals("9.3 registers one logic table", 1, step.getLogicTableList().size());
    LogicTable logicTable = step.getLogicTableList().get(0);
    assertEquals("Table 9-4 asks six questions", 6, logicTable.getLogicConditions().length);
    assertEquals("... and offers six outcomes", 6, logicTable.getLogicOutcomes().length);
  }

  // =====================================================================
  // MULTIANTVG-1 - the earliest date of the vaccine group forecast.
  // =====================================================================

  /**
   * MULTIANTVG-1's "otherwise" branch: with no contained forecast a priority
   * patient series forecast, the vaccine group forecast's earliest date is "the
   * latest earliest date of all contained patient series forecasts" - the group
   * cannot be started before every one of its antigens can be.
   *
   * <p>
   * The latest of the three is put in the middle of the list so that a
   * "first wins" or "last wins" implementation is distinguishable from a real
   * maximum.
   */
  @Test
  public void multiantvgOneWithoutAPriorityForecastTheEarliestDateIsTheLatestOfTheContainedEarliestDates()
      throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("06/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("09/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("03/01/2024"));

    assertEquals("MULTIANTVG-1, non-priority branch: the latest earliest date of all contained forecasts",
        date("09/01/2024"), theVaccineGroupForecast().getEarliestDate());
  }

  /**
   * MULTIANTVG-1's priority branch: "if <i>any</i> contained forecast is a
   * priority patient series forecast", the first term becomes "the earliest date
   * of all contained patient series forecasts" - a minimum over every contained
   * forecast, not only over the priority ones. One priority contained forecast
   * therefore flips the whole calculation for the group.
   *
   * <p>
   * Only Mumps is a priority patient series forecast here; the earliest of the
   * three dates belongs to Rubella, which is not.
   */
  @Test
  public void multiantvgOneOnePriorityForecastMakesTheEarliestDateTheEarliestOfAllContainedForecasts()
      throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("06/01/2024"));
    makePriorityPatientSeriesForecast(
        bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("09/01/2024")));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("03/01/2024"));

    assertEquals("MULTIANTVG-1, priority branch: the earliest date of all contained forecasts, including the"
        + " ones that are not themselves priority forecasts", date("03/01/2024"),
        theVaccineGroupForecast().getEarliestDate());
  }

  /**
   * MULTIANTVG-1's priority branch has two terms, and the second is "the latest
   * date administered of any vaccine dose administered belonging to the vaccine
   * group" - the group cannot be restarted before the last dose it already
   * received. Both contained forecasts are priority forecasts here and the
   * latest date administered is later than either earliest date, so the answer
   * is the dose date.
   *
   * <p>
   * 9.3's Review Findings flag this clause as "unconfirmed rather than asserted
   * as fully conformant" - this test is what confirms it.
   */
  @Test
  public void multiantvgOneThePriorityBranchIsNoEarlierThanTheLatestDoseAdministeredInTheVaccineGroup()
      throws Exception {
    VaccineGroup vaccineGroup = mmrVaccineGroup();
    makePriorityPatientSeriesForecast(
        bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("06/01/2024")));
    makePriorityPatientSeriesForecast(
        bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("09/01/2024")));
    vaccineDoseAdministeredInGroup(vaccineGroup, "M-M-R II", date("12/01/2024"));

    assertEquals("MULTIANTVG-1, priority branch: the later of the earliest contained date (06/01/2024) and the"
        + " latest date administered in the vaccine group (12/01/2024)", date("12/01/2024"),
        theVaccineGroupForecast().getEarliestDate());
  }

  // =====================================================================
  // FORECASTPRIORITY-1 - which contained forecasts are priority forecasts.
  // =====================================================================

  /**
   * FORECASTPRIORITY-1: "A patient series forecast is a priority patient series
   * forecast if its target dose has at least one preferable interval, and every
   * preferable interval for that target dose has an interval priority flag of
   * 'Y'." Measles' target dose has exactly one preferable interval and it
   * carries the flag, so Measles is a priority patient series forecast and
   * MULTIANTVG-1 takes its priority branch (the earliest of the two dates,
   * 06/01/2024) rather than the "otherwise" branch (the latest, 09/01/2024).
   *
   * <p>
   * Deliberately expressed only through the target dose's preferable intervals,
   * which is where FORECASTPRIORITY-1 defines it.
   */
  @Test
  public void forecastpriorityOneATargetDoseWhosePreferableIntervalHasThePriorityFlagIsAPriorityForecast()
      throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("06/01/2024"));
    measles.getForecast().setTargetDose(targetDoseWithPreferableIntervals(IntervalPriority.OVERRIDE));
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("09/01/2024"));

    assertEquals("FORECASTPRIORITY-1: a target dose with one preferable interval carrying the priority flag"
        + " makes its forecast a priority patient series forecast, so MULTIANTVG-1 takes the earliest date",
        date("06/01/2024"), theVaccineGroupForecast().getEarliestDate());
  }

  /**
   * FORECASTPRIORITY-1's "<i>every</i> preferable interval ... has an interval
   * priority flag of 'Y'": Measles' target dose has two preferable intervals and
   * only one of them carries the flag, so Measles is <b>not</b> a priority
   * patient series forecast and MULTIANTVG-1 must take its "otherwise" branch -
   * the latest earliest date, 09/01/2024.
   *
   * <p>
   * The flagged interval is also hung on the forecast itself, which is how a
   * per-interval reading of the rule would see a priority where the rule defines
   * none. Mumps is listed first so that the two readings cannot agree by
   * accident.
   */
  @Test
  public void forecastpriorityOneOneUnflaggedPreferableIntervalMeansTheForecastIsNotAPriorityForecast()
      throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("09/01/2024"));
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("06/01/2024"));
    TargetDose targetDose = targetDoseWithPreferableIntervals(IntervalPriority.OVERRIDE, null);
    measles.getForecast().setTargetDose(targetDose);
    measles.getForecast().setInterval(targetDose.getTrackedSeriesDose().getIntervalList().get(0));

    assertEquals("FORECASTPRIORITY-1: not every preferable interval of the target dose carries the priority"
        + " flag, so this is not a priority patient series forecast and MULTIANTVG-1 takes the latest"
        + " earliest date", date("09/01/2024"), theVaccineGroupForecast().getEarliestDate());
  }

  /**
   * FORECASTPRIORITY-1's "at least one preferable interval": a target dose that
   * defines no preferable interval at all cannot be a priority patient series
   * forecast, whatever else is true of it, so MULTIANTVG-1 takes the "otherwise"
   * branch. This is the ordinary case - 1402 of the 1492
   * {@code <intervalPriority>} elements in the bundled 4.65-508 release are
   * empty.
   */
  @Test
  public void forecastpriorityOneATargetDoseWithNoPreferableIntervalIsNotAPriorityForecast() throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("06/01/2024"));
    measles.getForecast().setTargetDose(targetDoseWithPreferableIntervals());
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("09/01/2024"));
    mumps.getForecast().setTargetDose(targetDoseWithPreferableIntervals());

    assertEquals("FORECASTPRIORITY-1: no preferable interval means no priority patient series forecast, so"
        + " MULTIANTVG-1 takes the latest earliest date", date("09/01/2024"),
        theVaccineGroupForecast().getEarliestDate());
  }

  // =====================================================================
  // Table 9-2's aggregation rules, which 9.3's State Changes say run here.
  // =====================================================================

  /**
   * <b>FORECASTVG-2</b>: adjusted recommended date = the latest of (the earliest
   * adjusted recommended date across the contained forecasts; the earliest date
   * of the vaccine group forecast). Here the earliest date (02/01/2024, the
   * latest of the two contained earliest dates) is the earlier of the two terms,
   * so the minimum adjusted recommended date wins.
   */
  @Test
  public void forecastvgTwoTheAdjustedRecommendedDateIsTheEarliestAcrossTheContainedForecasts() throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    measles.getForecast().setAdjustedRecommendedDate(date("05/01/2024"));
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("02/01/2024"));
    mumps.getForecast().setAdjustedRecommendedDate(date("08/01/2024"));

    assertEquals("FORECASTVG-2: the earliest adjusted recommended date of the contained forecasts",
        date("05/01/2024"), theVaccineGroupForecast().getAdjustedRecommendedDate());
  }

  /**
   * <b>FORECASTVG-2</b>'s other term: the adjusted recommended date can never be
   * before the vaccine group forecast's own earliest date. Here MULTIANTVG-1
   * makes the earliest date 09/01/2024, later than either contained adjusted
   * recommended date, so it becomes the answer.
   */
  @Test
  public void forecastvgTwoTheAdjustedRecommendedDateIsNeverBeforeTheVaccineGroupForecastsEarliestDate()
      throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    measles.getForecast().setAdjustedRecommendedDate(date("05/01/2024"));
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("09/01/2024"));
    mumps.getForecast().setAdjustedRecommendedDate(date("08/01/2024"));

    assertEquals("FORECASTVG-2: the latest of the earliest contained adjusted recommended date (05/01/2024)"
        + " and the vaccine group forecast's earliest date (09/01/2024)", date("09/01/2024"),
        theVaccineGroupForecast().getAdjustedRecommendedDate());
  }

  /**
   * <b>FORECASTVG-3</b>: adjusted past due date = the latest of (the earliest
   * adjusted past due date across the contained forecasts; the earliest date of
   * the vaccine group forecast). Same shape as FORECASTVG-2, first term winning.
   */
  @Test
  public void forecastvgThreeTheAdjustedPastDueDateIsTheEarliestAcrossTheContainedForecasts() throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    measles.getForecast().setAdjustedPastDueDate(date("06/01/2024"));
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("02/01/2024"));
    mumps.getForecast().setAdjustedPastDueDate(date("09/01/2024"));

    assertEquals("FORECASTVG-3: the earliest adjusted past due date of the contained forecasts",
        date("06/01/2024"), theVaccineGroupForecast().getAdjustedPastDueDate());
  }

  /**
   * <b>FORECASTVG-3</b>'s other term: the adjusted past due date can never be
   * before the vaccine group forecast's own earliest date.
   */
  @Test
  public void forecastvgThreeTheAdjustedPastDueDateIsNeverBeforeTheVaccineGroupForecastsEarliestDate()
      throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    measles.getForecast().setAdjustedPastDueDate(date("04/01/2024"));
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("10/01/2024"));
    mumps.getForecast().setAdjustedPastDueDate(date("07/01/2024"));

    assertEquals("FORECASTVG-3: the latest of the earliest contained adjusted past due date (04/01/2024) and"
        + " the vaccine group forecast's earliest date (10/01/2024)", date("10/01/2024"),
        theVaccineGroupForecast().getAdjustedPastDueDate());
  }

  /**
   * <b>FORECASTVG-4</b>: latest date = the earliest of the latest dates across
   * the contained forecasts - the group's window closes as soon as the first of
   * its antigens' windows does.
   */
  @Test
  public void forecastvgFourTheLatestDateIsTheEarliestOfTheContainedForecastsLatestDates() throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    measles.getForecast().setLatestDate(date("12/31/2024"));
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    mumps.getForecast().setLatestDate(date("06/30/2024"));

    assertEquals("FORECASTVG-4: the earliest of the contained forecasts' latest dates", date("06/30/2024"),
        theVaccineGroupForecast().getLatestDate());
  }

  /**
   * <b>FORECASTVG-5</b>: unadjusted recommended date = the earliest of the
   * unadjusted recommended dates across the contained forecasts.
   */
  @Test
  public void forecastvgFiveTheUnadjustedRecommendedDateIsTheEarliestOfTheContainedForecasts() throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    measles.getForecast().setUnadjustedRecommendedDate(date("05/01/2024"));
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    mumps.getForecast().setUnadjustedRecommendedDate(date("03/01/2024"));

    assertEquals("FORECASTVG-5: the earliest of the contained forecasts' unadjusted recommended dates",
        date("03/01/2024"), theVaccineGroupForecast().getUnadjustedRecommendedDate());
  }

  /**
   * <b>FORECASTVG-6</b>: unadjusted past due date = the earliest of the
   * unadjusted past due dates across the contained forecasts.
   */
  @Test
  public void forecastvgSixTheUnadjustedPastDueDateIsTheEarliestOfTheContainedForecasts() throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    measles.getForecast().setUnadjustedPastDueDate(date("07/01/2024"));
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    mumps.getForecast().setUnadjustedPastDueDate(date("04/01/2024"));

    assertEquals("FORECASTVG-6: the earliest of the contained forecasts' unadjusted past due dates",
        date("04/01/2024"), theVaccineGroupForecast().getUnadjustedPastDueDate());
  }

  /**
   * <b>FORECASTVG-7</b>: the vaccine group forecast's forecast reasons are the
   * union of the forecast reasons of all the contained forecasts, so every
   * distinct reason a contained forecast gave has to survive into the group's.
   */
  @Test
  public void forecastvgSevenTheForecastReasonsAreTheUnionOfTheContainedForecastsReasons() throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    measles.getForecast().setForecastReason("Due Now");
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    mumps.getForecast().setForecastReason("Age");

    String forecastReason = theVaccineGroupForecast().getForecastReason();
    assertTrue("FORECASTVG-7: the union keeps Measles' reason; was \"" + forecastReason + "\"",
        forecastReason.contains("Due Now"));
    assertTrue("FORECASTVG-7: the union keeps Mumps' reason; was \"" + forecastReason + "\"",
        forecastReason.contains("Age"));
  }

  /**
   * <b>FORECASTVG-7</b> says <i>union</i>, which is a set operation: a reason
   * two contained forecasts both give appears once in the vaccine group
   * forecast, not twice. In a multiple antigen vaccine group this is the common
   * case rather than the corner case - the antigens of MMR are forecast from one
   * combined vaccine, so they routinely become due for the same reason at the
   * same time.
   */
  @Test
  public void forecastvgSevenAReasonSharedByTwoContainedForecastsAppearsOnceInTheUnion() throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    measles.getForecast().setForecastReason("Due Now");
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    mumps.getForecast().setForecastReason("Due Now");

    assertEquals("FORECASTVG-7: the union of {\"Due Now\"} and {\"Due Now\"} is {\"Due Now\"}", "Due Now",
        theVaccineGroupForecast().getForecastReason());
  }

  /**
   * <b>FORECASTVG-8</b>: "an antigen is a recommended antigen if its best
   * patient series is the basis of a contained forecast with status 'Not
   * Complete'". Measles and Rubella still need doses, Mumps does not.
   *
   * <p>
   * This is the rule the 2026-09-07 cross-cutting entry predicted 9.3 would be
   * green on - it is implemented as {@code MULTIANTVG_8()} here, and commented
   * out in {@code SingleAntigenVaccineGroup}'s {@code SINGLEANTVG-9}.
   */
  @Test
  public void forecastvgEightTheAntigensNeededAreTheNotCompleteContainedForecastsTargetDiseases() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.COMPLETE, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    assertEquals("FORECASTVG-8: the recommended antigens are the target diseases of the not complete contained"
        + " forecasts", Arrays.asList(dataModel.getOrCreateAntigen(MEASLES), dataModel.getOrCreateAntigen(RUBELLA)),
        theVaccineGroupForecast().getAntigensNeededList());
  }

  /**
   * <b>FORECASTVG-8</b> is defined over the status of each <i>contained
   * forecast</i>, not over the vaccine group's own status, so a group whose
   * overall status Table 9-4 makes Contraindicated still has a recommended
   * antigen if one of its contained forecasts is Not Complete. Clinically this
   * is the ordinary MMR case for a patient with one contraindicated component:
   * the group is contraindicated as a combined vaccine, and the antigen that
   * still needs doses is what a monovalent recommendation would be based on.
   */
  @Test
  public void forecastvgEightTheAntigensNeededAreComputedWhateverTheVaccineGroupStatusIs() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.CONTRAINDICATED, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    VaccineGroupForecast vaccineGroupForecast = theVaccineGroupForecast();
    assertEquals("Precondition, Table 9-4 Rule 1: the group as a whole is Contraindicated",
        VaccineGroupStatus.CONTRAINDICATED, vaccineGroupForecast.getVaccineGroupStatus());
    assertEquals("FORECASTVG-8 asks about each contained forecast's own status, so Mumps is still a"
        + " recommended antigen", Arrays.asList(dataModel.getOrCreateAntigen(MUMPS)),
        vaccineGroupForecast.getAntigensNeededList());
  }

  /**
   * <b>FORECASTVG-9</b>: "A series dose vaccine must be considered a recommended
   * series dose vaccine for a vaccine group forecast if the series dose vaccine
   * is a recommended series dose vaccine for a patient series forecast contained
   * in the vaccine group forecast." The same probe, and the same expected
   * result, as 9.1's and 9.2's: neither {@code VaccineGroupForecast} nor the
   * {@code Forecast} it extends carries a recommended-vaccine list, so the union
   * has nowhere to be written. 9.3's own {@code MULTIANTVG-9} block builds a
   * {@code List<VaccineGroup>} of the contained forecasts' vaccine groups and
   * discards it without ever assigning it anywhere.
   */
  @Test
  public void forecastvgNineAVaccineGroupForecastCanCarryItsRecommendedSeriesDoseVaccines() {
    assertNotNull("FORECASTVG-9: a multiple antigen vaccine group's forecast must carry the recommended series"
        + " dose vaccines of its contained patient series forecasts, but VaccineGroupForecast has no"
        + " recommended-vaccine list; its accessors are " + accessorNames(VaccineGroupForecast.class),
        accessorMatching(VaccineGroupForecast.class,
            "(?i)get.*recommend\\w*.*vaccine.*|get.*vaccine.*recommend\\w*.*"));
  }

  /**
   * <b>FORECASTDN-2</b>: the forecast dose number of a vaccine group forecast is
   * the minimum of the contained forecasts' dose numbers if the vaccine group's
   * administer full vaccine group flag is 'Y', the maximum if it is 'N'. Both
   * branches are live for a multiple antigen vaccine group and only for one -
   * MMR is the release's 'Y' and DTaP/Tdap/Td its 'N', and they are exactly the
   * two groups VACCINEGROUP-2 classifies as multiple antigen - so 9.3 is the
   * step where this rule bites. Same probe as 9.1's.
   */
  @Test
  public void forecastdnTwoAVaccineGroupForecastCanCarryAForecastDoseNumber() {
    assertNotNull("FORECASTDN-2: a vaccine group forecast must carry a forecast dose number (the minimum or"
        + " maximum of its contained forecasts' dose numbers), but VaccineGroupForecast has no dose number"
        + " at all; its accessors are " + accessorNames(VaccineGroupForecast.class),
        accessorMatching(VaccineGroupForecast.class, "(?i)get.*dose.*number.*"));
  }

  /**
   * <b>FORECASTVG-1</b> defines what "contained in the vaccine group forecast"
   * means, and every one of Table 9-4's six conditions plus MULTIANTVG-1 and
   * FORECASTVG-2 through 9 is phrased over it.
   * {@code VaccineGroupForecast.forecastList} exists for exactly this relation,
   * with a getter and a setter; 9.3 assembles its own {@code selectedList} of
   * patient series instead and never records which forecasts the group forecast
   * was built from.
   */
  @Test
  public void theContainedPatientSeriesForecastsAreRecordedOnTheVaccineGroupForecast() throws Exception {
    mmrVaccineGroup();
    PatientSeries measles = bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    PatientSeries mumps = bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("02/01/2024"));

    assertEquals("FORECASTVG-1: the patient series forecasts the vaccine group forecast was made from are"
        + " contained in it", Arrays.asList(measles.getForecast(), mumps.getForecast()),
        theVaccineGroupForecast().getForecastList());
  }

  // =====================================================================
  // State Changes, scope, and Next Steps.
  // =====================================================================

  /**
   * State Changes: the step "builds one {@code VaccineGroupForecast}, selecting
   * the constituent {@code PatientSeries} whose tracked antigen series' target
   * disease is one of the vaccine group's antigens". So the forecast is
   * attributed to the vaccine group 4.6 made current, and records every antigen
   * that contributed to it.
   */
  @Test
  public void theVaccineGroupForecastIsAttributedToTheVaccineGroupAndTheAntigensThatContributed() throws Exception {
    VaccineGroup vaccineGroup = mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    VaccineGroupForecast vaccineGroupForecast = theVaccineGroupForecast();
    assertSame("the forecast is made for the vaccine group being processed", vaccineGroup,
        vaccineGroupForecast.getVaccineGroup());
    assertEquals("... and records all three contributing antigens",
        Arrays.asList(dataModel.getOrCreateAntigen(MEASLES), dataModel.getOrCreateAntigen(MUMPS),
            dataModel.getOrCreateAntigen(RUBELLA)),
        vaccineGroupForecast.getAntigenList());
  }

  /**
   * FORECASTVG-1 requires a contained forecast's antigen series to define a
   * regimen the vaccine group classifies, so a best patient series for an
   * antigen outside the group contributes nothing - not its status, and not its
   * dates, even when they would change both.
   */
  @Test
  public void aBestPatientSeriesForAnAntigenTheGroupDoesNotClassifyIsNotContained() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(HEPATITIS_B, PatientSeriesStatus.CONTRAINDICATED, date("01/01/2030"));
    bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("06/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("09/01/2024"));

    VaccineGroupForecast vaccineGroupForecast = theVaccineGroupForecast();
    assertEquals("the HepB series' Contraindicated status is not MMR's", VaccineGroupStatus.NOT_COMPLETE,
        vaccineGroupForecast.getVaccineGroupStatus());
    assertEquals("nor does its later earliest date reach MULTIANTVG-1", date("09/01/2024"),
        vaccineGroupForecast.getEarliestDate());
    assertFalse("nor is Hepatitis B one of the group forecast's antigens",
        vaccineGroupForecast.getAntigenList().contains(dataModel.getOrCreateAntigen(HEPATITIS_B)));
  }

  /**
   * A vaccine group forecast is made <i>for a vaccine group</i>, and 4.6 runs
   * 9.3 once per vaccine group - so one run produces exactly one, however many
   * patient series forecasts it contains. Both of Table 9-5's rules and all of
   * Table 9-4 are phrased over "the vaccine group forecast", singular.
   */
  @Test
  public void oneRunProducesOneVaccineGroupForecastForTheVaccineGroup() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    bestPatientSeries(RUBELLA, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));

    runStep();

    assertEquals("three contained patient series forecasts, one vaccine group forecast", 1,
        dataModel.getVaccineGroupForecastList().size());
  }

  /**
   * Next Steps / {@code transitions.yaml}: "Unconditional return to section '9'"
   * - the chapter's vaccine group loop driver - from every one of Table 9-4's
   * six outcomes and from the step's own entry point. Checked on the Not
   * Complete path (which runs the date aggregation), the Complete path (which
   * does not) and the path where no best patient series matches the group at
   * all.
   */
  @Test
  public void theStepAlwaysReturnsToTheChapterDriver() throws Exception {
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    LogicStep afterNotComplete = new MultipleAntigenVaccineGroup(dataModel).process();
    assertEquals(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, afterNotComplete.getLogicStepType());
    assertTrue("9.3 must return to section 9's own class, got " + afterNotComplete.getClass().getName(),
        afterNotComplete instanceof IdentifyAndEvaluateVaccineGroup);

    setUp();
    mmrVaccineGroup();
    bestPatientSeries(MEASLES, PatientSeriesStatus.COMPLETE, date("01/01/2024"));
    bestPatientSeries(MUMPS, PatientSeriesStatus.COMPLETE, date("01/01/2024"));
    LogicStep afterComplete = new MultipleAntigenVaccineGroup(dataModel).process();
    assertEquals("... including from the outcomes that skip the date aggregation",
        LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, afterComplete.getLogicStepType());

    setUp();
    mmrVaccineGroup();
    bestPatientSeries(HEPATITIS_B, PatientSeriesStatus.NOT_COMPLETE, date("01/01/2024"));
    LogicStep afterNoMatch = new MultipleAntigenVaccineGroup(dataModel).process();
    assertEquals("... and when no best patient series belongs to the vaccine group at all",
        LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, afterNoMatch.getLogicStepType());
  }

  /**
   * Entry Conditions: 9.3 is reached from 9.1 only when VACCINEGROUP-2 has
   * classified the vaccine group as classifying more than one antigen, and every
   * rule of the section is written for that case. Pinned so the fixture's own
   * premise is checked rather than assumed.
   */
  @Test
  public void theVaccineGroupUnderTestClassifiesMoreThanOneAntigen() {
    VaccineGroup vaccineGroup = mmrVaccineGroup();
    assertTrue("VACCINEGROUP-2: 9.3 runs only for a vaccine group classifying more than one antigen; " + MMR
        + " classifies " + vaccineGroup.getAntigenList(), vaccineGroup.getAntigenList().size() > 1);
  }
}
