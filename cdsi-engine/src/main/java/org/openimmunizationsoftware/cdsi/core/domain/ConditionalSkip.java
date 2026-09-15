package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.ConditionalSkipContext;

public class ConditionalSkip {

  public static final String SET_LOGIC_AND = "AND";
  public static final String SET_LOGIC_OR = "OR";
  private String setLogic = "";
  private ConditionalSkipContext context = null;
  private List<ConditionalSkipSet> conditionalSkipSetList = new ArrayList<ConditionalSkipSet>();

  public String getSetLogic() {
    return setLogic;
  }

  public void setSetLogic(String setLogic) {
    this.setLogic = setLogic;
  }

  public ConditionalSkipContext getContext() {
    return context;
  }

  public void setContext(ConditionalSkipContext context) {
    this.context = context;
  }

  /**
   * Table 6-4: only Evaluation or Both. An unset context (hand-built fixtures
   * that predate this field; never in real Supporting Data) applies
   * unconditionally so existing tests keep their one instance.
   */
  public boolean appliesToEvaluation() {
    return context == null || context == ConditionalSkipContext.EVALUATION
        || context == ConditionalSkipContext.BOTH;
  }

  /**
   * 7.1 / 7.6.1: only Forecast or Both. Unset context applies unconditionally,
   * same as {@link #appliesToEvaluation()}.
   */
  public boolean appliesToForecast() {
    return context == null || context == ConditionalSkipContext.FORECAST
        || context == ConditionalSkipContext.BOTH;
  }

  public List<ConditionalSkipSet> getConditionalSkipSetList() {
    return conditionalSkipSetList;
  }
}
