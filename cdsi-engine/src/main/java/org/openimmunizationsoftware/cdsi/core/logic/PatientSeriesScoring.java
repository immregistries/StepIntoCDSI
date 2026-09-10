package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

/**
 * Shared Chapter 8 scoring helpers. The N-way tie shape (lone winner +points,
 * every series tied at the extremum 0, everyone else -points) is the pattern
 * SPEC-4.6-0044 made correct in {@code CompletePatientSeries}; this generalizes
 * it so 8.5 and 8.6 stop carrying their own buggy copies.
 */
public final class PatientSeriesScoring {

  private PatientSeriesScoring() {
  }

  public static boolean belongsToCurrentAntigen(PatientSeries patientSeries, Antigen antigen) {
    if (patientSeries == null || patientSeries.getTrackedAntigenSeries() == null || antigen == null) {
      return false;
    }
    return antigen.equals(patientSeries.getTrackedAntigenSeries().getTargetDisease());
  }

  /**
   * SELECTB-12: forecast finish date is the earliest date of the patient series
   * forecast plus the latest minimum interval of the remaining (not-satisfied)
   * target doses. When those inputs are not populated, fall back to the
   * forecast's adjusted past due date so fixtures that only set that field still
   * exercise SELECTB-3.
   */
  public static Date forecastFinishDate(PatientSeries patientSeries) {
    if (patientSeries == null || patientSeries.getForecast() == null) {
      return null;
    }
    Forecast forecast = patientSeries.getForecast();
    Date earliestDate = forecast.getEarliestDate();
    if (earliestDate != null) {
      Date finishDate = earliestDate;
      if (patientSeries.getTargetDoseList() != null) {
        for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
          if (targetDose.getTargetDoseStatus() != TargetDoseStatus.NOT_SATISFIED
              || targetDose.getTrackedSeriesDose() == null) {
            continue;
          }
          for (Interval interval : targetDose.getTrackedSeriesDose().getIntervalList()) {
            TimePeriod minimumInterval = interval.getMinimumInterval();
            if (minimumInterval == null || !minimumInterval.isValued()) {
              continue;
            }
            Date candidate = minimumInterval.getDateFrom(earliestDate);
            if (candidate != null && candidate.after(finishDate)) {
              finishDate = candidate;
            }
          }
        }
      }
      return finishDate;
    }
    return forecast.getAdjustedPastDueDate();
  }

  public static boolean isCompletable(PatientSeries patientSeries, Date dateOfBirth) {
    Date finishDate = forecastFinishDate(patientSeries);
    Date maximumAgeDate = patientSeries.getMaximumAgeDateOfLastTargetDose(dateOfBirth);
    return finishDate != null && maximumAgeDate != null && finishDate.before(maximumAgeDate);
  }

  public static Date dateOfBirth(org.openimmunizationsoftware.cdsi.core.data.DataModel dataModel) {
    if (dataModel.getPatient() == null) {
      return null;
    }
    return dataModel.getPatient().getDateOfBirth();
  }

  public static void scoreIndependent(List<PatientSeries> patientSeriesList, Predicate<PatientSeries> condition,
      int points) {
    for (PatientSeries patientSeries : patientSeriesList) {
      if (condition.test(patientSeries)) {
        patientSeries.addScore(points);
      } else {
        patientSeries.addScore(-points);
      }
    }
  }

  /**
   * Find the extremum of a per-series metric, then score the lone winner
   * {@code +points}, every series tied at it {@code 0}, everyone else
   * {@code -points}. A null metric is treated as "not true" ({@code -points})
   * and does not compete for the extremum.
   */
  public static <T> void scoreExtremum(List<PatientSeries> patientSeriesList,
      Function<PatientSeries, T> metric, Comparator<T> comparator, int points) {
    if (patientSeriesList.isEmpty()) {
      return;
    }

    List<PatientSeries> competing = new ArrayList<PatientSeries>();
    for (PatientSeries patientSeries : patientSeriesList) {
      if (metric.apply(patientSeries) != null) {
        competing.add(patientSeries);
      }
    }

    if (competing.isEmpty()) {
      for (PatientSeries patientSeries : patientSeriesList) {
        patientSeries.addScore(-points);
      }
      return;
    }

    T extremum = null;
    for (PatientSeries patientSeries : competing) {
      T value = metric.apply(patientSeries);
      if (extremum == null || comparator.compare(value, extremum) > 0) {
        extremum = value;
      }
    }

    int tiedCount = 0;
    for (PatientSeries patientSeries : competing) {
      if (comparator.compare(metric.apply(patientSeries), extremum) == 0) {
        tiedCount++;
      }
    }

    for (PatientSeries patientSeries : patientSeriesList) {
      T value = metric.apply(patientSeries);
      if (value == null) {
        patientSeries.addScore(-points);
      } else if (comparator.compare(value, extremum) < 0) {
        patientSeries.addScore(-points);
      } else if (tiedCount == 1) {
        patientSeries.addScore(points);
      }
    }
  }
}
