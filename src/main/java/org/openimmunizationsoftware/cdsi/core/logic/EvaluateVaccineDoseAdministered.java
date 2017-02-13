package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.RecurringDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class EvaluateVaccineDoseAdministered extends LogicStep {
  public EvaluateVaccineDoseAdministered(DataModel dataModel) {
    super(LogicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    LogicStepType nextLogicStep;

    dataModel.incAntigenAdministeredRecordPos();

    if (dataModel.getAntigenAdministeredRecordPos() < dataModel.getAntigenAdministeredRecordList().size()) {
      log("   Looking at first dose administered");
      dataModel.setAntigenAdministeredRecord(
          dataModel.getAntigenAdministeredRecordList().get(dataModel.getAntigenAdministeredRecordPos()));
      if (gotoNextTargetDose()) {
        nextLogicStep = LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION;
      } else {
        nextLogicStep = LogicStepType.FORECAST_DATES_AND_REASONS;
      }
    } else {
      nextLogicStep = LogicStepType.FORECAST_DATES_AND_REASONS;
    }

    return LogicStepFactory.createLogicStep(nextLogicStep, dataModel);
  }

  private boolean gotoNextTargetDose() {
    if (dataModel.getTargetDose() == null) {
      log(" + Getting first target dose");
      dataModel.incTargetDoseListPos();
      dataModel.setTargetDose(dataModel.getTargetDoseList().get(dataModel.getTargetDoseListPos()));
      return true;
    } else {
      if (dataModel.getTargetDose().getSatisfiedByVaccineDoseAdministered() != null) {
        log(" + Previous target dose was satisifed, getting next target dose");
        RecurringDose recurringdose = dataModel.getTargetDose().getTrackedSeriesDose().getRecurringDose();
        if (recurringdose != null && recurringdose.getValue() == YesNo.YES) {
          // Create another target dose
          TargetDose targetDoseNext = new TargetDose(dataModel.getTargetDose());
          dataModel.getTargetDoseList().add(targetDoseNext);
        }
        dataModel.incTargetDoseListPos();
        if (dataModel.getTargetDoseListPos() < dataModel.getTargetDoseListPos()) {
          dataModel.setTargetDose(dataModel.getTargetDoseList().get(dataModel.getTargetDoseListPos()));
          return true;
        } else {
          markRestAsExtraneous();
          return false;
        }
      } else {
          log(" + Previous target dose was NOT satisifed, staying on this target dose");
        return true;
      }
    }
  }

  private AntigenAdministeredRecord findNextAntigenAdministeredRecord(AntigenAdministeredRecord aar) {
    AntigenAdministeredRecord aarNext = null;
    boolean found = false;
    for (AntigenAdministeredRecord antigenAdministeredRecord : dataModel.getAntigenAdministeredRecordList()) {
      if (found) {
        aarNext = antigenAdministeredRecord;
        break;
      } else if (antigenAdministeredRecord == aar) {
        found = true;
      }
    }
    return aarNext;
  }

  private void markRestAsExtraneous() {
    for (int i = dataModel.getAntigenAdministeredRecordPos() + 1; i < dataModel.getAntigenAdministeredRecordList()
        .size(); dataModel.incAntigenAdministeredRecordPos()) {
      dataModel.getAntigenAdministeredRecordList().get(i).getEvaluation()
          .setEvaluationStatus(EvaluationStatus.EXTRANEOUS);
    }
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  private void printStandard(PrintWriter out) {
    out.println("<h1>Evaluate Vaccine Dose Administered</h1>");
    out.println(
        "<p>The core of a CDS engine is the process of evaluating a single vaccine dose administered against  a  defined  target  dose to  determine  if  the  vaccine  dose  administered is valid or not valid. The  results  will  ultimately determine if all conditions of the target dose are satisfied and the dose does not need to be repeated.</p>");
    out.println("<img src=\"Figure 4.1.png\"/>");
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
  }

}
