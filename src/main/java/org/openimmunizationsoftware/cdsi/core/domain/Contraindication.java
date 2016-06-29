package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;


public class Contraindication {
  private Antigen antigen = null;
  private String contraindicationLanguage = "";
  private String concept = "";
  private String conceptCode = "";
  private String conceptText = "";
  private List<VaccineType> cvxList = new ArrayList<VaccineType>();

  public Antigen getAntigen() {
    return antigen;
  }

  public void setAntigen(Antigen antigen) {
    this.antigen = antigen;
  }

  public String getContraindicationLanguage() {
    return contraindicationLanguage;
  }

  public void setContraindicationLanguage(String contraindicationLanguage) {
    this.contraindicationLanguage = contraindicationLanguage;
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

  public List<VaccineType> getCvxList() {
    return cvxList;
  }

  public void setCvxList(List<VaccineType> cvxList) {
    this.cvxList = cvxList;
  }

}
