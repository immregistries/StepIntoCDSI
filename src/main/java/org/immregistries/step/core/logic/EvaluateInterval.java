package org.immregistries.step.core.logic;

import static org.immregistries.step.core.logic.concepts.DateRules.CALCDTINT_3;
import static org.immregistries.step.core.logic.concepts.DateRules.CALCDTINT_4;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.immregistries.step.core.data.DataModel;
import org.immregistries.step.core.domain.datatypes.TargetDoseStatus;
import org.immregistries.step.core.domain.datatypes.YesNo;
import org.immregistries.step.core.logic.items.ConditionAttribute;
import org.immregistries.step.core.logic.items.LogicCondition;
import org.immregistries.step.core.logic.items.LogicOutcome;
import org.immregistries.step.core.logic.items.LogicResult;
import org.immregistries.step.core.logic.items.LogicTable;
import org.immregistries.step.domain.AntigenAdministeredRecord;
import org.immregistries.step.domain.Interval;
import org.immregistries.step.domain.SeriesDose;

public class EvaluateInterval extends LogicStep {



  public EvaluateInterval(DataModel dataModel) {
    super(LogicStepType.EVALUATE_PREFERABLE_INTERVAL, dataModel);
    setConditionTableName("Table ");

    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();



    int intervalCount = 0;
    // if (seriesDose.getIntervalList().size() > 0) {
    for (Interval interval : seriesDose.getIntervalList()) {
      intervalCount++;
      LT logicTable = new LT();

      logicTable.caDateAdministered =
          new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
      logicTable.caFromImmediatePreviousDoseAdministered = new ConditionAttribute<YesNo>(
          "Supporting Data", "From Immediate Previous Dose Administered");
      logicTable.caFromTargetDoseNumberInSeries =
          new ConditionAttribute<String>("Supporting Data", "From Target Dose Number In Series");
      logicTable.caFromMostRecent =
          new ConditionAttribute<String>("Supporting Data (Interval)", "From Most Recent (CVX)");
      logicTable.caAbsoluteMinimumIntervalDate =
          new ConditionAttribute<Date>("Calculated Date", "Absolute Minimum Interval Date");
      logicTable.caMinimumIntervalDate =
          new ConditionAttribute<Date>("Calculated Date", "Mimium Interval Date");

      logicTable.caAbsoluteMinimumIntervalDate.setAssumedValue(PAST);
      logicTable.caMinimumIntervalDate.setAssumedValue(PAST);

      List<ConditionAttribute<?>> caList = new ArrayList<ConditionAttribute<?>>();
      caList.add(logicTable.caDateAdministered);
      caList.add(logicTable.caFromImmediatePreviousDoseAdministered);
      caList.add(logicTable.caFromTargetDoseNumberInSeries);
      caList.add(logicTable.caFromMostRecent);
      caList.add(logicTable.caAbsoluteMinimumIntervalDate);
      caList.add(logicTable.caMinimumIntervalDate);
      conditionAttributesAdditionalMap.put("Interval Check #" + intervalCount,
          caList);

      AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
      logicTable.caDateAdministered.setInitialValue(aar.getDateAdministered());
      logicTable.caFromImmediatePreviousDoseAdministered
          .setInitialValue(interval.getFromImmediatePreviousDoseAdministered());
      logicTable.caFromTargetDoseNumberInSeries
          .setInitialValue(interval.getFromTargetDoseNumberInSeries());
      logicTable.caAbsoluteMinimumIntervalDate
          .setInitialValue(CALCDTINT_3.evaluate(dataModel, this, null));
      logicTable.caMinimumIntervalDate.setInitialValue(CALCDTINT_4.evaluate(dataModel, this, null));

      logicTableList.add(logicTable);

    }
  }
  // }

  @Override
  public LogicStep process() throws Exception {
    YesNo result = YesNo.YES;
    for (LogicTable logicTable : logicTableList) {
      logicTable.evaluate();
      if (((LT) logicTable).getResult() == YesNo.NO) {
        result = YesNo.NO;
      }
    }
    if (result == YesNo.YES) {
      setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL);
    } else {
      setNextLogicStepType(LogicStepType.EVALUATE_FOR_LIVE_VIRUS_CONFLICT);
      dataModel.getTargetDose()
          .setStatusCause(dataModel.getTargetDose().getStatusCause() + "Interval");
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
    out.println("<h1> " + getTitle() + "</h1>");
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
    private ConditionAttribute<YesNo> caFromImmediatePreviousDoseAdministered = null;
    private ConditionAttribute<String> caFromTargetDoseNumberInSeries = null;
    private ConditionAttribute<String> caFromMostRecent = null;
    private ConditionAttribute<Date> caAbsoluteMinimumIntervalDate = null;
    private ConditionAttribute<Date> caMinimumIntervalDate = null;


    private YesNo result = null;

    public YesNo getResult() {
      return result;
    }

    public LT() {

      super(5, 5, "Table 4 - 14 Did the vaccine dose administered satisfy the defined interval?");

      setLogicCondition(0,
          new LogicCondition("Absolute minimum interval date > date administered?") {
            @Override
            public LogicResult evaluateInternal() {
              if (caAbsoluteMinimumIntervalDate.getFinalValue()
                  .after(caDateAdministered.getFinalValue())) {
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

      setLogicCondition(3, new LogicCondition("Is this the first target dose?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caFromTargetDoseNumberInSeries.getFinalValue().equals(String.valueOf(1))) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(4, new LogicCondition(
          "Is the evaluation status of the previous vaccine dose administered \"not valid\" due to age or interval recommendations? ") {
        @Override
        public LogicResult evaluateInternal() {
          if (dataModel.getTargetDose().getTargetDoseStatus() == TargetDoseStatus.NOT_SATISFIED)
            return LogicResult.YES;
          return LogicResult.NO;
        }
      });

      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.NO,
          LogicResult.NO);
      setLogicResults(1, LogicResult.NO, LogicResult.YES, LogicResult.YES, LogicResult.YES,
          LogicResult.NO);
      setLogicResults(2, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.NO,
          LogicResult.YES);
      setLogicResults(3, LogicResult.ANY, LogicResult.NO, LogicResult.NO, LogicResult.YES,
          LogicResult.ANY);
      setLogicResults(4, LogicResult.ANY, LogicResult.YES, LogicResult.NO, LogicResult.ANY,
          LogicResult.ANY);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("No. The vaccine dose administered did not satisfy the defined interval.  Evaluation Reason is \"too soon.\"");
          result = YesNo.NO;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("No. The vaccine dose administered did not satisfy the defined interval.  Evaluation Reason is \"too soon.\"");
          result = YesNo.NO;
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("Yes. The vaccine dose administered satisfied the defined interval.  Evaluation Reason is \"grace period.\"");
          result = YesNo.YES;
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("Yes. The vaccine dose administered satisfied the defined interval.  Evaluation Reason is \"grace period.\"");
          result = YesNo.YES;
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("Yes. The vaccine dose administered did satisfy the defined interval.");
          result = YesNo.YES;
        }
      });

    }
  }

}
