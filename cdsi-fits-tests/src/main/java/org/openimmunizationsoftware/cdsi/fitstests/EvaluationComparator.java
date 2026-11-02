package org.openimmunizationsoftware.cdsi.fitstests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;

/**
 * Compares the engine's dose evaluations against FITS' expected ones.
 *
 * The two are recorded at different levels, so this is not a straight
 * field comparison:
 *
 * - FITS gives one verdict per administered dose against one vaccine group,
 *   named by ExpectedEvaluation.relatedTo - a CVX that is sometimes the
 *   group's "unspecified formulation" code (107 DTaP, 03 MMR) and sometimes
 *   a specific product (115 Tdap, 215 PCV15, 10 IPV, 33 PPV23).
 * - CDSi evaluates each dose per antigen, once in each antigen's best
 *   patient series (Diphtheria, Tetanus and Pertussis separately for a
 *   DTaP dose).
 *
 * Mapping relatedTo to antigens: relatedTo's CVX -> the antigens Supporting
 * Data associates with that CVX -> their vaccine group(s) -> every antigen in
 * those groups, intersected with the antigens the administered vaccine
 * itself carries (a Pediarix dose judged against DTaP is compared on
 * Diphtheria/Tetanus/Pertussis only, not HepB/Polio). If the CVX isn't in
 * the Supporting Data's CVX map, or maps to no antigen, falls back to the
 * same CvxEquivalence table the forecast comparison uses, against each
 * antigen's forecast CVX.
 *
 * Rolling per-antigen evaluations up to one dose verdict: the Logic
 * Specification defines no vaccine-group-level dose evaluation, so this is
 * a test-harness choice, not spec text. Precedence NOT_VALID > SUB_STANDARD
 * > VALID > EXTRANEOUS: any antigen judging the dose not valid makes it not
 * valid; a dose that counted for at least one antigen is valid even if it
 * was extraneous for another (DTaP after the pertussis series is complete
 * but tetanus isn't). Each comparison keeps the per-antigen detail and
 * flags antigens that disagree, so this choice is always visible in the
 * results rather than hidden.
 */
public final class EvaluationComparator {

  /** Actual status when no antigen in the target group recorded any evaluation of the dose. */
  public static final String NOT_EVALUATED = "NOT_EVALUATED";

  private EvaluationComparator() {
  }

  public static List<DoseEvaluationComparison> compare(FitsTestCase testCase, DataModel dataModel) {
    Map<String, Map<Integer, EvaluationStatus>> byAntigenThenDoseId = collectEngineEvaluations(dataModel);
    List<DoseEvaluationComparison> comparisons = new ArrayList<>();
    List<FitsTestCase.Vaccination> vaccinations = testCase.getVaccinations();
    for (int i = 0; i < vaccinations.size(); i++) {
      FitsTestCase.Vaccination vaccination = vaccinations.get(i);
      int doseId = i + 1; // GatherNecessaryData numbers doses 1..n in input order
      for (FitsTestCase.ExpectedEvaluation expected : vaccination.getExpectedEvaluations()) {
        comparisons.add(compareOne(doseId, vaccination, expected, dataModel, byAntigenThenDoseId));
      }
    }
    return comparisons;
  }

  private static DoseEvaluationComparison compareOne(int doseId, FitsTestCase.Vaccination vaccination,
      FitsTestCase.ExpectedEvaluation expected, DataModel dataModel,
      Map<String, Map<Integer, EvaluationStatus>> byAntigenThenDoseId) {
    Set<String> groupAntigens = antigensForTarget(expected.getVaccineCvx(), dataModel);
    if (groupAntigens.isEmpty()) {
      return new DoseEvaluationComparison(doseId, vaccination, expected, null, Map.of(),
          "No vaccine group found for FITS relatedTo CVX " + expected.getVaccineCvx());
    }
    Set<String> doseAntigens = antigenNames(cvxAntigens(vaccination.getCvx(), dataModel));
    Set<String> compared = new LinkedHashSet<>(groupAntigens);
    compared.retainAll(doseAntigens);
    if (compared.isEmpty()) {
      return new DoseEvaluationComparison(doseId, vaccination, expected, null, Map.of(),
          "Administered CVX " + vaccination.getCvx() + " carries no antigen of the vaccine group for relatedTo CVX "
              + expected.getVaccineCvx() + " (" + String.join(", ", groupAntigens) + ")");
    }

    Map<String, String> perAntigen = new LinkedHashMap<>();
    List<EvaluationStatus> statuses = new ArrayList<>();
    for (String antigen : compared) {
      Map<Integer, EvaluationStatus> evaluations = byAntigenThenDoseId.get(antigen);
      EvaluationStatus status = evaluations == null ? null : evaluations.get(doseId);
      perAntigen.put(antigen, status == null ? NOT_EVALUATED : status.name());
      if (status != null) {
        statuses.add(status);
      }
    }
    String actual = statuses.isEmpty() ? NOT_EVALUATED : rollUp(statuses).name();
    return new DoseEvaluationComparison(doseId, vaccination, expected, actual, perAntigen, null);
  }

  static EvaluationStatus rollUp(Collection<EvaluationStatus> statuses) {
    for (EvaluationStatus preferred : new EvaluationStatus[] { EvaluationStatus.NOT_VALID,
        EvaluationStatus.SUB_STANDARD, EvaluationStatus.VALID }) {
      if (statuses.contains(preferred)) {
        return preferred;
      }
    }
    return EvaluationStatus.EXTRANEOUS;
  }

  /**
   * antigen name -> (dose id -> status) from every best patient series.
   * A dose is evaluated against at most one target dose per series, so one
   * status per antigen per dose; if a series somehow recorded two, the
   * rollup precedence picks between them the same way it does across
   * antigens.
   */
  private static Map<String, Map<Integer, EvaluationStatus>> collectEngineEvaluations(DataModel dataModel) {
    Map<String, Map<Integer, EvaluationStatus>> result = new LinkedHashMap<>();
    List<PatientSeries> bestSeries = dataModel.getBestPatientSeriesList();
    if (bestSeries == null) {
      return result;
    }
    for (PatientSeries series : bestSeries) {
      if (series == null || series.getTrackedAntigenSeries() == null
          || series.getTrackedAntigenSeries().getTargetDisease() == null || series.getTargetDoseList() == null) {
        continue;
      }
      String antigen = series.getTrackedAntigenSeries().getTargetDisease().getName();
      Map<Integer, EvaluationStatus> byDose = result.computeIfAbsent(antigen, k -> new LinkedHashMap<>());
      for (TargetDose targetDose : series.getTargetDoseList()) {
        if (targetDose.getEvaluationList() == null) {
          continue;
        }
        for (Evaluation evaluation : targetDose.getEvaluationList()) {
          VaccineDoseAdministered vda = evaluation == null ? null : evaluation.getVaccineDoseAdministered();
          if (vda == null || evaluation.getEvaluationStatus() == null) {
            continue;
          }
          byDose.merge(vda.getId(), evaluation.getEvaluationStatus(),
              (a, b) -> rollUp(List.of(a, b)));
        }
      }
    }
    return result;
  }

  private static Set<String> antigensForTarget(String relatedToCvx, DataModel dataModel) {
    Set<VaccineGroup> groups = new LinkedHashSet<>();
    for (Antigen antigen : cvxAntigens(relatedToCvx, dataModel)) {
      if (antigen.getVaccineGroup() != null) {
        groups.add(antigen.getVaccineGroup());
      }
    }
    if (groups.isEmpty() && dataModel.getAntigenMap() != null) {
      for (Antigen antigen : dataModel.getAntigenMap().values()) {
        if (antigen.getVaccineGroup() != null
            && CvxEquivalence.isSameVaccine(relatedToCvx, antigen.getCvxForForecast())) {
          groups.add(antigen.getVaccineGroup());
        }
      }
    }
    Set<String> names = new LinkedHashSet<>();
    for (VaccineGroup group : groups) {
      names.addAll(antigenNames(group.getAntigenList()));
    }
    return names;
  }

  private static List<Antigen> cvxAntigens(String cvx, DataModel dataModel) {
    if (cvx == null || dataModel.getCvxMap() == null) {
      return List.of();
    }
    VaccineType vaccineType = dataModel.getCvxMap().get(cvx);
    return vaccineType == null || vaccineType.getAntigenList() == null ? List.of() : vaccineType.getAntigenList();
  }

  private static Set<String> antigenNames(Collection<Antigen> antigens) {
    Set<String> names = new LinkedHashSet<>();
    for (Antigen antigen : antigens) {
      names.add(antigen.getName());
    }
    return names;
  }

  /** One expected-vs-actual dose evaluation. */
  public static final class DoseEvaluationComparison {
    private final int doseNumber;
    private final FitsTestCase.Vaccination vaccination;
    private final FitsTestCase.ExpectedEvaluation expected;
    private final String actualStatus;
    private final Map<String, String> perAntigen;
    private final String problem;

    DoseEvaluationComparison(int doseNumber, FitsTestCase.Vaccination vaccination,
        FitsTestCase.ExpectedEvaluation expected, String actualStatus, Map<String, String> perAntigen,
        String problem) {
      this.doseNumber = doseNumber;
      this.vaccination = vaccination;
      this.expected = expected;
      this.actualStatus = actualStatus;
      this.perAntigen = perAntigen;
      this.problem = problem;
    }

    /** 1-based position of the dose in the fixture's vaccination list. */
    public int getDoseNumber() {
      return doseNumber;
    }

    public FitsTestCase.Vaccination getVaccination() {
      return vaccination;
    }

    public FitsTestCase.ExpectedEvaluation getExpected() {
      return expected;
    }

    /** Rolled-up engine status, NOT_EVALUATED, or null when {@link #getProblem()} explains why none could be determined. */
    public String getActualStatus() {
      return actualStatus;
    }

    /** antigen name -> engine status (or NOT_EVALUATED) for each antigen compared. */
    public Map<String, String> getPerAntigen() {
      return perAntigen;
    }

    /** True when the compared antigens didn't all reach the same status - the rollup made a choice. */
    public boolean isAntigenDisagreement() {
      return new LinkedHashSet<>(perAntigen.values()).size() > 1;
    }

    public String getProblem() {
      return problem;
    }

    public boolean isMatch() {
      return actualStatus != null && actualStatus.equals(expected.getStatus());
    }

    public String describe() {
      String label = "dose " + doseNumber + " (cvx " + vaccination.getCvx() + " on " + vaccination.getDate()
          + ") vs " + expected.getVaccineCvx();
      if (problem != null) {
        return label + ": " + problem;
      }
      return label + ": expected " + expected.getStatus() + ", got " + actualStatus + " " + perAntigen;
    }
  }
}
