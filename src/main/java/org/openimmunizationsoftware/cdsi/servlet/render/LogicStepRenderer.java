package org.openimmunizationsoftware.cdsi.servlet.render;

import static org.openimmunizationsoftware.cdsi.servlet.ServletUtil.safe;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableInterval;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalNeed;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.ObservationCode;
import org.openimmunizationsoftware.cdsi.core.domain.PatientObservation;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.RecurringDose;
import org.openimmunizationsoftware.cdsi.core.domain.RequiredGender;
import org.openimmunizationsoftware.cdsi.core.domain.SeasonalRecommendation;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.SubstituteDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.logic.ApplyGeneralVaccineGroupRules;
import org.openimmunizationsoftware.cdsi.core.logic.ClassifyScorablePatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.CompletePatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.CreateRelevantPatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.DetermineBestPatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.DetermineContraindications;
import org.openimmunizationsoftware.cdsi.core.logic.DetermineEvidenceOfImmunity;
import org.openimmunizationsoftware.cdsi.core.logic.DetermineForecastNeed;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluateAllowableInterval;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluateAndForecastAllPatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluateConditionalSkipForEvaluation;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluateConditionalSkipForForecast;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluateDoseAdministeredCondition;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluateForAllowableVaccine;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluateForInadvertentVaccine;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluateForPreferableVaccine;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluateGender;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluatePreferableInterval;
import org.openimmunizationsoftware.cdsi.core.logic.EvaluateVaccineConflict;
import org.openimmunizationsoftware.cdsi.core.logic.ForecastDatesAndReasons;
import org.openimmunizationsoftware.cdsi.core.logic.GenerateForecastDatesAndRecommendedVaccines;
import org.openimmunizationsoftware.cdsi.core.logic.IdentifyAndEvaluateVaccineGroup;
import org.openimmunizationsoftware.cdsi.core.logic.IdentifyOnePrioritizedPatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.InProcessPatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;
import org.openimmunizationsoftware.cdsi.core.logic.MultipleAntigenVaccineGroup;
import org.openimmunizationsoftware.cdsi.core.logic.NoValidDoses;
import org.openimmunizationsoftware.cdsi.core.logic.OrganizeImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.logic.PreFilterPatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.SatisfyTargetDose;
import org.openimmunizationsoftware.cdsi.core.logic.SelectBestPatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.SelectPrioritizedPatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.SelectRelevantPatientSeries;
import org.openimmunizationsoftware.cdsi.core.logic.SingleAntigenVaccineGroup;
import org.openimmunizationsoftware.cdsi.core.logic.SkipTargetDoseForForecast;
import org.openimmunizationsoftware.cdsi.core.logic.ValidateRecommendation;
import org.openimmunizationsoftware.cdsi.core.logic.items.BusinessRule;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.openimmunizationsoftware.cdsi.servlet.ForecastServlet;
import org.openimmunizationsoftware.cdsi.servlet.dataModelView.AntigenServlet;

/**
 * Renders the interactive step-through HTML (printPre/printPost/printLog) for
 * each LogicStep. This is UI-layer presentation code, kept separate from the
 * CDSi engine (core.logic) so the engine can run headlessly (CLI, JUnit-based
 * FITS runner) without any HTML/servlet dependency. The engine still exposes
 * everything this renderer needs through plain public getters - explanation
 * (LogEvent/LogLevel) stays in the engine because it's produced as part of the
 * calculation itself; only turning that into HTML lives here.
 */
public class LogicStepRenderer {

  private LogicStepRenderer() {
  }

  public static void printPre(LogicStep step, PrintWriter out, jakarta.servlet.http.HttpServletRequest req)
      throws Exception {
    if (step instanceof ApplyGeneralVaccineGroupRules s) {
      printApplyGeneralVaccineGroupRulesPre(s, out);
    } else if (step instanceof ClassifyScorablePatientSeries s) {
      printClassifyScorablePatientSeriesPre(s, out);
    } else if (step instanceof CompletePatientSeries s) {
      printCompletePatientSeriesPre(s, out);
    } else if (step instanceof CreateRelevantPatientSeries s) {
      printCreateRelevantPatientSeriesPre(s, out);
    } else if (step instanceof DetermineBestPatientSeries s) {
      printDetermineBestPatientSeriesPre(s, out);
    } else if (step instanceof DetermineContraindications s) {
      printDetermineContraindicationsPre(s, out);
    } else if (step instanceof DetermineEvidenceOfImmunity s) {
      printDetermineEvidenceOfImmunityPre(s, out);
    } else if (step instanceof DetermineForecastNeed s) {
      printDetermineForecastNeedPre(s, out);
    } else if (step instanceof org.openimmunizationsoftware.cdsi.core.logic.End s) {
      printEndPre(s, out, req);
    } else if (step instanceof org.openimmunizationsoftware.cdsi.core.logic.EvaluateAge s) {
      printEvaluateAgePre(s, out);
    } else if (step instanceof EvaluateAllowableInterval s) {
      printEvaluateAllowableIntervalPre(s, out);
    } else if (step instanceof EvaluateAndForecastAllPatientSeries s) {
      printEvaluateAndForecastAllPatientSeriesPre(s, out);
    } else if (step instanceof EvaluateConditionalSkipForEvaluation s) {
      printEvaluateConditionalSkipForEvaluationPre(s, out);
    } else if (step instanceof EvaluateConditionalSkipForForecast s) {
      printEvaluateConditionalSkipForForecastPre(s, out);
    } else if (step instanceof EvaluateDoseAdministeredCondition s) {
      printEvaluateDoseAdministeredConditionPre(s, out);
    } else if (step instanceof EvaluateForAllowableVaccine s) {
      printEvaluateForAllowableVaccinePre(s, out);
    } else if (step instanceof EvaluateForInadvertentVaccine s) {
      printEvaluateForInadvertentVaccinePre(s, out);
    } else if (step instanceof EvaluateForPreferableVaccine s) {
      printEvaluateForPreferableVaccinePre(s, out);
    } else if (step instanceof EvaluateGender s) {
      printEvaluateGenderPre(s, out);
    } else if (step instanceof EvaluatePreferableInterval s) {
      printEvaluatePreferableIntervalPre(s, out);
    } else if (step instanceof EvaluateVaccineConflict s) {
      printEvaluateVaccineConflictPre(s, out);
    } else if (step instanceof ForecastDatesAndReasons s) {
      printForecastDatesAndReasonsPre(s, out);
    } else if (step instanceof org.openimmunizationsoftware.cdsi.core.logic.GatherNecessaryData s) {
      printGatherNecessaryDataPre(s, out, req);
    } else if (step instanceof GenerateForecastDatesAndRecommendedVaccines s) {
      printGenerateForecastDatesAndRecommendedVaccinesPre(s, out);
    } else if (step instanceof IdentifyAndEvaluateVaccineGroup s) {
      printIdentifyAndEvaluateVaccineGroupPre(s, out);
    } else if (step instanceof IdentifyOnePrioritizedPatientSeries s) {
      printIdentifyOnePrioritizedPatientSeriesPre(s, out);
    } else if (step instanceof InProcessPatientSeries s) {
      printInProcessPatientSeriesPre(s, out);
    } else if (step instanceof MultipleAntigenVaccineGroup s) {
      printMultipleAntigenVaccineGroupPre(s, out);
    } else if (step instanceof NoValidDoses s) {
      printNoValidDosesPre(s, out);
    } else if (step instanceof OrganizeImmunizationHistory s) {
      printOrganizeImmunizationHistoryPre(s, out);
    } else if (step instanceof PreFilterPatientSeries s) {
      printPreFilterPatientSeriesPre(s, out);
    } else if (step instanceof SatisfyTargetDose s) {
      printSatisfyTargetDosePre(s, out);
    } else if (step instanceof SelectBestPatientSeries s) {
      printSelectBestPatientSeriesPre(s, out);
    } else if (step instanceof SelectPrioritizedPatientSeries s) {
      printSelectPrioritizedPatientSeriesPre(s, out);
    } else if (step instanceof SelectRelevantPatientSeries s) {
      printSelectRelevantPatientSeriesPre(s, out);
    } else if (step instanceof SingleAntigenVaccineGroup s) {
      printSingleAntigenVaccineGroupPre(s, out);
    } else if (step instanceof SkipTargetDoseForForecast s) {
      printSkipTargetDoseForForecastPre(s, out);
    } else if (step instanceof ValidateRecommendation s) {
      printValidateRecommendationPre(s, out);
    }
  }

  public static void printPost(LogicStep step, PrintWriter out) throws Exception {
    if (step instanceof ApplyGeneralVaccineGroupRules s) {
      printApplyGeneralVaccineGroupRulesPost(s, out);
    } else if (step instanceof ClassifyScorablePatientSeries s) {
      printClassifyScorablePatientSeriesPost(s, out);
    } else if (step instanceof CompletePatientSeries s) {
      printCompletePatientSeriesPost(s, out);
    } else if (step instanceof CreateRelevantPatientSeries s) {
      printCreateRelevantPatientSeriesPost(s, out);
    } else if (step instanceof DetermineBestPatientSeries s) {
      printDetermineBestPatientSeriesPost(s, out);
    } else if (step instanceof DetermineContraindications s) {
      printDetermineContraindicationsPost(s, out);
    } else if (step instanceof DetermineEvidenceOfImmunity s) {
      printDetermineEvidenceOfImmunityPost(s, out);
    } else if (step instanceof DetermineForecastNeed s) {
      printDetermineForecastNeedPost(s, out);
    } else if (step instanceof org.openimmunizationsoftware.cdsi.core.logic.End s) {
      printEndPost(s, out);
    } else if (step instanceof org.openimmunizationsoftware.cdsi.core.logic.EvaluateAge s) {
      printEvaluateAgePost(s, out);
    } else if (step instanceof EvaluateAllowableInterval s) {
      printEvaluateAllowableIntervalPost(s, out);
    } else if (step instanceof EvaluateAndForecastAllPatientSeries s) {
      printEvaluateAndForecastAllPatientSeriesPost(s, out);
    } else if (step instanceof EvaluateConditionalSkipForEvaluation s) {
      printEvaluateConditionalSkipForEvaluationPost(s, out);
    } else if (step instanceof EvaluateConditionalSkipForForecast s) {
      printEvaluateConditionalSkipForForecastPost(s, out);
    } else if (step instanceof EvaluateDoseAdministeredCondition s) {
      printEvaluateDoseAdministeredConditionPost(s, out);
    } else if (step instanceof EvaluateForAllowableVaccine s) {
      printEvaluateForAllowableVaccinePost(s, out);
    } else if (step instanceof EvaluateForInadvertentVaccine s) {
      printEvaluateForInadvertentVaccinePost(s, out);
    } else if (step instanceof EvaluateForPreferableVaccine s) {
      printEvaluateForPreferableVaccinePost(s, out);
    } else if (step instanceof EvaluateGender s) {
      printEvaluateGenderPost(s, out);
    } else if (step instanceof EvaluatePreferableInterval s) {
      printEvaluatePreferableIntervalPost(s, out);
    } else if (step instanceof EvaluateVaccineConflict s) {
      printEvaluateVaccineConflictPost(s, out);
    } else if (step instanceof ForecastDatesAndReasons s) {
      printForecastDatesAndReasonsPost(s, out);
    } else if (step instanceof org.openimmunizationsoftware.cdsi.core.logic.GatherNecessaryData s) {
      printGatherNecessaryDataPost(s, out);
    } else if (step instanceof GenerateForecastDatesAndRecommendedVaccines s) {
      printGenerateForecastDatesAndRecommendedVaccinesPost(s, out);
    } else if (step instanceof IdentifyAndEvaluateVaccineGroup s) {
      printIdentifyAndEvaluateVaccineGroupPost(s, out);
    } else if (step instanceof IdentifyOnePrioritizedPatientSeries s) {
      printIdentifyOnePrioritizedPatientSeriesPost(s, out);
    } else if (step instanceof InProcessPatientSeries s) {
      printInProcessPatientSeriesPost(s, out);
    } else if (step instanceof MultipleAntigenVaccineGroup s) {
      printMultipleAntigenVaccineGroupPost(s, out);
    } else if (step instanceof NoValidDoses s) {
      printNoValidDosesPost(s, out);
    } else if (step instanceof OrganizeImmunizationHistory s) {
      printOrganizeImmunizationHistoryPost(s, out);
    } else if (step instanceof PreFilterPatientSeries s) {
      printPreFilterPatientSeriesPost(s, out);
    } else if (step instanceof SatisfyTargetDose s) {
      printSatisfyTargetDosePost(s, out);
    } else if (step instanceof SelectBestPatientSeries s) {
      printSelectBestPatientSeriesPost(s, out);
    } else if (step instanceof SelectPrioritizedPatientSeries s) {
      printSelectPrioritizedPatientSeriesPost(s, out);
    } else if (step instanceof SelectRelevantPatientSeries s) {
      printSelectRelevantPatientSeriesPost(s, out);
    } else if (step instanceof SingleAntigenVaccineGroup s) {
      printSingleAntigenVaccineGroupPost(s, out);
    } else if (step instanceof SkipTargetDoseForForecast s) {
      printSkipTargetDoseForForecastPost(s, out);
    } else if (step instanceof ValidateRecommendation s) {
      printValidateRecommendationPost(s, out);
    }
  }

  public static void printLog(LogicStep step, PrintWriter out) {
    List<String> messages = step.getLogList();
    if (messages.size() > 0) {
      out.println("<p>Processing log</p>");
      out.println("<ul>");
      for (String s : messages) {
        out.println("<li>" + s + "</li>");
      }
      out.println("</ul>");
    }
    printBussinessRules(step, out);
  }

  static void printConditionAttributesTable(LogicStep step, PrintWriter out) {
    printConditionAttributesTable(step, out, step.getConditionTableName());
  }

  static void printConditionAttributesTable(LogicStep step, PrintWriter out, String tableName) {
    {
      List<ConditionAttribute<?>> caList = step.getConditionAttributeList();
      if (caList.size() > 0) {
        printConditionAttributesTable(out, tableName, caList);
      }
    }
    if (step.getConditionAttributesAdditionalMap().size() > 0) {
      List<String> nameList = new ArrayList<String>(step.getConditionAttributesAdditionalMap().keySet());
      Collections.sort(nameList);
      for (String name : nameList) {
        List<ConditionAttribute<?>> caList = step.getConditionAttributesAdditionalMap().get(name);
        printConditionAttributesTable(out, name, caList);
      }
    }
  }

  private static void printConditionAttributesTable(PrintWriter out, String tableName,
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
      if (conditionAttribute == null) {
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

  static void printBussinessRules(LogicStep step, PrintWriter out) {
    out.println("<p>Business Rules Processing Log</p>");
    for (BusinessRule<?, ?> businessRule : step.getBusinessRuleList()) {
      if (businessRule.getLogList().size() > 0) {
        out.println("<p>Business Rule " + businessRule.getBusinessRuleId() + " " + businessRule.getTerm() + "</p>");
        out.println("<ul>");
        for (String s : businessRule.getLogList()) {
          out.println("<li>" + s + "</li>");
        }
        out.println("</ul>");
      }
    }
  }

  static void printLogicTables(LogicStep step, PrintWriter out) {
    for (LogicTable logicTable : step.getLogicTableList()) {
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
          } else if (logicResult == LogicResult.UNKNOWN) {
            out.println("    <td class=\"" + style + "\">Unknown</td>");
          } else if (logicResult == LogicResult.EXTRANEOUS) {
            out.println("    <td class=\"" + style + "\">Extraneous</td>");
          } else if (logicResult == LogicResult.ZERO) {
            out.println("    <td class=\"" + style + "\">0</td>");
          } else if (logicResult == LogicResult.ONE) {
            out.println("    <td class=\"" + style + "\">1</td>");
          } else if (logicResult == LogicResult.MORE_THAN_ONE) {
            out.println("    <td class=\"" + style + "\">&gt;1</td>");
          }
        }
        out.println("  </tr>");
      }
      out.println("  <tr>");

      LogicOutcome logicOutcomeDefault = logicTable.getLogicOutcomeDefault();
      if (logicOutcomeDefault != null && logicOutcomeDefault.getLogList() != null
          && logicOutcomeDefault.getLogList().size() > 0) {
        out.println("<th class=\"pass\"><ul>");
        for (String log : logicOutcomeDefault.getLogList()) {
          out.println("<li>" + log + "</li>");
        }
        out.println("</ul></th>");
      } else {
        out.println("<th>Outcomes</th>");
      }

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

  static void printBestPatientSeries(LogicStep step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    // print out best patient series
    if (dataModel.getBestPatientSeriesList() != null) {
      out.println("<h2>Best Patient Series</h2>");
      out.println("<ul>");
      for (PatientSeries ps : dataModel.getBestPatientSeriesList()) {
        out.println("<li>" + ps.getTrackedAntigenSeries().getTargetDisease().getName() + ": "
            + ps.getTrackedAntigenSeries().getSeriesName() + "</li>");
      }
      out.println("</ul>");
    }
  }

  static void printPatientSeriesList(LogicStep step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println("<h2> Patient Series List </h2>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th> Antigen </th>");
    out.println("    <th> Antigen Series </th>");
    out.println("    <th> Patient Series Status </th>");
    out.println("    <th> Target Dose List size </th>");
    out.println("  </tr>");
    for (PatientSeries patientSeries : dataModel.getPatientSeriesStepper().getList()) {
      out.println("  <tr>");
      out.println("    <td>" + patientSeries.getTrackedAntigenSeries().getTargetDisease().getName() + "</td>");
      out.println("    <td>" + patientSeries.getTrackedAntigenSeries().getSeriesName() + "</td>");
      out.println("    <td>" + patientSeries.getPatientSeriesStatus() + "</td>");
      int size = patientSeries.getTargetDoseList() == null ? 0 : patientSeries.getTargetDoseList().size();
      out.println("    <td>" + size + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");

    if (dataModel.getScorablePatientSeriesList() != null) {
      out.println("<h2> Scorable Patient Series List </h2>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th> Antigen </th>");
      out.println("    <th> Antigen Series </th>");
      out.println("    <th> Patient Series Status </th>");
      out.println("    <th> Target Dose List size </th>");
      out.println("  </tr>");
      for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
        out.println("  <tr>");
        out.println("    <td>" + patientSeries.getTrackedAntigenSeries().getTargetDisease().getName() + "</td>");
        out.println("    <td>" + patientSeries.getTrackedAntigenSeries().getSeriesName() + "</td>");
        out.println("    <td>" + patientSeries.getPatientSeriesStatus() + "</td>");
        int size = patientSeries.getTargetDoseList() == null ? 0 : patientSeries.getTargetDoseList().size();
        out.println("    <td>" + size + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
  }

  private static String seriesDoseDate(Date date) {
    if (date == null) {
      return "";
    } else {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      return sdf.format(date);
    }
  }

  static void printSeriesDoseHtml(SeriesDose seriesDose, PrintWriter out) {
    out.println("<h3>Series Dose</h3>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Dose Number</th>");
    out.println("    <td>" + seriesDose.getDoseNumber() + "</td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <th>AntigenSeries</th>");
    out.println("    <td>" + seriesDose.getAntigenSeries().getSeriesName() + "</td>");
    out.println("  </tr>");
    out.println("<table>");
    if (seriesDose.getAgeList().size() > 0) {
      out.println("<h4>Age</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Absolute Minimum Age</th>");
      out.println("    <th>Minimum Age</th>");
      out.println("    <th>Earliest Recommended Age</th>");
      out.println("    <th>Latest Recommended Age</th>");
      out.println("    <th>Maximum Age</th>");
      out.println("  </tr>");
      for (Age age : seriesDose.getAgeList()) {
        out.println("  <tr>");
        out.println("    <td>" + age.getAbsoluteMinimumAge() + "</td>");
        out.println("    <td>" + age.getMinimumAge() + "</td>");
        out.println("    <td>" + age.getEarliestRecommendedAge() + "</td>");
        out.println("    <td>" + age.getLatestRecommendedAge() + "</td>");
        out.println("    <td>" + age.getMaximumAge() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
    if (seriesDose.getIntervalList().size() > 0) {
      out.println("<h4>Interval</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>From Immediate Previous Dose Administered</th>");
      out.println("    <th>From Target Dose Number In Series</th>");
      out.println("    <th>Absolute Minimum Interval</th>");
      out.println("    <th>Minimum Interval</th>");
      out.println("    <th>Earliest Recommended Interval</th>");
      out.println("    <th>Latest RecommendedInterval</th>");
      out.println("  </tr>");
      for (Interval interval : seriesDose.getIntervalList()) {
        out.println("  <tr>");
        out.println("    <td>" + interval.getFromImmediatePreviousDoseAdministered() + "</td>");
        out.println("    <td>" + interval.getFromTargetDoseNumberInSeries() + "</td>");
        out.println("    <td>" + interval.getAbsoluteMinimumInterval() + "</td>");
        out.println("    <td>" + interval.getMinimumInterval() + "</td>");
        out.println("    <td>" + interval.getEarliestRecommendedInterval() + "</td>");
        out.println("    <td>" + interval.getLatestRecommendedInterval() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
    if (seriesDose.getAllowableintervalList().size() > 0) {
      out.println("<h4>Allowable Interval</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>From Immediate Previous Dose Administered</th>");
      out.println("    <th>From Target Dose Number In Series</th>");
      out.println("    <th>Absolute Minimum Interval</th>");
      out.println("  </tr>");
      for (AllowableInterval ainterval : seriesDose.getAllowableintervalList()) {
        out.println("  <tr>");
        out.println("    <td>" + ainterval.getFromImmediatePreviousDoseAdministered() + "</td>");
        out.println("    <td>" + ainterval.getFromTargetDoseNumberInSeries() + "</td>");
        out.println("    <td>" + ainterval.getAbsoluteMinimumInterval() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (seriesDose.getRecurringDoseList().size() > 0) {
      out.println("<h4>Recurring Dose</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Value</th>");
      out.println("  </tr>");
      for (RecurringDose recurringDose : seriesDose.getRecurringDoseList()) {
        out.println("  <tr>");
        out.println("    <td>" + recurringDose.getValue() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (seriesDose.getConditionalNeedList().size() > 0) {
      out.println("<h4>Conditional Need</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Conditional Set</th>");
      out.println("    <th>Conditional Start Date</th>");
      out.println("    <th>Conditional End Date</th>");
      out.println("    <th>Dose Count</th>");
      out.println("  </tr>");
      for (ConditionalNeed conditionalNeed : seriesDose.getConditionalNeedList()) {
        out.println("  <tr>");
        out.println("    <td>" + conditionalNeed.getConditionalSet() + "</td>");
        out.println("    <td>" + seriesDoseDate(conditionalNeed.getConditionalStartDate()) + "</td>");
        out.println("    <td>" + seriesDoseDate(conditionalNeed.getConditionalEndDate()) + "</td>");
        out.println("    <td>" + conditionalNeed.getDoseCount() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (seriesDose.getSeasonalRecommendationList().size() > 0) {
      out.println("<h4>Seasonal Recommendation</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Seasonal Recommendation Start Date</th>");
      out.println("    <th>Seasonal Recommendation End Date</th>");
      out.println("  </tr>");
      for (SeasonalRecommendation seasonalRecommendation : seriesDose.getSeasonalRecommendationList()) {
        out.println("  <tr>");
        out.println(
            "    <td>" + seriesDoseDate(seasonalRecommendation.getSeasonalRecommendationStartDate()) + "</td>");
        out.println(
            "    <td>" + seriesDoseDate(seasonalRecommendation.getSeasonalRecommendationEndDate()) + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (seriesDose.getSubstituteDoseList().size() > 0) {
      out.println("<h4>Subsititute Dose</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>First Dose Begin Age</th>");
      out.println("    <th>First Dose End Age</th>");
      out.println("    <th>Total Count of Valid Doses</th>");
      out.println("    <th>Number of Target Doses to Substitute</th>");
      out.println("  </tr>");
      for (SubstituteDose substituteDose : seriesDose.getSubstituteDoseList()) {
        out.println("  <tr>");
        out.println("    <td>" + substituteDose.getFirstDoseBeginAge() + "</td>");
        out.println("    <td>" + substituteDose.getFirstDoseEndAge() + "</td>");
        out.println("    <td>" + substituteDose.getTotalCountOfValidDoses() + "</td>");
        out.println("    <td>" + substituteDose.getNumberOfTargetDosesToSubstitue() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (seriesDose.getRequiredGenderList().size() > 0) {
      out.println("<h4>Required Gender</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Required Gender</th>");
      out.println("  </tr>");
      for (RequiredGender requiredGender : seriesDose.getRequiredGenderList()) {
        out.println("  <tr>");
        out.println("    <td>" + requiredGender.getValue() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (seriesDose.getPreferrableVaccineList().size() > 0) {
      out.println("<h4>Preferrable Vaccine</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Lot Expiration Date</th>");
      out.println("    <th>Manufacturer</th>");
      out.println("    <th>Trade Name</th>");
      out.println("    <th>Vaccine Type</th>");
      out.println("    <th>Vaccine Type Begin Age</th>");
      out.println("    <th>Vaccine Type End Age</th>");
      out.println("    <th>Volume</th>");
      out.println("  </tr>");
      for (PreferrableVaccine preferrableVaccine : seriesDose.getPreferrableVaccineList()) {
        out.println("  <tr>");
        out.println("    <td>" + seriesDoseDate(preferrableVaccine.getLotExpirationDate()) + "</td>");
        out.println("    <td>" + preferrableVaccine.getManufacturer() + "</td>");
        out.println("    <td>" + preferrableVaccine.getTradeName() + "</td>");
        out.println("    <td>" + preferrableVaccine.getVaccineType() + "</td>");
        out.println("    <td>" + preferrableVaccine.getVaccineTypeBeginAge() + "</td>");
        out.println("    <td>" + preferrableVaccine.getVaccineTypeEndAge() + "</td>");
        out.println("    <td>" + preferrableVaccine.getVolume() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
    if (seriesDose.getAllowableVaccineList().size() > 0) {
      out.println("<h4>Allowable Vaccine</h4>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Lot Expiration Date</th>");
      out.println("    <th>Manufacturer</th>");
      out.println("    <th>Trade Name</th>");
      out.println("    <th>Vaccine Type</th>");
      out.println("    <th>Vaccine Type Begin Age</th>");
      out.println("    <th>Vaccine Type End Age</th>");
      out.println("    <th>Volume</th>");
      out.println("  </tr>");
      for (AllowableVaccine allowableVaccine : seriesDose.getAllowableVaccineList()) {
        out.println("  <tr>");
        out.println("    <td>" + seriesDoseDate(allowableVaccine.getLotExpirationDate()) + "</td>");
        out.println("    <td>" + allowableVaccine.getManufacturer() + "</td>");
        out.println("    <td>" + allowableVaccine.getTradeName() + "</td>");
        out.println("    <td>" + allowableVaccine.getVaccineType() + "</td>");
        out.println("    <td>" + allowableVaccine.getVaccineTypeBeginAge() + "</td>");
        out.println("    <td>" + allowableVaccine.getVaccineTypeEndAge() + "</td>");
        out.println("    <td>" + allowableVaccine.getVolume() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
  }

  private static void printApplyGeneralVaccineGroupRulesPre(ApplyGeneralVaccineGroupRules step, PrintWriter out) {
    printApplyGeneralVaccineGroupRulesStandard(step, out);
  }

  private static void printApplyGeneralVaccineGroupRulesPost(ApplyGeneralVaccineGroupRules step, PrintWriter out) {
    printApplyGeneralVaccineGroupRulesStandard(step, out);
  }

  private static void printApplyGeneralVaccineGroupRulesStandard(ApplyGeneralVaccineGroupRules step,
      PrintWriter out) {
    out.println(
        "<p>Classify vaccine group provides initial questioning to determine which vaccine group forecast rules to apply.</p>");

    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printClassifyScorablePatientSeriesPre(ClassifyScorablePatientSeries step, PrintWriter out) {
    printClassifyScorablePatientSeriesStandard(step, out);
  }

  private static void printClassifyScorablePatientSeriesPost(ClassifyScorablePatientSeries step, PrintWriter out) {
    printClassifyScorablePatientSeriesStandard(step, out);
  }

  private static void printClassifyScorablePatientSeriesStandard(ClassifyScorablePatientSeries step,
      PrintWriter out) {
    out.println(
        "<p>Classify  patient series  is an attempt to reduce  the total number of  patient series  to only those  which have  a chance to be selected as the best patient series.</p>");

    // printConditionAttributesTable(out);
    printLogicTables(step, out);
    printBestPatientSeries(step, out);
  }

  private static void printCompletePatientSeriesPre(CompletePatientSeries step, PrintWriter out) {
    printCompletePatientSeriesStandard(step, out);
  }

  private static void printCompletePatientSeriesPost(CompletePatientSeries step, PrintWriter out) {
    printCompletePatientSeriesStandard(step, out);
  }

  private static void printCompletePatientSeriesStandard(CompletePatientSeries step, PrintWriter out) {
    out.println(
        "<p>Complete  patient  series  provides  the  decision  table  for  determining  the  "
            + "number  of  points  to  assign  to  a complete patient series based on a specified condition. </p>");
    printCompletePatientSeriesTable(out);
    printBestPatientSeries(step, out);
  }

  private static void printCompletePatientSeriesTable(PrintWriter out) {
    out.println("");
    out.println("<table BORDER=\"1\"> ");
    out.println("  <tr> ");
    out.println(" <th> Conditions </th> ");
    out.println(" <th> If this condition is true for the candidate patient series </th> ");
    out.println(" <th>If this condition is true for two or more candidate patient series </th> ");
    out.println(" <th>If this condition is not true for the candidate patient serie </th> ");
    out.println("  </tr> ");
    out.println("  <tr> ");
    out.println(" <td >A candidate patient series has the most valid doses.</th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> 0</td> ");
    out.println(" <td align=\"center\"> -1 </td> ");
    out.println("  </tr> ");
    out.println("</table>");
  }

  private static void printCreateRelevantPatientSeriesPre(CreateRelevantPatientSeries step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println("   <h2>Antigen Series</h2>");
    out.println("   <table>");
    if (dataModel.getAntigenSelectedList() == null) {
      java.util.Set<Antigen> antigenSet = new java.util.HashSet<Antigen>();
      for (VaccineDoseAdministered vda : dataModel.getImmunizationHistory()
          .getVaccineDoseAdministeredList()) {
        for (Antigen antigen : vda.getVaccine().getVaccineType().getAntigenList()) {
          antigenSet.add(antigen);
        }
      }
      out.println("     <tr>");
      out.println("       <th>Include</th>");
      out.println("       <th>Antigen</th>");
      out.println("     </tr>");
      int i = 1;
      for (Antigen antigen : dataModel.getAntigenList()) {
        String checked = "";
        if (antigenSet.contains(antigen)) {
          checked = " checked";
        }
        out.println("     <tr>");
        out.println("       <td><input type=\"checkbox\" name=\"" + LogicStep.PARAM_ANTIGEN_INCLUDE + i
            + "\" value=\"true\"" + checked + "></td>");
        out.println("       <td>" + antigen.getName() + "</td>");
        out.println("     </tr>");
        i++;
      }
    } else {
      out.println("     <tr>");
      out.println("       <th>Antigen</th>");
      out.println("     </tr>");
      for (Antigen antigen : dataModel.getAntigenSelectedList()) {
        out.println("     <tr>");
        out.println("       <td>" + antigen.getName() + "</td>");
        out.println("     </tr>");
      }
    }
    out.println("   </table>");
  }

  private static void printCreateRelevantPatientSeriesPost(CreateRelevantPatientSeries step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println(
        "   <p>An antigen series is one way to reach perceived immunity against a disease.  "
            + "An antigen series can be thought of as a \"path to immunity\" and is described in "
            + "relative terms.  In many cases, a single antigen may have more than one successful "
            + "path to immunity and as such may have more than one antigen series.  Antigen "
            + "series are defined through supporting data spreadsheets defined in chapter 3.</p>");

    out.println("   <h2>Patient Series Included</h2>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Series Name</th>");
    out.println("       <th>Target Disease</th>");
    out.println("       <th>Vaccine Group</th>");
    out.println("     </tr>");
    for (PatientSeries patientSeries : dataModel.getPatientSeriesStepper().getList()) {
      org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries antigenSeries = patientSeries
          .getTrackedAntigenSeries();
      out.println("     <tr>");
      out.println("       <td>" + antigenSeries.getSeriesName() + "</td>");
      out.println("       <td>" + antigenSeries.getTargetDisease() + "</td>");
      out.println("       <td>" + (antigenSeries.getVaccineGroup() == null ? ""
          : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
  }

  private static void printDetermineBestPatientSeriesPre(DetermineBestPatientSeries step, PrintWriter out) {
    printDetermineBestPatientSeriesStandard(step, out);
  }

  private static void printDetermineBestPatientSeriesPost(DetermineBestPatientSeries step, PrintWriter out) {
    printDetermineBestPatientSeriesStandard(step, out);
  }

  private static void printDetermineBestPatientSeriesStandard(DetermineBestPatientSeries step, PrintWriter out) {
    out.print("<h4> " + step.getDataModel().getAntigen().getName() + " </h4>");
    printBestPatientSeries(step, out);
    printLogicTables(step, out);
  }

  private static void printDetermineContraindicationsPre(DetermineContraindications step, PrintWriter out) {
    printDetermineContraindicationsStandard(step, out);
  }

  private static void printDetermineContraindicationsPost(DetermineContraindications step, PrintWriter out) {
    printDetermineContraindicationsStandard(step, out);
  }

  private static void printDetermineContraindicationsStandard(DetermineContraindications step, PrintWriter out) {
    out.println("<p>Placeholder text</p>");
  }

  private static void printDetermineEvidenceOfImmunityPre(DetermineEvidenceOfImmunity step, PrintWriter out) {
    printDetermineEvidenceOfImmunityStandard(step, out);
  }

  private static void printDetermineEvidenceOfImmunityPost(DetermineEvidenceOfImmunity step, PrintWriter out) {
    printDetermineEvidenceOfImmunityStandard(step, out);
  }

  private static void printDetermineEvidenceOfImmunityStandard(DetermineEvidenceOfImmunity step, PrintWriter out) {
    out.println(
        "<p>Determine evidence of immunity  assesses the patient’s profile to determine if the patient is already potentially immune to the target disease, negating the need for additional doses.</p>");
    out.println(
        "<p>A patient may be considered immune due to their clinical history or if they were born before a defined date for the given target disease.</p>");
    out.println("<img src=\"Figure 7.2.png\"/>");
    out.println("<p>FIGURE 7 - 2 EVIDENCE OF IMMUNITY PROCESS MODEL</p>");

    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printDetermineForecastNeedPre(DetermineForecastNeed step, PrintWriter out) {
    printDetermineForecastNeedStandard(step, out);
  }

  private static void printDetermineForecastNeedPost(DetermineForecastNeed step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    printDetermineForecastNeedStandard(step, out);

    if (!dataModel.getPatient().getMedicalHistory().getContraindicationSet().isEmpty()) {
      out.println("<h2>Contraindications for Patient</h2>");
      for (org.openimmunizationsoftware.cdsi.core.domain.Contraindication_TO_BE_REMOVED contraindication : dataModel
          .getPatient().getMedicalHistory()
          .getContraindicationSet()) {
        if (contraindication.getAntigen()
            .equals(dataModel.getPatientSeriesStepper().getCurrent().getTrackedAntigenSeries().getTargetDisease())) {
          out.println("<li>" + contraindication + "</li>");
        }
      }
    }
  }

  private static void printDetermineForecastNeedStandard(DetermineForecastNeed step, PrintWriter out) {
    out.println(
        "<p>Determine forecast need determines  if there is a need to forecast dates. This involves reviewing patient data, antigen  administered  records,  and  patient  series.  This  is  a  prerequisite  before  a  CDS  engine  can  produce forecast dates and reasons </p>");
    out.println(
        "<p>The following process model, attribute table, and decision table are used to determine the need to generate forecast dates.</p>");
    out.println("<img src=\"Figure 7.6.png\"/>");
    out.println("<p>FIGURE 7 - 6 DETERMINE FORECAST NEED PROCESS MODEL</p>");
    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printEvaluateAllowableIntervalPre(EvaluateAllowableInterval step, PrintWriter out) {
    printEvaluateAllowableIntervalStandard(step, out);
  }

  private static void printEvaluateAllowableIntervalPost(EvaluateAllowableInterval step, PrintWriter out) {
    printEvaluateAllowableIntervalStandard(step, out);
  }

  private static void printEvaluateAllowableIntervalStandard(EvaluateAllowableInterval step, PrintWriter out) {
    out.println(
        "<p>Evaluate allowable interval validates the date administered of a vaccine dose administered against defined allowable interval(s) from previous vaccine dose(s) administered.  In rare cases, intervals can be applied which are either abnormally early – usually specified in ACIP footnotes or subsequent clarifications – or intervals which differ following a not valid administration.</p>");
    out.println(
        "<p>In cases where a target dose does not specify allowable interval attributes, evaluate allowable interval cannot be used to validate a vaccine dose administered.  To avoid a false validation, the allowable interval should be considered \"not valid\" in these cases.</p>");
    out.println(
        "<p>The figure below provides evaluate allowable interval timeline used to define all adjacent intervals by using from immediate previous dose administered as the reference dose.</p>");
    out.println("<img src=\"Figure 4.10.PNG\"/>");
    out.println(
        "<p>FIGURE 4 - 10 EVALUATE ALLOWABLE INTERVAL 'FROM IMMEDIATE PREVIOUS DOSE' TIMELINE</p>");
    out.println(
        "<p>The figure below illustrates evaluate allowable interval timeline used to define all non-adjacent intervals by using from target dose number in series as the reference dose.</p>");
    out.println("<img src=\"Figure 4.11.PNG\"/>");
    out.println(
        "<p>FIGURE 4 - 11 EVALUATE ALLOWABLE INTERVAL 'FROM TARGET DOSE NUMBER IN SERIES' TIMELINE</p>");
    out.println(
        "<p>The following process model, attribute table, decision table, and business rule table are used to evaluate interval of a vaccine dose administered.</p>");
    out.println("<img src=\"Figure 4.12.PNG\"/>");
    out.println("<p>FIGURE 6 - 14 EVALUATE ALLOWABLE INTERVAL PROCESS MODEL</p>");
    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
    if (step.getLogicTableList().size() == 0) {
      out.println("<p>No allowable intervals defined. Interval defaulting to 'Not Valid'</p>");
    }
  }

  private static void printEvaluateAndForecastAllPatientSeriesPre(EvaluateAndForecastAllPatientSeries step,
      PrintWriter out) {
    out.println(
        "<p>This step is the core of the business logic and decision points many people think of when describing evaluation and forecasting. In the Logic Specification, this step contains all of the clinical business rules and decision logic in the form of business rules and decision tables.</p>");
    out.println(
        "<p>At the end of this step, each patient series will have an evaluated history and a forecast.</p>");
  }

  private static void printEvaluateAndForecastAllPatientSeriesPost(EvaluateAndForecastAllPatientSeries step,
      PrintWriter out) {
    out.println(
        "<p>This step is the core of the business logic and decision points many people think of when describing evaluation and forecasting. In the Logic Specification, this step contains all of the clinical business rules and decision logic in the form of business rules and decision tables.</p>");
    out.println(
        "<p>At the end of this step, each patient series will have an evaluated history and a forecast.</p>");
    out.println("<h2>Selected Patient Series</h2>");
    if (step.getDataModel().getPatientSeriesStepper().getList() == null) {
      out.println("<p>No patient series to process</p>");
    }
  }

  private static void printEvaluateConditionalSkipForEvaluationPre(EvaluateConditionalSkipForEvaluation step,
      PrintWriter out) {
    printEvaluateConditionalSkipForEvaluationStandard(step, out);
  }

  private static void printEvaluateConditionalSkipForEvaluationPost(EvaluateConditionalSkipForEvaluation step,
      PrintWriter out) {
    printEvaluateConditionalSkipForEvaluationStandard(step, out);
  }

  private static void printEvaluateConditionalSkipForEvaluationStandard(EvaluateConditionalSkipForEvaluation step,
      PrintWriter out) {
    out.println(
        "<p>Evaluate Conditional Skip addresses times when a target dose can be skipped. A dose should be considered necessary unless it is determined that it can be skipped. The most common scenarios for skipping a dose are:</p>");
    out.println("<ul>");
    out.println(
        "<li>Catch-up doses where the patient is current with their administrations and does not need to catch-up</lui>");
    out.println(
        "<li>The patient is behind schedule and the total number of doses needed to satisfy the patient series can be reduced</lui>");
    out.println(
        "<li>The previously administered dose(s) negates the need for the current target dose</lui>");
    out.println("</ul>");

    out.println(
        "<p>In cases where a target dose does not specify Conditional Skip attributes, the target dose cannot be skipped.</p>");
    out.println(
        "<p>A dose may be skipped based on whether or not one or more conditions evaluates to true. Conditions are classified as one of a number of types, each with one or more parameters in the Supporting Data. Conditions are contained within sets. Each set contains one or more conditions to be evaluated. Within a set, one or more conditions must be met for the set to be met. In the case where a set contains multiple conditions, whether all conditions or just one condition must be met is specified by the Condition Logic (e.g., AND vs. OR). Similarly, a dose may contain multiple sets. In the case where a dose contains multiple sets, whether all sets or just one set must be met is specified by the Set Logic.</p>");
    out.println(
        "<p>Finally, in an effort to reduce page size and eliminate duplicate logic which could result in typographical and consistency errors, this section of logic is defined here once, but used in both Evaluation and Forecasting. The forecasting chapter refers the reader back to this section for appropriate logic.</p>");

    out.println("<img src=\"Figure 6.3.png\"/>");
    out.println("<p>FIGURE 6 - 3 CONDITIONAL SKIP PROCESS MODEL</p>");
    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printEvaluateConditionalSkipForForecastPre(EvaluateConditionalSkipForForecast step,
      PrintWriter out) {
    printEvaluateConditionalSkipForForecastStandard(step, out);
  }

  private static void printEvaluateConditionalSkipForForecastPost(EvaluateConditionalSkipForForecast step,
      PrintWriter out) {
    printEvaluateConditionalSkipForForecastStandard(step, out);
  }

  private static void printEvaluateConditionalSkipForForecastStandard(EvaluateConditionalSkipForForecast step,
      PrintWriter out) {
    out.println(
        "<p>Evaluate Conditional Skip addresses times when a target dose can be skipped. A dose should be considered necessary unless it is determined that it can be skipped. The most common scenarios for skipping a dose are:</p>");
    out.println("<ul>");
    out.println(
        "    <li>Catch-up doses where the patient is current with their administrations and does not need to catch-up</li>");
    out.println(
        "    <li>The patient is behind schedule and the total number of doses needed to satisfy the patient series can be reudced</li>");
    out.println(
        "    <li>The previously administered dose(s) negates the need for the current target dose</li>");
    out.println("</ul>");
    out.println(
        "<p>In cases where a target dose does not specify Conditional Skip attributes, the target dose cannot be skipped.</p>");
    out.println(
        "<p>The process model, attribute table, and decision table are used to determine if the target dose can be skipped is the same as described in Chapter 4.2.</p>");

    out.println("<img src=\"Figure 6.3.png\"/>");
    out.println("<p>FIGURE 6 - 3 CONDITIONAL SKIP PROCESS MODEL</p>");
    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printEvaluateDoseAdministeredConditionPre(EvaluateDoseAdministeredCondition step,
      PrintWriter out) {
    printEvaluateDoseAdministeredConditionStandard(step, out);
  }

  private static void printEvaluateDoseAdministeredConditionPost(EvaluateDoseAdministeredCondition step,
      PrintWriter out) {
    printEvaluateDoseAdministeredConditionStandard(step, out);
  }

  private static void printEvaluateDoseAdministeredConditionStandard(EvaluateDoseAdministeredCondition step,
      PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println(
        "<p>Target dose : " + dataModel.getTargetDose().getTrackedSeriesDose().getDoseNumber() + " "
            + dataModel.getTargetDose().getTrackedSeriesDose().getAntigenSeries().getSeriesName()
            + " </p>");
    out.println(
        "<p>Dose administered condition checks the dose administered to see if the dose must be repeated regardless of the other evaluation rules.</p>");
    out.println("<p>Relationship to ACIP recommendations:</p>");
    out.println("<ul>");
    out.println(
        "  <li>Doses which were administered after the lot expiration date or which contain a condition do not need to be evaluated.</li>");
    out.println(
        "  <li>Examples of conditions which would prevent evaluation of dose range from misadministration to recalls to cold chain breaks.</li>");
    out.println("</ul>");
    out.println("<img src=\"Figure 4.2.png\"/>");
    out.println("<p>FIGURE 4 - 2 VACCINE DOSE ADMINISTERED CONDITION PROCESS MODEL</p>");

    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printEvaluateForAllowableVaccinePre(EvaluateForAllowableVaccine step, PrintWriter out) {
    printEvaluateForAllowableVaccineStandard(step, out);
  }

  private static void printEvaluateForAllowableVaccinePost(EvaluateForAllowableVaccine step, PrintWriter out) {
    printEvaluateForAllowableVaccineStandard(step, out);
  }

  private static void printEvaluateForAllowableVaccineStandard(EvaluateForAllowableVaccine step, PrintWriter out) {
    out.println(
        "<p>Evaluate for allowable vaccine validates the vaccine of a vaccine dose administered against the list of allowable vaccines. </p>");
    out.println(
        "<p>Figures 6-20 depicts a patient who received an allowable vaccine while figure 6-21 depicts a patient who did not receive an allowable vaccine.</p>");
    out.println("<img src=\"Figure 6.20.PNG\"/>");
    out.println("<p>FIGURE 6 - 20 PATIENT RECEIVED AN ALLOWABLE VACCINE</p>");
    out.println("<img src=\"Figure 6.21.PNG\"/>");
    out.println("<p>FIGURE 6 - 21 PATIENT DID NOT RECEIVE AN ALLOWABLE VACCINE</p>");
    out.println(
        "<p>The following process model, attribute table, decision table, and business rule table are used to evaluate for an allowable vaccine.</p>");
    out.println("<img src=\"Figure 6.22.PNG\"/>");
    out.println("<p>FIGURE 6 - 22 EVALUATE FOR AN ALLOWABLE VACCINE PROCESS MODEL</p>");
    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printEvaluateForInadvertentVaccinePre(EvaluateForInadvertentVaccine step, PrintWriter out) {
    printEvaluateForInadvertentVaccineStandard(step, out);
  }

  private static void printEvaluateForInadvertentVaccinePost(EvaluateForInadvertentVaccine step, PrintWriter out) {
    printEvaluateForInadvertentVaccineStandard(step, out);
  }

  private static void printEvaluateForInadvertentVaccineStandard(EvaluateForInadvertentVaccine step,
      PrintWriter out) {
    printLogicTables(step, out);
  }

  private static void printEvaluateForPreferableVaccinePre(EvaluateForPreferableVaccine step, PrintWriter out) {
    printEvaluateForPreferableVaccineStandard(step, out);
  }

  private static void printEvaluateForPreferableVaccinePost(EvaluateForPreferableVaccine step, PrintWriter out) {
    printEvaluateForPreferableVaccineStandard(step, out);
  }

  private static void printEvaluateForPreferableVaccineStandard(EvaluateForPreferableVaccine step, PrintWriter out) {
    out.println(
        "<p>Evaluate for preferable vaccine validates the vaccine of a vaccine dose administered against the list of preferable vaccines.</p>");
    out.println(
        "<p>Figures 6-17 depicts a patient who received a preferable vaccine while figure 6-18 depicts a patient who did not receive a preferable vaccine.</p>");
    out.println("<img src=\"Figure 6.17.PNG\"/>");
    out.println("<p>FIGURE 6 - 17 PATIENT RECEIVED A PREFERABLE VACCINE</p>");
    out.println("<img src=\"Figure 6.18.PNG\"/>");
    out.println("<p>FIGURE 6 - 18 PATIENT DID NOT RECEIVE A PREFERABLE VACCINE</p>");
    out.println(
        "<p>It should be noted that volume is sparsely populated and tracked differently in most systems. Therefore, volume will not be used to evaluate the validity of a vaccine dose administered. However, it will be provided as an evaluation reason that less than sufficient volume was administered.</p>");
    out.println(
        "<p>The following process model, attribute table, decision table, and business rule table are used to evaluate for a preferable vaccine.</p>");
    out.println("<img src=\"Figure 6.19.PNG\"/>");
    out.println("<p>FIGURE 6 - 19 EVALUATE FOR A PREFERABLE VACCINE PROCESS MODEL</p>");
    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printEvaluateGenderPre(EvaluateGender step, PrintWriter out) {
    printEvaluateGenderStandard(step, out);
  }

  private static void printEvaluateGenderPost(EvaluateGender step, PrintWriter out) {
    printEvaluateGenderStandard(step, out);
  }

  private static void printEvaluateGenderStandard(EvaluateGender step, PrintWriter out) {
    out.println(
        "<p>Evaluate gender  validates the  patient gender  against the  required  gender.  In cases where a  target dose  does not specify gender attributes, the gender is valid.</p>");

    out.println(
        "<p>The following process model, attribute table, and decision table are used to evaluate the gender.</p>");
    out.println("<img src=\"Figure 4.21.PNG\"/>");
    out.println("<p>FIGURE 4 - 21 GENDER PROCESS MODEL</p>");

    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printEvaluatePreferableIntervalPre(EvaluatePreferableInterval step, PrintWriter out) {
    printEvaluatePreferableIntervalStandard(step, out);
  }

  private static void printEvaluatePreferableIntervalPost(EvaluatePreferableInterval step, PrintWriter out) {
    printEvaluatePreferableIntervalStandard(step, out);
  }

  private static void printEvaluatePreferableIntervalStandard(EvaluatePreferableInterval step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println(
        "<p>Evaluate interval validates the date administered of a vaccine dose administered against defined interval(s) from previous vaccine dose(s) administered. In cases where a target dose does not specify interval attributes, the interval is considered \"valid.\"</p>");
    out.println("<p>Intervals can be measures in three different ways:</p>");
    out.println("<ul>");
    out.println(
        "  <li>\"From Immediate Previous Dose Administered\" requires the interval to be evaluated from the immediate previous vaccine dose administered and is used in the majority of cases.</li>");
    out.println(
        "  <li>\"From Target Dose # in Series\" requires the interval to be evaluated from the date of the specified dose. </li>");
    out.println(
        "  <li>\"From Most Recent\" requires the interval to be evaluated from the date of the most recently administered dose of a specific vaccine type (e.g., this is used in Pneumococcal to ensure proper spacing between the different intervals between PCV13 and PPSV23).</li>");
    out.println("</ul>");
    out.println(
        "<p>It is possible for a given dose to use multiple interval types. For example, dose 3 of HepB and dose 3 of HPV, each have two intervals.  The first interval is from the immediate previous vaccine dose administered.  The second interval is from satisfied target dose 1 in each respective series. Note that if multiple intervals are specified, then all intervals must be satisfied in order for the dose to satisfy the interval requirements.</p>");
    out.println(
        "<p>Figure 4-6 provides the evaluation interval timeline used to define adjacent intervals by using from immediate previous dose administered as the reference point.</p>");
    out.println("<img src=\"Figure 4.6.PNG\"/>");
    out.println("<p>FIGURE 4 - 6 EVALUATE INTERVAL 'FROM IMMEDIATE PREVIOUS DOSE' TIMELINE</p>");
    out.println("<img src=\"Figure 4.7.png\"/>");
    out.println(
        "<p>FIGURE 4 - 7 EVALUATE INTERVAL 'FROM TARGET DOSE NUMBER IN SERIES' TIMELINE</p>");
    out.println("<img src=\"Figure 4.8.PNG\"/>");
    out.println(
        "<p>FIGURE 4 - 8 EVALUATE INTERVAL ‘FROM MOST RECENT DOSE OF SPECIFIED VACCINE TYPE’ TIMELINE</p>");
    out.println("<img src=\"Figure 4.9.png\"/>");
    out.println("<p>FIGURE 4 - 9 EVALUATE INTERVAL PROCESS MODEL</p>");
    out.println("<h2>Intervals</h2>");
    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    if (seriesDose.getIntervalList().size() == 0) {
      out.println("<p>No intervals for series dose " + seriesDose + "</p>");
    } else {
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>From Immediate Previous Dose Administered?</th>");
      out.println("    <th>From Target Dose # in Series</th>");
      out.println("    <th>From Most Recent</th>");
      out.println("    <th>Absolute Minimum Interval</th>");
      out.println("    <th>Minimum Interval/th>");
      out.println("    <th>Earliest Recommended Interval</th>");
      out.println("    <th>Latest Recommended Interval (less than)</th>");
      out.println("    <th>Interval Priority Flag</th>");
      out.println("  </tr>");
      for (Interval interval : seriesDose.getIntervalList()) {
        out.println("  <tr>");
        out.println("    <td>" + interval.getFromImmediatePreviousDoseAdministered() + "</td>");
        out.println("    <td>" + interval.getFromTargetDoseNumberInSeries() + "</td>");
        out.println("    <td>?" + "</td>");
        out.println("    <td>" + interval.getAbsoluteMinimumInterval() + "</td>");
        out.println("    <td>" + interval.getMinimumInterval() + "</td>");
        out.println("    <td>" + interval.getEarliestRecommendedInterval() + "</td>");
        out.println("    <td>" + interval.getLatestRecommendedInterval() + "</td>");
        out.println("    <td>?" + "</td>");
        out.println("  </tr>");
      }

      out.println("</table>");
    }

    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printEvaluateVaccineConflictPre(EvaluateVaccineConflict step, PrintWriter out) {
    printEvaluateVaccineConflictStandard(step, out);
  }

  private static void printEvaluateVaccineConflictPost(EvaluateVaccineConflict step, PrintWriter out) {
    printEvaluateVaccineConflictStandard(step, out);
  }

  private static void printEvaluateVaccineConflictStandard(EvaluateVaccineConflict step, PrintWriter out) {
    out.println(
        "<p>Evaluate live virus conflict validates the date administered of a live virus vaccine dose administered against previous live virus administered vaccines to ensure proper spacing between administrations. For some live virus vaccines and for inactivated virus or recombinant vaccines, this condition does not exist. Therefore, if no live virus supporting data exists for the vaccine dose administered being evaluated, the vaccine dose administered is not in conflict with any other vaccine dose administered.</p>");
    out.println("<img src=\"Figure 4.13.PNG\"/>");
    out.println("<p>FIGURE 4 - 13 EVALUATE LIVE VIRUS CONFLICT TIMELINE</p>");
    out.println(
        "<p>The following process model, attribute table, decision tables, and business rule table are used to evaluate for a live virus conflict.</p>");
    out.println("<img src=\"Figure 4.14.PNG\"/>");
    out.println("<p>FIGURE 4 - 14 EVALUATE LIVE VIRUS CONFLICT PROCESS MODEL</p>");
    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printForecastDatesAndReasonsPre(ForecastDatesAndReasons step, PrintWriter out) {
    printForecastDatesAndReasonsStandard(step, out);
  }

  private static void printForecastDatesAndReasonsPost(ForecastDatesAndReasons step, PrintWriter out) {
    printForecastDatesAndReasonsStandard(step, out);
  }

  private static void printForecastDatesAndReasonsTableAndFigure(PrintWriter out) {
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Section</th>");
    out.println("    <th>Activity</th>");
    out.println("    <th>Goal</th>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>5.1</td>");
    out.println("    <td>Evaluate Dose Conditional Skip</td>");
    out.println(
        "    <td>The goal of this step is to determine if the target dose can be skipped due to a patient‘s age at assessment or immunization history.</td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>5.2</td>");
    out.println("    <td>Determine Evidence of Immunity</td>");
    out.println(
        "    <td>The goal of this step is to determine if the patient has evidence of immunity.");
    out.println("</td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>5.3</td>");
    out.println("    <td>Determine Forecast Need</td>");
    out.println("    <td>The goal of this step is to determine if the patient should");
    out.println("receive another dose.</td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>5.4</td>");
    out.println("    <td>Generate Forecast Dates</td>");
    out.println("    <td>The goal of this step is to generate forecast dates for the");
    out.println("next target dose.</td>");
    out.println("  </tr>");
    out.println("</table>");
    out.println("");
    out.println(
        "<p>The figure below provides an illustration of the forecast dates and reasons process.</p>");
    out.println("");
    out.println("<img src=\"Figure 5.1.png\"/>");
  }

  private static void printForecastDatesAndReasonsStandard(ForecastDatesAndReasons step, PrintWriter out) {
    out.println(
        "<p>The CDS engine uses a patient's medical and vaccine history to forecast immunization due dates. This chapter identifies specific business rules that are used by a CDS engine to forecast the next  target dose.  The major steps involved in this process are listed in the table below.</p>");
    printForecastDatesAndReasonsTableAndFigure(out);
    org.openimmunizationsoftware.cdsi.core.domain.TargetDose targetDose = step.getDataModel().getTargetDose();
    if (targetDose == null) {
      out.println("<p>No Target Dose defined</p>");
    } else {
      out.println("<p>Tracked Series Dose: " + targetDose.getTrackedSeriesDose() + "</p>");
    }
  }

  private static void printGenerateForecastDatesAndRecommendedVaccinesPre(
      GenerateForecastDatesAndRecommendedVaccines step, PrintWriter out) throws Exception {
    printGenerateForecastDatesAndRecommendedVaccinesStandard(step, out);
    generateForecastTablePre(out);
  }

  private static void printGenerateForecastDatesAndRecommendedVaccinesPost(
      GenerateForecastDatesAndRecommendedVaccines step, PrintWriter out) throws Exception {
    printGenerateForecastDatesAndRecommendedVaccinesStandard(step, out);
    generateForecastTablePost(step, out);
  }

  private static void printGenerateForecastDatesAndRecommendedVaccinesStandard(
      GenerateForecastDatesAndRecommendedVaccines step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println(
        "<p>Generate forecast dates  and recommend vaccines  determines the forecast dates for the next  target dose  and identifies one  or  more recommended vaccines if the target dose warrants specific vaccine recommendations. The forecast dates are generated based on the patient’s immunization history. If the patient has not adhered to  the preferred schedule, then the forecast dates  are  adjusted to provide  the best  dates for the next target dose.</p>");
    out.println(
        "<p>Figure 7-7 below provides an illustration of how forecast dates appear on the timeline.</p>");
    out.println("<img src=\"Figure 7.7.png\"/>");
    out.println("<p>FIGURE 7 - 7 FORECAST DATES TIMELINE</p>");
    out.println(
        "<p>The following process model, attribute table, and business rule table are used to generate forecast dates.If an attribute value is empty, then the date calculations will remain empty. No assumptions will be made for the attribute.</p>");
    out.println("<img src=\"Figure 7.8.png\"/>");
    out.print("<p>FIGURE 7 - 8 GENERATE FORECAST DATES AND RECOMMENDED VACCINE PROCESS MODEL</p>");
    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
    out.println("<p>Patient Series Status: " + dataModel.getPatientSeriesStepper().getCurrent().getPatientSeriesStatus()
        + "</p>");
    out.println(
        "<p>" + dataModel.getPatientSeriesStepper().getCurrent().getTrackedAntigenSeries().getSeriesName() + "</p>");
  }

  private static void generateForecastInsertTableRow(PrintWriter out, String businessRuleId, String term,
      String businessRule) {
    out.println("  <tr>");
    out.println("    <td>" + businessRuleId + "</td>");
    out.println("    <td>" + term + "</td> ");
    out.println("    <td>" + businessRule + "</td>");
    out.println("  </tr>");
  }

  private static void generateForecastInsertTableInit(PrintWriter out) {
    out.println("  <tr>");
    out.println("    <th> BusinessRuleID  </th>");
    out.println("    <th> Term </th> ");
    out.println("    <th> BusinessRule </th>");
    out.println("  </tr>");
  }

  private static void generateForecastTablePre(PrintWriter out) {
    out.println("<p>TABLE 7 - 13 GENERATE FORECAST DATE AND RECOMMENDED VACCINE BUSINESS RULES</p>");
    out.println("<table BORDER=\"1\"> ");
    generateForecastInsertTableInit(out);
    generateForecastInsertTableRow(out, "FORECASTDT-1", "Earliest Date", "");
    generateForecastInsertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date", "");
    generateForecastInsertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date", "");
    generateForecastInsertTableRow(out, "FORECASTDT-4", "Latest Date", "");
    generateForecastInsertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date", "");
    generateForecastInsertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date", "");
    generateForecastInsertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", "");
    out.println("</table>");
  }

  private static void generateForecastTablePost(GenerateForecastDatesAndRecommendedVaccines step, PrintWriter out)
      throws java.text.ParseException {
    out.println(
        "<p> TABLE 7 - 13 GENERATE FORECAST DATE AND RECOMMENDED VACCINE BUSINESS RULES</p>");
    out.println("<table BORDER=\"1\"> ");
    generateForecastInsertTableInit(out);
    generateForecastInsertTableRow(out, "FORECASTDT-1", "Earliest Date", step.computeEarliestDate().toString());
    generateForecastInsertTableRow(out, "FORECASTDT-2", "Unadjusted Recommended Date",
        step.computeUnadjustedRecommendedDate().toString());
    generateForecastInsertTableRow(out, "FORECASTDT-3", "Unadjusted Past Due Date",
        LogicStep.n(step.computeUnadjustedPastDueDate()));
    generateForecastInsertTableRow(out, "FORECASTDT-4", "Latest Date", LogicStep.n(step.computeUnadjustedPastDueDate()));
    generateForecastInsertTableRow(out, "FORECASTDT-5", "Adjusted Recommended Date",
        step.computeAdjustedRecommendedDate().toString());
    generateForecastInsertTableRow(out, "FORECASTDT-6", "Adjusted Past Due Date",
        LogicStep.n(step.computeAdjustedPastDueDate()));
    generateForecastInsertTableRow(out, "FORECASTRECVACT-1", "Recommended Vaccine", "recommendedVaccine");
    out.println("</table>");
  }

  private static void printIdentifyAndEvaluateVaccineGroupPre(IdentifyAndEvaluateVaccineGroup step,
      PrintWriter out) {
    printIdentifyAndEvaluateVaccineGroupStandard(step, out);
  }

  private static void printIdentifyAndEvaluateVaccineGroupPost(IdentifyAndEvaluateVaccineGroup step,
      PrintWriter out) {
    printIdentifyAndEvaluateVaccineGroupStandard(step, out);
  }

  private static void printIdentifyAndEvaluateVaccineGroupStandard(IdentifyAndEvaluateVaccineGroup step,
      PrintWriter out) {
    out.println(
        "<p>The  goal  of  identify  and  evaluate  vaccine  group  is  to  merge  together  antigen-based  forecasts  into  vaccine group forecasts. This is especially important in MMR and  DTaP/Tdap/Td vaccine groups which each contain more than one antigen in their respective vaccine groups. In these cases, it is important to provide a forecast consistent  with  the  vaccine  group  rather  than  the  individual  antigen.</p>");

    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printIdentifyOnePrioritizedPatientSeriesPre(IdentifyOnePrioritizedPatientSeries step,
      PrintWriter out) {
    printIdentifyOnePrioritizedPatientSeriesStandard(step, out);
  }

  private static void printIdentifyOnePrioritizedPatientSeriesPost(IdentifyOnePrioritizedPatientSeries step,
      PrintWriter out) {
    printIdentifyOnePrioritizedPatientSeriesStandard(step, out);
  }

  private static void printIdentifyOnePrioritizedPatientSeriesStandard(IdentifyOnePrioritizedPatientSeries step,
      PrintWriter out) {
    out.println(
        "<p>Identify one prioritized patient series examines all of the patient "
            + "series for a given Series Group to determine if one of the patient "
            + "series is superior to all other patient series and can be considered "
            + "the prioritized patient series.</p>");
    printLogicTables(step, out);
    // print out list of patient series
    printPatientSeriesList(step, out);
    printBestPatientSeries(step, out);
  }

  private static void printSkipTargetDoseForForecastPre(SkipTargetDoseForForecast step, PrintWriter out) {
    printSkipTargetDoseForForecastStandard(step, out);
  }

  private static void printSkipTargetDoseForForecastPost(SkipTargetDoseForForecast step, PrintWriter out) {
    printSkipTargetDoseForForecastStandard(step, out);
  }

  private static void printSkipTargetDoseForForecastStandard(SkipTargetDoseForForecast step, PrintWriter out) {
    out.println(
        "<p>Evaluate Conditional Skip  addresses times when  a  target dose can be skipped.  A dose should be considered necessary unless it is determined that it can be skipped</p>");
  }

  private static void printValidateRecommendationPre(ValidateRecommendation step, PrintWriter out) {
    printValidateRecommendationStandard(step, out);
  }

  private static void printValidateRecommendationPost(ValidateRecommendation step, PrintWriter out) {
    printValidateRecommendationStandard(step, out);
  }

  private static void printValidateRecommendationStandard(ValidateRecommendation step, PrintWriter out) {
  }

  private static void printSelectBestPatientSeriesPre(SelectBestPatientSeries step, PrintWriter out) {
    printSelectBestPatientSeriesStandard(step, out);
  }

  private static void printSelectBestPatientSeriesPost(SelectBestPatientSeries step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    printSelectBestPatientSeriesStandard(step, out);
    if (dataModel.getAntigenPos() < dataModel.getAntigenSelectedList().size()) {
      out.println("<p>Now looking at Antigen: " + dataModel.getAntigen() + "</p>");
    } else {
      out.println("<p>Done checking Antigens, moving onto Identify and Evaluate Vaccine Group</p>");
    }
    out.println("<h2>Selected Patient Series</h2>");
    out.println("<ul>");
    for (PatientSeries ps : dataModel.getSelectedPatientSeriesList()) {
      out.println("<li>" + ps.getTrackedAntigenSeries().getSeriesName() + "</li>");
    }
    out.println("</ul>");
    printBestPatientSeries(step, out);
  }

  private static void printSelectBestPatientSeriesStandard(SelectBestPatientSeries step, PrintWriter out) {
    printSelectBestPatientSeriesTableAndFigures(out);
  }

  private static void printSelectBestPatientSeriesTableAndFigures(PrintWriter out) {
    out.println(
        "<p>Select best patient series involves reviewing all potential patient series which might satisfy the goals of an antigen and determining the one series which best fits the patient based on several important factors. The four steps of this process are listed in table 6-1.</p>");
    out.println("<p>TABLE 6 - 1 SELECT BEST PATIENT SERIES PROCESS STEPS</p>");
    out.println("<table>");
    out.println("	<tr>");
    out.println("		<th>Section</th>");
    out.println("		<th>Activity</th>");
    out.println("		<th>Goal</th>");
    out.println("	</tr>");
    out.println("	<tr>");
    out.println("		<td>6.2</td>");
    out.println("		<td>Identify Superior Patient Series </td>");
    out.println(
        "		<td>The goal of this step is to determine if one patient series is superior to the other entire patient series.</td>");
    out.println("	</tr>");
    out.println("	<tr>");
    out.println("		<td>6.3</td>");
    out.println("		<td>Classify Patient Series</td>");
    out.println(
        "		<td>The goal of this step is to classify where the patient is in the overall  path to immunity and pass those candidate patient series onto the next step. Only those patient series with the most likely chance to be considered the best are retained for further consideration.</td>");
    out.println("	</tr>");
    out.println("	<tr>");
    out.println("		<td>6.4-6.6</td>");
    out.println("		<td>Scoring Patient Series</td>");
    out.println(
        "		<td>The goal of this step is to apply the proper scoring business rules based on results of the second step. The scoring business rules will determine the best patient series. Scoring business rules are specific to where the patient is in the overall path to immunity. The complete patient series scoring business rules look at factors important when candidate patient series are complete. Similarly in-process patient series scoring business rules and no valid doses scoring business rules look at factors important to their respective situation. For any given antigen, only one set of these scoring business rules will be applied to each candidate patient series.</td>");
    out.println("	</tr>");
    out.println("	<tr>");
    out.println("		<td>6.7</td>");
    out.println("		<td>Select Best Patient Series</td>");
    out.println(
        "		<td>The goal of this step is to evaluate the scored candidate patient series and determine which of the candidate patient series is the one and only best patient series.</td>");
    out.println("	</tr>");
    out.println("</table>");
    out.println(
        "<p>The process model below illustrates the major steps involved in selecting the best patient series.</p>");
    out.println("<img src=\"Figure 6.1.png\"/>");
    out.println("<p>FIGURE 6 - 1 SELECT BEST PATIENT SERIES PROCESS MODEL</p>");
  }

  private static void printSelectPrioritizedPatientSeriesPre(SelectPrioritizedPatientSeries step, PrintWriter out) {
    printSelectPrioritizedPatientSeriesStandard(step, out);
  }

  private static void printSelectPrioritizedPatientSeriesPost(SelectPrioritizedPatientSeries step, PrintWriter out) {
    printSelectPrioritizedPatientSeriesStandard(step, out);
    out.println("<p>Prioritized Patient Series: " + step.getPrioritizedPatientSeries() + "</p>");
    for (PatientSeries patientSeries : step.getPatientSeriesList()) {
      out.println(
          "<p> PatientSeries : " + patientSeries.getTrackedAntigenSeries().getSeriesName() + " Value : "
              + patientSeries.getScorePatientSeries() + " valid doses : " + step.numberOfValidDoses(patientSeries)
              + " </p>");
    }
  }

  private static void printSelectPrioritizedPatientSeriesStandard(SelectPrioritizedPatientSeries step,
      PrintWriter out) {
    out.println(
        "<p>Select prioritized patient series provides the business rules to be applied to the scored patient series which will result in the prioritized patient series for the series group.</p>");

    out.print("<h4> " + step.getDataModel().getAntigen().getName() + " </h4>");
  }

  private static void printMultipleAntigenVaccineGroupPre(MultipleAntigenVaccineGroup step, PrintWriter out) {
    printMultipleAntigenVaccineGroupStandard(step, out);
  }

  private static void printMultipleAntigenVaccineGroupPost(MultipleAntigenVaccineGroup step, PrintWriter out) {
    printMultipleAntigenVaccineGroupStandard(step, out);
  }

  private static void printMultipleAntigenVaccineGroupStandard(MultipleAntigenVaccineGroup step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println(
        "<p>The forecasting  decisions and  rules which need to be applied to a multiple antigen  vaccine group are  listed below</p>");

    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
    out.println(
        "<h2>Selected Patient Series for " + dataModel.getVaccineGroup().getName() + "</h2>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Antigen</th>");
    out.println("    <th>Patient Series Status</th>");
    out.println("  </tr>");
    for (PatientSeries patientSeries : step.getSelectedList()) {
      out.println("  <tr>");
      out.println(
          "    <td>" + patientSeries.getTrackedAntigenSeries().getTargetDisease() + "</td>");
      out.println("    <td>" + patientSeries.getPatientSeriesStatus() + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");

    printLog(step, out);
  }

  private static void printSatisfyTargetDosePre(SatisfyTargetDose step, PrintWriter out) {
    printSatisfyTargetDoseStandard(step, out);
  }

  private static void printSatisfyTargetDosePost(SatisfyTargetDose step, PrintWriter out) {
    printSatisfyTargetDoseStandard(step, out);
  }

  private static void printSatisfyTargetDoseStandard(SatisfyTargetDose step, PrintWriter out) {
    out.println(
        "<p>Satisfy  target  dose  uses  the  results  from  the  previous  evaluation  sections  as  conditions  to  determine  if the target dose is satisfied.  </p>");

    out.println(
        "<p>The following processing model and decision table are used to determine if the target dose was satisfied</p>");
    out.println("<img src=\"Figure 6.23.PNG\"/>");
    out.println("<p>FIGURE 6 - 23 SATISFY TARGET DOSE PROCESS MODEL</p>");
    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printNoValidDosesPre(NoValidDoses step, PrintWriter out) {
    printNoValidDosesStandard(step, out);
  }

  private static void printNoValidDosesPost(NoValidDoses step, PrintWriter out) {
    printNoValidDosesStandard(step, out);
  }

  private static void printNoValidDosesStandard(NoValidDoses step, PrintWriter out) {
    out.println(
        "<p>This section  provides the decision table for determining the number of points to assign to a scorable patient series when there are no valid doses.</p>");
    printNoValidDosesTable(out);
    // printConditionAttributesTable(out);
    // printLogicTables(out);
    printBestPatientSeries(step, out);
  }

  private static void printNoValidDosesTable(PrintWriter out) {
    out.println("<table BORDER=\"1\"> ");
    out.println("  <tr> ");
    out.println(" <th> Conditions </th> ");
    out.println(" <th> If this condition is true for the scorable patient series </th> ");
    out.println(" <th>If this condition is true for two or more scorable patient series </th> ");
    out.println(" <th>If this condition is not true for the scorable patient series </th> ");
    out.println("  </tr> ");
    out.println("  <tr> ");
    out.println(" <td >A scorable patient series can start earliest. </th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> 0</td> ");
    out.println(" <td align=\"center\"> -1 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(" <td>A scorable patient series is completable.</th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
    out.println(" <td align=\"center\"> -1 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(
        " <td>A scorable patient series is a gender-specific patient series and the patient's gender matches a required gender specified on the first target dose.</th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
    out.println(" <td align=\"center\"> 0 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(" <td>A scorable patient series is a product patient series. </th> ");
    out.println(" <td align=\"center\"> -1</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
    out.println(" <td align=\"center\"> +1 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(" <td>A scorable patient series has exceeded maximum age. </th> ");
    out.println(" <td align=\"center\"> -1</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
    out.println(" <td align=\"center\"> +1 </td> ");
    out.println("  </tr> ");
    out.println("</table>");
  }

  private static void printSingleAntigenVaccineGroupPre(SingleAntigenVaccineGroup step, PrintWriter out) {
    printSingleAntigenVaccineGroupStandard(step, out);
  }

  private static void printSingleAntigenVaccineGroupPost(SingleAntigenVaccineGroup step, PrintWriter out) {
    printSingleAntigenVaccineGroupStandard(step, out);
  }

  private static void printSingleAntigenVaccineGroupStandard(SingleAntigenVaccineGroup step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println(
        "<p>The forecasting rules which need to be applied to a single antigen vaccine group are listed in the table below</p>");

    org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup vaccineGroup = dataModel.getVaccineGroup();
    out.println("<h2>" + vaccineGroup.getName() + "</h2>");
    PatientSeries p = dataModel.getBestPatientSeriesList().size() == 0 ? null
        : dataModel.getBestPatientSeriesList().get(0);
    if (dataModel.getBestPatientSeriesList() == null) {
      out.println("<p>Best Patient Series List is null!</p>");
    } else {
      out.println("<p>Best Patient Series List size = " + dataModel.getBestPatientSeriesList().size() + "</p>");
    }
    out.println("<p>Forecast List size = " + dataModel.getForecastList().size() + "</p>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Antigen</th>");
    out.println("    <th>Target Dose</th>");
    out.println("    <th>Patient Series Status</th>");
    out.println("    <th>Earliest Date</th>");
    out.println("    <th>Adjusted Recommended Date</th>");
    out.println("    <th>Adjusted Past Due Date</th>");
    out.println("    <th>Latest Date</th>");
    out.println("    <th>Unadjusted Recommended Date</th>");
    out.println("    <th>Unadjusted Past Due Date</th>");
    out.println("    <th>Forecast Reason</th>");
    out.println("  </tr>");

    for (org.openimmunizationsoftware.cdsi.core.domain.Forecast forecast : dataModel.getForecastList()) {
      out.println("  <tr>");
      out.println("    <td>" + forecast.getAntigen().getName() + "</td>");
      out.println("    <td>" + (p == null ? null : p.getForecast().getTargetDose()) + "</td>");
      out.println("    <td>" + (p == null ? null : p.getPatientSeriesStatus()) + "</td>");
      out.println("    <td>" + LogicStep.n(forecast.getEarliestDate()) + "</td>");
      out.println("    <td>" + LogicStep.n(forecast.getAdjustedRecommendedDate()) + "</td>");
      out.println("    <td>" + LogicStep.n(forecast.getAdjustedPastDueDate()) + "</td>");
      out.println("    <td>" + LogicStep.n(forecast.getLatestDate()) + "</td>");
      out.println("    <td>" + LogicStep.n(forecast.getUnadjustedRecommendedDate()) + "</td>");
      out.println("    <td>" + LogicStep.n(forecast.getUnadjustedPastDueDate()) + "</td>");
      out.println("    <td>" + forecast.getForecastReason() + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");

    java.util.List<org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast> vgfl = dataModel
        .getVaccineGroupForecastList();
    out.println("<p>Vaccine Group Forecast List size = " + vgfl.size() + "</p>");
    if (vgfl.size() > 0) {
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>Target Dose</th>");
      out.println("    <th>Patient Series Status</th>");
      out.println("  </tr>");
      for (org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast vgf : vgfl) {
        out.println("  <tr>");
        out.println("    <td>" + vgf.getAntigen().getName() + "</td>");
        out.println("    <td>" + vgf.getTargetDose() + "</td>");
        out.println("    <td>" + vgf.getPatientSeriesStatus() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    step.setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
  }

  private static void printSelectRelevantPatientSeriesPre(SelectRelevantPatientSeries step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println("   <h2>Antigen Series</h2>");
    out.println("     <p>Looking at antigen " + (dataModel.getAntigenSelectedPos() + 1)
        + " out of " + dataModel.getAntigenSelectedList().size() + " antigens selected. </p>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Include</th>");
    out.println("       <th>Series Name</th>");
    out.println("       <th>Target Disease</th>");
    out.println("       <th>Vaccine Group</th>");
    out.println("     </tr>");
    Antigen antigen = dataModel.getAntigenSelectedList().get(dataModel.getAntigenSelectedPos());
    int i = 1;
    for (org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
      if (!antigenSeries.getTargetDisease().equals(antigen)) {
        continue;
      }
      out.println("     <tr>");
      out.println("       <td><input type=\"checkbox\" name=\"" + LogicStep.PARAM_ANTIGEN_SERIES_INCLUDE + i
          + "\" value=\"true\" checked></td>");
      out.println("       <td>" + antigenSeries.getSeriesName() + "</td>");
      out.println("       <td>" + antigenSeries.getTargetDisease() + "</td>");
      out.println("       <td>" + (antigenSeries.getVaccineGroup() == null ? ""
          : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
      i++;
    }
    out.println("   </table>");
  }

  private static void printSelectRelevantPatientSeriesPost(SelectRelevantPatientSeries step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println(
        "   <p>An antigen series is one way to reach perceived immunity against a disease.  "
            + "An antigen series can be thought of as a \"path to immunity\" and is described in "
            + "relative terms.  In many cases, a single antigen may have more than one successful "
            + "path to immunity and as such may have more than one antigen series.  Antigen "
            + "series are defined through supporting data spreadsheets defined in chapter 3.</p>");

    printLogicTables(step, out);

    out.println("   <h2>Patient Series Included</h2>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Series Name</th>");
    out.println("       <th>Target Disease</th>");
    out.println("       <th>Vaccine Group</th>");
    out.println("     </tr>");
    for (PatientSeries patientSeries : dataModel.getPatientSeriesStepper().getList()) {
      org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries antigenSeries = patientSeries
          .getTrackedAntigenSeries();
      out.println("     <tr>");
      out.println("       <td>" + antigenSeries.getSeriesName() + "</td>");
      out.println("       <td>" + antigenSeries.getTargetDisease() + "</td>");
      out.println("       <td>" + (antigenSeries.getVaccineGroup() == null ? ""
          : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
  }

  private static void printPreFilterPatientSeriesPre(PreFilterPatientSeries step, PrintWriter out) {
    printPreFilterPatientSeriesStandard(step, out);
  }

  private static void printPreFilterPatientSeriesPost(PreFilterPatientSeries step, PrintWriter out) {
    printPreFilterPatientSeriesStandard(step, out);
  }

  private static void printPreFilterPatientSeriesStandard(PreFilterPatientSeries step, PrintWriter out) {
    printPatientSeriesList(step, out);
    printBestPatientSeries(step, out);
  }

  private static void printOrganizeImmunizationHistoryPre(OrganizeImmunizationHistory step, PrintWriter out) {
    out.println(
        "   <p>The third step in the process is to look at the patient's immunization history and prepare those records "
            + "for evaluation and forecasting by breaking them into their antigen parts. This allows the evaluation and "
            + "forecasting engine to be as granular and specific as possible for both evaluation and forecasting purposes. "
            + "Later in the process (section 8.6), these antigens are assembled into commonly known vaccine groups (vaccine families) "
            + "for vaccine group forecasts.</p>");
  }

  private static void printOrganizeImmunizationHistoryPost(OrganizeImmunizationHistory step, PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

    out.println(
        "   <p>The third step in the process is to look at the patient's immunization history and prepare those records "
            + "for evaluation and forecasting by breaking them into their antigen parts. This allows the evaluation and "
            + "forecasting engine to be as granular and specific as possible for both evaluation and forecasting purposes. "
            + "Later in the process (section 8.6), these antigens are assembled into commonly known vaccine groups (vaccine families) "
            + "for vaccine group forecasts.</p>");

    out.println("<h2>Table 8 - 2 Prior to Organize Immunization History Example</h2>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Product (CVX/MVX) - Description</th>");
    out.println("    <th>Date</th>");
    out.println("  </tr>");
    for (VaccineDoseAdministered vda : dataModel.getImmunizationHistory()
        .getVaccineDoseAdministeredList()) {
      out.println("  <tr>");
      out.println("    <td>" + vda.getVaccine().getTradeName() + " ("
          + vda.getVaccine().getVaccineType().getCvxCode() + "/"
          + vda.getVaccine().getManufacturer() + ") - "
          + vda.getVaccine().getVaccineType().getShortDescription() + "</td>");
      out.println("    <td>" + sdf.format(vda.getDateAdministered()) + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");

    out.println("<h2>Table 8 - 3 After Organize Immunization History Example</h2>");
    out.println("<p>*Sorted by antigen and then by date</p>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Product (CVX/MVX) - Description</th>");
    out.println("    <th>Date</th>");
    out.println("    <th>Antigen*</th>");
    out.println("  </tr>");
    for (org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord aar : dataModel
        .getAntigenAdministeredRecordList()) {
      out.println("  <tr>");
      out.println("    <td>" + aar.getTradeName() + " (" + aar.getVaccineType().getCvxCode() + "/"
          + aar.getManufacturer() + ") - " + aar.getVaccineType().getShortDescription() + "</td>");
      out.println("    <td>" + sdf.format(aar.getDateAdministered()) + "</td>");
      out.println("    <td>" + AntigenServlet.makeLink(aar.getAntigen()) + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");
  }

  private static void printInProcessPatientSeriesPre(InProcessPatientSeries step, PrintWriter out) {
    printInProcessPatientSeriesStandard(step, out);
  }

  private static void printInProcessPatientSeriesPost(InProcessPatientSeries step, PrintWriter out) {
    printInProcessPatientSeriesStandard(step, out);
  }

  private static void printInProcessPatientSeriesStandard(InProcessPatientSeries step, PrintWriter out) {
    out.println(
        "<p>In-process  patient series provides the decision table for determining the number of points to assign to an  inprocess patient series based on a specified condition.</p>");
    printInProcessPatientSeriesTable(out);
    printBestPatientSeries(step, out);
  }

  private static void printInProcessPatientSeriesTable(PrintWriter out) {
    out.println("<table BORDER=\"1\"> ");
    out.println("  <tr> ");
    out.println(" <th> Conditions </th> ");
    out.println(" <th> If this condition is true for the candidate patient series </th> ");
    out.println(" <th>If this condition is true for two or more candidate patient series </th> ");
    out.println(" <th>If this condition is not true for the candidate patient serie </th> ");
    out.println("  </tr> ");
    out.println("  <tr> ");
    out.println(
        " <td >A candidate patient series is a product patient series and has all valid doses </th> ");
    out.println(" <td align=\"center\"> +2</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
    out.println(" <td align=\"center\"> -2 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(" <td>A candidate patient series is completable.</th> ");
    out.println(" <td align=\"center\"> +3</td> ");
    out.println(" <td align=\"center\"> n/a</td> ");
    out.println(" <td align=\"center\"> -3 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(" <td>A candidate patient series has the most valid doses.</th> ");
    out.println(" <td align=\"center\"> +2</td> ");
    out.println(" <td align=\"center\"> 0</td> ");
    out.println(" <td align=\"center\"> -2 </td> ");
    out.println("  </tr> ");
    out.println("<tr> ");
    out.println(" <td>A candidate patient series is closest to completion. </th> ");
    out.println(" <td align=\"center\"> +2</td> ");
    out.println(" <td align=\"center\"> 0</td> ");
    out.println(" <td align=\"center\"> -2 </td> ");
    out.println("  </tr> ");
    out.println("  <tr> ");
    out.println(" <td>A candidate patient series can finish earliest. </th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> 0</td> ");
    out.println(" <td align=\"center\"> -1 </td> ");
    out.println("  </tr> ");
    out.println("</table>");
  }

  private static void printEndPre(org.openimmunizationsoftware.cdsi.core.logic.End step, PrintWriter out,
      jakarta.servlet.http.HttpServletRequest req)
      throws Exception {
    DataModel dataModel = step.getDataModel();
    out.println(
        "<p>End printing forecast stuff</p>");

    org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup vaccineGroup = dataModel.getVaccineGroup();
    out.println("<h2>" + vaccineGroup.getName() + "</h2>");

    if (dataModel.getBestPatientSeriesList() == null) {
      out.println("<p>Best Patient Series List is null!</p>");
    } else {
      out.println("<p>Best Patient Series List size = " + dataModel.getBestPatientSeriesList().size() + "</p>");
    }
    out.println("<p>Forecast List size = " + dataModel.getForecastList().size() + "</p>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Antigen</th>");
    out.println("    <th>Target Dose</th>");
    out.println("    <th>VGF Status</th>");
    out.println("    <th>Earliest Date</th>");
    out.println("    <th>Adjusted Recommended Date</th>");
    out.println("    <th>Adjusted Past Due Date</th>");
    out.println("    <th>Latest Date</th>");
    out.println("    <th>Unadjusted Recommended Date</th>");
    out.println("    <th>Unadjusted Past Due Date</th>");
    out.println("    <th>Forecast Reason</th>");
    out.println("  </tr>");

    for (org.openimmunizationsoftware.cdsi.core.domain.Forecast forecast : dataModel.getForecastList()) {
      for (Antigen antigen : dataModel.getAntigenSelectedList()) {
        if (forecast.getAntigen().equals(antigen)) {
          out.println("  <tr>");
          out.println("    <td>" + forecast.getAntigen().getName() + "</td>");
          out.println("    <td>" + forecast.getTargetDose() + "</td>");
          out.println("    <td>" + (forecast.getVaccineGroupForecast() == null ? "null"
              : forecast.getVaccineGroupForecast().getVaccineGroupStatus()) + "</td>");
          out.println("    <td>" + LogicStep.n(forecast.getEarliestDate()) + "</td>");
          out.println("    <td>" + LogicStep.n(forecast.getAdjustedRecommendedDate()) + "</td>");
          out.println("    <td>" + LogicStep.n(forecast.getAdjustedPastDueDate()) + "</td>");
          out.println("    <td>" + LogicStep.n(forecast.getLatestDate()) + "</td>");
          out.println("    <td>" + LogicStep.n(forecast.getUnadjustedRecommendedDate()) + "</td>");
          out.println("    <td>" + LogicStep.n(forecast.getUnadjustedPastDueDate()) + "</td>");
          out.println("    <td>" + forecast.getForecastReason() + "</td>");
          out.println("  </tr>");
        }
      }
    }
    out.println("</table>");

    List<org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast> vgfl = dataModel
        .getVaccineGroupForecastList();
    out.println("<p>Vaccine Group Forecast List size = " + vgfl.size() + "</p>");
    if (vgfl.size() > 0) {
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>Target Dose</th>");
      out.println("    <th>Patient Series Status</th>");
      out.println("  </tr>");
      for (org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast vgf : vgfl) {
        out.println("  <tr>");
        out.println("    <td>" + vgf.getAntigen().getName() + "</td>");
        out.println("    <td>" + vgf.getTargetDose() + "</td>");
        out.println("    <td>" + vgf.getPatientSeriesStatus() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }
    out.println("<pre>");
    ForecastServlet.printText(dataModel, out, null, req);
    out.println("</pre>");
    out.println("<h2>Printing Standard</h2>");
    printEndStandard(step, out);
  }

  private static void printEndPost(org.openimmunizationsoftware.cdsi.core.logic.End step, PrintWriter out) {
    printEndStandard(step, out);
  }

  private static void printEndStandard(org.openimmunizationsoftware.cdsi.core.logic.End step, PrintWriter out) {
    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
  }

  private static void printEvaluateAgePre(org.openimmunizationsoftware.cdsi.core.logic.EvaluateAge step,
      PrintWriter out) {
    printEvaluateAgeStandard(step, out);
  }

  private static void printEvaluateAgePost(org.openimmunizationsoftware.cdsi.core.logic.EvaluateAge step,
      PrintWriter out) {
    printEvaluateAgeStandard(step, out);
  }

  private static void printEvaluateAgeStandard(org.openimmunizationsoftware.cdsi.core.logic.EvaluateAge step,
      PrintWriter out) {
    out.println(
        "<p>Evaluate age validates the age at administration of a vaccine dose administered against a defined age range of a target dose. In cases where a target dose does not specify age attributes, the age at administration is considered \"valid.\"</p>");
    out.println("<img src=\"Figure 6.5.PNG\"/>");
    out.println("<p>FIGURE 6 - 5 EVALUATE AGE TIMELINE</p>");
    out.println("<img src=\"Figure 6.6.PNG\"/>");
    out.println("<p>FIGURE 6 - 6 EVALUATE AGE PROCESS MODEL</p>");

    printConditionAttributesTable(step, out);
    printLogicTables(step, out);
    SeriesDose seriesDose = step.getDataModel().getTargetDose().getTrackedSeriesDose();
    printSeriesDoseHtml(seriesDose, out);
  }

  private static String gatherNecessaryDataNullToEmpty(String value) {
    if (value == null) {
      return "";
    }
    return value;
  }

  private static void printGatherNecessaryDataPre(org.openimmunizationsoftware.cdsi.core.logic.GatherNecessaryData step,
      PrintWriter out, jakarta.servlet.http.HttpServletRequest req) throws Exception {
    out.println("<h2>Input Data</h2>");

    out.println("<p>Patient input data:</p>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Patient DOB</th>");
    out.println("    <td><input type=\"text\" name=\"" + LogicStep.PARAM_PATIENT_DOB + "\" value=\""
        + gatherNecessaryDataNullToEmpty(req.getParameter(LogicStep.PARAM_PATIENT_DOB)) + "\" size=\"10\"/></td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <th>Patient Gender</th>");
    out.println("    <td><input type=\"text\" name=\"" + LogicStep.PARAM_PATIENT_SEX + "\" value=\""
        + gatherNecessaryDataNullToEmpty(req.getParameter(LogicStep.PARAM_PATIENT_SEX)) + "\" size=\"3\"/></td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <th>Evaluation Date</th>");
    out.println("    <td><input type=\"text\" name=\"" + LogicStep.PARAM_EVAL_DATE + "\" value=\""
        + gatherNecessaryDataNullToEmpty(req.getParameter(LogicStep.PARAM_EVAL_DATE)) + "\" size=\"10\"/></td>");
    out.println("  </tr>");
    out.println("</table>");

    out.println("<p>Immunization history input data:</p>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Vaccine</th>");
    out.println("    <th>CVX</th>");
    out.println("    <th>MVX</th>");
    out.println("    <th>Date</th>");
    out.println("    <th>Condition</th>");
    out.println("  </tr>");
    int i = 1;
    while (req.getParameter(LogicStep.PARAM_VACCINE_CVX + i) != null) {
      // i needs to e in a hidden field called id
      out.println("  <tr>");
      out.println("    <th>" + i + "</th>");
      out.println("    <td><input type=\"text\" name=\"" + LogicStep.PARAM_VACCINE_CVX + i + "\" value=\""
          + gatherNecessaryDataNullToEmpty(req.getParameter(LogicStep.PARAM_VACCINE_CVX + i)) + "\" size=\"3\"/></td>");
      out.println("    <td><input type=\"text\" name=\"" + LogicStep.PARAM_VACCINE_MVX + i + "\" value=\""
          + gatherNecessaryDataNullToEmpty(req.getParameter(LogicStep.PARAM_VACCINE_MVX + i)) + "\" size=\"3\"/></td>");
      out.println("    <td><input type=\"text\" name=\"" + LogicStep.PARAM_VACCINE_DATE + i + "\" value=\""
          + gatherNecessaryDataNullToEmpty(req.getParameter(LogicStep.PARAM_VACCINE_DATE + i)) + "\" size=\"10\"/></td>");
      out.println(
          "    <td><input type=\"text\" name=\"" + LogicStep.PARAM_VACCINE_CONDITION_CODE + i + "\" value=\""
              + gatherNecessaryDataNullToEmpty(req.getParameter(LogicStep.PARAM_VACCINE_CONDITION_CODE + i))
              + "\" size=\"3\"/></td>");
      out.println("  </tr>");
      i++;
    }
    out.println("</table>");

    out.println("<p>Patient observation input data:</p>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Observation</th>");
    out.println("    <th>Code</th>");
    out.println("    <th>Date</th>");
    out.println("  </tr>");
    i = 1;
    while (req.getParameter(LogicStep.PARAM_OBSERVATION_CODE + i) != null) {
      out.println("  <tr>");
      out.println("    <th>" + i + "</th>");
      out.println("    <td><input type=\"text\" name=\"" + LogicStep.PARAM_OBSERVATION_CODE + i + "\" value=\""
          + gatherNecessaryDataNullToEmpty(req.getParameter(LogicStep.PARAM_OBSERVATION_CODE + i))
          + "\" size=\"12\"/></td>");
      out.println("    <td><input type=\"text\" name=\"" + LogicStep.PARAM_OBSERVATION_DATE + i + "\" value=\""
          + gatherNecessaryDataNullToEmpty(req.getParameter(LogicStep.PARAM_OBSERVATION_DATE + i))
          + "\" size=\"10\"/></td>");
      out.println("  </tr>");
      i++;
    }
    out.println("</table>");
  }

  private static void printGatherNecessaryDataPost(org.openimmunizationsoftware.cdsi.core.logic.GatherNecessaryData step,
      PrintWriter out) {
    DataModel dataModel = step.getDataModel();
    out.println("   <h2>Patient-Related Data</h2>");
    out.println("   <h3>Patient</h3>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Age</th>");
    out.println("       <td>" + step.formatDate(dataModel.getPatient().getDateOfBirth()) + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Gender</th>");
    out.println("       <td>" + dataModel.getPatient().getGender() + "</td>");
    out.println("     </tr>");
    out.println("   </table>");
    out.println("   <h3>Vaccine Dose Administered</h3>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Date</th>");
    out.println("       <th>Vaccine</th>");
    out.println("       <th>Manufacturer</th>");
    out.println("     </tr>");
    for (VaccineDoseAdministered vaccineDoseAdministered : dataModel.getImmunizationHistory()
        .getVaccineDoseAdministeredList()) {
      out.println("     <tr>");
      out.println(
          "       <td>" + step.formatDate(vaccineDoseAdministered.getDateAdministered()) + "</td>");
      out.println("       <td>" + vaccineDoseAdministered.getVaccine().getVaccineType() + "</td>");
      out.println("       <td>" + vaccineDoseAdministered.getVaccine().getManufacturer() + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
    out.println("   <h3>Adverse Reactions</h3>");
    out.println("   <p><em>Not implemented yet</em></p>");
    out.println("   <h3>Patient Observations</h3>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Code</th>");
    out.println("       <th>Text</th>");
    out.println("       <th>Date</th>");
    out.println("     </tr>");
    for (PatientObservation patientObservation : dataModel.getPatient().getMedicalHistory()
        .getPatientObservationList()) {
      out.println("     <tr>");
      ObservationCode observationCode = patientObservation.getObservationCode();
      out.println("       <td>" + safe(observationCode == null ? "" : observationCode.getCode()) + "</td>");
      out.println("       <td>" + safe(observationCode == null ? "" : observationCode.getText()) + "</td>");
      out.println("       <td>" + LogicStep.n(patientObservation.getObservationDate()) + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");

    out.println("   <h2>Evaluation and Forecasting Related Data</h2>");

    out.println("   <h3>CVX to Antigen Map</h3>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Cvx</th>");
    out.println("       <th>Short Description</th>");
    out.println("       <th>Antigen(s)</th>");
    out.println("     </tr>");
    for (String cvxCode : dataModel.getCvxMap().keySet()) {
      VaccineType cvx = dataModel.getCvx(cvxCode);
      out.println("     <tr>");
      out.println("       <td>" + safe(cvx.getCvxCode()) + "</td>");
      out.println("       <td>" + safe(cvx.getShortDescription()) + "</td>");
      out.print("       <td>");
      boolean first = true;
      for (Antigen antigen : cvx.getAntigenList()) {
        if (!first) {
          out.print(", ");
        }
        first = false;
        out.print(AntigenServlet.makeLink(antigen));
      }
      out.println("</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
    out.println("   <h3>Live Virus Conflicts</h3>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Previous Vaccine</th>");
    out.println("       <th>Current Vaccine</th>");
    out.println("       <th>Conflict Begin</th>");
    out.println("       <th>Minimum Conflict End</th>");
    out.println("       <th>Conflict End</th>");
    out.println("     </tr>");
    for (LiveVirusConflict liveVirusConflict : dataModel.getLiveVirusConflictList()) {
      out.println("     <tr>");
      out.println("       <td>" + safe(liveVirusConflict.getPreviousVaccineType()) + "</td>");
      out.println("       <td>" + safe(liveVirusConflict.getCurrentVaccineType()) + "</td>");
      out.println("       <td>" + safe(liveVirusConflict.getConflictBeginInterval()) + "</td>");
      out.println(
          "       <td>" + safe(liveVirusConflict.getMinimalConflictEndInterval()) + "</td>");
      out.println("       <td>" + safe(liveVirusConflict.getConflictEndInterval()) + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
  }
}
