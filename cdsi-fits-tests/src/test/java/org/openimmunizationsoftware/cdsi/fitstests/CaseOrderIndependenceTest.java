package org.openimmunizationsoftware.cdsi.fitstests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openimmunizationsoftware.cdsi.fitstests.FitsEngineRunner.FitsRunResult;
import org.openimmunizationsoftware.cdsi.fitstests.FitsEngineRunner.ForecastComparison;

/**
 * Phase 22: DataModelLoader now caches one SupportingDataModel per
 * supportingDataSet string and shares it across every DataModel built from
 * it, instead of re-parsing the Supporting Data ZIP per case. Production
 * code confirmed to never mutate that shared instance after load (see the
 * Phase 22 plan), but this is the regression test proving it: two genuinely
 * different real FITS cases, run back to back in both orders, must produce
 * identical results for each case regardless of which one ran first through
 * the shared cache.
 */
class CaseOrderIndependenceTest {

  private static final String DTAP_CASE_ID = "5eeca0522cc4517a96b0f417-DTAP-2013-0001";
  private static final String MMR_CASE_ID = "5eeca0522cc4517a96b0f417-MMR-2013-0523";

  @Test
  void twoDifferentCasesProduceTheSameResultRegardlessOfRunOrder() {
    String supportingDataSet = DefaultSupportingDataSet.resolve();
    List<FitsTestCase> allCases = FitsFixtures.loadAll();

    FitsTestCase dtapCase = findCase(allCases, DTAP_CASE_ID);
    FitsTestCase mmrCase = findCase(allCases, MMR_CASE_ID);

    // Order A: DTAP then MMR.
    FitsRunResult dtapFirst = FitsEngineRunner.run(dtapCase, supportingDataSet);
    FitsRunResult mmrSecond = FitsEngineRunner.run(mmrCase, supportingDataSet);

    // Order B: MMR then DTAP - both runs share the same cached
    // SupportingDataModel that Order A's runs did, per DataModelLoader's
    // cache being keyed only on supportingDataSet, not on call order.
    FitsRunResult mmrFirst = FitsEngineRunner.run(mmrCase, supportingDataSet);
    FitsRunResult dtapSecond = FitsEngineRunner.run(dtapCase, supportingDataSet);

    assertNoExceptions(dtapFirst, mmrSecond, mmrFirst, dtapSecond);

    assertComparisonsMatch("DTAP", dtapFirst, dtapSecond);
    assertComparisonsMatch("MMR", mmrFirst, mmrSecond);
  }

  private static void assertNoExceptions(FitsRunResult... results) {
    for (FitsRunResult result : results) {
      assertNotNull(result, "FitsEngineRunner.run must not return null");
      if (result.getException() != null) {
        throw new AssertionError(
            "Case " + result.getTestCase().caseId() + " threw " + result.getException(), result.getException());
      }
    }
  }

  private static void assertComparisonsMatch(String label, FitsRunResult a, FitsRunResult b) {
    List<ForecastComparison> comparisonsA = a.getComparisons();
    List<ForecastComparison> comparisonsB = b.getComparisons();
    assertEquals(comparisonsA.size(), comparisonsB.size(),
        label + ": expected the same number of expected-forecast comparisons in both orders");

    for (int i = 0; i < comparisonsA.size(); i++) {
      ForecastComparison ca = comparisonsA.get(i);
      ForecastComparison cb = comparisonsB.get(i);
      assertEquals(ca.getActualStatus(), cb.getActualStatus(),
          label + " comparison " + i + ": status must not depend on which case ran through the shared "
              + "SupportingDataModel first");
      assertEquals(ca.getActualEarliest(), cb.getActualEarliest(),
          label + " comparison " + i + ": earliest date must not depend on run order");
      assertEquals(ca.getActualRecommended(), cb.getActualRecommended(),
          label + " comparison " + i + ": recommended date must not depend on run order");
      assertTrue(ca.isPass() == cb.isPass(), label + " comparison " + i + ": pass/fail must not depend on run order");
    }
  }

  private static FitsTestCase findCase(List<FitsTestCase> cases, String caseId) {
    for (FitsTestCase testCase : cases) {
      if (caseId.equals(testCase.caseId())) {
        return testCase;
      }
    }
    throw new IllegalStateException(
        "Fixture " + caseId + " was not found under src/test/resources/fits/ - "
            + "this test needs two specific, already-known fixtures to exist.");
  }
}
