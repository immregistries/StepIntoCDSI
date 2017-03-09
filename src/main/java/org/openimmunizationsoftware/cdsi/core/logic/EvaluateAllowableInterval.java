package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_3;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableInterval;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateAllowableInterval extends LogicStep {

  private ConditionAttribute<Date> caDateAdministered = null;
  private ConditionAttribute<YesNo> caFromImmediatePreviousDoseAdministered = null;
  private ConditionAttribute<String> caFromTargetDoseNumberInSeries = null;
  private ConditionAttribute<Date> caAbsoluteMinimumIntervalDate = null;

  public EvaluateAllowableInterval(DataModel dataModel) {
    super(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL, dataModel);
    setConditionTableName("Table ");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caFromImmediatePreviousDoseAdministered = new ConditionAttribute<YesNo>("Supporting Data",
        "From Immediate Previous Dose Administered");
    caFromTargetDoseNumberInSeries = new ConditionAttribute<String>("Supporting Data",
        "From Target Dose Number In Series");
    caAbsoluteMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date", "Absolute Minimum Interval Date");

    caAbsoluteMinimumIntervalDate.setAssumedValue(PAST);

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caFromImmediatePreviousDoseAdministered);
    conditionAttributesList.add(caFromTargetDoseNumberInSeries);
    conditionAttributesList.add(caAbsoluteMinimumIntervalDate);

    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    
    if (seriesDose.getAllowableintervalList().size() > 0) {
      for (AllowableInterval ainterval : seriesDose.getAllowableintervalList()) {
        caFromImmediatePreviousDoseAdministered.setInitialValue(ainterval.getFromImmediatePreviousDoseAdministered());
        caFromTargetDoseNumberInSeries.setInitialValue(ainterval.getFromTargetDoseNumberInSeries());
        caAbsoluteMinimumIntervalDate.setInitialValue(CALCDTINT_3.evaluate(dataModel, this, null));

        LT logicTable = new LT();
        logicTableList.add(logicTable);
      }
    }
    // caMinimumIntervalDate.setInitialValue(CALCDTINT_4.evaluate(dataModel,
    // this));

  }

  @Override
  public LogicStep process() throws Exception {
	  //System.out.println(logicTableList.size());
	  YesNo y = YesNo.YES;
    for (LogicTable logicTable : logicTableList) {
      logicTable.evaluate();
      if (((LT) logicTable).getResult() == YesNo.NO) {
    	  y=YesNo.NO;
      }
    }
    setNextLogicStepType(LogicStepType.EVALUATE_FOR_LIVE_VIRUS_CONFLICT);

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
    out.println("<p>Evaluate allowable interval validates the date administered of a vaccine dose administered against defined allowable interval(s) from previous vaccine dose(s) administered.  In rare cases, intervals can be applied which are either abnormally early – usually specified in ACIP footnotes or subsequent clarifications – or intervals which differ following a not valid administration.</p>"); 
    out.println("<p>In cases where a target dose does not specify allowable interval attributes, evaluate allowable interval cannot be used to validate a vaccine dose administered.  To avoid a false validation, the allowable interval should be considered “not valid” in these cases.</p>");
    out.println("<p>The figure below provides evaluate allowable interval timeline used to define all adjacent intervals by using from immediate previous dose administered as the reference dose.</p>");
    out.println("<img src=\"Figure 4.10.PNG\"/>");
    out.println("<p>FIGURE 4 - 10 EVALUATE ALLOWABLE INTERVAL 'FROM IMMEDIATE PREVIOUS DOSE' TIMELINE</p>");
    out.println("<p>The figure below illustrates evaluate allowable interval timeline used to define all non-adjacent intervals by using from target dose number in series as the reference dose.</p>");
    out.println("<img src=\"Figure 4.11.PNG\"/>");
    out.println("<p>FIGURE 4 - 11 EVALUATE ALLOWABLE INTERVAL 'FROM TARGET DOSE NUMBER IN SERIES' TIMELINE</p>");
    out.println("<p>The following process model, attribute table, decision table, and business rule table are used to evaluate interval of a vaccine dose administered.</p>");
    out.println("<img src=\"Figure 4.12.PNG\"/>");
    out.println("<p>FIGURE 4 - 12 EVALUATE ALLOWABLE INTERVAL PROCESS MODEL</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    private YesNo result = null;

    public LT() {
      super(1, 2, "Table 4 - 17 Did the vaccine dose administered satisfy the defined Allowable interval?");

      setLogicCondition(0, new LogicCondition("date administered < Absolute minimum interval date?") {
        @Override
        public LogicResult evaluateInternal() {
        
          if (caAbsoluteMinimumIntervalDate.getFinalValue().after(caDateAdministered.getFinalValue())) {
            return LogicResult.YES;
          }
          // if
          // (caDateAdministered.getFinalValue().before(caMinimumIntervalDate.getFinalValue()))
          // {
          // return LogicResult.NO;
          // }
          return LogicResult.NO;
        }
      });

      setLogicResults(0, LogicResult.YES, LogicResult.NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The vaccine dose administered did not satisfy the defined allowable interval.  Evaluation Reason is \" too soon. \" ");
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
          dataModel.getTargetDose().setStatusCause(dataModel.getTargetDose().getStatusCause()+"Interval");
          result = YesNo.NO;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The vaccine dose administered did satisfy the defined allowable interval. ");
          result = YesNo.YES;
        }
      });

    }

    public YesNo getResult() {
      // TODO Auto-generated method stub
      return result;
    }
  }

}
