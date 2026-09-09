package org.openimmunizationsoftware.cdsi.core.logic;

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

  private static final int MAX_TOTAL_CYCLES = 1000;
  private static final int MAX_REPEATED_STATE_CYCLES = 200;

  public EvaluateAndForecastAllPatientSeries(DataModel dataModel) {
    super(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, dataModel);
  }

  private String buildLoopSignature() {
    Stepper<PatientSeries> patientSeriesStepper = dataModel.getPatientSeriesStepper();
    PatientSeries currentPatientSeries = patientSeriesStepper.getCurrent();
    String seriesName = "none";
    if (currentPatientSeries != null && currentPatientSeries.getTrackedAntigenSeries() != null) {
      seriesName = currentPatientSeries.getTrackedAntigenSeries().getSeriesName();
    }

    String targetDoseStatus = "none";
    if (dataModel.getTargetDose() != null && dataModel.getTargetDose().getTargetDoseStatus() != null) {
      targetDoseStatus = dataModel.getTargetDose().getTargetDoseStatus().name();
    }

    int targetDoseListSize = dataModel.getTargetDoseList() == null ? -1 : dataModel.getTargetDoseList().size();
    int selectedAarListSize = dataModel.getSelectedAntigenAdministeredRecordList() == null
        ? -1
        : dataModel.getSelectedAntigenAdministeredRecordList().size();

    return dataModel.getNeighborhood() +
        "|series=" + seriesName +
        "|targetDosePos=" + dataModel.getTargetDoseListPos() +
        "|aarPos=" + dataModel.getSelectedAntigenAdministeredRecordPos() +
        "|targetDoseStatus=" + targetDoseStatus +
        "|targetDoseListSize=" + targetDoseListSize +
        "|selectedAarListSize=" + selectedAarListSize;
  }

  private LogicStep finalizeStep(LogicStepType nextStepType) {
    int totalCycleCount = dataModel.getEvaluateForecastTotalCycleCount();
    int repeatedStateCount = dataModel.getEvaluateForecastRepeatedStateCount();

    if (totalCycleCount > MAX_TOTAL_CYCLES) {
      alert(LogLevel.CONTROL,
          "ALERT.MAX_PROCESS_CALLS: EvaluateAndForecastAllPatientSeries exceeded " + MAX_TOTAL_CYCLES +
              " cycles. Forcing transition to SELECT_BEST_PATIENT_SERIES. " +
              "Current state: targetDosePos=" + dataModel.getTargetDoseListPos() +
              " aarPos=" + dataModel.getSelectedAntigenAdministeredRecordPos() +
              " neighborhood=" + dataModel.getNeighborhood() +
              " repeatedStateCount=" + repeatedStateCount);
      dataModel.setNeighborhood(Neighborhood.SELECT_BEST_SERIES);
      dataModel.resetEvaluateForecastLoopGuard();
      return LogicStepFactory.createLogicStep(LogicStepType.SELECT_BEST_PATIENT_SERIES, dataModel);
    }

    if (repeatedStateCount > MAX_REPEATED_STATE_CYCLES) {
      alert(LogLevel.CONTROL,
          "ALERT.LOOP_DETECTED: EvaluateAndForecastAllPatientSeries repeated same state " +
              repeatedStateCount + " times. Forcing transition to SELECT_BEST_PATIENT_SERIES to avoid lockup.");
      dataModel.setNeighborhood(Neighborhood.SELECT_BEST_SERIES);
      dataModel.resetEvaluateForecastLoopGuard();
      return LogicStepFactory.createLogicStep(LogicStepType.SELECT_BEST_PATIENT_SERIES, dataModel);
    }

    if (nextStepType == LogicStepType.SELECT_BEST_PATIENT_SERIES) {
      dataModel.resetEvaluateForecastLoopGuard();
    }

    return LogicStepFactory.createLogicStep(nextStepType, dataModel);
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

    int totalCycleCount = dataModel.incrementEvaluateForecastTotalCycleCount();
    String loopSignature = buildLoopSignature();
    int repeatedStateCount = dataModel.recordEvaluateForecastLoopSignature(loopSignature);

    log(LogLevel.TRACE,
        "TRACE: Loop guard state totalCycles=" + totalCycleCount + " repeatedStateCycles=" + repeatedStateCount +
            " signature=" + loopSignature);

    if (totalCycleCount > MAX_TOTAL_CYCLES || repeatedStateCount > MAX_REPEATED_STATE_CYCLES) {
      return finalizeStep(LogicStepType.SELECT_BEST_PATIENT_SERIES);
    }

    // check to see if Patient Series Stepper is setup
    Stepper<PatientSeries> patientSeriesStepper = dataModel.getPatientSeriesStepper();
    log(LogLevel.TRACE, "Checking patient series stepper status");
    int totalSeries = patientSeriesStepper.getList() != null ? patientSeriesStepper.getList().size() : 0;
    log(LogLevel.TRACE, "TRACE: Patient series list size=" + totalSeries + " started=" +
        patientSeriesStepper.isStarted() + " hasCurrent=" + patientSeriesStepper.hasCurrent());

    // Very first time
    if (!patientSeriesStepper.isStarted()) {
      dataModel.resetEvaluateForecastLoopGuard();
      log(LogLevel.CONTROL, "Patient series stepper not started - initializing first patient series");
      patientSeriesStepper.increment();
    }
    // very last time, won't actually run (just in case)
    if (!patientSeriesStepper.hasCurrent()) {
      log(LogLevel.CONTROL, "No current patient series available - moving to SELECT_BEST_PATIENT_SERIES");
      log(LogLevel.TRACE, "TRACE: Patient series list size=" + totalSeries +
          " current=" + patientSeriesStepper.getCurrent());
      nullOutDatafields();
      return finalizeStep(SELECT_BEST);
    }

    PatientSeries currentPatientSeries = patientSeriesStepper.getCurrent();
    // Alert if currentPatientSeries is unexpectedly null
    if (currentPatientSeries == null) {
      alert(LogLevel.CONTROL, "ALERT.MISSING: currentPatientSeries is null entering EVALUATE neighborhood; " +
          "step=EvaluateAndForecastAllPatientSeries neighborhood=EVALUATE antigen=" +
          (dataModel.getAntigen() != null ? dataModel.getAntigen().getName() : "null"));
      return finalizeStep(END);
    }
    String seriesName = currentPatientSeries.getTrackedAntigenSeries().getSeriesName();
    String antigenName = currentPatientSeries.getTrackedAntigenSeries().getTargetDisease().getName();
    int targetDoseCount = currentPatientSeries.getTargetDoseList() != null
        ? currentPatientSeries.getTargetDoseList().size()
        : 0;
    int aarCount = dataModel.getSelectedAntigenAdministeredRecordList() != null
        ? dataModel.getSelectedAntigenAdministeredRecordList().size()
        : 0;

    log(LogLevel.STATE, "STATE: Current patient series: " + seriesName +
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
          return finalizeStep(FORECAST);
        }
        log(LogLevel.REASONING,
            "REASONING: No more target doses available after skipped dose - transitioning to next series");
        setNextPatientSeries();
        dataModel.setNeighborhood(Neighborhood.EVALUATE);
      } else {
        log(LogLevel.REASONING, "REASONING: Target dose status is " + dataModel.getTargetDose().getTargetDoseStatus()
            + " - completed forecast for series, moving to next patient series");
        setNextPatientSeries();
        dataModel.setNeighborhood(Neighborhood.EVALUATE);
      }
    }

    if (dataModel.isNeighborhoodSetup()) {
      log(LogLevel.CONTROL, "CONTROL: In SETUP neighborhood - initializing next patient series");
      log(LogLevel.TRACE, "TRACE: Before setNextPatientSeries: list size=" + totalSeries +
          " current=" + patientSeriesStepper.getCurrent());
      setupCurrentPatientSeries();
      log(LogLevel.TRACE, "TRACE: After setNextPatientSeries: current=" +
          dataModel.getPatientSeriesStepper().getCurrent() +
          " targetDoseListSize=" +
          (dataModel.getTargetDoseList() != null ? dataModel.getTargetDoseList().size() : 0));
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
            dataModel.setPreviousAntigenAdministeredRecord(dataModel.getAntigenAdministeredRecord());
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
          dataModel.setPreviousAntigenAdministeredRecord(dataModel.getAntigenAdministeredRecord());
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
        return finalizeStep(EVALUATE);
      } else {
        log(LogLevel.CONTROL, "CONTROL: Evaluation complete (targetDoses=" + stillHaveTargetDoses +
            ", aars=" + stillHaveAARs + ") - setting up forecast");
        setupForecast();
        dataModel.setNeighborhood(Neighborhood.FORECAST);
        return finalizeStep(FORECAST);
      }
    }

    if (patientSeriesStepper.hasCurrent()) {
      log(LogLevel.CONTROL, "CONTROL: Patient series available - moving to EVALUATE");
      return finalizeStep(EVALUATE);
    } else {
      log(LogLevel.CONTROL, "CONTROL: No more patient series - cleaning up and moving to SELECT_BEST_PATIENT_SERIES");
      nullOutDatafields();
      dataModel.setNeighborhood(Neighborhood.SELECT_BEST_SERIES);
      return finalizeStep(SELECT_BEST);
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
    TargetDose justProcessedDose = dataModel.getTargetDose();
    int oldPos = dataModel.getTargetDoseListPos();

    // Figure 4-6 asks "is this target dose a recurring target dose?" of every
    // satisfied target dose, not only the last one in the series - a
    // mid-series recurring dose must also get a duplicate created for
    // "another target dose" to process, exactly like the last one does.
    // Checking this before advancing the position (and before comparing
    // against the list size below) means a recurring dose's duplicate has
    // already grown the list by the time that comparison runs, whether the
    // recurring dose was in the middle or at the end.
    //
    // This must only fire for a dose that was actually satisfied (consumed
    // an AAR) - not a SKIPPED one. moveToNextTargetDoseIfAvailable() is also
    // called for SKIPPED doses (which never consume an AAR), and a copy
    // constructed from a still-SKIPPED dose evaluates SKIPPED again on its
    // own turn - checking recurring status there would duplicate it forever,
    // tripping the loop guard instead of terminating.
    TargetDoseStatus justProcessedStatus = justProcessedDose.getTargetDoseStatus();
    boolean wasSatisfiedOutcome = justProcessedStatus == TargetDoseStatus.SATISFIED
        || justProcessedStatus == TargetDoseStatus.SUBSTITUTED
        || justProcessedStatus == TargetDoseStatus.UNNECESSARY;
    RecurringDose recurringDose = justProcessedDose.getTrackedSeriesDose().getRecurringDose();
    if (wasSatisfiedOutcome && recurringDose != null && recurringDose.getValue() == YesNo.YES) {
      log(LogLevel.STATE, "STATE: Target dose is RECURRING - adding duplicate to target dose list");
      TargetDose duplicate = new TargetDose(justProcessedDose);
      dataModel.getTargetDoseList().add(duplicate);
      log(LogLevel.TRACE, "TRACE: Recurring dose added - new list size: " + dataModel.getTargetDoseList().size());
    }

    dataModel.incTargetDoseListPos();
    int newPos = dataModel.getTargetDoseListPos();
    log(LogLevel.TRACE, "TRACE: Target dose position: " + oldPos + " -> " + newPos);

    // if there are no more target doses (a recurring dose's own duplicate,
    // added above, already grew the list past newPos, so reaching here means
    // this dose was not recurring - explicitly "No" or not declared at all)
    if (newPos >= dataModel.getTargetDoseList().size()) {
      log(LogLevel.REASONING,
          "REASONING: Not a recurring dose (or explicitly not) - marking remaining AARs as EXTRANEOUS");
      markRestAsExtraneous();
      return false;
    } else {
      log(LogLevel.STATE, "STATE: Transitioning to next target dose in list");
      dataModel.setPreviousTargetDose(justProcessedDose);
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
    return setupCurrentPatientSeries();
  }

  private boolean setupCurrentPatientSeries() {
    Stepper<PatientSeries> patientSeriesStepper = dataModel.getPatientSeriesStepper();
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
    // aarPos already points at the first un-evaluated record, not the last
    // evaluated one - whichever caller reached here (a SATISFIED/SUBSTITUTED/
    // UNNECESSARY dose already advanced aarPos past the record it consumed
    // before calling moveToNextTargetDoseIfAvailable; a SKIPPED dose never
    // advanced it at all, so it still points at the current, untouched
    // record) - so the loop must start at aarPos itself, not aarPos + 1.
    int remainingAARs = aarListSize - aarPos;

    log(LogLevel.CONTROL, "CONTROL: Marking remaining " + remainingAARs + " AARs (#" +
        (aarPos + 1) + "-" + aarListSize + ") as EXTRANEOUS");

    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    for (int i = aarPos; i < aarListSize; i++) {
      dataModel.setAntigenAdministeredRecord(dataModel.getSelectedAntigenAdministeredRecordList().get(i));
      log(LogLevel.TRACE, "TRACE: AAR #" + (i + 1) + " (dated " +
          dataModel.getAntigenAdministeredRecord().getDateAdministered() + ") -> EXTRANEOUS");

      TargetDose targetDose = new TargetDose(seriesDose);
      dataModel.getTargetDoseList().add(targetDose);
      dataModel.setTargetDose(targetDose);
      dataModel.setEvaluationForCurrentTargetDose(EvaluationStatus.EXTRANEOUS, null);
      // TargetDoseStatus has no EXTRANEOUS value of its own (Table 3-2 never
      // defines one) and defaults to NOT_SATISFIED - left unset here, this
      // placeholder dose would wrongly trip Table 7-10's "does any target
      // dose have status NOT_SATISFIED?" check and mark the whole series
      // NOT_COMPLETE, even though the series' real target doses were already
      // satisfied and this is just a bookkeeping record for an extra AAR.
      // UNNECESSARY is the closest existing status for "an extra dose that
      // wasn't required."
      targetDose.setTargetDoseStatus(TargetDoseStatus.UNNECESSARY);
      dataModel.incSelectedAntigenAdministeredRecordPos();
    }

    log(LogLevel.STATE, "STATE: Completed marking " + remainingAARs + " AARs as EXTRANEOUS");
  }

}
