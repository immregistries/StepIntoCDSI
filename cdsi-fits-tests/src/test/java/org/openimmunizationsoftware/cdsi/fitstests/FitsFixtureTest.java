package org.openimmunizationsoftware.cdsi.fitstests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

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
 * A fresh checkout has no fixtures until someone runs FitsDownloader
 * (org.openimmunizationsoftware.cdsi.fitstests.download) against a live
 * NIST FITS account - see its class Javadoc. Until then this factory
 * produces zero dynamic tests, which is a no-op, not a failure.
 */
class FitsFixtureTest {

  @TestFactory
  List<DynamicTest> fitsFixtures() {
    String supportingDataSet = DefaultSupportingDataSet.resolve();
    return FitsFixtures.loadAll().stream()
        .map(testCase -> dynamicTest(testCase.displayName(), () -> {
          FitsEngineRunner.FitsRunResult result = FitsEngineRunner.run(testCase, supportingDataSet);
          assertTrue(result.isPass(), result.describeFailure());
        }))
        .collect(Collectors.toList());
  }
}
