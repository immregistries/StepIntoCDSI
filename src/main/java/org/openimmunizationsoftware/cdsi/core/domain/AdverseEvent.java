package org.openimmunizationsoftware.cdsi.core.domain;

public class AdverseEvent {
  private ImmunizationHistory immunizationHistory = null;

  public ImmunizationHistory getImmunizationHistory() {
    return immunizationHistory;
  }

  public void setImmunizationHistory(ImmunizationHistory immunizationHistory) {
    this.immunizationHistory = immunizationHistory;
  }
}
