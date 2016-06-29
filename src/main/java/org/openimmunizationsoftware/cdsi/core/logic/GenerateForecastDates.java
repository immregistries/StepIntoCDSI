package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class GenerateForecastDates extends LogicStep
{

  private ConditionAttribute<Date> caMinimumAgeDate = null;
  private ConditionAttribute<Date> caEarliestRecommendedAgeDate = null;
  private ConditionAttribute<Date> caLatestRecommendedAgeDate = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<Date> caMinimumIntervalDate = null;
  private ConditionAttribute<Date> caEarliestRecommendedIntervalDate = null;
  private ConditionAttribute<Date> caLatestRecommendedIntervalDate = null;
  private ConditionAttribute<Date> caLatestConflictEndIntervalDate = null;
  private ConditionAttribute<Date> caSeasonalRecommendationStartDate = null;
  private ConditionAttribute<String> caVaccineType = null;
  private ConditionAttribute<String> caForecastVaccineType = null;

  public GenerateForecastDates(DataModel dataModel) {
    super(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES, dataModel);
    setConditionTableName("Table ");

    caMinimumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-4)", "Minimum Age Date");
    caEarliestRecommendedAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-3)",
        "Earliest Recommended Age Date");
    caLatestRecommendedAgeDate = new ConditionAttribute<Date>("Calcualted date (CALCDTAGE-2)",
        "Latest Recommended Age Date");
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)", "Maximum Age Date");
    caMinimumIntervalDate = new ConditionAttribute<Date>("Calcualated date (CALCDTINT-4)", "Minimal Interval Date(s)");
    caEarliestRecommendedIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-5)",
        "Earliest Recommended Interval Date(s)");
    caLatestRecommendedIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-6)",
        "Lastest Recommended Interval Date(s)");
    caLatestConflictEndIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTINT-4)",
        "Latest Conflict End Interval Date");
    caSeasonalRecommendationStartDate = new ConditionAttribute<Date>("Supporting data (Seasonal Recommendation)",
        "Seasonal Recommendation Start Date");
    caVaccineType = new ConditionAttribute<String>("Supporting data (Preferable Vaccine", "Vaccine Type (CVX)");
    caForecastVaccineType = new ConditionAttribute<String>("Supporting data (Preferable Vaccine)",
        "Forecast Vaccine Type");

    caSeasonalRecommendationStartDate.setAssumedValue(PAST);
    caForecastVaccineType.setAssumedValue("N");

    conditionAttributesList.add(caMinimumAgeDate);
    conditionAttributesList.add(caEarliestRecommendedAgeDate);
    conditionAttributesList.add(caLatestRecommendedAgeDate);
    conditionAttributesList.add(caMaximumAgeDate);
    conditionAttributesList.add(caMinimumIntervalDate);
    conditionAttributesList.add(caEarliestRecommendedIntervalDate);
    conditionAttributesList.add(caLatestRecommendedIntervalDate);
    conditionAttributesList.add(caLatestConflictEndIntervalDate);
    conditionAttributesList.add(caSeasonalRecommendationStartDate);
    conditionAttributesList.add(caVaccineType);
    conditionAttributesList.add(caForecastVaccineType);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.FOR_EACH_PATIENT_SERIES);
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
    out.println("<p>TODO</p>");

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
