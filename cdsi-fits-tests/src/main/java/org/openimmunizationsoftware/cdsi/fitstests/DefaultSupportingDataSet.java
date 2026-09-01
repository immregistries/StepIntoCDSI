package org.openimmunizationsoftware.cdsi.fitstests;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;

/**
 * Picks which of cdsi-engine's bundled supporting-data zips to forecast
 * against. cdsi-engine can bundle more than one (e.g. a demo/preview set
 * alongside the standard CDC set, or the standard CDC set at more than one
 * published version once a new release has been added); FITS conformance
 * is only meaningful against the latest published standard CDC set, so
 * this deliberately ignores anything that doesn't look like one, and
 * among CDC sets picks the highest version number - never just the first
 * one found. Do not compare version strings lexicographically ("4.9" would
 * sort after "4.10"); this parses each dot-separated segment as an integer,
 * matching cdsi-web's SupportingDataManager/VersionComparator convention
 * for the same "supporting-data-<version>[-508].zip" naming pattern.
 */
public final class DefaultSupportingDataSet {

  private static final Pattern CDC_ZIP_VERSION_PATTERN = Pattern
      .compile("^supporting-data-(\\d+(?:\\.\\d+)*)(?:-508)?\\.zip$", Pattern.CASE_INSENSITIVE);

  private DefaultSupportingDataSet() {
  }

  public static String resolve() {
    List<String> bundled = DataModelLoader.listBundledSupportingDataZipNames();

    String latestName = null;
    List<Integer> latestVersion = null;
    for (String name : bundled) {
      Matcher matcher = CDC_ZIP_VERSION_PATTERN.matcher(name);
      if (!matcher.matches()) {
        continue;
      }
      List<Integer> version = parseVersion(matcher.group(1));
      if (latestVersion == null || compareVersions(version, latestVersion) > 0) {
        latestVersion = version;
        latestName = name;
      }
    }
    if (latestName != null) {
      return latestName;
    }

    if (!bundled.isEmpty()) {
      return bundled.get(0);
    }
    throw new IllegalStateException(
        "No supporting-data zip bundled in cdsi-engine (checked its classpath resources)");
  }

  private static List<Integer> parseVersion(String version) {
    List<Integer> segments = new ArrayList<Integer>();
    for (String part : version.split("\\.")) {
      segments.add(Integer.parseInt(part));
    }
    return segments;
  }

  private static int compareVersions(List<Integer> a, List<Integer> b) {
    int maxLength = Math.max(a.size(), b.size());
    for (int i = 0; i < maxLength; i++) {
      int segA = i < a.size() ? a.get(i) : 0;
      int segB = i < b.size() ? b.get(i) : 0;
      if (segA != segB) {
        return Integer.compare(segA, segB);
      }
    }
    return 0;
  }
}
