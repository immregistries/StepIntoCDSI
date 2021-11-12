package org.openimmunizationsoftware.cdsi.core.domain;

public class VaccineContraindication {
  private Contraindication contraindication = null;
  private VaccineType vaccineType = null;

  public Contraindication getContraindication() {
    return contraindication;
  }

  public void setContraindication(Contraindication contraindication) {
    this.contraindication = contraindication;
  }

  public VaccineType getVaccineType() {
    return vaccineType;
  }

  public void setVaccineType(VaccineType vaccineType) {
    this.vaccineType = vaccineType;
  }
}
