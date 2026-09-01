package org.openimmunizationsoftware.cdsi.fitstests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks in that ReferenceSetVerifier agrees with cdsi-reference's Python
 * computation of the same fixture-set checksum (the real cross-language
 * correctness check this whole mechanism depends on - see
 * reference_sets.py's compute_fixture_set docstring), and that a
 * deliberately wrong checksum is actually rejected, not silently accepted.
 */
class ReferenceSetVerifierTest {

  @Test
  void loadAndVerifySucceedsAgainstTheRealExportedReferenceSet() {
    ReferenceSetVerifier.ReferenceSet referenceSet = ReferenceSetVerifier.loadAndVerify();
    assertTrue(referenceSet.id().startsWith("acip-"));
    assertEquals(4896, referenceSet.fitsFixtureSetCaseCount());
  }

  @Test
  void javaAndPythonComputeTheIdenticalFixtureSetChecksum() {
    ReferenceSetVerifier.ReferenceSet referenceSet = ReferenceSetVerifier.load();
    var computed = ReferenceSetVerifier.computeFixtureSet();
    // cdsi-reference computed referenceSet.fitsFixtureSetSha256() with its own,
    // independent Python implementation when the reference set was exported -
    // agreement here is not circular.
    assertEquals(referenceSet.fitsFixtureSetSha256(), computed.sha256());
    assertEquals(referenceSet.fitsFixtureSetCaseCount(), computed.caseCount());
  }

  @Test
  void verifyRejectsAWrongSupportingDataChecksum() {
    ReferenceSetVerifier.ReferenceSet real = ReferenceSetVerifier.load();
    ReferenceSetVerifier.ReferenceSet tampered = new ReferenceSetVerifier.ReferenceSet(
        real.id(), real.logicSpecVersion(), real.supportingDataRelease(), real.supportingDataZipName(),
        "0000000000000000000000000000000000000000000000000000000000000000", real.fitsFixtureSetSha256(),
        real.fitsFixtureSetCaseCount(), real.createdAt());
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> ReferenceSetVerifier.verify(tampered));
    assertTrue(e.getMessage().contains("Supporting Data release changed"));
  }

  @Test
  void verifyRejectsAWrongFixtureSetChecksum() {
    ReferenceSetVerifier.ReferenceSet real = ReferenceSetVerifier.load();
    ReferenceSetVerifier.ReferenceSet tampered = new ReferenceSetVerifier.ReferenceSet(
        real.id(), real.logicSpecVersion(), real.supportingDataRelease(), real.supportingDataZipName(),
        real.supportingDataBundleSha256(), "0000000000000000000000000000000000000000000000000000000000000000",
        real.fitsFixtureSetCaseCount(), real.createdAt());
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> ReferenceSetVerifier.verify(tampered));
    assertTrue(e.getMessage().contains("fixture set changed"));
  }

  @Test
  void verifyRejectsAMissingSupportingDataZip() {
    ReferenceSetVerifier.ReferenceSet real = ReferenceSetVerifier.load();
    ReferenceSetVerifier.ReferenceSet tampered = new ReferenceSetVerifier.ReferenceSet(
        real.id(), real.logicSpecVersion(), real.supportingDataRelease(), "supporting-data-99.9-508.zip",
        real.supportingDataBundleSha256(), real.fitsFixtureSetSha256(), real.fitsFixtureSetCaseCount(),
        real.createdAt());
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> ReferenceSetVerifier.verify(tampered));
    assertTrue(e.getMessage().contains("is not on the classpath"));
  }
}
