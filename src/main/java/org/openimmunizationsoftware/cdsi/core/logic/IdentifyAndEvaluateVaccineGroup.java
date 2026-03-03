package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel;

public class IdentifyAndEvaluateVaccineGroup extends LogicStep {

  // private ConditionAttribute<Date> caDateAdministered = null;

  public IdentifyAndEvaluateVaccineGroup(DataModel dataModel) {
    super(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    dataModel.incVaccineGroupPos();
    int vaccineGroupPos = dataModel.getVaccineGroupPos();
    int vaccineGroupListSize = dataModel.getVaccineGroupList() != null ? dataModel.getVaccineGroupList().size() : 0;

    log(LogLevel.CONTROL, "IDANDEVAL_VG: Vaccine group iteration; position=" + vaccineGroupPos +
        ", listSize=" + vaccineGroupListSize +
        ", vaccineGroupList_isNull=" + (dataModel.getVaccineGroupList() == null));

    if (vaccineGroupPos < vaccineGroupListSize) {
      dataModel.setVaccineGroup(dataModel.getVaccineGroupList().get(vaccineGroupPos));
      log(LogLevel.STATE, "IDANDEVAL_VG: Processing vaccine group; " +
          "name=" + (dataModel.getVaccineGroup() != null ? dataModel.getVaccineGroup().getName() : "null") +
          ", position=" + vaccineGroupPos);
      setNextLogicStepType(LogicStepType.APPLY_GENERAL_VACCINE_GROUP_RULES);
    } else {
      log(LogLevel.STATE, "IDANDEVAL_VG: All vaccine groups processed, moving to END");
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
    out.println(
        "<p>The  goal  of  identify  and  evaluate  vaccine  group  is  to  merge  together  antigen-based  forecasts  into  vaccine group forecasts. This is especially important in MMR and  DTaP/Tdap/Td vaccine groups which each contain more than one antigen in their respective vaccine groups. In these cases, it is important to provide a forecast consistent  with  the  vaccine  group  rather  than  the  individual  antigen.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

}
