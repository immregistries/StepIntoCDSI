package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;

public class Interval {
  private SeriesDose seriesDose = null;
  private YesNo fromImmediatePreviousDoseAdministered = null;
  private String fromTargetDoseNumberInSeries = "";
  private VaccineType fromMostRecentVaccineType = null;
  private ObservationCode fromRelevantObservation = null;
  private TimePeriod absoluteMinimumInterval = null;
  private TimePeriod minimumInterval = null;
  private TimePeriod earliestRecommendedInterval = null;
  private TimePeriod latestRecommendedInterval = null;
  private IntervalPriority intervalPriority = null;
  private Date effectiveDate = null;
  private Date cessationDate = null;

  public IntervalPriority getIntervalPriority() {
    return intervalPriority;
  }

  public void setIntervalPriority(IntervalPriority intervalPriority) {
    this.intervalPriority = intervalPriority;
  }

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

  public YesNo getFromImmediatePreviousDoseAdministered() {
    return fromImmediatePreviousDoseAdministered;
  }

  public void setFromImmediatePreviousDoseAdministered(
      YesNo fromImmediatePreviousDoseAdministered) {
    this.fromImmediatePreviousDoseAdministered = fromImmediatePreviousDoseAdministered;
  }

  public String getFromTargetDoseNumberInSeries() {
    return fromTargetDoseNumberInSeries;
  }

  public void setFromTargetDoseNumberInSeries(String fromTargetDoseNumberInSeries) {
    this.fromTargetDoseNumberInSeries = fromTargetDoseNumberInSeries;
  }

  public VaccineType getFromMostRecentVaccineType() {
    return fromMostRecentVaccineType;
  }

  public void setFromMostRecentVaccineType(VaccineType fromMostRecentVaccineType) {
    this.fromMostRecentVaccineType = fromMostRecentVaccineType;
  }

  public ObservationCode getFromRelevantObservation() {
    return fromRelevantObservation;
  }

  public void setFromRelevantObservation(ObservationCode fromRelevantObservation) {
    this.fromRelevantObservation = fromRelevantObservation;
  }

  public TimePeriod getAbsoluteMinimumInterval() {
    return absoluteMinimumInterval;
  }

  public void setAbsoluteMinimumInterval(TimePeriod absoluteMinimumInterval) {
    this.absoluteMinimumInterval = absoluteMinimumInterval;
  }

  public TimePeriod getMinimumInterval() {
    return minimumInterval;
  }

  public void setMinimumInterval(TimePeriod minimumInterval) {
    this.minimumInterval = minimumInterval;
  }

  public TimePeriod getEarliestRecommendedInterval() {
    return earliestRecommendedInterval;
  }

  public void setEarliestRecommendedInterval(TimePeriod earliestRecommendedInterval) {
    this.earliestRecommendedInterval = earliestRecommendedInterval;
  }

  public TimePeriod getLatestRecommendedInterval() {
    return latestRecommendedInterval;
  }

  public void setLatestRecommendedInterval(TimePeriod latestRecommendedInterval) {
    this.latestRecommendedInterval = latestRecommendedInterval;
  }

  public Date getEffectiveDate() {
    return effectiveDate;
  }

  public void setEffectiveDate(Date effectiveDate) {
    this.effectiveDate = effectiveDate;
  }

  public Date getCessationDate() {
    return cessationDate;
  }

  public void setCessationDate(Date cessationDate) {
    this.cessationDate = cessationDate;
  }

  public Date getPatientReferenceDoseDate(DataModel dataModel, LogicStep logicStep) {
    logicStep.log(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.TRACE,
        "TRACE: Calculating Patient Reference Dose Date (PRDD)");
    logicStep.log(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.TRACE,
        "TRACE: Absolute minimum interval is (" + absoluteMinimumInterval.toString() + ")");

    if (dataModel.getAntigenAdministeredRecord() == null) {
      logicStep.alert(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
          "ALERT.MISSING: AntigenAdministeredRecord is null when calculating PRDD; returning null");
      return null;
    }

    Evaluation previousVdaEvaluation;
    {
      TargetDose previousTargetDose = dataModel.getPreviousTargetDose();
      if (previousTargetDose == null) {
        logicStep.alert(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
            "ALERT.MISSING: Previous target dose is null when calculating PRDD; returning null");
        return null;
      } else {
        logicStep.log(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.TRACE,
            "TRACE: Previous targetDose #" + previousTargetDose.getTrackedSeriesDose().getDoseNumber());
        previousVdaEvaluation = previousTargetDose.getEvaluation();
      }
    }

    Date tmpPatientReferenceDoseDate = null;
    if (previousVdaEvaluation == null) {
      logicStep.alert(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
          "ALERT.MISSING: Previous evaluation is null when calculating PRDD; returning null");
      return null;
    }
    try {
      // CALCDTINT-1
      if (fromImmediatePreviousDoseAdministered == YesNo.YES) {
        logicStep
            .log(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
                "REASONING: Attempting to use CALCDTINT-1 where previous evaluation status = "
                    + previousVdaEvaluation.getEvaluationStatus());
        if (previousVdaEvaluation.getEvaluationStatus().equals(EvaluationStatus.VALID)
            || previousVdaEvaluation.getEvaluationStatus().equals(EvaluationStatus.NOT_VALID)) {
          logicStep.log(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.TRACE,
              "TRACE: evaluationReason is " + previousVdaEvaluation.getEvaluationReason());
          if (previousVdaEvaluation.getEvaluationReason() == null
              || !previousVdaEvaluation.getEvaluationReason().equals(EvaluationReason.INADVERTENT_ADMINISTRATION)) {
            AntigenAdministeredRecord previousAAR = dataModel.getPreviousAntigenAdministeredRecord();
            if (previousAAR == null) {
              logicStep.alert(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
                  "ALERT.MISSING: Previous AAR is null in CALCDTINT-1; cannot determine PRDD");
            } else {
              tmpPatientReferenceDoseDate = previousAAR.getVaccineDoseAdministered().getDateAdministered();
              logicStep.log(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
                  "REASONING: Success using CALCDTINT-1");
            }
          }
        }
      }
      // CALCDTINT-2
      if (fromImmediatePreviousDoseAdministered == YesNo.NO) {
        if (!this.getFromTargetDoseNumberInSeries().equals("")) {
          for (TargetDose td : dataModel.getTargetDoseList()) {
            if (td.getSatisfiedByVaccineDoseAdministered() == null
                || td.getSatisfiedByVaccineDoseAdministered().getDateAdministered() == null) {
              continue;
            }
            if (this.getFromTargetDoseNumberInSeries().equals(td.getTrackedSeriesDose().getDoseNumber())) {
              tmpPatientReferenceDoseDate = td.getSatisfiedByVaccineDoseAdministered().getDateAdministered();
              logicStep.log(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
                  "REASONING: Success using CALCDTINT-2");
            }
          }
        }
      }
      // CALCDTINT-8
      if (fromImmediatePreviousDoseAdministered == YesNo.NO) {
        if (this.fromMostRecentVaccineType != null) {
          if (!previousVdaEvaluation.getEvaluationReason().equals(EvaluationReason.INADVERTENT_ADMINISTRATION)) {
            Date mostRecentDate = null;
            for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {
              if (aar.getVaccineType().equals(this.fromMostRecentVaccineType)) {
                mostRecentDate = aar.getDateAdministered();
              }
            }
            tmpPatientReferenceDoseDate = mostRecentDate;
            logicStep.log(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
                "REASONING: Using CALCDTINT-8");
          }
        }
      }
      // CALCDTINT-9
      if (fromImmediatePreviousDoseAdministered == YesNo.NO) {
        if (this.getFromRelevantObservation() != null && !this.getFromRelevantObservation().getCode().equals("")) {
          logicStep.log(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
              "REASONING: Using CALCDTINT-9");
        }
      }
    } catch (NullPointerException np) {
      logicStep.alert(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
          "ALERT.INVARIANT: NullPointerException in PRDD calculation: " + np.getMessage());
    }
    if (tmpPatientReferenceDoseDate != null) {
      logicStep.log(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
          "REASONING: PRDD calculated as " + logicStep.formatDate(tmpPatientReferenceDoseDate));
    } else {
      logicStep.alert(org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel.REASONING,
          "ALERT.MISSING: PRDD is null; no valid interval calculation rule (CALCDTINT-1/2/8/9) succeeded");
    }

    return tmpPatientReferenceDoseDate;

  }
}
