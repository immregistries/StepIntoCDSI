package org.openimmunizationsoftware.cdsi.core.data;

import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.crypto.Data;

import java.time.LocalDate;

import org.openimmunizationsoftware.cdsi.core.data.ConditionPairResult.ConditionResult;
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
        int dateElm = ConditionPairResult.Table6_3.conditionPairs.get("Date").get(0).size();
        int doseElm = ConditionPairResult.Table6_3.conditionPairs.get("DoseCondition").get(0).size();
        ConditionResult dates = ConditionPairResult.Table6_3.conditionPairs.get("Date").get(0);
        ConditionResult doses = ConditionPairResult.Table6_3.conditionPairs.get("DoseCondition").get(0);


        //create list of empty data models
        for (int i = 0; i < dateElm; i++){
            DataModel dataModel = new DataModel();
            models.add(dataModel);
        }

        //Deal with first condition in table for "Date administered > lot number expiration date?""
        for (int i = 0; i< dateElm; i++) {

            AntigenAdministeredRecord aar = new AntigenAdministeredRecord();
            VaccineDoseAdministered vda = new VaccineDoseAdministered();

            //Set each of the properties required by our table using the test values from Table6_3, we know the types so we can just cast to them
            vda.setDateAdministered((Date) dates.get(i).get(0)); //0 is index for date admin for table 6_3
            aar.setVaccineDoseAdministered(vda);

            if (dates.get(i).get(1) != null) { 
                aar.setLotExpirationDate((Date) dates.get(i).get(1)); //1 is index for lotexp for table 6_3
            }
            else { //Use default date if date is not provided
                LocalDate localDate = LocalDate.of(2999, 12, 31);
                Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                aar.setLotExpirationDate(date);
            }

            DataModel curModel = models.get(i);
            curModel.setAntigenAdministeredRecord(aar); 
        }
        
        for (int i = 1; i< doseElm + 1; i++) { //Offset by 1 to deal with first case being ANY 
            DataModel curModel = models.get(i);
            curModel.getAntigenAdministeredRecord().getVaccineDoseAdministered().setDoseCondition((DoseCondition) doses.get(i).get(0));
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
