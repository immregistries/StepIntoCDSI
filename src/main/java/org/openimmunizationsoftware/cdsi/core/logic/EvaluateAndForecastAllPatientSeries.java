package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.Neighborhood;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.RecurringDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.Stepper;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;

public class EvaluateAndForecastAllPatientSeries extends LogicStep {
  public EvaluateAndForecastAllPatientSeries(DataModel dataModel) {
    super(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() {
    // The first time 4.4 is run
    // - get first Target dose and first patient series
    // - if there are no AntigenAdministeredRecords, go to forecasting

    // Step 2.5: if the target dose has been skipped:
    // - if we are in evaluation, get the next target dose and go to evaluation
    // - if we are in forecasting, get the next target dose and go to forecasting

    // If forecasting has finished, get the next patient series
    // - if there are no more patient series, go to 4.5: Select Best Patient Series
    // - if there is another patient series, run evaluation

    // Run evaluation

    // To run evaluation, get the next AAR, then go to evaluation
    // - if the resulting target dose is satisfied, get the next AAR, then get the
    // next target dose and rerun evaluation
    // - if the resulting target dose is not satisfied, get next
    // AntigenAdministeredRecord

    // To get the next AntigenAdministeredRecord:
    // - if there are no more AARs, go to forecasting (no more AARs)
    // - if there is another AAR, select it and run evaluation:

    // To get the next target dose:
    // - if there are no more target doses, check if the target dose is a recurring
    // dose
    // - - if the target dose is a recurring dose, add and then increment to a
    // duplicate of it
    // - - if the target dose is not a recurring dose, evaluation ends and go to
    // forecasting (no more target dose), all remaining AARs are set to EXTRANEOUS

    // Different starting inputs
    // ** Starting for the very first time
    // ** Starting a new patient series
    // ** In the middle of evaluating
    // ** In the middle of forecasting

    // Evaluation loop on TargetDose List
    //

    final LogicStepType EVALUATE = LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION;
    final LogicStepType FORECAST = LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST;
    final LogicStepType SELECT_BEST = LogicStepType.SELECT_BEST_PATIENT_SERIES;

    // check to see if Patient Series Stepper is setup
    Stepper<PatientSeries> patientSeriesStepper = dataModel.getPatientSeriesStepper();

    // Very first time
    if (!patientSeriesStepper.isStarted()) {
      patientSeriesStepper.increment();
    }
    // very last time, won't actually run (just in case)
    if (!patientSeriesStepper.hasCurrent()) {
      nullOutDatafields();
      return LogicStepFactory.createLogicStep(SELECT_BEST, dataModel);
    }

    // Three possible outcomes
    // LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION;
    // LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST;
    // LogicStepType.SELECT_BEST_PATIENT_SERIES;

    if (dataModel.isNeighborhoodForecast()) {
      if (dataModel.getTargetDose().getTargetDoseStatus() == TargetDoseStatus.SKIPPED) {
        if (moveToNextTargetDoseIfAvailable()) {
          return LogicStepFactory.createLogicStep(FORECAST, dataModel);
        }
      } else {
        setNextPatientSeries();
        dataModel.setNeighborhood(Neighborhood.EVALUATE);
      }
    }

    if (dataModel.isNeighborhoodSetup()) {
      setNextPatientSeries();
      dataModel.setNeighborhood(Neighborhood.EVALUATE);
    }

    if (dataModel.isNeighborhoodEvaluate() && patientSeriesStepper.hasCurrent()) {
      boolean stillHaveTargetDoses = true;
      switch (dataModel.getTargetDose().getTargetDoseStatus()) {
        case SKIPPED:
          stillHaveTargetDoses = moveToNextTargetDoseIfAvailable();
          break;
        case NOT_SATISFIED:
          // stay on the same target dose
          dataModel.incSelectedAntigenAdministeredRecordPos();
          break;
        case SATISFIED:
        case SUBSTITUTED:
        case UNNECESSARY:
          dataModel.incSelectedAntigenAdministeredRecordPos();
          stillHaveTargetDoses = moveToNextTargetDoseIfAvailable();
          break;
      }

      boolean stillHaveAARs = dataModel.getSelectedAntigenAdministeredRecordPos() <= dataModel
          .getSelectedAntigenAdministeredRecordList().size();

      if (stillHaveAARs && stillHaveTargetDoses) {
        return LogicStepFactory.createLogicStep(EVALUATE, dataModel);
      } else {
        setupForecast();
        dataModel.setNeighborhood(Neighborhood.FORECAST);
        return LogicStepFactory.createLogicStep(FORECAST, dataModel);
      }
    }

    if (patientSeriesStepper.hasCurrent()) {
      return LogicStepFactory.createLogicStep(EVALUATE, dataModel);
    } else {
      nullOutDatafields();
      dataModel.setNeighborhood(Neighborhood.SELECT_BEST_SERIES);
      return LogicStepFactory.createLogicStep(SELECT_BEST, dataModel);
    }

  }

  private void nullOutDatafields() {
    dataModel.setTargetDose(null);
    dataModel.setPreviousTargetDose(null);
    dataModel.setTargetDoseList(null);
    dataModel.setTargetDoseListPos(-1);
    dataModel.setAntigen(null);
    dataModel.setAntigenAdministeredRecord(null);
    dataModel.setSelectedAntigenAdministeredRecordList(null);
  }

  private boolean moveToNextTargetDoseIfAvailable() {
    log("Getting next target dose");
    dataModel.incTargetDoseListPos();
    // if there are no more target doses
    if (dataModel.getTargetDoseListPos() >= dataModel.getTargetDoseList().size()) {
      // if the current target dose is a recurring dose, add a duplicate of it to the
      // target dose list
      RecurringDose recurringDose = dataModel.getTargetDose().getTrackedSeriesDose().getRecurringDose();
      if (recurringDose != null) {
        if (recurringDose.getValue() == YesNo.YES) {
          log("target dose is a recurring dose");
          TargetDose targetDoseNext = new TargetDose(dataModel.getTargetDose());
          dataModel.getTargetDoseList().add(targetDoseNext);
        } else {
          return false;
        }
      } else {
        markRestAsExtraneous();
        return false;
      }
    } else {
      dataModel.setPreviousTargetDose(dataModel.getTargetDose());
      dataModel.setTargetDose(dataModel.getTargetDoseList().get(dataModel.getTargetDoseListPos()));
    }
    return true;
  }

  private boolean setNextPatientSeries() {
    Stepper<PatientSeries> patientSeriesStepper = dataModel.getPatientSeriesStepper();
    patientSeriesStepper.increment();
    if (patientSeriesStepper.hasCurrent()) {
      dataModel.setTargetDoseList(new ArrayList<TargetDose>());
      PatientSeries patientSeries = dataModel.getPatientSeriesStepper().getCurrent();
      patientSeries.setTargetDoseList(dataModel.getTargetDoseList());
      dataModel.setAntigen(patientSeries.getTrackedAntigenSeries().getTargetDisease());
      for (SeriesDose seriesDose : patientSeries.getTrackedAntigenSeries().getSeriesDoseList()) {
        TargetDose targetDose = new TargetDose(seriesDose);
        dataModel.getTargetDoseList().add(targetDose);
      }
      dataModel.setTargetDose(dataModel.getTargetDoseList().get(0));
      dataModel.setPreviousTargetDose(null);
      dataModel.setTargetDoseListPos(0);
      setupSelectedAntigenAdministeredRecordList();
      return true;
    }

    return false;
  }

  private void setupSelectedAntigenAdministeredRecordList() {
    List<AntigenAdministeredRecord> selectedAntigenAdministeredRecordList = new ArrayList<AntigenAdministeredRecord>();
    dataModel.setSelectedAntigenAdministeredRecordList(selectedAntigenAdministeredRecordList);
    for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {
      if (aar.getAntigen() == dataModel.getAntigen()) {
        selectedAntigenAdministeredRecordList.add(aar);
      }
    }
    dataModel.setSelectedAntigenAdministeredRecordPos(0);
    dataModel.setPreviousAntigenAdministeredRecord(null);
    dataModel.setAntigenAdministeredRecord(selectedAntigenAdministeredRecordList.get(0));
  }

  private void setupForecast() {
    Forecast forecast = new Forecast();
    PatientSeries patientSeries = dataModel.getPatientSeriesStepper().getCurrent();
    patientSeries.setForecast(forecast);
    forecast.setAntigen(patientSeries.getTrackedAntigenSeries().getTargetDisease());
    forecast.setTargetDose(dataModel.getTargetDose());
    dataModel.setForecast(forecast);
    dataModel.setForecastingForPatientSeries(patientSeries);
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
    if (dataModel.getPatientSeriesStepper().getList() == null) {
      out.println("<p>No patient series to process</p>");
    }
  }
}
