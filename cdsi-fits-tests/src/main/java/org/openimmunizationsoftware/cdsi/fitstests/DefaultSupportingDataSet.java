package org.openimmunizationsoftware.cdsi.fitstests;

import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;

/**
 * Picks which of cdsi-engine's bundled supporting-data zips to forecast
 * against. cdsi-engine can bundle more than one (e.g. a demo/preview set
 * alongside the standard CDC set); FITS conformance is only meaningful
 * against the standard CDC set, so this deliberately ignores anything that
 * doesn't look like one.
 */
public final class DefaultSupportingDataSet {

  private DefaultSupportingDataSet() {
  }

  public static String resolve() {
    List<String> bundled = DataModelLoader.listBundledSupportingDataZipNames();
    for (String name : bundled) {
      if (name.toLowerCase().startsWith("supporting-data-")) {
        return name;
      }
    }
    if (!bundled.isEmpty()) {
      return bundled.get(0);
    }
    throw new IllegalStateException(
        "No supporting-data zip bundled in cdsi-engine (checked its classpath resources)");
  }
}
