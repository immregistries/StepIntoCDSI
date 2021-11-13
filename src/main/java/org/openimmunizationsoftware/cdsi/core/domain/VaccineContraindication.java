package org.openimmunizationsoftware.cdsi.core.domain;

public class VaccineContraindication extends Contraindication {
  private VaccineType vaccineType = null;

  public VaccineType getVaccineType() {
    return vaccineType;
  }

  public void setVaccineType(VaccineType vaccineType) {
    this.vaccineType = vaccineType;
  }
}
