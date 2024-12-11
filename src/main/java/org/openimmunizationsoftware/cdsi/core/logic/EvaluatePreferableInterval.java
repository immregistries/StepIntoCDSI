package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_3;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTINT_4;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluatePreferableInterval extends LogicStep {

  public EvaluatePreferableInterval(DataModel dataModel) {
    super(LogicStepType.EVALUATE_PREFERABLE_INTERVAL, dataModel);
    setConditionTableName("Table ");

    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();

    int intervalCount = 0;
    for (Interval interval : seriesDose.getIntervalList()) {
      intervalCount++;
      LT logicTable = new LT();

      logicTable.caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
      logicTable.caPreferableIntervalElements = new ConditionAttribute<Interval>("Supporting Data",
          "Preferable Interval Elements");
      logicTable.caAbsoluteMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date",
          "Absolute Minimum Interval Date");
      logicTable.caMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date", "Mimium Interval Date");

      logicTable.caAbsoluteMinimumIntervalDate.setAssumedValue(PAST);
      logicTable.caMinimumIntervalDate.setAssumedValue(PAST);

      List<ConditionAttribute<?>> caList = new ArrayList<ConditionAttribute<?>>();
      caList.add(logicTable.caDateAdministered);
      caList.add(logicTable.caPreferableIntervalElements);
      caList.add(logicTable.caAbsoluteMinimumIntervalDate);
      caList.add(logicTable.caMinimumIntervalDate);
      conditionAttributesAdditionalMap.put("Interval Check #" + intervalCount,
          caList);

      AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
      logicTable.caDateAdministered.setInitialValue(aar.getDateAdministered());
      logicTable.caPreferableIntervalElements
          .setInitialValue(interval);
      logicTable.caAbsoluteMinimumIntervalDate
          .setInitialValue(CALCDTINT_3.evaluate(dataModel, this, interval));
      logicTable.caMinimumIntervalDate.setInitialValue(CALCDTINT_4.evaluate(dataModel, this, interval));

      logicTableList.add(logicTable);

    }
  }

  @Override
  public LogicStep process() throws Exception {
    YesNo satisfiedAll = YesNo.YES;
    for (LogicTable logicTable : logicTableList) {
      logicTable.evaluate();
      if (((LT) logicTable).getResult() == YesNo.NO) {
        satisfiedAll = YesNo.NO;
      }
    }
    if (satisfiedAll == YesNo.YES) {
      setNextLogicStepType(LogicStepType.EVALUATE_VACCINE_CONFLICT);
    } else {
      setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL);
    }
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
    out.println(
        "<p>Evaluate interval validates the date administered of a vaccine dose administered against defined interval(s) from previous vaccine dose(s) administered. In cases where a target dose does not specify interval attributes, the interval is considered \"valid.\"</p>");
    out.println("<p>Intervals can be measures in three different ways:</p>");
    out.println("<ul>");
    out.println(
        "  <li>\"From Immediate Previous Dose Administered\" requires the interval to be evaluated from the immediate previous vaccine dose administered and is used in the majority of cases.</li>");
    out.println(
        "  <li>\"From Target Dose # in Series\" requires the interval to be evaluated from the date of the specified dose. </li>");
    out.println(
        "  <li>\"From Most Recent\" requires the interval to be evaluated from the date of the most recently administered dose of a specific vaccine type (e.g., this is used in Pneumococcal to ensure proper spacing between the different intervals between PCV13 and PPSV23).</li>");
    out.println("</ul>");
    out.println(
        "<p>It is possible for a given dose to use multiple interval types. For example, dose 3 of HepB and dose 3 of HPV, each have two intervals.  The first interval is from the immediate previous vaccine dose administered.  The second interval is from satisfied target dose 1 in each respective series. Note that if multiple intervals are specified, then all intervals must be satisfied in order for the dose to satisfy the interval requirements.</p>");
    out.println(
        "<p>Figure 4-6 provides the evaluation interval timeline used to define adjacent intervals by using from immediate previous dose administered as the reference point.</p>");
    out.println("<img src=\"Figure 4.6.PNG\"/>");
    out.println("<p>FIGURE 4 - 6 EVALUATE INTERVAL 'FROM IMMEDIATE PREVIOUS DOSE' TIMELINE</p>");
    out.println("<img src=\"Figure 4.7.png\"/>");
    out.println(
        "<p>FIGURE 4 - 7 EVALUATE INTERVAL 'FROM TARGET DOSE NUMBER IN SERIES' TIMELINE</p>");
    out.println("<img src=\"Figure 4.8.PNG\"/>");
    out.println(
        "<p>FIGURE 4 - 8 EVALUATE INTERVAL ‘FROM MOST RECENT DOSE OF SPECIFIED VACCINE TYPE’ TIMELINE</p>");
    out.println("<img src=\"Figure 4.9.png\"/>");
    out.println("<p>FIGURE 4 - 9 EVALUATE INTERVAL PROCESS MODEL</p>");
    out.println("<h2>Intervals</h2>");
    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    if (seriesDose.getIntervalList().size() == 0) {
      out.println("<p>No intervals for series dose " + seriesDose + "</p>");
    } else {
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>From Immediate Previous Dose Administered?</th>");
      out.println("    <th>From Target Dose # in Series</th>");
      out.println("    <th>From Most Recent</th>");
      out.println("    <th>Absolute Minimum Interval</th>");
      out.println("    <th>Minimum Interval/th>");
      out.println("    <th>Earliest Recommended Interval</th>");
      out.println("    <th>Latest Recommended Interval (less than)</th>");
      out.println("    <th>Interval Priority Flag</th>");
      out.println("  </tr>");
      for (Interval interval : seriesDose.getIntervalList()) {
        out.println("  <tr>");
        out.println("    <td>" + interval.getFromImmediatePreviousDoseAdministered() + "</td>");
        out.println("    <td>" + interval.getFromTargetDoseNumberInSeries() + "</td>");
        out.println("    <td>?" + "</td>");
        out.println("    <td>" + interval.getAbsoluteMinimumInterval() + "</td>");
        out.println("    <td>" + interval.getMinimumInterval() + "</td>");
        out.println("    <td>" + interval.getEarliestRecommendedInterval() + "</td>");
        out.println("    <td>" + interval.getLatestRecommendedInterval() + "</td>");
        out.println("    <td>?" + "</td>");
        out.println("  </tr>");
      }

      out.println("</table>");
    }

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {

    private ConditionAttribute<Date> caDateAdministered = null;
    private ConditionAttribute<Interval> caPreferableIntervalElements = null;
    private ConditionAttribute<Date> caAbsoluteMinimumIntervalDate = null;
    private ConditionAttribute<Date> caMinimumIntervalDate = null;

    private YesNo result = null;

    public YesNo getResult() {
      return result;
    }

    public LT() {

      super(3, 3, "Table 6 - 18 Did the vaccine dose administered satisfy the defined interval?");

      setLogicCondition(0,
          new LogicCondition("Is the date administered < absolute minimum interval date?") {
            @Override
            public LogicResult evaluateInternal() {
              if (caDateAdministered.getFinalValue()
                  .before(caAbsoluteMinimumIntervalDate.getFinalValue())) {
                return LogicResult.YES;
              }
              return LogicResult.NO;
            }
          });

      setLogicCondition(1, new LogicCondition(
          "Absolute minimum interval date <= date administered < minimum interval date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caAbsoluteMinimumIntervalDate.getFinalValue()
              .after(caDateAdministered.getFinalValue())) {
            return LogicResult.NO;
          }
          if (caDateAdministered.getFinalValue().before(caMinimumIntervalDate.getFinalValue())) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(2, new LogicCondition("Minimum interval date <= date administered?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caDateAdministered.getFinalValue().before(caMinimumIntervalDate.getFinalValue())) {
            return LogicResult.NO;
          }
          return LogicResult.YES;
        }
      });

      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO);
      setLogicResults(1, LogicResult.NO, LogicResult.YES, LogicResult.NO);
      setLogicResults(2, LogicResult.NO, LogicResult.NO, LogicResult.YES);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The vaccine dose administered did not satisfy the preferable interval for the target dose. Evaluation reason is 'Too Soon'.");
          Evaluation evaluation = dataModel.getTargetDose().getEvaluation();
          evaluation.setEvaluationReason(EvaluationReason.GRACE_PERIOD);
          result = YesNo.NO;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The vaccine dose administered satisfied the preferable interval for the target dose. Evaluation reason is 'Grace Period'.");
          Evaluation evaluation = dataModel.getTargetDose().getEvaluation();
          evaluation.setEvaluationReason(EvaluationReason.GRACE_PERIOD);
          result = YesNo.YES;
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The vaccine dose administered satisfied the preferable interval for the target dose.");
          result = YesNo.YES;
        }
      });

    }
  }

}
