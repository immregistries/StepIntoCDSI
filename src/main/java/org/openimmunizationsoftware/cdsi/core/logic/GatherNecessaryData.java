package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.servlet.ServletUtil.safe;

import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.ForecastInput;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.Observation;
import org.openimmunizationsoftware.cdsi.core.domain.ObservationCode;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientObservation;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.servlet.dataModelView.AntigenServlet;

public class GatherNecessaryData extends LogicStep {

  public GatherNecessaryData(DataModel dataModel) {
    super(LogicStepType.GATHER_NECESSARY_DATA, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {

    ForecastInput input = dataModel.getForecastInput();
    if (input == null) {
      throw new IllegalStateException(
          "No ForecastInput has been set on the data model; the caller must adapt its input (web request, FITS test case, FHIR operation, etc.) into a ForecastInput before running the engine");
    }

    Patient patient = new Patient();
    dataModel.setPatient(patient);
    patient.setDateOfBirth(input.getPatientDateOfBirth());
    patient.setGender(input.getPatientSex());
    dataModel.setAssessmentDate(input.getAssessmentDate());

    ImmunizationHistory immunizationHistory = new ImmunizationHistory();
    dataModel.setImmunizationHistory(immunizationHistory);
    patient.getMedicalHistory().setImmunizationHistory(immunizationHistory);
    immunizationHistory.setMedicalHistory(patient.getMedicalHistory());

    int id = 1;
    for (ForecastInput.VaccinationInput v : input.getVaccinationList()) {
      VaccineDoseAdministered vda = new VaccineDoseAdministered();
      vda.setId(id++);
      vda.setPatient(patient);
      vda.setImmunizationHistory(immunizationHistory);
      immunizationHistory.getVaccineDoseAdministeredList().add(vda);
      patient.getReceivesList().add(vda);
      vda.setDateAdministered(v.getDateAdministered());
      if (v.getDoseCondition() != null) {
        vda.setDoseCondition(v.getDoseCondition());
      }
      String cvxCode = v.getVaccineCvx();
      VaccineType cvx = dataModel.getCvxMap().get(cvxCode);
      if (cvx == null) {
        throw new IllegalArgumentException("Unrecognized cvx code '" + cvxCode + "'");
      }
      Vaccine vaccine = new Vaccine();
      vaccine.setVaccineType(cvx);
      vaccine.setManufacturer(v.getVaccineMvx());
      vda.setVaccine(vaccine);
    }

    for (ForecastInput.ObservationInput o : input.getObservationList()) {
      String observationCodeValue = o.getObservationCode();
      if (observationCodeValue == null || observationCodeValue.equals("")) {
        continue;
      }
      Observation observation = dataModel.getObservationMap().get(observationCodeValue);
      if (observation == null) {
        throw new IllegalArgumentException("Unrecognized observation code '" + observationCodeValue + "'");
      }

      PatientObservation patientObservation = new PatientObservation();
      ObservationCode observationCode = new ObservationCode();
      observationCode.setCode(observation.getObservationCode());
      observationCode.setText(observation.getObservationTitle());
      patientObservation.setObservationCode(observationCode);
      patientObservation.setObservationDate(o.getObservationDate());

      patient.getMedicalHistory().getPatientObservationList().add(patientObservation);
    }

    return LogicStepFactory.createLogicStep(LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY, dataModel);
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println("<h2>Input Data</h2>");

    HttpServletRequest req = dataModel.getRequest();

    out.println("<p>Patient input data:</p>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Patient DOB</th>");
    out.println("    <td><input type=\"text\" name=\"" + PARAM_PATIENT_DOB + "\" value=\""
        + n(req.getParameter(PARAM_PATIENT_DOB)) + "\" size=\"10\"/></td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <th>Patient Gender</th>");
    out.println("    <td><input type=\"text\" name=\"" + PARAM_PATIENT_SEX + "\" value=\""
        + n(req.getParameter(PARAM_PATIENT_SEX)) + "\" size=\"3\"/></td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <th>Evaluation Date</th>");
    out.println("    <td><input type=\"text\" name=\"" + PARAM_EVAL_DATE + "\" value=\""
        + n(req.getParameter(PARAM_EVAL_DATE)) + "\" size=\"10\"/></td>");
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
    while (req.getParameter(PARAM_VACCINE_CVX + i) != null) {
      // i needs to e in a hidden field called id
      out.println("  <tr>");
      out.println("    <th>" + i + "</th>");
      out.println("    <td><input type=\"text\" name=\"" + PARAM_VACCINE_CVX + i + "\" value=\""
          + n(req.getParameter(PARAM_VACCINE_CVX + i)) + "\" size=\"3\"/></td>");
      out.println("    <td><input type=\"text\" name=\"" + PARAM_VACCINE_MVX + i + "\" value=\""
          + n(req.getParameter(PARAM_VACCINE_MVX + i)) + "\" size=\"3\"/></td>");
      out.println("    <td><input type=\"text\" name=\"" + PARAM_VACCINE_DATE + i + "\" value=\""
          + n(req.getParameter(PARAM_VACCINE_DATE + i)) + "\" size=\"10\"/></td>");
      out.println(
          "    <td><input type=\"text\" name=\"" + PARAM_VACCINE_CONDITION_CODE + i + "\" value=\""
              + n(req.getParameter(PARAM_VACCINE_CONDITION_CODE + i)) + "\" size=\"3\"/></td>");
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
    while (req.getParameter(PARAM_OBSERVATION_CODE + i) != null) {
      out.println("  <tr>");
      out.println("    <th>" + i + "</th>");
      out.println("    <td><input type=\"text\" name=\"" + PARAM_OBSERVATION_CODE + i + "\" value=\""
          + n(req.getParameter(PARAM_OBSERVATION_CODE + i)) + "\" size=\"12\"/></td>");
      out.println("    <td><input type=\"text\" name=\"" + PARAM_OBSERVATION_DATE + i + "\" value=\""
          + n(req.getParameter(PARAM_OBSERVATION_DATE + i)) + "\" size=\"10\"/></td>");
      out.println("  </tr>");
      i++;
    }
    out.println("</table>");

  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    out.println("   <h2>Patient-Related Data</h2>");
    out.println("   <h3>Patient</h3>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Age</th>");
    out.println("       <td>" + sdf.format(dataModel.getPatient().getDateOfBirth()) + "</td>");
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
          "       <td>" + sdf.format(vaccineDoseAdministered.getDateAdministered()) + "</td>");
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
      out.println("       <td>" + n(patientObservation.getObservationDate()) + "</td>");
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

  private String n(String value) {
    if (value == null) {
      return "";
    }
    return value;
  }

}
