package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
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

    PatientSeries patientSeries = dataModel.getPatientSeries();

    while (dataModel.getAntigenAdministeredRecordPos() < dataModel
        .getAntigenAdministeredRecordList().size()) {
      AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecordList()
          .get(dataModel.getAntigenAdministeredRecordPos());
      if (aar.getAntigen().equals(patientSeries.getTrackedAntigenSeries().getTargetDisease())) {
        break;
      } else {
        dataModel.incAntigenAdministeredRecordPos();
        log("   Skipping " + aar + " for antigen " + aar.getAntigen());
      }
    }

    if (dataModel.getAntigenAdministeredRecordPos() < dataModel.getAntigenAdministeredRecordList()
        .size()) {
      if (dataModel.getAntigenAdministeredRecordPos() == 0) {
        log("   Looking at first dose administered");
      }
      dataModel.setAntigenAdministeredRecord(dataModel.getAntigenAdministeredRecordList()
          .get(dataModel.getAntigenAdministeredRecordPos()));
      if (gotoNextTargetDose()) {
        nextLogicStep = LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION;
      } else {
        nextLogicStep = LogicStepType.FORECAST_DATES_AND_REASONS;
      }
    } else {
      gotoNextTargetDose();
      nextLogicStep = LogicStepType.FORECAST_DATES_AND_REASONS;
    }

    return LogicStepFactory.createLogicStep(nextLogicStep, dataModel);
  }

  private boolean gotoNextTargetDose() {
    if (dataModel.getTargetDose() == null) {
      //// System.out.println(" + Getting first target dose");
      log(" + Getting first target dose");
      dataModel.incTargetDoseListPos();
      dataModel.setTargetDose(dataModel.getTargetDoseList().get(dataModel.getTargetDoseListPos()));
      return true;
    } else {
      //// System.err.println("-->"+dataModel.getTargetDose().getStatusCause());
      // System.err.println("============"+
      //// dataModel.getTargetDose().getSatisfiedByVaccineDoseAdministered());

      if (dataModel.getTargetDose().getSatisfiedByVaccineDoseAdministered() != null) {
        // System.err.println("-->"+dataModel.getTargetDose().getStatusCause());
        //// System.out.println("+++++++++++++++++++ Previous target dose was satisifed, getting
        //// next target dose");
        log(" + Previous target dose was satisifed, getting next target dose");
        RecurringDose recurringdose =
            dataModel.getTargetDose().getTrackedSeriesDose().getRecurringDose();
        if (recurringdose != null && recurringdose.getValue() == YesNo.YES) {
          // Create another target dose
          TargetDose targetDoseNext = new TargetDose(dataModel.getTargetDose());
          dataModel.getTargetDoseList().add(targetDoseNext);
        }
        dataModel.incTargetDoseListPos();
        if (dataModel.getTargetDoseListPos() < dataModel.getTargetDoseList().size()) {
          dataModel
              .setTargetDose(dataModel.getTargetDoseList().get(dataModel.getTargetDoseListPos()));
          ////// System.err.println("------------------>"+dataModel.getTargetDose().getTrackedSeriesDose().getDoseNumber());
          // dataModel.setAntigenAdministeredRecordPos(0);
          return true;
        } else {
          markRestAsExtraneous();
          return false;
        }
      } else {
        //// System.out.println("++++++++++++++++++ Previous target dose was NOT satisifed, staying
        //// on this target dose");
        log(" + Previous target dose was NOT satisifed, staying on this target dose");
        return true;
      }
    }
  }

  private AntigenAdministeredRecord findNextAntigenAdministeredRecord(
      AntigenAdministeredRecord aar) {
    AntigenAdministeredRecord aarNext = null;
    boolean found = false;
    for (AntigenAdministeredRecord antigenAdministeredRecord : dataModel
        .getAntigenAdministeredRecordList()) {
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
    for (int i = dataModel.getAntigenAdministeredRecordPos() + 1; i < dataModel
        .getAntigenAdministeredRecordList().size(); dataModel.incAntigenAdministeredRecordPos()) {
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
        "<p>The core of a CDS engine is the process of evaluating a single vaccine dose administered against a defined target dose to determine if the vaccine dose administered is “valid” or “not valid.” The results will ultimately determine if all conditions of the target dose are satisfied and the dose does not need to be repeated.  This can be accomplished by breaking the evaluation process into simple and logical components.  After processing each logical component, the results of those logical components are used to determine if the vaccine dose administered satisfies the goals of the target dose.</p>");
    out.println(
        "<p>Each logical component has its own set of business rules that are used to determine if a target dose is \"satisfied.\" These business rules are documented using the decision table format. (See section 3.5 to review an example of a decision table using a real-world scenario.)  The decision table describes the way that the CDS engine responds to various combinations of conditions. The implementer is able to clearly see the set of conditions, how they work in combination, and what actions should be taken on a given set of conditions.</p>");
    out.println(
        "<p>Specific attributes and decision tables are provided for each step of the evaluation process.</p>");

    out.println("<img src=\"TABLE 4 - 1 EVALUATION PROCESS STEPS.PNG\"/>");
    out.println("<p>TABLE 4 - 1 EVALUATION PROCESS STEPS</p>");
    out.println("<img src=\"Figure 4.1.png\"/>");
    out.println("<p>FIGURE 4 - 1 EVALUATION PROCESS MODEL</p>");

    if (dataModel.getTargetDose() != null) {
      out.println("<p>Target dose is dose number #"
          + dataModel.getTargetDose().getTrackedSeriesDose().getDoseNumber() + "</p>");
    }
    if (dataModel.getAntigenAdministeredRecord() != null) {
      out.println("<p>Antigen  "
          + dataModel.getAntigenAdministeredRecord().getVaccineDoseAdministered() + "</p>");
    }
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
  }

}
