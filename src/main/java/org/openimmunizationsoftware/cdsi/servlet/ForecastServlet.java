package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepFactory;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;

public class ForecastServlet extends HttpServlet {

  public static final String RESULT_FORMAT_TEXT = "text";
  public static final String RESULT_FORMAT_HTML = "html";
  public static final String RESULT_FORMAT_COMPACT = "compact";

  public static final String PARAM_RESULT_FORMAT = "resultFormat";

  protected static final String SCHEDULE_NAME_DEFAULT = "default";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try {
      DataModel dataModel = readRequest(req);
      process(dataModel);
      PrintWriter out = new PrintWriter(resp.getOutputStream());
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

  private void printText(HttpServletResponse resp, DataModel dataModel, PrintWriter out) {
    resp.setContentType("text/plain");
    out.println("Step Into Clinical Decision Support for Immunizations - Demonstration System");
    out.println();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    
    
    out.println("VACCINATIONS RECOMMENDED " + sdf.format(dataModel.getAssessmentDate()));
    out.println();

    out.println("VACCINATIONS RECOMMENDED AFTER " + sdf.format(dataModel.getAssessmentDate()));
    out.println();

    out.println("VACCINATIONS COMPLETE OR NOT RECOMMENDED");
    out.println();

    if (dataModel.getAntigenAdministeredRecordList().size() > 0) {
      out.println("IMMUNIZATION EVALUATION");
      int count = 0;

      for (VaccineDoseAdministered vda : dataModel.getImmunizationHistory().getVaccineDoseAdministeredList()) {
        count++;
        for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {
          if (aar.getVaccineType().equals(vda.getVaccine().getVaccineType())
              && aar.getDateAdministered().equals(vda.getDateAdministered())) {
            out.print("Vaccination #" + count + ": " + aar.getVaccineType().getShortDescription() + " given "
                + sdf.format(aar.getDateAdministered()));
            // is a valid Hib dose 1. Dose 1 valid at 6 weeks of age,
            // 10/14/2012.
            out.println();
          }
        }
      }
      out.println();
    }

    out.println(
        "Forecast generated " + sdf.format(new Date()) + " using software version " + SoftwareVersion.VERSION + ".");
  }

  private void process(DataModel dataModel) throws Exception {
    int count = 0;
    while (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END) {
      dataModel.setNextLogicStep(dataModel.getLogicStep().process());
      count++;
      if (count > 100000) {
        System.err.println("Appear to be caught in a loop at this step: " + dataModel.getLogicStep().getTitle());
        // too many steps!
        if (count > 100010) {
          throw new RuntimeException("Logic steps seem to be caught in a loop, unable to get results");
        }
      }
    }
  }

  protected DataModel readRequest(HttpServletRequest req) throws Exception {
    DataModel dataModel = DataModelLoader.createDataModel();
    dataModel.setRequest(req);
    dataModel.setNextLogicStep(LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel));
    return dataModel;
  }
}
