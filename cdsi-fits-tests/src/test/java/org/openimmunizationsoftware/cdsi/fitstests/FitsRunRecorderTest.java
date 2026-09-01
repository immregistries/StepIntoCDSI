package org.openimmunizationsoftware.cdsi.fitstests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openimmunizationsoftware.cdsi.fitstests.diagnostics.FitsRunRecorder;
import org.openimmunizationsoftware.cdsi.fitstests.diagnostics.RunContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Exercises FitsRunRecorder with synthetic, controlled results (built
 * directly in this package, since FitsRunResult/ForecastComparison have
 * package-private constructors) rather than running the real engine -
 * fast and lets every status (PASS/FAIL/ERROR) be tested precisely,
 * unlike a real run where the mix of outcomes isn't under the test's
 * control.
 */
class FitsRunRecorderTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private FitsTestCase testCase(String group, String uid, String cvx, String status, String earliest, String recommended) {
    return testCase("synthetic-plan", group, uid, cvx, status, earliest, recommended);
  }

  private FitsTestCase testCase(String testPlanId, String group, String uid, String cvx, String status,
      String earliest, String recommended) {
    FitsTestCase testCase = new FitsTestCase();
    testCase.setGroupName(group);
    testCase.setUid(uid);
    testCase.setTitle("synthetic case " + uid);
    testCase.setTestPlanId(testPlanId);
    testCase.setPatientSex("F");
    testCase.setBirthDate("2020-01-01");
    testCase.setEvalDate("2026-01-01");
    FitsTestCase.ExpectedForecast expected = new FitsTestCase.ExpectedForecast();
    expected.setVaccineCvx(cvx);
    expected.setSerieStatus(status);
    expected.setEarliestDate(earliest);
    expected.setRecommendedDate(recommended);
    testCase.setExpectedForecasts(List.of(expected));
    return testCase;
  }

  private FitsEngineRunner.FitsRunResult passingResult(FitsTestCase testCase) {
    FitsTestCase.ExpectedForecast expected = testCase.getExpectedForecasts().get(0);
    FitsEngineRunner.ForecastComparison comparison = new FitsEngineRunner.ForecastComparison(
        expected, expected.getSerieStatus(), expected.getEarliestDate(), expected.getRecommendedDate(), true, null);
    return new FitsEngineRunner.FitsRunResult(testCase, List.of(comparison), null);
  }

  private FitsEngineRunner.FitsRunResult failingResult(FitsTestCase testCase, String actualEarliest) {
    FitsTestCase.ExpectedForecast expected = testCase.getExpectedForecasts().get(0);
    FitsEngineRunner.ForecastComparison comparison = new FitsEngineRunner.ForecastComparison(
        expected, expected.getSerieStatus(), actualEarliest, actualEarliest, false, null);
    return new FitsEngineRunner.FitsRunResult(testCase, List.of(comparison), null);
  }

  private FitsEngineRunner.FitsRunResult erroredResult(FitsTestCase testCase) {
    return new FitsEngineRunner.FitsRunResult(testCase, List.of(), new RuntimeException("synthetic engine failure"));
  }

  @Test
  void writesACompleteBundleWithMixedOutcomes() throws IOException {
    FitsTestCase pass = testCase("Synthetic", "PASS-1", "01", "NOT_COMPLETE", "2026-01-01", "2026-01-01");
    FitsTestCase fail = testCase("Synthetic", "FAIL-1", "02", "NOT_COMPLETE", "2026-01-01", "2026-01-01");
    FitsTestCase error = testCase("Synthetic", "ERROR-1", "03", "NOT_COMPLETE", "2026-01-01", "2026-01-01");

    FitsRunRecorder recorder = new FitsRunRecorder();
    recorder.start(RunContext.capture(ReferenceSetVerifier.load()), 3);
    recorder.record(pass, passingResult(pass), 5L);
    recorder.record(fail, failingResult(fail, "2026-06-01"), 7L);
    recorder.record(error, erroredResult(error), 1L);
    Path runDir = recorder.finish();

    try {
      assertTrue(Files.isDirectory(runDir));
      Map<?, ?> summary = MAPPER.readValue(runDir.resolve("summary.json").toFile(), Map.class);
      assertEquals(3, summary.get("discoveredCases"));
      assertEquals(3, summary.get("executedCases"));
      assertEquals(1, summary.get("passedCases"));
      assertEquals(1, summary.get("failedAssertions"));
      assertEquals(1, summary.get("executionErrors"));
      assertNull(summary.get("knownFailures"));

      List<String> lines = Files.readAllLines(runDir.resolve("results.jsonl"), StandardCharsets.UTF_8);
      assertEquals(3, lines.size());
      for (String line : lines) {
        Map<?, ?> entry = MAPPER.readValue(line, Map.class); // must parse as exactly one compact JSON object
        assertTrue(entry.containsKey("caseId"));
      }

      assertTrue(Files.isDirectory(runDir.resolve("failures").resolve("synthetic-plan-Synthetic-FAIL-1")));
      assertTrue(Files.isDirectory(runDir.resolve("failures").resolve("synthetic-plan-Synthetic-ERROR-1")));
      assertTrue(Files.notExists(runDir.resolve("failures").resolve("synthetic-plan-Synthetic-PASS-1")));

      List<Map<String, Object>> difference = MAPPER.readValue(
          runDir.resolve("failures").resolve("synthetic-plan-Synthetic-FAIL-1").resolve("difference.json").toFile(), List.class);
      assertEquals(2, difference.size()); // earliestDate and recommendedDate both diverged
    } finally {
      deleteRecursively(runDir);
    }
  }

  @Test
  void caseIdsDoNotCollideAcrossDifferentTestPlansWithTheSameGroupAndUid() throws IOException {
    // Confirmed for real against the actual FITS fixtures: the same group
    // name (e.g. "HepA") and the same uid numbering scheme recur across
    // different NIST test plans - groupName+uid alone is not a unique key.
    FitsTestCase caseA = testCase("planA", "HepA", "2013-0001", "85", "NOT_COMPLETE", "2026-01-01", "2026-01-01");
    FitsTestCase caseB = testCase("planB", "HepA", "2013-0001", "85", "NOT_COMPLETE", "2026-01-01", "2026-01-01");

    FitsRunRecorder recorder = new FitsRunRecorder();
    recorder.start(RunContext.capture(ReferenceSetVerifier.load()), 2);
    recorder.record(caseA, failingResult(caseA, "2026-02-01"), 1L);
    recorder.record(caseB, failingResult(caseB, "2026-03-01"), 1L);
    Path runDir = recorder.finish();

    try {
      List<String> lines = Files.readAllLines(runDir.resolve("results.jsonl"), StandardCharsets.UTF_8);
      List<Object> caseIds = lines.stream().map(line -> {
        try {
          return MAPPER.readValue(line, Map.class).get("caseId");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }).toList();
      assertEquals(2, caseIds.stream().distinct().count(), "planA and planB cases must not share a caseId: " + caseIds);

      // Both failure bundles must exist, independently - neither overwrote the other.
      assertTrue(Files.isDirectory(runDir.resolve("failures").resolve("planA-HepA-2013-0001")));
      assertTrue(Files.isDirectory(runDir.resolve("failures").resolve("planB-HepA-2013-0001")));
    } finally {
      deleteRecursively(runDir);
    }
  }

  private void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (var stream = Files.walk(path)) {
      stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        } catch (IOException e) {
          // best-effort cleanup of disposable test-run output
        }
      });
    }
  }
}
