package org.openimmunizationsoftware.cdsi.core.logic;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class End extends LogicStep {

  public End(DataModel dataModel) {
    super(LogicStepType.END, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    return null;
  }

}
