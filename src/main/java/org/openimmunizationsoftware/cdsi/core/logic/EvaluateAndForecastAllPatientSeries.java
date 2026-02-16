package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.Neighborhood;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.RecurringDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.Stepper;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel;

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
    final LogicStepType END = LogicStepType.END;

    // check to see if Patient Series Stepper is setup
    Stepper<PatientSeries> patientSeriesStepper = dataModel.getPatientSeriesStepper();
    log(LogLevel.TRACE, "Checking patient series stepper status");

    // Very first time
    if (!patientSeriesStepper.isStarted()) {
      log(LogLevel.CONTROL, "Patient series stepper not started - initializing first patient series");
      patientSeriesStepper.increment();
    }
    // very last time, won't actually run (just in case)
    if (!patientSeriesStepper.hasCurrent()) {
      log(LogLevel.CONTROL, "No current patient series available - moving to SELECT_BEST_PATIENT_SERIES");
      nullOutDatafields();
      return LogicStepFactory.createLogicStep(SELECT_BEST, dataModel);
    }

    PatientSeries currentPatientSeries = patientSeriesStepper.getCurrent();
    // Alert if currentPatientSeries is unexpectedly null
    if (currentPatientSeries == null) {
      alert(LogLevel.CONTROL, "ALERT.MISSING: currentPatientSeries is null entering EVALUATE neighborhood; " +
          "step=EvaluateAndForecastAllPatientSeries neighborhood=EVALUATE antigen=" +
          (dataModel.getAntigen() != null ? dataModel.getAntigen().getName() : "null"));
      return LogicStepFactory.createLogicStep(END, dataModel);
    }
    String seriesName = currentPatientSeries.getTrackedAntigenSeries().getSeriesName();
    String antigenName = currentPatientSeries.getTrackedAntigenSeries().getTargetDisease().getName();
    int targetDoseCount = currentPatientSeries.getTargetDoseList() != null
        ? currentPatientSeries.getTargetDoseList().size()
        : 0;
    int aarCount = dataModel.getSelectedAntigenAdministeredRecordList() != null
        ? dataModel.getSelectedAntigenAdministeredRecordList().size()
        : 0;

    log(LogLevel.STATE, "CONTROL: Current patient series: " + seriesName +
        " (antigen=" + antigenName + ", targetDoses=" + targetDoseCount + ", aars=" + aarCount + ")");

    // Three possible outcomes
    // LogicStepType.EVALUATE_DOSE_ADMINISTERED_CONDITION;
    // LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST;
    // LogicStepType.SELECT_BEST_PATIENT_SERIES;

    if (dataModel.isNeighborhoodForecast()) {
      log(LogLevel.CONTROL, "CONTROL: In FORECAST neighborhood");
      if (dataModel.getTargetDose().getTargetDoseStatus() == TargetDoseStatus.SKIPPED) {
        log(LogLevel.REASONING, "REASONING: Target dose status is SKIPPED - attempting to move to next target dose");
        if (moveToNextTargetDoseIfAvailable()) {
          log(LogLevel.CONTROL, "CONTROL: Moved to next target dose - continuing FORECAST (dose " +
              dataModel.getTargetDose().getTrackedSeriesDose().getDoseNumber() + ")");
          return LogicStepFactory.createLogicStep(FORECAST, dataModel);
        }
        log(LogLevel.REASONING,
            "REASONING: No more target doses available after skipped dose - transitioning to next series");
      } else {
        log(LogLevel.REASONING, "REASONING: Target dose status is " + dataModel.getTargetDose().getTargetDoseStatus()
            + " - completed forecast for series, moving to next patient series");
        setNextPatientSeries();
        dataModel.setNeighborhood(Neighborhood.EVALUATE);
      }
    }

    if (dataModel.isNeighborhoodSetup()) {
      log(LogLevel.CONTROL, "CONTROL: In SETUP neighborhood - initializing next patient series");
      setNextPatientSeries();
      dataModel.setNeighborhood(Neighborhood.EVALUATE);
    }

    if (dataModel.isNeighborhoodEvaluate() && patientSeriesStepper.hasCurrent()) {
      log(LogLevel.CONTROL, "CONTROL: In EVALUATE neighborhood");

      boolean stillHaveTargetDoses = true;
      TargetDoseStatus status = dataModel.getTargetDose().getTargetDoseStatus();
      log(LogLevel.TRACE, "TRACE: Current target dose status: " + status + " (pos=" +
          dataModel.getTargetDoseListPos() + ", dose " +
          dataModel.getTargetDose().getTrackedSeriesDose().getDoseNumber() + ")");

      switch (status) {
        case SKIPPED:
          log(LogLevel.REASONING, "REASONING: Target dose SKIPPED - advancing to next target dose");
          stillHaveTargetDoses = moveToNextTargetDoseIfAvailable();
          break;
        case NOT_SATISFIED:
          // Only advance AAR if evaluation has actually been performed and completed
          // Check if there's a latest evaluation with a status set - if so, it completed
          boolean shouldAdvanceAAR = false;
          if (!dataModel.getTargetDose().getEvaluationList().isEmpty()) {
            Evaluation latestEval = dataModel.getTargetDose().getEvaluation();
            if (latestEval != null && latestEval.getEvaluationStatus() != null) {
              shouldAdvanceAAR = true;
            }
          }

          if (shouldAdvanceAAR) {
            log(LogLevel.CONTROL, "CONTROL: Target dose NOT_SATISFIED - advancing AAR (pos " +
                dataModel.getSelectedAntigenAdministeredRecordPos() + " -> " +
                (dataModel.getSelectedAntigenAdministeredRecordPos() + 1) + ")");
            // stay on the same target dose
            dataModel.incSelectedAntigenAdministeredRecordPos();
            int nextAARPos = dataModel.getSelectedAntigenAdministeredRecordPos();
            if (nextAARPos < dataModel.getSelectedAntigenAdministeredRecordList().size()) {
              dataModel
                  .setAntigenAdministeredRecord(dataModel.getSelectedAntigenAdministeredRecordList().get(nextAARPos));
            }
          } else {
            log(LogLevel.TRACE,
                "TRACE: First evaluation for this target dose or evaluation not yet completed - proceeding to evaluation without advancing AAR");
          }
          break;
        case SATISFIED:
        case SUBSTITUTED:
        case UNNECESSARY:
          log(LogLevel.REASONING, "REASONING: Target dose " + status +
              " - advancing both AAR and target dose");
          dataModel.incSelectedAntigenAdministeredRecordPos();
          int nextAARPos = dataModel.getSelectedAntigenAdministeredRecordPos();
          if (nextAARPos < dataModel.getSelectedAntigenAdministeredRecordList().size()) {
            dataModel
                .setAntigenAdministeredRecord(dataModel.getSelectedAntigenAdministeredRecordList().get(nextAARPos));
          }
          stillHaveTargetDoses = moveToNextTargetDoseIfAvailable();
          break;
      }

      int aarPos = dataModel.getSelectedAntigenAdministeredRecordPos();
      int aarListSize = dataModel.getSelectedAntigenAdministeredRecordList().size();
      boolean stillHaveAARs = aarPos < aarListSize;

      // Alert if aarPos is out of bounds
      if (aarPos > aarListSize) {
        alert(LogLevel.CONTROL, "ALERT.INVARIANT: aarIndex out of range (" + aarPos + " > " + aarListSize + "); " +
            "step=EvaluateAndForecastAllPatientSeries series=" + seriesName + " targetDose=" +
            dataModel.getTargetDoseListPos());
      }

      log(LogLevel.TRACE, "TRACE: stillHaveTargetDoses=" + stillHaveTargetDoses + " stillHaveAARs=" + stillHaveAARs +
          " aarIndex=" + aarPos + "/" + aarListSize + " targetDoseIndex=" + dataModel.getTargetDoseListPos());

      if (stillHaveAARs && stillHaveTargetDoses) {
        log(LogLevel.CONTROL, "CONTROL: Continuing evaluation - selecting AAR #" + aarPos);
        return LogicStepFactory.createLogicStep(EVALUATE, dataModel);
      } else {
        log(LogLevel.CONTROL, "CONTROL: Evaluation complete (targetDoses=" + stillHaveTargetDoses +
            ", aars=" + stillHaveAARs + ") - setting up forecast");
        setupForecast();
        dataModel.setNeighborhood(Neighborhood.FORECAST);
        return LogicStepFactory.createLogicStep(FORECAST, dataModel);
      }
    }

    if (patientSeriesStepper.hasCurrent()) {
      log(LogLevel.CONTROL, "CONTROL: Patient series available - moving to EVALUATE");
      return LogicStepFactory.createLogicStep(EVALUATE, dataModel);
    } else {
      log(LogLevel.CONTROL, "CONTROL: No more patient series - cleaning up and moving to SELECT_BEST_PATIENT_SERIES");
      nullOutDatafields();
      dataModel.setNeighborhood(Neighborhood.SELECT_BEST_SERIES);
      return LogicStepFactory.createLogicStep(SELECT_BEST, dataModel);
    }

  }

  private void nullOutDatafields() {
    log(LogLevel.STATE, "STATE: Nulling out data fields - clearing target dose, antigen, and AAR data");
    dataModel.setTargetDose(null);
    dataModel.setPreviousTargetDose(null);
    dataModel.setTargetDoseList(null);
    dataModel.setTargetDoseListPos(-1);
    dataModel.setAntigen(null);
    dataModel.setAntigenAdministeredRecord(null);
    dataModel.setSelectedAntigenAdministeredRecordList(null);
  }

  private boolean moveToNextTargetDoseIfAvailable() {
    log(LogLevel.CONTROL, "CONTROL: Advancing to next target dose");
    int oldPos = dataModel.getTargetDoseListPos();
    dataModel.incTargetDoseListPos();
    int newPos = dataModel.getTargetDoseListPos();
    log(LogLevel.TRACE, "TRACE: Target dose position: " + oldPos + " -> " + newPos);

    // if there are no more target doses
    if (newPos >= dataModel.getTargetDoseList().size()) {
      log(LogLevel.TRACE, "TRACE: Target dose index (" + newPos + ") >= list size (" +
          dataModel.getTargetDoseList().size() + ") - checking for recurring dose");

      // if the current target dose is a recurring dose, add a duplicate of it to the
      // target dose list
      RecurringDose recurringDose = dataModel.getTargetDose().getTrackedSeriesDose().getRecurringDose();
      if (recurringDose != null) {
        if (recurringDose.getValue() == YesNo.YES) {
          log(LogLevel.STATE, "STATE: Target dose is RECURRING - adding duplicate to target dose list");
          TargetDose targetDoseNext = new TargetDose(dataModel.getTargetDose());
          dataModel.getTargetDoseList().add(targetDoseNext);
          log(LogLevel.TRACE, "TRACE: Recurring dose added - new list size: " + dataModel.getTargetDoseList().size());
        } else {
          log(LogLevel.REASONING,
              "REASONING: Recurring dose value is NO - no more target doses available, ending evaluation");
          return false;
        }
      } else {
        log(LogLevel.REASONING, "REASONING: No recurring dose defined - marking remaining AARs as EXTRANEOUS");
        markRestAsExtraneous();
        return false;
      }
    } else {
      log(LogLevel.STATE, "STATE: Transitioning to next target dose in list");
      dataModel.setPreviousTargetDose(dataModel.getTargetDose());
      TargetDose nextDose = dataModel.getTargetDoseList().get(newPos);
      dataModel.setTargetDose(nextDose);
      log(LogLevel.TRACE, "TRACE: Now on target dose: " + nextDose.getTrackedSeriesDose().getDoseNumber() +
          " (index " + newPos + ")");
    }
    return true;
  }

  private boolean setNextPatientSeries() {
    log(LogLevel.CONTROL, "CONTROL: Transitioning to next patient series");
    Stepper<PatientSeries> patientSeriesStepper = dataModel.getPatientSeriesStepper();
    patientSeriesStepper.increment();

    if (patientSeriesStepper.hasCurrent()) {
      PatientSeries patientSeries = dataModel.getPatientSeriesStepper().getCurrent();
      String seriesName = patientSeries.getTrackedAntigenSeries().getSeriesName();
      String antigenName = patientSeries.getTrackedAntigenSeries().getTargetDisease().getName();

      log(LogLevel.STATE, "STATE: Selected patient series: " + seriesName + " (antigen=" + antigenName + ")");

      dataModel.setTargetDoseList(new ArrayList<TargetDose>());
      patientSeries.setTargetDoseList(dataModel.getTargetDoseList());
      dataModel.setAntigen(patientSeries.getTrackedAntigenSeries().getTargetDisease());

      log(LogLevel.TRACE, "TRACE: Building target doses from series doses");
      for (SeriesDose seriesDose : patientSeries.getTrackedAntigenSeries().getSeriesDoseList()) {
        TargetDose targetDose = new TargetDose(seriesDose);
        dataModel.getTargetDoseList().add(targetDose);
      }
      int targetDoseCount = dataModel.getTargetDoseList().size();

      // Alert if target dose list is empty
      if (targetDoseCount == 0) {
        alert(LogLevel.CONTROL, "ALERT.MISSING: targetDoseList is empty after building; " +
            "step=EvaluateAndForecastAllPatientSeries series=" + seriesName +
            " antigen=" + antigenName);
      }

      log(LogLevel.STATE, "STATE: Created " + targetDoseCount + " target doses for this series");

      if (targetDoseCount > 0) {
        dataModel.setTargetDose(dataModel.getTargetDoseList().get(0));
        dataModel.setPreviousTargetDose(null);
        dataModel.setTargetDoseListPos(0);
        log(LogLevel.STATE, "STATE: Starting with target dose 1 (dose number: " +
            dataModel.getTargetDose().getTrackedSeriesDose().getDoseNumber() + ")");
      }

      setupSelectedAntigenAdministeredRecordList();
      return true;
    }

    log(LogLevel.CONTROL, "CONTROL: No more patient series available");
    return false;
  }

  private void setupSelectedAntigenAdministeredRecordList() {
    String antigenName = dataModel.getAntigen().getName();
    log(LogLevel.CONTROL, "CONTROL: Building selected AAR list for antigen: " + antigenName);

    List<AntigenAdministeredRecord> selectedAntigenAdministeredRecordList = new ArrayList<AntigenAdministeredRecord>();
    dataModel.setSelectedAntigenAdministeredRecordList(selectedAntigenAdministeredRecordList);

    int totalAARs = dataModel.getAntigenAdministeredRecordList() != null
        ? dataModel.getAntigenAdministeredRecordList().size()
        : 0;
    log(LogLevel.TRACE, "TRACE: Filtering AARs from " + totalAARs + " total");

    if (dataModel.getAntigenAdministeredRecordList() != null) {
      for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {
        if (aar.getAntigen() == dataModel.getAntigen()) {
          selectedAntigenAdministeredRecordList.add(aar);
        }
      }
    }

    int selectedCount = selectedAntigenAdministeredRecordList.size();
    log(LogLevel.STATE, "STATE: Selected " + selectedCount + " AARs matching antigen " + antigenName);

    dataModel.setSelectedAntigenAdministeredRecordPos(0);
    dataModel.setPreviousAntigenAdministeredRecord(null);

    if (!selectedAntigenAdministeredRecordList.isEmpty()) {
      dataModel.setAntigenAdministeredRecord(selectedAntigenAdministeredRecordList.get(0));
      log(LogLevel.STATE, "STATE: Starting with AAR #1 (dated: " +
          selectedAntigenAdministeredRecordList.get(0).getDateAdministered() + ")");
    } else {
      // Alert if no AARs found - this may be ok (no history) but denotes missed AAR
      // opportunity
      log(LogLevel.REASONING, "REASONING: No AARs found for antigen - will proceed directly to forecasting");
    }
  }

  private void setupForecast() {
    log(LogLevel.CONTROL, "CONTROL: Entering FORECAST phase - initializing forecast object");

    PatientSeries patientSeries = dataModel.getPatientSeriesStepper().getCurrent();

    // Alert if patientSeries is null
    if (patientSeries == null) {
      alert(LogLevel.CONTROL, "ALERT.MISSING: patientSeries is null when trying to setup forecast; " +
          "step=EvaluateAndForecastAllPatientSeries neighborhood=FORECAST->SETUP");
      return;
    }

    Forecast forecast = new Forecast();
    patientSeries.setForecast(forecast);

    String antigenName = patientSeries.getTrackedAntigenSeries().getTargetDisease().getName();
    String targetDoseNumber = "";
    if (dataModel.getTargetDose() != null) {
      targetDoseNumber = String.valueOf(dataModel.getTargetDose().getTrackedSeriesDose().getDoseNumber());
    } else {
      targetDoseNumber = "unknown";
    }

    forecast.setAntigen(patientSeries.getTrackedAntigenSeries().getTargetDisease());
    forecast.setTargetDose(dataModel.getTargetDose());

    log(LogLevel.STATE, "STATE: Forecast initialized for antigen=" + antigenName +
        " targetDose=" + targetDoseNumber);

    dataModel.setForecast(forecast);
    dataModel.setForecastingForPatientSeries(patientSeries);
  }

  private void markRestAsExtraneous() {
    int aarListSize = dataModel.getSelectedAntigenAdministeredRecordList().size();
    int aarPos = dataModel.getSelectedAntigenAdministeredRecordPos();
    int remainingAARs = aarListSize - (aarPos + 1);

    log(LogLevel.CONTROL, "CONTROL: Marking remaining " + remainingAARs + " AARs (#" +
        (aarPos + 2) + "-" + aarListSize + ") as EXTRANEOUS");

    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    for (int i = aarPos + 1; i < aarListSize; i++) {
      dataModel.setAntigenAdministeredRecord(dataModel.getSelectedAntigenAdministeredRecordList().get(i));
      log(LogLevel.TRACE, "TRACE: AAR #" + (i + 1) + " (dated " +
          dataModel.getAntigenAdministeredRecord().getDateAdministered() + ") -> EXTRANEOUS");

      TargetDose targetDose = new TargetDose(seriesDose);
      dataModel.getTargetDoseList().add(targetDose);
      dataModel.setTargetDose(targetDose);
      dataModel.setEvaluationForCurrentTargetDose(EvaluationStatus.EXTRANEOUS, null);
      dataModel.incSelectedAntigenAdministeredRecordPos();
    }

    log(LogLevel.STATE, "STATE: Completed marking " + remainingAARs + " AARs as EXTRANEOUS");
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
