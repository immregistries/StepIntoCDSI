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

            assertEquals(table.getLabel(), info.getTableName());
            LogicCondition[] conditions  = table.getLogicConditions();

            int numConditions = info.getExpectedLogicResultTable().length; //num rows
            for(int j = 0; j < numConditions; j++){
                conditions[j].evaluate();
                assertArrayEquals("Got unexpected value in Logic results table",info.getExpectedLogicResultTable()[j], table.getLogicResultTable()[j]); 
                assertEquals("Got unexpected value for condition evaluation", conditions[j].getLogicResult(), tableResults.get(i).get(j));
            }
            
            info.getExpectedConditionText();
            info.getExpectedOutcomeText();
            info.getExpectedLogicResultTable();
            i++;
        }
    }


    @Test
    public abstract void testSectionName();
}
