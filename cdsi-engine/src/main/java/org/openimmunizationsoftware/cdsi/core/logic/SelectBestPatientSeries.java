package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;

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
      // antigen) so SelectNextSeriesGroup can step through them one at a time.
      Set<String> seriesGroups = new LinkedHashSet<String>();
      for (AntigenSeries antigenSeries : antigenSeriesSelectedList) {
        seriesGroups.add(seriesGroupOf(antigenSeries));
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

}
