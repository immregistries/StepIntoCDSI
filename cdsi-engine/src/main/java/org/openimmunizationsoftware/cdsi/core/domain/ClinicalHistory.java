package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class ClinicalHistory {
  private String immunityGuidelineCode = "";
  private String immunityGuidelineTitle = "";
  private List<Concept> conceptList = new ArrayList<Concept>();

  public String getImmunityGuidelineCode() {
    return immunityGuidelineCode;
  }

  public void setImmunityGuidelineCode(String immunityGuidelineCode) {
    this.immunityGuidelineCode = immunityGuidelineCode;
  }

  public String getImmunityGuidelineTitle() {
    return immunityGuidelineTitle;
  }

  public void setImmunityGuidelineTitle(String immunityGuidelineTitle) {
    this.immunityGuidelineTitle = immunityGuidelineTitle;
  }

  public List<Concept> getConceptList() {
    return conceptList;
  }
}
