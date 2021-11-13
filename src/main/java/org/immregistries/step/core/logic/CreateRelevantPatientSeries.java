package org.immregistries.step.core.logic;

import java.io.PrintWriter;
import org.immregistries.step.core.data.DataModel;
import org.immregistries.step.domain.AntigenSeries;
import org.immregistries.step.domain.PatientSeries;

public class CreateRelevantPatientSeries extends LogicStep {

  public CreateRelevantPatientSeries(DataModel dataModel) {
    super(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    int i = 1;
    boolean foundAtLeastOne = false;
    for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
      if (dataModel.getRequest().getParameter(PARAM_ANTIGEN_SERIES_INCLUDE + i) != null) {
        PatientSeries patientSeries = new PatientSeries(antigenSeries);
        dataModel.getPatientSeriesList().add(patientSeries);
        foundAtLeastOne = true;
      }
      i++;
    }
    // If none are checked then check them all
    if (!foundAtLeastOne) {
      for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
        PatientSeries patientSeries = new PatientSeries(antigenSeries);
        dataModel.getPatientSeriesList().add(patientSeries);
        foundAtLeastOne = true;
      }
    }
    return LogicStepFactory.createLogicStep(LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY, dataModel);
  }

  public void printPre(PrintWriter out) throws Exception {
    out.println("   <h2>4.3 Create Relevant Patient Series</h2>");
    out.println(
        "   <p>An antigen series is one way to reach perceived immunity against a disease. An antigen series can be thought "
            + "of as a \"path to immunity\" and is described in relative terms. In many cases, a single antigen may have more "
            + "than one successful path to immunity and as such may have more than one antigen series. Antigen series are "
            + "defined through Supporting Data spreadsheets defined in Chapter 3. Some series, classified here as \"Standard\" "
            + "series, are based on recommendations for all patients based on age. Other series, classified as \"Risk\" series, "
            + "are based on recommendations for patients with specific characteristics or underlying conditions which put them "
            + "at increased risk. Finally, some series are strictly for the purpose of \"Evaluation Only\" and should not be "
            + "recommended for completion, but if already complete can be used as proof of series completion.</p>");
    out.println("   <h3>Instructions</h3>");
    out.println(
        "   <p>To support simple testing only antigen series selected will be run. In normal production mode all "
            + "antigen series would be considered. Select the antigen series that are desired to be included as patient series "
            + "and thus considered.</p>");
    out.println("   <h2>Antigen Series</h2>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Include</th>");
    out.println("       <th>Series Name</th>");
    out.println("       <th>Target Disease</th>");
    out.println("       <th>Vaccine Group</th>");
    out.println("     </tr>");
    int i = 1;
    for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
      out.println("     <tr>");
      out.println("       <td><input type=\"checkbox\" name=\"" + PARAM_ANTIGEN_SERIES_INCLUDE + i
          + "\" value=\"true\"></td>");
      out.println("       <td>" + antigenSeries.getSeriesName() + "</td>");
      out.println("       <td>" + antigenSeries.getAntigen() + "</td>");
      out.println("       <td>" + (antigenSeries.getVaccineGroup() == null ? ""
          : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
      i++;
    }
    out.println("   </table>");
  }

  public void printPost(PrintWriter out) throws Exception {
    out.println("   <h2>4.3 Create Relevant Patient Series</h2>");
    out.println(
        "   <p>An antigen series is one way to reach perceived immunity against a disease. An antigen series can be thought "
            + "of as a \"path to immunity\" and is described in relative terms. In many cases, a single antigen may have more "
            + "than one successful path to immunity and as such may have more than one antigen series. Antigen series are "
            + "defined through Supporting Data spreadsheets defined in Chapter 3. Some series, classified here as \"Standard\" "
            + "series, are based on recommendations for all patients based on age. Other series, classified as \"Risk\" series, "
            + "are based on recommendations for patients with specific characteristics or underlying conditions which put them "
            + "at increased risk. Finally, some series are strictly for the purpose of \"Evaluation Only\" and should not be "
            + "recommended for completion, but if already complete can be used as proof of series completion.</p>");

    out.println("   <h2>Patient Series Included</h2>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Series Name</th>");
    out.println("       <th>Target Disease</th>");
    out.println("       <th>Vaccine Group</th>");
    out.println("     </tr>");
    for (PatientSeries patientSeries : dataModel.getPatientSeriesList()) {
      AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
      out.println("     <tr>");
      out.println("       <td>" + antigenSeries.getSeriesName() + "</td>");
      out.println("       <td>" + antigenSeries.getAntigen() + "</td>");
      out.println("       <td>" + (antigenSeries.getVaccineGroup() == null ? ""
          : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
  }

}
