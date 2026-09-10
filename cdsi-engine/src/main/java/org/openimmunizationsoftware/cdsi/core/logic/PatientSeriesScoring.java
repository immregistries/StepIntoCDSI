package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;

public final class PatientSeriesScoring {

  private PatientSeriesScoring() {
  }

  public static <T> void scoreExtremum(List<PatientSeries> patientSeriesList,
      Function<PatientSeries, T> metric, Comparator<T> comparator, int points) {
    if (patientSeriesList.isEmpty()) {
      return;
    }

    T extremum = null;
    for (PatientSeries patientSeries : patientSeriesList) {
      T value = metric.apply(patientSeries);
      if (extremum == null || comparator.compare(value, extremum) > 0) {
        extremum = value;
      }
    }

    int tiedCount = 0;
    for (PatientSeries patientSeries : patientSeriesList) {
      if (comparator.compare(metric.apply(patientSeries), extremum) == 0) {
        tiedCount++;
      }
    }

    for (PatientSeries patientSeries : patientSeriesList) {
      int comparison = comparator.compare(metric.apply(patientSeries), extremum);
      if (comparison < 0) {
        patientSeries.addScore(-points);
      } else if (tiedCount == 1) {
        patientSeries.addScore(points);
      }
    }
  }
}