package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationReason;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

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
  private TimePeriod effectiveDate = null;
  private TimePeriod cessationDate = null;

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

  public TimePeriod getEffectiveDate() {
    return effectiveDate;
  }

  public void setEffectiveDate(TimePeriod effectiveDate) {
    this.effectiveDate = effectiveDate;
  }

  public TimePeriod getCessationDate() {
    return cessationDate;
  }

  public void setCessationDate(TimePeriod cessationDate) {
    this.cessationDate = cessationDate;
  }

  public Date getPatientReferenceDoseDate(DataModel dataModel) {
    Date tmpPatientReferenceDoseDate = null;

    if (dataModel.getAntigenAdministeredRecord() == null) {
      return tmpPatientReferenceDoseDate;
    }

    VaccineDoseAdministered vda = dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered();

    if (vda.getTargetDose() == null) {
      return tmpPatientReferenceDoseDate;
    }
    Evaluation vdaEvaluation = vda.getTargetDose().getEvaluation();
    try {
      // CALCDTINT-1
      if (this.getFromImmediatePreviousDoseAdministered().equals(YesNo.YES)) {
        if (vdaEvaluation.getEvaluationStatus().equals(EvaluationStatus.VALID)
            || vdaEvaluation.getEvaluationStatus().equals(EvaluationStatus.NOT_VALID)) {
          if (!vdaEvaluation.getEvaluationReason().equals(EvaluationReason.INADVERTENT_ADMINISTRATION)) {
            AntigenAdministeredRecord previousAAR = dataModel.getPreviousAntigenAdministeredRecord();
            tmpPatientReferenceDoseDate = previousAAR.getDateAdministered();
          }
        }
      }
      // CALCDTINT-2
      if (this.getFromImmediatePreviousDoseAdministered().equals(YesNo.NO)) {
        if (!this.getFromTargetDoseNumberInSeries().equals("")) {
          // TODO set tmpPatientReferenceDoseDate to 'the date administered of the vaccine
          // dose administered that satisfies the target dose with the same target dose
          // number as the from target dose number in series'
          // Maybe this?
          for (TargetDose td : dataModel.getTargetDoseList()) {
            if (this.getFromTargetDoseNumberInSeries().equals(td.getTrackedSeriesDose().getDoseNumber())) {
              tmpPatientReferenceDoseDate = td.getSatisfiedByVaccineDoseAdministered().getDateAdministered();
            }
          }
        }
      }
      // CALCDTINT-8
      if (this.getFromImmediatePreviousDoseAdministered().equals(YesNo.NO)) {
        if (this.getFromMostRecentVaccineType() != null) {
          if (!vdaEvaluation.getEvaluationReason().equals(EvaluationReason.INADVERTENT_ADMINISTRATION)) {
            // TODO set tmpPatientReferenceDoseDate to 'the date administered of the most
            // recent vaccine dose administered that is the same vaccine type as the from
            // most recent vaccine type'
          }
        }
      }
      // CALCDTINT-9
      if (this.getFromImmediatePreviousDoseAdministered().equals(YesNo.NO)) {
        if (!this.getFromRelevantObservation().getCode().equals("")) {
          // TODO set tmpPatientReferenceDoseDate to 'the observation date of the most
          // recent active patient observation'
        }
      }
    } catch (NullPointerException np) {
      np.getCause();
    }
    return tmpPatientReferenceDoseDate;

  }
}
