package org.openimmunizationsoftware.cdsi.core.logic.items;

public class LogicTable {
  private String label = "";
  private LogicResult[][] logicResultTable;
  private LogicCondition[] logicConditions;
  private LogicOutcome[] logicOutcomes;

  public void evaluate() {
    for (LogicCondition logicCondition : logicConditions) {
      logicCondition.evaluate();
    }
    for (int j = 0; j < logicOutcomes.length; j++) {
      boolean allTrue = true;
      for (int i = 0; i < logicConditions.length; i++) {
        if (logicResultTable[i][j] != LogicResult.ANY
            && logicResultTable[i][j] != logicConditions[i].getLogicResult()) {
          allTrue = false;
        }
      }
      if (allTrue) {
        logicOutcomes[j].perform();
        break;
      }
    }
  }

  public LogicResult[][] getLogicResultTable() {
    return logicResultTable;
  }

  public LogicCondition[] getLogicConditions() {
    return logicConditions;
  }

  public LogicOutcome[] getLogicOutcomes() {
    return logicOutcomes;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public LogicTable(int conditionCount, int outcomeCount, String label) {
    this.label = label;
    logicResultTable = new LogicResult[conditionCount][outcomeCount];
    // initialize array
    for (int i = 0; i < logicResultTable.length; i++) {
      for (int j = 0; j < logicResultTable[i].length; j++) {
        logicResultTable[i][j] = LogicResult.ANY;
      }
    }
    logicConditions = new LogicCondition[conditionCount];
    logicOutcomes = new LogicOutcome[outcomeCount];
  }

  public void setLogicCondition(int pos, LogicCondition logicCondition) {
    logicConditions[pos] = logicCondition;
  }

  public void setLogicOutcome(int pos, LogicOutcome logicOutcome) {
    logicOutcomes[pos] = logicOutcome;
  }

  public void setLogicResults(int pos, LogicResult... lrs) {
    for (int j = 0; j < lrs.length; j++) {
      logicResultTable[pos][j] = lrs[j];
    }
  }
}
