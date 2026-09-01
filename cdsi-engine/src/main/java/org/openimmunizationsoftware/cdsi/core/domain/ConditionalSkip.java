package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class ConditionalSkip {

  public static final String SET_LOGIC_AND = "AND";
  public static final String SET_LOGIC_OR = "OR";
  private String setLogic = "";
  private List<ConditionalSkipSet> conditionalSkipSetList = new ArrayList<ConditionalSkipSet>();

  public String getSetLogic() {
    return setLogic;
  }

  public void setSetLogic(String setLogic) {
    this.setLogic = setLogic;
  }

  public List<ConditionalSkipSet> getConditionalSkipSetList() {
    return conditionalSkipSetList;
  }
}
