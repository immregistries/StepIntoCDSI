package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;

public class CreateRelevantPatientSeries extends LogicStep {

  public CreateRelevantPatientSeries(DataModel dataModel) {
    super(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {

    if (dataModel.getAntigenSelectedList() == null) {
      log("Antigen selected list is null, creating");
      List<Antigen> antigenSelectedList = new ArrayList<Antigen>();
      dataModel.setAntigenSelectedList(antigenSelectedList);
      int i = 1;
      boolean foundAtLeastOne = false;
      for (Antigen antigen : dataModel.getAntigenList()) {
        if (dataModel.getRequest().getParameter(PARAM_ANTIGEN_INCLUDE + i) != null) {
          log("  + antigen indicated " + antigen.getName());
          foundAtLeastOne = true;
          antigenSelectedList.add(antigen);
        } else {
          log("  - antigen not indicated " + antigen.getName());
        }
        i++;
      }
      if (foundAtLeastOne) {
        log("Found at least one antigen selected, only forecasting for selected antigens");
      } else {
        log("No antigens selected, forecasting for all antigens");
        for (Antigen antigen : dataModel.getAntigenList()) {
          antigenSelectedList.add(antigen);
        }
      }
      log("Forecasting for " + antigenSelectedList.size() + " antigens");
      dataModel.setAntigenSelectedPos(0);
    } else {
      log("Antigen selected list already exists, incrementing");
      dataModel.incAntigenAdministeredRecordPos();
      dataModel.incAntigenSelectedPos();
    }

    if (dataModel.getAntigenSelectedPos() < dataModel.getAntigenSelectedList().size()) {
      log("Selecting antigen series for this antigen: "
          + dataModel.getAntigenSelectedList().get(dataModel.getAntigenSelectedPos()).getName());
      return LogicStepFactory.createLogicStep(LogicStepType.SELECT_RELEVANT_PATIENT_SERIES, dataModel);
    } else {
      log("Done, now evaluating and forecasting all patient series");
      return LogicStepFactory.createLogicStep(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, dataModel);
    }

  }

  public void printPre(PrintWriter out) throws Exception {
    out.println("   <h2>4.3 Create Relevant Patient Series</h2>");
    out.println("   <h2>Antigen Series</h2>");
    out.println("   <table>");
    if (dataModel.getAntigenSelectedList() == null) {
      Set<Antigen> antigenSet = new HashSet<Antigen>();
      for (VaccineDoseAdministered vda : dataModel.getImmunizationHistory()
          .getVaccineDoseAdministeredList()) {
        for (Antigen antigen : vda.getVaccine().getVaccineType().getAntigenList()) {
          antigenSet.add(antigen);
        }
      }
      out.println("     <tr>");
      out.println("       <th>Include</th>");
      out.println("       <th>Antigen</th>");
      out.println("     </tr>");
      int i = 1;
      for (Antigen antigen : dataModel.getAntigenList()) {
        String checked = "";
        if (antigenSet.contains(antigen)) {
          checked = " checked";
        }
        out.println("     <tr>");
        out.println("       <td><input type=\"checkbox\" name=\"" + PARAM_ANTIGEN_INCLUDE + i
            + "\" value=\"true\"" + checked + "></td>");
        out.println("       <td>" + antigen.getName() + "</td>");
        out.println("     </tr>");
        i++;
      }
    } else {
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
    out.println("   <h1>4.3 Create Patient Series</h2>");
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
