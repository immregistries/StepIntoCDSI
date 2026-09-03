package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.ForecastInput;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Observation;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientObservation;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;

/**
 * Section 4.1 "Gather Necessary Data" (Logic Specification for ACIP
 * Recommendations v4.6, page 31) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/04-01-gather-necessary-data/index.md}.
 *
 * <p>
 * 4.1 is explicitly declared by the specification to be "outside of the purview
 * of this document" - it defines <i>no</i> business rules and <i>no</i> decision
 * tables, only the two lists of data an implementation needs before evaluation
 * can begin. There is therefore no normative rule for the implementation to
 * mismatch. What is verifiable here is the step package's documented
 * <b>State Changes</b> section, which records what
 * {@link GatherNecessaryData#process()} actually contributes to the shared
 * {@code DataModel}: a {@code Patient}, an {@code ImmunizationHistory} holding
 * one {@code VaccineDoseAdministered} per input vaccination (CVX resolved
 * against Supporting Data, unrecognized codes rejected), zero or more
 * {@code PatientObservation}s (same resolution and same failure behaviour), and
 * an unconditional transition to 4.2. These tests pin exactly that.
 *
 * <p>
 * The step is driven directly through its public {@code process()} rather than
 * reflectively (unlike {@code NoValidDosesCompletableTest}): it needs only a
 * bare {@code DataModel} with a CVX map and an observation map, so no Supporting
 * Data load or pipeline scaffolding is required. Constructing the returned next
 * step is inert - {@code process()} is never called on it here.
 */
public class GatherNecessaryDataTest {

  private static final String CVX_HEPB = "08";
  private static final String CVX_MMR = "03";
  private static final String OBS_CODE = "070.30";

  private DataModel dataModel;
  private ForecastInput input;
  private VaccineType hepB;
  private VaccineType mmr;
  private Observation observation;

  private static Date date(int year, int month, int day) {
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(year, month - 1, day);
    return calendar.getTime();
  }

  private static VaccineType vaccineType(String cvxCode, String shortDescription) {
    VaccineType vaccineType = new VaccineType();
    vaccineType.setCvxCode(cvxCode);
    vaccineType.setShortDescription(shortDescription);
    return vaccineType;
  }

  @Before
  public void setUp() {
    dataModel = new DataModel();

    hepB = vaccineType(CVX_HEPB, "Hep B, adolescent or pediatric");
    mmr = vaccineType(CVX_MMR, "MMR");
    Map<String, VaccineType> cvxMap = new HashMap<String, VaccineType>();
    cvxMap.put(CVX_HEPB, hepB);
    cvxMap.put(CVX_MMR, mmr);
    dataModel.setCvxMap(cvxMap);

    observation = new Observation();
    observation.setObservationCode(OBS_CODE);
    observation.setObservationTitle("Chronic Hepatitis B");
    dataModel.getObservationMap().put(OBS_CODE, observation);

    input = new ForecastInput();
    input.setPatientDateOfBirth(date(2020, 1, 15));
    input.setPatientSex("F");
    input.setAssessmentDate(date(2024, 6, 1));
    dataModel.setForecastInput(input);
  }

  private LogicStep process() throws Exception {
    return new GatherNecessaryData(dataModel).process();
  }

  private ForecastInput.VaccinationInput vaccination(String cvx, String mvx, Date administered) {
    ForecastInput.VaccinationInput vaccination = input.addVaccination();
    vaccination.setVaccineCvx(cvx);
    vaccination.setVaccineMvx(mvx);
    vaccination.setDateAdministered(administered);
    return vaccination;
  }

  private ForecastInput.ObservationInput observation(String code, Date observedOn) {
    ForecastInput.ObservationInput observationInput = input.addObservation();
    observationInput.setObservationCode(code);
    observationInput.setObservationDate(observedOn);
    return observationInput;
  }

  /**
   * State Changes: the step populates "a {@code Patient} (date of birth,
   * gender)" on the data model, from the {@code ForecastInput}'s patient date of
   * birth and patient sex.
   */
  @Test
  public void patientIsPopulatedFromForecastInput() throws Exception {
    process();

    Patient patient = dataModel.getPatient();
    assertNotNull("4.1 must put a Patient on the data model", patient);
    assertEquals(date(2020, 1, 15), patient.getDateOfBirth());
    assertEquals("F", patient.getGender());
  }

  /**
   * Inputs and Attributes lists the assessment date as part of the
   * {@code ForecastInput} this step consumes; {@code process()} copies it onto
   * the data model for every later step to read. (The step package's State
   * Changes list does not itself mention the assessment date - see the notes on
   * unit 4.1 in {@code cdsi-reference/step-tests/status.yaml}.)
   */
  @Test
  public void assessmentDateIsCopiedOntoTheDataModel() throws Exception {
    process();

    assertEquals(date(2024, 6, 1), dataModel.getAssessmentDate());
  }

  /**
   * State Changes: the step populates "an {@code ImmunizationHistory}". It is
   * reachable both directly from the data model and through the patient's
   * medical history, and the medical-history back-reference is set, since later
   * steps navigate from either end.
   */
  @Test
  public void immunizationHistoryIsCreatedAndCrossLinkedWithTheMedicalHistory() throws Exception {
    process();

    Patient patient = dataModel.getPatient();
    ImmunizationHistory history = dataModel.getImmunizationHistory();
    assertNotNull("4.1 must put an ImmunizationHistory on the data model", history);
    assertSame(history, patient.getMedicalHistory().getImmunizationHistory());
    assertSame(patient.getMedicalHistory(), history.getMedicalHistory());
  }

  /**
   * State Changes: "one {@code VaccineDoseAdministered} per input vaccination".
   * Input order is preserved, each dose carries its administered date, and each
   * is linked back to the patient and the immunization history (and forward from
   * the patient's receives list).
   */
  @Test
  public void oneVaccineDoseAdministeredIsCreatedPerInputVaccination() throws Exception {
    vaccination(CVX_HEPB, "MSD", date(2020, 1, 15));
    vaccination(CVX_MMR, "MSD", date(2021, 2, 20));

    process();

    Patient patient = dataModel.getPatient();
    ImmunizationHistory history = dataModel.getImmunizationHistory();
    assertEquals(2, history.getVaccineDoseAdministeredList().size());

    VaccineDoseAdministered first = history.getVaccineDoseAdministeredList().get(0);
    VaccineDoseAdministered second = history.getVaccineDoseAdministeredList().get(1);
    assertEquals(date(2020, 1, 15), first.getDateAdministered());
    assertEquals(date(2021, 2, 20), second.getDateAdministered());

    assertSame(patient, first.getPatient());
    assertSame(history, first.getImmunizationHistory());
    assertEquals(history.getVaccineDoseAdministeredList(), patient.getReceivesList());
  }

  /**
   * State Changes: each dose is created by "resolving each CVX code against the
   * loaded supporting data's CVX map". The resolved {@code VaccineType} must be
   * the Supporting Data instance itself, not a copy, because later steps compare
   * antigens by identity through it. The MVX travels through as the vaccine's
   * manufacturer.
   */
  @Test
  public void cvxCodeIsResolvedAgainstTheSupportingDataCvxMap() throws Exception {
    vaccination(CVX_MMR, "MSD", date(2021, 2, 20));

    process();

    VaccineDoseAdministered vda = dataModel.getImmunizationHistory().getVaccineDoseAdministeredList().get(0);
    assertNotNull(vda.getVaccine());
    assertSame(mmr, vda.getVaccine().getVaccineType());
    assertEquals("MSD", vda.getVaccine().getManufacturer());
  }

  /**
   * State Changes: the step "throws {@code IllegalArgumentException} for an
   * unrecognized code".
   */
  @Test
  public void unrecognizedCvxCodeIsRejected() throws Exception {
    vaccination("999", "MSD", date(2021, 2, 20));

    try {
      process();
      fail("An unrecognized CVX code must be rejected, not silently dropped");
    } catch (IllegalArgumentException expected) {
      assertTrue("The message should name the offending code, was: " + expected.getMessage(),
          expected.getMessage() != null && expected.getMessage().contains("999"));
    }
  }

  /**
   * Inputs and Attributes lists an "optional dose condition" on each
   * vaccination. When the caller supplies one it reaches the
   * {@code VaccineDoseAdministered} (5.1 reads it); when the caller omits it the
   * dose is left with none rather than a substituted default.
   */
  @Test
  public void doseConditionIsCarriedThroughOnlyWhenTheCallerSuppliesOne() throws Exception {
    vaccination(CVX_HEPB, "MSD", date(2020, 1, 15)).setDoseCondition(DoseCondition.YES);
    vaccination(CVX_MMR, "MSD", date(2021, 2, 20));

    process();

    ImmunizationHistory history = dataModel.getImmunizationHistory();
    assertEquals(DoseCondition.YES, history.getVaccineDoseAdministeredList().get(0).getDoseCondition());
    assertNull("An omitted dose condition must not be defaulted",
        history.getVaccineDoseAdministeredList().get(1).getDoseCondition());
  }

  /**
   * State Changes: "zero or more {@code PatientObservation}s (resolving each
   * observation code against the loaded supporting data's observation map)". The
   * resulting observation code carries the Supporting Data code and title, and
   * the caller-supplied observation date.
   */
  @Test
  public void observationIsResolvedAgainstTheSupportingDataObservationMap() throws Exception {
    observation(OBS_CODE, date(2022, 3, 10));

    process();

    java.util.List<PatientObservation> observations =
        dataModel.getPatient().getMedicalHistory().getPatientObservationList();
    assertEquals(1, observations.size());
    PatientObservation patientObservation = observations.get(0);
    assertEquals(OBS_CODE, patientObservation.getObservationCode().getCode());
    assertEquals(observation.getObservationTitle(), patientObservation.getObservationCode().getText());
    assertEquals(date(2022, 3, 10), patientObservation.getObservationDate());
  }

  /**
   * State Changes: "same failure behavior for an unrecognized code" as for CVX.
   */
  @Test
  public void unrecognizedObservationCodeIsRejected() throws Exception {
    observation("not-a-real-observation", date(2022, 3, 10));

    try {
      process();
      fail("An unrecognized observation code must be rejected, not silently dropped");
    } catch (IllegalArgumentException expected) {
      assertTrue("The message should name the offending code, was: " + expected.getMessage(),
          expected.getMessage() != null && expected.getMessage().contains("not-a-real-observation"));
    }
  }

  /**
   * The "zero or more" in State Changes: an observation slot the caller left
   * blank contributes nothing and is not treated as an unrecognized code. This
   * is implementation behaviour (a caller-shaped concession to web forms and
   * fixtures that always send a fixed number of observation slots); section 4.1
   * says nothing about it either way.
   */
  @Test
  public void blankObservationCodeIsSkippedRatherThanRejected() throws Exception {
    observation("", date(2022, 3, 10));
    observation(null, date(2022, 3, 10));

    process();

    assertEquals(0, dataModel.getPatient().getMedicalHistory().getPatientObservationList().size());
  }

  /**
   * Plain-Language Walkthrough: "whatever the caller is ... adapts its own input
   * shape into a {@code ForecastInput} before the engine ever runs." With no
   * {@code ForecastInput} on the data model there is nothing to gather, and the
   * step reports that rather than producing a half-built patient.
   */
  @Test
  public void missingForecastInputIsRejected() throws Exception {
    dataModel.setForecastInput(null);

    try {
      process();
      fail("Running 4.1 with no ForecastInput must be reported, not silently ignored");
    } catch (IllegalStateException expected) {
      assertNull("No Patient should have been left half-built on the data model", dataModel.getPatient());
    }
  }

  /**
   * Next Steps: "Unconditional transition to 4.2 Organize Immunization History"
   * - unconditional meaning it does not depend on what was gathered, so an empty
   * history transitions the same way a populated one does.
   */
  @Test
  public void transitionToOrganizeImmunizationHistoryIsUnconditional() throws Exception {
    LogicStep afterEmptyInput = process();
    assertEquals(LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY, afterEmptyInput.getLogicStepType());

    setUp();
    vaccination(CVX_HEPB, "MSD", date(2020, 1, 15));
    observation(OBS_CODE, date(2022, 3, 10));
    LogicStep afterPopulatedInput = process();
    assertEquals(LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY, afterPopulatedInput.getLogicStepType());
  }

  /**
   * State Changes: "It does not itself load Schedule/Antigen Series/Series
   * Dose/Vaccine Group/Antigen data - those are loaded earlier, when the
   * {@code DataModel} is constructed", and organizing the history into antigen
   * administered records is 4.2's job, not 4.1's.
   */
  @Test
  public void stepDoesNotLoadSupportingDataOrOrganizeTheHistory() throws Exception {
    vaccination(CVX_HEPB, "MSD", date(2020, 1, 15));

    process();

    assertTrue("4.1 must not load Schedule data", dataModel.getScheduleList().isEmpty());
    assertTrue("4.1 must not load Antigen Series data", dataModel.getAntigenSeriesList().isEmpty());
    assertTrue("Organizing the history into antigen administered records belongs to 4.2",
        dataModel.getAntigenAdministeredRecordList().isEmpty());
  }
}
