package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class ValidateRecommendation extends EvaluateConditionalSkip {

    public ValidateRecommendation(DataModel dataModel) {
        super(dataModel, ConditionalSkipType.VALIDATING,
                LogicStepType.VALIDATE_RECOMMENDATION,
                LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES,
                LogicStepType.FORECAST_DATES_AND_REASONS);
        setConditionTableName("Table 6.4 Conditional Skip Attributes");

    }

    @Override
    public LogicStep process() throws Exception {

        // setNextLogicStepType(LogicStepType.FORECAST_DATES_AND_REASONS);
        setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
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
    }

}
