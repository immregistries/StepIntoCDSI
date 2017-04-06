package org.openimmunizationsoftware.cdsi.core.logic.concepts;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;

public abstract class DateRule<T> {

  private String businessRuleId = "";
  private String businessRule = "";
  private String logicalComponent = "";
  private String fieldName = "";

  public String getFieldName() {
    return fieldName;
  }

  protected void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getBusinessRuleId() {
    return businessRuleId;
  }

  protected void setBusinessRuleId(String businessRuleId) {
    this.businessRuleId = businessRuleId;
  }

  public String getBusinessRule() {
    return businessRule;
  }

  protected void setBusinessRule(String businessRule) {
    this.businessRule = businessRule;
  }

  public String getLogicalComponent() {
    return logicalComponent;
  }

  protected void setLogicalComponent(String logicalComponent) {
    this.logicalComponent = logicalComponent;
  }

  public Date evaluate(DataModel dataModel, LogicStep logicStep, T t) {
    logicStep.log("Calculating using date rule " + getBusinessRuleId());
    logicStep.log("  + business rule: " + getBusinessRule());
    logicStep.log("  + logical component: " + getLogicalComponent());
    logicStep.log("  + field being returned: " + getFieldName());
    Date value = evaluateInternal(dataModel, logicStep, t);
    if (value == null) {
      logicStep.log("No value calculated");
    } else {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      logicStep.log("Calculated value = " + sdf.format(value));
    }
    return value;
  }

  protected abstract Date evaluateInternal(DataModel dataModel, LogicStep logicStep, T t);

}
