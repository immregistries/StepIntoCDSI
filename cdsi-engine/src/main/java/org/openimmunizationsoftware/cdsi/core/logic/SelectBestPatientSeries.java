package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class SelectBestPatientSeries extends LogicStep {
  public SelectBestPatientSeries(DataModel dataModel) {
    super(LogicStepType.SELECT_BEST_PATIENT_SERIES, dataModel);
  }

  @Override
  public LogicStep process() {
    if (dataModel.getBestPatientSeriesList() == null) {
      dataModel.setBestPatientSeriesList(new ArrayList<>());
    }
    dataModel.incAntigenPos();
    if (dataModel.getAntigenPos() < dataModel.getAntigenSelectedList().size()) {
      Antigen antigen = dataModel.getAntigenSelectedList().get(dataModel.getAntigenPos());
      dataModel.setAntigen(antigen);
      ArrayList<AntigenSeries> antigenSeriesSelectedList = new ArrayList<AntigenSeries>();
      for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
        if (antigenSeries.getTargetDisease().equals(antigen)) {
          antigenSeriesSelectedList.add(antigenSeries);
        }
      }
      dataModel.setAntigenSeriesSelectedList(antigenSeriesSelectedList);

      // Chapter 8's overview: "Process steps 8.1 through 8.7 are repeated for each
      // series group". Collect the antigen's distinct series groups here (once per
      // antigen) so SelectNextSeriesGroup can step through them one at a time -
      // excluding a group entirely (SPEC-4.6-0021) when none of its member
      // series is age-appropriate for the patient and the patient has made no
      // progress toward any series in it, e.g. an "Increased Risk 50+" series
      // group must not compete for a young patient's antigen status just
      // because it happens to exist in the Supporting Data.
      Set<String> allSeriesGroups = new LinkedHashSet<String>();
      for (AntigenSeries antigenSeries : antigenSeriesSelectedList) {
        allSeriesGroups.add(seriesGroupOf(antigenSeries));
      }
      Set<String> seriesGroups = new LinkedHashSet<String>();
      for (String seriesGroup : allSeriesGroups) {
        if (isSeriesGroupApplicable(seriesGroup, antigenSeriesSelectedList, antigen)) {
          seriesGroups.add(seriesGroup);
        }
      }
      dataModel.getSeriesGroupStepper().reset();
      dataModel.getSeriesGroupStepper().setList(new ArrayList<String>(seriesGroups));

      setNextLogicStepType(LogicStepType.SELECT_NEXT_SERIES_GROUP);
    } else {
      dataModel.setAntigen(null);
      dataModel.setAntigenSeriesSelectedList(null);
      setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
    }
    dataModel.getPrioritizedPatientSeriesList().clear();

    return next();
  }

  /**
   * An AntigenSeries with no series group declared (or no selectSeries element
   * at all) is treated as belonging to a single, implicit group of its own -
   * behavior for antigens with only one series group is unchanged.
   */
  static String seriesGroupOf(AntigenSeries antigenSeries) {
    if (antigenSeries.getSelectPatientSeries() == null) {
      return "";
    }
    String seriesGroup = antigenSeries.getSelectPatientSeries().getSeriesGroup();
    return seriesGroup == null ? "" : seriesGroup;
  }

  /**
   * A series group is applicable to this patient if at least one of its
   * member series has no age-to-start window at all, or has one that
   * includes the patient's current age (SPEC-4.6-0021) - or, failing that,
   * the patient already has a satisfied target dose toward some series in
   * the group, so genuine progress is never hidden by an age window.
   */
  private boolean isSeriesGroupApplicable(String seriesGroup, List<AntigenSeries> antigenSeriesForAntigen,
      Antigen antigen) {
    Date dateOfBirth = dataModel.getPatient() == null ? null : dataModel.getPatient().getDateOfBirth();
    Date referenceDate = dataModel.getAssessmentDate();
    for (AntigenSeries antigenSeries : antigenSeriesForAntigen) {
      if (seriesGroup.equals(seriesGroupOf(antigenSeries))
          && isWithinAgeToStartWindow(antigenSeries.getSelectPatientSeries(), dateOfBirth, referenceDate)) {
        return true;
      }
    }
    for (PatientSeries patientSeries : dataModel.getPatientSeriesStepper().getList()) {
      AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
      if (!antigenSeries.getTargetDisease().equals(antigen) || !seriesGroup.equals(seriesGroupOf(antigenSeries))) {
        continue;
      }
      if (patientSeries.getTargetDoseList() == null) {
        continue;
      }
      for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
        if (targetDose.getTargetDoseStatus() == TargetDoseStatus.SATISFIED) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isWithinAgeToStartWindow(SelectPatientSeries selectPatientSeries, Date dateOfBirth,
      Date referenceDate) {
    if (selectPatientSeries == null) {
      return true;
    }
    TimePeriod minAgeToStart = selectPatientSeries.getMinAgeToStart();
    boolean hasMin = minAgeToStart != null && minAgeToStart.isValued();
    TimePeriod maxAgeToStart = selectPatientSeries.getMaxAgeToStart();
    boolean hasMax = maxAgeToStart != null && maxAgeToStart.isValued();
    if (!hasMin && !hasMax) {
      return true;
    }
    if (dateOfBirth == null || referenceDate == null) {
      return true;
    }
    if (hasMin && referenceDate.before(minAgeToStart.getDateFrom(dateOfBirth))) {
      return false;
    }
    if (hasMax && !referenceDate.before(maxAgeToStart.getDateFrom(dateOfBirth))) {
      return false;
    }
    return true;
  }

}
