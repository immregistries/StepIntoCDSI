package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepFactory;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;

public class ForecastServlet extends HttpServlet {

  public static final String RESULT_FORMAT_TEXT = "text";
  public static final String RESULT_FORMAT_HTML = "html";
  public static final String RESULT_FORMAT_COMPACT = "compact";

  public static final String PARAM_EVAL_DATE = "evalDate";
  public static final String PARAM_EVAL_SCHEDULE = "evalSchedule";
  public static final String PARAM_RESULT_FORMAT = "resultFormat";
  public static final String PARAM_PATIENT_DOB = "patientDob";
  public static final String PARAM_PATIENT_SEX = "patientSex";
  public static final String PARAM_VACCINE_DATE_ = "vaccineDate";
  public static final String PARAM_VACCINE_CVX_ = "vaccineCvx";
  public static final String PARAM_VACCINE_MVX_ = "vaccineMvx";
  public static final String PARAM_VACCINE_CONDITION_CODE_ = "vaccineConditionCode";

  protected static final String SCHEDULE_NAME_DEFAULT = "default";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try {
      DataModel dataModel = readRequest(req);
      resp.setContentType("text/plain");
      PrintWriter out = new PrintWriter(resp.getOutputStream());
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
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServletException(e);
    }
  }

  protected DataModel readRequest(HttpServletRequest req) throws Exception {
    DataModel dataModel = DataModelLoader.createDataModel();
    dataModel.setRequest(req);
    {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
      Patient patient = new Patient();
      dataModel.setPatient(patient);
      patient.setDateOfBirth(sdf.parse(req.getParameter(PARAM_PATIENT_DOB)));
      patient.setGender(req.getParameter(PARAM_PATIENT_SEX));
      dataModel.setAssessmentDate(sdf.parse(req.getParameter(PARAM_EVAL_DATE)));

      ImmunizationHistory immunizationHistory = new ImmunizationHistory();
      dataModel.setImmunizationHistory(immunizationHistory);
      int i = 1;
      while (req.getParameter(PARAM_VACCINE_CVX_ + i) != null) {
        VaccineDoseAdministered vaccineDoseAdministered = new VaccineDoseAdministered();
        vaccineDoseAdministered.setPatient(patient);
        vaccineDoseAdministered.setImmunizationHistory(immunizationHistory);
        immunizationHistory.getVaccineDoseAdministeredList().add(vaccineDoseAdministered);
        patient.getReceivesList().add(vaccineDoseAdministered);
        vaccineDoseAdministered.setDateAdministered(sdf.parse(req.getParameter(PARAM_VACCINE_DATE_ + i)));
        if (req.getParameter(PARAM_VACCINE_CONDITION_CODE_ + i) != null
            && !req.getParameter(PARAM_VACCINE_CONDITION_CODE_ + i).equals("")) {
          vaccineDoseAdministered
              .setDoseCondition(req.getParameter(PARAM_VACCINE_CONDITION_CODE_ + i).equalsIgnoreCase("yes")
                  ? DoseCondition.YES : DoseCondition.NO);
        }
        String cvxCode = req.getParameter(PARAM_VACCINE_CVX_ + i);
        String mvxCode = req.getParameter(PARAM_VACCINE_MVX_ + i);
        Vaccine vaccine = new Vaccine();
        VaccineType cvx = dataModel.getCvxMap().get(cvxCode);
        if (cvx == null) {
          throw new IllegalArgumentException("Unrecognized cvx code '" + cvxCode + "'");
        }
        vaccine.setVaccineType(cvx);
        vaccine.setManufacturer(mvxCode);
        vaccineDoseAdministered.setVaccine(vaccine);
        i++;
      }

    }
    dataModel.setNextLogicStep(LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel));
    return dataModel;
  }
}
