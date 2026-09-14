package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class ApplyGeneralVaccineGroupRules extends LogicStep {

  // private ConditionAttribute<Date> caDateAdministered = null;

  public ApplyGeneralVaccineGroupRules(DataModel dataModel) {
    super(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES, dataModel);
    LT logicTable = new LT();
    logicTable.setLogicStepSink(this.getLogicStepSink());
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    evaluateLogicTables();
    return next();
  }

  private class LT extends LogicTable {
    public LT() {
      super(1, 2, "Table 9-2 General Vaccine Group Business Rules");

      setLogicCondition(0, new LogicCondition("Does the vaccine group contain exactly 1 antigen?") {
        public LogicResult evaluateInternal() {
          if (dataModel.getVaccineGroup().getAntigenList().size() == 1) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });
      setLogicResults(0, YES, NO);


      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          setNextLogicStepType(LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP);
          log("Vaccine group is a single antigen vaccine group.");
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Vaccine group is a multiple antigen vaccine group.");
          setNextLogicStepType(LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP);
        }
      });

    }
  }

}
