package org.openimmunizationsoftware.cdsi.core.data;

import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;


import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.data.ConditionPairResult;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.data.GenExpectedTableResults;
import org.openimmunizationsoftware.cdsi.core.data.LogicEnum;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;

/**
 * The goal of this class is to set up the data model for the test cases.
 *  Not quite sure how i want to structure this yet  but we should have a list of methods for each section
 * that return the appropriate data models for that section, maybe we 
 */
public class ModelSetUp {

    public List<DataModel> EvaluateDoseAdministeredCondition() {
        List<DataModel> models = new ArrayList<DataModel>();


        for (ConditionPairResult.ConditionResult condition : ConditionPairResult.Table6_3) {
            DataModel dataModel = new DataModel();
            AntigenAdministeredRecord aar = new AntigenAdministeredRecord();

            int numElements = condition.size();
            VaccineDoseAdministered vda = new VaccineDoseAdministered();

            //Set each of the properties required by our table using the test values from Table6_3, we know the types so we can just cast to them
            for (int i = 0; i< numElements; i++) {
                vda.setDateAdministered((Date) condition.get(i).get(0));
                vda.setDoseCondition((DoseCondition) condition.get(i).get(1));

                if (condition.get(i).get(2) != null) { 
                    aar.setLotExpirationDate((Date) condition.get(i).get(2));
                }
                else { //Use default date is date is not provided
                    LocalDate localDate = LocalDate.of(2999, 12, 31);
                    Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    aar.setLotExpirationDate(date);
                }
            }
            models.add(dataModel);
        }
        return models;
    }
    
    private class ExpectedSectionOutput {
    
        LogicStepType nextStep;
        LogicEnum expectedOut;

        public ExpectedSectionOutput(LogicStepType nextStep, LogicEnum expectedOut) {
            this.nextStep = nextStep;
            this.expectedOut = expectedOut;
        }
    }
}
