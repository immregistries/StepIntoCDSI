package org.openimmunizationsoftware.cdsi.core.data;

import java.util.Arrays;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;

public class TableInfo {

    private String tableName;
    private List<String> expectedConditionText;
    private List<String> expectedOutcomeText;
    private LogicResult[][] expectedLogicResultTable;
    
    public TableInfo(String tableName, List<String> expectedConditionText, List<String> expectedOutcomeText, LogicResult[][] expectedLogicResultTable){ 
        this.tableName = tableName;
        this.expectedOutcomeText = expectedOutcomeText;
        this.expectedLogicResultTable = expectedLogicResultTable;
        this.expectedConditionText = expectedConditionText;
    };
    
    public String getTableName(){
        return tableName;
    }

    public List<String> getExpectedConditionText(){
        return expectedConditionText;
    }
    
    public List<String> getExpectedOutcomeText(){
        return expectedOutcomeText;
    }
    
    public LogicResult[][] getExpectedLogicResultTable(){
        return expectedLogicResultTable;
    }

}
