package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;

import gov.nist.healthcare.cds.enumeration.SerieStatus;

public enum VaccineGroupStatus {
  COMPLETE, CONTRAINDICATED, IMMUNE, NOT_COMPLETE, NOT_RECOMMENDED, AGED_OUT;

  static VaccineGroupStatus getVaccineGroupStatus(PatientSeriesStatus patientSeriesStatus) {
    switch (patientSeriesStatus) {
      case COMPLETE:
        return COMPLETE;
      case CONTRAINDICATED:
        return CONTRAINDICATED;
      case IMMUNE:
        return IMMUNE;
      case NOT_COMPLETE:
        return NOT_COMPLETE;
      case NOT_RECOMMENDED:
        return NOT_RECOMMENDED;
      case AGED_OUT:
        return AGED_OUT;
    }
    return null;
  }

  public SerieStatus getSerieStatus() {
    switch (this) {
      case COMPLETE:
        return SerieStatus.C;
      case CONTRAINDICATED:
        return SerieStatus.X;
      case IMMUNE:
        return SerieStatus.I;
      case NOT_COMPLETE:
        return SerieStatus.N;
      case NOT_RECOMMENDED:
        return SerieStatus.R;
      case AGED_OUT:
        return SerieStatus.G;
    }
    return null;
  }

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
