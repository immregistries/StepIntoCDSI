package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.RecurringDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;

public class EvaluateAndForecastAllPatientSeries extends LogicStep {
  public EvaluateAndForecastAllPatientSeries(DataModel dataModel) {
    super(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() {
    //The first time 4.4 is run
    // - get first Target dose and first patient series
    // - if there are no AntigenAdministeredRecords, go to forecasting

    //Step 2.5: if the target dose has been skipped:
    // - if we are in evaluation, get the next target dose and go to evaluation
    // - if we are in forecasting, get the next target dose and go to forecasting

    //If forecasting has finished, get the next patient series
    // - if there are no more patient series, go to 4.5: Select Best Patient Series
    // - if there is another patient series, run evaluation

    //Run evaluation

    //To run evaluation, get the next AAR, then go to evaluation
    // - if the resulting target dose is satisfied, get the next AAR, then get the next target dose and rerun evaluation
    // - if the resulting target dose is not satisfied, get next AntigenAdministeredRecord

    //To get the next AntigenAdministeredRecord:
    // - if there are no more AARs, go to forecasting (no more AARs)
    // - if there is another AAR, select it and run evaluation:

    //To get the next target dose:
    // - if there are no more target doses, check if the target dose is a recurring dose
    // - - if the target dose is a recurring dose, add and then increment to a duplicate of it
    // - - if the target dose is not a recurring dose, evaluation ends and go to forecasting (no more target dose), all remaining AARs are set to EXTRANEOUS
    
    if(dataModel.getPatientSeriesPos() >= dataModel.getPatientSeriesList().size() && dataModel.getForecastingForPatientSeries() != null && dataModel.getForecastingForPatientSeries().equals(dataModel.getPatientSeries())) {
      return LogicStepFactory.createLogicStep(LogicStepType.SELECT_BEST_PATIENT_SERIES, dataModel);
    }

    //if the target dose has been skipped
    if(dataModel.getTargetDose() != null) {
      if (dataModel.getTargetDose().getTargetDoseStatus() == TargetDoseStatus.SKIPPED) {
        //if patient series is being forecasted
        if(dataModel.getForecastingForPatientSeries() != null && dataModel.getForecastingForPatientSeries().equals(dataModel.getPatientSeries())) {
          if(canGotoNextTargetDose()) {
            return LogicStepFactory.createLogicStep(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, dataModel);
          } else {
            setNextPatientSeries();
            return LogicStepFactory.createLogicStep(LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION, dataModel);
          }
        } else {
          if(!canGotoNextTargetDose()) {
            return LogicStepFactory.createLogicStep(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST, dataModel);
          }
          return LogicStepFactory.createLogicStep(LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION, dataModel);
        }
      }
    }

    // get current patient series
    boolean patientSeriesNeedsSetup = true;
    PatientSeries patientSeriesSelected = null;
    if (dataModel.getPatientSeriesList().size() == 0) {
      log("The patient series list is empty, no patient series to process");
    } else {
      if (dataModel.getPatientSeries() == null) {
        // when we first start, we haven't done anything yet.

        patientSeriesSelected = dataModel.getPatientSeriesList().get(0);
        log("Setting patient series, as it has not yet been set.");
      } else {
        // We may need to stay on this patient series, need to check if we are done
        if (dataModel.getSelectedAntigenAdministeredRecordPos() < dataModel.getSelectedAntigenAdministeredRecordList()
            .size() && stillEvaluating()) {
          log("Current patient series is not yet set, staying on this patient series");
          patientSeriesSelected = dataModel.getPatientSeries();
          patientSeriesNeedsSetup = false;
        } else {
          log("Looking in patient series list for the next patient series to work on (list size = "
              + dataModel.getPatientSeriesList().size() + ")");
          boolean foundCurrent = false;
          for (PatientSeries patientSeries : dataModel.getPatientSeriesList()) {
            if (foundCurrent) {
              log("Found the next patient series to work on");
              patientSeriesSelected = patientSeries;
              break;
            }
            if (dataModel.getPatientSeries() == patientSeries) {
              foundCurrent = true;
            }
          }
        }

      }
    }

    // Is there another relevant patientSeries to process?
    log("Checking for another patient series to process");
    if (patientSeriesSelected == null) {
      log("No more patient series to process");
      dataModel.setPatientSeries(null);
      dataModel.setTargetDose(null);
      dataModel.setPreviousTargetDose(null);
      dataModel.setTargetDoseList(null);
      dataModel.setTargetDoseListPos(-1);
      dataModel.setAntigen(null);
      dataModel.setAntigenAdministeredRecord(null);
      dataModel.setSelectedAntigenAdministeredRecordList(null);
      if (dataModel.getPatientSeriesList() == null) {
        log("Patient series list is null");
      } else {
        log("Patient series list size = " + dataModel.getPatientSeriesList().size());
      }
      return LogicStepFactory.createLogicStep(LogicStepType.SELECT_BEST_PATIENT_SERIES, dataModel);
    } else if (patientSeriesNeedsSetup) {
      dataModel.setPatientSeries(patientSeriesSelected);
      dataModel.setTargetDoseList(new ArrayList<TargetDose>());
      patientSeriesSelected.setTargetDoseList(dataModel.getTargetDoseList());
      dataModel.setAntigen(patientSeriesSelected.getTrackedAntigenSeries().getTargetDisease());
      for (SeriesDose seriesDose : dataModel.getPatientSeries().getTrackedAntigenSeries()
          .getSeriesDoseList()) {
        TargetDose targetDose = new TargetDose(seriesDose);
        dataModel.getTargetDoseList().add(targetDose);
      }
      dataModel.setTargetDose(null);
      dataModel.setPreviousTargetDose(null);
      dataModel.setTargetDoseListPos(-1);
      setupSelectedAntigenAdministeredRecordList();
    }

    LogicStepType nextLogicStep;

    List<AntigenAdministeredRecord> selectedAarList = dataModel.getSelectedAntigenAdministeredRecordList();
    if (dataModel.getSelectedAntigenAdministeredRecordList() == null) {
      setupSelectedAntigenAdministeredRecordList();
    }

    if (dataModel.getTargetDose() == null
        || dataModel.getTargetDose().getTargetDoseStatus() != TargetDoseStatus.SKIPPED) {
      dataModel.incSelectedAntigenAdministeredRecordPos();
    }

    if (dataModel.getSelectedAntigenAdministeredRecordPos() < selectedAarList.size()) {
      if (dataModel.getSelectedAntigenAdministeredRecordPos() == 0) {
        log("   Looking at first dose administered");
      } else {
        dataModel.setPreviousAntigenAdministeredRecord(
            selectedAarList.get(dataModel.getSelectedAntigenAdministeredRecordPos() - 1));
      }
      dataModel.setAntigenAdministeredRecord(selectedAarList.get(dataModel.getSelectedAntigenAdministeredRecordPos()));

      // choose whether chapter 6 or chapter 7 is next
      if (OLDgotoNextTargetDose()) {
        nextLogicStep = LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION;
      } else {
        log("Cannot go to next target dose in evaluation, now forecasting");
        run7();
        nextLogicStep = LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST;
      }
    } else {
      log("AAR selected pos = " + dataModel.getSelectedAntigenAdministeredRecordPos());
      log("AAR list length = " + selectedAarList.size());
      if (dataModel.getSelectedAntigenAdministeredRecordPos() > 0) {
        dataModel.setPreviousAntigenAdministeredRecord(
            selectedAarList.get(dataModel.getSelectedAntigenAdministeredRecordPos() - 1));
      }
      OLDgotoNextTargetDose();
      run7();
      nextLogicStep = LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST;
    }

    return LogicStepFactory.createLogicStep(nextLogicStep, dataModel);
  }

  private boolean stillEvaluating() {
    return dataModel.getPatientSeries().getPatientSeriesStatus() == null;
  }

  private boolean canGotoNextTargetDose() {
    log("Getting next target dose");
    dataModel.incTargetDoseListPos();
    //if there are no more target doses
    if (dataModel.getTargetDoseListPos() >= dataModel.getTargetDoseList().size()) {
      //if the current target dose is a recurring dose, add a duplicate of it to the target dose list
      RecurringDose recurringDose = dataModel.getTargetDose().getTrackedSeriesDose().getRecurringDose();
      if (recurringDose != null) {
        if(recurringDose.getValue() == YesNo.YES) {
          log("target dose is a recurring dose");
          TargetDose targetDoseNext = new TargetDose(dataModel.getTargetDose());
          dataModel.getTargetDoseList().add(targetDoseNext);
        } else {
          //evaluation ends and go to forecasting (no more target dose), all remaining AARs are set to EXTRANEOUS
          log("target dose is not a recurring dose, going to forecasting and setting all remaining AARs to EXTRANEOUS");
          dataModel.setForecastingForPatientSeries(dataModel.getPatientSeries());
          return false;
        }
      }
    } else {
      dataModel.setPreviousTargetDose(dataModel.getTargetDose());
      dataModel.setTargetDose(dataModel.getTargetDoseList().get(dataModel.getTargetDoseListPos()));
    }
    return true;
  }

  private boolean setNextPatientSeries() {
    log("Looking in patient series list for the next patient series to work on (list size = "
      + dataModel.getPatientSeriesList().size() + ")");
    boolean foundCurrent = false;
    for (PatientSeries patientSeries : dataModel.getPatientSeriesList()) {
      if (foundCurrent) {
        log("Found the next patient series to work on");
        dataModel.setPatientSeries(patientSeries);
        dataModel.incPatientSeriesPos();
        if(dataModel.getForecastingForPatientSeries() != null && !dataModel.getForecastingForPatientSeries().equals(dataModel.getPatientSeries())) {
          setupCurrentPatientSeriesAndTargetDoseList();
        }
        return true;
      }
      if (dataModel.getPatientSeries() == patientSeries) {
        foundCurrent = true;
      }
    }
    log("Did not find the next patient series to work on");
    log("No more patient series to process");
    dataModel.setPatientSeries(null);
    dataModel.setTargetDose(null);
    dataModel.setPreviousTargetDose(null);
    dataModel.setTargetDoseList(null);
    dataModel.setTargetDoseListPos(-1);
    dataModel.setAntigen(null);
    dataModel.setAntigenAdministeredRecord(null);
    dataModel.setSelectedAntigenAdministeredRecordList(null);
    if (dataModel.getPatientSeriesList() == null) {
      log("Patient series list is null");
    } else {
      log("Patient series list size = " + dataModel.getPatientSeriesList().size());
    }
    dataModel.setForecastingForPatientSeries(null);
    return false;
  }

  private void setupCurrentPatientSeriesAndTargetDoseList() {
    dataModel.setTargetDoseList(new ArrayList<TargetDose>());
    dataModel.getPatientSeries().setTargetDoseList(dataModel.getTargetDoseList());
    dataModel.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
    for (SeriesDose seriesDose : dataModel.getPatientSeries().getTrackedAntigenSeries()
        .getSeriesDoseList()) {
      TargetDose targetDose = new TargetDose(seriesDose);
      dataModel.getTargetDoseList().add(targetDose);
    }
    dataModel.setTargetDose(null);
    dataModel.setPreviousTargetDose(null);
    dataModel.setTargetDoseListPos(-1);
    setupSelectedAntigenAdministeredRecordList();
  }

  private void setupSelectedAntigenAdministeredRecordList() {
    List<AntigenAdministeredRecord> selectedAntigenAdministeredRecordList = new ArrayList<AntigenAdministeredRecord>();
    dataModel.setSelectedAntigenAdministeredRecordList(selectedAntigenAdministeredRecordList);
    for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {
      if (aar.getAntigen() == dataModel.getAntigen()) {
        selectedAntigenAdministeredRecordList.add(aar);
      }
    }
    dataModel.setSelectedAntigenAdministeredRecordPos(-1);
    dataModel.setPreviousAntigenAdministeredRecord(null);
  }

  private boolean OLDgotoNextTargetDose() {
    if (dataModel.getTargetDose() == null) {
      log(" + Getting first target dose");
      dataModel.incTargetDoseListPos();
      dataModel.setPreviousTargetDose(dataModel.getTargetDose());
      dataModel.setTargetDose(dataModel.getTargetDoseList().get(dataModel.getTargetDoseListPos()));
      return true;
    } else {
      log("  + Target dose is not null");
      log("  + Target dose list pos = " + dataModel.getTargetDoseListPos());
      log("  + Target dose list size = " + dataModel.getTargetDoseList().size());
      
      if (dataModel.getTargetDose().getTargetDoseStatus() == TargetDoseStatus.SKIPPED) {
        log(" + Target dose was skipped, getting next target dose");
        dataModel.incTargetDoseListPos();
        //if the current target dose is a recurring dose, add a duplicate of it to the target dose list
        RecurringDose recurringDose = dataModel.getTargetDose().getTrackedSeriesDose().getRecurringDose();
        if (recurringDose != null && recurringDose.getValue() == YesNo.YES) {
          TargetDose targetDoseNext = new TargetDose(dataModel.getTargetDose());
          dataModel.getTargetDoseList().add(targetDoseNext);
        }
        dataModel.setPreviousTargetDose(dataModel.getTargetDose());
        
      } else if (dataModel.getTargetDose().getSatisfiedByVaccineDoseAdministered() != null) {
        log(" + Previous target dose was satisfied, getting next target dose");
        dataModel.incTargetDoseListPos();
        //if the current target dose is a recurring dose, add a duplicate of it to the target dose list
        RecurringDose recurringDose = dataModel.getTargetDose().getTrackedSeriesDose().getRecurringDose();
        if (recurringDose != null && recurringDose.getValue() == YesNo.YES) {
          TargetDose targetDoseNext = new TargetDose(dataModel.getTargetDose());
          dataModel.getTargetDoseList().add(targetDoseNext);
        }
        log("  + New target dose list pos = " + dataModel.getTargetDoseListPos());
        dataModel.setPreviousTargetDose(dataModel.getTargetDose());
      }

      if(dataModel.getTargetDose().getSatisfiedByVaccineDoseAdministered() != null || dataModel.getTargetDose().getTargetDoseStatus() == TargetDoseStatus.SKIPPED) {
        log(" + target dose was skipped or satisfied");
        if (dataModel.getTargetDoseListPos() < dataModel.getTargetDoseList().size()) {
          log(" + Setting next target dose");
          dataModel
              .setTargetDose(dataModel.getTargetDoseList().get(dataModel.getTargetDoseListPos()));
          return true;
        } else {
          log(" + No more target doses, marking rest as extraneous and setting patient series status to 'NOT COMPLETE'");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
          markRestAsExtraneous();
          return false;
        }
      } else {
        log(" + Target dose was NOT skipped or satisfied, staying on this target dose");
          return true;
      }

    }
  }

  private void run7() {
    Forecast forecast = new Forecast();
    forecast.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
    forecast.setTargetDose(dataModel.getTargetDose());
    dataModel.setForecast(forecast);
    dataModel.getPatientSeries().setForecast(forecast);
    dataModel.setForecastingForPatientSeries(dataModel.getPatientSeries());
  }

  private void markRestAsExtraneous() {
    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    for (int i = dataModel.getSelectedAntigenAdministeredRecordPos() + 1; i < dataModel
        .getSelectedAntigenAdministeredRecordList()
        .size(); i++) {
      dataModel.setAntigenAdministeredRecord(dataModel.getSelectedAntigenAdministeredRecordList().get(i));
      TargetDose targetDose = new TargetDose(seriesDose);
      dataModel.getTargetDoseList().add(targetDose);
      dataModel.setTargetDose(targetDose);
      dataModel.setEvaluationForCurrentTargetDose(EvaluationStatus.EXTRANEOUS, null);
      dataModel.incSelectedAntigenAdministeredRecordPos();
    }
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    out.println(
        "<p>This step is the core of the business logic and decision points many people think of when describing evaluation and forecasting. In the Logic Specification, this step contains all of the clinical business rules and decision logic in the form of business rules and decision tables.</p>");
    out.println(
        "<p>At the end of this step, each patient series will have an evaluated history and a forecast.</p>");
  }

  @Override
  public void printPost(PrintWriter out) {
    out.println(
        "<p>This step is the core of the business logic and decision points many people think of when describing evaluation and forecasting. In the Logic Specification, this step contains all of the clinical business rules and decision logic in the form of business rules and decision tables.</p>");
    out.println(
        "<p>At the end of this step, each patient series will have an evaluated history and a forecast.</p>");
    out.println("<h2>Selected Patient Series</h2>");
    if (dataModel.getPatientSeriesList() == null) {
      out.println("<p>No patient series to process</p>");
    } else {
      out.println("<p>Patient series count: " + dataModel.getPatientSeriesList().size() + "</p>");
      for (PatientSeries patientSeries : dataModel.getPatientSeriesList()) {
        out.println("<p>" + patientSeries.getTrackedAntigenSeries().getSeriesName() + "</p>");
      }
    }
  }
}
