package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class IdentifyAndEvaluateVaccineGroup extends LogicStep {

  // private ConditionAttribute<Date> caDateAdministered = null;

  public IdentifyAndEvaluateVaccineGroup(DataModel dataModel) {
    super(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    dataModel.incVaccineGroupPos();
    if (dataModel.getVaccineGroupPos() < dataModel.getVaccineGroupList().size()) {
      dataModel
          .setVaccineGroup(dataModel.getVaccineGroupList().get(dataModel.getVaccineGroupPos()));
      setNextLogicStepType(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES);
    } else {
      setNextLogicStepType(LogicStepType.END);
    }
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
        "<p>The  goal  of  identify  and  evaluate  vaccine  group  is  to  merge  together  antigen-based  forecasts  into  vaccine group forecasts. This is especially important in MMR and  DTaP/Tdap/Td vaccine groups which each contain more than one antigen in their respective vaccine groups. In these cases, it is important to provide a forecast consistent  with  the  vaccine  group  rather  than  the  individual  antigen.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

}
