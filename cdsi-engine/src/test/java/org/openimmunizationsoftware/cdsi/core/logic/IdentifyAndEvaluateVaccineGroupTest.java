package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;

/**
 * Section 4.6 "Identify and Evaluate Vaccine Group" (Logic Specification for
 * ACIP Recommendations v4.6, pages 39-40, Figure 4-8 "Identify and Evaluate
 * Vaccine Group Process Model") as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/04-06-identify-and-evaluate-vaccine-group/index.md}.
 *
 * <p>
 * 4.6 defines <i>no</i> inputs, <i>no</i> business rules and <i>no</i> decision
 * tables of its own, and says so outright: "The process of identifying and
 * evaluating a vaccine group at the highest level is a simple iterative process
 * which loops through each vaccine group and applies the business rules defined
 * in Chapter 9 to each vaccine group." Every rule that actually merges
 * antigen-level forecasts into a vaccine-group forecast lives in Chapter 9 -
 * 9.1 Apply General Vaccine Group Rules (Table 9-2), 9.2 Single Antigen Vaccine
 * Group, 9.3 Multiple Antigen Vaccine Group (Table 9-4). This is the same shape
 * as 4.3 and 4.5: the class named for the chapter-4 summary is really the outer
 * loop, and here it is the outermost loop of the whole engine - its exit is the
 * end of the run rather than a handoff to another loop.
 *
 * <p>
 * What is testable at this level is therefore Figure 4-8's looping structure -
 * get the first vaccine group, apply Chapter 9's rules, "is there another
 * vaccine group to process?", Yes gets the next group and re-enters Chapter 9,
 * No ends the process - plus the step package's documented State Changes
 * ("advances {@code dataModel.vaccineGroupPos}; while vaccine groups remain,
 * sets the current vaccine group and delegates to 9.1; once exhausted, ends the
 * entire forecast") and the negative claims implied by the section: 4.6 merges
 * nothing, blends nothing and classifies nothing itself.
 *
 * <p>
 * The step is driven directly through its public {@code process()}, with a
 * hand-built {@code DataModel}: {@link IdentifyAndEvaluateVaccineGroup} reads
 * only {@code vaccineGroupPos} and the vaccine group list, so no Supporting
 * Data release is needed for the loop mechanics. Constructing the returned next
 * step is inert - {@code ApplyGeneralVaccineGroupRules}' constructor only builds
 * its Table 9-2 logic table (nothing is evaluated until {@code process()}, which
 * is never called here) and {@code End}'s constructor does nothing at all. That
 * is what keeps this a 4.6 test rather than a 9.1 test.
 *
 * <p>
 * A fresh {@code IdentifyAndEvaluateVaccineGroup} is constructed per iteration,
 * exactly as the real pipeline does: 4.5 {@code SelectBestPatientSeries} enters
 * 4.6 once when its antigen loop is exhausted, and both
 * {@code SingleAntigenVaccineGroup} (9.2) and {@code MultipleAntigenVaccineGroup}
 * (9.3) return a brand-new instance of this step each time they finish a group.
 *
 * <p>
 * <b>Scoping note:</b> Chapter 9's own classes are out of scope here. Nothing
 * below invokes any Chapter 9 {@code process()}; where a test needs to represent
 * "Chapter 9 ran and produced something", it writes to the shared
 * {@code DataModel} lists by hand, the way 9.2/9.3 would.
 */
public class IdentifyAndEvaluateVaccineGroupTest {

  private static final String HEPB = "HepB";
  private static final String MMR = "MMR";
  private static final String DTAP = "DTaP/Tdap/Td";
  private static final String POLIO = "Polio";

  private DataModel dataModel;

  @Before
  public void setUp() {
    dataModel = new DataModel();
  }

  /**
   * Registers vaccine groups the way {@code DataModelLoader} does - through the
   * vaccine group map - and then pins the list order explicitly so the
   * assertions below are about 4.6's iteration and not about
   * {@code HashMap.values()}' ordering. Both reach the step through the same
   * {@code DataModel.vaccineGroupList} field.
   */
  private List<VaccineGroup> vaccineGroups(String... vaccineGroupNames) {
    List<VaccineGroup> groups = new ArrayList<VaccineGroup>();
    for (String vaccineGroupName : vaccineGroupNames) {
      groups.add(dataModel.getOrCreateVaccineGroup(vaccineGroupName));
    }
    dataModel.setVaccineGroupList(groups);
    return groups;
  }

  /** Gives a registered vaccine group the antigens 9.1's Table 9-2 counts. */
  private VaccineGroup withAntigens(VaccineGroup vaccineGroup, String... antigenNames) {
    for (String antigenName : antigenNames) {
      vaccineGroup.getAntigenList().add(dataModel.getOrCreateAntigen(antigenName));
    }
    return vaccineGroup;
  }

  /**
   * Stands in for Chapter 8's output, which 4.6 hands on to Chapter 9 untouched:
   * a best patient series for one antigen.
   */
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

  private LogicStep process() throws Exception {
    return new IdentifyAndEvaluateVaccineGroup(dataModel).process();
  }

  private static List<String> namesOf(List<VaccineGroup> vaccineGroupList) {
    List<String> names = new ArrayList<String>();
    for (VaccineGroup vaccineGroup : vaccineGroupList) {
      names.add(vaccineGroup == null ? null : vaccineGroup.getName());
    }
    return names;
  }

  /**
   * Drives Figure 4-8's loop the way the pipeline does - one {@code process()}
   * per iteration, a fresh step instance each time, as if 9.2/9.3 had handed
   * control back at the "is there another vaccine group to process?" decision -
   * and returns every vaccine group Chapter 9 would have been pointed at, in
   * order.
   */
  private List<VaccineGroup> runLoopToCompletion() throws Exception {
    List<VaccineGroup> visited = new ArrayList<VaccineGroup>();
    int maximumIterations = dataModel.getVaccineGroupList().size() + 2;
    for (int i = 0; i <= maximumIterations; i++) {
      LogicStep next = process();
      if (next.getLogicStepType() == LogicStepType.END) {
        return visited;
      }
      assertEquals("While vaccine groups remain, 4.6 delegates to Chapter 9 (9.1) and nowhere else",
          LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, next.getLogicStepType());
      visited.add(dataModel.getVaccineGroup());
    }
    throw new AssertionError("4.6's loop did not terminate after " + maximumIterations + " iterations");
  }

  // ---------------------------------------------------------------------------
  // Figure 4-8 - the looping structure
  // ---------------------------------------------------------------------------

  /**
   * Figure 4-8: get the first vaccine group, then straight into the Chapter 9
   * rules. The first call must point Chapter 9 at the first vaccine group and
   * transition to 9.1.
   */
  @Test
  public void firstCallSelectsTheFirstVaccineGroupAndDelegatesToApplyGeneralVaccineGroupRules() throws Exception {
    List<VaccineGroup> groups = vaccineGroups(HEPB, MMR, POLIO);
    assertNull("Precondition: no vaccine group is current before 4.6 runs", dataModel.getVaccineGroup());
    assertEquals("Precondition: the loop has not started", -1, dataModel.getVaccineGroupPos());

    LogicStep next = process();

    assertEquals("The loop starts at the first vaccine group", 0, dataModel.getVaccineGroupPos());
    assertSame("Chapter 9 must be pointed at the first vaccine group", groups.get(0), dataModel.getVaccineGroup());
    assertEquals(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, next.getLogicStepType());
  }

  /**
   * Figure 4-8's "get next vaccine group" on the Yes branch. State Changes:
   * "advances {@code dataModel.vaccineGroupPos}". Each re-entry from Chapter 9
   * moves exactly one vaccine group forward - no skipping, no repeating.
   */
  @Test
  public void eachSubsequentCallAdvancesToTheNextVaccineGroup() throws Exception {
    List<VaccineGroup> groups = vaccineGroups(HEPB, MMR, POLIO);

    process();
    assertSame(groups.get(0), dataModel.getVaccineGroup());
    process();
    assertEquals(1, dataModel.getVaccineGroupPos());
    assertSame(groups.get(1), dataModel.getVaccineGroup());
    process();
    assertEquals(2, dataModel.getVaccineGroupPos());
    assertSame(groups.get(2), dataModel.getVaccineGroup());
  }

  /**
   * Figure 4-8's loop as a whole: "loops through each vaccine group and applies
   * the business rules defined in Chapter 9 to each vaccine group". Every
   * vaccine group gets exactly one turn at Chapter 9 - none skipped, none
   * visited twice - and then the loop ends.
   */
  @Test
  public void everyVaccineGroupIsHandedToChapterNineExactlyOnce() throws Exception {
    List<VaccineGroup> groups = vaccineGroups(HEPB, MMR, DTAP, POLIO);

    List<VaccineGroup> visited = runLoopToCompletion();

    assertEquals("One delegation to Chapter 9 per vaccine group, no more and no fewer", 4, visited.size());
    assertEquals("The loop walks the vaccine group list in its own order", groups, visited);
  }

  /**
   * Figure 4-8: the "No" branch of "is there another vaccine group to process?"
   * ends the process. Next Steps: 4.6 "transitions to END once every vaccine
   * group has been processed - this is the final step of the entire engine run."
   */
  @Test
  public void transitionsToEndOnceEveryVaccineGroupIsProcessed() throws Exception {
    vaccineGroups(HEPB, MMR);

    assertEquals(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, process().getLogicStepType());
    assertEquals(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, process().getLogicStepType());
    assertEquals("After the last vaccine group the loop ends the run",
        LogicStepType.END, process().getLogicStepType());
  }

  /**
   * State Changes: "once exhausted, ends the entire forecast
   * ({@code LogicStepType.END})". The step 4.6 hands control to must actually
   * terminate the run rather than continue it - {@code End.process()} returns
   * null, which is how every driver of the step chain stops.
   */
  @Test
  public void theEndStepReturnedAtTheEndOfTheLoopTerminatesTheRun() throws Exception {
    vaccineGroups(HEPB);
    process();

    LogicStep end = process();

    assertEquals(LogicStepType.END, end.getLogicStepType());
    assertNull("END must terminate the run, not hand off to a further step", end.process());
  }

  /**
   * Degenerate case of "loops through each vaccine group": with no vaccine group
   * to process there is nothing for Chapter 9 to merge, so the step ends the run
   * immediately without ever delegating. (Figure 4-8 does not describe this case
   * - it draws "get first vaccine group" as unconditional - so this pins the
   * implementation's reading of it.)
   */
  @Test
  public void noVaccineGroupsAtAllGoesStraightToEnd() throws Exception {
    vaccineGroups();

    LogicStep next = process();

    assertEquals(LogicStepType.END, next.getLogicStepType());
    assertNull("Nothing to process means no vaccine group is ever made current", dataModel.getVaccineGroup());
  }

  /**
   * The loop's exit is stable: an extra call after the loop has finished must
   * keep ending the run rather than wrapping around to the first vaccine group
   * or reading past the end of the list. 4.6 is re-entered from 9.2 and 9.3 on
   * every path they take, so a group whose Chapter 9 processing bounced back
   * one extra time must not restart the loop.
   */
  @Test
  public void callingAgainAfterTheLoopHasFinishedStillEndsTheRun() throws Exception {
    List<VaccineGroup> groups = vaccineGroups(HEPB, MMR);
    runLoopToCompletion();

    assertEquals(LogicStepType.END, process().getLogicStepType());
    assertEquals(LogicStepType.END, process().getLogicStepType());
    assertSame("The loop must not wrap around to the first vaccine group",
        groups.get(1), dataModel.getVaccineGroup());
  }

  // ---------------------------------------------------------------------------
  // What 4.6 delegates, and what it refuses to decide itself
  // ---------------------------------------------------------------------------

  /**
   * Purpose: merging matters "especially ... in MMR and DTaP/Tdap/Td vaccine
   * groups which each contain more than one antigen". 4.6 still treats a
   * multi-antigen group exactly like any other - it hands it to 9.1, which is
   * where Table 9-2's "does the vaccine group contain exactly 1 antigen?"
   * decides between 9.2 and 9.3. 4.6 must not short-circuit that decision.
   */
  @Test
  public void multiAntigenVaccineGroupsAreHandedToNineOneLikeAnyOtherGroup() throws Exception {
    List<VaccineGroup> groups = vaccineGroups(HEPB, MMR);
    withAntigens(groups.get(0), "Hepatitis B");
    withAntigens(groups.get(1), "Measles", "Mumps", "Rubella");

    assertEquals("A single-antigen group goes to 9.1, not straight to 9.2",
        LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, process().getLogicStepType());
    assertEquals("A multi-antigen group goes to 9.1 too, not straight to 9.3",
        LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, process().getLogicStepType());
    assertSame(groups.get(1), dataModel.getVaccineGroup());
  }

  /**
   * "Loops through each vaccine group" means every vaccine group, not only the
   * ones the patient has a best patient series for: a vaccine group with no
   * antigen at all still gets its turn at Chapter 9. 4.6 applies no filter of
   * its own - deciding what to do with a group that has nothing to merge is
   * Chapter 9's problem (9.2 raises ALERT.SPECGAP for it).
   */
  @Test
  public void vaccineGroupWithNoAntigensIsStillHandedToChapterNine() throws Exception {
    List<VaccineGroup> groups = vaccineGroups(HEPB);

    LogicStep next = process();

    assertEquals(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, next.getLogicStepType());
    assertSame(groups.get(0), dataModel.getVaccineGroup());
    assertTrue("4.6 does not require a group to have antigens before delegating",
        dataModel.getVaccineGroup().getAntigenList().isEmpty());
  }

  /**
   * The same claim from the patient's side: a vaccine group for which Chapter 8
   * selected no best patient series is still visited. 4.6 does not cross-check
   * the best patient series list before delegating, so every vaccine group in
   * the loaded schedule is offered to Chapter 9 whether or not this patient has
   * anything in it.
   */
  @Test
  public void vaccineGroupsWithNoBestPatientSeriesAreStillVisited() throws Exception {
    vaccineGroups(HEPB, POLIO);
    bestPatientSeries("HepB standard", "Hepatitis B");

    List<VaccineGroup> visited = runLoopToCompletion();

    assertEquals("Both groups are visited even though only one has a best patient series",
        Arrays.asList(HEPB, POLIO), namesOf(visited));
  }

  /**
   * Purpose: "The business rules to create vaccine group forecasts are defined
   * in Chapter 9." Driving 4.6's whole loop with best patient series present
   * must therefore produce no vaccine group forecast at all - creating them is
   * 9.2's and 9.3's job.
   */
  @Test
  public void stepCreatesNoVaccineGroupForecastsOfItsOwn() throws Exception {
    List<VaccineGroup> groups = vaccineGroups(HEPB, MMR);
    withAntigens(groups.get(0), "Hepatitis B");
    withAntigens(groups.get(1), "Measles", "Mumps", "Rubella");
    bestPatientSeries("HepB standard", "Hepatitis B");
    bestPatientSeries("MMR standard", "Measles");

    runLoopToCompletion();

    assertTrue("Merging antigen forecasts into a vaccine group forecast is Chapter 9's job, not 4.6's",
        dataModel.getVaccineGroupForecastList().isEmpty());
    assertNull("Attaching a forecast to the vaccine group is Chapter 9's job too",
        groups.get(1).getVaccineGroupForecast());
  }

  /**
   * The best patient series list is Chapter 8's output and Chapter 9's input.
   * 4.6 sits between them and must pass it through untouched - neither consuming
   * entries as it visits groups nor clearing it at the end of the loop, or the
   * last vaccine groups in the list would have nothing left to merge.
   */
  @Test
  public void bestPatientSeriesListIsPassedThroughToChapterNineUntouched() throws Exception {
    vaccineGroups(HEPB, MMR, POLIO);
    bestPatientSeries("HepB standard", "Hepatitis B");
    bestPatientSeries("MMR standard", "Measles");
    List<PatientSeries> beforeTheLoop = dataModel.getBestPatientSeriesList();

    runLoopToCompletion();

    assertSame("The list instance must survive the loop", beforeTheLoop, dataModel.getBestPatientSeriesList());
    assertEquals("Every best patient series must still be there for the last vaccine group",
        2, dataModel.getBestPatientSeriesList().size());
  }

  // ---------------------------------------------------------------------------
  // State Changes, pinned as they actually behave
  // ---------------------------------------------------------------------------

  /**
   * The step package's State Changes for 4.6 describe advancing the position,
   * setting the current vaccine group and ending the forecast - and, unlike
   * 4.5's, say nothing about clearing per-iteration state. Pinned as it behaves:
   * the last vaccine group processed is still current after the loop has ended,
   * because 4.6 is the final step of the run and nothing downstream needs it
   * cleared.
   */
  @Test
  public void theLastVaccineGroupRemainsCurrentAfterTheLoopEnds() throws Exception {
    List<VaccineGroup> groups = vaccineGroups(HEPB, MMR);

    runLoopToCompletion();

    assertSame("Actual behaviour: 4.6 clears no per-group state on the exhaustion branch",
        groups.get(1), dataModel.getVaccineGroup());
  }

  /**
   * The set of vaccine groups the loop will visit is fixed the first time 4.6
   * reads it, because {@code DataModel.getVaccineGroupList()} materializes the
   * vaccine group map once and caches the result. A vaccine group registered
   * after the loop has started is therefore never visited. This is benign in the
   * real pipeline - all Supporting Data is loaded in 4.1, long before 4.6 runs -
   * but it is what makes "loops through each vaccine group" a well-defined,
   * terminating loop rather than one that could be extended while running.
   */
  @Test
  public void theSetOfVaccineGroupsIsFixedWhenTheLoopStarts() throws Exception {
    dataModel.getOrCreateVaccineGroup(HEPB);
    dataModel.getOrCreateVaccineGroup(MMR);

    LogicStep firstIteration = process();
    assertEquals(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, firstIteration.getLogicStepType());
    dataModel.getOrCreateVaccineGroup(POLIO);

    assertEquals(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, process().getLogicStepType());
    assertEquals("A vaccine group registered after the loop started is not visited",
        LogicStepType.END, process().getLogicStepType());
  }

  /**
   * With no explicit list ever set, the loop iterates the vaccine group map that
   * {@code DataModelLoader} populates - which is the only way 4.6 is ever
   * reached in the real pipeline, since nothing in {@code cdsi-engine} calls
   * {@code setVaccineGroupList}. Every registered group is visited exactly once;
   * the order comes from {@code HashMap.values()} and is not asserted here
   * because neither section 4.6 nor Figure 4-8 states one.
   */
  @Test
  public void loopIteratesTheVaccineGroupMapWhenNoListWasExplicitlySet() throws Exception {
    dataModel.getOrCreateVaccineGroup(HEPB);
    dataModel.getOrCreateVaccineGroup(MMR);
    dataModel.getOrCreateVaccineGroup(DTAP);

    Set<String> visited = new LinkedHashSet<String>(namesOf(runLoopToCompletion()));

    assertEquals("Every registered vaccine group is visited exactly once",
        new LinkedHashSet<String>(Arrays.asList(HEPB, MMR, DTAP)), visited);
  }

  /**
   * The step guards against a null vaccine group list (treating it as size
   * zero), but that branch cannot actually be reached:
   * {@code DataModel.getVaccineGroupList()} re-materializes the list from the
   * vaccine group map whenever the field is null. Pinned so that the guard is
   * visibly dead code rather than silently assumed to be live - if
   * {@code DataModel}'s lazy initialization ever changes, this test says what
   * 4.6 was relying on.
   */
  @Test
  public void theNullVaccineGroupListGuardIsUnreachable() throws Exception {
    dataModel.getOrCreateVaccineGroup(HEPB);
    dataModel.setVaccineGroupList(null);

    assertNotNull("The data model re-materializes the list rather than handing 4.6 a null",
        dataModel.getVaccineGroupList());

    dataModel.setVaccineGroupList(null);
    LogicStep next = process();

    assertEquals("A nulled list still yields the registered vaccine groups, not an immediate END",
        LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, next.getLogicStepType());
    assertEquals(HEPB, dataModel.getVaccineGroup().getName());
  }

  // ---------------------------------------------------------------------------
  // Materiality against a bundled CDC Supporting Data release
  // ---------------------------------------------------------------------------

  /**
   * The loop mechanics above are hand-built; this one checks what they mean for
   * a real schedule. Against a bundled CDC Supporting Data release, 4.6 visits
   * every vaccine group the release defines - including the multi-antigen MMR
   * and DTaP/Tdap/Td groups the Purpose calls out by name - exactly once, and
   * then ends the run. It does so with no patient data loaded at all, which is
   * the concrete form of "4.6 applies no filter of its own": the number of
   * Chapter 9 invocations per run is a property of the schedule, not of the
   * patient.
   */
  @Test
  public void everyVaccineGroupInABundledSupportingDataReleaseIsVisitedExactlyOnce() throws Exception {
    List<String> zipNames = DataModelLoader.listBundledSupportingDataZipNames();
    String supportingDataSet = null;
    for (String zipName : zipNames) {
      if (zipName.startsWith("supporting-data-")) {
        supportingDataSet = zipName;
      }
    }
    assertNotNull("No bundled CDC supporting data release found, names were: " + zipNames, supportingDataSet);

    dataModel = DataModelLoader.createDataModel(supportingDataSet);
    Set<String> releaseVaccineGroupNames = new LinkedHashSet<String>(dataModel.getVaccineGroupMap().keySet());
    assertTrue("Precondition: " + supportingDataSet + " should define several vaccine groups",
        releaseVaccineGroupNames.size() > 1);

    List<VaccineGroup> visited = runLoopToCompletion();

    assertEquals("Chapter 9 is invoked once per vaccine group in the release",
        releaseVaccineGroupNames.size(), visited.size());
    assertEquals("Every vaccine group in the release is visited, and only those",
        releaseVaccineGroupNames, new LinkedHashSet<String>(namesOf(visited)));

    List<VaccineGroup> multiAntigenGroupsVisited = new ArrayList<VaccineGroup>();
    for (VaccineGroup vaccineGroup : visited) {
      if (vaccineGroup.getAntigenList().size() > 1) {
        multiAntigenGroupsVisited.add(vaccineGroup);
      }
    }
    assertTrue("The Purpose's multi-antigen groups (MMR, DTaP/Tdap/Td) must be among those visited,"
        + " visited groups were: " + namesOf(visited),
        multiAntigenGroupsVisited.size() > 0);
  }

  /**
   * The other half of the materiality check, and the reason 4.6 is worth pinning
   * at all: run against a real release with a single antigen's worth of best
   * patient series, the loop still delegates once per vaccine group in the
   * schedule. Every group beyond the patient's own is handed to Chapter 9 with
   * nothing of its own to merge. Recorded as observed behaviour, not asserted as
   * a defect - section 4.6 says "loops through each vaccine group" without
   * qualifying which, and no business rule in 4.6 narrows it.
   */
  @Test
  public void aPatientWithOneAntigenStillDrivesChapterNineOncePerScheduleVaccineGroup() throws Exception {
    List<String> zipNames = DataModelLoader.listBundledSupportingDataZipNames();
    String supportingDataSet = null;
    for (String zipName : zipNames) {
      if (zipName.startsWith("supporting-data-")) {
        supportingDataSet = zipName;
      }
    }
    assertNotNull("No bundled CDC supporting data release found, names were: " + zipNames, supportingDataSet);

    dataModel = DataModelLoader.createDataModel(supportingDataSet);
    int scheduleVaccineGroupCount = dataModel.getVaccineGroupMap().size();
    Antigen someAntigen = dataModel.getAntigenList().isEmpty() ? null : dataModel.getAntigenList().get(0);
    assertNotNull("Precondition: " + supportingDataSet + " should define antigens", someAntigen);
    bestPatientSeries("only series", someAntigen.getName());

    List<VaccineGroup> visited = runLoopToCompletion();

    assertEquals("The Chapter 9 invocation count follows the schedule, not the patient",
        scheduleVaccineGroupCount, visited.size());
    assertEquals("One patient antigen does not reduce the loop to one vaccine group",
        1, dataModel.getBestPatientSeriesList().size());
  }
}
