package org.openimmunizationsoftware.cdsi.core.domain;

public class Immunity
{
  private Antigen antigen = null;
  private String immunityLanguage = "";
  private String concept = "";
  private String conceptCode = "";
  private String conceptText = "";

  public Antigen getAntigen() {
    return antigen;
  }

  public void setAntigen(Antigen antigen) {
    this.antigen = antigen;
  }

  public String getImmunityLanguage() {
    return immunityLanguage;
  }

  public void setImmunityLanguage(String immunityLanguage) {
    this.immunityLanguage = immunityLanguage;
  }

  public String getConcept() {
    return concept;
  }

  public void setConcept(String concept) {
    this.concept = concept;
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
