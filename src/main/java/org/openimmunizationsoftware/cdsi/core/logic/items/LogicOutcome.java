package org.openimmunizationsoftware.cdsi.core.logic.items;

import java.util.ArrayList;
import java.util.List;

public abstract class LogicOutcome
{
  public abstract void perform();
  
  private List<String> logList = new ArrayList<String>();
  
  public List<String> getLogList() {
    return logList;
  }

  protected void log(String s)
  {
    if (logList != null)
    {
      logList.add(s);
    }
  }
}
