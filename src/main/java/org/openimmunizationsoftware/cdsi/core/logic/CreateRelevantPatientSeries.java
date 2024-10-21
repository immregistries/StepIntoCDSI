package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;

public class CreateRelevantPatientSeries extends LogicStep {

  public CreateRelevantPatientSeries(DataModel dataModel) {
    super(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {

    if (dataModel.getAntigenSelectedList() == null) {
      List<Antigen> antigenSelectedList = new ArrayList<Antigen>();
      dataModel.setAntigenSelectedList(antigenSelectedList);
      int i = 1;
      boolean foundAtLeastOne = false;
      for (Antigen antigen : dataModel.getAntigenList()) {
        if (dataModel.getRequest().getParameter(PARAM_ANTIGEN_INCLUDE + i) != null) {
          foundAtLeastOne = true;
          antigenSelectedList.add(antigen);
        }
        i++;
      }
      // If none are checked then check them all
      if (!foundAtLeastOne) {
        for (Antigen antigen : dataModel.getAntigenList()) {
          antigenSelectedList.add(antigen);
        }
      }
      dataModel.setAntigenSelectedPos(0);
    }
    else { 
      dataModel.incAntigenAdministeredRecordPos();
      dataModel.incAntigenSelectedPos();
    }

    if (dataModel.getAntigenSelectedPos() < dataModel.getAntigenSelectedList().size()) {
      return LogicStepFactory.createLogicStep(LogicStepType.SELECT_RELEVANT_PATIENT_SERIES, dataModel);
    }
    else {
      return LogicStepFactory.createLogicStep(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, dataModel);
    }
    
  }

  public void printPre(PrintWriter out) throws Exception {
    out.println("   <h2>4.3 Create Relevant Patient Series</h2>");
    out.println("   <h2>Antigen Series</h2>");
    out.println("   <table>");
    if (dataModel.getAntigenSelectedList() == null) {
      out.println("     <tr>");
      out.println("       <th>Include</th>");
      out.println("       <th>Antigen</th>");
      out.println("     </tr>");
      int i = 1;
      for (Antigen antigen : dataModel.getAntigenList()) {
        out.println("     <tr>");
        out.println("       <td><input type=\"checkbox\" name=\"" + PARAM_ANTIGEN_INCLUDE + i
            + "\" value=\"true\"></td>");
        out.println("       <td>" + antigen.getName() + "</td>");
        out.println("     </tr>");
        i++;
      }
    }
    else {
      out.println("     <tr>");
      out.println("       <th>Antigen</th>");
      out.println("     </tr>");
      for (Antigen antigen : dataModel.getAntigenSelectedList()) {
        out.println("     <tr>");
        out.println("       <td>" + antigen.getName() + "</td>");
        out.println("     </tr>");
      }
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
