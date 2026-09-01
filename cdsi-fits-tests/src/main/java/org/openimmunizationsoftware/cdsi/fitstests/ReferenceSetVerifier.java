package org.openimmunizationsoftware.cdsi.fitstests;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Phase 16: verifies the reference set cdsi-reference's `reference-set
 * export` wrote to src/test/resources/reference-set.json is still
 * accurate before a FITS run trusts it - the Supporting Data zip actually
 * bundled in cdsi-engine and the FITS fixtures actually on this
 * classpath must still hash to what the reference set recorded.
 *
 * logicSpecVersion is recorded but never checked here - cdsi-engine
 * carries no runtime marker of which Logic Specification version its
 * code implements (that's the entire reason cdsi-reference's mappings
 * and findings exist as a separate, human-reviewed record), so there is
 * no real artifact in this module to hash and compare it against. Only
 * Supporting Data and the FITS fixture set have one.
 *
 * The fixture-set checksum algorithm must stay byte-for-byte identical to
 * cdsi-reference's Python implementation
 * (tools/cdsi_reference_tools/reference_sets.py's compute_fixture_set) -
 * both hash each file's own sha256, keyed by its POSIX-style relative
 * path and sorted, then hash the concatenated "path:hash\n" lines. If one
 * side ever changes, change both together.
 */
public final class ReferenceSetVerifier {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String EXPORT_RESOURCE = "reference-set.json";

  private ReferenceSetVerifier() {
  }

  public record ReferenceSet(
      String id,
      String logicSpecVersion,
      String supportingDataRelease,
      String supportingDataZipName,
      String supportingDataBundleSha256,
      String fitsFixtureSetSha256,
      int fitsFixtureSetCaseCount,
      String createdAt) {
  }

  /** Throws IllegalStateException with a clear message - the reference
   * set is either missing, or one of the two checkable bindings drifted. */
  public static ReferenceSet loadAndVerify() {
    ReferenceSet referenceSet = load();
    verify(referenceSet);
    return referenceSet;
  }

  public static ReferenceSet load() {
    try (InputStream in = ReferenceSetVerifier.class.getClassLoader().getResourceAsStream(EXPORT_RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException(
            "No " + EXPORT_RESOURCE + " on the classpath - run `python -m cdsi_reference_tools reference-set "
                + "export --id <id>` from cdsi-reference to generate it before running the FITS suite.");
      }
      return MAPPER.readValue(in, ReferenceSet.class);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + EXPORT_RESOURCE, e);
    }
  }

  public static void verify(ReferenceSet referenceSet) {
    String actualSupportingDataSha256 = sha256OfClasspathResource("supporting-data/" + referenceSet.supportingDataZipName());
    if (actualSupportingDataSha256 == null) {
      throw new IllegalStateException(
          "Reference set " + referenceSet.id() + " requires supporting-data/" + referenceSet.supportingDataZipName()
              + " to be bundled in cdsi-engine, but it is not on the classpath.");
    }
    if (!actualSupportingDataSha256.equals(referenceSet.supportingDataBundleSha256())) {
      throw new IllegalStateException(
          "Reference set " + referenceSet.id() + " recorded supporting-data/" + referenceSet.supportingDataZipName()
              + " with sha256=" + referenceSet.supportingDataBundleSha256() + ", but the bundled zip now hashes to "
              + actualSupportingDataSha256 + ". The Supporting Data release changed since this reference set was "
              + "created - create a new reference set (cdsi-reference: `reference-set create`) rather than trusting "
              + "a stale one.");
    }

    FixtureSet actualFixtureSet = computeFixtureSet();
    if (!actualFixtureSet.sha256.equals(referenceSet.fitsFixtureSetSha256())) {
      throw new IllegalStateException(
          "Reference set " + referenceSet.id() + " recorded a FITS fixture set with sha256="
              + referenceSet.fitsFixtureSetSha256() + " (" + referenceSet.fitsFixtureSetCaseCount() + " cases), but "
              + "the fixtures actually on the classpath now hash to " + actualFixtureSet.sha256 + " ("
              + actualFixtureSet.caseCount + " cases). The fixture set changed since this reference set was created "
              + "(re-downloaded, added to, or edited) - create a new reference set rather than trusting a stale one.");
    }
  }

  /** Package-visible so ReferenceSetVerifierTest can cross-check this
   * against cdsi-reference's independently-computed value. */
  record FixtureSet(String sha256, int caseCount) {
  }

  static FixtureSet computeFixtureSet() {
    try {
      List<String> lines = new ArrayList<>();
      for (Path root : FitsFixtures.fixtureRoots()) {
        try (Stream<Path> paths = Files.walk(root)) {
          List<Path> jsonFiles = paths.filter(p -> p.toString().toLowerCase().endsWith(".json")).toList();
          for (Path jsonFile : jsonFiles) {
            String relative = root.relativize(jsonFile).toString().replace('\\', '/');
            lines.add(relative + ":" + sha256(Files.readAllBytes(jsonFile)));
          }
        }
      }
      lines.sort(Comparator.naturalOrder());
      StringBuilder canonical = new StringBuilder();
      for (String line : lines) {
        canonical.append(line).append('\n');
      }
      return new FixtureSet(sha256(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)), lines.size());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to compute the FITS fixture set checksum", e);
    }
  }

  private static String sha256OfClasspathResource(String resourcePath) {
    try (InputStream in = ReferenceSetVerifier.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (in == null) {
        return null;
      }
      return sha256(in.readAllBytes());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read classpath resource " + resourcePath, e);
    }
  }

  private static String sha256(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
