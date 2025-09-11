package org.openimmunizationsoftware.cdsi.core.data;

import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;

class ExpectedCondition extends LogicCondition {

  private LogicResult logicResult;

  public ExpectedCondition(String label, LogicResult logicResult) {
    super(label);
    this.logicResult = logicResult;
  }

  public boolean namesMatch(LogicCondition actual){
    return this.getLabel().equals(actual.getLabel());
  }

  public boolean resultsMatch(LogicCondition actual){
    if (this.logicResult == LogicResult.ANY) {
      return true;
    }
    return this.logicResult.equals(actual.getLogicResult());
  }

  public boolean resultsMatch(LogicResult actual){
    if (this.logicResult == LogicResult.ANY) {
      return true;
    }
    return this.logicResult.equals(actual);
  }

  @Override
  public void evaluate() {
    ;
  }


  @Override
  protected LogicResult evaluateInternal() {
    return null;
  }
}