package org.openimmunizationsoftware.cdsi.core.data;

import java.util.Arrays;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;

public class TableInfo {

    private String tableName;
    private List<String> expectedConditionText;
    private LogicResult[][] expectedLogicResultTable;
    
    public TableInfo(String tableName, List<String> expectedConditionText, LogicResult[][] expectedLogicResultTable){ 
        this.tableName = tableName;
        this.expectedLogicResultTable = expectedLogicResultTable;
        this.expectedConditionText = expectedConditionText;
    };
    
    public String getTableName(){
        return tableName;
    }

    public List<String> getExpectedConditionText(){
        return expectedConditionText;
    }
    
    public LogicResult[][] getExpectedLogicResultTable(){
        return expectedLogicResultTable;
    }

}
