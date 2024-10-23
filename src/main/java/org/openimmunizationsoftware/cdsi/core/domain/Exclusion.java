package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class Exclusion {
  private String exclusionCode = "";
  private String exclusionTitle = "";
  private List<Concept> conceptList = new ArrayList<Concept>();

  public String getExclusionCode() {
    return exclusionCode;
  }

  public void setExclusionCode(String exclusionCode) {
    this.exclusionCode = exclusionCode;
  }

  public String getExclusionTitle() {
    return exclusionTitle;
  }

  public void setExclusionTitle(String exclusionTitle) {
    this.exclusionTitle = exclusionTitle;
  }

  public List<Concept> getConceptList() {
    return conceptList;
  }
}
