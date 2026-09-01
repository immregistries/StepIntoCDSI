package org.openimmunizationsoftware.cdsi.servlet;

import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class ServletUtil {
  public static String safe(VaccineType cvx) {
    if (cvx == null || cvx.getCvxCode().equals("")) {
      return "&nbsp;";
    }
    return cvx.getShortDescription() + " (" + cvx.getCvxCode() + ")";
  }

  public static String safe(TimePeriod timePeriod) {
    if (timePeriod == null) {
      return "&nbsp;";
    }
    return timePeriod.toString();
  }

  public static String safe(String s) {
    if (s == null || s.equals("")) {
      return "&nbsp;";
    }
    return s;
  }
}
