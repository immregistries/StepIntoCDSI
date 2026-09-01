package org.openimmunizationsoftware.cdsi.core.logic;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class EvaluateConditionalSkipForForecast extends EvaluateConditionalSkip {

  public EvaluateConditionalSkipForForecast(DataModel dataModel) {
    super(dataModel, ConditionalSkipType.FORECAST,
        LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST,
        LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY,
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
    setConditionTableName("Table 6.4 Conditional Skip Attributes");
  }

}
