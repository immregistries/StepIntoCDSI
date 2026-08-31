package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;

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
      boolean foundAtLeastOne = false;

      // Check if antigen label filter has been specified by the caller (e.g. ForecastServlet)
      List<String> antigenLabelFilterList = dataModel.getAntigenLabelFilterList();
      if (antigenLabelFilterList != null && !antigenLabelFilterList.isEmpty()) {
        log("Antigen label filter specified, filtering antigens by label");
        for (Antigen antigen : dataModel.getAntigenList()) {
          if (antigenLabelFilterList.contains(antigen.getName())) {
            log("  + antigen matched by label: " + antigen.getName());
            foundAtLeastOne = true;
            antigenSelectedList.add(antigen);
          }
        }
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

}
