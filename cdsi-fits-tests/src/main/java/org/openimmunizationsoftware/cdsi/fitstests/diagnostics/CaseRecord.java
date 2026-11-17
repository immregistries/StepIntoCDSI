package org.openimmunizationsoftware.cdsi.fitstests.diagnostics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.cdsi.fitstests.EvaluationComparator;
import org.openimmunizationsoftware.cdsi.fitstests.FitsEngineRunner;
import org.openimmunizationsoftware.cdsi.fitstests.FitsTestCase;

/** One executed FITS case's result, in the shape FitsRunRecorder needs
 * for results.jsonl and (for a non-passing case) its failures/ bundle. */
final class CaseRecord {

  private final FitsTestCase testCase;
  private final FitsEngineRunner.FitsRunResult result;
  private final long durationMs;

  CaseRecord(FitsTestCase testCase, FitsEngineRunner.FitsRunResult result, long durationMs) {
    this.testCase = testCase;
    this.result = result;
    this.durationMs = durationMs;
  }

  /** Delegates to FitsTestCase.caseId() - the single place this id is
   * computed, so diagnostics and the regression allowlist can never
   * drift apart on the sanitization/format rules. */
  String caseId() {
    return testCase.caseId();
  }

  String status() {
    if (result.getException() != null) {
      return "ERROR";
    }
    return result.isPass() ? "PASS" : "FAIL";
  }

  List<Map<String, Object>> expectedSummary() {
    List<Map<String, Object>> expected = new ArrayList<>();
    for (FitsTestCase.ExpectedForecast e : testCase.getExpectedForecasts()) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("cvx", e.getVaccineCvx());
      entry.put("status", e.getSerieStatus());
      entry.put("earliestDate", e.getEarliestDate());
      entry.put("recommendedDate", e.getRecommendedDate());
      expected.add(entry);
    }
    return expected;
  }

  List<Map<String, Object>> actualSummary() {
    List<Map<String, Object>> actual = new ArrayList<>();
    for (FitsEngineRunner.ForecastComparison c : result.getComparisons()) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("cvx", c.getExpected().getVaccineCvx());
      entry.put("status", c.getActualStatus());
      entry.put("earliestDate", c.getActualEarliest());
      entry.put("recommendedDate", c.getActualRecommended());
      entry.put("problem", c.getProblem());
      actual.add(entry);
    }
    return actual;
  }

  Map<String, Object> inputSummary() {
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("testPlanId", testCase.getTestPlanId());
    input.put("testPlanName", testCase.getTestPlanName());
    input.put("groupName", testCase.getGroupName());
    input.put("uid", testCase.getUid());
    input.put("title", testCase.getTitle());
    input.put("patientSex", testCase.getPatientSex());
    input.put("birthDate", testCase.getBirthDate());
    input.put("evalDate", testCase.getEvalDate());
    List<Map<String, Object>> vaccinations = new ArrayList<>();
    for (FitsTestCase.Vaccination v : testCase.getVaccinations()) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("date", v.getDate());
      entry.put("cvx", v.getCvx());
      entry.put("mvx", v.getMvx());
      entry.put("doseCondition", v.getDoseCondition());
      vaccinations.add(entry);
    }
    input.put("vaccinations", vaccinations);
    return input;
  }

  List<Map<String, Object>> fieldDifferences() {
    List<Map<String, Object>> differences = new ArrayList<>();
    for (FitsEngineRunner.ForecastComparison c : result.getComparisons()) {
      if (c.isPass()) {
        continue;
      }
      FitsTestCase.ExpectedForecast expected = c.getExpected();
      if (c.getProblem() != null) {
        differences.add(fieldDifference(expected.getVaccineCvx(), "noMatchingVaccineGroup", null, c.getProblem()));
        continue;
      }
      if (expected.getSerieStatus() != null && !expected.getSerieStatus().equalsIgnoreCase(c.getActualStatus())) {
        differences.add(fieldDifference(expected.getVaccineCvx(), "status", expected.getSerieStatus(), c.getActualStatus()));
      }
      if (expected.getEarliestDate() != null && !expected.getEarliestDate().equals(c.getActualEarliest())) {
        differences.add(fieldDifference(expected.getVaccineCvx(), "earliestDate", expected.getEarliestDate(), c.getActualEarliest()));
      }
      if (expected.getRecommendedDate() != null && !expected.getRecommendedDate().equals(c.getActualRecommended())) {
        differences.add(fieldDifference(expected.getVaccineCvx(), "recommendedDate", expected.getRecommendedDate(), c.getActualRecommended()));
      }
    }
    return differences;
  }

  private Map<String, Object> fieldDifference(String cvx, String field, String expected, String actual) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("cvx", cvx);
    entry.put("field", field);
    entry.put("expected", expected);
    entry.put("actual", actual);
    return entry;
  }

  /** PASS / FAIL / NONE (FITS expects no dose evaluations) / ERROR - independent of {@link #status()}, which is forecast-only. */
  String evaluationStatus() {
    return result.getEvaluationStatus();
  }

  List<EvaluationComparator.DoseEvaluationComparison> evaluationComparisons() {
    return result.getEvaluations();
  }

  /** A failure bundle is written when either the forecast or the evaluation didn't pass. */
  boolean needsFailureBundle() {
    return !status().equals("PASS") || evaluationStatus().equals("FAIL");
  }

  List<Map<String, Object>> evaluationEntries() {
    List<Map<String, Object>> entries = new ArrayList<>();
    for (EvaluationComparator.DoseEvaluationComparison c : result.getEvaluations()) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("dose", c.getDoseNumber());
      entry.put("date", c.getVaccination().getDate());
      entry.put("cvx", c.getVaccination().getCvx());
      entry.put("targetCvx", c.getExpected().getVaccineCvx());
      entry.put("targetName", c.getExpected().getVaccineName());
      entry.put("expected", c.getExpected().getStatus());
      entry.put("expectedReason", c.getExpected().getReason());
      entry.put("actual", c.getActualStatus());
      entry.put("match", c.isMatch());
      entry.put("perAntigen", c.getPerAntigen());
      entry.put("perAntigenReason", c.getPerAntigenReason());
      entry.put("antigenDisagreement", c.isAntigenDisagreement());
      entry.put("problem", c.getProblem());
      entries.add(entry);
    }
    return entries;
  }

  Map<String, Object> toResultEntry() {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("caseId", caseId());
    entry.put("group", testCase.getGroupName());
    entry.put("testPlanId", testCase.getTestPlanId());
    entry.put("title", testCase.getTitle());
    entry.put("status", status());
    entry.put("evaluationStatus", evaluationStatus());
    entry.put("evaluations", evaluationEntries());
    String expectedJson = FitsRunRecorder.mapper().valueToTree(expectedSummary()).toString();
    String actualJson = FitsRunRecorder.mapper().valueToTree(actualSummary()).toString();
    entry.put("expectedHash", FitsRunRecorder.sha256(expectedJson));
    entry.put("actualHash", FitsRunRecorder.sha256(actualJson));
    entry.put("expected", expectedSummary());
    entry.put("actual", actualSummary());
    entry.put("fieldDifferences", fieldDifferences());
    entry.put("durationMs", durationMs);
    entry.put("baselineComparison", null);
    entry.put("failureBundlePath", needsFailureBundle() ? "failures/" + caseId() + "/" : null);
    if (result.getException() != null) {
      entry.put("exception", result.getException().getClass().getSimpleName() + ": " + result.getException().getMessage());
    }
    return entry;
  }

}
