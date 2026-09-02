package org.openimmunizationsoftware.cdsi.fitstests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.openimmunizationsoftware.cdsi.fitstests.diagnostics.FitsRunRecorder;
import org.openimmunizationsoftware.cdsi.fitstests.diagnostics.RunContext;

/**
 * Runs every FITS fixture under src/test/resources/fits/ against cdsi-engine
 * directly. No servlet container, no live NIST connection, no browser -
 * `mvn test` (or `mvn test -Dtest=FitsFixtureTest`) is the entire FITS
 * regression suite.
 *
 * A failure here names the exact FITS group/uid and what mismatched
 * (expected vs. actual status/earliest/recommended date), which is the
 * trace a human or an AI agent needs to localize the defect to a layer
 * (supporting data, rule semantics, or orchestration - see
 * docs/15-separation-of-concerns-in-cdsi-architecture.md) and propose a
 * minimal fix, then re-run this suite to confirm the fix and check for
 * regressions across the rest of the fixture set.
 *
 * The fixtures themselves are committed - a normal checkout already has
 * them. FitsDownloader (org.openimmunizationsoftware.cdsi.fitstests.download)
 * is only for refreshing them from a live NIST FITS account, not a
 * prerequisite for a first run.
 *
 * Phase 16: before building the dynamic test list, verifies the reference
 * set exported from cdsi-reference (reference-set.json - see
 * ReferenceSetVerifier) still matches what's actually bundled and on the
 * classpath. If the Supporting Data zip or the fixture set itself has
 * drifted from what that reference set recorded, this factory throws
 * instead of silently running against something different than intended
 * - see ReferenceSetVerifier's class Javadoc for exactly what is and
 * isn't checked, and why.
 *
 * Phase 17: every case's result (pass, fail, or error) is recorded into a
 * structured diagnostic bundle under target/fits-runs/&lt;run-id&gt;/ - see
 * FitsRunRecorder's class Javadoc for exactly what that bundle contains
 * and what it deliberately doesn't yet (case-level baseline comparison,
 * Phase 19; structured engine traces, Phase 18). This is disposable local
 * build output, never committed automatically.
 *
 * Known-passing allowlist: cdsi-engine does not pass every FITS case today,
 * and won't for a while - some gaps are real defects, some may turn out to
 * be CDSi spec/fixture errors we can't just "fix" (see cdsi-fits-tests's
 * AGENTS.md). Failing the whole build on every not-yet-investigated case
 * would either block all other work or train everyone to ignore red builds
 * while waiting for "one day we'll pass them all." Instead: a case listed
 * in src/test/resources/known-passing-cases.txt is asserted (a regression
 * there fails the build - that's a real signal); every other case is run
 * and recorded exactly the same way, but checked with an assumption, so a
 * miss there is reported as skipped, not failed, and never silently
 * swallowed. See README.md's "Known-passing allowlist" section for the
 * regeneration and review process - membership only ever comes from a real
 * run's results, reviewed by hand, never a guess or a hand-added id.
 */
class FitsFixtureTest {

  private static final String KNOWN_PASSING_RESOURCE = "known-passing-cases.txt";

  private static final FitsRunRecorder RECORDER = new FitsRunRecorder();
  private static final Set<String> KNOWN_PASSING = loadKnownPassingCases();

  @TestFactory
  List<DynamicTest> fitsFixtures() {
    ReferenceSetVerifier.ReferenceSet referenceSet = ReferenceSetVerifier.loadAndVerify();
    String supportingDataSet = DefaultSupportingDataSet.resolve();
    if (!supportingDataSet.equals(referenceSet.supportingDataZipName())) {
      throw new IllegalStateException(
          "Reference set " + referenceSet.id() + " was created for " + referenceSet.supportingDataZipName()
              + ", but DefaultSupportingDataSet.resolve() currently picks " + supportingDataSet + " (a newer "
              + "Supporting Data release was likely added). Create and export a new reference set from "
              + "cdsi-reference before running this suite against it.");
    }
    List<FitsTestCase> testCases = FitsFixtures.loadAll();
    RECORDER.start(RunContext.capture(referenceSet), testCases.size());
    return testCases.stream()
        .map(testCase -> dynamicTest(testCase.displayName(), () -> {
          long start = System.nanoTime();
          FitsEngineRunner.FitsRunResult result = FitsEngineRunner.run(testCase, supportingDataSet);
          long durationMs = (System.nanoTime() - start) / 1_000_000;
          RECORDER.record(testCase, result, durationMs);
          if (KNOWN_PASSING.contains(testCase.caseId())) {
            assertTrue(result.isPass(), result.describeFailure());
          } else {
            assumeTrue(result.isPass(), result.describeFailure());
          }
        }))
        .collect(Collectors.toList());
  }

  @AfterAll
  static void writeRunBundle() {
    Path runDir = RECORDER.finish();
    System.out.println("FITS run bundle written to " + runDir.toAbsolutePath());
  }

  private static Set<String> loadKnownPassingCases() {
    try (InputStream in = FitsFixtureTest.class.getClassLoader().getResourceAsStream(KNOWN_PASSING_RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException(
            "src/test/resources/" + KNOWN_PASSING_RESOURCE + " is missing - see README.md's "
                + "\"Known-passing allowlist\" section.");
      }
      Set<String> caseIds = new HashSet<>();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          String trimmed = line.trim();
          if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            continue;
          }
          caseIds.add(trimmed);
        }
      }
      return Collections.unmodifiableSet(caseIds);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read " + KNOWN_PASSING_RESOURCE, e);
    }
  }
}
