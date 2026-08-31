package org.openimmunizationsoftware.cdsi.core.logic;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class ValidateRecommendation extends EvaluateConditionalSkip {

    public ValidateRecommendation(DataModel dataModel) {
        super(dataModel, ConditionalSkipType.VALIDATING,
                LogicStepType.VALIDATE_RECOMMENDATION,
                LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES,
                LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST);
        setConditionTableName("Table 6.4 Conditional Skip Attributes");

    }

    @Override
    public LogicStep process() throws Exception {
        // setNextLogicStepType(LogicStepType.FORECAST_DATES_AND_REASONS);
        setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
        return next();
    }

}
