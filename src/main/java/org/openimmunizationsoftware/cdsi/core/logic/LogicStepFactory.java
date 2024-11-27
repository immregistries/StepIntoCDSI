package org.openimmunizationsoftware.cdsi.core.logic;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class LogicStepFactory {

  public static LogicStep createLogicStep(LogicStepType stepName, DataModel dataModel) {
    if (stepName.equals(LogicStepType.GATHER_NECESSARY_DATA)) {
      return new GatherNecessaryData(dataModel);
    }
    if (stepName.equals(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES)) {
      return new CreateRelevantPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.SELECT_RELEVANT_PATIENT_SERIES)) {
      return new SelectRelevantPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY)) {
      return new OrganizeImmunizationHistory(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES)) {
      return new EvaluateAndForecastAllPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION)) {
      return new EvaluateDoseAdministeredCondition(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION)) {
      return new EvaluateConditionalSkipForEvaluation(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE)) {
      return new EvaluateForInadvertentVaccine(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_AGE)) {
      return new EvaluateAge(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_PREFERABLE_INTERVAL)) {
      return new EvaluatePreferableInterval(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL)) {
      return new EvaluateAllowableInterval(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_VACCINE_CONFLICT)) {
      return new EvaluateVaccineConflict(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_FOR_PREFERABLE_VACCINE)) {
      return new EvaluateForPreferableVaccine(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_FOR_ALLOWABLE_VACCINE)) {
      return new EvaluateForAllowableVaccine(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_GENDER)) {
      return new EvaluateGender(dataModel);
    }
    if (stepName.equals(LogicStepType.SATISFY_TARGET_DOSE)) {
      return new SatisfyTargetDose(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST)) {
      return new EvaluateConditionalSkipForForecast(dataModel);
    }
    if (stepName.equals(LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY)) {
      return new DetermineEvidenceOfImmunity(dataModel);
    }
    if (stepName.equals(LogicStepType.DETERMINE_FORECAST_NEED)) {
      return new DetermineForecastNeed(dataModel);
    }
    if (stepName.equals(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES)) {
      return new GenerateForecastDatesAndRecommendedVaccines(dataModel);
    }
    if (stepName.equals(LogicStepType.SELECT_BEST_PATIENT_SERIES)) {
      return new SelectBestPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.IDENTIFY_ONE_PRIORITIZED_PATIENT_SERIES)) {
      return new IdentifyOnePrioritizedPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES)) {
      return new ClassifyScorablePatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.COMPLETE_PATIENT_SERIES)) {
      return new CompletePatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.NO_VALID_DOSES)) {
      return new NoValidDoses(dataModel);
    }
    if (stepName.equals(LogicStepType.IN_PROCESS_PATIENT_SERIES)) {
      return new InProcessPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES)) {
      return new SelectPrioritizedPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP)) {
      return new IdentifyAndEvaluateVaccineGroup(dataModel);
    }
    if (stepName.equals(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES)) {
      return new ApplyGeneralVaccineGroupRules(dataModel);
    }
    if (stepName.equals(LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP)) {
      return new MultipleAntigenVaccineGroup(dataModel);
    }
    if (stepName.equals(LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP)) {
      return new SingleAntigenVaccineGroup(dataModel);
    }
    if (stepName.equals(LogicStepType.FORECAST_DATES_AND_REASONS)) {
      return new ForecastDatesAndReasons(dataModel);
    }
    if (stepName.equals(LogicStepType.VALIDATE_RECOMMENDATION)) {
      return new ValidateRecommendation(dataModel);
    }
    if (stepName.equals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES)) {
      return new DetermineBestPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.PRE_FILTER_PATIENT_SERIES)) {
      return new PreFilterPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.DETERMINE_CONTRAINDICATIONS)) {
      return new DetermineContraindications(dataModel);
    }
    if (stepName.equals(LogicStepType.END)) {
      return new End(dataModel);
    }

    throw new IllegalArgumentException("Step '" + stepName + "' is not yet implemented");
  }

  public static LogicStep createLogicStep(LogicStepType stepName, DataModel dataModel, boolean b) {
    if (stepName.equals(LogicStepType.GATHER_NECESSARY_DATA)) {
      return new GatherNecessaryData(dataModel);
    }
    if (stepName.equals(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES)) {
      return new CreateRelevantPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY)) {
      return new OrganizeImmunizationHistory(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES)) {
      return new EvaluateAndForecastAllPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION)) {
      return new EvaluateDoseAdministeredCondition(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION)) {
      return new EvaluateConditionalSkipForEvaluation(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_AGE)) {
      return new EvaluateAge(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_PREFERABLE_INTERVAL)) {
      return new EvaluatePreferableInterval(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL)) {
      return new EvaluateAllowableInterval(dataModel);
    }

    if (stepName.equals(LogicStepType.EVALUATE_VACCINE_CONFLICT)) {
      return new EvaluateVaccineConflict(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_FOR_PREFERABLE_VACCINE)) {
      return new EvaluateForPreferableVaccine(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_FOR_ALLOWABLE_VACCINE)) {
      return new EvaluateForAllowableVaccine(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_GENDER)) {
      return new EvaluateGender(dataModel);
    }
    if (stepName.equals(LogicStepType.SATISFY_TARGET_DOSE)) {
      return new SatisfyTargetDose(dataModel);
    }

    if (stepName.equals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST)) {
      return new EvaluateConditionalSkipForForecast(dataModel);
    }
    if (stepName.equals(LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY)) {
      return new DetermineEvidenceOfImmunity(dataModel);
    }
    if (stepName.equals(LogicStepType.DETERMINE_FORECAST_NEED)) {
      return new DetermineForecastNeed(dataModel);
    }
    if (stepName.equals(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES)) {
      return new GenerateForecastDatesAndRecommendedVaccines(dataModel);
    }

    if (stepName.equals(LogicStepType.SELECT_BEST_PATIENT_SERIES)) {
      return new SelectBestPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.IDENTIFY_ONE_PRIORITIZED_PATIENT_SERIES)) {
      return new IdentifyOnePrioritizedPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES)) {
      return new ClassifyScorablePatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.COMPLETE_PATIENT_SERIES)) {
      return new CompletePatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.NO_VALID_DOSES)) {
      return new NoValidDoses(dataModel);
    }
    if (stepName.equals(LogicStepType.IN_PROCESS_PATIENT_SERIES)) {
      return new InProcessPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES)) {
      return new SelectPrioritizedPatientSeries(dataModel);
    }

    if (stepName.equals(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP)) {
      return new IdentifyAndEvaluateVaccineGroup(dataModel);
    }
    if (stepName.equals(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES)) {
      return new ApplyGeneralVaccineGroupRules(dataModel);
    }
    if (stepName.equals(LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP)) {
      return new MultipleAntigenVaccineGroup(dataModel);
    }
    if (stepName.equals(LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP)) {
      return new SingleAntigenVaccineGroup(dataModel);
    }
    if (stepName.equals(LogicStepType.FORECAST_DATES_AND_REASONS)) {
      return new ForecastDatesAndReasons(dataModel);
    }
    if (stepName.equals(LogicStepType.VALIDATE_RECOMMENDATION)) {
      return new ValidateRecommendation(dataModel);
    }
    if (stepName.equals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES)) {
      return new DetermineBestPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.PRE_FILTER_PATIENT_SERIES)) {
      return new PreFilterPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.END)) {
      return new End(dataModel);
    }

    throw new IllegalArgumentException("Step '" + stepName + "' is not yet implemented");
  }
}
