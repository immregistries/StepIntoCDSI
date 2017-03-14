package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class SingleAntigenVaccineGroup extends LogicStep
{

  // private ConditionAttribute<Date> caDateAdministered = null;

  public SingleAntigenVaccineGroup(DataModel dataModel) {
    super(LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP, dataModel);
    setConditionTableName("Table ");

    // caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");

    // caTriggerAgeDate.setAssumedValue(FUTURE);

    //    conditionAttributesList.add(caDateAdministered);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    List<VaccineGroupForecast> vaccineGroupForecastList = new ArrayList<VaccineGroupForecast>();
    
    Forecast f = new Forecast();
    VaccineGroupForecast vgf = new VaccineGroupForecast();
    vgf.setEarliestDate(f.getEarliestDate());
    
    setNextLogicStepType(LogicStepType.END);
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
    out.println("<h1> " + getTitle() + "</h1>");
    out.println("<p>The forecasting rules which need to be applied to a single antigen vaccine group are listed in the table below</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(0, 0, "Table ?-?");

      //      setLogicCondition(0, new LogicCondition("date administered > lot expiration date?") {
      //        @Override
      //        public LogicResult evaluateInternal() {
      //          if (caDateAdministered.getFinalValue() == null || caTriggerAgeDate.getFinalValue() == null) {
      //            return LogicResult.NO;
      //          }
      //          if (caDateAdministered.getFinalValue().before(caTriggerAgeDate.getFinalValue())) {
      //            return LogicResult.YES;
      //          }
      //          return LogicResult.NO;
      //        }
      //      });

      //      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.ANY);

      //      setLogicOutcome(0, new LogicOutcome() {
      //        @Override
      //        public void perform() {
      //          log("No. The target dose cannot be skipped. ");
      //          log("Setting next step: 4.3 Substitute Target Dose");
      //          setNextLogicStep(LogicStep.SUBSTITUTE_TARGET_DOSE_FOR_EVALUATION);
      //        }
      //      });
      //      
    }
  }

}
