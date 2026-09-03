package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;

/**
 * Section 4.2 "Organize Immunization History" (Logic Specification for ACIP
 * Recommendations v4.6, pages 32-34) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/04-02-organize-immunization-history/index.md}.
 *
 * <p>
 * 4.2 declares no business rules and no decision tables - it is described as "a
 * fairly simple iterative process" laid out as three numbered steps around
 * Figure 4-3, with Tables 4-2/4-3 as a worked before/after example. The three
 * numbered steps are what these tests pin:
 *
 * <ol>
 * <li>"For each vaccine dose administered in the patient's immunization
 * history, the vaccine dose administered is interrogated for the antigens
 * contained within."</li>
 * <li>"For each antigen within a vaccine dose administered, an antigen
 * administered record is created" - with note 2a on age-based CVX-to-antigen
 * association and note 2b on the record's data elements.</li>
 * <li>"...sort the antigen administered records by antigen and then by
 * ascending date order within each antigen."</li>
 * </ol>
 *
 * <p>
 * The step is driven directly through its public {@code process()}: it reads
 * only {@code DataModel.getImmunizationHistory()} and writes only
 * {@code DataModel.getAntigenAdministeredRecordList()}, so hand-built domain
 * objects are enough for everything except the age-based association of note
 * 2a, whose Association Begin/End Age data exists only in the CDC Supporting
 * Data release - those two tests load a bundled release the way
 * {@code DataModelLoaderTest} does. Constructing the returned next step is
 * inert - {@code process()} is never called on it here.
 */
public class OrganizeImmunizationHistoryTest {

  /** CVX 121, the one Supporting Data entry with age-limited antigen associations. */
  private static final String CVX_ZOSTER_LIVE = "121";

  private DataModel dataModel;
  private ImmunizationHistory immunizationHistory;
  private final Map<String, Antigen> antigenMap = new HashMap<String, Antigen>();

  @Before
  public void setUp() {
    dataModel = new DataModel();
    immunizationHistory = new ImmunizationHistory();
    dataModel.setImmunizationHistory(immunizationHistory);
    dataModel.setPatient(patient(date(2011, 1, 1)));
    antigenMap.clear();
  }

  private static Date date(int year, int month, int day) {
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(year, month - 1, day);
    return calendar.getTime();
  }

  private static Patient patient(Date dateOfBirth) {
    Patient patient = new Patient();
    patient.setDateOfBirth(dateOfBirth);
    return patient;
  }

  /** One shared {@code Antigen} instance per name, as Supporting Data does. */
  private Antigen antigen(String name) {
    Antigen antigen = antigenMap.get(name);
    if (antigen == null) {
      antigen = new Antigen();
      antigen.setName(name);
      antigenMap.put(name, antigen);
    }
    return antigen;
  }

  private VaccineType vaccineType(String cvxCode, String shortDescription, String... antigenNames) {
    VaccineType vaccineType = new VaccineType();
    vaccineType.setCvxCode(cvxCode);
    vaccineType.setShortDescription(shortDescription);
    for (String antigenName : antigenNames) {
      vaccineType.getAntigenList().add(antigen(antigenName));
    }
    return vaccineType;
  }

  /** Adds one administered dose of {@code vaccineType} to the immunization history. */
  private VaccineDoseAdministered dose(VaccineType vaccineType, Date administered) {
    Vaccine vaccine = new Vaccine();
    vaccine.setVaccineType(vaccineType);
    VaccineDoseAdministered vda = new VaccineDoseAdministered();
    vda.setVaccine(vaccine);
    vda.setDateAdministered(administered);
    vda.setImmunizationHistory(immunizationHistory);
    immunizationHistory.getVaccineDoseAdministeredList().add(vda);
    return vda;
  }

  private LogicStep process() throws Exception {
    return new OrganizeImmunizationHistory(dataModel).process();
  }

  private List<AntigenAdministeredRecord> records() {
    return dataModel.getAntigenAdministeredRecordList();
  }

  private List<String> antigenNames() {
    List<String> names = new ArrayList<String>();
    for (AntigenAdministeredRecord aar : records()) {
      names.add(aar.getAntigen().getName());
    }
    return names;
  }

  /** "antigen|MM/dd/yyyy|cvx" per record, the three columns of Table 4-3. */
  private List<String> table43Rows() {
    SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
    List<String> rows = new ArrayList<String>();
    for (AntigenAdministeredRecord aar : records()) {
      rows.add(aar.getAntigen().getName() + "|" + format.format(aar.getDateAdministered()) + "|"
          + aar.getVaccineType().getCvxCode());
    }
    return rows;
  }

  /**
   * Numbered steps 1 and 2: a combination product is "interrogated for the
   * antigens contained within" and "for each antigen within a vaccine dose
   * administered, an antigen administered record is created". Table 4-3 turns
   * one Pediarix (110, DTaP-HepB-IPV) dose into five records - Diphtheria,
   * HepB, Pertussis, Polio, Tetanus - all carrying that dose's administration
   * date and all pointing back at the single dose they came from.
   */
  @Test
  public void combinationDoseIsExpandedIntoOneRecordPerAntigenItContains() throws Exception {
    VaccineDoseAdministered pediarix = dose(
        vaccineType("110", "DTaP-HepB-IPV", "Diphtheria", "HepB", "Pertussis", "Polio", "Tetanus"),
        date(2011, 3, 1));

    process();

    assertEquals(Arrays.asList("Diphtheria", "HepB", "Pertussis", "Polio", "Tetanus"), antigenNames());
    for (AntigenAdministeredRecord aar : records()) {
      assertEquals(date(2011, 3, 1), aar.getDateAdministered());
      assertSame("Every record from one dose must point back at that dose", pediarix,
          aar.getVaccineDoseAdministered());
    }
  }

  /**
   * The same two numbered steps for a single-antigen product: Table 4-3 turns
   * one Engerix B-Peds (08, HepB) dose into exactly one HepB record - the
   * expansion is one record per antigen, not one record per dose plus extras.
   */
  @Test
  public void singleAntigenDoseIsExpandedIntoExactlyOneRecord() throws Exception {
    dose(vaccineType("08", "HepB", "HepB"), date(2011, 1, 1));

    process();

    assertEquals(1, records().size());
    assertEquals("HepB", records().get(0).getAntigen().getName());
  }

  /**
   * Note 2b: "The activity diagram above provides the basic data elements used
   * in evaluation and forecasting." Figure 4-3's antigen administered record
   * carries the administering details of the dose it came from, so later steps
   * never have to navigate back to the product: date administered, vaccine
   * type, manufacturer, trade name, amount, lot expiration date and dose
   * condition all travel onto the record.
   */
  @Test
  public void recordCarriesTheDataElementsOfTheDoseItWasDerivedFrom() throws Exception {
    VaccineType hepB = vaccineType("08", "HepB", "HepB");
    VaccineDoseAdministered vda = dose(hepB, date(2011, 1, 1));
    vda.getVaccine().setManufacturer("SKB");
    vda.getVaccine().setTradeName("Engerix B-Peds");
    vda.getVaccine().setVolume("0.5");
    vda.getVaccine().setLotExpirationDate(date(2012, 12, 31));
    vda.setDoseCondition(DoseCondition.YES);

    process();

    AntigenAdministeredRecord aar = records().get(0);
    assertEquals(date(2011, 1, 1), aar.getDateAdministered());
    assertSame(hepB, aar.getVaccineType());
    assertEquals("SKB", aar.getManufacturer());
    assertEquals("Engerix B-Peds", aar.getTradeName());
    assertEquals("0.5", aar.getAmount());
    assertEquals(date(2012, 12, 31), aar.getLotExpirationDate());
    assertEquals(DoseCondition.YES, aar.getDoseCondition());
    assertSame(hepB.getAntigenList().get(0), aar.getAntigen());
  }

  /**
   * Numbered step 3, first sort key: "sort the antigen administered records by
   * antigen". The doses are supplied in an order that is neither alphabetical
   * by antigen nor the order the records must come out in.
   */
  @Test
  public void recordsAreSortedByAntigen() throws Exception {
    dose(vaccineType("21", "Varicella", "Varicella"), date(2012, 1, 1));
    dose(vaccineType("08", "HepB", "HepB"), date(2012, 1, 1));
    dose(vaccineType("48", "Hib", "Hib"), date(2012, 1, 1));
    dose(vaccineType("20", "DT", "Diphtheria", "Tetanus"), date(2012, 1, 1));

    process();

    assertEquals(Arrays.asList("Diphtheria", "HepB", "Hib", "Tetanus", "Varicella"), antigenNames());
  }

  /**
   * Numbered step 3, second sort key: "and then by ascending date order within
   * each antigen". The three HepB doses are supplied newest-first, and the two
   * antigens are interleaved, so only a two-key sort produces the expected
   * result.
   */
  @Test
  public void recordsAreSortedByAscendingDateWithinEachAntigen() throws Exception {
    dose(vaccineType("08", "HepB", "HepB"), date(2011, 6, 1));
    dose(vaccineType("48", "Hib", "Hib"), date(2011, 6, 1));
    dose(vaccineType("08", "HepB", "HepB"), date(2011, 3, 1));
    dose(vaccineType("48", "Hib", "Hib"), date(2011, 3, 1));
    dose(vaccineType("08", "HepB", "HepB"), date(2011, 1, 1));

    process();

    assertEquals(Arrays.asList(
        "HepB|01/01/2011|08",
        "HepB|03/01/2011|08",
        "HepB|06/01/2011|08",
        "Hib|03/01/2011|48",
        "Hib|06/01/2011|48"), table43Rows());
  }

  /**
   * The worked example the specification gives for this step end to end: the
   * eight administered products of Table 4-2 ("Prior to Organize Immunization
   * History Example") must come out as the nineteen antigen administered
   * records of Table 4-3 ("After Organize Immunization History Example"), in
   * Table 4-3's stated order ("*Sorted by antigen and then by date").
   */
  @Test
  public void table4_2ImmunizationHistoryProducesTable4_3AntigenAdministeredRecords() throws Exception {
    VaccineType engerixB = vaccineType("08", "HepB", "HepB");
    VaccineType pediarix = vaccineType("110", "DTaP-HepB-IPV",
        "Diphtheria", "HepB", "Pertussis", "Polio", "Tetanus");
    VaccineType actHib = vaccineType("48", "Hib", "Hib");
    VaccineType prevnar13 = vaccineType("133", "PCV13", "PCV");
    VaccineType proQuad = vaccineType("94", "MMRV", "Measles", "Mumps", "Rubella", "Varicella");

    dose(engerixB, date(2011, 1, 1));
    dose(pediarix, date(2011, 3, 1));
    dose(actHib, date(2011, 3, 1));
    dose(prevnar13, date(2011, 3, 1));
    dose(pediarix, date(2011, 6, 1));
    dose(actHib, date(2011, 6, 1));
    dose(prevnar13, date(2011, 6, 1));
    dose(proQuad, date(2012, 1, 1));

    process();

    assertEquals(Arrays.asList(
        "Diphtheria|03/01/2011|110",
        "Diphtheria|06/01/2011|110",
        "HepB|01/01/2011|08",
        "HepB|03/01/2011|110",
        "HepB|06/01/2011|110",
        "Hib|03/01/2011|48",
        "Hib|06/01/2011|48",
        "Measles|01/01/2012|94",
        "Mumps|01/01/2012|94",
        "PCV|03/01/2011|133",
        "PCV|06/01/2011|133",
        "Pertussis|03/01/2011|110",
        "Pertussis|06/01/2011|110",
        "Polio|03/01/2011|110",
        "Polio|06/01/2011|110",
        "Rubella|01/01/2012|94",
        "Tetanus|03/01/2011|110",
        "Tetanus|06/01/2011|110",
        "Varicella|01/01/2012|94"), table43Rows());
  }

  /**
   * Numbered step 1 iterates the immunization history; with nothing in it there
   * is nothing to interrogate and no antigen administered record to create.
   */
  @Test
  public void emptyImmunizationHistoryProducesNoRecords() throws Exception {
    process();

    assertTrue("An empty immunization history must organize into no records", records().isEmpty());
  }

  /**
   * Numbered step 2 creates a record "for each antigen within a vaccine dose
   * administered" - a product the CVX-to-Antigen Supporting Data associates
   * with no antigen at all contributes none, rather than an antigen-less
   * record that later steps would have to defend against.
   */
  @Test
  public void doseWithNoAssociatedAntigenProducesNoRecords() throws Exception {
    dose(vaccineType("998", "no vaccine administered"), date(2011, 1, 1));
    dose(vaccineType("08", "HepB", "HepB"), date(2011, 1, 1));

    process();

    assertEquals(1, records().size());
    assertEquals("HepB", records().get(0).getAntigen().getName());
  }

  /**
   * Next Steps: Table 4-1 lists 4.3 Create Relevant Patient Series next, and
   * the transition is unconditional - it does not depend on whether the
   * immunization history produced any antigen administered records.
   */
  @Test
  public void transitionToCreateRelevantPatientSeriesIsUnconditional() throws Exception {
    LogicStep afterEmptyHistory = process();
    assertEquals(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES, afterEmptyHistory.getLogicStepType());

    setUp();
    dose(vaccineType("08", "HepB", "HepB"), date(2011, 1, 1));
    LogicStep afterPopulatedHistory = process();
    assertEquals(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES,
        afterPopulatedHistory.getLogicStepType());
  }

  /**
   * Note 2a: "The CVX to Antigen Supporting Data includes Association Begin Age
   * and Association End Age attributes to properly associate the administered
   * vaccine with the proper antigen based on the age of patient at the time of
   * administration (e.g., a Zoster vaccine administered below 50 years should
   * be associated with Varicella)."
   *
   * <p>
   * This is the specification's own worked example of the rule. CVX 121 (Zoster
   * live) is associated with Varicella for [0 days, 50 years) and with Zoster
   * from 50 years on, so a dose given to a two-year-old must organize into a
   * single Varicella record - not one record per listed association.
   */
  @Test
  public void zosterLiveGivenBelowFiftyYearsIsAssociatedWithVaricellaOnly() throws Exception {
    loadSupportingDataAndAdministerZosterLive(date(2020, 1, 15), date(2022, 1, 15));

    process();

    assertEquals("A Zoster live dose below 50 years associates with Varicella alone",
        Arrays.asList("Varicella"), antigenNames());
  }

  /**
   * The other side of note 2a's age-based association: the same CVX 121 dose
   * given at or after the Association Begin Age of the Zoster association (50
   * years) must organize into a single Zoster record, the Varicella
   * association having ended at that age.
   */
  @Test
  public void zosterLiveGivenAtOrAboveFiftyYearsIsAssociatedWithZosterOnly() throws Exception {
    loadSupportingDataAndAdministerZosterLive(date(1960, 1, 15), date(2015, 1, 15));

    process();

    assertEquals("A Zoster live dose at 50 years or older associates with Zoster alone",
        Arrays.asList("Zoster"), antigenNames());
  }

  /**
   * Replaces the hand-built data model with one loaded from a bundled CDC
   * Supporting Data release - the Association Begin/End Age attributes note 2a
   * depends on live only there, not in any hand-buildable domain object - and
   * administers one CVX 121 dose to a patient born on {@code dateOfBirth}.
   */
  private void loadSupportingDataAndAdministerZosterLive(Date dateOfBirth, Date administered)
      throws Exception {
    List<String> zipNames = DataModelLoader.listBundledSupportingDataZipNames();
    String supportingDataSet = null;
    for (String zipName : zipNames) {
      if (zipName.startsWith("supporting-data-")) {
        supportingDataSet = zipName;
      }
    }
    assertNotNull("No bundled CDC supporting data release found, names were: " + zipNames,
        supportingDataSet);

    dataModel = DataModelLoader.createDataModel(supportingDataSet);
    immunizationHistory = new ImmunizationHistory();
    dataModel.setImmunizationHistory(immunizationHistory);
    dataModel.setPatient(patient(dateOfBirth));
    dataModel.setAssessmentDate(administered);

    VaccineType zosterLive = dataModel.getCvxMap().get(CVX_ZOSTER_LIVE);
    assertNotNull("CVX " + CVX_ZOSTER_LIVE + " (Zoster live) missing from " + supportingDataSet,
        zosterLive);
    assertTrue("CVX " + CVX_ZOSTER_LIVE + " should carry more than one age-limited antigen"
        + " association in " + supportingDataSet + ", was: " + zosterLive.getAntigenList(),
        zosterLive.getAntigenList().size() > 1);
    assertFalse("Supporting data should be loaded before the step runs",
        dataModel.getCvxMap().isEmpty());

    dose(zosterLive, administered);
  }
}
