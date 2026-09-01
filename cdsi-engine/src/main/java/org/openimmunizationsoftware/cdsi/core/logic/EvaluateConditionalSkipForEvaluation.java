package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;

public class EvaluateConditionalSkipForEvaluation extends EvaluateConditionalSkip {

  // Creating null attributes for use later
  protected ConditionAttribute<Date> caDateAdministered = null;
  protected ConditionAttribute<Integer> caAdministeredDoseCount = null;
  protected ConditionAttribute<Date> caAssessmentDate = null;
  protected ConditionAttribute<Date> caEarliestDate = null;

  protected boolean isForecast;
  protected boolean isValidating;

  protected LogicStepType noSkipLogicStep;
  protected LogicStepType skipLogicStep;

  // Constructor 1
  protected EvaluateConditionalSkipForEvaluation(DataModel dataModel) {
    super(dataModel, ConditionalSkipType.EVALUATE, LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION,
        LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE, LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
    setConditionTableName("Table 6.4 Conditional Skip Attributes");
  }

}
