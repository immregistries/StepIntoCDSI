package org.immregistries.step.servlet.dataModelView;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.immregistries.step.core.data.DataModel;
import org.immregistries.step.domain.AntigenAdministeredRecord;
import org.immregistries.step.domain.LiveVirusConflict;
import org.immregistries.step.domain.Patient;
import org.immregistries.step.domain.VaccineDoseAdministered;

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
      printHeader(out, session, MiniMenuItem.PATIENT);
      if (view.equals(VIEW_PATIENT)) {
        printViewPatient(dataModel, out);
      } else if (view.equals(VIEW_VACCINE)) {
        int pos = Integer.parseInt(req.getParameter(PARAM_POS));
        printViewVaccine(dataModel, pos, out);
      }
      printFooter(out, session);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }
  }

  private void printViewPatient(DataModel dataModel, PrintWriter out) {
    out.println("   <table>");

    out.println("     <tr>");
    out.println("       <caption>Patient</caption>");
    out.println("       <th>Patient DOB</th>");
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    out.println("       <td>" + sdf.format(dataModel.getPatient().getDateOfBirth()) + "</td>");
    out.println("     </tr>");

    out.println("     <tr>");
    out.println("       <th>Gender</th>");
    out.println("       <td>" + dataModel.getPatient().getGender() + "</td>");
    out.println("     </tr>");

    out.println("     <tr>");
    out.println("       <th>Country of Birth</th>");
    out.println("       <td>" + dataModel.getPatient().getCountryOfBirth() + "</td>");
    out.println("     </tr>");
    out.println("   </table>");

    out.println("   <table>");
    out.println("     <caption>Vaccine Dose Administered</caption>");
    out.println("     <tr>");
    out.println("       <th>Date Administered</th>");
    out.println("       <th>Vaccine</th>");
    out.println("       <th>Dose Condition</th>");
    // out.println(" <th>Target Dose</th>");
    // out.println(" <th>Antigen</th>");
    out.println("     </tr>");

    int pos = 0;
    for (VaccineDoseAdministered vaccineDoseAdministered : dataModel.getPatient()
        .getReceivesList()) {
      printRowVaccineDoseAdmistered(vaccineDoseAdministered, pos, out);
      pos++;
    }

    out.println("   </table>");
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


  private String n(Date d) {
    if (d == null) {
      return "<center>-</center>";
    } else {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      return sdf.format(d);
    }
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
      out.println("     </tr>");
      out.println("       <th>Evaluation</th>");
      out.println("       <td>" + antigenAdministeredRecord.getEvaluation() + "</td>");
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
