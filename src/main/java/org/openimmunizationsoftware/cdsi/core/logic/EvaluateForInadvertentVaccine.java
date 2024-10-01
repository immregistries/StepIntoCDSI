package org.openimmunizationsoftware.cdsi.core.logic;
// Created by Nicole on 8/13/24.

// Importing modules

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;


public class EvaluateForInadvertentVaccine extends LogicStep {
    // Initialization of attributes
    protected ConditionAttribute<VaccineType> caVaccineDoseAdministered = null;
    protected ConditionAttribute<VaccineType> caInadvertentVaccine = null;

    // Constructor
    public EvaluateForInadvertentVaccine(DataModel dataModel){
        super(LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE, dataModel);
        setConditionTableName("Table 6-12 Inadvertent Vaccine Attributes");

        // Defining values?
        caVaccineDoseAdministered = new ConditionAttribute<VaccineType>("Vaccine dose administered","Vaccine Type");
        caInadvertentVaccine = new ConditionAttribute<VaccineType>("Supporting Data (inadvertent vaccine)","Vaccine Type");

        // Setting initial values
        caVaccineDoseAdministered.setInitialValue(aar.getVaccineDoseAdministered);

        conditionAttributesList.add(caVaccineDoseAdministered);
        conditionAttributesList.add(caInadvertentVaccine);
        
        LT logicTable = new LT();
        logicTableList.add(logicTable);

        // Placeholder values
        List<VaccineType> placeholderList = new ArrayList<>();
        VaccineType one = new VaccineType();
        VaccineType two = new VaccineType();
        VaccineType three = new VaccineType();
        one.setCvxCode("36");
        two.setCvxCode("72");
        three.setCvxCode("108");
        placeholderList.addAll(one,two,three);
        caInadvertentVaccineList.setInitialValue(placeholderList); 
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

    private class LT extends LogicTable{    
        public LT(){
            super(1,1,"Table 6-13 Was the Vaccine Dose Administered an Inadvertent Administration for the Target Dose?");

            // Logic
            setLogicCondition(0, new LogicCondition("Is the vaccine type of the vaccine dose administered one of the vaccine types of an inadvertent vaccine for the target dose?") {
                @Override
                public LogicResult evaluateInternal(){
                    if (caInadvertentVaccineList.contains(caVaccineDoseAdministered)){
                        return LogicResult.YES;
                    }
                    else{
                        return LogicResult.NO;
                    }
                }
            });   
    
            setLogicResults(0, new LogicResult[] {LogicResult.YES, LogicResult.NO});
            // If yes:
            setLogicOutcome(0, new LogicOutcome() {
                @Override
                public void perform() {
                    log("Yes. The vaccine dose administered was an inadvertent administration for the target dose. Target Dose Status is 'Not Satisfied'. Evaluation Status is 'Not Valid'. Evaluation Reason is 'Inadvertent Administration'.");
                    setNextLogicStepType(LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION);
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