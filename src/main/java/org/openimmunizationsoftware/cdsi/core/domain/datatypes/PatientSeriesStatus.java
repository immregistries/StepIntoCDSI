package org.openimmunizationsoftware.cdsi.core.domain.datatypes;

import gov.nist.healthcare.cds.enumeration.SerieStatus;

public enum PatientSeriesStatus {
  COMPLETE, CONTRAINDICATED, IMMUNE, NOT_COMPLETE, NOT_RECOMMENDED, AGED_OUT;

  public String toString() {
    switch (this) {
      case COMPLETE:
        return "Complete";
      case CONTRAINDICATED:
        return "Contraindicated";
      case IMMUNE:
        return "Immune";
      case NOT_COMPLETE:
        return "Not Complete";
      case NOT_RECOMMENDED:
        return "Not Recommended";
      case AGED_OUT:
        return "Aged Out";
      default:
        return null;
    }
  }
}
