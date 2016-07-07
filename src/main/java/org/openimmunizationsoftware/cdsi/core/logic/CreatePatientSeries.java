package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;

public class CreatePatientSeries extends LogicStep
{

  public CreatePatientSeries(DataModel dataModel) {
    super(LogicStepType.CREATE_PATIENT_SERIES, dataModel);
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
    out.println("   <h2>8.2 Create Patient Series</h2>");
    out.println("   <p>An antigen series is one way to reach perceived immunity against a disease.  "
        + "An antigen series can be thought of as a \"path to immunity\" and is described in "
        + "relative terms.  In many cases, a single antigen may have more than one successful "
        + "path to immunity and as such may have more than one antigen series.  Antigen "
        + "series are defined through supporting data spreadsheets defined in chapter 3.</p>");
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
      out.println(
          "       <td><input type=\"checkbox\" name=\"" + PARAM_ANTIGEN_SERIES_INCLUDE + i + "\" value=\"true\"></td>");
      out.println("       <td>" + antigenSeries.getSeriesName() + "</td>");
      out.println("       <td>" + antigenSeries.getTargetDisease() + "</td>");
      out.println("       <td>"
          + (antigenSeries.getVaccineGroup() == null ? "" : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
      i++;
    }
    out.println("   </table>");
  }

  public void printPost(PrintWriter out) throws Exception {
    out.println("   <h1>8.2 Create Patient Series</h2>");
    out.println("   <p>An antigen series is one way to reach perceived immunity against a disease.  "
        + "An antigen series can be thought of as a \"path to immunity\" and is described in "
        + "relative terms.  In many cases, a single antigen may have more than one successful "
        + "path to immunity and as such may have more than one antigen series.  Antigen "
        + "series are defined through supporting data spreadsheets defined in chapter 3.</p>");

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
      out.println("       <td>" + antigenSeries.getTargetDisease() + "</td>");
      out.println("       <td>"
          + (antigenSeries.getVaccineGroup() == null ? "" : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
  }

}
