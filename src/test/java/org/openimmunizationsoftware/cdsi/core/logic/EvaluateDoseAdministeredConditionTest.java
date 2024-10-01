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
public class EvaluateDoseAdministeredConditionTest extends SectionTest {
    
    private List<TableInfo> tableInfo = new ArrayList<>();

    // Constructor to inject the parameters
    public EvaluateDoseAdministeredConditionTest(DataModel model, List<List<LogicResult>> tableResults, LogicStepType nextStep) {
        super(model, tableResults, "EvaluateDoseAdministeredCondition", LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION, nextStep);
        
        tableInfo.add(new TableInfo(
            "TABLE 6-3 CAN THE VACCINE DOSE ADMINISTERED BE EVALUATED?",
            Arrays.asList(
                "Date administered > lot number expiration date?", 
                "Is the dose condition flag 'Y'?"),
            Arrays.asList(
                "No. The vaccine dose administered cannot be evaluated. Target dose status is 'not satisfied.' Evaluation status is 'sub-standard.'",
                "No. The vaccine dose administered cannot be evaluated. Target dose status is 'not satisfied.' Evaluation status is 'sub-standard.'",
                "Yes. The vaccine dose administered can be evaluated."),
            new LogicResult[][]{
                {LogicResult.YES, LogicResult.NO, LogicResult.NO},
                {LogicResult.ANY, LogicResult.YES, LogicResult.NO}
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
