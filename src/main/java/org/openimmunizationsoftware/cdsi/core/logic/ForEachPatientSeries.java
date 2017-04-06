package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;

public class ForEachPatientSeries extends LogicStep {

  public ForEachPatientSeries(DataModel dataModel) {
    super(LogicStepType.FOR_EACH_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() {
    PatientSeries patientSeriesSelected = null;
    if (dataModel.getPatientSeriesList().size() > 0) {
      if (dataModel.getPatientSeries() == null) {
        patientSeriesSelected = dataModel.getPatientSeriesList().get(0);
      } else {
        boolean found = false;
        for (PatientSeries patientSeries : dataModel.getPatientSeriesList()) {
          if (found) {
            patientSeriesSelected = patientSeries;
            break;
          }
          if (dataModel.getPatientSeries() == patientSeries) {
            found = true;
          }
        }
      }
    }
    if (patientSeriesSelected == null) {
      return LogicStepFactory.createLogicStep(LogicStepType.SELECT_BEST_PATIENT_SERIES, dataModel);
    } else {
      dataModel.setPatientSeries(patientSeriesSelected);
      dataModel.setTargetDoseList(new ArrayList<TargetDose>());
      patientSeriesSelected.setTargetDoseList(dataModel.getTargetDoseList());
      for (SeriesDose seriesDose : dataModel.getPatientSeries().getTrackedAntigenSeries()
          .getSeriesDoseList()) {
        TargetDose targetDose = new TargetDose(seriesDose);
        dataModel.getTargetDoseList().add(targetDose);
      }
      dataModel.setTargetDose(null);
      dataModel.setTargetDoseListPos(-1);
      dataModel.setAntigenAdministeredRecordPos(-1);
      return LogicStepFactory.createLogicStep(LogicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED,
          dataModel);
    }
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println("<h1>for each Patient Series</h1>");
    printTable(out);

  }

  private void printTable(PrintWriter out) {
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Status</th>");
    out.println("       <th>Series Name</th>");
    out.println("       <th>Target Disease</th>");
    out.println("       <th>Vaccine Group</th>");
    out.println("     </tr>");
    boolean found = false;
    for (PatientSeries patientSeries : dataModel.getPatientSeriesList()) {
      AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
      out.println("     <tr>");
      if (!found) {
        if ((dataModel.getPatientSeries() == null
            || dataModel.getPatientSeries() == patientSeries)) {
          found = true;
          out.println("       <td>Selected</td>");
        } else {
          out.println("       <td>Completed</td>");
        }
      } else {
        out.println("       <td></td>");
      }
      out.println("       <td>" + antigenSeries.getSeriesName() + "</td>");
      out.println("       <td>" + antigenSeries.getTargetDisease() + "</td>");
      out.println("       <td>" + (antigenSeries.getVaccineGroup() == null ? ""
          : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
  }

  @Override
  public void printPost(PrintWriter out) {
    out.println("<h1>for each Patient Series</h1>");
    printTable(out);
  }

}
