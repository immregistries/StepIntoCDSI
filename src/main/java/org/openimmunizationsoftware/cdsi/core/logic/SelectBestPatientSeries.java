package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;

public class SelectBestPatientSeries extends LogicStep {
  public SelectBestPatientSeries(DataModel dataModel) {
    super(LogicStepType.SELECT_BEST_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() {
    if (dataModel.getBestPatientSeriesList() == null) {
      dataModel.setBestPatientSeriesList(new ArrayList<>());
    }
    dataModel.incAntigenPos();
    if (dataModel.getAntigenPos() < dataModel.getAntigenSelectedList().size()) {
      Antigen antigen = dataModel.getAntigenSelectedList().get(dataModel.getAntigenPos());
      dataModel.setAntigen(dataModel.getAntigenSelectedList().get(dataModel.getAntigenPos()));
      ArrayList<AntigenSeries> antigenSeriesSelectedList = new ArrayList<AntigenSeries>();
      for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
        if (antigenSeries.getTargetDisease().equals(antigen)) {
          antigenSeriesSelectedList.add(antigenSeries);
        }
      }
      dataModel.setAntigenSeriesSelectedList(antigenSeriesSelectedList);
      setNextLogicStepType(LogicStepType.PRE_FILTER_PATIENT_SERIES);

      List<PatientSeries> patientSeriesSelectedList = new ArrayList<PatientSeries>();
      for (PatientSeries patientSeries : dataModel.getPatientSeriesStepper().getList()) {
        if (patientSeries.getTrackedAntigenSeries().getTargetDisease().equals(antigen)) {
          patientSeriesSelectedList.add(patientSeries);
        }
      }
      dataModel.setSelectedPatientSeriesList(patientSeriesSelectedList);
    } else {
      dataModel.setAntigen(null);
      dataModel.setAntigenSeriesSelectedList(null);
      setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
    }
    dataModel.getPrioritizedPatientSeriesList().clear();

    return next();
  }

}
