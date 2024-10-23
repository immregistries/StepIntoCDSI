package org.openimmunizationsoftware.cdsi.core.logic.items;

public class LogicTable {
  private String label = "";
  private LogicResult[][] logicResultTable;
  private LogicCondition[] logicConditions;
  private LogicOutcome[] logicOutcomes;


  //This is so ugly needs some comments and/or refactoring
  public void evaluate() throws IllegalStateException {
    for (LogicCondition logicCondition : logicConditions) {
      logicCondition.evaluate(); //Get the logic result for each condition. Assigns it to LogicCondition.logicResult
    }
    for (int j = 0; j < logicOutcomes.length; j++) {
      boolean validConditionColumn = true;
      int validColumnCount =0;
      //This is saying it is not the case that any value in column j is ANY OR any value in column j is  equal to the logic result of the condition
      //If so set all True to false. 

      //So the way we get this is  a != b && a != c -> !(a ==b || a == c) (DeMorgans Law)
      //By that logic we could go further and say since allTrue starts true we could set it to false the start and  then drop ! from !(a ==b Or a == c)
      //since !(a ==b Or a == c) = !d -> (a ==b Or a == c) = d
      //So we could set allTrue to true if it held for every value in out loop. 

      //The disadvantage of this is we we would lose the ability to break for some efficient gains as we could just throw a break after allTrue = false;
      //right now. Though i think the readability outweighs this. 

      //We should also just have a method in LogicCondition that does this logic and returns a boolean with some meaningful name so its much more readable.
     
      //So basically this ends up saying if EVERY LogicalOutcome in our column is either ANY or the as the LogicResult of the condition
      //Then we can evaluate the column.  Which we do if allTrue is true(really bad name) I think we should call it something like canEvaluate or something
      //BTW if we this we could also then remove the next if condition and just run perform here.
      
      for (int i = 0; i < logicConditions.length; i++) {
        if (logicResultTable[i][j] != LogicResult.ANY
            && logicResultTable[i][j] != logicConditions[i].getLogicResult()) {
              validConditionColumn = false;
        }
      }
      //perform sets the dataModel properties to the appropriate values based on the logic outcome in the table. 
      if (validConditionColumn) {
        logicOutcomes[j].perform(); 
        validColumnCount++;

        //Should only ever have one valid column.
        if (validColumnCount !=1) {
          throw new IllegalStateException("Can only have 1 valid column in a logic table found: " + validColumnCount);
        }
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
