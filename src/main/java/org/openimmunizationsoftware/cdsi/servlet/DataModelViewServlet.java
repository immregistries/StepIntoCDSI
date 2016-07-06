package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;

public class DataModelViewServlet extends ForecastServlet {

  private static final String PARAM_VIEW = "view";
  private static final String VIEW_PATIENT = "patient";
  private static final String VIEW_MEDICAL_HISTORY = "medical_history";
  private static final String VIEW_VACCINE = "vaccine";
  private static final String PARAM_POS = "pos";
  private static final String VIEW_DATA_MODEL = "data_model";
  private static final String VIEW_IMMUNITY = "immunity";
  private static final String VIEW_ANTIGEN = "antigen";


  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
      view = VIEW_DATA_MODEL;
    }

    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
      out.println("<html>");
      out.println("  <head>");
      out.println("    <title>CDSi - Data Model View</title>");
      out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"index.css\">");
      out.println("  </head>");
      out.println("  <body>");
      if (view.equals(VIEW_PATIENT)) {
        printViewPatient(dataModel, out);
      } else if (view.equals(VIEW_VACCINE)) {
        int pos = Integer.parseInt(req.getParameter(PARAM_POS));
        printViewVaccine(dataModel, pos, out);
      } else if (view.equals(VIEW_DATA_MODEL)) {
        printViewDataModel(dataModel, out);
      }
      out.println("  </body>");
      out.println("</html>");
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
//    out.println("       <th>Target Dose</th>");
//    out.println("       <th>Antigen</th>");
    out.println("     </tr>");

    int pos = 0;
    for (VaccineDoseAdministered vaccineDoseAdministered: dataModel.getPatient().getReceivesList()) {
      printRowVaccineDoseAdmistered(vaccineDoseAdministered,pos, out);
      pos++;
    }


    out.println("   </table>");
  }

  private void printViewVaccine(DataModel dataModel, int i, PrintWriter out) {
    out.println("   <table>");

    out.println("     <tr>");
    out.println("       <caption>Vaccine</caption>");

    out.println("       <th>Expiration Date</th>");
    out.println("       <td>" + n(dataModel.getPatient().getReceivesList().get(i).getVaccine().getLotExpirationDate()) + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Manufacturer</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine().getManufacturer() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Trade Name</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine().getTradeName() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Vaccine Type</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine().getVaccineType() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Vaccine Type Begin Age</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine().getVaccineTypeBeginAge() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Vaccine Type End Age</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine().getVaccineTypeEndAge() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Volume</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine().getVolume() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Antigen List</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine().getAntigenList() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Preferable Vaccine For Series</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine().getPreferableVaccineForSeries() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Allowable Vaccine For Series</th>");
    out.println("       <td>" + dataModel.getPatient().getReceivesList().get(i).getVaccine().getAllowableVaccineForSeries() + "</td>");
    out.println("     </tr>");

    out.println("   </table>");
  }


  private void printRowVaccineDoseAdmistered(VaccineDoseAdministered vaccineDoseAdministered, int pos, PrintWriter out) {
    out.println("     <tr>");
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    out.println("       <td>" + sdf.format(vaccineDoseAdministered.getDateAdministered()) + "</td>");
    out.println("       <td><a href=\"/dataModelView?" + PARAM_VIEW + "=" + VIEW_VACCINE + "&" + PARAM_POS + "=" + pos + "\">" + vaccineDoseAdministered.getVaccine().getVaccineType().getShortDescription() + " (" + vaccineDoseAdministered.getVaccine().getVaccineType().getCvxCode() + ")</a></td>");
    out.println("       <td>" + n(vaccineDoseAdministered.getDoseCondition()) + "</td>");
//    out.println("       <td>" +  + "</td>");
//    out.println("       <td></td>");
    out.println("     </tr>");
  }

  private String n(Object o) {
    if (o == null) {
      return "<center>-</center>";
    } else {
      return o.toString();
    }
  }
  private String n(Date d) {
    if (d == null) {
      return "<center>-</center>";
    } else {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      return sdf.format(d);
    }
  }
  private void printViewDataModel(DataModel dataModel, PrintWriter out) {
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <caption>Data Model</caption>");

    out.println("       <th>Immunity List</th>"); //another table on same page?
    out.println("       <td><a href=\"/dataMoselView?" + PARAM_VIEW + "=" + VIEW_IMMUNITY + "\">View</a></td>");
    out.println("     </tr>");

    out.println("     <tr>");
    out.println("       <th>Evaluation Status</th>");
    out.println("       <td>" + dataModel.getEvaluationStatus() + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Patient</th>");
    out.println("       <td><a href = \"/dataModelView?" + PARAM_VIEW + "=" + VIEW_PATIENT + "\">View</a></td>");
    out.println("     </tr>");
  /*  out.println("     <tr>");
    out.println("       <th>Immunization History</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");*/
    out.println("       <th>Assessment Date</th>");
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    out.println("       <td>" + sdf.format(dataModel.getAssessmentDate()) + "</td>");
    out.println("     </tr>");
    out.println("   </table>");





    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <caption>Live Virus Conflict</caption>");
    out.println("       <th>Schedule</th>");
    out.println("       <th>Previous Vaccine Type</th>");
    out.println("       <th>Current Vaccine Type</th>");
    out.println("       <th>Conflict Begin Interval</th>");
    out.println("       <th>Minimal Conflict End Interval</th>");
    out.println("       <th>Conflict End Interval</th>");
    out.println("     </tr>");
    for (LiveVirusConflict liveVirusConflict: dataModel.getLiveVirusConflictList()) {
      printRowLiveVirusConflict(liveVirusConflict, out);
    }
    out.println("   </table>");


/*    out.println("     <tr>");
    out.println("       <th>cvx Map</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Antigen Map</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Vaccine Group Map</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");*/

/*    out.println("     <tr>");
    out.println("       <th>Constraindictation List</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Schedule List</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");

    out.println("     <tr>");
    out.println("       <th>Target Dose</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Target Dose List</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");*/



    if (dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose() != null) {
      out.println("   <table>");
      out.println("     <tr>");
      out.println("       <caption>Antigen Administered Record That Satisfied Previous Target Dose</caption>");

      out.println("       <th>Antigen</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose().getAntigen() + "/td>");
      out.println("     </tr>");
      out.println("     <tr>");
      out.println("       <th>Date Administered</th>");

      out.println("       <td>" + sdf.format(dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose().getDateAdministered()) + "</td>");
      out.println("     </tr>");
      out.println("       <th>Vaccine Type</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose().getVaccineType() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Manufacturer</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose().getManufacturer() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Trade Name</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose().getTradeName() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Amount</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose().getAmount() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Lot Expiration Date</th>");
      out.println("       <td>" + sdf.format(dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose().getVaccineType()) + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Dose Condition</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose().getDoseCondition() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Evaluation</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose().getEvaluation() + "</td>");
      out.println("     </tr>");
      out.println("   </table>");
    }
    if (dataModel.getAntigenAdministeredRecord() != null) {
      out.println("   <table>");
      out.println("     <tr>");
      out.println("       <caption>Antigen Administered Record</caption>");

      out.println("       <th>Antigen</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecord().getAntigen() + "/td>");
      out.println("     </tr>");
      out.println("     <tr>");
      out.println("       <th>Date Administered</th>");

      out.println("       <td>" + sdf.format(dataModel.getAntigenAdministeredRecord().getDateAdministered()) + "</td>");
      out.println("     </tr>");
      out.println("       <th>Vaccine Type</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecord().getVaccineType() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Manufacturer</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecord().getManufacturer() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Trade Name</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecord().getTradeName() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Amount</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecord().getAmount() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Lot Expiration Date</th>");
      out.println("       <td>" + sdf.format(dataModel.getAntigenAdministeredRecord().getVaccineType()) + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Dose Condition</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecord().getDoseCondition() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Evaluation</th>");
      out.println("       <td>" + dataModel.getAntigenAdministeredRecord().getEvaluation() + "</td>");
      out.println("     </tr>");
      out.println("   </table>");
    }

    if (dataModel.getPreviousAntigenAdministeredRecord() != null) {
      out.println("   <table>");
      out.println("     <tr>");
      out.println("       <caption>Previous Antigen Administered Record</caption>");

      out.println("       <th>Antigen</th>");
      out.println("       <td>" + dataModel.getPreviousAntigenAdministeredRecord().getAntigen() + "/td>");
      out.println("     </tr>");
      out.println("     <tr>");
      out.println("       <th>Date Administered</th>");

      out.println("       <td>" + sdf.format(dataModel.getPreviousAntigenAdministeredRecord().getDateAdministered()) + "</td>");
      out.println("     </tr>");
      out.println("       <th>Vaccine Type</th>");
      out.println("       <td>" + dataModel.getPreviousAntigenAdministeredRecord().getVaccineType() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Manufacturer</th>");
      out.println("       <td>" + dataModel.getPreviousAntigenAdministeredRecord().getManufacturer() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Trade Name</th>");
      out.println("       <td>" + dataModel.getPreviousAntigenAdministeredRecord().getTradeName() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Amount</th>");
      out.println("       <td>" + dataModel.getPreviousAntigenAdministeredRecord().getAmount() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Lot Expiration Date</th>");
      out.println("       <td>" + sdf.format(dataModel.getPreviousAntigenAdministeredRecord().getVaccineType()) + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Dose Condition</th>");
      out.println("       <td>" + dataModel.getPreviousAntigenAdministeredRecord().getDoseCondition() + "</td>");
      out.println("     </tr>");
      out.println("     </tr>");
      out.println("       <th>Evaluation</th>");
      out.println("       <td>" + dataModel.getPreviousAntigenAdministeredRecord().getEvaluation() + "</td>");
      out.println("     </tr>");
      out.println("   </table>");
    }

/*    out.println("     <tr>");

    out.println("       <th>Antigen Administered Record List</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Antigen Series List</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Patient Series List</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Patient Series</th>");
    out.println("       <td>" +  + "</td>");
    out.println("     </tr>");
    out.println("   </table>");*/
  }
  private void printRowLiveVirusConflict(LiveVirusConflict liveVirusConflict, PrintWriter out) {
    out.println("     <tr>");
    out.println("       <td>" + liveVirusConflict.getSchedule() + "</td>");

    out.println("       <td>" + liveVirusConflict.getPreviousVaccineType() + "</td>");

    out.println("       <td>" + liveVirusConflict.getCurrentVaccineType() + "</td>");

    out.println("       <td>" + liveVirusConflict.getConflictBeginInterval() + "</td>");

    out.println("       <td>" + liveVirusConflict.getMinimalConflictEndInterval() + "</td>");

    out.println("       <td>" + liveVirusConflict.getConflictEndInterval() + "</td>");
    out.println("     </tr>");
  }

}
