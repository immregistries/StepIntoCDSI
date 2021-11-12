package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class ClinicalHistoryImmunity {
  private String immunityGuideline = "";
  private List<Concept> conceptList = new ArrayList<Concept>();

  public String getImmunityGuideline() {
    return immunityGuideline;
  }

  public void setImmunityGuideline(String immunityGuideline) {
    this.immunityGuideline = immunityGuideline;
  }

  public List<Concept> getConceptList() {
    return conceptList;
  }
}
