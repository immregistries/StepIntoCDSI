package org.immregistries.step.core.logic;

public enum LogicStepType {

                           GATHER_NECESSARY_DATA("Gather Necessary Data",
                               "4.1 Gather Necessary Data", false),
                           ORGANIZE_IMMUNIZATION_HISTORY("Organize Immunization History",
                               "4.2 Organize Immunization History", false),
                           CREATE_RELEVANT_PATIENT_SERIES("Create Relevant Patient Series",
                               "4.3 Create Relevant Patient Series", false),
                           EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES(
                               "Evaluate and Forecast all Patient Series",
                               "4.4 Evaluate and Forecast all Patient Series", false),
                           SELECT_PATIENT_SERIES("Select Patient Series",
                               "4.5 Select Patient Series", false),
                           IDENTIFY_AND_EVALUATE_VACCINE_GROUP(
                               "Identify and Evaluate Vaccine Group",
                               "4.6 Identify and Evaluate Vaccine Group", false),

                           FOR_EACH_PATIENT_SERIES("for each Patient Series",
                               "for each Patient Series", true),
                           EVALUATE_VACCINE_DOSE_ADMINISTERED("Evaluate Vaccine Dose Administered",
                               "6 Evaluate Vaccine Dose Administered", true),
                           EVALUATE_DOSE_ADMININISTERED_CONDITION(
                               "Evaluate Dose Administered Condition",
                               "6.1 Evaluate Dose Administered Condition", true),
                           EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION("Evaluate Conditional Skip",
                               "6.2 Evaluate Conditional Skip", true),
                           EVALUATE_INADVERTANT_VACCINE("Evaluate for Inadvertent Vaccine",
                               "6.3 Evaluate for Inadvertent Vaccine", true),
                           EVALUATE_AGE("Evaluate Age", "6.4 Evaluate Age", true),
                           EVALUATE_PREFERABLE_INTERVAL("Evaluate Perferable Interval",
                               "6.5 Evaluate Preferable Interval", true),
                           EVALUATE_ALLOWABLE_INTERVAL("Evaluate Allowable Interval",
                               "6.6 Evaluate Allowable Interval", true),
                           EVALUATE_FOR_LIVE_VIRUS_CONFLICT("Evaluate for Live Virus Conflict",
                               "6.7 Evaluate for Live Virus Conflict", true),
                           EVALUATE_PREFERABLE_VACCINE("Evaluate Preferable Vaccine",
                               "6.8 Evaluate Preferable Vaccine", true),
                           EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED("Evaluate Allowable Vaccine",
                               "6.9 Evaluate Allowable Vaccine", true),
                           EVALUATE_GENDER("Evaluate Gender", "X Evaluate Gender", true),
                           SATISFY_TARGET_DOSE("Satisfy Target Dose", "6.10 Satisfy Target Dose",
                               true),
                           FORECAST_DATES_AND_REASONS("Forecast Dates and Reasons",
                               "7 Forecast Dates and Reasons", true),
                           EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST("Evaluate Conditional Skip",
                               "7.1 Evaluate Conditional Skip", true),
                           DETERMINE_EVIDENCE_OF_IMMUNITY("Determine Evidence of Immunity",
                               "7.2 Determine Evidence of Immunity", true),
                           DETERMINE_CONTRAINDICATIONS("Determine Contraindications",
                               "7.3 Determine Contraindications", true),
                           DETERMINE_FORECAST_NEED("Determine Forecast Need",
                               "7.4 Determine Forecast Need", true),
                           GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES(
                               "Generate Forecast Dates and Recommend Vaccines",
                               "7.5 Generate Forecast Dates and Recommend Vaccines", true),
                           VALIDATE_RECOMMENDATION(
                               "Validate Recommendation",
                               "7.6 Validate Recommendation", true),
                           PRE_FILTER_PATIENT_SERIES("Pre-filter Patient Series",
                               "8.1 Pre-filter Patient Series", true),
                           IDENTIFY_ONE_PRIORITIZED_PATIENT_SERIES("Identify One Prioritized Patient Series",
                               "8.2 Identify One Prioritized Patient Series", true),
                           CLASSIFY_SCORABLE_PATIENT_SERIES("Classify Scorable Patient Series",
                               "8.3 Classify Scorable Patient Series", true),
                           COMPLETE_PATIENT_SERIES("Complete Patient Series",
                               "8.4 Complete Patient Series", true),
                           IN_PROCESS_PATIENT_SERIES("In-Process Patient Series",
                               "8.5 In-Process Patient Series", true),
                           NO_VALID_DOSES("No Valid Doses", "8.6 No Valid Doses", true),
                           SELECT_PRIORITIZED_PATIENT_SERIES(
                               "Select Prioritized Patient Series",
                               "8.7 Select Prioritized Patient Series", true),
                           DETERMINE_BEST_PATIENT_SERIES(
                               "Determine Best Patient Series",
                               "8.8 Determine Best Patient Series", true),
                           CLASSIFY_VACCINE_GROUP("Classify Vaccine Group",
                               "9.1 Classify Vaccine Group", true),
                           SINGLE_ANTIGEN_VACCINE_GROUP("Single Antigen Vaccine Group",
                               "9.2 Single Antigen Vaccine Group", true),
                           MULTIPLE_ANTIGEN_VACCINE_GROUP("Multiple Antigen Vaccine Group",
                               "9.3 Multiple Antigen Vaccine Group", true),
                           END("End", "End", false),;
  private String name = "";
  private String display = "";
  private boolean indent = false;

  public String getName() {
    return name;
  }

  public String getDisplay() {
    return display;
  }

  public boolean isIndent() {
    return indent;
  }

  private LogicStepType(String name, String display, boolean indent) {
    this.name = name;
    this.display = display;
    this.indent = indent;
  }
}
