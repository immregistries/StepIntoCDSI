package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.TableInfo;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public abstract class SectionTest {

    protected String sectionName;
    protected DataModel model;
    protected List<List<LogicResult>> tableResults = new ArrayList<>();
    protected List<TableInfo> tableInfo = new ArrayList<>();
    protected LogicStepType stepName;
    protected LogicStepType nextStep;
    protected LogicStep step;

    public SectionTest(DataModel model, List<List<LogicResult>> tableResults, String sectionName, LogicStepType stepName, LogicStepType nextStep) {
        this.model = model;
        this.tableResults = tableResults;
        this.sectionName = sectionName;
        this.stepName = stepName;
        this.nextStep = nextStep;
    }
 
    @Before
    public void setUp() {
        step = LogicStepFactory.createLogicStep(stepName, model);
    }
    
    @Test
    public void testEvaluateLogicTables() {
        List<LogicTable> tables = step.getLogicTableList();
        Iterator<LogicTable> tablesIterator = tables.iterator();
        Iterator<TableInfo> tableInfoIterator = tableInfo.iterator();
    
        int i = 0;
        while (tablesIterator.hasNext() && tableInfoIterator.hasNext()) {
            LogicTable table = tablesIterator.next();
            TableInfo info = tableInfoIterator.next();
    
            // Call helper methods for each set of related assertions
            testTableLabel(table, info);
            testLogicConditions(table, info, i);
            // Add additional helper methods as needed for other assertions
    
            i++;
        }
    }
    
    // Helper method to test table labels
    private void testTableLabel(LogicTable table, TableInfo info) {
        assertEquals(table.getLabel(), info.getTableName());
    }
    
    // Helper method to test logic conditions and results
    private void testLogicConditions(LogicTable table, TableInfo info, int index) {
        LogicCondition[] conditions = table.getLogicConditions();
        int numConditions = info.getExpectedLogicResultTable().length; // num rows
    
        for (int j = 0; j < numConditions; j++) {
            conditions[j].evaluate();
            assertArrayEquals(
                "Got unexpected value in Logic results table",
                info.getExpectedLogicResultTable()[j], 
                table.getLogicResultTable()[j]
            );
            assertEquals(
                "Got unexpected value for condition evaluation",
                conditions[j].getLogicResult(),
                tableResults.get(index).get(j)
            );
        }
    }
    
    // You can add more helper methods to handle other specific logic checks
    

    @Test
    public abstract void testSectionName();
}
