package org.openimmunizationsoftware.cdsi.core.domain;

public class Concept {
  private String conceptCodeSystem = "";
  private String conceptCode = "";
  private String conceptText = "";

  public String getConceptCodeSystem() {
    return conceptCodeSystem;
  }

  public void setConceptCodeSystem(String conceptCodeSystem) {
    this.conceptCodeSystem = conceptCodeSystem;
  }

  public String getConceptCode() {
    return conceptCode;
  }

  public void setConceptCode(String conceptCode) {
    this.conceptCode = conceptCode;
  }

  public String getConceptText() {
    return conceptText;
  }

  public void setConceptText(String conceptText) {
    this.conceptText = conceptText;
  }
}
