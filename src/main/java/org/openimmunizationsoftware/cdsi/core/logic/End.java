package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class End extends LogicStep
{

  public End(DataModel dataModel) {
    super(LogicStepType.END, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    // TODO Auto-generated method stub

  }

}
