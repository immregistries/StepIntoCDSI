package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;
import org.apache.jena.sparql.function.library.leviathan.log;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;

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

    if (dataModel.getAntigenAdministeredRecord() == null) {
      logicStep.log("PRDD dataModel.getAntigenAdministeredRecord() is null");
      return null;
    }

    Evaluation previousVdaEvaluation;
    {
      TargetDose previousTargetDose = dataModel.getPreviousTargetDose();
      if (previousTargetDose == null) {
        logicStep.log("Previous target dose is null");
        return null;
      } else {
        logicStep.log("Previous targetDose #" + previousTargetDose.getTrackedSeriesDose().getDoseNumber());
        previousVdaEvaluation = previousTargetDose.getEvaluation();
      }
    }

    Date tmpPatientReferenceDoseDate = null;
    if (previousVdaEvaluation == null) {
      logicStep.log("PRDD vdaEvaluation is null");
      return null;
    }
    try {
      // CALCDTINT-1
      if (fromImmediatePreviousDoseAdministered == YesNo.YES) {
        logicStep
            .log("PRDD Using CALCDTINT-1 where evaluation status = " + previousVdaEvaluation.getEvaluationStatus());
        if (previousVdaEvaluation.getEvaluationStatus().equals(EvaluationStatus.VALID)
            || previousVdaEvaluation.getEvaluationStatus().equals(EvaluationStatus.NOT_VALID)) {
          logicStep.log("PRDD evaluationReason is " + previousVdaEvaluation.getEvaluationReason());
          if (previousVdaEvaluation.getEvaluationReason() == null
              || !previousVdaEvaluation.getEvaluationReason().equals(EvaluationReason.INADVERTENT_ADMINISTRATION)) {
            AntigenAdministeredRecord previousAAR = dataModel.getPreviousAntigenAdministeredRecord();
            tmpPatientReferenceDoseDate = previousAAR.getVaccineDoseAdministered().getDateAdministered();
          }
        }
      }
      // CALCDTINT-2
      if (fromImmediatePreviousDoseAdministered == YesNo.NO) {
        logicStep.log("PRDD Using CALCDTINT-2");
        if (!this.getFromTargetDoseNumberInSeries().equals("")) {
          for (TargetDose td : dataModel.getTargetDoseList()) {
            if (this.getFromTargetDoseNumberInSeries().equals(td.getTrackedSeriesDose().getDoseNumber())) {
              tmpPatientReferenceDoseDate = td.getSatisfiedByVaccineDoseAdministered().getDateAdministered();
            }
          }
        }
      }
      // CALCDTINT-8
      if (fromImmediatePreviousDoseAdministered == YesNo.NO) {
        logicStep.log("PRDD Using CALCDTINT-8");
        if (this.fromMostRecentVaccineType != null) {
          if (!previousVdaEvaluation.getEvaluationReason().equals(EvaluationReason.INADVERTENT_ADMINISTRATION)) {
            Date mostRecentDate = null;
            for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {
              if (aar.getVaccineType().equals(this.fromMostRecentVaccineType)) {
                mostRecentDate = aar.getDateAdministered();
              }
            }
            tmpPatientReferenceDoseDate = mostRecentDate;
          }
        }
      }
      // CALCDTINT-9
      if (fromImmediatePreviousDoseAdministered == YesNo.NO) {
        logicStep.log("PRDD Using CALCDTINT-9");
        if (!this.getFromRelevantObservation().getCode().equals("")) {
          // TODO set tmpPatientReferenceDoseDate to 'the observation date of the most
          // recent active patient observation'
        }
      }
    } catch (NullPointerException np) {
      np.getCause();
      logicStep.log("NullPointerException " + np.getMessage());
    }
    if (tmpPatientReferenceDoseDate != null) {
      logicStep.log("PRDD returning " + tmpPatientReferenceDoseDate.toString());
    } else {
      logicStep.log("PRDD returning null");
    }

    return tmpPatientReferenceDoseDate;

  }
}
