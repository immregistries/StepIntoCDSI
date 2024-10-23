package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.RecurringDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;


public class EvaluateAndForecastAllPatientSeries extends LogicStep {
  public EvaluateAndForecastAllPatientSeries(DataModel dataModel) {
    super(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() {
    //get current patient series
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
    
    //Is there another relevant patientSeries to process?
    if (patientSeriesSelected == null) {
      return LogicStepFactory.createLogicStep(LogicStepType.SELECT_BEST_PATIENT_SERIES, dataModel);
    } 
    else {
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
    }

    LogicStepType nextLogicStep;

    dataModel.incAntigenAdministeredRecordPos();
    PatientSeries patientSeries = dataModel.getPatientSeries();

    //log skips
    while (dataModel.getAntigenAdministeredRecordPos() < dataModel
        .getAntigenAdministeredRecordList().size()) {
      AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecordList()
          .get(dataModel.getAntigenAdministeredRecordPos());
      if (aar.getAntigen().equals(patientSeries.getTrackedAntigenSeries().getTargetDisease())) {
        break;
      } else {
        dataModel.incAntigenAdministeredRecordPos();
        log("   Skipping " + aar + " for antigen " + aar.getAntigen() + "rrrrr"
            + patientSeries.getTrackedAntigenSeries().getTargetDisease());
      }
    }

    //choose whether chapter 6 or chapter 7 is next
    if (dataModel.getAntigenAdministeredRecordPos() < dataModel.getAntigenAdministeredRecordList()
        .size()) {
      if (dataModel.getAntigenAdministeredRecordPos() == 0) {
        log("   Looking at first dose administered");
      }
      dataModel.setAntigenAdministeredRecord(dataModel.getAntigenAdministeredRecordList()
          .get(dataModel.getAntigenAdministeredRecordPos()));
      if (gotoNextTargetDose()) {
        nextLogicStep = LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION;
      } else {
        nextLogicStep = LogicStepType.FORECAST_DATES_AND_REASONS;
      }
    } else {
      gotoNextTargetDose();
      nextLogicStep = LogicStepType.FORECAST_DATES_AND_REASONS;
    }

    return LogicStepFactory.createLogicStep(nextLogicStep, dataModel);
  
  }
  

  private boolean gotoNextTargetDose() {
    if (dataModel.getTargetDose() == null) {
      log(" + Getting first target dose");
      dataModel.incTargetDoseListPos();
      dataModel.setTargetDose(dataModel.getTargetDoseList().get(dataModel.getTargetDoseListPos()));
      return true;
    } else {
      if (dataModel.getTargetDose().getSatisfiedByVaccineDoseAdministered() != null) {
        log(" + Previous target dose was satisifed, getting next target dose");
        RecurringDose recurringdose =
            dataModel.getTargetDose().getTrackedSeriesDose().getRecurringDose();
        if (recurringdose != null && recurringdose.getValue() == YesNo.YES) {
          // Create another target dose
          TargetDose targetDoseNext = new TargetDose(dataModel.getTargetDose());
          dataModel.getTargetDoseList().add(targetDoseNext);
        }
        dataModel.incTargetDoseListPos();
        if (dataModel.getTargetDoseListPos() < dataModel.getTargetDoseList().size()) {
          dataModel
              .setTargetDose(dataModel.getTargetDoseList().get(dataModel.getTargetDoseListPos()));
          return true;
        } else {
          markRestAsExtraneous();
          return false;
        }
      } else {

        log(" + Previous target dose was NOT satisifed, staying on this target dose");
        return true;
      }
    }
  }

  private AntigenAdministeredRecord findNextAntigenAdministeredRecord(
      AntigenAdministeredRecord aar) {
    AntigenAdministeredRecord aarNext = null;
    boolean found = false;
    for (AntigenAdministeredRecord antigenAdministeredRecord : dataModel
        .getAntigenAdministeredRecordList()) {
      if (found) {
        aarNext = antigenAdministeredRecord;
        break;
      } else if (antigenAdministeredRecord == aar) {
        found = true;
      }
    }
    return aarNext;
  }

  private void markRestAsExtraneous() {
    for (int i = dataModel.getAntigenAdministeredRecordPos() + 1; i < dataModel
        .getAntigenAdministeredRecordList().size(); dataModel.incAntigenAdministeredRecordPos()) {
      dataModel.getAntigenAdministeredRecordList().get(i).getEvaluation()
          .setEvaluationStatus(EvaluationStatus.EXTRANEOUS);
    }
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println("<h1>8.4 Evaluate and Forecast all Patient Series</h1>");

    out.println(
        "<p>This step is the core of the business logic and decision points many people think of when describing evaluation and forecasting. In the Logic Specification, this step contains all of the clinical business rules and decision logic in the form of business rules and decision tables.</p>");
    out.println(
        "<p>At the end of this step, each patient series will have an evaluated history and a forecast.</p>");

  }

  @Override
  public void printPost(PrintWriter out) {
    out.println("<h1>8.4 Evaluate and Forecast all Patient Series</h1>");

    out.println(
        "<p>This step is the core of the business logic and decision points many people think of when describing evaluation and forecasting. In the Logic Specification, this step contains all of the clinical business rules and decision logic in the form of business rules and decision tables.</p>");
    out.println(
        "<p>At the end of this step, each patient series will have an evaluated history and a forecast.</p>");


  }

}
