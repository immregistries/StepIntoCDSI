package org.openimmunizationsoftware.cdsi.fitstests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;

/**
 * Locks in that resolve() picks the highest-numbered standard CDC
 * supporting-data set actually bundled in cdsi-engine right now, not
 * merely the first one found - the bug this class exists to avoid.
 * Runs against cdsi-engine's real bundled resources, the same way
 * FitsFixtureTest does, rather than a fake/mocked file list, so this
 * fails the moment a real zip is added, renamed, or removed in a way
 * that would silently change which supporting data FITS tests run
 * against.
 */
class DefaultSupportingDataSetTest {

  @Test
  void resolvesToTheHighestVersionedStandardCdcSet() {
    String resolved = DefaultSupportingDataSet.resolve();
    assertTrue(resolved.toLowerCase().startsWith("supporting-data-"),
        "Expected a standard CDC set, not an alternative schedule like a demo bundle: " + resolved);

    List<String> bundled = DataModelLoader.listBundledSupportingDataZipNames();
    for (String name : bundled) {
      if (!name.toLowerCase().startsWith("supporting-data-") || name.equals(resolved)) {
        continue;
      }
      assertTrue(compareCdcZipVersions(resolved, name) > 0,
          "resolve() picked " + resolved + ", but " + name + " is also bundled and is not older");
    }
  }

  /**
   * Test-only numeric comparison of two "supporting-data-<version>[-508].zip"
   * names, independent of DefaultSupportingDataSet's own implementation, so
   * this test doesn't just restate the production code under test.
   */
  private static int compareCdcZipVersions(String zipNameA, String zipNameB) {
    int[] versionA = extractVersion(zipNameA);
    int[] versionB = extractVersion(zipNameB);
    int maxLength = Math.max(versionA.length, versionB.length);
    for (int i = 0; i < maxLength; i++) {
      int a = i < versionA.length ? versionA[i] : 0;
      int b = i < versionB.length ? versionB[i] : 0;
      if (a != b) {
        return Integer.compare(a, b);
      }
    }
    return 0;
  }

  private static int[] extractVersion(String zipName) {
    String withoutPrefix = zipName.substring("supporting-data-".length());
    String withoutExtension = withoutPrefix.replaceFirst("(?i)\\.zip$", "");
    String withoutSuffix = withoutExtension.replaceFirst("(?i)-508$", "");
    String[] parts = withoutSuffix.split("\\.");
    int[] version = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      version[i] = Integer.parseInt(parts[i]);
    }
    return version;
  }

  @Test
  void currentlyBundledSetsIncludeBoth464And465() {
    // Documents the exact scenario this fix was written for: two published
    // CDC releases bundled at once. If this ever fails because 4.64 has
    // been removed, that's fine - update this test, not resolve()'s logic.
    List<String> bundled = DataModelLoader.listBundledSupportingDataZipNames();
    assertTrue(bundled.contains("supporting-data-4.64-508.zip"), bundled.toString());
    assertTrue(bundled.contains("supporting-data-4.65-508.zip"), bundled.toString());
    assertEquals("supporting-data-4.65-508.zip", DefaultSupportingDataSet.resolve());
  }
}
