package org.openimmunizationsoftware.cdsi.core.logic.items;

public class ConditionAttribute<param>
{
  public ConditionAttribute(String attributeType, String attributeName) {
    this.attributeType = attributeType;
    this.attributeName = attributeName;
  }

  private String attributeType = "";
  private String attributeName = "";
  private param assumedValue = null;
  private param initialValue = null;

  public String getAttributeType() {
    return attributeType;
  }

  public void setAttributeType(String attributeType) {
    this.attributeType = attributeType;
  }

  public String getAttributeName() {
    return attributeName;
  }

  public void setAttributeName(String attributeName) {
    this.attributeName = attributeName;
  }

  public param getAssumedValue() {
    return assumedValue;
  }

  public void setAssumedValue(param assumedValue) {
    this.assumedValue = assumedValue;
  }

  public param getInitialValue() {
    return initialValue;
  }

  public void setInitialValue(param initialValue) {
    this.initialValue = initialValue;
  }

  public param getFinalValue() {
    return initialValue == null ? assumedValue : initialValue;
  }

}
