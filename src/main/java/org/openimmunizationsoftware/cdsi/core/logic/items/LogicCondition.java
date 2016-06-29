package org.openimmunizationsoftware.cdsi.core.logic.items;

public abstract class LogicCondition
{
  private String label = "";
  private LogicResult logicResult = null;

  public LogicResult getLogicResult() {
    return logicResult;
  }

  public LogicCondition(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
  
  public void evaluate()
  {
    logicResult = evaluateInternal();
  }

  protected abstract LogicResult evaluateInternal();
}
