package org.openimmunizationsoftware.cdsi.core.logic;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.Neighborhood;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel;

public class ForecastDatesAndReasons extends LogicStep {
  private static final int MAX_FORECAST_HANDOFF_CYCLES = 1100;

  public ForecastDatesAndReasons(DataModel dataModel) {
    super(LogicStepType.FORECAST_DATES_AND_REASONS, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {

    if (dataModel.getEvaluateForecastTotalCycleCount() > MAX_FORECAST_HANDOFF_CYCLES) {
      alert(LogLevel.CONTROL,
          "ALERT.LOOP_DETECTED: ForecastDatesAndReasons detected excessive forecast/evaluate handoffs ("
              + dataModel.getEvaluateForecastTotalCycleCount() + ") and is forcing SELECT_BEST_PATIENT_SERIES.");
      dataModel.setNeighborhood(Neighborhood.SELECT_BEST_SERIES);
      dataModel.resetEvaluateForecastLoopGuard();
      setNextLogicStepType(LogicStepType.SELECT_BEST_PATIENT_SERIES);
      return next();
    }

    setNextLogicStepType(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST);

    return next();
  }

}
