package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public abstract class LogicStep {

  public static final LogicStepType[] STEPS = {LogicStepType.GATHER_NECESSARY_DATA,

      LogicStepType.CREATE_RELEVANT_PATIENT_SERIES,
      LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY,
      LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, 
      LogicStepType.FOR_EACH_PATIENT_SERIES,
      LogicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED,
      LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION,
      LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION,
      LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE,
      LogicStepType.EVALUATE_AGE,
      LogicStepType.EVALUATE_PREFERABLE_INTERVAL,
      LogicStepType.EVALUATE_ALLOWABLE_INTERVAL,
      LogicStepType.EVALUATE_FOR_LIVE_VIRUS_CONFLICT,
      LogicStepType.EVALUATE_PREFERABLE_VACCINE_ADMINISTERED,
      LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED,
      LogicStepType.EVALUATE_GENDER,
      LogicStepType.SATISFY_TARGET_DOSE,
      LogicStepType.FORECAST_DATES_AND_REASONS,
      LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST,
      LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY,
      LogicStepType.DETERMINE_FORECAST_NEED,
      LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES,
      LogicStepType.SELECT_BEST_PATIENT_SERIES,
      LogicStepType.ONE_BEST_PATIENT_SERIES,
      LogicStepType.CLASSIFY_PATIENT_SERIES,
      LogicStepType.COMPLETE_PATIENT_SERIES,
      LogicStepType.IN_PROCESS_PATIENT_SERIES,
      LogicStepType.NO_VALID_DOSES,
      LogicStepType.SELECT_BEST_CANDIDATE_PATIENT_SERIES,
      LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP,
      LogicStepType.CLASSIFY_VACCINE_GROUP,
      LogicStepType.SINGLE_ANTIGEN_VACCINE_GROUP,
      LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP};

  public static final String PARAM_VACCINE_MVX = "vaccineMvx";
  public static final String PARAM_VACCINE_CVX = "vaccineCvx";
  public static final String PARAM_VACCINE_CONDITION_CODE = "vaccineConditionCode";
  public static final String PARAM_VACCINE_DATE = "vaccineDate";
  public static final String PARAM_PATIENT_SEX = "patientSex";
  public static final String PARAM_PATIENT_DOB = "patientDob";
  public static final String PARAM_RESULT_FORMAT = "resultFormat";
  public static final String PARAM_EVAL_DATE = "evalDate";
  public static final String PARAM_FLU_SEASON_START = "fluSeasonStart";
  public static final String PARAM_FLU_SEASON_DUE = "fluSeasonDue";
  public static final String PARAM_FLU_SEASON_OVERDUE = "fluSeasonOverdue";
  public static final String PARAM_FLU_SEASON_END = "fluSeasonEnd";
  public static final String PARAM_DUE_USE_EARLY = "dueUseEarly";
  public static final String PARAM_ASSUME_DTAP_SERIES_COMPLETE_AT_AGE =
      "assumeDtapSeriesCompleteAtAge";
  public static final String PARAM_ASSUME_HEPA_SERIES_COMPLETE_AT_AGE =
      "assumeHepASeriesCompleteAtAge";
  public static final String PARAM_ASSUME_HEPB_SERIES_COMPLETE_AT_AGE =
      "assumeHepBSeriesCompleteAtAge";
  public static final String PARAM_ASSUME_MMR_SERIES_COMPLETE_AT_AGE =
      "assumeMMRSeriesCompleteAtAge";
  public static final String PARAM_ASSUME_VAR_SERIES_COMPLETE_AT_AGE =
      "assumeVarSeriesCompleteAtAge";
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
  private List<String> logList = new ArrayList<String>();

  public LogicStepType getLogicStepType() {
    return logicStepType;
  }

  public LogicStepType getNextLogicStepType() {
    return nextLogicStepType;
  }

  public void setNextLogicStepType(LogicStepType nextLogicStepType) {
    this.nextLogicStepType = nextLogicStepType;
  }

  public List<String> getLogList() {
    return logList;
  }

  public String getTitle() {
    return logicStepType.getDisplay();
  }

  public void log(String s) {
    logList.add(s);
  }

  public void printLog(PrintWriter out) {
    if (logList.size() > 0) {
      out.println("<p>Processing log</p>");
      out.println("<ul>");
      for (String s : logList) {
        out.println("<li>" + s + "</li>");
      }
      out.println("</ul>");
    }
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

  protected List<ConditionAttribute<?>> conditionAttributesList =
      new ArrayList<ConditionAttribute<?>>();
  protected Map<String, List<ConditionAttribute<?>>> conditionAttributesAdditionalMap =
      new HashMap<String, List<ConditionAttribute<?>>>();

  protected List<LogicTable> logicTableList = new ArrayList<LogicTable>();

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

  public abstract void printPre(PrintWriter out) throws Exception;

  public abstract void printPost(PrintWriter out) throws Exception;

  protected void printConditionAttributesTable(PrintWriter out) {
    printConditionAttributesTable(out, conditionTableName);
  }

  protected void printConditionAttributesTable(PrintWriter out, String tableName) {
    {
      List<ConditionAttribute<?>> caList = conditionAttributesList;
      if (caList.size() > 0) {
        printConditionAttributesTable(out, tableName, caList);
      }
    }
    if (conditionAttributesAdditionalMap.size() > 0) {
      List<String> nameList = new ArrayList<String>(conditionAttributesAdditionalMap.keySet());
      Collections.sort(nameList);
      for (String name : nameList) {
        List<ConditionAttribute<?>> caList = conditionAttributesAdditionalMap.get(name);
        printConditionAttributesTable(out, name, caList);
      }
    }
  }

  private void printConditionAttributesTable(PrintWriter out, String tableName,
      List<ConditionAttribute<?>> caList) {
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    out.println("<h2>" + tableName + "</h2>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Attribute Type</th>");
    out.println("    <th>Attribute Name</th>");
    out.println("    <th>Value</th>");
    out.println("    <th>Assumed Value if empty</th>");
    out.println("    <th>Final Value</th>");
    out.println("  </tr>");
    for (ConditionAttribute<?> conditionAttribute : caList) {
      if(conditionAttribute == null) {
        continue;
      }
      out.println("  <tr>");
      out.println("    <td>" + conditionAttribute.getAttributeType() + "</td>");
      out.println("    <td>" + conditionAttribute.getAttributeName() + "</td>");
      if (conditionAttribute.getInitialValue() == null) {
        out.println("    <td>-</td>");
      } else if (conditionAttribute.getInitialValue() instanceof Date) {
        out.println("    <td>" + sdf.format(conditionAttribute.getInitialValue()) + "</td>");
      } else {
        out.println("    <td>" + conditionAttribute.getInitialValue() + "</td>");
      }
      if (conditionAttribute.getAssumedValue() == null) {
        out.println("    <td>-</td>");
      } else if (conditionAttribute.getAssumedValue() instanceof Date) {
        out.println("    <td>" + sdf.format(conditionAttribute.getAssumedValue()) + "</td>");
      } else {
        out.println("    <td>" + conditionAttribute.getAssumedValue() + "</td>");
      }
      if (conditionAttribute.getFinalValue() == null) {
        out.println("    <td>-</td>");
      } else if (conditionAttribute.getFinalValue() instanceof Date) {
        out.println("    <td>" + sdf.format(conditionAttribute.getFinalValue()) + "</td>");
      } else {
        out.println("    <td>" + conditionAttribute.getFinalValue() + "</td>");
      }
      out.println("  </tr>");
    }
    out.println("</table>");
  }

  protected void printLogicTables(PrintWriter out) {
    for (LogicTable logicTable : logicTableList) {
      out.println("<h2>" + logicTable.getLabel() + "</h2>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Conditions</th>");
      out.println("    <th colspan=\"" + logicTable.getLogicOutcomes().length + "\">Rules</th>");
      out.println("  </tr>");
      for (int i = 0; i < logicTable.getLogicConditions().length; i++) {
        out.println("  <tr>");
        LogicCondition logicCondition = logicTable.getLogicConditions()[i];

        if (logicCondition == null) {
          out.println("    <td>TODO</td>");
        } else if (logicCondition.getLogicResult() == null) {
          out.println("    <td>" + logicCondition.getLabel() + "</td>");
        } else {
          out.println("    <td>" + logicCondition.getLabel() + " <b>"
              + logicCondition.getLogicResult() + "</b></td>");
        }
        for (int j = 0; j < logicTable.getLogicResultTable()[i].length; j++) {
          LogicResult logicResult = logicTable.getLogicResultTable()[i][j];
          String style = "";
          if (logicCondition != null && logicCondition.getLogicResult() != null
              && (logicCondition.getLogicResult() == logicResult
                  || logicResult == LogicResult.ANY)) {
            style = "pass";
          }
          if (logicResult == LogicResult.YES) {
            out.println("    <td class=\"" + style + "\">Yes</td>");
          } else if (logicResult == LogicResult.NO) {
            out.println("    <td class=\"" + style + "\">No</td>");
          } else if (logicResult == LogicResult.ANY) {
            out.println("    <td class=\"" + style + "\">-</td>");
          }
        }
        out.println("  </tr>");
      }
      out.println("  <tr>");
      out.println("    <th>Outcomes</th>");
      for (int j = 0; j < logicTable.getLogicOutcomes().length; j++) {
        LogicOutcome logicOutcome = logicTable.getLogicOutcomes()[j];
        if (logicOutcome != null && logicOutcome.getLogList() != null
            && logicOutcome.getLogList().size() > 0) {
          out.println("    <td class=\"pass\"><ul>");
          for (String log : logicOutcome.getLogList()) {
            out.println("<li>" + log + "</li>");
          }
          out.println("</ul></td>");
        } else {
          out.println("    <td></td>");
        }
      }
      out.println("  </tr>");

      out.println("</table>");
    }
  }
}
