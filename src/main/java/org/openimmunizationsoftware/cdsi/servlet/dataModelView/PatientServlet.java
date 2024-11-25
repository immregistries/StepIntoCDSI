package org.openimmunizationsoftware.cdsi.servlet.dataModelView;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;

public class PatientServlet extends MainServlet {

  public static final String SERVLET_NAME = "dataModelViewPatient";

  public static final String PARAM_VIEW = "view";
  public static final String VIEW_PATIENT = "patient";
  private static final String VIEW_VACCINE = "vaccine";
  private static final String PARAM_POS = "pos";

  public static String makeLink(Patient patient) {
    if (patient == null) {
      return "";
    }
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    return "<a href=\"" + SERVLET_NAME + "\" target=\"dataModelView\">Patient born "
        + sdf.format(patient.getDateOfBirth()) + "</a>";
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    HttpSession session = req.getSession(true);
    DataModel dataModel = (DataModel) session.getAttribute("dataModel");
    if (dataModel == null) {
      return;
    }

    resp.setContentType("text/html");

    // String s= req.getParameter("s");
    // Integer.parseInt(s);
    // s.equals("");
    // out.println(" <a href=\"dataModelView?s=" + s + "\"");

    String view = req.getParameter("view");
    if (view == null) {
      view = VIEW_PATIENT;
    }

    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      printHeader(out, "Patient");
      if (view.equals(VIEW_PATIENT)) {
        printViewPatient(dataModel, out);
      } else if (view.equals(VIEW_VACCINE)) {
        int pos = Integer.parseInt(req.getParameter(PARAM_POS));
        printViewVaccine(dataModel, pos, out);
      }
      printViewForecast(dataModel, out);
      printFooter(out);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }
  }

  private void printViewPatient(DataModel dataModel, PrintWriter out) {
    // class="w3-table w3-bordered w3-striped w3-border test w3-hoverable"
    out.println("  <div class=\"w3-card w3-cell w3-margin\">");
    out.println("    <header class=\"w3-container w3-khaki\">");
    out.println("      <h2>Patient</h2>");
    out.println("    </header>");
    out.println("    <div class=\"w3-container\">");
    out.println("      <table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable w3-margin\">");
    out.println("        <caption>Demographics</caption>");
    out.println("        <tr>");
    out.println("          <th>Patient DOB</th>");
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    out.println("          <td>" + sdf.format(dataModel.getPatient().getDateOfBirth()) + "</td>");
    out.println("        </tr>");

    out.println("        <tr>");
    out.println("          <th>Gender</th>");
    out.println("          <td>" + dataModel.getPatient().getGender() + "</td>");
    out.println("        </tr>");

    out.println("        <tr>");
    out.println("          <th>Country of Birth</th>");
    out.println("          <td>" + dataModel.getPatient().getCountryOfBirth() + "</td>");
    out.println("        </tr>");
    out.println("      </table>");
    out.println("    </div>");
    out.println("  </div>");

    out.println("  <div class=\"w3-card w3-cell w3-margin\">");
    out.println("    <header class=\"w3-container w3-khaki\">");
    out.println("      <h2>Vaccinations</h2>");
    out.println("    </header>");
    out.println("    <div class=\"w3-container\">");
    out.println("      <table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
    out.println("        <caption>Vaccine Dose Administered</caption>");
    out.println("        <tr>");
    out.println("          <th>Date Administered</th>");
    out.println("          <th>Vaccine</th>");
    out.println("          <th>Dose Condition</th>");
    out.println("        </tr>");
    int pos = 0;
    for (VaccineDoseAdministered vaccineDoseAdministered : dataModel.getPatient()
        .getReceivesList()) {
      printRowVaccineDoseAdmistered(vaccineDoseAdministered, pos, out);
      pos++;
    }
    out.println("      </table>");
    out.println("    </div>");
    out.println("  </div>");
  }

  private void printViewVaccine(DataModel dataModel, int i, PrintWriter out) {
    out.println("   <table>");

    out.println("     <tr>");
    out.println("       <caption>Vaccine</caption>");

    out.println("       <th>Expiration Date</th>");
    out.println("       <td>"
        + n(dataModel.getPatient().getReceivesList().get(i).getVaccine().getLotExpirationDate())
        + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Manufacturer</th>");
    out.println("       <td>"
        + dataModel.getPatient().getReceivesList().get(i).getVaccine().getManufacturer() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Trade Name</th>");
    out.println("       <td>"
        + dataModel.getPatient().getReceivesList().get(i).getVaccine().getTradeName() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Vaccine Type</th>");
    out.println("       <td>"
        + dataModel.getPatient().getReceivesList().get(i).getVaccine().getVaccineType() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Vaccine Type Begin Age</th>");
    out.println("       <td>"
        + dataModel.getPatient().getReceivesList().get(i).getVaccine().getVaccineTypeBeginAge()
        + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Vaccine Type End Age</th>");
    out.println("       <td>"
        + dataModel.getPatient().getReceivesList().get(i).getVaccine().getVaccineTypeEndAge()
        + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Volume</th>");
    out.println("       <td>"
        + dataModel.getPatient().getReceivesList().get(i).getVaccine().getVolume() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Antigen List</th>");
    out.println("       <td>"
        + dataModel.getPatient().getReceivesList().get(i).getVaccine().getAntigenList() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Preferable Vaccine For Series</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine()
        .getPreferableVaccineForSeries() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Allowable Vaccine For Series</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine()
        .getAllowableVaccineForSeries() + "</td>");
    out.println("     </tr>");

    out.println("   </table>");
  }

  private void printRowVaccineDoseAdmistered(VaccineDoseAdministered vaccineDoseAdministered,
      int pos, PrintWriter out) {
    out.println("     <tr>");
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    out.println(
        "       <td>" + sdf.format(vaccineDoseAdministered.getDateAdministered()) + "</td>");
    out.println("       <td><a href=\"/dataModelView?" + PARAM_VIEW + "=" + VIEW_VACCINE + "&"
        + PARAM_POS + "=" + pos + "\">"
        + vaccineDoseAdministered.getVaccine().getVaccineType().getShortDescription() + " ("
        + vaccineDoseAdministered.getVaccine().getVaccineType().getCvxCode() + ")</a></td>");
    out.println("       <td>" + n(vaccineDoseAdministered.getDoseCondition()) + "</td>");
    // out.println(" <td>" + + "</td>");
    // out.println(" <td></td>");
    out.println("     </tr>");
  }

  private void printViewForecast(DataModel dataModel, PrintWriter out) {
    out.println("  <div class=\"w3-card w3-cell w3-margin\">");
    out.println("    <header class=\"w3-container w3-khaki\">");
    out.println("      <h2>Forecast</h2>");
    out.println("    </header>");
    out.println("    <div class=\"w3-container\">");
    out.println("      <table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
    out.println("        <caption>Forecast Doses</caption>");
    out.println("        <tr>");
    out.println("          <th>Antigen</th>");
    out.println("          <th>Adjusted Recommended Date</th>");
    out.println("          <th>Adjusted Past Due Date</th>");
    out.println("          <th>Earliest Date</th>");
    out.println("          <th>Forecast Reason</th>");
    out.println("          <th>Latest Date</th>");
    out.println("          <th>Unadjusted Recommended Date</th>");
    out.println("          <th>Unadjusted Past Due Date</th>");
    out.println("          <th>Vaccine Assessment Date</th>");
    out.println("          <th>Target Dose</th>");
    out.println("          <th>Best Patient Series</th>");
    out.println("        </tr>");
    int pos = 0;
    for (Forecast forecast : dataModel.getForecastList()) {
      printRowForecast(forecast, pos, out);
      pos++;
    }
    out.println("      </table>");
    out.println("    </div>");
    out.println("  </div>");
  }

  private void printRowForecast(Forecast forecast, int pos, PrintWriter out) {
    out.println("     <tr>");
    out.println("       <td>" + forecast.getAntigen().toString() + "</td>");
    out.println("       <td>" + n(forecast.getAdjustedRecommendedDate()) + "</td>");
    out.println("       <td>" + n(forecast.getAdjustedPastDueDate()) + "</td>");
    out.println("       <td>" + n(forecast.getEarliestDate()) + "</td>");
    out.println("       <td>" + forecast.getForecastReason() + "</td>");
    out.println("       <td>" + n(forecast.getLatestDate()) + "</td>");
    out.println("       <td>" + n(forecast.getUnadjustedRecommendedDate()) + "</td>");
    out.println("       <td>" + n(forecast.getUnadjustedPastDueDate()) + "</td>");
    out.println("       <td>" + n(forecast.getAssessmentDate()) + "</td>");
    out.println("       <td>" + forecast.getTargetDose().getTargetDoseStatus() + "</td>");
    out.println("       <td>" + forecast.getBestPatientSeries() + "</td>");
    out.println("     </tr>");
  }

  private void printAntigenAdministeredRecordTable(
      AntigenAdministeredRecord antigenAdministeredRecord, String caption, PrintWriter out) {
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    if (antigenAdministeredRecord != null) {
      out.println("   <table>");
      out.println("     <tr>");
      out.println("       <caption>" + caption + "</caption>");

      out.println("       <th>Antigen</th>");
      out.println("       <td>" + antigenAdministeredRecord.getAntigen() + "/td>");
      out.println("     </tr>");

      out.println("     <tr>");
      out.println("       <th>Date Administered</th>");

      out.println(
          "       <td>" + sdf.format(antigenAdministeredRecord.getDateAdministered()) + "</td>");
      out.println("     </tr>");
      out.println("       <th>Vaccine Type</th>");
      out.println("       <td>" + antigenAdministeredRecord.getVaccineType() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Manufacturer</th>");
      out.println("       <td>" + antigenAdministeredRecord.getManufacturer() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Trade Name</th>");
      out.println("       <td>" + antigenAdministeredRecord.getTradeName() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Amount</th>");
      out.println("       <td>" + antigenAdministeredRecord.getAmount() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Lot Expiration Date</th>");
      out.println("       <td>" + sdf.format(antigenAdministeredRecord.getVaccineType()) + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Dose Condition</th>");
      out.println("       <td>" + antigenAdministeredRecord.getDoseCondition() + "</td>");
      out.println("     </tr>");
      out.println("   </table>");
    }
  }

  private void printRowLiveVirusConflict(LiveVirusConflict liveVirusConflict, PrintWriter out) {
    out.println("     <tr>");
    out.println("       <td>" + n(liveVirusConflict.getSchedule()) + "</td>");

    out.println("       <td>" + liveVirusConflict.getPreviousVaccineType() + "</td>");

    out.println("       <td>" + liveVirusConflict.getCurrentVaccineType() + "</td>");

    out.println("       <td>" + liveVirusConflict.getConflictBeginInterval() + "</td>");

    out.println("       <td>" + liveVirusConflict.getMinimalConflictEndInterval() + "</td>");

    out.println("       <td>" + liveVirusConflict.getConflictEndInterval() + "</td>");
    out.println("     </tr>");
  }
}
