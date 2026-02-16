package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepFactory;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;

public class ForecastServlet extends HttpServlet {

  public static final String RESULT_FORMAT_TEXT = "text";
  public static final String RESULT_FORMAT_HTML = "html";
  public static final String RESULT_FORMAT_COMPACT = "compact";

  public static final String PARAM_RESULT_FORMAT = "resultFormat";

  // Test case parameters for displaying test information
  public static final String PARAM_TEST_CASE = "_test_case";
  public static final String PARAM_TEST_TITLE = "_test_title";
  public static final String PARAM_TEST_EXP_STATUS = "_test_exp_status";
  public static final String PARAM_TEST_EXP_EARLIEST = "_test_exp_earliest";
  public static final String PARAM_TEST_EXP_RECOMMENDED = "_test_exp_recommended";
  public static final String PARAM_TEST_ACT_STATUS = "_test_act_status";
  public static final String PARAM_TEST_ACT_EARLIEST = "_test_act_earliest";
  public static final String PARAM_TEST_ACT_RECOMMENDED = "_test_act_recommended";
  public static final String PARAM_TEST_ACT_STATUS_PASS = "_test_act_status_pass";
  public static final String PARAM_TEST_ACT_EARLIEST_PASS = "_test_act_earliest_pass";
  public static final String PARAM_TEST_ACT_RECOMMENDED_PASS = "_test_act_recommended_pass";

  protected static final String SCHEDULE_NAME_DEFAULT = "default";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      StepServlet.registerRequest(req);
      PrintWriter out = new PrintWriter(resp.getOutputStream());
      DataModel dataModel = readRequest(req);

      // Check if logging is requested
      StringBuilder logBuffer = req.getParameter("log") != null ? new StringBuilder() : null;

      // Build per-step log level map
      Map<LogicStepType, LogicStep.Level> stepLevelMap = null;
      if (logBuffer != null) {
        stepLevelMap = buildStepLevelMap(req);
      }

      process(dataModel, out, logBuffer, stepLevelMap);
      String resultFormat = req.getParameter(PARAM_RESULT_FORMAT);
      if (resultFormat == null) {
        resultFormat = RESULT_FORMAT_TEXT;
      }
      if (resultFormat.equals(RESULT_FORMAT_HTML)) {
        resp.setContentType("text/plain");
        out.println("not implemented yet");
      } else if (resultFormat.equals(RESULT_FORMAT_COMPACT)) {
        resp.setContentType("text/plain");
        out.println("not implemented yet");
      } else {
        printText(resp, dataModel, out, logBuffer, req);
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServletException(e);
    }
  }

  private static void printText(HttpServletResponse resp, DataModel dataModel, PrintWriter out,
      StringBuilder logBuffer, HttpServletRequest req) {
    resp.setContentType("text/plain");
    printText(dataModel, out, logBuffer, req);
  }

  /**
   * Generate forecast text output as a String.
   * 
   * @param dataModel The data model
   * @param logBuffer The log buffer (may be null)
   * @param req       The HTTP request
   * @return The forecast text output
   */
  protected static String generateTextOutput(DataModel dataModel, StringBuilder logBuffer,
      HttpServletRequest req) {
    java.io.StringWriter sw = new java.io.StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    printText(dataModel, pw, logBuffer, req);
    pw.flush();
    return sw.toString();
  }

  public static void printText(DataModel dataModel, PrintWriter out, StringBuilder logBuffer,
      HttpServletRequest req) {
    out.println("Step Into Clinical Decision Support for Immunizations - Demonstration //System");
    out.println();

    // Print test case information if provided
    if (logBuffer != null && req != null) {
      String testCase = req.getParameter(PARAM_TEST_CASE);
      if (testCase != null && !testCase.isEmpty()) {
        out.println("========================================");
        out.println("TEST CASE INFORMATION");
        out.println("========================================");
        out.println();

        out.println(padLabel("Test Case:", 20) + testCase);

        String testTitle = req.getParameter(PARAM_TEST_TITLE);
        if (testTitle != null && !testTitle.isEmpty()) {
          out.println(padLabel("Title:", 20) + testTitle);
        }

        out.println();

        // Retrieve all parameters
        String expStatus = req.getParameter(PARAM_TEST_EXP_STATUS);
        String actStatus = req.getParameter(PARAM_TEST_ACT_STATUS);
        String statusPass = req.getParameter(PARAM_TEST_ACT_STATUS_PASS);

        String expEarliest = req.getParameter(PARAM_TEST_EXP_EARLIEST);
        String actEarliest = req.getParameter(PARAM_TEST_ACT_EARLIEST);
        String earliestPass = req.getParameter(PARAM_TEST_ACT_EARLIEST_PASS);

        String expRecommended = req.getParameter(PARAM_TEST_EXP_RECOMMENDED);
        String actRecommended = req.getParameter(PARAM_TEST_ACT_RECOMMENDED);
        String recommendedPass = req.getParameter(PARAM_TEST_ACT_RECOMMENDED_PASS);

        // Print STATUS section
        if ((expStatus != null && !expStatus.isEmpty()) ||
            (actStatus != null && !actStatus.isEmpty()) ||
            (statusPass != null && !statusPass.isEmpty())) {
          out.println("STATUS:");
          if (expStatus != null && !expStatus.isEmpty()) {
            out.println(padLabel("  Expected:", 15) + expStatus);
          }
          if (actStatus != null && !actStatus.isEmpty()) {
            out.println(padLabel("  Actual:", 15) + actStatus);
          }
          if (statusPass != null && !statusPass.isEmpty()) {
            out.println(padLabel("  Pass:", 15) + ("true".equalsIgnoreCase(statusPass) ? "PASS" : "FAIL"));
          }
          out.println();
        }

        // Print EARLIEST DATE section
        if ((expEarliest != null && !expEarliest.isEmpty()) ||
            (actEarliest != null && !actEarliest.isEmpty()) ||
            (earliestPass != null && !earliestPass.isEmpty())) {
          out.println("EARLIEST DATE:");
          if (expEarliest != null && !expEarliest.isEmpty()) {
            out.println(padLabel("  Expected:", 15) + expEarliest);
          }
          if (actEarliest != null && !actEarliest.isEmpty()) {
            out.println(padLabel("  Actual:", 15) + actEarliest);
          }
          if (earliestPass != null && !earliestPass.isEmpty()) {
            out.println(padLabel("  Pass:", 15) + ("true".equalsIgnoreCase(earliestPass) ? "PASS" : "FAIL"));
          }
          out.println();
        }

        // Print RECOMMENDED DATE section
        if ((expRecommended != null && !expRecommended.isEmpty()) ||
            (actRecommended != null && !actRecommended.isEmpty()) ||
            (recommendedPass != null && !recommendedPass.isEmpty())) {
          out.println("RECOMMENDED DATE:");
          if (expRecommended != null && !expRecommended.isEmpty()) {
            out.println(padLabel("  Expected:", 15) + expRecommended);
          }
          if (actRecommended != null && !actRecommended.isEmpty()) {
            out.println(padLabel("  Actual:", 15) + actRecommended);
          }
          if (recommendedPass != null && !recommendedPass.isEmpty()) {
            out.println(padLabel("  Pass:", 15) + ("true".equalsIgnoreCase(recommendedPass) ? "PASS" : "FAIL"));
          }
          out.println();
        }

        out.println("========================================");
        out.println();
      }
    }

    // Print event dates
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    if (dataModel.getPatient() != null && dataModel.getPatient().getDateOfBirth() != null) {
      out.println("EVENT DATES:");
      out.println(padLabel("  Birth:", 20) + sdf.format(dataModel.getPatient().getDateOfBirth()));

      if (dataModel.getPatient().getReceivesList() != null && !dataModel.getPatient().getReceivesList().isEmpty()) {
        for (VaccineDoseAdministered vda : dataModel.getPatient().getReceivesList()) {
          String cvxCode = "";
          if (vda.getVaccine() != null && vda.getVaccine().getVaccineType() != null) {
            cvxCode = vda.getVaccine().getVaccineType().getCvxCode();
          }
          String vaccineLabel = "  Vaccine " + cvxCode + ":";
          out.println(padLabel(vaccineLabel, 20) + sdf.format(vda.getDateAdministered()));
        }
      }

      out.println();
    }

    List<VaccineGroupForecast> vgfl = dataModel.getVaccineGroupForecastList();

    List<VaccineGroupForecast> vgfNow = new ArrayList<VaccineGroupForecast>();
    List<VaccineGroupForecast> vgfLater = new ArrayList<VaccineGroupForecast>();
    List<VaccineGroupForecast> vgfDone = new ArrayList<VaccineGroupForecast>();

    Date today = dataModel.getAssessmentDate();
    try {
      today = sdf.parse(sdf.format(today));
    } catch (ParseException pe) {
      pe.printStackTrace();
    }

    for (VaccineGroupForecast vgf : vgfl) {
      if (vgf.getVaccineGroupStatus() != null
          && vgf.getVaccineGroupStatus().equals(VaccineGroupStatus.NOT_COMPLETE)) {
        if (vgf.getAdjustedRecommendedDate() != null
            && vgf.getAdjustedRecommendedDate().after(today)) {
          vgfLater.add(vgf);
        } else {
          vgfNow.add(vgf);
        }
      } else {
        vgfDone.add(vgf);
      }
    }

    if (logBuffer != null) {
      // Print detailed vaccination forecasts
      out.println("========================================");
      out.println("VACCINATION FORECASTS");
      out.println("========================================");
      out.println();

      List<VaccineGroupForecast> allVaccinations = new ArrayList<VaccineGroupForecast>();
      allVaccinations.addAll(vgfNow);
      allVaccinations.addAll(vgfLater);
      allVaccinations.addAll(vgfDone);

      printDetailedVaccinations(dataModel, out, sdf, today, allVaccinations);
      out.println();
    }

    printList(dataModel, out, sdf, today, vgfNow, "VACCINATIONS RECOMMENDED");
    printList(dataModel, out, sdf, today, vgfLater, "VACCINATIONS RECOMMENDED AFTER");
    printList(dataModel, out, sdf, today, vgfDone, "VACCINATIONS COMPLETE OR NOT RECOMMENDED");

    // printListRaw(dataModel, out, sdf, today, dataModel.getForecastList(), "RAW
    // LIST FOR DEBUG");

    if (dataModel.getAntigenAdministeredRecordList().size() > 0) {
      out.println("IMMUNIZATION EVALUATION");

      for (PatientSeries patientSeries : dataModel.getBestPatientSeriesList()) {
        if (patientSeries.getTargetDoseList() != null) {
          for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
            if (targetDose.getEvaluationList() != null) {
              for (Evaluation evaluation : targetDose.getEvaluationList()) {
                VaccineDoseAdministered vda = evaluation.getVaccineDoseAdministered();
                if (evaluation.getEvaluationStatus() != null) {
                  out.print("Vaccination #" + vda.getId() + ": ");
                  out.print(vda.getVaccine().getVaccineType().getShortDescription());
                  out.print(" given ");
                  out.print(sdf.format(vda.getDateAdministered()));
                  out.print(" is a ");
                  if (evaluation.getEvaluationStatus() != null
                      && evaluation.getEvaluationStatus() == EvaluationStatus.VALID) {
                    out.print("valid ");
                  } else {
                    out.print("not valid ");
                  }
                  out.print(targetDose.getTrackedSeriesDose().getAntigenSeries().getTargetDisease());
                  out.print(" dose ");
                  out.print(targetDose.getTrackedSeriesDose().getDoseNumber());
                  out.println();
                }
              }
            }
          }
        }
      }
      out.println();
    }

    out.println("Forecast generated " + sdf.format(new Date()) + " using software version "
        + SoftwareVersion.VERSION + ".");

    // Print detailed log if requested
    if (logBuffer != null && logBuffer.length() > 0) {
      out.println();
      out.println();
      out.println("========================================");
      out.println("DETAILED PROCESSING LOG");
      out.println("========================================");
      out.print(logBuffer.toString());
    }

  }

  /**
   * Print detailed vaccination forecast information.
   * 
   * @param dataModel                The data model
   * @param out                      The print writer
   * @param sdf                      The date format
   * @param today                    The current date
   * @param vaccineGroupForecastList The list of vaccinations to print
   */
  private static void printDetailedVaccinations(DataModel dataModel, PrintWriter out, SimpleDateFormat sdf,
      Date today, List<VaccineGroupForecast> vaccineGroupForecastList) {
    for (VaccineGroupForecast vgf : vaccineGroupForecastList) {
      if (vgf.getAntigen() != null) {
        String name = vgf.getAntigen() == null ? "No Antigen" : vgf.getAntigen().getName();
        if (name.equals("Tetanus")) {
          Calendar c = Calendar.getInstance();
          c.setTime(dataModel.getPatient().getDateOfBirth());
          c.add(Calendar.YEAR, 7);
          if (today.before(c.getTime())) {
            name = "DTaP";
          } else {
            name = "Tdap";
          }
        } else if (name.equals("Mumps") || name.equals("Measles") || name.equals("Rubella")) {
          name = "MMR";
        }

        out.println("Vaccination Recommended:");
        out.println("  + Forecasting: " + name);

        // Get series name and dose
        if (vgf.getTargetDose() != null && vgf.getTargetDose().getTrackedSeriesDose() != null) {
          SeriesDose trackedDose = vgf.getTargetDose().getTrackedSeriesDose();
          if (trackedDose.getAntigenSeries() != null) {
            out.println("  + Series Name: " + trackedDose.getAntigenSeries().getSeriesName());
          }
          out.println("  + Dose: " + trackedDose.getDoseNumber());
        }

        // Print status
        if (vgf.getPatientSeriesStatus() != null
            && vgf.getPatientSeriesStatus() == PatientSeriesStatus.NOT_COMPLETE) {
          if (vgf.getAdjustedRecommendedDate() != null && vgf.getAdjustedRecommendedDate().after(today)) {
            out.println("  + Status: due later");
          } else {
            out.println("  + Status: due");
          }
        } else {
          out.println("  + Status: " + vgf.getPatientSeriesStatus());
        }

        // Print dates
        if (vgf.getAdjustedRecommendedDate() != null) {
          out.println("  + Recommended Date: " + sdf.format(vgf.getAdjustedRecommendedDate()));
        }
        if (vgf.getEarliestDate() != null) {
          out.println("  + Earliest Date: " + sdf.format(vgf.getEarliestDate()));
        }
        if (vgf.getAdjustedPastDueDate() != null) {
          out.println("  + Past Due Date: " + sdf.format(vgf.getAdjustedPastDueDate()));
        }
        if (vgf.getLatestDate() != null) {
          out.println("  + Latest Date: " + sdf.format(vgf.getLatestDate()));
        } else {
          out.println("  + Latest Date: 01/01/2200");
        }

        out.println();
      }
    }
  }

  // Measles Mumps Rubella
  // combined into MMR

  private static void printList(DataModel dataModel, PrintWriter out, SimpleDateFormat sdf, Date today,
      List<VaccineGroupForecast> vaccineGroupForecastList, String title) {
    if (vaccineGroupForecastList.size() > 0) {
      out.println(title + " " + sdf.format(dataModel.getAssessmentDate()));
      for (VaccineGroupForecast vgf : vaccineGroupForecastList) {
        if (vgf.getAntigen() != null) {
          String name = vgf.getAntigen() == null ? "No Antigen" : vgf.getAntigen().getName();
          if (name.equals("Tetanus")) {
            Calendar c = Calendar.getInstance();
            c.setTime(dataModel.getPatient().getDateOfBirth());
            c.add(Calendar.YEAR, 7);
            if (today.before(c.getTime())) {
              name = "DTaP";
            } else {
              name = "Tdap";
            }
          } else if (name.equals("Mumps") || name.equals("Measles") || name.equals("Rubella")) {
            name = "MMR";
          }
          out.print("Forecasting " + name + " status ");
          if (vgf.getPatientSeriesStatus() != null
              && vgf.getPatientSeriesStatus() == PatientSeriesStatus.NOT_COMPLETE) {
            if (vgf.getAdjustedRecommendedDate() != null && vgf.getAdjustedRecommendedDate().after(today)) {
              out.print("due later ");
            } else {
              out.print("due ");
            }
            out.print("dose ");
            if (vgf.getTargetDose() == null) {
              out.print("? ");
            } else {
              out.print(vgf.getTargetDose().getTrackedSeriesDose().getDoseNumber() + " ");
            }
            if (vgf.getAdjustedRecommendedDate() != null) {
              out.print("due ");
              out.print(sdf.format(vgf.getAdjustedRecommendedDate()));
              out.print(" ");
              if (vgf.getEarliestDate() != null) {
                out.print("valid ");
                out.print(sdf.format(vgf.getEarliestDate()));
                out.print(" ");
                if (vgf.getAdjustedPastDueDate() != null) {
                  out.print("overdue ");
                  out.print(sdf.format(vgf.getAdjustedPastDueDate()));
                  out.print(" ");
                  if (vgf.getLatestDate() != null) {
                    out.print("finished ");
                    out.print(sdf.format(vgf.getLatestDate()));
                  } else {
                    out.print("finished 01/01/2200");
                  }
                }
              }
            }
          } else {
            out.print(vgf.getPatientSeriesStatus());
          }
          out.println();
        }
      }
      out.println();
    }
  }

  protected void process(DataModel dataModel, PrintWriter out,
      StringBuilder logBuffer, Map<LogicStepType, LogicStep.Level> stepLevelMap) throws Exception {
    int count = 0;
    while (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END) {
      LogicStep currentStep = dataModel.getLogicStep();
      LogicStep nextLogicStep = dataModel.getLogicStep().process();
      dataModel.setNextLogicStep(nextLogicStep);

      // Collect and filter logs if requested
      if (logBuffer != null && stepLevelMap != null) {
        LogicStepType currentStepType = currentStep.getLogicStepType();
        LogicStep.Level threshold = stepLevelMap.getOrDefault(currentStepType, LogicStep.Level.CONTROL);

        // Print step header
        logBuffer.append("\n");
        logBuffer.append("Step ").append(count).append(": ")
            .append(currentStep.getLogicStepType().getName())
            .append(" --> ")
            .append(nextLogicStep.getLogicStepType().getName())
            .append("\n");

        // Print filtered log events
        if (currentStep.getLogEventList() != null) {
          for (LogicStep.LogEvent event : currentStep.getLogEventList()) {
            if (meetsThreshold(event.getLevel(), threshold)) {
              if (event.isAlert()) {
                logBuffer.append("  + ALERT: ").append(event.getMessage()).append("\n");
              } else {
                logBuffer.append("  + ").append(event.getMessage()).append("\n");
              }
            }
          }
        }
      }

      count++;
      if (count > 100000) {
        System.err.println(
            "Appear to be caught in a loop at this step: " + dataModel.getLogicStep().getTitle());
        // too many steps!
        if (count > 100100) {
          throw new RuntimeException(
              "Logic steps seem to be caught in a loop at " + dataModel.getLogicStep().getTitle()
                  + ", unable to get results");
        }
      }
    }
  }

  /**
   * Build a map of per-step log levels from request parameters.
   * 
   * @param req The HTTP request
   * @return Map of LogicStepType to log Level
   */
  protected Map<LogicStepType, LogicStep.Level> buildStepLevelMap(HttpServletRequest req) {
    Map<LogicStepType, LogicStep.Level> stepLevelMap = new EnumMap<>(LogicStepType.class);

    // Parse global default level
    String globalLevelParam = req.getParameter("logLevel");
    LogicStep.Level defaultLevel = LogicStep.Level.CONTROL;
    if (globalLevelParam != null) {
      try {
        defaultLevel = LogicStep.Level.valueOf(globalLevelParam.toUpperCase());
      } catch (IllegalArgumentException e) {
        // Invalid level, use default
        defaultLevel = LogicStep.Level.CONTROL;
      }
    }

    // Initialize all steps with default level
    for (LogicStepType stepType : LogicStep.STEPS) {
      stepLevelMap.put(stepType, defaultLevel);
    }

    // Apply per-step overrides
    for (LogicStepType stepType : LogicStep.STEPS) {
      String paramName = "log" + stepType.name();
      String levelParam = req.getParameter(paramName);
      if (levelParam != null) {
        try {
          LogicStep.Level level = LogicStep.Level.valueOf(levelParam.toUpperCase());
          stepLevelMap.put(stepType, level);
        } catch (IllegalArgumentException e) {
          // Invalid level, keep default
        }
      }
    }

    return stepLevelMap;
  }

  /**
   * Check if an event level meets the threshold for printing.
   * Levels are ordered: CONTROL < STATE < REASONING < TRACE < DUMP
   * An event meets threshold if its level is at or below (less verbose than or
   * equal to) the threshold.
   * 
   * @param eventLevel The level of the log event
   * @param threshold  The threshold level for filtering
   * @return true if the event should be printed
   */
  private boolean meetsThreshold(LogicStep.Level eventLevel, LogicStep.Level threshold) {
    // Lower ordinal = less verbose (CONTROL=0, DUMP=4)
    // Print if event level is at or below threshold
    return eventLevel.ordinal() <= threshold.ordinal();
  }

  /**
   * Helper method to pad a label string to a specified width for alignment.
   * 
   * @param label The label text to pad
   * @param width The total width to pad to
   * @return The padded string
   */
  private static String padLabel(String label, int width) {
    return String.format("%-" + width + "s", label);
  }

  protected DataModel readRequest(HttpServletRequest req) throws Exception {
    DataModel dataModel = DataModelLoader.createDataModel();
    dataModel.setRequest(req);

    // Parse antigenInclude parameters (antigenInclude1, antigenInclude2, etc.)
    List<String> antigenLabelFilterList = new ArrayList<String>();
    int i = 1;
    while (true) {
      String antigenLabel = req.getParameter("antigenInclude" + i);
      if (antigenLabel == null) {
        break; // No more antigen includes, stop
      }
      antigenLabelFilterList.add(antigenLabel);
      i++;
    }

    // Set the filter list if any antigens were specified
    if (!antigenLabelFilterList.isEmpty()) {
      dataModel.setAntigenLabelFilterList(antigenLabelFilterList);
    }

    dataModel.setNextLogicStep(
        LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel));
    return dataModel;
  }

  public static String n(Date date) {
    if (date == null) {
      return "<center>-</center>";
    } else {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      return sdf.format(date);
    }
  }

}
