package org.openimmunizationsoftware.cdsi.servlet;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.fromString;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;

public class StepServlet extends ForecastServlet {

  static List<StepExample> stepExamples = null;
  static int stepExampleStartCount = 0;
  static {
    stepExamples = new ArrayList<StepExample>();
    {
      StepExample stepExample = new StepExample(
          "Original Hib",
          "evalDate=20160630&scheduleName=default&resultFormat=text&patientDob=20150630&patientSex=F&vaccineDate1=20150826&vaccineCvx1=47&vaccineMvx1=&vaccineDate2=20151027&vaccineCvx2=47&vaccineMvx2=&vaccineDate3=20151229&vaccineCvx3=47&vaccineMvx3=");
      stepExamples.add(stepExample);
    }
    {
      StepExample stepExample = new StepExample(
          "Original Complete Record",
          "evalDate=20140515&scheduleName=default&resultFormat=text&patientDob=20051215&patientSex=M&vaccineDate1=20060213&vaccineCvx1=10&vaccineMvx1=&vaccineDate2=20060214&vaccineCvx2=100&vaccineMvx2=&vaccineDate3=20060420&vaccineCvx3=10&vaccineMvx3=&vaccineDate4=20060420&vaccineCvx4=20&vaccineMvx4=&vaccineDate5=20060420&vaccineCvx5=17&vaccineMvx5=&vaccineDate6=20060616&vaccineCvx6=17&vaccineMvx6=&vaccineDate7=20060616&vaccineCvx7=10&vaccineMvx7=&vaccineDate8=20060616&vaccineCvx8=08&vaccineMvx8=&vaccineDate9=20060616&vaccineCvx9=20&vaccineMvx9=&vaccineDate10=20060616&vaccineCvx10=100&vaccineMvx10=&vaccineDate11=20060929&vaccineCvx11=08&vaccineMvx11=&vaccineDate12=20060929&vaccineCvx12=100&vaccineMvx12=&vaccineDate13=20061213&vaccineCvx13=20&vaccineMvx13=&vaccineDate14=20061215&vaccineCvx14=08&vaccineMvx14=&vaccineDate15=20061215&vaccineCvx15=85&vaccineMvx15=&vaccineDate16=20061215&vaccineCvx16=03&vaccineMvx16=&vaccineDate17=20061215&vaccineCvx17=21&vaccineMvx17=&vaccineDate18=20071105&vaccineCvx18=17&vaccineMvx18=&vaccineDate19=20071105&vaccineCvx19=20&vaccineMvx19=&vaccineDate20=20071105&vaccineCvx20=10&vaccineMvx20=&vaccineDate21=20080110&vaccineCvx21=85&vaccineMvx21=&vaccineDate22=20080110&vaccineCvx22=100&vaccineMvx22=&vaccineDate23=20140515&vaccineCvx23=21&vaccineMvx23=&vaccineDate24=20140515&vaccineCvx24=139&vaccineMvx24=&vaccineDate25=20140515&vaccineCvx25=10&vaccineMvx25=&vaccineDate26=20140515&vaccineCvx26=03&vaccineMvx26=");
      stepExamples.add(stepExample);
    }
    stepExampleStartCount = stepExamples.size();
  }

  private static final int MAX_STEP_EXAMPLES = 100;

  public static void registerRequest(HttpServletRequest req) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String label = req.getParameter("Received forecast " + sdf.format(new java.util.Date()));
    String requestString = req.getQueryString();
    StepExample stepExample = new StepExample(label, requestString);
    synchronized (stepExamples) {
      stepExamples.add(stepExampleStartCount, stepExample);
      if (stepExamples.size() > stepExampleStartCount + MAX_STEP_EXAMPLES) {
        stepExamples.remove(stepExampleStartCount + MAX_STEP_EXAMPLES);
      }
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    DataModel dataModel = null;
    String action = req.getParameter("action");
    if (action != null && action.equals("next")) {
      dataModel = (DataModel) session.getAttribute("dataModel");
    }

    Exception exception = null;

    if (dataModel == null) {
      try {
        dataModel = readRequest(req);
        session.setAttribute("dataModel", dataModel);
      } catch (Exception e) {
        e.printStackTrace();
        throw new ServletException(e);
      }
    } else {
      dataModel.setRequest(req);
      try {
        String submit = req.getParameter("submit");
        if (submit != null && submit.equals("Jump")) {
          String jumpTo = req.getParameter("jumpTo");
          jump(dataModel, jumpTo);
        } else if (submit != null && submit.equals("Jump4.4")) {
          String jumpTo = "Evaluate and Forecast all Patient Series";
          jump(dataModel, jumpTo);
        } else if (submit != null && submit.equals("Jump4.5")) {
          String jumpTo = "Select Best Patient Series";
          jump(dataModel, jumpTo);
        } else if (submit != null && submit.equals("Jump4.6")) {
          String jumpTo = "Identify and Evaluate Vaccine Group";
          jump(dataModel, jumpTo);
        } else if (submit != null && submit.equals("End")) {
          String jumpTo = "End";
          jump(dataModel, jumpTo);
        }
        if (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END) {
          dataModel.setNextLogicStep(dataModel.getLogicStep().process());
        }
      } catch (Exception e) {
        e.printStackTrace();
        exception = e;
        dataModel.setLogicStepPrevious(dataModel.getLogicStep());
      }
    }

    resp.setContentType("text/html");

    PrintWriter out = new PrintWriter(resp.getOutputStream());
    out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>CDSi - " + dataModel.getLogicStep().getTitle() + "</title>");
    out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"indexStep.css\">");
    out.println("  </head>");
    out.println("  <body>");

    out.println("      <form action=\"step\" method=\"POST\" id=\"stepForm\">");
    out.println("    <div class=\"cell\">");
    if (exception != null) {
      out.println("<pre>");
      exception.printStackTrace(out);
      out.println("</pre>");
    } else {
      out.println(
          "  <a href=\"step\"><img src=\"Logo Large.png\" height=\"120\" align=\"left\"/></a>");
      out.println("  <a href=\"dataModelView\" target=\"dataModelView\">View Data Model</a><br/>");
      out.println("  <a href=\"fits\">FITS Test Cases</a>");
      out.println("<br clear=\"all\"/>");
      if (dataModel.getLogicStepPrevious() == null) {
        out.println("<h1>CDSi Demonstration System</h1> ");
        // make a table with two columns one with the step link and label and the other
        // with forecast
        out.println("<h2>Step Examples</h2>");
        out.println("<table>");
        out.println("  <tr>");
        out.println("    <th>Step</th>");
        out.println("    <th>Forecast</th>");
        out.println("  </tr>");
        List<StepExample> stepExamplesCopy = new ArrayList<StepExample>();
        synchronized (stepExamples) {
          stepExamplesCopy.addAll(stepExamples);
        }
        for (StepExample stepExample : stepExamples) {
          out.println("  <tr>");
          String stepLink = "step?" + stepExample.getRequestString();
          String forecastLink = "forecast?" + stepExample.getRequestString();
          out.println("      <td><a href=\"" + stepLink + "\">" + stepExample.getLabel() + "</a></td><td><a href=\""
              + forecastLink + "\">Forecast</a></td>");
          out.println("  </tr>");
        }
        out.println("</table>");
      } else {
        printStableView(dataModel, out);
      }

    }
    out.println("    </div>");

    out.println("    <div class=\"cell\">");
    if (dataModel.getLogicStepPrevious() != null || req.getParameter(LogicStep.PARAM_EVAL_DATE) != null) {
      out.println("        <input type=\"submit\" name=\"submit\" value=\"Next Step\"/>");
      out.println("        <select name=\"jumpTo\">");
      for (LogicStepType logicStepType : LogicStep.STEPS) {
        String display = logicStepType.getName();
        if (logicStepType.isIndent()) {
          display = " + " + display;
        }
        if (dataModel.getLogicStep().getLogicStepType() == logicStepType) {
          out.println("          <option value=\"" + logicStepType.getName() + "\" selected>"
              + display + "</option>");
        } else {
          out.println("          <option value=\"" + logicStepType.getName() + "\">" + display
              + "</option>");
        }
      }
      out.println("        </select>");
      out.println("        <input type=\"submit\" name=\"submit\" value=\"Jump\"/>");
      out.println("        <input type=\"submit\" name=\"submit\" value=\"Jump4.4\"/>");
      out.println("        <input type=\"submit\" name=\"submit\" value=\"Jump4.5\"/>");
      out.println("        <input type=\"submit\" name=\"submit\" value=\"Jump4.6\"/>");
      out.println("        <input type=\"submit\" name=\"submit\" value=\"End\"/>");
      out.println("        <input type=\"hidden\" name=\"action\" value=\"next\"/>");
      out.println("        <br/>");
    }
    if (dataModel.getLogicStepPrevious() == null || dataModel.getLogicStep() == null) {
      String imageLink = "pm.png?v=" + SoftwareVersion.VERSION;
      out.println("        <img src=\"" + imageLink
          + "\" width=\"800\" onclick=\"document.getElementById('stepForm').submit();\">");
    } else {
      String imageLink = dataModel.getLogicStepPrevious().getLogicStepType().getChapter()
          + "-" + dataModel.getLogicStep().getLogicStepType().getChapter() + ".png?v=" + SoftwareVersion.VERSION;
      out.println("        <img src=\"pm-" + imageLink
          + ".png\" width=\"800\" onclick=\"document.getElementById('stepForm').submit();\">");
    }
    out.println("    </div>");

    out.println("    <div class=\"cell\">");
    try {
      LogicStep logicStep = dataModel.getLogicStepPrevious();
      if (logicStep != null) {
        LogicStepType logicStepType = logicStep.getLogicStepType();
        out.println("<h1>" + logicStepType.getChapter() + " " + logicStepType.getName() + "</h1>");
        logicStep.printPost(out);
        logicStep.printLog(out);
      }
    } catch (Exception e) {
      e.printStackTrace();
      out.println("<pre>");
      e.printStackTrace(out);
      out.println("</pre>");
    }
    out.println("    </div>");
    out.println("    <div class=\"cell\">");
    try {
      LogicStep logicStep = dataModel.getLogicStep();
      if (logicStep != null) {
        LogicStepType logicStepType = logicStep.getLogicStepType();
        out.println("<h1>" + logicStepType.getChapter() + " " + logicStepType.getName() + "</h1>");
        logicStep.printPre(out);
      }
    } catch (Exception e) {
      e.printStackTrace();
      out.println("<pre>");
      e.printStackTrace(out);
      out.println("</pre>");
    }
    out.println("    </div>");
    out.println("      </form>");

    out.println("  </body>");
    out.println("</html>");
    out.close();
  }

  private void printStableView(DataModel dataModel, PrintWriter out) {
    // print out antigen

    if (dataModel.getPatientSeries() != null) {
      out.println("<h2>" + dataModel.getPatientSeries() + "</h2>");
    } else if (dataModel.getAntigen() != null) {
      out.println("<h2>" + dataModel.getAntigen() + "</h2>");
    }
    if (dataModel.getTargetDoseList() != null) {

      List<VaccineDoseAdministered> vaccineDoseAdministeredList = new ArrayList<VaccineDoseAdministered>();
      if (dataModel.getSelectedAntigenAdministeredRecordList() != null) {
        for (AntigenAdministeredRecord aar : dataModel.getSelectedAntigenAdministeredRecordList()) {
          vaccineDoseAdministeredList.add(aar.getVaccineDoseAdministered());
        }
      }
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th colspan=\"2\">Series</th>");
      out.println("    <th colspan=\"2\">Dose Administered</th>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <th>Selected</th>");
      out.println("    <th>Dose</th>");
      out.println("    <th>Status</th>");
      out.println("    <th>Vaccine Dose Admin</th>");
      out.println("  </tr>");
      for (TargetDose targetDose : dataModel.getTargetDoseList()) {
        TargetDose targetDoseSelected = dataModel.getTargetDose();
        List<Evaluation> evaluationList = targetDose.getEvaluationList();
        int rowSpan = evaluationList.size();
        // need to check if a new vaccination record is being evaluated
        AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
        boolean aarSelectedButNotYetEvaluated = false;
        if (aar != null && targetDoseSelected != null && targetDoseSelected == targetDose) {
          aarSelectedButNotYetEvaluated = true;
          for (Evaluation evaluation : evaluationList) {
            if (evaluation.getVaccineDoseAdministered() != null
                && evaluation.getVaccineDoseAdministered() == aar.getVaccineDoseAdministered()) {
              aarSelectedButNotYetEvaluated = false;
            }
          }
          if (aarSelectedButNotYetEvaluated) {
            rowSpan++;
          }
        }
        if (rowSpan == 0) {
          rowSpan = 1;
        }

        String doseNumber = targetDose.getTrackedSeriesDose().getDoseNumber();
        out.println("  <tr>");
        if (targetDoseSelected != null && targetDoseSelected == targetDose) {
          out.println("    <td rowspan=\"" + rowSpan + "\">--&gt;</td>");
        } else {
          out.println("    <td rowspan=\"" + rowSpan + "\"></td>");
        }
        out.println("    <td rowspan=\"" + rowSpan + "\">" + doseNumber + "</td>");
        if (evaluationList.size() == 0 && !aarSelectedButNotYetEvaluated) {
          out.println("    <td></td>");
          out.println("    <td></td>");
        }
        boolean first = true;
        for (Evaluation evaluation : evaluationList) {
          if (!first) {
            out.println("</tr><tr>");
          }
          out.println("    <td>" + evaluation.getEvaluationStatus() + "</td>");
          VaccineDoseAdministered vda = evaluation.getVaccineDoseAdministered();
          printVda(out, vda);
          vaccineDoseAdministeredList.remove(vda);
          first = false;
        }
        if (aarSelectedButNotYetEvaluated) {
          if (!first) {
            out.println("</tr><tr>");
          }
          out.println("    <td>--&gt;</td>");
          VaccineDoseAdministered vda = aar.getVaccineDoseAdministered();
          printVda(out, vda);
          vaccineDoseAdministeredList.remove(vda);
        }
        out.println("  </tr>");
      }
      for (VaccineDoseAdministered vda : vaccineDoseAdministeredList) {
        out.println("  <tr>");
        out.println("    <td colspan=\"3\"></td>");
        printVda(out, vda);
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (dataModel.getPatientSeriesList() != null && dataModel.getPatientSeriesList().size() > 0) {
      out.println("psl hashcode " + dataModel.getPatientSeriesList().hashCode() + "<br/>");
      out.println("<h2>Forecasts</h2>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>Antigen Series</th>");
      out.println("    <th>Status</th>");
      out.println("    <th>Earliest</th>");
      out.println("    <th>Recommended</th>");
      out.println("  </tr>");
      for (PatientSeries patientSeries : dataModel.getPatientSeriesList()) {
        AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
        out.println("  <tr>");
        out.println("    <td>" + antigenSeries.getTargetDisease().getName() + "</td>");
        out.println("    <td>" + antigenSeries.getSeriesName() + "</td>");
        out.println("    <td>" + patientSeries.getPatientSeriesStatus() + "</td>");
        if (patientSeries.getForecast() == null) {
          out.println("    <td></td>");
          out.println("    <td></td>");
        } else {
          out.println("    <td>" + n(patientSeries.getForecast().getEarliestDate()) + "</td>");
          out.println("    <td>" + n(patientSeries.getForecast().getAdjustedRecommendedDate()) + "</td>");
        }
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (dataModel.getForecastList().size() > 0) {
      out.println("<p>Forecasts:</p>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>VGF Status</th>");
      out.println("    <th>Earliest</th>");
      out.println("    <th>Recommended</th>");
      out.println("  </tr>");
      for (Forecast forecast : dataModel.getForecastList()) {
        out.println("  <tr>");
        out.println("    <td>" + forecast.getAntigen().getName() + "</td>");
        out.println("    <td>" + (forecast.getVaccineGroupForecast() == null ? "null"
            : forecast.getVaccineGroupForecast().getVaccineGroupStatus()) + "</td>");
        out.println("    <td>" + n(forecast.getEarliestDate()) + "</td>");
        out.println("    <td>" + n(forecast.getAdjustedRecommendedDate()) + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (dataModel.getBestPatientSeriesList() != null) {
      out.println("<h3>Best Patient Series</h3>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>Antigen Series</th>");
      out.println("    <th>Status</th>");
      out.println("    <th>Earliest</th>");
      out.println("    <th>Recommended</th>");
      out.println("  </tr>");
      for (PatientSeries patientSeries : dataModel.getBestPatientSeriesList()) {
        AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
        out.println("  <tr>");
        out.println("    <td>" + antigenSeries.getTargetDisease().getName() + "</td>");
        out.println("    <td>" + antigenSeries.getSeriesName() + "</td>");
        out.println("    <td>" + patientSeries.getPatientSeriesStatus() + "</td>");
        if (patientSeries.getForecast() == null) {
          out.println("    <td>null</td>");
          out.println("    <td>null</td>");
        } else {
          out.println("    <td>" + n(patientSeries.getForecast().getEarliestDate()) + "</td>");
          out.println("    <td>" + n(patientSeries.getForecast().getAdjustedRecommendedDate()) + "</td>");
        }
        out.println("  </tr>");
      }
      out.println("</table>");
    }
  }

  private void printVda(PrintWriter out, VaccineDoseAdministered vda) {
    if (vda == null) {
      out.println("    <td></td>");
    } else {
      VaccineType vaccineType = vda.getVaccine().getVaccineType();
      String vaccineLabel = vaccineType.getShortDescription() + " (" + vaccineType.getCvxCode() + ") given "
          + n(vda.getDateAdministered());
      out.println("    <td>" + vaccineLabel + "</td>");
    }
  }

  private void jump(DataModel dataModel, String jumpTo) throws Exception {
    int count = 0;
    while (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END
        && !dataModel.getLogicStep().getLogicStepType().getName().equals(jumpTo)) {
      dataModel.setNextLogicStep(dataModel.getLogicStep().process());
      if (count++ > 100000) {
        throw new Exception("Jump loop over 100000 detected");
      }
    }
  }

}
