package org.openimmunizationsoftware.cdsi.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility for comparing version strings using numeric segment comparison.
 * Supports version formats like "4.64", "4.10", "2025.02", etc.
 * 
 * Comparison rules:
 * - Parse versions as dot-separated integer segments
 * - Compare segments left-to-right numerically
 * - Missing trailing segments treated as 0 (4.10 == 4.10.0)
 * - Leading zeros ignored (2025.02 == 2025.2)
 * 
 * Examples:
 * - 4.10 > 4.9 (10 > 9 numerically)
 * - 4.10 == 4.10.0 (trailing 0 implicit)
 * - 2025.02 == 2025.2 (leading zero ignored)
 */
public class VersionComparator {

    /**
     * Parse a version string into a list of integer segments.
     * Non-numeric segments are treated as 0.
     * 
     * @param version Version string (e.g., "4.64", "2025.02")
     * @return List of integer segments (e.g., [4, 64], [2025, 2])
     */
    public static List<Integer> parseVersion(String version) {
        List<Integer> segments = new ArrayList<>();
        if (version == null || version.trim().isEmpty()) {
            return segments;
        }

        String[] parts = version.trim().split("\\.");
        for (String part : parts) {
            try {
                segments.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException e) {
                // Non-numeric segment treated as 0
                segments.add(0);
            }
        }

        return segments;
    }

    /**
     * Compare two version strings numerically.
     * 
     * @param a First version string
     * @param b Second version string
     * @return Negative if a < b, zero if a == b, positive if a > b
     */
    public static int compareVersions(String a, String b) {
        List<Integer> segmentsA = parseVersion(a);
        List<Integer> segmentsB = parseVersion(b);

        int maxLength = Math.max(segmentsA.size(), segmentsB.size());

        for (int i = 0; i < maxLength; i++) {
            int segA = i < segmentsA.size() ? segmentsA.get(i) : 0;
            int segB = i < segmentsB.size() ? segmentsB.get(i) : 0;

            if (segA != segB) {
                return Integer.compare(segA, segB);
            }
        }

        return 0; // Equal
    }

    /**
     * Select the latest version from a collection of version strings.
     * Uses numeric segment comparison.
     * 
     * @param versions Collection of version strings
     * @return Latest version string, or null if collection is empty/null
     */
    public static String selectLatest(Collection<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        String latest = null;
        for (String version : versions) {
            if (latest == null || compareVersions(version, latest) > 0) {
                latest = version;
            }
        }

        return latest;
    }
}
