package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;

import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.data.TableInfo;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;

@RunWith(Parameterized.class)
public class EvaluateForPreferableVaccineTest extends SectionTest {
    
    private List<TableInfo> tableInfo = new ArrayList<>();
    public EvaluateForPreferableVaccineTest(DataModel model, List<List<LogicResult>> tableResults, LogicStepType nextStep) {
    
        super(model, tableResults, "EvaluateForPreferableVaccine", LogicStepType.EVALUATE_PREFERABLE_VACCINE_ADMINISTERED, nextStep);
        
        // Add Table 6-26 information
        tableInfo.add(new TableInfo(
            "TABLE 6-26 WAS THE VACCINE DOSE ADMINISTERED A PREFERABLE VACCINE FOR THE TARGET DOSE?",
            Arrays.asList(
                "Is the vaccine type of the vaccine dose administered the same as the vaccine type of a preferable vaccine for the target dose?",
                "Is the preferable vaccine type begin age date ≤ date administered < preferable vaccine type end age date?",
                "Is the trade name of the vaccine dose administered the same as the trade name of the preferable vaccine for the target dose?",
                "Is the volume of the vaccine dose administered ≥ the volume of the preferable vaccine for the target dose?"
            ),
            new LogicResult[][]{
                {LogicResult.YES, LogicResult.YES, LogicResult.NO, LogicResult.YES, LogicResult.YES},
                {LogicResult.YES, LogicResult.YES, LogicResult.ANY, LogicResult.NO, LogicResult.YES},
                {LogicResult.YES, LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.NO},
                {LogicResult.YES, LogicResult.NO, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY}
            }
        ));
    }

        // Static method to provide the parameters
        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            Date dateAdmin = new Date();
            Date lotExpBad = new Date(dateAdmin.getTime() - 1000);
            Date lotExpGood = new Date(dateAdmin.getTime() + 1000);
    
            DataModel model1 = new DataModel();
            AntigenAdministeredRecord aar1 = new AntigenAdministeredRecord();
            VaccineDoseAdministered vda1 = new VaccineDoseAdministered();
            vda1.setDateAdministered(dateAdmin);
            aar1.setLotExpirationDate(lotExpGood);
            aar1.setVaccineDoseAdministered(vda1);
            model1.setAntigenAdministeredRecord(aar1);
    
            DataModel model2 = new DataModel();
            AntigenAdministeredRecord aar2 = new AntigenAdministeredRecord();
            VaccineDoseAdministered vda2 = new VaccineDoseAdministered();
            vda2.setDateAdministered(dateAdmin);
            vda2.setDoseCondition(DoseCondition.YES); 
            aar2.setLotExpirationDate(lotExpBad);
            aar2.setVaccineDoseAdministered(vda2);
            model2.setAntigenAdministeredRecord(aar2);
    
            DataModel model3 = new DataModel();
            AntigenAdministeredRecord aar3 = new AntigenAdministeredRecord();
            VaccineDoseAdministered vda3 = new VaccineDoseAdministered();
            vda3.setDateAdministered(dateAdmin);
            vda3.setDoseCondition(DoseCondition.NO);
            aar3.setLotExpirationDate(dateAdmin);
            aar3.setVaccineDoseAdministered(vda3);
            model3.setAntigenAdministeredRecord(aar3);
    
            return Arrays.asList(new Object[][]{
                {model1,  Arrays.asList(Arrays.asList(LogicResult.YES, LogicResult.ANY)), LogicStepType.FORECAST_DATES_AND_REASONS},
                {model2, Arrays.asList(Arrays.asList(LogicResult.NO, LogicResult.YES)),  LogicStepType.FORECAST_DATES_AND_REASONS}, 
                {model3, Arrays.asList(Arrays.asList(LogicResult.NO, LogicResult.NO)), LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION}, 
            });
        }
    

    //Not Sure this provides any value atm 
    @Test
    public void testSectionTitle(){
        assertEquals(stepName.getDisplay(), LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION.getDisplay());
    }
}