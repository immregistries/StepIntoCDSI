package org.immregistries.step.core.logic;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import org.immregistries.step.core.data.DataModel;
import org.immregistries.step.domain.Antigen;
import org.immregistries.step.domain.AntigenAdministeredRecord;
import org.immregistries.step.domain.VaccineDoseAdministered;
import org.immregistries.step.servlet.dataModelView.AntigenServlet;

public class OrganizeImmunizationHistory extends LogicStep {

  public OrganizeImmunizationHistory(DataModel dataModel) {
    super(LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY, dataModel);
  }

  @Override
  public LogicStep process() {

    for (VaccineDoseAdministered vda : dataModel.getImmunizationHistory()
        .getVaccineDoseAdministeredList()) {
      for (Antigen antigen : vda.getVaccine().getVaccineType().getAntigenList()) {
        AntigenAdministeredRecord aar = new AntigenAdministeredRecord(vda, antigen);
        dataModel.getAntigenAdministeredRecordList().add(aar);
      }
    }

    Collections.sort(dataModel.getAntigenAdministeredRecordList(),
        new Comparator<AntigenAdministeredRecord>() {
          @Override
          public int compare(AntigenAdministeredRecord o1, AntigenAdministeredRecord o2) {
            Antigen a1 = o1.getAntigen();
            Antigen a2 = o2.getAntigen();
            if (a1 == null || a2 == null || a1.getName().equalsIgnoreCase(a2.getName())) {
              return o1.getDateAdministered().compareTo(o2.getDateAdministered());
            }
            return a1.getName().compareTo(a2.getName());
          }
        });

    return LogicStepFactory.createLogicStep(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES,
        dataModel);
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println("<h1>8.3 Organize Immunization History</h1>");
    out.println(
        "   <p>The third step in the process is to look at the patient's immunization history and prepare those records "
            + "for evaluation and forecasting by breaking them into their antigen parts. This allows the evaluation and "
            + "forecasting engine to be as granular and specific as possible for both evaluation and forecasting purposes. "
            + "Later in the process (section 8.6), these antigens are assembled into commonly known vaccine groups (vaccine families) "
            + "for vaccine group forecasts.</p>");

  }

  @Override
  public void printPost(PrintWriter out) {
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    out.println("<h1>8.3 Organize Immunization History</h1>");

    out.println(
        "   <p>The third step in the process is to look at the patient's immunization history and prepare those records "
            + "for evaluation and forecasting by breaking them into their antigen parts. This allows the evaluation and "
            + "forecasting engine to be as granular and specific as possible for both evaluation and forecasting purposes. "
            + "Later in the process (section 8.6), these antigens are assembled into commonly known vaccine groups (vaccine families) "
            + "for vaccine group forecasts.</p>");

    out.println("<h2>Table 8 - 2 Prior to Organize Immunization History Example</h2>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Product (CVX/MVX) - Description</th>");
    out.println("    <th>Date</th>");
    out.println("  </tr>");
    for (VaccineDoseAdministered vda : dataModel.getImmunizationHistory()
        .getVaccineDoseAdministeredList()) {
      out.println("  <tr>");
      out.println("    <td>" + vda.getVaccine().getTradeName() + " ("
          + vda.getVaccine().getVaccineType().getCvxCode() + "/"
          + vda.getVaccine().getManufacturer() + ") - "
          + vda.getVaccine().getVaccineType().getShortDescription() + "</td>");
      out.println("    <td>" + sdf.format(vda.getDateAdministered()) + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");

    out.println("<h2>Table 8 - 3 After Organize Immunization History Example</h2>");
    out.println("<p>*Sorted by antigen and then by date</p>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Product (CVX/MVX) - Description</th>");
    out.println("    <th>Date</th>");
    out.println("    <th>Antigen*</th>");
    out.println("  </tr>");
    for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {
      out.println("  <tr>");
      out.println("    <td>" + aar.getTradeName() + " (" + aar.getVaccineType().getCvxCode() + "/"
          + aar.getManufacturer() + ") - " + aar.getVaccineType().getShortDescription() + "</td>");
      out.println("    <td>" + sdf.format(aar.getDateAdministered()) + "</td>");
      out.println("    <td>" + AntigenServlet.makeLink(aar.getAntigen()) + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");
  }

}
