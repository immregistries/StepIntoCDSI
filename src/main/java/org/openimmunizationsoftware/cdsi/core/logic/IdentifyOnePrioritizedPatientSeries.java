package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.MORE_THAN_ONE;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ZERO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ONE;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class IdentifyOnePrioritizedPatientSeries extends LogicStep {

  // private ConditionAttribute<Date> caDateAdministered = null;

  public IdentifyOnePrioritizedPatientSeries(DataModel dataModel) {
    super(LogicStepType.ONE_BEST_PATIENT_SERIES, dataModel);
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
    out.println("<h1> " + getTitle() + "</h1>");
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
          setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
        }
      });

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single default patient series is the prioritized patient series for the series group.");
          setNextLogicStepType(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single scorable patient series is the prioritized patient series for the series group.");
          setNextLogicStepType(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES);
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single scorable patient series is the prioritized patient series for the series group.");
          setNextLogicStepType(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES);
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The single in-process patient series is the prioritized patient series for the series group.");
          setNextLogicStepType(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES);
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The default patient series is the prioritized patient series for the series group.");
          setNextLogicStepType(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES);
        }
      });

    }
  }

  private class LTOld extends LogicTable {
    public LTOld() {
      super(4, 5, "Table 8-3 Is there a single prioritized patient series in a series group ? ");

      setLogicCondition(0, new LogicCondition("Antigen contains only one patient series ?") {
        @Override
        protected LogicResult evaluateInternal() {
          int numberOfPatientSeries = 0;
          for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
            if (patientSeries.getTrackedAntigenSeries().getTargetDisease()
                .equals(dataModel.getAntigen())) {
              numberOfPatientSeries++;
            }
          }
          if (numberOfPatientSeries == 1) {
            return YES;
          } else {
            return NO;
          }

        }
      });

      setLogicCondition(1, new LogicCondition("Patient has only 1 complete patient series?") {
        @Override
        protected LogicResult evaluateInternal() {
          int completePatientSeries = 0;
          List<PatientSeries> psl = dataModel.getScorablePatientSeriesList();
          for (PatientSeries ps : psl) {
            if (ps.getPatientSeriesStatus() == null) {
              continue;
            }
            if (ps.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)) {
              completePatientSeries++;
            }
          }
          if (completePatientSeries == 1) {
            return YES;
          } else {
            return NO;
          }
        }
      });

      setLogicCondition(2, new LogicCondition(
          "Patient has only 1 in-process patient series and no complete patient series ?") {

        @Override
        protected LogicResult evaluateInternal() {

          int notCompletePatientSeries = 0;
          List<PatientSeries> psl = dataModel.getScorablePatientSeriesList();
          for (PatientSeries ps : psl) {
            if (ps.getPatientSeriesStatus() == null) {
              continue;
            }
            if (ps.getPatientSeriesStatus().equals(PatientSeriesStatus.NOT_COMPLETE)) {
              notCompletePatientSeries++;
            }
          }
          /**
           * An in-process patient series must be a patient series with at least one
           * target dose
           * status satisfied and the patient series status not complete.
           */
          List<String> antigenSerieNameWithASatisfiedTargetDose = new ArrayList<String>();

          List<TargetDose> targetDoseList = dataModel.getTargetDoseList();
          if (targetDoseList != null) {
            for (TargetDose targetDose : targetDoseList) {
              if (targetDose.getTargetDoseStatus() != null) {
                if (targetDose.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {
                  String antigenSeriesName1 = targetDose.getTrackedSeriesDose().getAntigenSeries().getSeriesName();
                  antigenSerieNameWithASatisfiedTargetDose.add(antigenSeriesName1);
                }
              }
            }
          }

          List<String> antigenSerieNameWithANotCompletePatientSerieStatus = new ArrayList<String>();

          List<PatientSeries> ps2 = dataModel.getScorablePatientSeriesList();
          for (PatientSeries ps : ps2) {
            if (ps.getPatientSeriesStatus() != null) {
              if (ps.getPatientSeriesStatus().equals(PatientSeriesStatus.NOT_COMPLETE)) {
                String antigenSeriesName2 = ps.getTrackedAntigenSeries().getSeriesName();
                antigenSerieNameWithANotCompletePatientSerieStatus.add(antigenSeriesName2);
              }
            }

          }

          antigenSerieNameWithANotCompletePatientSerieStatus
              .retainAll(new HashSet<String>(antigenSerieNameWithASatisfiedTargetDose));

          int inProcessPatientSeriesNumber = antigenSerieNameWithANotCompletePatientSerieStatus.size();

          if (inProcessPatientSeriesNumber == 1 && notCompletePatientSeries == 0) {
            return YES;
          } else {
            return NO;
          }
        }
      });

      setLogicCondition(3, new LogicCondition(
          "Patient has all Patient Series with 0 valid doses and 1 patient series is identified as the default patient series ?") {
        @Override
        protected LogicResult evaluateInternal() {
          int numberOfDefaultPatientSeries = 0;
          List<AntigenSeries> asl = dataModel.getAntigenSeriesSelectedList();
          for (AntigenSeries as : asl) {
            if (as.getSelectPatientSeries() != null && as.getSelectPatientSeries().getDefaultSeries() != null) {
              boolean isDefaultSeries = as.getSelectPatientSeries().getDefaultSeries().equals(YES);
              if (isDefaultSeries) {
                numberOfDefaultPatientSeries++;
              }
            }

          }
          // A valid dose is a dose with
          List<TargetDose> targetDoseList = dataModel.getTargetDoseList();
          boolean isThereAVlidDose = false;
          if (targetDoseList != null) {
            for (TargetDose targetDose : targetDoseList) {
              if (targetDose.getTargetDoseStatus() != null) {
                if (targetDose.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {
                  isThereAVlidDose = true;
                }
              }
            }
          }
          if (!isThereAVlidDose && numberOfDefaultPatientSeries == 1) {
            return YES;
          } else {
            return NO;
          }
        }
      });

      setLogicResults(0, YES, NO, NO, NO, NO);
      setLogicResults(1, ANY, YES, NO, NO, NO);
      setLogicResults(2, ANY, ANY, YES, NO, NO);
      setLogicResults(3, ANY, ANY, ANY, YES, NO);

      setLogicOutcome(0, new LogicOutcome() {

        @Override
        public void perform() {
          // TODO Auto-generated method stub

          log("Yes. The lone patient series is the best patient series.");
          for (AntigenSeries as : dataModel.getAntigenSeriesSelectedList()) {
            for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
              if (patientSeries.getTrackedAntigenSeries().equals(as)) {
                dataModel.getBestPatientSeriesList().add(patientSeries);
              }
            }
          }
          setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);

        }
      });

      setLogicOutcome(1, new LogicOutcome() {

        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("Yes. The lone complete patient series is the best patient series");
          for (AntigenSeries as : dataModel.getAntigenSeriesSelectedList()) {
            for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
              if (patientSeries.getTrackedAntigenSeries().equals(as)) {
                dataModel.getBestPatientSeriesList().add(patientSeries);
              }
            }
          }
          setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
        }
      });

      setLogicOutcome(2, new LogicOutcome() {

        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("Yes. The lone in-process patient series is the best patient series");
          for (AntigenSeries as : dataModel.getAntigenSeriesSelectedList()) {
            for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
              if (patientSeries.getTrackedAntigenSeries().equals(as)) {
                dataModel.getBestPatientSeriesList().add(patientSeries);
              }
            }
          }
          setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
        }
      });

      setLogicOutcome(3, new LogicOutcome() {

        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("Yes. The lone default patient series is the best patient series");
          for (AntigenSeries as : dataModel.getAntigenSeriesSelectedList()) {
            for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
              if (patientSeries.getTrackedAntigenSeries().equals(as)) {
                dataModel.getBestPatientSeriesList().add(patientSeries);
              }
            }
          }
          setNextLogicStepType(LogicStepType.DETERMINE_BEST_PATIENT_SERIES);
        }
      });

      setLogicOutcome(4, new LogicOutcome() {

        @Override
        public void perform() {
          // TODO Auto-generated method stub
          log("No. More than one patient series has potential. All patient series are examined to see which should be scored and selected as the best patient series");
          setNextLogicStepType(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES);

        }
      });

    }
  }

}
