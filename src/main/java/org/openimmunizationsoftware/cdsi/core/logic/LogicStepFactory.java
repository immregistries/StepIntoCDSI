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
    if (stepName.equals(LogicStepType.FOR_EACH_PATIENT_SERIES)) {
      return new ForEachPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED)) {
      return new EvaluateVaccineDoseAdministered(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION)) {
      return new EvaluateDoseAdministeredCondition(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION)) {
      return new EvaluateConditionalSkipForEvaluation(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_AGE)) {
      return new EvaluateAge(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_INTERVAL)) {
      return new EvaluateInterval(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL)) {
      return new EvaluateAllowableInterval(dataModel);
    }

    if (stepName.equals(LogicStepType.EVALUATE_FOR_LIVE_VIRUS_CONFLICT)) {
      return new EvaluateLiveVirusConflict(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_PREFERABLE_VACCINE_ADMINISTERED)) {
      return new EvaluateForPreferableVaccine(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED)) {
      return new EvaluateForAllowableVaccines(dataModel);
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
      return new DetermineEvidenceOfImmunityDose(dataModel);
    }
    if (stepName.equals(LogicStepType.DETERMINE_FORECAST_NEED)) {
      return new DetermineForecastNeed(dataModel);
    }
    if (stepName.equals(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES)) {
      return new GenerateForecastDates(dataModel);
    }

    if (stepName.equals(LogicStepType.SELECT_BEST_PATIENT_SERIES)) {
      return new SelectBestPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.ONE_BEST_PATIENT_SERIES)) {
      return new OneBestPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.CLASSIFY_PATIENT_SERIES)) {
      return new ClassifyPatientSeries(dataModel);
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
    if (stepName.equals(LogicStepType.SELECT_BEST_CANDIDATE_PATIENT_SERIES)) {
      return new SelectBestCandidatePatientSeries(dataModel);
    }

    if (stepName.equals(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP)) {
      return new IdentifyAndEvaluateVaccineGroup(dataModel);
    }
    if (stepName.equals(LogicStepType.CLASSIFY_VACCINE_GROUP)) {
      return new ClassifyVaccineGroup(dataModel);
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
    if (stepName.equals(LogicStepType.FOR_EACH_PATIENT_SERIES)) {
      return new ForEachPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED)) {
      return new EvaluateVaccineDoseAdministered(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION)) {
      return new EvaluateDoseAdministeredCondition(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION)) {
      return new EvaluateConditionalSkipForEvaluation(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_AGE)) {
      return new EvaluateAge(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_INTERVAL)) {
      return new EvaluateInterval(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_ALLOWABLE_INTERVAL)) {
      return new EvaluateAllowableInterval(dataModel);
    }

    if (stepName.equals(LogicStepType.EVALUATE_FOR_LIVE_VIRUS_CONFLICT)) {
      return new EvaluateLiveVirusConflict(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_PREFERABLE_VACCINE_ADMINISTERED)) {
      return new EvaluateForPreferableVaccine(dataModel);
    }
    if (stepName.equals(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED)) {
      return new EvaluateForAllowableVaccines(dataModel);
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
      return new DetermineEvidenceOfImmunityDose(dataModel);
    }
    if (stepName.equals(LogicStepType.DETERMINE_FORECAST_NEED)) {
      return new DetermineForecastNeed(dataModel);
    }
    if (stepName.equals(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES)) {
      return new GenerateForecastDates(dataModel);
    }

    if (stepName.equals(LogicStepType.SELECT_BEST_PATIENT_SERIES)) {
      return new SelectBestPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.ONE_BEST_PATIENT_SERIES)) {
      return new OneBestPatientSeries(dataModel);
    }
    if (stepName.equals(LogicStepType.CLASSIFY_PATIENT_SERIES)) {
      return new ClassifyPatientSeries(dataModel);
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
    if (stepName.equals(LogicStepType.SELECT_BEST_CANDIDATE_PATIENT_SERIES)) {
      return new SelectBestCandidatePatientSeries(dataModel);
    }

    if (stepName.equals(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP)) {
      return new IdentifyAndEvaluateVaccineGroup(dataModel);
    }
    if (stepName.equals(LogicStepType.CLASSIFY_VACCINE_GROUP)) {
      return new ClassifyVaccineGroup(dataModel);
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
    if (stepName.equals(LogicStepType.END)) {
      return new End(dataModel);
    }

    throw new IllegalArgumentException("Step '" + stepName + "' is not yet implemented");
  }
}
