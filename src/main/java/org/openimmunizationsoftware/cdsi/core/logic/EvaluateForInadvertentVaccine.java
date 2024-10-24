package org.openimmunizationsoftware.cdsi.core.logic;
// Created by Nicole on 8/13/24.

// Importing modules

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;


public class EvaluateForInadvertentVaccine extends LogicStep {
    // Initialization of attributes
    protected ConditionAttribute<VaccineDoseAdministered> caVaccineDoseAdministered = null;
    protected ConditionAttribute<VaccineDoseAdministered> caInadvertentVaccine = null;
    protected List<VaccineType> caInadvertentVaccineList = new ArrayList<>();

    // Constructor
    public EvaluateForInadvertentVaccine(DataModel dataModel){
        super(LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE, dataModel);
        setConditionTableName("Table 6-12 Inadvertent Vaccine Attributes");

        // Defining values?
        caVaccineDoseAdministered = new ConditionAttribute<VaccineDoseAdministered>("Vaccine dose administered","Vaccine Type");
        caInadvertentVaccine = new ConditionAttribute<VaccineDoseAdministered>("Supporting Data (inadvertent vaccine)","Vaccine Type");

        // Setting initial values
        AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
        caVaccineDoseAdministered.setInitialValue(aar.getVaccineDoseAdministered());

        conditionAttributesList.add(caVaccineDoseAdministered);
        conditionAttributesList.add(caInadvertentVaccine);
        
        LT logicTable = new LT();
        logicTableList.add(logicTable);
    }

    @Override
    public LogicStep process() throws Exception{ 
        setNextLogicStepType(LogicStepType.EVALUATE_AGE);
        evaluateLogicTables();
        return next();
    }

    
    @Override
    public void printPre(PrintWriter out) throws Exception {
      printStandard(out);
    }
  
    @Override
    public void printPost(PrintWriter out) throws Exception {
      printStandard(out);
    }

    private void printStandard(PrintWriter out) {
        
    }


    private class LT extends LogicTable{    
        public LT(){
            super(1,2,"Table 6-13 Was the Vaccine Dose Administered an Inadvertent Administration for the Target Dose?");

            // Logic
            setLogicCondition(0, new LogicCondition("Is the vaccine type of the vaccine dose administered one of the vaccine types of an inadvertent vaccine for the target dose?") {
                @Override
                public LogicResult evaluateInternal(){
                    for (VaccineType iv : dataModel.getTargetDose().getTrackedSeriesDose().getInadvertentVaccineList()) {
                        if (iv.equals(caVaccineDoseAdministered.getFinalValue().getVaccine().getVaccineType())){
                            return LogicResult.YES;
                        }
                    }

                    return LogicResult.NO;
                    
                }
            });   
    
            setLogicResults(0, new LogicResult[] {LogicResult.YES, LogicResult.NO});
            // If yes:
            setLogicOutcome(0, new LogicOutcome() {
                @Override
                public void perform() {
                    log("Yes. The vaccine dose administered was an inadvertent administration for the target dose. Target Dose Status is 'Not Satisfied'. Evaluation Status is 'Not Valid'. Evaluation Reason is 'Inadvertent Administration'.");
                    setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
                }
            });

            // If no:
            setLogicOutcome(1, new LogicOutcome() {
                @Override
                public void perform() {
                log("No. The vaccine dose administered was not an inadvertent administration for the target dose.");
                setNextLogicStepType(LogicStepType.EVALUATE_AGE);
                }
            });
        }
    }
}