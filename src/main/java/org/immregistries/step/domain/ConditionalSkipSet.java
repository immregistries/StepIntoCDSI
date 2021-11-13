package org.immregistries.step.domain;

import java.util.ArrayList;
import java.util.List;

public class ConditionalSkipSet {

  public static final String CONDITION_LOGIC_AND = "AND";
  public static final String CONDITION_LOGIC_OR = "OR";

  private int setId = 0;
  private String setDescription = "";
  private String conditionLogic = "";
  private List<ConditionalSkipCondition> conditionList = new ArrayList<ConditionalSkipCondition>();

  public int getSetId() {
    return setId;
  }

  public void setSetId(int setId) {
    this.setId = setId;
  }

  public String getSetDescription() {
    return setDescription;
  }

  public void setSetDescription(String setDescription) {
    this.setDescription = setDescription;
  }

  public String getConditionLogic() {
    return conditionLogic;
  }

  public void setConditionLogic(String conditionLogic) {
    this.conditionLogic = conditionLogic;
  }

  public List<ConditionalSkipCondition> getConditionList() {
    return conditionList;
  }

}
