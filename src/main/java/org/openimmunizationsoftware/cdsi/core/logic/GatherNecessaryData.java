package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.servlet.ServletUtil.safe;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;
import org.openimmunizationsoftware.cdsi.servlet.dataModelView.AntigenServlet;

public class GatherNecessaryData extends LogicStep
{

  public GatherNecessaryData(DataModel dataModel) {
    super(LogicStepType.GATHER_NECESSARY_DATA, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {

    HttpServletRequest req = dataModel.getRequest();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    Patient patient = new Patient();
    dataModel.setPatient(patient);
    patient.setDateOfBirth(sdf.parse(req.getParameter(PARAM_PATIENT_DOB)));
    patient.setGender(req.getParameter(PARAM_PATIENT_SEX));
    dataModel.setAssessmentDate(sdf.parse(req.getParameter(PARAM_EVAL_DATE)));

    ImmunizationHistory immunizationHistory = new ImmunizationHistory();
    dataModel.setImmunizationHistory(immunizationHistory);
    int i = 1;
    while (req.getParameter(PARAM_VACCINE_CVX + i) != null) {
      VaccineDoseAdministered vaccineDoseAdministered = new VaccineDoseAdministered();
      vaccineDoseAdministered.setPatient(patient);
      vaccineDoseAdministered.setImmunizationHistory(immunizationHistory);
      immunizationHistory.getVaccineDoseAdministeredList().add(vaccineDoseAdministered);
      patient.getReceivesList().add(vaccineDoseAdministered);
      vaccineDoseAdministered.setDateAdministered(sdf.parse(req.getParameter(PARAM_VACCINE_DATE + i)));
      if (req.getParameter(PARAM_VACCINE_CONDITION_CODE + i) != null
          && !req.getParameter(PARAM_VACCINE_CONDITION_CODE + i).equals("")) {
        vaccineDoseAdministered.setDoseCondition(req.getParameter(PARAM_VACCINE_CONDITION_CODE + i).equalsIgnoreCase(
            "yes") ? DoseCondition.YES : DoseCondition.NO);
      }
      String cvxCode = req.getParameter(PARAM_VACCINE_CVX + i);
      String mvxCode = req.getParameter(PARAM_VACCINE_MVX + i);
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
    return LogicStepFactory.createLogicStep(LogicStepType.CREATE_PATIENT_SERIES, dataModel);
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println("<h1>8.1 Gather Necessary Data</h2>");
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
      out.println("  <tr>");
      out.println("    <th>" + i + "</th>");
      out.println("    <td><input type=\"text\" name=\"" + PARAM_VACCINE_CVX + i + "\" value=\""
          + n(req.getParameter(PARAM_VACCINE_CVX + i)) + "\" size=\"3\"/></td>");
      out.println("    <td><input type=\"text\" name=\"" + PARAM_VACCINE_MVX + i + "\" value=\""
          + n(req.getParameter(PARAM_VACCINE_MVX + i)) + "\" size=\"3\"/></td>");
      out.println("    <td><input type=\"text\" name=\"" + PARAM_VACCINE_DATE + i + "\" value=\""
          + n(req.getParameter(PARAM_VACCINE_DATE + i)) + "\" size=\"10\"/></td>");
      out.println("    <td><input type=\"text\" name=\"" + PARAM_VACCINE_CONDITION_CODE + i + "\" value=\""
          + n(req.getParameter(PARAM_VACCINE_CONDITION_CODE + i)) + "\" size=\"3\"/></td>");
      out.println("  </tr>");
      i++;
    }
    out.println("</table>");

  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    out.println("<h1>8.1 Gather Necessary Data</h2>");

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
      out.println("       <td>" + sdf.format(vaccineDoseAdministered.getDateAdministered()) + "</td>");
      out.println("       <td>" + vaccineDoseAdministered.getVaccine().getVaccineType() + "</td>");
      out.println("       <td>" + vaccineDoseAdministered.getVaccine().getManufacturer() + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
    out.println("   <h3>Adverse Events</h3>");
    out.println("   <p><em>Not implemented yet</em></p>");
    out.println("   <h3>Relevant Medical History</h3>");
    out.println("   <p><em>Not implemented yet</em></p>");

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
      out.println("       <td>" + safe(liveVirusConflict.getMinimalConflictEndInterval()) + "</td>");
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
