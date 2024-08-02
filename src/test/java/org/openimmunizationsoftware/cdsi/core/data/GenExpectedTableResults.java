package org.openimmunizationsoftware.cdsi.core.data;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.junit.runners.Parameterized;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.openimmunizationsoftware.cdsi.core.data.ExpectedLogicTables.TableValues;

public class GenExpectedTableResults  {
 
    //This only covers first element of logic table conditions, need to cover 'Y' flag as well
    public static List<ComparablePair<Date>> conditionGen6_3(){
        Date dateAdmin = new Date();
        Date lotExpGood = new Date(dateAdmin.getTime() - 1000);
        Date lotExpBad = new Date(dateAdmin.getTime() + 1000);

        ComparablePair<Date> ex1 = new ComparablePair<Date>(dateAdmin, lotExpGood, LogicResult.NO);//dateadmin is before lot expiration date
        ComparablePair<Date> ex2= new ComparablePair<Date>(dateAdmin, lotExpBad, LogicResult.YES);//dateadmin is after lot expiration date
        ComparablePair<Date> ex3 = new ComparablePair<Date>(dateAdmin, dateAdmin, LogicResult.NO);//dateadmin is same as lot expiration date

        return Arrays.asList(ex1, ex2, ex3);
    }
    
    public String expectedLabel;
    public ExpectedCondition[] expectedLogicConditions;
    public List<String> expectedLogicOutcomes;
    public LogicResult[][] expectedLogicResultTable;


    public GenExpectedTableResults(String expectedLabel, ExpectedCondition[] expectedLogicConditions, List<String> expectedLogicOutcomes, LogicResult[][] expectedLogicResultTable) {
        this.expectedLabel = expectedLabel;
        this.expectedLogicConditions = expectedLogicConditions;
        this.expectedLogicOutcomes = expectedLogicOutcomes;
        this.expectedLogicResultTable = expectedLogicResultTable;
    }

    public int getOutcomePos(){
        for (int j = 0; j < expectedLogicOutcomes.size(); j++) {
            boolean validCol = true;
            for (int i = 0; i < expectedLogicConditions.length; i++) {
                if (!expectedLogicConditions[i].resultsMatch(expectedLogicResultTable[i][j])) {
                    validCol = false;
                    break;
                }
            }
            if (validCol) {
                return j;
            }
            
        }
        return -1;
    }
    
}

