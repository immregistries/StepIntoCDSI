package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.TableInfo;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * Abstract class that should be the parent of each section test.
 * It provides a structured approach to evaluating logic tables and conditions.
 * This includes testing the accuracy of logic step transitions, table evaluations, and condition checks against expected results.
 *
 * <p><b>Fields:</b></p>
 * <ul>
 * <li><code>String sectionName</code> - Name of the section being tested.</li>
 * <li><code>DataModel model</code> - The DataModel configured for this test.</li>
 * <li><code>&lt;List&lt;List&lt;LogicResult&gt;&gt;&gt; tableResults</code> - Expected results for logic evaluations stored as a list of lists. 
 * Each inner list corresponds to a table.</li>
 * <li><code>List&lt;TableInfo&gt; tableInfo</code> - List of test helper classes that stores expected outcomes 
 * for logic table testing for each table in our section.</li>
 * <li><code>LogicStepType stepName</code> - Current step in the being tested.</li>
 * <li><code>LogicStepType nextStep</code> - Next expected step</li>
 * <li><code>LogicStep step</code> - LogicStep instance being tested.</li>
 * </ul>
 */
public abstract class SectionTest {

    protected String sectionName;
    protected DataModel model;
    protected List<List<LogicResult>> tableResults = new ArrayList<>();
    protected List<TableInfo> tableInfo = new ArrayList<>();
    protected LogicStepType stepName;
    protected LogicStepType nextStep;
    protected LogicStep step;

    /**
     * Constructs a SectionTest instance.
     *
     * @param model DataModel instance that is being tested.
     * @param tableResults List of lists containing expected LogicResults for each table in section.
     * @param sectionName The name of the section under test.
     * @param stepName The current logic step being tested.
     * @param nextStep The expected next logic step following the current.
     */
    public SectionTest(DataModel model, List<List<LogicResult>> tableResults, String sectionName, LogicStepType stepName, LogicStepType nextStep) {
        this.model = model;
        this.tableResults = tableResults;
        this.sectionName = sectionName;
        this.stepName = stepName;
        this.nextStep = nextStep;
    }

    /**
     * Sets up necessary preconditions before each test.
     * This method initializes the LogicStep instance based on the current step name and model.
     */
    @Before
    public void setUp() {
        step = LogicStepFactory.createLogicStep(stepName, model);
    }

    /**
     * Tests the evaluation of logic tables to ensure all conditions and results match expected outputs.
     * Each logic table is associated with a table info instance which provides expected results and labels.
     */
    @Test
    public void testEvaluateLogicTables() {
        List<LogicTable> tables = step.getLogicTableList();
        Iterator<LogicTable> tablesIterator = tables.iterator();
        Iterator<TableInfo> tableInfoIterator = tableInfo.iterator();

        int i = 0;
        while (tablesIterator.hasNext() && tableInfoIterator.hasNext()) {
            LogicTable table = tablesIterator.next();
            TableInfo info = tableInfoIterator.next();

            // Evaluates the label of the logic table to ensure it matches expected name.
            testTableLabel(table, info);
            // Checks each logic condition in the table against expected results.
            testLogicConditions(table, info, i);

            i++;
        }
    }

    /**
     * Helper method to test the label of a logic table against expected table name.
     * @param table The logic table being tested.
     * @param info TableInfo containing the expected table name.
     */
    private void testTableLabel(LogicTable table, TableInfo info) {
        assertEquals(table.getLabel(), info.getTableName());
    }

    /**
     * Helper method to test logic conditions within a table and compare results against expected outcomes.
     * This method iterates over each condition and asserts the results and condition evaluations.
     * 
     * @param table The logic table containing conditions to test.
     * @param info TableInfo containing expected results for logic conditions.
     * @param index Index of the current table in the list of logic tables.
     */
    private void testLogicConditions(LogicTable table, TableInfo info, int index) {
        LogicCondition[] conditions = table.getLogicConditions();
        int numConditions = info.getExpectedLogicResultTable().length; 

        //for each row in the table
        for (int j = 0; j < numConditions; j++) {
            conditions[j].evaluate();

            //Check to make sure logic results are in correct location in table
            assertArrayEquals(
                "Got unexpected value in Logic results table",
                info.getExpectedLogicResultTable()[j], 
                table.getLogicResultTable()[j]
            );

            //Check to make sure logic condition gets evaluated correctly
            assertEquals(
                "Got unexpected value for condition evaluation",
                conditions[j].getLogicResult(),
                tableResults.get(index).get(j)
            );
        }
    }

    /**
     * Tests the determination of the next logic step after evaluating all logic tables.
     * This verifies that the logic step correctly identifies the subsequent step as expected.
     */
    @Test
    public void testCorrectNextStep(){
        LogicStep step = LogicStepFactory.createLogicStep(stepName, model);
        step.evaluateLogicTables();
        assertEquals(this.nextStep, step.getNextLogicStepType());
    }

    /**
     * Abstract method to test the title of the section. Must be implemented by subclasses to ensure section titles are correctly set.
     */
    @Test
    public abstract void testSectionTitle();
}
