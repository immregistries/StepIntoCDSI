package org.immregistries.step.domain;

import java.util.ArrayList;
import java.util.List;

public class Exclusion {
  private String exclusionCondition = "";
  private List<Concept> conceptList = new ArrayList<Concept>();

  public String getExclusionCondition() {
    return exclusionCondition;
  }

  public void setExclusionCondition(String exclusionCondition) {
    this.exclusionCondition = exclusionCondition;
  }

  public List<Concept> getConceptList() {
    return conceptList;
  }
}
