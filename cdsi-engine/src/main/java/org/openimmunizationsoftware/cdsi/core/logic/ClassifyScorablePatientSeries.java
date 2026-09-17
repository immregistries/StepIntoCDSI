package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

/**
 * 8.3 Classify Scorable Patient Series (Table 8-5). SELECTB-16's in-process
 * count and SELECTB-21's valid-dose count are separate questions. Table 8-5
 * has no column for "one complete and one in-process"; the default outcome
 * prefers complete scoring, then in-process scoring, rather than the
 * historical NO_VALID_DOSES pre-evaluation default (SPECIFICATION_AMBIGUITY).
 */
public class ClassifyScorablePatientSeries extends LogicStep {

  public ClassifyScorablePatientSeries(DataModel dataModel) {
    super(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES, dataModel);
    LT logicTable = new LT();
    logicTable.setLogicStepSink(this.getLogicStepSink());
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.NO_VALID_DOSES);
    evaluateLogicTables();
    return next();
  }

  private List<PatientSeries> scorablePatientSeries() {
    return dataModel.getScorablePatientSeriesList();
  }

  private int completePatientSeriesCount() {
    int completePatientSeries = 0;
    for (PatientSeries patientSeries : scorablePatientSeries()) {
      if (patientSeries != null && PatientSeriesStatus.COMPLETE.equals(patientSeries.getPatientSeriesStatus())) {
        completePatientSeries++;
      }
    }
    return completePatientSeries;
  }

  /**
   * SELECTB-16: a scorable patient series is in-process when it is Not Complete
   * and has at least one Satisfied target dose. Counted per series, not per
   * dose.
   */
  private int inProcessPatientSeriesCount() {
    int inProcessPatientSeries = 0;
    for (PatientSeries patientSeries : scorablePatientSeries()) {
      if (patientSeries != null && isInProcess(patientSeries)) {
        inProcessPatientSeries++;
      }
    }
    return inProcessPatientSeries;
  }

  private static boolean isInProcess(PatientSeries patientSeries) {
    if (!PatientSeriesStatus.NOT_COMPLETE.equals(patientSeries.getPatientSeriesStatus())) {
      return false;
    }
    return hasASatisfiedTargetDose(patientSeries);
  }

  /**
   * SELECTB-21: valid dose count is the count of Satisfied target doses,
   * status-agnostic. Used here as "does this series have any valid dose?"
   */
  private int patientSeriesWithValidDosesCount() {
    int count = 0;
    for (PatientSeries patientSeries : scorablePatientSeries()) {
      if (patientSeries != null && hasASatisfiedTargetDose(patientSeries)) {
        count++;
      }
    }
    return count;
  }

  private static boolean hasASatisfiedTargetDose(PatientSeries patientSeries) {
    if (patientSeries.getTargetDoseList() == null) {
      return false;
    }
    for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
      if (TargetDoseStatus.SATISFIED.equals(targetDose.getTargetDoseStatus())) {
        return true;
      }
    }
    return false;
  }

  private class LT extends LogicTable {
    public LT() {
      super(3, 3, "Table 8-5 Which scorable patient series should be scored? ");

      setLogicCondition(0, new LogicCondition("Are there 2 or more complete patient series in the series group?") {
        @Override
        protected LogicResult evaluateInternal() {
          int completePatientSeries = completePatientSeriesCount();
          log("Complete patient series count: " + completePatientSeries);
          return completePatientSeries > 1 ? YES : NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Are there 2 or more in-process patient series and no complete patient series in the series group?") {
        @Override
        protected LogicResult evaluateInternal() {
          int completePatientSeries = completePatientSeriesCount();
          int inProcessPatientSeries = inProcessPatientSeriesCount();
          log("In-process patient series count: " + inProcessPatientSeries);
          log("Complete patient series count: " + completePatientSeries);
          return inProcessPatientSeries > 1 && completePatientSeries == 0 ? YES : NO;
        }
      });

      setLogicCondition(2,
          new LogicCondition("Is the number of valid doses = 0 for all scorable patient series in the series group?") {
            @Override
            protected LogicResult evaluateInternal() {
              int withValidDoses = patientSeriesWithValidDosesCount();
              log("Count of patient series with valid doses: " + withValidDoses);
              return withValidDoses == 0 ? YES : NO;
            }
          });

      setLogicResults(0, YES, NO, NO);
      setLogicResults(1, ANY, YES, NO);
      setLogicResults(2, ANY, NO, YES);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Apply complete patient series scoring business rules to all complete patient series. "
              + "Inprocess patient series and patient series with 0 valid doses are not scored and dropped from consideration");
          setNextLogicStepType(LogicStepType.COMPLETE_PATIENT_SERIES);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Apply in-process patient series scoring business rules to all in-process patient series. "
              + "Patient Series with 0 valid doses are not scored and dropped from consideration.");
          setNextLogicStepType(LogicStepType.IN_PROCESS_PATIENT_SERIES);
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Apply no valid doses scoring business rules to all patient series.");
          setNextLogicStepType(LogicStepType.NO_VALID_DOSES);
        }
      });

      // Table 8-5 has no column for mixed complete + in-process groups (one of
      // each, or one complete and several in-process). The historical
      // pre-evaluation default sent those groups to 8.6, which scores them as
      // if they had no valid doses. Prefer complete scoring when any complete
      // series is present, otherwise in-process scoring.
      setLogicOutcomeDefault(new LogicOutcome() {
        @Override
        public void perform() {
          int completePatientSeries = completePatientSeriesCount();
          int inProcessPatientSeries = inProcessPatientSeriesCount();
          log("Table 8-5 matched no column. complete=" + completePatientSeries + " in-process="
              + inProcessPatientSeries);
          // One complete + one in-process matches no column. Prefer complete
          // scoring over the historical NO_VALID_DOSES pre-evaluation default.
          // Do not invent an in-process branch for a lone in-process series:
          // Role A tests assert that Rule 2 does not fire in that shape.
          if (completePatientSeries > 0) {
            setNextLogicStepType(LogicStepType.COMPLETE_PATIENT_SERIES);
          } else {
            setNextLogicStepType(LogicStepType.NO_VALID_DOSES);
          }
        }
      });
    }
  }
}
