package org.openimmunizationsoftware.cdsi.core.logic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.BusinessRule;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogEvent;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogSink;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicStepSink;

public abstract class LogicStep implements LogSink {

  public static final LogicStepType[] STEPS = { LogicStepType.GATHER_NECESSARY_DATA,

      LogicStepType.CREATE_RELEVANT_PATIENT_SERIES,
      LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY,
      LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES,
      LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION,
      LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION,
      LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE,
      LogicStepType.EVALUATE_AGE,
      LogicStepType.EVALUATE_PREFERABLE_INTERVAL,
      LogicStepType.EVALUATE_ALLOWABLE_INTERVAL,
      LogicStepType.EVALUATE_VACCINE_CONFLICT,
      LogicStepType.EVALUATE_FOR_PREFERABLE_VACCINE,
      LogicStepType.EVALUATE_FOR_ALLOWABLE_VACCINE,
      LogicStepType.EVALUATE_GENDER,
      LogicStepType.SATISFY_TARGET_DOSE,
      LogicStepType.FORECAST_DATES_AND_REASONS,
      LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST,
      LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY,
      LogicStepType.DETERMINE_CONTRAINDICATIONS,
      LogicStepType.DETERMINE_FORECAST_NEED,
      LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES,
      LogicStepType.SELECT_BEST_PATIENT_SERIES,
      LogicStepType.PRE_FILTER_PATIENT_SERIES,
      LogicStepType.IDENTIFY_ONE_PRIORITIZED_PATIENT_SERIES,
      LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES,
      LogicStepType.COMPLETE_PATIENT_SERIES,
      LogicStepType.IN_PROCESS_PATIENT_SERIES,
      LogicStepType.NO_VALID_DOSES,
      LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES,
      LogicStepType.DETERMINE_BEST_PATIENT_SERIES,
      LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP,
      LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES,
      LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP,
      LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP,
      LogicStepType.END };

  public static final String PARAM_VACCINE_MVX = "vaccineMvx";
  public static final String PARAM_VACCINE_CVX = "vaccineCvx";
  public static final String PARAM_VACCINE_CONDITION_CODE = "vaccineConditionCode";
  public static final String PARAM_VACCINE_DATE = "vaccineDate";
  public static final String PARAM_OBSERVATION_CODE = "observationCode";
  public static final String PARAM_OBSERVATION_DATE = "observationDate";
  public static final String PARAM_PATIENT_SEX = "patientSex";
  public static final String PARAM_PATIENT_DOB = "patientDob";
  public static final String PARAM_RESULT_FORMAT = "resultFormat";
  public static final String PARAM_EVAL_DATE = "evalDate";
  public static final String PARAM_FLU_SEASON_START = "fluSeasonStart";
  public static final String PARAM_FLU_SEASON_DUE = "fluSeasonDue";
  public static final String PARAM_FLU_SEASON_OVERDUE = "fluSeasonOverdue";
  public static final String PARAM_FLU_SEASON_END = "fluSeasonEnd";
  public static final String PARAM_DUE_USE_EARLY = "dueUseEarly";
  public static final String PARAM_ASSUME_DTAP_SERIES_COMPLETE_AT_AGE = "assumeDtapSeriesCompleteAtAge";
  public static final String PARAM_ASSUME_HEPA_SERIES_COMPLETE_AT_AGE = "assumeHepASeriesCompleteAtAge";
  public static final String PARAM_ASSUME_HEPB_SERIES_COMPLETE_AT_AGE = "assumeHepBSeriesCompleteAtAge";
  public static final String PARAM_ASSUME_MMR_SERIES_COMPLETE_AT_AGE = "assumeMMRSeriesCompleteAtAge";
  public static final String PARAM_ASSUME_VAR_SERIES_COMPLETE_AT_AGE = "assumeVarSeriesCompleteAtAge";
  public static final String PARAM_IGNORE_FOUR_DAY_GRACE = "ignoreFourDayGrace";
  public static final String PARAM_SCHEDULE_NAME = "scheduleName";
  public static final String PARAM_ASSUME_SERIES_COMPLETED = "assumeSeriesCompleted";

  public static final String PARAM_ANTIGEN_SERIES_INCLUDE = "antigenSeriesInclude";
  public static final String PARAM_ANTIGEN_INCLUDE = "antigenInclude";

  protected static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
  protected static Date FUTURE = null;
  protected static Date PAST = null;

  static {
    try {
      FUTURE = sdf.parse("12/31/2999");
      PAST = sdf.parse("01/01/1900");
    } catch (ParseException pse) {
      pse.printStackTrace();
    }
  }

  protected DataModel dataModel = null;
  protected LogicStepType logicStepType = null;
  protected LogicStepType nextLogicStepType = null;
  private String conditionTableName = "";
  private LogicStepSink logicStepSink = new LogicStepSink();

  public LogicStepType getLogicStepType() {
    return logicStepType;
  }

  public DataModel getDataModel() {
    return dataModel;
  }

  public Map<String, List<ConditionAttribute<?>>> getConditionAttributesAdditionalMap() {
    return conditionAttributesAdditionalMap;
  }

  public LogicStepType getNextLogicStepType() {
    return nextLogicStepType;
  }

  public void setNextLogicStepType(LogicStepType nextLogicStepType) {
    this.nextLogicStepType = nextLogicStepType;
  }

  /**
   * Get the LogicSink for this LogicStep.
   * This is used to propagate logging to child LogicTables and LogicOutcomes.
   * 
   * @return The LogicSink instance
   */
  protected LogSink getLogicStepSink() {
    return logicStepSink;
  }

  /**
   * Returns the list of log messages as strings, preserving order.
   * Maintained for backwards compatibility.
   * 
   * @return List of log messages
   */
  public List<String> getLogList() {
    return logicStepSink.getLogList();
  }

  /**
   * Returns the list of log events with level and alert information.
   * 
   * @return Unmodifiable list of log events
   */
  public List<LogEvent> getLogEventList() {
    return logicStepSink.getLogEventList();
  }

  public String getTitle() {
    return logicStepType.getDisplay();
  }

  public String formatDate(Date date) {
    if (date == null) {
      return "null";
    }
    synchronized (sdf) {
      return sdf.format(date);
    }
  }

  public String formatDateList(List<Date> dates) {
    if (dates == null) {
      return "null";
    }
    if (dates.isEmpty()) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < dates.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(formatDate(dates.get(i)));
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Log a message at DUMP level (most verbose).
   * Maintained for backwards compatibility.
   * 
   * @param s The message to log
   */
  public void log(String s) {
    logicStepSink.log(s);
  }

  /**
   * Log a message at the specified level.
   * 
   * For guidance on choosing the appropriate log level,
   * see: docs/Alerting Semantics for Step Into CDSi.md
   * 
   * @param level   The log level
   * @param message The message to log
   */
  public void log(LogLevel level, String message) {
    logicStepSink.log(level, message);
  }

  /**
   * Log an alert message at CONTROL level (least verbose).
   * 
   * Alerts are used to flag important conditions that require attention.
   * For guidance on when to use alerts vs. regular logs,
   * see: docs/Alerting Semantics for Step Into CDSi.md
   * 
   * @param message The alert message to log
   */
  public void alert(String message) {
    alert(LogLevel.CONTROL, message);
  }

  /**
   * Log an alert message at the specified level.
   * 
   * Alerts are used to flag important conditions that require attention.
   * For guidance on when to use alerts vs. regular logs,
   * see: docs/Alerting Semantics for Step Into CDSi.md
   * 
   * @param level   The log level
   * @param message The alert message to log
   */
  public void alert(LogLevel level, String message) {
    logicStepSink.alert(level, message);
  }

  public LogicStep next() {
    return LogicStepFactory.createLogicStep(nextLogicStepType, dataModel);
  }

  public LogicStep next(boolean b) {
    return LogicStepFactory.createLogicStep(nextLogicStepType, dataModel, b);
  }

  public String getConditionTableName() {
    return conditionTableName;
  }

  public void setConditionTableName(String conditionTableName) {
    this.conditionTableName = conditionTableName;
  }

  protected void evaluateLogicTables() {
    for (LogicTable logicTable : logicTableList) {
      logicTable.evaluate();
    }
  }

  protected LogicStep(LogicStepType logicStepType, DataModel dataModel) {
    this.logicStepType = logicStepType;
    this.dataModel = dataModel;
  }

  protected List<ConditionAttribute<?>> conditionAttributesList = new ArrayList<ConditionAttribute<?>>();
  protected Map<String, List<ConditionAttribute<?>>> conditionAttributesAdditionalMap = new HashMap<String, List<ConditionAttribute<?>>>();

  protected List<LogicTable> logicTableList = new ArrayList<LogicTable>();
  protected List<BusinessRule<?, ?>> businessRuleList = new ArrayList<BusinessRule<?, ?>>();

  public List<BusinessRule<?, ?>> getBusinessRuleList() {
    return businessRuleList;
  }

  public void setBusinessRuleList(List<BusinessRule<?, ?>> businessRuleList) {
    this.businessRuleList = businessRuleList;
  }

  public List<LogicTable> getLogicTableList() {
    return logicTableList;
  }

  public void setLogicTableList(List<LogicTable> logicTableList) {
    this.logicTableList = logicTableList;
  }

  public List<ConditionAttribute<?>> getConditionAttributeList() {
    return conditionAttributesList;
  }

  public abstract LogicStep process() throws Exception;

  public static String n(Date d) {
    if (d == null) {
      return "-";
    }
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    return sdf.format(d);
  }
}
