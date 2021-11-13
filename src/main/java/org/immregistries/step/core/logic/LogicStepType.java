package org.immregistries.step.core.logic;

public enum LogicStepType {

                           GATHER_NECESSARY_DATA("Gather Necessary Data",
                               "8.1 Gather Necessary Data", false),
                           CREATE_PATIENT_SERIES("Create Patient Series",
                               "8.2 Create Patient Series", false),
                           ORGANIZE_IMMUNIZATION_HISTORY("Organize Immunization History",
                               "8.3 Organize Immunization History", false),
                           EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES(
                               "Evaluate and Forecast all Patient Series",
                               "8.4 Evaluate and Forecast all Patient Series", false),
                           SELECT_BEST_PATIENT_SERIES("Select Best Patient Series",
                               "8.5 Select Best Patient Series", false),
                           IDENTIFY_AND_EVALUATE_VACCINE_GROUP(
                               "Identify and Evaluate Vaccine Group",
                               "8.6 Identify and Evaluate Vaccine Group", false),

                           FOR_EACH_PATIENT_SERIES("for each Patient Series",
                               "for each Patient Series", true),
                           EVALUATE_VACCINE_DOSE_ADMINISTERED("Evaluate Vaccine Dose Administered",
                               "4 Evaluate Vaccine Dose Administered", true),
                           EVALUATE_DOSE_ADMININISTERED_CONDITION(
                               "Evaluate Dose Administered Condition",
                               "4.1 Evaluate Dose Administered Condition", true),
                           EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION("Evaluate Conditional Skip",
                               "4.2 Evaluate Conditional Skip", true),
                           EVALUATE_AGE("Evaluate Age", "4.3 Evaluate Age", true),
                           EVALUATE_INTERVAL("Evaluate Interval", "4.4 Evaluate Interval", true),
                           EVALUATE_ALLOWABLE_INTERVAL("Evaluate Allowable Interval",
                               "4.5 Evaluate Allowable Interval", true),
                           EVALUATE_FOR_LIVE_VIRUS_CONFLICT("Evaluate for Live Virus Conflict",
                               "4.6 Evaluate for Live Virus Conflict", true),
                           EVALUATE_PREFERABLE_VACCINE_ADMINISTERED(
                               "Evaluate Preferable Vaccine Administered",
                               "4.7 Evaluate Preferable Vaccine Administered", true),
                           EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED(
                               "Evaluate Allowable Vaccine Administered",
                               "4.8 Evaluate Allowable Vaccine Administered", true),
                           EVALUATE_GENDER("Evaluate Gender", "4.9 Evaluate Gender", true),
                           SATISFY_TARGET_DOSE("Satisfy Target Dose", "4.10 Satisfy Target Dose",
                               true),
                           FORECAST_DATES_AND_REASONS("Forecast Dates and Reasons",
                               "5 Forecast Dates and Reasons", true),
                           EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST("Evaluate Conditional Skip",
                               "5.1 Evaluate Conditional Skip", true),
                           DETERMINE_EVIDENCE_OF_IMMUNITY("Determine Evidence of Immunity",
                               "5.2 Determine Evidence of Immunity", true),
                           DETERMINE_FORECAST_NEED("Determine Forecast Need",
                               "5.3 Determine Forecast Need", true),
                           GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES(
                               "Generate Forecast Dates and Recommend Vaccines",
                               "5.5 Generate Forecast Dates and Recommend Vaccines", true),
                           ONE_BEST_PATIENT_SERIES("One Best Patient Series",
                               "6.2 One Best Patient Series", true),
                           CLASSIFY_PATIENT_SERIES("Classify Patient Series",
                               "6.3 Classify Patient Series", true),
                           COMPLETE_PATIENT_SERIES("Complete Patient Series",
                               "6.4 Complete Patient Series", true),
                           IN_PROCESS_PATIENT_SERIES("In-Process Patient Series",
                               "6.5 In-Process Patient Series", true),
                           NO_VALID_DOSES("No Valid Doses", "6.6 No Valid Doses", true),
                           SELECT_BEST_CANDIDATE_PATIENT_SERIES(
                               "Select Best Candidate Patient Series",
                               "6.7 Select Best Candidate Patient Series", true),
                           CLASSIFY_VACCINE_GROUP("Classify Vaccine Group",
                               "7.1 Classify Vaccine Group", true),
                           SINGLE_ANTIGEN_VACCINE_GROUP("Single Antigen Vaccine Group",
                               "7.2 Single Antigen Vaccine Group", true),
                           MULTIPLE_ANTIGEN_VACCINE_GROUP("Multiple Antigen Vaccine Group",
                               "7.3 Multiple Antigen Vaccine Group", true),
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
