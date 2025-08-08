package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.MORE_THAN_ONE;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ZERO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ONE;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class IdentifyOnePrioritizedPatientSeries extends LogicStep {

  // private ConditionAttribute<Date> caDateAdministered = null;

  public IdentifyOnePrioritizedPatientSeries(DataModel dataModel) {
    super(LogicStepType.IDENTIFY_ONE_PRIORITIZED_PATIENT_SERIES, dataModel);
    // setConditionTableName("Table ");

    // caDateAdministered = new ConditionAttribute<Date>("Vaccine dose
    // administered", "Date
    // Administered");

    // caTriggerAgeDate.setAssumedValue(FUTURE);

    // conditionAttributesList.add(caDateAdministered);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES);
    evaluateLogicTables();
    return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
  }

  private void printStandard(PrintWriter out) {
    out.println(
        "<p>Identify one prioritized patient series examines all of the patient "
            + "series for a given Series Group to determine if one of the patient "
            + "series is superior to all other patient series and can be considered "
            + "the prioritized patient series.</p>");
    printLogicTables(out);
    // print out list of patient series
    printPatientSeriesList(out);
    printBestPatientSeries(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(4, 5, "Table 8-3 Is there a single prioritized patient series in a series group?");
      setLogicCondition(0, new LogicCondition("How many scorable patient series are in the series group?") {
        @Override
        protected LogicResult evaluateInternal() {
          int scorablePatientSeries = dataModel.getScorablePatientSeriesList().size();
          if (scorablePatientSeries == 0) {
            return ZERO;
          } else if (scorablePatientSeries == 1) {
            return ONE;
          } else {
            return MORE_THAN_ONE;
          }
        }
      });

      // 1: How many default patient series are in the series group?
      setLogicCondition(1, new LogicCondition("How many default patient series are in the series group?") {
        @Override
        protected LogicResult evaluateInternal() {
          int defaultPatientSeries = 0;
          log("Looking for default series");
          for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
            if (!patientSeries.getTrackedAntigenSeries().getTargetDisease().equals(dataModel.getAntigen())) {
              continue;
            }
            
            AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
            boolean isDefaultSeries = antigenSeries.getSelectPatientSeries().getDefaultSeries() == YesNo.YES;
            log(" - " + antigenSeries.getSeriesName() + " is default series: "
                + antigenSeries.getSelectPatientSeries().getDefaultSeries());
            if (isDefaultSeries) {
              defaultPatientSeries++;
            }
          }
          log("Found " + defaultPatientSeries + " default series");
          if (defaultPatientSeries == 0) {
            return ZERO;
          } else if (defaultPatientSeries == 1) {
            return ONE;
          } else {
            return MORE_THAN_ONE;
          }
        }
      });
      // 2: How many complete patient series are in the series group?
      setLogicCondition(2, new LogicCondition("How many complete patient series are in the series group?") {
        @Override
        protected LogicResult evaluateInternal() {
          int completePatientSeries = 0;
          for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
            if (!patientSeries.getTrackedAntigenSeries().getTargetDisease().equals(dataModel.getAntigen())) {
              continue;
            }

            if (patientSeries.getPatientSeriesStatus() == PatientSeriesStatus.COMPLETE) {
              completePatientSeries++;
            }
          }
          if (completePatientSeries == 0) {
            return ZERO;
          } else if (completePatientSeries == 1) {
            return ONE;
          } else {
            return MORE_THAN_ONE;
          }
        }
      });
      // 3: How many in-process patient series are in the series group?
      setLogicCondition(3, new LogicCondition("How many in-process patient series are in the series group?") {
        @Override
        protected LogicResult evaluateInternal() {
          int inProcessPatientSeries = 0;
          for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
            if (!patientSeries.getTrackedAntigenSeries().getTargetDisease().equals(dataModel.getAntigen())) {
              continue;
            }

            if (patientSeries.getPatientSeriesStatus() == PatientSeriesStatus.NOT_COMPLETE) {
              for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
                VaccineDoseAdministered vda = targetDose.getSatisfiedByVaccineDoseAdministered();
                if (vda != null) {
                  inProcessPatientSeries++;
                }
              }
            }
          }
          if (inProcessPatientSeries == 0) {
            return ZERO;
          } else if (inProcessPatientSeries == 1) {
            return ONE;
          } else {
            return MORE_THAN_ONE;
          }
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
          for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
            AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
            boolean isDefaultSeries = antigenSeries.getSelectPatientSeries().getDefaultSeries() == YesNo.YES;
            if(isDefaultSeries) {
              dataModel.getPrioritizedPatientSeriesList().add(patientSeries);
              break;
            }
          }
          setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single scorable patient series is the prioritized patient series for the series group.");
          for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
            if (!patientSeries.getTrackedAntigenSeries().getTargetDisease().equals(dataModel.getAntigen())) {
              continue;
            }

            dataModel.getPrioritizedPatientSeriesList().add(patientSeries);
          }
          setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single complete patient series is the prioritized patient series for the series group.");
          for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
            if (!patientSeries.getTrackedAntigenSeries().getTargetDisease().equals(dataModel.getAntigen())) {
              continue;
            }

            boolean isCompleteSeries = patientSeries.getPatientSeriesStatus() == PatientSeriesStatus.COMPLETE;
            if(isCompleteSeries) {
              dataModel.getPrioritizedPatientSeriesList().add(patientSeries);
              break;
            }
          }
          setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single in-process patient series is the prioritized patient series for the series group.");
          for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
            if (!patientSeries.getTrackedAntigenSeries().getTargetDisease().equals(dataModel.getAntigen())) {
              continue;
            }

            if (patientSeries.getPatientSeriesStatus() == PatientSeriesStatus.NOT_COMPLETE) {
              for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
                VaccineDoseAdministered vda = targetDose.getSatisfiedByVaccineDoseAdministered();
                if (vda != null) {
                  dataModel.getPrioritizedPatientSeriesList().add(patientSeries);
                  break;
                }
              }
            }
          }
          setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The default patient series is the prioritized patient series for the series group.");
          for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
            if (!patientSeries.getTrackedAntigenSeries().getTargetDisease().equals(dataModel.getAntigen())) {
              continue;
            }
            
            AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
            boolean isDefaultSeries = antigenSeries.getSelectPatientSeries().getDefaultSeries() == YesNo.YES;
            if(isDefaultSeries) {
              dataModel.getPrioritizedPatientSeriesList().add(patientSeries);
              break;
            }
          }
          setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
        }
      });
    }
  }
}
