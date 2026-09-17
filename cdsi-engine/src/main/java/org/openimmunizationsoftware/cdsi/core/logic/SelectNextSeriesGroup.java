package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.Stepper;

/**
 * Implements the repetition Chapter 8's own overview text describes ("Process
 * steps 8.1 through 8.7 are repeated for each series group... Process step 8.8
 * is then used to determine which prioritized patient series are selected as a
 * best patient series") - infrastructure with no numbered subsection of its
 * own. SelectBestPatientSeries (4.5) populates the series-group stepper once
 * per antigen; every exit from the 8.1-8.7 scoring phase (8.2's shortcut
 * outcomes and 8.7's normal path) dispatches back here rather than straight to
 * 8.8, so 8.8 only ever runs once all of an antigen's series groups are done.
 */
public class SelectNextSeriesGroup extends LogicStep {
  public SelectNextSeriesGroup(DataModel dataModel) {
    super(LogicStepType.SELECT_NEXT_SERIES_GROUP, dataModel);
  }

  @Override
  public LogicStep process() {
    Stepper<String> seriesGroupStepper = dataModel.getSeriesGroupStepper();
    seriesGroupStepper.increment();
    if (!seriesGroupStepper.hasCurrent()) {
      dataModel.setCurrentSeriesGroup(null);
      setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
      return next();
    }

    String seriesGroup = seriesGroupStepper.getCurrent();
    dataModel.setCurrentSeriesGroup(seriesGroup);

    List<PatientSeries> patientSeriesForGroup = new ArrayList<PatientSeries>();
    for (PatientSeries patientSeries : dataModel.getPatientSeriesStepper().getList()) {
      if (patientSeries.getTrackedAntigenSeries().getTargetDisease().equals(dataModel.getAntigen())
          && seriesGroup.equals(SelectBestPatientSeries.seriesGroupOf(patientSeries.getTrackedAntigenSeries()))) {
        patientSeriesForGroup.add(patientSeries);
      }
    }
    dataModel.setSelectedPatientSeriesList(patientSeriesForGroup);

    setNextLogicStepType(LogicStepType.PRE_FILTER_PATIENT_SERIES);
    return next();
  }
}
