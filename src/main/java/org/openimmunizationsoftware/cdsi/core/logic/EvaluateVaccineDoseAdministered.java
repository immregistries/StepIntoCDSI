package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.RecurringDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class EvaluateVaccineDoseAdministered extends LogicStep
{
  private boolean started = false;

  public EvaluateVaccineDoseAdministered(DataModel dataModel) {
    super(LogicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED, dataModel);
  }

  private List<String> logList = new ArrayList<String>();

  @Override
  public LogicStep process() throws Exception {
    LogicStepType nextLogicStep;
    if (!started) {
      logList.add(" + Get First Target Dose");
      dataModel.setTargetDose(dataModel.getTargetDoseList().get(0));
      logList.add(" + Get First Antigen Administered Record if present");
      if (dataModel.getAntigenAdministeredRecordList().size() == 0) {
        logList.add("   No Doses Administered");
        logList.add("   Finished evaluating administered doses");
        nextLogicStep = LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST;
      } else {
        logList.add("   Looking at first dose administered");
        dataModel.setAntigenAdministeredRecord(dataModel.getAntigenAdministeredRecordList().get(0));
        nextLogicStep = LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION;
      }
      started = true;
    } else {
      TargetDose targetDose = dataModel.getTargetDose();
      AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
      AntigenAdministeredRecord aarNext = findNextAntigenAdministeredRecord(aar);
      TargetDose targetDoseNext = dataModel.findNextTargetDose(targetDose);
      nextLogicStep = LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST;
      if (targetDose.getTargetDoseStatus() == TargetDoseStatus.SATISFIED) {
        // Is there another Target Dose to process?
        if (targetDoseNext == null) {
          // Is this target dose a recurring dose?
          RecurringDose recurringdose = targetDose.getTrackedSeriesDose().getRecurringDose();
          if (recurringdose != null && recurringdose.getValue() == YesNo.YES) {
            // Create another target dose
            targetDoseNext = new TargetDose(targetDose);
            dataModel.getTargetDoseList().add(targetDoseNext);
          } else {
            markRestAsExtraneous(aar);
          }
        }
        targetDose = targetDoseNext;
        dataModel.setTargetDose(targetDose);
      }
      if (targetDose != null && aarNext != null) {
        aar = aarNext;
        nextLogicStep = LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION;
      } else {
        aar = null;
      }
      dataModel.setTargetDose(targetDose);
      if (dataModel.getAntigenAdministeredRecord() != null) {
        if (aar == null || aar != dataModel.getAntigenAdministeredRecord()) {
          dataModel.setPreviousAntigenAdministeredRecord(dataModel.getAntigenAdministeredRecord());
        }
      }
      dataModel.setAntigenAdministeredRecord(aar);
    }

    return LogicStepFactory.createLogicStep(nextLogicStep, dataModel);
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

  private AntigenAdministeredRecord markRestAsExtraneous(AntigenAdministeredRecord aar) {
    AntigenAdministeredRecord aarNext = null;
    boolean found = false;
    for (AntigenAdministeredRecord antigenAdministeredRecord : dataModel.getAntigenAdministeredRecordList()) {
      if (found) {
        antigenAdministeredRecord.getEvaluation().setEvaluationStatus(EvaluationStatus.EXTRANEOUS);
      } else if (antigenAdministeredRecord == aar) {
        found = true;
      }
    }
    return aarNext;
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printPrePost(out);
  }

  private void printPrePost(PrintWriter out) {
    out.println("<h1>Evaluate Vaccine Dose Administered</h1>");
    out.println("<p>The core of a CDS engine is the process of evaluating a single vaccine dose administered against  a  defined  target  dose to  determine  if  the  vaccine  dose  administered is valid or not valid. The  results  will  ultimately determine if all conditions of the target dose are satisfied and the dose does not need to be repeated.</p>");
    out.println("<img src=\"Figure 4.1.png\"/>");
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printPrePost(out);
  }

}
