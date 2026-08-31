package org.openimmunizationsoftware.cdsi.core.logic;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class SkipTargetDoseForForecast extends LogicStep {

  public SkipTargetDoseForForecast(DataModel dataModel) {
    super(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY);
    return next();
  }

}
