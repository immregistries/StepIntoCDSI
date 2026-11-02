package org.openimmunizationsoftware.cdsi.fitstests.diagnostics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HexFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.openimmunizationsoftware.cdsi.fitstests.FitsEngineRunner;
import org.openimmunizationsoftware.cdsi.fitstests.FitsTestCase;

/**
 * Phase 17: writes the complete, disposable diagnostic bundle for one FITS
 * run under target/fits-runs/&lt;run-id&gt;/ - run.json, summary.json,
 * results.jsonl, changed-cases.json, and a failures/&lt;case-id&gt;/ directory
 * per non-passing case. target/ is Maven's own build directory, already
 * gitignored - nothing this class writes is ever committed automatically.
 *
 * Four of summary.json's categories the plan asks for - known failures,
 * new regressions, newly passing cases, changed known failures - only
 * mean something relative to a reviewed case-level baseline, which is
 * Phase 19 of the plan and not built yet. They are written as null with
 * an explanatory note rather than guessed at or silently omitted.
 * changed-cases.json instead compares against the most recent *previous*
 * local run under target/fits-runs/, when one exists - a same-machine
 * convenience, explicitly not a substitute for a reviewed baseline.
 *
 * trace.jsonl, shown in the plan's own failure-bundle layout, is not
 * written - it depends on the structured per-decision engine trace Phase
 * 18 ("Improve Structured Engine Tracing") adds, which does not exist
 * yet. Writing an empty file would misrepresent that as "no trace data,"
 * when the real state is "this capability isn't built."
 */
public final class FitsRunRecorder {

  private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  private final List<CaseRecord> records = new ArrayList<>();
  private RunContext context;
  private int discoveredCount;
  private Instant startedAt;

  public synchronized void start(RunContext context, int discoveredCount) {
    this.context = context;
    this.discoveredCount = discoveredCount;
    this.startedAt = Instant.now();
  }

  public synchronized void record(FitsTestCase testCase, FitsEngineRunner.FitsRunResult result, long durationMs) {
    records.add(new CaseRecord(testCase, result, durationMs));
  }

  public synchronized Path finish() {
    if (context == null) {
      throw new IllegalStateException("finish() called before start()");
    }
    Path runDir = runsRoot().resolve(context.runId());
    try {
      Files.createDirectories(runDir);
      writeRunJson(runDir);
      writeResultsJsonl(runDir);
      writeSummaryAndChangedCases(runDir);
      writeFailureBundles(runDir);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write FITS run bundle to " + runDir, e);
    }
    return runDir;
  }

  static Path runsRoot() {
    return Paths.get("target", "fits-runs");
  }

  private void writeRunJson(Path runDir) throws IOException {
    Map<String, Object> run = new LinkedHashMap<>();
    run.put("runId", context.runId());
    run.put("startedAt", context.startedAt);
    run.put("finishedAt", Instant.now().toString());
    run.put("gitCommit", context.gitCommit);
    run.put("gitCommitAbbreviated", context.gitCommitAbbreviated);
    run.put("gitBranch", context.gitBranch);
    run.put("gitDirty", context.gitDirty);
    run.put("javaVersion", context.javaVersion);
    run.put("mavenVersion", context.mavenVersion);
    run.put("testFilter", context.testFilter);
    if (context.referenceSet != null) {
      run.put("referenceSetId", context.referenceSet.id());
      run.put("logicSpecVersion", context.referenceSet.logicSpecVersion());
      run.put("supportingDataRelease", context.referenceSet.supportingDataRelease());
      run.put("supportingDataZipName", context.referenceSet.supportingDataZipName());
      run.put("supportingDataBundleSha256", context.referenceSet.supportingDataBundleSha256());
      run.put("fitsFixtureSetSha256", context.referenceSet.fitsFixtureSetSha256());
      run.put("fitsFixtureSetCaseCount", context.referenceSet.fitsFixtureSetCaseCount());
    }
    MAPPER.writeValue(runDir.resolve("run.json").toFile(), run);
  }

  private void writeResultsJsonl(Path runDir) throws IOException {
    // JSONL requires exactly one compact JSON object per physical line -
    // must not use MAPPER directly here, since it's configured to pretty-print
    // (multi-line) for run.json/summary.json/etc.
    var compactWriter = MAPPER.writer().without(SerializationFeature.INDENT_OUTPUT);
    StringBuilder jsonl = new StringBuilder();
    for (CaseRecord record : records) {
      jsonl.append(compactWriter.writeValueAsString(record.toResultEntry())).append('\n');
    }
    Files.writeString(runDir.resolve("results.jsonl"), jsonl.toString(), StandardCharsets.UTF_8);
  }

  private void writeSummaryAndChangedCases(Path runDir) throws IOException {
    int passed = 0, failedAssertions = 0, executionErrors = 0;
    for (CaseRecord record : records) {
      switch (record.status()) {
        case "PASS" -> passed++;
        case "ERROR" -> executionErrors++;
        default -> failedAssertions++;
      }
    }

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("discoveredCases", discoveredCount);
    summary.put("executedCases", records.size());
    summary.put("passedCases", passed);
    summary.put("failedAssertions", failedAssertions);
    summary.put("executionErrors", executionErrors);
    summary.put("skippedCases", discoveredCount - records.size());
    summary.put("evaluation", evaluationSummary());
    summary.put("knownFailures", null);
    summary.put("newRegressions", null);
    summary.put("newlyPassingCases", null);
    summary.put("changedKnownFailures", null);
    summary.put("baselineNote",
        "Case-level regression baseline (Phase 19 of the reference-module plan) is not built yet - "
            + "knownFailures/newRegressions/newlyPassingCases/changedKnownFailures are not available. "
            + "See changed-cases.json for a same-machine comparison against the most recent previous "
            + "local run, which is a convenience, not a reviewed baseline.");

    Path previousRun = findPreviousRun(runDir);
    Map<String, Object> changedCases = new LinkedHashMap<>();
    if (previousRun == null) {
      changedCases.put("comparedAgainst", null);
      changedCases.put("note", "No previous run found under " + runsRoot() + " to compare against.");
      summary.put("addedCases", null);
      summary.put("removedCases", null);
      summary.put("dosesNewlyMatched", null);
      summary.put("dosesNewlyMismatched", null);
    } else {
      ChangedCases diff = compareAgainst(previousRun);
      changedCases.put("comparedAgainst", previousRun.getFileName().toString());
      changedCases.put("added", diff.added());
      changedCases.put("removed", diff.removed());
      changedCases.put("statusChanged", diff.statusChanged());
      changedCases.put("evaluationStatusChanged", diff.evaluationStatusChanged());
      changedCases.put("dosesNewlyMatched", diff.dosesNewlyMatched());
      changedCases.put("dosesNewlyMismatched", diff.dosesNewlyMismatched());
      changedCases.put("dosesStillMismatchedButChanged", diff.dosesStillMismatchedButChanged());
      summary.put("addedCases", diff.added().size());
      summary.put("removedCases", diff.removed().size());
      summary.put("dosesNewlyMatched", diff.dosesNewlyMatched().size());
      summary.put("dosesNewlyMismatched", diff.dosesNewlyMismatched().size());
    }

    MAPPER.writeValue(runDir.resolve("summary.json").toFile(), summary);
    MAPPER.writeValue(runDir.resolve("changed-cases.json").toFile(), changedCases);
  }

  /**
   * Dose-level evaluation totals, kept separate from the case-level forecast
   * counts above (different unit: one entry per expected dose evaluation).
   * confusion is expected status -> actual status -> count, where actual can
   * also be NOT_EVALUATED, or UNMAPPED when no vaccine group could be found
   * to compare (see EvaluationComparator).
   */
  private Map<String, Object> evaluationSummary() {
    int casesWithEvaluations = 0, casesPassed = 0, casesFailed = 0;
    int doses = 0, matched = 0, disagreements = 0;
    Map<String, Map<String, Integer>> confusion = new java.util.TreeMap<>();
    for (CaseRecord record : records) {
      switch (record.evaluationStatus()) {
        case "PASS" -> { casesWithEvaluations++; casesPassed++; }
        case "FAIL" -> { casesWithEvaluations++; casesFailed++; }
        default -> { }
      }
      for (var c : record.evaluationComparisons()) {
        doses++;
        if (c.isMatch()) {
          matched++;
        }
        if (c.isAntigenDisagreement()) {
          disagreements++;
        }
        String actual = c.getActualStatus() == null ? "UNMAPPED" : c.getActualStatus();
        confusion.computeIfAbsent(c.getExpected().getStatus(), k -> new java.util.TreeMap<>())
            .merge(actual, 1, Integer::sum);
      }
    }
    Map<String, Object> evaluation = new LinkedHashMap<>();
    evaluation.put("casesWithExpectedEvaluations", casesWithEvaluations);
    evaluation.put("casesAllDosesMatched", casesPassed);
    evaluation.put("casesWithAMismatch", casesFailed);
    evaluation.put("dosesCompared", doses);
    evaluation.put("dosesMatched", matched);
    evaluation.put("dosesWithAntigenDisagreement", disagreements);
    evaluation.put("confusion", confusion);
    return evaluation;
  }

  private Path findPreviousRun(Path currentRunDir) throws IOException {
    if (!Files.isDirectory(runsRoot())) {
      return null;
    }
    Path latest = null;
    try (var stream = Files.newDirectoryStream(runsRoot(), Files::isDirectory)) {
      for (Path candidate : stream) {
        if (candidate.equals(currentRunDir)) {
          continue;
        }
        if (latest == null || Files.getLastModifiedTime(candidate).compareTo(Files.getLastModifiedTime(latest)) > 0) {
          latest = candidate;
        }
      }
    }
    return latest;
  }

  /**
   * statusChanged is the forecast status; evaluationStatusChanged the case's
   * evaluation status. The three dose lists compare individual dose
   * evaluations (keyed by case, dose position and FITS target CVX) present
   * in both runs - a previous run from before dose evaluations were recorded
   * simply contributes none.
   */
  private record ChangedCases(List<String> added, List<String> removed, List<String> statusChanged,
      List<String> evaluationStatusChanged, List<String> dosesNewlyMatched, List<String> dosesNewlyMismatched,
      List<String> dosesStillMismatchedButChanged) {
  }

  private static String doseKey(String caseId, Object dose, Object targetCvx) {
    return caseId + " dose " + dose + " vs " + targetCvx;
  }

  private ChangedCases compareAgainst(Path previousRunDir) throws IOException {
    Path previousResults = previousRunDir.resolve("results.jsonl");
    Map<String, String> previousStatusByCaseId = new LinkedHashMap<>();
    Map<String, String> previousEvaluationStatusByCaseId = new LinkedHashMap<>();
    Map<String, Map<?, ?>> previousDoses = new LinkedHashMap<>();
    if (Files.exists(previousResults)) {
      for (String line : Files.readAllLines(previousResults, StandardCharsets.UTF_8)) {
        if (line.isBlank()) {
          continue;
        }
        Map<?, ?> entry = MAPPER.readValue(line, Map.class);
        String caseId = (String) entry.get("caseId");
        previousStatusByCaseId.put(caseId, (String) entry.get("status"));
        if (entry.get("evaluationStatus") != null) {
          previousEvaluationStatusByCaseId.put(caseId, (String) entry.get("evaluationStatus"));
        }
        if (entry.get("evaluations") instanceof List<?> evaluations) {
          for (Object o : evaluations) {
            if (o instanceof Map<?, ?> dose) {
              previousDoses.put(doseKey(caseId, dose.get("dose"), dose.get("targetCvx")), dose);
            }
          }
        }
      }
    }
    Set<String> currentCaseIds = new TreeSet<>();
    Map<String, String> currentStatusByCaseId = new LinkedHashMap<>();
    for (CaseRecord record : records) {
      currentCaseIds.add(record.caseId());
      currentStatusByCaseId.put(record.caseId(), record.status());
    }

    List<String> added = new ArrayList<>();
    for (String caseId : currentCaseIds) {
      if (!previousStatusByCaseId.containsKey(caseId)) {
        added.add(caseId);
      }
    }
    List<String> removed = new ArrayList<>();
    for (String caseId : new TreeSet<>(previousStatusByCaseId.keySet())) {
      if (!currentStatusByCaseId.containsKey(caseId)) {
        removed.add(caseId);
      }
    }
    List<String> statusChanged = new ArrayList<>();
    for (String caseId : currentCaseIds) {
      String previousStatus = previousStatusByCaseId.get(caseId);
      if (previousStatus != null && !previousStatus.equals(currentStatusByCaseId.get(caseId))) {
        statusChanged.add(caseId + ": " + previousStatus + " -> " + currentStatusByCaseId.get(caseId));
      }
    }
    List<String> evaluationStatusChanged = new ArrayList<>();
    List<String> newlyMatched = new ArrayList<>();
    List<String> newlyMismatched = new ArrayList<>();
    List<String> stillMismatchedButChanged = new ArrayList<>();
    for (CaseRecord record : records) {
      String caseId = record.caseId();
      String previousEvaluationStatus = previousEvaluationStatusByCaseId.get(caseId);
      if (previousEvaluationStatus != null && !previousEvaluationStatus.equals(record.evaluationStatus())) {
        evaluationStatusChanged.add(caseId + ": " + previousEvaluationStatus + " -> " + record.evaluationStatus());
      }
      for (var c : record.evaluationComparisons()) {
        String key = doseKey(caseId, c.getDoseNumber(), c.getExpected().getVaccineCvx());
        Map<?, ?> before = previousDoses.get(key);
        if (before == null) {
          continue;
        }
        boolean wasMatch = Boolean.TRUE.equals(before.get("match"));
        String previousActual = String.valueOf(before.get("actual"));
        String actual = String.valueOf(c.getActualStatus());
        String line = key + " (cvx " + c.getVaccination().getCvx() + "): " + previousActual + " -> " + actual
            + ", expected " + c.getExpected().getStatus();
        if (!wasMatch && c.isMatch()) {
          newlyMatched.add(line);
        } else if (wasMatch && !c.isMatch()) {
          newlyMismatched.add(line);
        } else if (!c.isMatch() && (!previousActual.equals(actual)
            || !String.valueOf(before.get("perAntigen")).equals(String.valueOf(c.getPerAntigen())))) {
          stillMismatchedButChanged.add(line + " " + before.get("perAntigen") + " -> " + c.getPerAntigen());
        }
      }
    }
    return new ChangedCases(added, removed, statusChanged, evaluationStatusChanged, newlyMatched, newlyMismatched,
        stillMismatchedButChanged);
  }

  private void writeFailureBundles(Path runDir) throws IOException {
    Path failuresDir = runDir.resolve("failures");
    for (CaseRecord record : records) {
      if (!record.needsFailureBundle()) {
        continue;
      }
      Path caseDir = failuresDir.resolve(record.caseId());
      Files.createDirectories(caseDir);
      MAPPER.writeValue(caseDir.resolve("input.json").toFile(), record.inputSummary());
      MAPPER.writeValue(caseDir.resolve("expected.json").toFile(), record.expectedSummary());
      MAPPER.writeValue(caseDir.resolve("actual.json").toFile(), record.actualSummary());
      MAPPER.writeValue(caseDir.resolve("difference.json").toFile(), record.fieldDifferences());
      MAPPER.writeValue(caseDir.resolve("evaluations.json").toFile(), record.evaluationEntries());
    }
  }

  static String sha256(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  static ObjectMapper mapper() {
    return MAPPER;
  }
}
