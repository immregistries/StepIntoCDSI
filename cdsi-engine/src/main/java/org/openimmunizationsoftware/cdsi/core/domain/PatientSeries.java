package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class PatientSeries {
  private PatientSeriesStatus patientSeriesStatus = null;
  private AntigenSeries trackedAntigenSeries = null;
  private int scorePatientSeries = 0;
  private List<TargetDose> targetDoseList = null;
  private Forecast forecast = null;

  public Forecast getForecast() {
    return forecast;
  }

  public void setForecast(Forecast forecast) {
    this.forecast = forecast;
  }

  public List<TargetDose> getTargetDoseList() {
    return targetDoseList;
  }

  public void setTargetDoseList(List<TargetDose> targetDoseList) {
    this.targetDoseList = targetDoseList;
  }

  public PatientSeries() {
    // default;
  }

  public PatientSeries(AntigenSeries trackedAntigenSeries) {
    this.trackedAntigenSeries = trackedAntigenSeries;
  }

  public PatientSeriesStatus getPatientSeriesStatus() {
    return patientSeriesStatus;
  }

  public void setPatientSeriesStatus(PatientSeriesStatus patientSeriesStatus) {
    this.patientSeriesStatus = patientSeriesStatus;
  }

  public AntigenSeries getTrackedAntigenSeries() {
    return trackedAntigenSeries;
  }

  public void setTrackedAntigenSeries(AntigenSeries trackedAntigenSeries) {
    this.trackedAntigenSeries = trackedAntigenSeries;
  }

  public void setScorePatientSeriesScore(int scorePatientSerie) {
    this.scorePatientSeries = scorePatientSerie;
  }

  public void incPatientScoreSeries() {
    scorePatientSeries++;
  }

  public void descPatientScoreSeries() {
    scorePatientSeries--;
  }

  public int getScorePatientSeries() {
    return scorePatientSeries;
  }

  public void resetScore() {
    scorePatientSeries = 0;
  }

  public int getValidDoseCount() {
    int validDoseCount = 0;
    if (targetDoseList != null) {
      for (TargetDose targetDose : targetDoseList) {
        if (targetDose.getTargetDoseStatus() == TargetDoseStatus.SATISFIED) {
          validDoseCount++;
        }
      }
    }
    return validDoseCount;
  }

  public int getNotSatisfiedDoseCount() {
    int notSatisfiedDoseCount = 0;
    if (targetDoseList != null) {
      for (TargetDose targetDose : targetDoseList) {
        if (targetDose.getTargetDoseStatus() == TargetDoseStatus.NOT_SATISFIED) {
          notSatisfiedDoseCount++;
        }
      }
    }
    return notSatisfiedDoseCount;
  }

  public boolean isProductPatientSeries() {
    return trackedAntigenSeries != null
        && trackedAntigenSeries.getSelectPatientSeries() != null
        && trackedAntigenSeries.getSelectPatientSeries().getProductPath() == YesNo.YES;
  }

  /**
   * SELECTB-2: every administered dose that was evaluated against this series
   * came back Valid. Target doses that have not been evaluated yet (the rest of
   * an in-process series) do not count against the series.
   */
  public boolean hasAllValidAdministeredDoses() {
    boolean sawAnEvaluation = false;
    if (targetDoseList != null) {
      for (TargetDose targetDose : targetDoseList) {
        Evaluation evaluation = targetDose.getEvaluation();
        if (evaluation != null && evaluation.getEvaluationStatus() != null) {
          sawAnEvaluation = true;
          if (evaluation.getEvaluationStatus() != EvaluationStatus.VALID) {
            return false;
          }
        }
      }
    }
    return sawAnEvaluation;
  }

  public Date getEarliestValidAdministeredDate() {
    Date earliest = null;
    if (targetDoseList != null) {
      for (TargetDose targetDose : targetDoseList) {
        if (targetDose.getTargetDoseStatus() != TargetDoseStatus.SATISFIED) {
          continue;
        }
        Evaluation evaluation = targetDose.getEvaluation();
        if (evaluation == null || evaluation.getEvaluationStatus() != EvaluationStatus.VALID) {
          continue;
        }
        Date administered = null;
        if (evaluation.getVaccineDoseAdministered() != null) {
          administered = evaluation.getVaccineDoseAdministered().getDateAdministered();
        }
        if (administered == null && targetDose.getSatisfiedByVaccineDoseAdministered() != null) {
          administered = targetDose.getSatisfiedByVaccineDoseAdministered().getDateAdministered();
        }
        if (administered != null && (earliest == null || administered.before(earliest))) {
          earliest = administered;
        }
      }
    }
    return earliest;
  }

  public Date getMaximumAgeDateOfLastTargetDose(Date dateOfBirth) {
    if (dateOfBirth == null) {
      return null;
    }
    TargetDose lastTargetDose = null;
    if (targetDoseList != null && !targetDoseList.isEmpty()) {
      lastTargetDose = targetDoseList.get(targetDoseList.size() - 1);
    }
    if (!hasMaximumAge(lastTargetDose) && forecast != null) {
      // SELECTB-3 names the last target dose. Fixtures that only stamp a
      // maximum age on the forecast target dose (often the next dose, not the
      // last) still need a date to compare against.
      lastTargetDose = forecast.getTargetDose();
    }
    if (!hasMaximumAge(lastTargetDose)) {
      return null;
    }
    return lastTargetDose.getTrackedSeriesDose().getAgeList().get(0).getMaximumAge().getDateFrom(dateOfBirth);
  }

  private static boolean hasMaximumAge(TargetDose targetDose) {
    return targetDose != null && targetDose.getTrackedSeriesDose() != null
        && !targetDose.getTrackedSeriesDose().getAgeList().isEmpty()
        && targetDose.getTrackedSeriesDose().getAgeList().get(0).getMaximumAge() != null;
  }

  public void addScore(int value) {

    scorePatientSeries = scorePatientSeries + value;
  }

  @Override
  public String toString() {
    return this.getTrackedAntigenSeries().getTargetDisease() + ": " + this.getTrackedAntigenSeries().getSeriesName();
  }

}
