package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
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

  protected static final String SCHEDULE_NAME_DEFAULT = "default";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      StepServlet.registerRequest(req);
      PrintWriter out = new PrintWriter(resp.getOutputStream());
      DataModel dataModel = readRequest(req);
      String logStep = req.getParameter("logStep");
      LogicStepType logStepType = null;
      if (logStep != null) {
        logStepType = LogicStepType.valueOf(logStep);
      }
      process(dataModel, out, logStepType);
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
        printText(resp, dataModel, out);
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServletException(e);
    }
  }

  private static void printText(HttpServletResponse resp, DataModel dataModel, PrintWriter out) {
    resp.setContentType("text/plain");
    printText(dataModel, out);
  }

  public static void printText(DataModel dataModel, PrintWriter out) {
    out.println("Step Into Clinical Decision Support for Immunizations - Demonstration //System");
    out.println();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

    List<Forecast> fl = dataModel.getForecastList();
    List<VaccineGroupForecast> vgfl = dataModel.getVaccineGroupForecastList();

    List<VaccineGroupForecast> vgfNow = new ArrayList<VaccineGroupForecast>();
    List<VaccineGroupForecast> vgfLater = new ArrayList<VaccineGroupForecast>();
    List<VaccineGroupForecast> vgfDone = new ArrayList<VaccineGroupForecast>();

    Date today = new Date();
    try {
      today = sdf.parse(sdf.format(today));
    } catch (ParseException pe) {
      pe.printStackTrace();
    }
    {
      for (VaccineGroupForecast vgf : vgfl) {
        if (!vgf.getVaccineGroupStatus().equals(VaccineGroupStatus.COMPLETE)) {
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
    }

    printList(dataModel, out, sdf, today, vgfNow, "VACCINATIONS RECOMMENDED");
    printList(dataModel, out, sdf, today, vgfLater, "VACCINATIONS RECOMMENDED AFTER");
    printList(dataModel, out, sdf, today, vgfDone, "VACCINATIONS COMPLETE OR NOT RECOMMENDED");

    // printListRaw(dataModel, out, sdf, today, dataModel.getForecastList(), "RAW
    // LIST FOR DEBUG");

    if (dataModel.getAntigenAdministeredRecordList().size() > 0) {
      out.println("IMMUNIZATION EVALUATION");
      int count = 0;

      for (PatientSeries patientSeries : dataModel.getBestPatientSeriesList()) {
        for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
          if (targetDose.getEvaluationList() != null) {
            for (Evaluation evaluation : targetDose.getEvaluationList()) {
              VaccineDoseAdministered vda = evaluation.getVaccineDoseAdministered();
              if (evaluation.getEvaluationStatus() != null) {
                count++;
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

      for (VaccineDoseAdministered vda : dataModel.getImmunizationHistory()
          .getVaccineDoseAdministeredList()) {
        count++;
        for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {
          if (aar.getVaccineType().equals(vda.getVaccine().getVaccineType())
              && aar.getDateAdministered().equals(vda.getDateAdministered())) {
            out.print("Vaccination #" + count + ": " + aar.getVaccineType().getShortDescription()
                + " given " + sdf.format(aar.getDateAdministered()));
            // is a valid Hib dose 1. Dose 1 valid at 6 weeks of age,
            // 10/14/2012.
            out.println();
          }
        }
      }
      out.println();
    }

    out.println("Forecast generated " + sdf.format(new Date()) + " using software version "
        + SoftwareVersion.VERSION + ".");

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
          if (vgf.getPatientSeriesStatus() == PatientSeriesStatus.NOT_COMPLETE) {
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

  private void printListRaw(DataModel dataModel, PrintWriter out, SimpleDateFormat sdf, Date today,
      List<Forecast> forecastList, String title) {
    if (forecastList.size() > 0) {
      out.println(title + " " + sdf.format(dataModel.getAssessmentDate()));
      // for (VaccineGroupForecast vaccineGroupForecast : vaccineGroupForecastList)
      // {
      //
      // }
      for (Forecast forecast : forecastList) {
        if (forecast.getAntigen() != null) {
          String name = forecast.getAntigen().getName();
          // down to here
          out.print("Forecasting " + name + " status ");
          if (forecast.getForecastReason().equals("")) {
            if (forecast.getAdjustedRecommendedDate().after(today)) {
              out.print("due later ");
            } else {
              out.print("due ");
            }
            out.print("dose ");
            if (forecast.getTargetDose() == null) {
              out.print("? ");
            } else {
              out.print(forecast.getTargetDose().getTrackedSeriesDose().getDoseNumber() + " ");
            }
            if (forecast.getAdjustedRecommendedDate() != null) {
              out.print("due ");
              out.print(sdf.format(forecast.getAdjustedRecommendedDate()));
              out.print(" ");
              if (forecast.getEarliestDate() != null) {
                out.print("valid ");
                out.print(sdf.format(forecast.getEarliestDate()));
                out.print(" ");
                if (forecast.getAdjustedPastDueDate() != null) {
                  out.print("overdue ");
                  out.print(sdf.format(forecast.getAdjustedPastDueDate()));
                  out.print(" ");
                  if (forecast.getLatestDate() != null) {
                    out.print("finished ");
                    out.print(sdf.format(forecast.getLatestDate()));
                  }
                }
              }
            }
          } else {
            out.print(forecast.getForecastReason());
          }
          out.println();
        }
      }
      out.println();
    }
  }

  private void process(DataModel dataModel, PrintWriter out, LogicStepType logLogicStepType) throws Exception {
    int count = 0;
    while (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END) {
      LogicStep currentStep = dataModel.getLogicStep();
      LogicStep nextLogicStep = dataModel.getLogicStep().process();
      dataModel.setNextLogicStep(nextLogicStep);
      if (logLogicStepType != null) {
        if (logLogicStepType == currentStep.getLogicStepType()) {
          out.println("========================================================================================");
          out.println(
              "Step " + count + ": " + currentStep.getLogicStepType().getName() + " -> "
                  + nextLogicStep.getLogicStepType().getName());
          currentStep.printPost(out);
          currentStep.printLog(out);
          out.println("========================================================================================");
        }
      }

      count++;
      if (count > 100000) {
        System.err.println(
            "Appear to be caught in a loop at this step: " + dataModel.getLogicStep().getTitle());
        // too many steps!
        if (count > 100100) {
          throw new RuntimeException(
              "Logic steps seem to be caught in a loop, unable to get results");
        }
      }
    }
  }

  protected DataModel readRequest(HttpServletRequest req) throws Exception {
    DataModel dataModel = DataModelLoader.createDataModel();
    dataModel.setRequest(req);
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
