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
    return LogicStepFactory.createLogicStep(LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION, dataModel);
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  private void printStandard(PrintWriter out) {
    out.println("<h1>Evaluate Vaccine Dose Administered</h1>");
    out.println(
        "<p>The core of a CDS engine is the process of evaluating a single vaccine dose administered against a defined target dose within a relevant patient series to determine if the vaccine dose administered is \"valid\" or \"not valid\"for the relevant patient series. The results will ultimately determine if all requirements of the target dose are satisfied. This can be accomplished by breaking the evaluation process into simple logical components. After processing each logical component, the results of those logical components are used to determine if the vaccine dose administered satisfies the goals of the target dose.</p>");
    out.println(
        "<p>Each logical component has its own set of business rules that are used to determine if a target dose is “satisfied.” These business rules are documented using business rules and decision tables. (See section 2.11 to review an example of a decision table using a real-world scenario.) The decision table describes the way that the CDS engine responds to various combinations of conditions. The implementer can clearly see the set of conditions, how they work in combination, and what actions should be taken on a given set of conditions.</p>");
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
