package org.openimmunizationsoftware.cdsi.core.logic;

public enum LogicStepType {

  GATHER_NECESSARY_DATA("4.1", "Gather Necessary Data", false),
  ORGANIZE_IMMUNIZATION_HISTORY("4.2", "Organize Immunization History", false), 
  CREATE_PATIENT_SERIES("4.3", "Create Relevant Patient Series", false), 
  EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES("4.4", "Evaluate and Forecast all Patient Series", false), 
  SELECT_BEST_PATIENT_SERIES("4.5", "Select Best Patient Series", false), 
  IDENTIFY_AND_EVALUATE_VACCINE_GROUP("4.6", "Identify and Evaluate Vaccine Group", false),
  FOR_EACH_PATIENT_SERIES("xx", "for each Patient Series", true), 
  EVALUATE_VACCINE_DOSE_ADMINISTERED("6", "Evaluate Vaccine Doses Administered", true), 
  EVALUATE_DOSE_ADMINISTERED_CONDITION("6.1", "Evaluate Dose Administered Condition", true), 
  EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION("6.2", "Evaluate Conditional Skip", true), 
  EVALUATE_FOR_INADVERTENT_VACCINE("6.3", "Evaluate for Inadvertent Vaccine", true),
  EVALUATE_AGE("6.4", "Evaluate Age", true), 
  EVALUATE_INTERVAL("6.5", "Evaluate Preferable Age", true), 
  EVALUATE_ALLOWABLE_INTERVAL("6.6", "Evaluate Allowable Interval", true), 
  EVALUATE_FOR_LIVE_VIRUS_CONFLICT("6.7", "Evaluate Vaccine Conflict", true), 
  EVALUATE_PREFERABLE_VACCINE_ADMINISTERED("6.8", "Evaluate for Preferable Vaccine", true), 
  EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED("6.9", "Evaluate Allowable Vaccine ", true), 
  EVALUATE_GENDER("xx","Evaluate Gender", true), 
  SATISFY_TARGET_DOSE("6.10", "Satisfy Target Dose", true), 
  FORECAST_DATES_AND_REASONS("7", "Forecast Dates and Reasons", true), 
  EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST("7.1", "Evaluate Conditional Skip", true), 
  DETERMINE_EVIDENCE_OF_IMMUNITY("7.2", "Determine Evidence of Immunity", true),
  DETERMINE_CONTRAINDICATIONS("7.3", "Determine Contraindications", true),
  DETERMINE_FORECAST_NEED("7.4", "Determine Forecast Need", true),
  GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES("7.5", "Generate Forecast Dates and Recommend Vaccines", true), 
  VALIDATE_RECOMMENDATIONS("7.6", "Validate Recommendations", true),
  SELECT_BEST_PATIENT_SERIES_("8", "Select Best Patient Series", true),
  PRE_FILTER_PATIENT_SERIES("8.1", "Pre-Filter Patient Series", true),
  ONE_BEST_PATIENT_SERIES("8.2", "One Best Patient Series", true), 
  CLASSIFY_PATIENT_SERIES("8.3", "Classify Scorable Patient Series", true), 
  COMPLETE_PATIENT_SERIES("8.4", "Complete Patient Series", true),
  IN_PROCESS_PATIENT_SERIES("8.5", "In-Process Patient Series", true),
  NO_VALID_DOSES("8.6", "No Valid Doses", true),
  SELECT_BEST_CANDIDATE_PATIENT_SERIES("8.7", "Select Best Candidate Patient Series", true), 
  SELECT_BEST_PATIENT_SERIES_FOR_SELECT_BEST_PATIENT_SERIES("8.8", "Select Best Patient Series", true),
  IDENTIFY_AND_EVALUATE_VACCINE_GROUP_("9", "Identify and Evaluate Vaccine Group", true),
  CLASSIFY_VACCINE_GROUP("9.1", "Apply General Vaccine Group Rules", true), 
  SINGLE_ANTIGEN_VACCINE_GROUP("9.2", "Single Antigen Vaccine Group", true), 
  MULTIPLE_ANTIGEN_VACCINE_GROUP("9.3", "Multiple Antigen Vaccine Group", true), 
  END("End", "End", false),
  ;

  private String name = "";
  private String display = "";
  private boolean indent = false;
  private String chapter = "";

  public String getName() {
    return name;
  }

  public String getDisplay() {
    return display;
  }

  public boolean isIndent() {
    return indent;
  }

  public String getChapter() {
    return chapter;
  }

  private LogicStepType(String chapter, String name, boolean indent) {
    this.chapter = chapter;
    this.name = name;
    this.display = chapter + " " + display;
    this.indent = indent;
  }
}
