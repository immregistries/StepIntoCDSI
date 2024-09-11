package org.openimmunizationsoftware.cdsi.core.data;

import java.util.List;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;

/**
 * Stores metadata and expected outcomes for logic table testing.
 * This includes the table name, condition descriptions, and expected logic results,
 * enabling verification of logic table evaluations.
 */
public class TableInfo {

    private String tableName;
    private List<String> expectedConditionText;
    private LogicResult[][] expectedLogicResultTable;
    
    /**
     * Initializes a new TableInfo with details for logic table testing.
     *
     * @param tableName Name of the logic table for label checks.
     * @param expectedConditionText Descriptions of each condition for validation.
     * @param expectedLogicResultTable Expected possible Logic table values, these should be exactly matched from the documentation.
     */
    public TableInfo(String tableName, List<String> expectedConditionText, LogicResult[][] expectedLogicResultTable) { 
        this.tableName = tableName;
        this.expectedConditionText = expectedConditionText;
        this.expectedLogicResultTable = expectedLogicResultTable;
    }
    
    /**
     * Returns the expected table name.
     *
     * @return Name used for table label verification.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Returns the text of expected conditions.
     *
     * @return List of condition descriptions for validation.
     */
    public List<String> getExpectedConditionText() {
        return expectedConditionText;
    }
    
    /**
     * Returns the expected outcomes of logic evaluations.
     *
     * @return 2D Array of logic results for outcome comparison.
     */
    public LogicResult[][] getExpectedLogicResultTable() {
        return expectedLogicResultTable;
    }
}
