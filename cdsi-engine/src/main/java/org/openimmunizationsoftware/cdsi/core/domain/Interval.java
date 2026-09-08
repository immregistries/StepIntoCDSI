package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
  private List<VaccineType> fromMostRecentVaccineTypeList = new ArrayList<VaccineType>();
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

  public List<VaccineType> getFromMostRecentVaccineTypeList() {
    return fromMostRecentVaccineTypeList;
  }

  public void setFromMostRecentVaccineTypeList(List<VaccineType> fromMostRecentVaccineTypeList) {
    this.fromMostRecentVaccineTypeList = fromMostRecentVaccineTypeList;
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
        if (!this.fromMostRecentVaccineTypeList.isEmpty()) {
          Date dateAdministeredForThisEvaluation = dataModel.getAntigenAdministeredRecord().getDateAdministered();
          Date mostRecentDate = null;
          for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {
            if (!this.fromMostRecentVaccineTypeList.contains(aar.getVaccineType())) {
              continue;
            }
            Date dateAdministered = aar.getDateAdministered();
            // dataModel.getAntigenAdministeredRecordList() holds the patient's whole
            // history, including the dose currently being evaluated and any later ones -
            // "most recent" must mean strictly before this dose, or a dose could
            // reference itself (or a future dose) as its own reference date.
            if (!dateAdministered.before(dateAdministeredForThisEvaluation)) {
              continue;
            }
            if (!isEligibleCalcdtint8ReferenceDose(aar)) {
              continue;
            }
            if (mostRecentDate == null || dateAdministered.after(mostRecentDate)) {
              mostRecentDate = dateAdministered;
            }
          }
          if (mostRecentDate != null) {
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

  /**
   * CALCDTINT-8 excludes a candidate dose that "is an inadvertent administration" -
   * that dose's own evaluation, not the previous target dose's, which
   * CALCDTINT-1/2 already check separately. It must also exclude an extraneous
   * dose, the same way CALCDTINT-1 only accepts a reference dose whose evaluation
   * status is VALID or NOT_VALID - an extraneous dose (e.g. a booster given too
   * soon after a prior one) does not represent a real, countable administration
   * of the vaccine, so it must not become the "most recent" reference point
   * either. Reads {@code VaccineDoseAdministered.getEvaluatedAgainstTargetDose()}
   * (set by 6.10 Satisfy Target Dose for every outcome, not just the satisfied
   * one) rather than {@code TargetDose.getSatisfiedByVaccineDoseAdministered()},
   * which several other steps rely on staying null for anything but a genuinely
   * satisfied dose - reusing that field here would have broken those. A dose
   * that was never evaluated against any target dose (belongs to a
   * series/antigen this interval's target dose isn't part of) is not excluded -
   * there is no evidence against it.
   */
  private boolean isEligibleCalcdtint8ReferenceDose(AntigenAdministeredRecord aar) {
    TargetDose targetDose = aar.getVaccineDoseAdministered().getEvaluatedAgainstTargetDose();
    if (targetDose == null || targetDose.getEvaluation() == null) {
      return true;
    }
    Evaluation evaluation = targetDose.getEvaluation();
    if (evaluation.getEvaluationStatus() == EvaluationStatus.EXTRANEOUS) {
      return false;
    }
    if (evaluation.getEvaluationReason() == EvaluationReason.INADVERTENT_ADMINISTRATION) {
      return false;
    }
    return true;
  }
}
