package org.openimmunizationsoftware.cdsi.core.logic.items;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

/**
 * Created by Eric on 7/1/16.
 */
public abstract class BusinessRule<T, S> {

  private String businessRuleId = "";
  private String term = "";
  private String businessRuleText = "";
  private List<String> logList = new ArrayList<String>();

  public List<String> getLogList() {
    return logList;
  }

  public void log(String s) {
    logList.add(s);
  }

  public String getBusinessRuleId() {
    return businessRuleId;
  }

  public void setBusinessRuleId(String businessRuleId) {
    this.businessRuleId = businessRuleId;
  }

  public String getTerm() {
    return term;
  }

  public void setTerm(String term) {
    this.term = term;
  }

  public String getBusinessRuleText() {
    return businessRuleText;
  }

  public void setBusinessRuleText(String businessRuleText) {
    this.businessRuleText = businessRuleText;
  }

  public abstract T evaluate(DataModel dataModel, S s);
}
