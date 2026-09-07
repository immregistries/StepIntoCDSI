package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * Section 9.1 "Apply General Vaccine Group Rules" (Logic Specification for ACIP
 * Recommendations v4.6, page 94; Table 9-2 "General Vaccine Group Business
 * Rules") as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/09-01-apply-general-vaccine-group-rules/index.md}.
 *
 * <p>
 * The section has two jobs by its own text: it "provides the business rules
 * which are applied to both types of vaccine groups (i.e., Single Antigen and
 * Multiple Antigen)" - FORECASTVG-1 through FORECASTVG-9 and FORECASTDN-2, the
 * rules for aggregating contained patient series forecasts into one vaccine
 * group forecast - and "finally, this table provides rules to classify the
 * vaccine group type (Single Antigen or Multiple Antigen) for subsequent
 * business rule sections (9.2 or 9.3)" - VACCINEGROUP-1 and VACCINEGROUP-2.
 *
 * <p>
 * <b>What this class actually implements, and therefore what is tested here.</b>
 * {@code ApplyGeneralVaccineGroupRules} implements the classification and
 * nothing else. Its one {@code LogicTable} asks exactly one question ("Does the
 * vaccine group contain exactly 1 antigen?") and branches to 9.2 or 9.3. The
 * rule IDs FORECASTVG-1..9 and FORECASTDN-2 appear nowhere in
 * {@code cdsi-engine} or {@code cdsi-web} (verified by grep); the behaviour most
 * of them describe runs in the two branch classes instead, under their own
 * labels - {@code SingleAntigenVaccineGroup} copies the one contained forecast's
 * fields as {@code SINGLEANTVG-1} through {@code SINGLEANTVG-10}, and
 * {@code MultipleAntigenVaccineGroup} merges several as
 * {@code MULTIANTVG_1()} through {@code MULTIANTVG_8()}. Those are 9.2's and
 * 9.3's units and are tested there, not here.
 *
 * <p>
 * Two of Table 9-2's rules land in <i>no</i> class at all, and those are in
 * scope here because 9.1 is the section that owns them: <b>FORECASTVG-9</b>
 * (recommended series dose vaccines for the vaccine group forecast) and
 * <b>FORECASTDN-2</b> (the vaccine group forecast's dose number, min or max of
 * the contained dose numbers depending on the administer full vaccine group
 * flag). The last two tests below ask only whether the domain model can carry
 * those two outputs at all - the same "can the rule even be expressed?" shape
 * used in 6.2, 7.1, 7.6 and 8.8 - and both are red. See the 2026-09-07 entry in
 * {@code cdsi-reference/step-tests/cross-cutting-notes.md}.
 *
 * <p>
 * <b>Isolation.</b> {@code process()} is exactly
 * {@code evaluateLogicTables(); return next();}, so most tests call
 * {@code evaluateLogicTables()} directly (it is {@code protected} and this test
 * is in the same package) and read {@code getNextLogicStepType()}. That runs the
 * whole of 9.1's own logic without constructing 9.2's or 9.3's class, which
 * matters because both of those read state 9.1 never touches - in particular
 * {@code MultipleAntigenVaccineGroup}'s constructor walks
 * {@code dataModel.getBestPatientSeriesList()}, which defaults to {@code null}.
 * Two tests do drive the public {@code process()}, to pin that the handoff
 * really reaches the right class. The fixture is hand-built throughout except
 * for two tests that load a bundled CDC Supporting Data release, in the manner
 * of {@link IdentifyAndEvaluateVaccineGroupTest}.
 */
public class ApplyGeneralVaccineGroupRulesTest {

  /** The two multiple antigen vaccine groups in the bundled release. */
  private static final String MMR = "MMR";
  private static final String DTAP = "DTaP/Tdap/Td";
  private static final String HEPB = "HepB";

  /** Loaded once - {@code createDataModel} parses the whole release. */
  private static DataModel bundledRelease = null;

  private DataModel dataModel;

  @Before
  public void setUp() {
    dataModel = new DataModel();
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the minimal shape 9.1 actually reads, which is one
  // vaccine group made current by 4.6 and its antigen list.
  // ---------------------------------------------------------------------

  /**
   * Registers a vaccine group the way {@code DataModelLoader} does, gives it the
   * antigens {@code readVaccineGroupToAntigenMap} would have mapped onto it, and
   * makes it the current vaccine group - the state 4.6
   * {@code IdentifyAndEvaluateVaccineGroup} leaves behind when it delegates to
   * 9.1.
   */
  private VaccineGroup currentVaccineGroup(String vaccineGroupName, String... antigenNames) {
    VaccineGroup vaccineGroup = dataModel.getOrCreateVaccineGroup(vaccineGroupName);
    for (String antigenName : antigenNames) {
      vaccineGroup.getAntigenList().add(dataModel.getOrCreateAntigen(antigenName));
    }
    dataModel.setVaccineGroup(vaccineGroup);
    return vaccineGroup;
  }

  /** Stands in for Chapter 8's output, which 9.2 and 9.3 consume and 9.1 does not. */
  private PatientSeries bestPatientSeries(String seriesName, String antigenName) {
    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setTargetDisease(dataModel.getOrCreateAntigen(antigenName));
    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    if (dataModel.getBestPatientSeriesList() == null) {
      dataModel.setBestPatientSeriesList(new ArrayList<PatientSeries>());
    }
    dataModel.getBestPatientSeriesList().add(patientSeries);
    return patientSeries;
  }

  // ---------------------------------------------------------------------
  // Driving the step.
  // ---------------------------------------------------------------------

  /**
   * Runs 9.1's own logic - its one decision table - and reports the section it
   * classified the current vaccine group into.
   */
  private LogicStepType classify() {
    return classifiedBy(new ApplyGeneralVaccineGroupRules(dataModel));
  }

  private LogicStepType classifiedBy(ApplyGeneralVaccineGroupRules step) {
    step.evaluateLogicTables();
    return step.getNextLogicStepType();
  }

  /** Runs the whole step, including the handoff to the branch class. */
  private LogicStep processWholeStep() throws Exception {
    return new ApplyGeneralVaccineGroupRules(dataModel).process();
  }

  private static LogicTable tableNineTwoOf(ApplyGeneralVaccineGroupRules step) {
    assertEquals("9.1 makes exactly one decision, so it registers exactly one logic table",
        1, step.getLogicTableList().size());
    return step.getLogicTableList().get(0);
  }

  /** The messages one outcome of the table recorded when (and only if) it fired. */
  private static List<String> outcomeLog(LogicTable logicTable, int outcomePosition) {
    LogicOutcome outcome = logicTable.getLogicOutcomes()[outcomePosition];
    return outcome.getLogList();
  }

  /**
   * The first accessor of {@code type} whose name matches {@code namePattern},
   * or null - the "can the rule even be expressed?" probe used by 7.5's
   * FORECASTDN-1/FORECASTRECVAC-1 tests and by 8.8's equivalent series group
   * test.
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

  /** The bundled CDC Supporting Data release, loaded once for the whole class. */
  private static DataModel bundledRelease() throws Exception {
    if (bundledRelease == null) {
      List<String> zipNames = DataModelLoader.listBundledSupportingDataZipNames();
      String supportingDataSet = null;
      for (String zipName : zipNames) {
        if (zipName.startsWith("supporting-data-")) {
          supportingDataSet = zipName;
        }
      }
      assertNotNull("No bundled CDC supporting data release found, names were: " + zipNames, supportingDataSet);
      bundledRelease = DataModelLoader.createDataModel(supportingDataSet);
    }
    return bundledRelease;
  }

  // ---------------------------------------------------------------------
  // VACCINEGROUP-1 - "A vaccine group must be considered a single antigen
  // vaccine group if it classifies exactly one antigen."
  // ---------------------------------------------------------------------

  /**
   * VACCINEGROUP-1 directly: a vaccine group classifying exactly one antigen is
   * a single antigen vaccine group, which the Purpose says means "subsequent
   * business rule section" 9.2.
   */
  @Test
  public void vaccinegroupOneAGroupThatClassifiesExactlyOneAntigenIsASingleAntigenVaccineGroup() {
    currentVaccineGroup(HEPB, "Hepatitis B");

    assertEquals("VACCINEGROUP-1: exactly one classified antigen makes this a single antigen vaccine group,"
        + " so 9.2's rules apply", LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP, classify());
  }

  /**
   * VACCINEGROUP-1 counts the antigens the vaccine group <i>classifies</i>, not
   * the vaccines it contains. A group such as HepB lists many vaccine products
   * (and in the bundled release every group does) while classifying one antigen;
   * that is still a single antigen vaccine group.
   */
  @Test
  public void vaccinegroupOneTheCountIsOfAntigensClassifiedNotOfVaccinesInTheGroup() {
    VaccineGroup vaccineGroup = currentVaccineGroup(HEPB, "Hepatitis B");
    vaccineGroup.getVaccineList().add(new Vaccine());
    vaccineGroup.getVaccineList().add(new Vaccine());
    vaccineGroup.getVaccineList().add(new Vaccine());

    assertEquals("VACCINEGROUP-1 is about classified antigens; three vaccine products of one antigen do not"
        + " make a multiple antigen vaccine group", LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP, classify());
  }

  /**
   * The classification is a control-flow decision, so the handoff has to reach
   * the class that implements 9.2's rules, not merely name the step type.
   */
  @Test
  public void vaccinegroupOneTheSingleAntigenBranchHandsControlToSectionNineTwo() throws Exception {
    currentVaccineGroup(HEPB, "Hepatitis B");

    LogicStep next = processWholeStep();

    assertEquals(LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP, next.getLogicStepType());
    assertTrue("9.1 must hand a single antigen vaccine group to 9.2's own class, got "
        + next.getClass().getName(), next instanceof SingleAntigenVaccineGroup);
  }

  // ---------------------------------------------------------------------
  // VACCINEGROUP-2 - "A vaccine group must be considered a multiple antigen
  // vaccine group if it classifies more than one antigen."
  // ---------------------------------------------------------------------

  /**
   * VACCINEGROUP-2 directly, at its boundary (two antigens) and at the shape the
   * bundled release actually has (three - MMR classifies Measles, Mumps and
   * Rubella; DTaP/Tdap/Td classifies Diphtheria, Pertussis and Tetanus).
   */
  @Test
  public void vaccinegroupTwoAGroupThatClassifiesMoreThanOneAntigenIsAMultipleAntigenVaccineGroup() {
    currentVaccineGroup("Two antigen group", "Measles", "Rubella");
    assertEquals("VACCINEGROUP-2: two classified antigens is 'more than one', so 9.3's rules apply",
        LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, classify());

    setUp();
    currentVaccineGroup(MMR, "Measles", "Mumps", "Rubella");
    assertEquals("VACCINEGROUP-2: MMR classifies three antigens, so 9.3's rules apply",
        LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, classify());
  }

  /** The other half of the handoff: a multiple antigen group reaches 9.3's class. */
  @Test
  public void vaccinegroupTwoTheMultipleAntigenBranchHandsControlToSectionNineThree() throws Exception {
    currentVaccineGroup(MMR, "Measles", "Mumps", "Rubella");
    // 9.3's constructor walks Chapter 8's output; 8.8 has always run by now.
    dataModel.setBestPatientSeriesList(new ArrayList<PatientSeries>());

    LogicStep next = processWholeStep();

    assertEquals(LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, next.getLogicStepType());
    assertTrue("9.1 must hand a multiple antigen vaccine group to 9.3's own class, got "
        + next.getClass().getName(), next instanceof MultipleAntigenVaccineGroup);
  }

  // ---------------------------------------------------------------------
  // The two rules together - the decision 9.1 exists to make.
  // ---------------------------------------------------------------------

  /**
   * Entry Conditions: 9.1 "runs once per vaccine group, entered from the chapter
   * driver (4.6) after ... a vaccine group's antigens" have been selected. The
   * classification must therefore be remade for whichever group 4.6 has made
   * current, not decided once for the run: the same fixture yields 9.3 for MMR
   * and 9.2 for HepB.
   */
  @Test
  public void theClassificationIsRemadeForEachVaccineGroupTheChapterDriverMakesCurrent() {
    VaccineGroup mmr = currentVaccineGroup(MMR, "Measles", "Mumps", "Rubella");
    VaccineGroup hepB = dataModel.getOrCreateVaccineGroup(HEPB);
    hepB.getAntigenList().add(dataModel.getOrCreateAntigen("Hepatitis B"));

    dataModel.setVaccineGroup(mmr);
    assertEquals("MMR classifies three antigens", LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, classify());

    dataModel.setVaccineGroup(hepB);
    assertEquals("HepB classifies one antigen, and 9.1 must classify it on its own merits",
        LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP, classify());
  }

  /**
   * VACCINEGROUP-1 and VACCINEGROUP-2 are mutually exclusive, so exactly one of
   * the table's two outcomes may fire for a given vaccine group. Worth pinning
   * explicitly here because {@code LogicTable.evaluate()} performs <i>every</i>
   * validating column rather than stopping at the first - see the 2026-09-04
   * cross-cutting entry - so "only one column can match" is a property of this
   * table's conditions, not something the engine enforces.
   */
  @Test
  public void exactlyOneOfTableNineTwosTwoClassificationsIsMade() {
    currentVaccineGroup(HEPB, "Hepatitis B");
    ApplyGeneralVaccineGroupRules singleAntigenStep = new ApplyGeneralVaccineGroupRules(dataModel);
    LogicTable singleAntigenTable = tableNineTwoOf(singleAntigenStep);
    classifiedBy(singleAntigenStep);

    assertEquals("the single antigen outcome fires for a one-antigen group", 1,
        outcomeLog(singleAntigenTable, 0).size());
    assertEquals("and the multiple antigen outcome must not also fire", 0,
        outcomeLog(singleAntigenTable, 1).size());

    setUp();
    currentVaccineGroup(MMR, "Measles", "Mumps", "Rubella");
    ApplyGeneralVaccineGroupRules multipleAntigenStep = new ApplyGeneralVaccineGroupRules(dataModel);
    LogicTable multipleAntigenTable = tableNineTwoOf(multipleAntigenStep);
    classifiedBy(multipleAntigenStep);

    assertEquals("the single antigen outcome must not fire for a three-antigen group", 0,
        outcomeLog(multipleAntigenTable, 0).size());
    assertEquals("the multiple antigen outcome fires", 1, outcomeLog(multipleAntigenTable, 1).size());
  }

  /**
   * A vaccine group that classifies no antigen at all is matched by neither
   * VACCINEGROUP-1 ("exactly one") nor VACCINEGROUP-2 ("more than one"), and the
   * specification says nothing else about it. Pinned as behaviour rather than
   * asserted as a rule: the implementation's single question is "exactly 1?", so
   * a No sends the empty group down the multiple antigen branch.
   *
   * <p>
   * Not reachable from the bundled release - all 26 of its vaccine groups map to
   * at least one antigen - but reachable in principle, since 4.6 hands over every
   * vaccine group without checking (see
   * {@code IdentifyAndEvaluateVaccineGroupTest.vaccineGroupWithNoAntigensIsStillHandedToChapterNine}).
   * Recorded here so the gap in Table 9-2's coverage is visible; 9.2's class
   * carries an explicit "primary antigen missing" guard, which suggests its
   * author expected the empty group to arrive there instead.
   */
  @Test
  public void aVaccineGroupThatClassifiesNoAntigenIsMatchedByNeitherRule() {
    currentVaccineGroup("Empty group");

    assertEquals("Actual behaviour, pinned: neither VACCINEGROUP-1 nor VACCINEGROUP-2 covers a group with no"
        + " classified antigen, and 9.1's one question sends it to 9.3",
        LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, classify());
  }

  // ---------------------------------------------------------------------
  // The shape of the decision, and its citation of Table 9-2.
  // ---------------------------------------------------------------------

  /**
   * Table 9-2 is prose-style business rules rather than a Yes/No grid, so the
   * one-condition/two-outcome table is a code-side structuring choice - but it
   * is the structure VACCINEGROUP-1/2 imply: one question about the antigen
   * count, two mutually exclusive classifications.
   */
  @Test
  public void theStepAsksOneQuestionAndOffersTwoOutcomes() {
    currentVaccineGroup(HEPB, "Hepatitis B");
    LogicTable logicTable = tableNineTwoOf(new ApplyGeneralVaccineGroupRules(dataModel));

    assertEquals("VACCINEGROUP-1/2 turn on one question - how many antigens does the group classify?",
        1, logicTable.getLogicConditions().length);
    assertEquals("... with exactly two classifications available", 2, logicTable.getLogicOutcomes().length);
    assertNotNull("the question must be asked", logicTable.getLogicConditions()[0]);
    assertNotNull("the single antigen classification must exist", logicTable.getLogicOutcomes()[0]);
    assertNotNull("the multiple antigen classification must exist", logicTable.getLogicOutcomes()[1]);
  }

  /**
   * The decision table names the specification table it implements - it is
   * rendered as the heading of the step's table in the {@code cdsi-web} step
   * viewer ({@code LogicStepRenderer}, line 347), so it is the citation a reader
   * follows back to the specification. 9.1's table is drawn from Table 9-2 of
   * v4.6.
   *
   * <p>
   * The label reads "TABLE 7 - 2 WHAT IS THE VACCINE GROUP TYPE?" - a leftover
   * from an earlier chapter numbering, pointing a reader at Chapter 7's Table
   * 7-2 (Evaluate Conditional Skip attributes) instead. Cosmetic in the sense
   * that it changes no forecast, but wrong in the sense that it is the only
   * specification reference this step displays.
   */
  @Test
  public void theDecisionTableCitesTableNineTwo() {
    currentVaccineGroup(HEPB, "Hepatitis B");
    String label = tableNineTwoOf(new ApplyGeneralVaccineGroupRules(dataModel)).getLabel();

    assertTrue("9.1's decision comes from Table 9-2 'General Vaccine Group Business Rules', so the label the"
        + " step viewer displays must cite that table; it reads \"" + label + "\"",
        label.matches("(?i).*\\b9\\s*-\\s*2\\b.*"));
  }

  /**
   * Pinned as behaviour, not asserted as a rule: 9.1's constructor never calls
   * {@code setLogicStepSink} on its logic table, unlike both of the classes it
   * branches to, so the classification message the outcome records never reaches
   * the step's own log. The decision is still made correctly - this is only
   * about what the step viewer shows for 9.1.
   */
  @Test
  public void theClassificationIsRecordedOnTheOutcomeRatherThanOnTheStepsOwnLog() {
    currentVaccineGroup(HEPB, "Hepatitis B");
    ApplyGeneralVaccineGroupRules step = new ApplyGeneralVaccineGroupRules(dataModel);
    LogicTable logicTable = tableNineTwoOf(step);

    classifiedBy(step);

    assertEquals("the outcome itself records the classification", 1, outcomeLog(logicTable, 0).size());
    assertTrue("Actual behaviour: 9.1 does not propagate its log sink to its table, so the step's own log is"
        + " empty; 9.2 and 9.3 both do propagate. Step log was: " + step.getLogList(),
        step.getLogList().isEmpty());
  }

  // ---------------------------------------------------------------------
  // What 9.1 does not do - the aggregation half of Table 9-2 is not here.
  // ---------------------------------------------------------------------

  /**
   * FORECASTVG-2 through FORECASTVG-9 all describe fields of a vaccine group
   * forecast, and FORECASTVG-1 describes which patient series forecasts it
   * contains - so if any of them ran here, a vaccine group forecast would exist
   * after 9.1. None does: 9.1 classifies and hands over, and the forecast is
   * built by 9.2 or 9.3. Pinned so that this unit's coverage claim is explicit
   * rather than implied by absence.
   */
  @Test
  public void theStepCreatesNoVaccineGroupForecastAndChangesNoOtherState() {
    VaccineGroup vaccineGroup = currentVaccineGroup(MMR, "Measles", "Mumps", "Rubella");
    bestPatientSeries("MMR standard", "Measles");
    bestPatientSeries("Mumps standard", "Mumps");
    int antigensBefore = vaccineGroup.getAntigenList().size();
    int vaccineGroupPosBefore = dataModel.getVaccineGroupPos();

    classify();

    assertTrue("aggregating contained forecasts (FORECASTVG-1..9, FORECASTDN-2) is not done here - the"
        + " vaccine group forecast is built by 9.2/9.3", dataModel.getVaccineGroupForecastList().isEmpty());
    assertNull("nor is a forecast attached to the vaccine group itself", vaccineGroup.getVaccineGroupForecast());
    assertSame("the classified vaccine group stays current for the branch class", vaccineGroup,
        dataModel.getVaccineGroup());
    assertEquals("9.1 does not add to or remove from the group's antigen list", antigensBefore,
        vaccineGroup.getAntigenList().size());
    assertEquals("advancing the vaccine group loop is 4.6's job", vaccineGroupPosBefore,
        dataModel.getVaccineGroupPos());
    assertEquals("Chapter 8's output is passed through untouched", 2,
        dataModel.getBestPatientSeriesList().size());
  }

  /**
   * VACCINEGROUP-1/2 are properties of the vaccine group, not of the patient, so
   * the classification cannot depend on what Chapter 8 selected. Confirmed from
   * three directions: no best patient series list at all (its default state -
   * 9.1 must not dereference it, which is also why the isolation used here does
   * not construct 9.3's class), an empty one, and one holding a series for an
   * antigen the group does not classify.
   */
  @Test
  public void theClassificationDoesNotDependOnChapterEightsBestPatientSeries() {
    currentVaccineGroup(MMR, "Measles", "Mumps", "Rubella");
    assertNull("Precondition: Chapter 8's output starts out unset", dataModel.getBestPatientSeriesList());
    assertEquals("MMR is a multiple antigen vaccine group with no best patient series list at all",
        LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, classify());

    dataModel.setBestPatientSeriesList(new ArrayList<PatientSeries>());
    assertEquals("... and with an empty one", LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, classify());

    bestPatientSeries("HepB standard", "Hepatitis B");
    assertEquals("... and with one holding a series for an antigen MMR does not classify",
        LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, classify());
  }

  // ---------------------------------------------------------------------
  // Against a bundled CDC Supporting Data release.
  // ---------------------------------------------------------------------

  /**
   * VACCINEGROUP-1/2 applied to a real schedule. Every vaccine group the release
   * defines is classified by the number of antigens the release maps to it, and
   * both branches are genuinely exercised: the release's two multiple antigen
   * vaccine groups are MMR and DTaP/Tdap/Td - the two the specification's own
   * 4.6 Purpose calls out by name - and every other group classifies exactly one
   * antigen.
   */
  @Test
  public void everyVaccineGroupInABundledReleaseIsClassifiedByItsAntigenCount() throws Exception {
    dataModel = bundledRelease();
    Set<String> classifiedMultiple = new LinkedHashSet<String>();
    int classifiedSingle = 0;

    for (VaccineGroup vaccineGroup : dataModel.getVaccineGroupList()) {
      dataModel.setVaccineGroup(vaccineGroup);
      LogicStepType section = classify();
      if (vaccineGroup.getAntigenList().size() == 1) {
        assertEquals("VACCINEGROUP-1: " + vaccineGroup.getName() + " classifies one antigen",
            LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP, section);
        classifiedSingle++;
      } else {
        assertEquals("VACCINEGROUP-2: " + vaccineGroup.getName() + " classifies "
            + vaccineGroup.getAntigenList().size() + " antigens",
            LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, section);
        classifiedMultiple.add(vaccineGroup.getName());
      }
    }

    assertTrue("the release must exercise the single antigen branch", classifiedSingle > 0);
    assertEquals("the release's multiple antigen vaccine groups are MMR and DTaP/Tdap/Td",
        new LinkedHashSet<String>(java.util.Arrays.asList(MMR, DTAP)), classifiedMultiple);
  }

  /**
   * <b>FORECASTDN-2</b>, first half: the flag the rule switches on is real,
   * populated data. "The forecast dose number for a vaccine group forecast must
   * be ... the minimum of the forecast dose numbers of the patient series
   * forecasts contained in the vaccine group forecast if the administer full
   * vaccine group flag is 'Y' ... the maximum ... if [it] is 'N'."
   *
   * <p>
   * In the bundled release exactly two vaccine groups carry a populated
   * {@code <administerFullVaccineGroup>} element - MMR is 'Yes' and DTaP/Tdap/Td
   * is 'No' - and they are exactly the two groups VACCINEGROUP-2 classifies as
   * multiple antigen, i.e. the only groups for which a min-versus-max of several
   * contained dose numbers could differ. So both branches of FORECASTDN-2 have
   * live data behind them.
   */
  @Test
  public void forecastdnTwoTheAdministerFullVaccineGroupFlagIsPopulatedForTheMultipleAntigenGroups()
      throws Exception {
    dataModel = bundledRelease();

    assertEquals("MMR takes FORECASTDN-2's 'Y' branch (minimum of the contained dose numbers)",
        YesNo.YES, dataModel.getVaccineGroup(MMR).getAdministerFullVaccineGroup());
    assertEquals("DTaP/Tdap/Td takes FORECASTDN-2's 'N' branch (maximum of the contained dose numbers)",
        YesNo.NO, dataModel.getVaccineGroup(DTAP).getAdministerFullVaccineGroup());

    for (VaccineGroup vaccineGroup : dataModel.getVaccineGroupList()) {
      if (vaccineGroup.getAntigenList().size() == 1) {
        assertNull("a single antigen vaccine group declares no administer full vaccine group flag: "
            + vaccineGroup.getName(), vaccineGroup.getAdministerFullVaccineGroup());
      }
    }
  }

  // ---------------------------------------------------------------------
  // Table 9-2 rules that land in no class at all - can they even be expressed?
  // ---------------------------------------------------------------------

  /**
   * <b>FORECASTDN-2</b>, second half: whichever branch of the rule applies, its
   * output is "the forecast dose number for a vaccine group forecast". Nothing
   * in the domain model can hold one - {@code VaccineGroupForecast} and the
   * {@code Forecast} it extends carry dates, a forecast reason, an antigen and a
   * target dose, but no dose number - so the rule has nowhere to land in 9.1,
   * 9.2 or 9.3.
   *
   * <p>
   * The patient series half of the same gap is already red in 7.5
   * ({@code forecastdnOneIsTheCountOfSatisfiedTargetDosesPlusOne}, FORECASTDN-1),
   * which is the number FORECASTDN-2 takes the minimum or maximum of; this is
   * the vaccine group end of it. See the 2026-09-07 cross-cutting entry.
   */
  @Test
  public void forecastdnTwoAVaccineGroupForecastCanCarryAForecastDoseNumber() {
    assertNotNull("FORECASTDN-2: a vaccine group forecast must carry a forecast dose number (the minimum or"
        + " maximum of its contained forecasts' dose numbers), but VaccineGroupForecast has no dose number"
        + " at all; its accessors are " + accessorNames(VaccineGroupForecast.class),
        accessorMatching(VaccineGroupForecast.class, "(?i)get.*dose.*number.*"));
  }

  /**
   * <b>FORECASTVG-9</b>: "A series dose vaccine must be considered a recommended
   * series dose vaccine for a vaccine group forecast if the series dose vaccine
   * is a recommended series dose vaccine for a patient series forecast contained
   * in the vaccine group forecast." The union it describes has nowhere to be
   * stored: {@code VaccineGroupForecast} exposes no recommended-vaccine list, and
   * neither does the {@code Forecast} it would take the union of - 7.5's
   * FORECASTRECVAC-1 test is red for the same missing field on the patient series
   * side.
   *
   * <p>
   * This is the one rule of Table 9-2 that is implemented in neither 9.2 nor 9.3:
   * {@code SingleAntigenVaccineGroup}'s own {@code SINGLEANTVG-10} comment
   * ("recommended vaccines ... must be the best patient series forecast
   * recommended vaccines") has no statement under it.
   */
  @Test
  public void forecastvgNineAVaccineGroupForecastCanCarryItsRecommendedSeriesDoseVaccines() {
    assertNotNull("FORECASTVG-9: a vaccine group forecast must carry the recommended series dose vaccines of"
        + " its contained patient series forecasts, but VaccineGroupForecast has no recommended-vaccine"
        + " list; its accessors are " + accessorNames(VaccineGroupForecast.class),
        accessorMatching(VaccineGroupForecast.class,
            "(?i)get.*recommend\\w*.*vaccine.*|get.*vaccine.*recommend\\w*.*"));
  }
}
