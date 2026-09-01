package org.openimmunizationsoftware.cdsi.fitstests;

/**
 * Which CVX codes FITS treats as the same vaccine group for the purpose of
 * matching an expected forecast to one of the engine's forecasted vaccine
 * groups. Copied from the interactive FITS UI's FitsServlet (cdsi-web) -
 * that table is small, stable, and this module intentionally does not
 * depend on cdsi-web (it would drag in the servlet/HAPI FHIR/Spring
 * dependency footprint for a handful of lines of string matching).
 */
public final class CvxEquivalence {

  private CvxEquivalence() {
  }

  private static final String[][] EQUIVALENT_CVX = {
      { "85", "52" },
      { "45", "08", "189", "43" },
      { "163", "164", "162" },
      { "137", "165" },
      { "48", "17" },
      { "03", "05", "06", "07" }, // MMR (03), Measles (05), Rubella (06), Mumps (07)
      { "107", "112", "20", "11", "112" }, // DTaP: Diphtheria (20), Pertussis (11), Tetanus (112)
      { "304", "122", "303", "305", "306", "307" }, // RSV vaccines and monoclonal antibodies
      { "109", "152", "133" }, { "188", "187" }, { "108", "147" } };

  public static boolean isSameVaccine(String cvx1, String cvx2) {
    if (cvx1 == null || cvx2 == null) {
      return false;
    }
    if (cvx1.equals(cvx2)) {
      return true;
    }
    for (String[] group : EQUIVALENT_CVX) {
      boolean hasFirst = false;
      boolean hasSecond = false;
      for (String cvx : group) {
        if (cvx.equals(cvx1)) {
          hasFirst = true;
        }
        if (cvx.equals(cvx2)) {
          hasSecond = true;
        }
      }
      if (hasFirst && hasSecond) {
        return true;
      }
    }
    return false;
  }
}
