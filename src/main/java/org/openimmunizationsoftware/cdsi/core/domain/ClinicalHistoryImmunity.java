package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class ClinicalHistoryImmunity {
  private Immunity immunity = null;
  private String immunityGuideline = "";
  private List<Concept> conceptList = new ArrayList<Concept>();

  public Immunity getImmunity() {
    return immunity;
  }
  
  public void setImmunity(Immunity immunity) {
    this.immunity = immunity;
  }
  
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
