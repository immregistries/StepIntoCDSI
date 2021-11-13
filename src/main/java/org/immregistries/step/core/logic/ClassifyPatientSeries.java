package org.immregistries.step.core.logic;


import static org.immregistries.step.core.logic.items.LogicResult.ANY;
import static org.immregistries.step.core.logic.items.LogicResult.NO;
import static org.immregistries.step.core.logic.items.LogicResult.YES;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.immregistries.step.core.data.DataModel;
import org.immregistries.step.core.domain.datatypes.PatientSeriesStatus;
import org.immregistries.step.core.domain.datatypes.TargetDoseStatus;
import org.immregistries.step.core.logic.items.LogicCondition;
import org.immregistries.step.core.logic.items.LogicOutcome;
import org.immregistries.step.core.logic.items.LogicResult;
import org.immregistries.step.core.logic.items.LogicTable;
import org.immregistries.step.domain.PatientSeries;
import org.immregistries.step.domain.TargetDose;

public class ClassifyPatientSeries extends LogicStep {

  // private ConditionAttribute<Date> caDateAdministered = null;

  public ClassifyPatientSeries(DataModel dataModel) {
    super(LogicStepType.CLASSIFY_PATIENT_SERIES, dataModel);
    // setConditionTableName("Table ");

    // caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date
    // Administered");

    // caTriggerAgeDate.setAssumedValue(FUTURE);

    // conditionAttributesList.add(caDateAdministered);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.NO_VALID_DOSES);

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
        "<p>Classify  patient series  is an attempt to reduce  the total number of  patient series  to only those  which have  a chance to be selected as the best patient series.</p>");

    // printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(3, 3, "Table 6-4 : Which patient series should be scored ? ");

      setLogicCondition(0, new LogicCondition("2 or more complete patient series?") {

        @Override
        protected LogicResult evaluateInternal() {
          // TODO Auto-generated method stub
          int completePatientSeries = 0;
          List<PatientSeries> patientSeriesList = dataModel.getPatientSeriesList();
          for (PatientSeries patientSeries : patientSeriesList) {
            if (patientSeries != null) {
              if (patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)) {
                completePatientSeries++;
              }
            }
          }
          if (completePatientSeries > 1) {
            return LogicResult.YES;
          } else {
            return LogicResult.NO;
          }
        }
      });

      setLogicCondition(1, new LogicCondition(
          "2 or more in-process patient series and 0 are complete patient series ?") {

        @Override
        protected LogicResult evaluateInternal() {
          // TODO Auto-generated method stub
          int completePatientSeries = 0;
          int inProcessPatientSeries = 0;
          List<PatientSeries> patientSeriesList = dataModel.getPatientSeriesList();
          for (PatientSeries patientSeries : patientSeriesList) {
            if (patientSeries != null) {
              if (patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)) {
                completePatientSeries++;
              }
            }
          }

          /**
           * An in-process patient series must be a patient series with at least one target dose
           * status satisfied and the patient series status not complete.
           */
          List<String> antigenSerieNameWithASatisfiedTargetDose = new ArrayList<String>();

          List<TargetDose> targetDoseList = dataModel.getTargetDoseList();
          for (TargetDose targetDose : targetDoseList) {
            if (targetDose.getTargetDoseStatus() != null) {
              if (targetDose.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {
                String antigenSeriesName1 =
                    targetDose.getTrackedSeriesDose().getAntigenSeries().getSeriesName();
                antigenSerieNameWithASatisfiedTargetDose.add(antigenSeriesName1);
              }
            }
          }

          List<String> antigenSerieNameWithANotCompletePatientSerieStatus = new ArrayList<String>();

          List<PatientSeries> ps2 = dataModel.getPatientSeriesList();
          for (PatientSeries ps : ps2) {
            if (ps != null) {
              if (ps.getPatientSeriesStatus().equals(PatientSeriesStatus.NOT_COMPLETE)) {
                String antigenSeriesName2 = ps.getTrackedAntigenSeries().getSeriesName();
                antigenSerieNameWithANotCompletePatientSerieStatus.add(antigenSeriesName2);
              }
            }

          }

          antigenSerieNameWithANotCompletePatientSerieStatus
              .retainAll(new HashSet<String>(antigenSerieNameWithASatisfiedTargetDose));

          inProcessPatientSeries = antigenSerieNameWithANotCompletePatientSerieStatus.size();


          if (inProcessPatientSeries > 1 && completePatientSeries == 0) {
            return LogicResult.YES;
          } else {
            return LogicResult.NO;
          }
        }
      });

      setLogicCondition(2, new LogicCondition("All patient series have 0 valid doses ?") {

        @Override
        protected LogicResult evaluateInternal() {
          // A valid dose is a dose with a satisfied target dose
          List<TargetDose> targetDoseList = dataModel.getTargetDoseList();
          boolean isThereAVlidDose = false;
          for (TargetDose targetDose : targetDoseList) {
            if (targetDose.getTargetDoseStatus() != null) {
              if (targetDose.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {
                isThereAVlidDose = true;
              }
            }
          }
          if (!isThereAVlidDose) {
            return LogicResult.YES;
          } else {
            return LogicResult.NO;
          }

        }
      });


      setLogicResults(0, YES, NO, NO);
      setLogicResults(1, ANY, YES, NO);
      setLogicResults(2, ANY, NO, YES);

      setLogicOutcome(0, new LogicOutcome() {

        @Override
        public void perform() {
          log("Apply complete patient series scoring business rules to all complete patient series. Inprocess patient series and patient series with 0 valid doses are not scored and dropped from consideration");
          setNextLogicStepType(LogicStepType.COMPLETE_PATIENT_SERIES);

        }
      });

      setLogicOutcome(1, new LogicOutcome() {

        @Override
        public void perform() {
          log("Apply in-process patient series scoring business rules to all in-process patient series. Patient Series with 0 valid doses are not scored and dropped from consideration.");
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



      // setLogicCondition(0, new LogicCondition("date administered > lot expiration date?") {
      // @Override
      // public LogicResult evaluateInternal() {
      // if (caDateAdministered.getFinalValue() == null || caTriggerAgeDate.getFinalValue() == null)
      // {
      // return LogicResult.NO;
      // }
      // if (caDateAdministered.getFinalValue().before(caTriggerAgeDate.getFinalValue())) {
      // return LogicResult.YES;
      // }
      // return LogicResult.NO;
      // }
      // });

      // setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.ANY);

      // setLogicOutcome(0, new LogicOutcome() {
      // @Override
      // public void perform() {
      // log("No. The target dose cannot be skipped. ");
      // log("Setting next step: 4.3 Substitute Target Dose");
      // setNextLogicStep(LogicStep.SUBSTITUTE_TARGET_DOSE_FOR_EVALUATION);
      // }
      // });
      //
    }
  }
}
