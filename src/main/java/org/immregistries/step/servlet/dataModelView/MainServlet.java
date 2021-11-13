package org.immregistries.step.servlet.dataModelView;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.immregistries.step.core.data.DataModel;
import org.immregistries.step.domain.AntigenAdministeredRecord;
import org.immregistries.step.domain.TargetDose;
import org.immregistries.step.servlet.HomeServlet;

public class MainServlet extends HomeServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    HttpSession session = req.getSession(true);
    DataModel dataModel = (DataModel) session.getAttribute("dataModel");
    if (dataModel == null) {
      return;
    }

    resp.setContentType("text/html");

    PrintWriter out = new PrintWriter(resp.getOutputStream());
    try {
      printHeader(out, session, MiniMenuItem.DATA_MODEL);
      printViewDataModel(dataModel, out);
      printFooter(out, session);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }
  }

  protected void printFooter(PrintWriter out, HttpSession session) {
    out.println("  </div>");
    out.println("</div>");
    doFooter(out, session);
  }


  protected void printHeader(PrintWriter out, HttpSession session, MiniMenuItem miniMenuItem) {
    doHeader(out, session);

    out.println("<div class=\"row\">");
    out.println("  <div class=\"col-sm-4\">");
    out.println("    <div class=\"list-group\">");
    for (MiniMenuItem mmi : MiniMenuItem.values()) {
      out.println("      <a href=\"" + mmi.getUrl() + "\"" + (mmi == miniMenuItem ? " active" : "")
          + ">" + mmi.getName() + "</a>");
    }
    out.println("    </div>");
    out.println("  </div>");
    out.println("  <div class=\"col-sm-8\">");
  }

  public String n(Object o) {
    if (o == null) {
      return "<center>-</center>";
    } else {
      return o.toString();
    }
  }

  private void printViewDataModel(DataModel dataModel, PrintWriter out) {
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <caption>Data Model</caption>");

    out.println("       <th>Evaluation Status</th>");
    out.println("       <td>" + n(dataModel.getEvaluationStatus()) + "</td>");
    out.println("     </tr>");
    out.println("     <tr>");
    out.println("       <th>Patient</th>");
    out.println("       <td>" + PatientServlet.makeLink(dataModel.getPatient()) + "</td>");
    out.println("     </tr>");
    out.println("       <th>Assessment Date</th>");
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    out.println("       <td>" + sdf.format(dataModel.getAssessmentDate()) + "</td>");
    out.println("     </tr>");
    out.println("   </table>");


    if (dataModel.getTargetDoseList() != null) {
      out.println("   <table>");
      out.println("     <tr>");
      out.println("       <caption>Target Dose List</caption>");
      out.println("       <th>Target Dose Status</th>");
      out.println("       <th>Tracked Dose Series</th>");
      out.println("       <th>Satisfied By Vaccine Dose Administered</th>");
      out.println("     </tr>");
      for (TargetDose targetDose : dataModel.getTargetDoseList()) {
        printRowTargetDoseList(targetDose, out);
      }
    }
    out.println("   </table>");

    if (dataModel.getTargetDose() != null) {

      out.println("   <table>");
      out.println("     <tr>");
      out.println("       <caption>Target Dose</caption>");
      out.println("       <th>Target Dose Status</th>");
      out.println("       <td>" + dataModel.getTargetDose().getTargetDoseStatus() + "</td>");
      out.println("     </tr>");
      out.println("     <tr>");
      out.println("       <th>Tracked Series Dose</th>");
      out.println("       <td>" + "-_-"
          + dataModel.getTargetDose().getTrackedSeriesDose().getDoseNumber() + "</td>"); ///
      out.println("     </tr>");
      out.println("     <tr>");
      out.println("       <th>Satisfied By Vaccine Dose Administered</th>");
      out.println("       <td>" + dataModel.getTargetDose().getSatisfiedByVaccineDoseAdministered()
          + "</td>");
      out.println("     </tr>");
      out.println("   </table>");
    }
    /*
     * out.println("     <tr>"); out.println("       <th>cvx Map</th>"); out.println("       <td>" +
     * + "</td>"); out.println("     </tr>"); out.println("     <tr>");
     * out.println("       <th>Antigen Map</th>"); out.println("       <td>" + + "</td>");
     * out.println("     </tr>"); out.println("     <tr>");
     * out.println("       <th>Vaccine Group Map</th>" ); out.println("       <td>" + + "</td>");
     * out.println("     </tr>");
     */

    /*
     * out.println("     <tr>"); out.println( "       <th>Constraindictation List</th>");
     * out.println("       <td>" + + "</td>"); out.println("     </tr>"); out.println("     <tr>");
     * out.println("       <th>Schedule List</th>"); out.println("       <td>" + + "</td>");
     * out.println("     </tr>");
     */

    AntigenAdministeredRecord antigenAdministeredRecordThatSatisfiedPreviousTargetDose =
        dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose();
    printAntigenAdministeredRecordTable(antigenAdministeredRecordThatSatisfiedPreviousTargetDose,
        "Antigen Administered Record That Satisfied Previous Target Dose", out);

    AntigenAdministeredRecord antigenAdministeredRecord = dataModel.getAntigenAdministeredRecord();
    printAntigenAdministeredRecordTable(antigenAdministeredRecord, "Antigen Administered Record",
        out);

    AntigenAdministeredRecord previousAntigenAdministeredRecord =
        dataModel.getPreviousAntigenAdministeredRecord();
    printAntigenAdministeredRecordTable(previousAntigenAdministeredRecord,
        "Previous Antigen Administered Record", out);

    /*
     * out.println("     <tr>");
     * 
     * out.println("       <th>Antigen Administered Record List</th>"); out.println("       <td>" +
     * + "</td>"); out.println("     </tr>"); out.println("     <tr>"); out.println(
     * "       <th>Antigen Series List</th>"); out.println("       <td>" + + "</td>");
     * out.println("     </tr>"); out.println("     <tr>");
     * out.println("       <th>Patient Series List</th>"); out.println( "       <td>" + + "</td>");
     * out.println("     </tr>"); out.println( "     <tr>");
     * out.println("       <th>Patient Series</th>"); out.println( "       <td>" + + "</td>");
     * out.println("     </tr>"); out.println( "   </table>");
     */
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



  void printRowTargetDoseList(TargetDose targetDose, PrintWriter out) {
    out.println("     <tr>");
    out.println("       <td>" + targetDose.getTargetDoseStatus() + "</td>");

    out.println(
        "       <td>" + "-_- " + targetDose.getTrackedSeriesDose().getDoseNumber() + "</td>");

    out.println("       <td>" + targetDose.getSatisfiedByVaccineDoseAdministered() + "</td>");

    out.println("     </tr>");
  }
}
