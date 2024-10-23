package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;

public class SelectRelevantPatientSeries extends LogicStep {

  public SelectRelevantPatientSeries(DataModel dataModel) {
    super(LogicStepType.SELECT_RELEVANT_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {

    Antigen antigen = dataModel.getAntigenSelectedList().get(dataModel.getAntigenSelectedPos());

    int i = 1;
    for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
      if (!antigenSeries.getTargetDisease().equals(antigen)) {
        continue;
      }
      if (dataModel.getRequest().getParameter(PARAM_ANTIGEN_SERIES_INCLUDE + i) != null) {
        // Logic table goes here to see if Antigen should be added to PatientSeries
        PatientSeries patientSeries = new PatientSeries(antigenSeries);
        dataModel.getPatientSeriesList().add(patientSeries);
      }
      i++;
    }

    return LogicStepFactory.createLogicStep(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES, dataModel);
  }

  public void printPre(PrintWriter out) throws Exception {
    out.println("   <h2>5.1 Create Relevant Patient Series</h2>");
    out.println("   <h2>Antigen Series</h2>");
    out.println("     <p>Looking at antigen " + (dataModel.getAntigenSelectedPos() + 1) 
      + " out of " + dataModel.getAntigenSelectedList().size() + " antigens selected. </p>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Include</th>");
    out.println("       <th>Series Name</th>");
    out.println("       <th>Target Disease</th>");
    out.println("       <th>Vaccine Group</th>");
    out.println("     </tr>");
    Antigen antigen = dataModel.getAntigenSelectedList().get(dataModel.getAntigenSelectedPos());
    int i = 1;
    for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
      if (!antigenSeries.getTargetDisease().equals(antigen)) {
        continue;
      }
      out.println("     <tr>");
      out.println("       <td><input type=\"checkbox\" name=\"" + PARAM_ANTIGEN_SERIES_INCLUDE + i
          + "\" value=\"true\" checked></td>");
      out.println("       <td>" + antigenSeries.getSeriesName() + "</td>");
      out.println("       <td>" + antigenSeries.getTargetDisease() + "</td>");
      out.println("       <td>" + (antigenSeries.getVaccineGroup() == null ? ""
          : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
      i++;
    }
    out.println("   </table>");
  }

  public void printPost(PrintWriter out) throws Exception {
    out.println("   <h1>8.2 Create Patient Series</h2>");
    out.println(
        "   <p>An antigen series is one way to reach perceived immunity against a disease.  "
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
      out.println("       <td>" + (antigenSeries.getVaccineGroup() == null ? ""
          : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
  }

}
