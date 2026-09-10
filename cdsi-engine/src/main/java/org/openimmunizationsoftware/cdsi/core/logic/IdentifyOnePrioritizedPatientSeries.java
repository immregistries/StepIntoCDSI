package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.MORE_THAN_ONE;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ONE;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ZERO;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * 8.2 Identify One Prioritized Patient Series (Table 8-3). SELECTB-7 counts
 * default series over the relevant (stepper) list, not the scorable list, so
 * Rule 1 is reachable when 8.1 promoted nothing.
 */
public class IdentifyOnePrioritizedPatientSeries extends LogicStep {

  public IdentifyOnePrioritizedPatientSeries(DataModel dataModel) {
    super(LogicStepType.IDENTIFY_ONE_PRIORITIZED_PATIENT_SERIES, dataModel);
    LT logicTable = new LT();
    logicTable.setLogicStepSink(this.getLogicStepSink());
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES);
    evaluateLogicTables();
    return next();
  }

  /**
   * SELECTB-7's population: relevant patient series of the antigen (and of the
   * current series group, when 4.5/SelectNextSeriesGroup has set one). Unit
   * tests do not set {@code currentSeriesGroup}, so that filter is applied only
   * when it is present.
   */
  private List<PatientSeries> relevantPatientSeriesInScope() {
    List<PatientSeries> scoped = new ArrayList<PatientSeries>();
    List<PatientSeries> relevant = dataModel.getPatientSeriesStepper() == null ? null
        : dataModel.getPatientSeriesStepper().getList();
    if (relevant == null) {
      return scoped;
    }
    Antigen currentAntigen = dataModel.getAntigen();
    String currentSeriesGroup = dataModel.getCurrentSeriesGroup();
    for (PatientSeries patientSeries : relevant) {
      if (patientSeries == null || patientSeries.getTrackedAntigenSeries() == null) {
        continue;
      }
      if (currentAntigen != null
          && !currentAntigen.equals(patientSeries.getTrackedAntigenSeries().getTargetDisease())) {
        continue;
      }
      if (currentSeriesGroup != null) {
        String seriesGroup = SelectBestPatientSeries.seriesGroupOf(patientSeries.getTrackedAntigenSeries());
        if (!currentSeriesGroup.equals(seriesGroup)) {
          continue;
        }
      }
      scoped.add(patientSeries);
    }
    return scoped;
  }

  private List<PatientSeries> scorablePatientSeriesInScope() {
    List<PatientSeries> scoped = new ArrayList<PatientSeries>();
    List<PatientSeries> scorable = dataModel.getScorablePatientSeriesList();
    if (scorable == null) {
      return scoped;
    }
    Antigen currentAntigen = dataModel.getAntigen();
    for (PatientSeries patientSeries : scorable) {
      if (patientSeries == null || patientSeries.getTrackedAntigenSeries() == null) {
        continue;
      }
      if (currentAntigen != null
          && !currentAntigen.equals(patientSeries.getTrackedAntigenSeries().getTargetDisease())) {
        continue;
      }
      scoped.add(patientSeries);
    }
    return scoped;
  }

  private static boolean isDefaultSeries(PatientSeries patientSeries) {
    AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
    return antigenSeries != null && antigenSeries.getSelectPatientSeries() != null
        && antigenSeries.getSelectPatientSeries().getDefaultSeries() == YesNo.YES;
  }

  private static boolean isInProcess(PatientSeries patientSeries) {
    if (!PatientSeriesStatus.NOT_COMPLETE.equals(patientSeries.getPatientSeriesStatus())) {
      return false;
    }
    if (patientSeries.getTargetDoseList() == null) {
      return false;
    }
    for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
      if (targetDose.getTargetDoseStatus() == TargetDoseStatus.SATISFIED
          || targetDose.getSatisfiedByVaccineDoseAdministered() != null) {
        return true;
      }
    }
    return false;
  }

  private void addDefaultPatientSeriesAsPrioritized() {
    for (PatientSeries patientSeries : relevantPatientSeriesInScope()) {
      if (isDefaultSeries(patientSeries)) {
        dataModel.getPrioritizedPatientSeriesList().add(patientSeries);
        return;
      }
    }
  }

  private LogicResult countToResult(int count) {
    if (count == 0) {
      return ZERO;
    }
    if (count == 1) {
      return ONE;
    }
    return MORE_THAN_ONE;
  }

  private class LT extends LogicTable {
    public LT() {
      super(4, 5, "Table 8-3 Is there a single prioritized patient series in a series group?");
      setLogicCondition(0, new LogicCondition("How many scorable patient series are in the series group?") {
        @Override
        protected LogicResult evaluateInternal() {
          return countToResult(scorablePatientSeriesInScope().size());
        }
      });

      // SELECTB-7 counts over *relevant* patient series, not the scorable list.
      // Table 8-3 Rule 1 is "0 scorable AND 1 default", which is unreachable if
      // both counts walk the same list.
      setLogicCondition(1, new LogicCondition("How many default patient series are in the series group?") {
        @Override
        protected LogicResult evaluateInternal() {
          int defaultPatientSeries = 0;
          log("Looking for default series among relevant patient series");
          for (PatientSeries patientSeries : relevantPatientSeriesInScope()) {
            AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
            log(" - " + antigenSeries.getSeriesName() + " is default series: "
                + (antigenSeries.getSelectPatientSeries() == null ? "null"
                    : antigenSeries.getSelectPatientSeries().getDefaultSeries()));
            if (isDefaultSeries(patientSeries)) {
              defaultPatientSeries++;
            }
          }
          log("Found " + defaultPatientSeries + " default series");
          return countToResult(defaultPatientSeries);
        }
      });

      setLogicCondition(2, new LogicCondition("How many complete patient series are in the series group?") {
        @Override
        protected LogicResult evaluateInternal() {
          int completePatientSeries = 0;
          for (PatientSeries patientSeries : scorablePatientSeriesInScope()) {
            if (PatientSeriesStatus.COMPLETE.equals(patientSeries.getPatientSeriesStatus())) {
              completePatientSeries++;
            }
          }
          return countToResult(completePatientSeries);
        }
      });

      setLogicCondition(3, new LogicCondition("How many in-process patient series are in the series group?") {
        @Override
        protected LogicResult evaluateInternal() {
          int inProcessPatientSeries = 0;
          for (PatientSeries patientSeries : scorablePatientSeriesInScope()) {
            if (isInProcess(patientSeries)) {
              inProcessPatientSeries++;
            }
          }
          return countToResult(inProcessPatientSeries);
        }
      });

      setLogicResults(0, ZERO, ONE, MORE_THAN_ONE, MORE_THAN_ONE, MORE_THAN_ONE);
      setLogicResults(1, ONE, ANY, ANY, ANY, ONE);
      setLogicResults(2, ANY, ANY, ONE, ZERO, ZERO);
      setLogicResults(3, ANY, ANY, ANY, ONE, ZERO);

      setLogicOutcomeDefault(new LogicOutcome() {
        @Override
        public void perform() {
          log("No. There is no single prioritized patient series. "
              + "More than one scorable patient series has potential. "
              + "All scorable patient series are examined to see which should "
              + "be scored and selected as the prioritized patient series for the series group.");
          setNextLogicStepType(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES);
        }
      });

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single default patient series is the prioritized patient series for the series group.");
          addDefaultPatientSeriesAsPrioritized();
          setNextLogicStepType(LogicStepType.SELECT_NEXT_SERIES_GROUP);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single scorable patient series is the prioritized patient series for the series group.");
          for (PatientSeries patientSeries : scorablePatientSeriesInScope()) {
            dataModel.getPrioritizedPatientSeriesList().add(patientSeries);
          }
          setNextLogicStepType(LogicStepType.SELECT_NEXT_SERIES_GROUP);
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single complete patient series is the prioritized patient series for the series group.");
          for (PatientSeries patientSeries : scorablePatientSeriesInScope()) {
            if (PatientSeriesStatus.COMPLETE.equals(patientSeries.getPatientSeriesStatus())) {
              dataModel.getPrioritizedPatientSeriesList().add(patientSeries);
              break;
            }
          }
          setNextLogicStepType(LogicStepType.SELECT_NEXT_SERIES_GROUP);
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single in-process patient series is the prioritized patient series for the series group.");
          for (PatientSeries patientSeries : scorablePatientSeriesInScope()) {
            if (isInProcess(patientSeries)) {
              dataModel.getPrioritizedPatientSeriesList().add(patientSeries);
              break;
            }
          }
          setNextLogicStepType(LogicStepType.SELECT_NEXT_SERIES_GROUP);
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The default patient series is the prioritized patient series for the series group.");
          addDefaultPatientSeriesAsPrioritized();
          setNextLogicStepType(LogicStepType.SELECT_NEXT_SERIES_GROUP);
        }
      });
    }
  }
}
