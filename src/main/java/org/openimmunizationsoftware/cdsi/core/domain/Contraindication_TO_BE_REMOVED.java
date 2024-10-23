package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;


public class Contraindication_TO_BE_REMOVED {
  private Antigen antigen = null;
  private String contraindicationLanguage = "";
  private Concept concept = new Concept();

  @Override
  public String toString() {
    return antigen.getName() + ": " + contraindicationLanguage;
  }

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

  public List<VaccineType> getCvxList() {
    return cvxList;
  }

  public void setCvxList(List<VaccineType> cvxList) {
    this.cvxList = cvxList;
  }

  public Concept getConcept() {
    return concept;
  }

  public void setConcept(Concept concept) {
    this.concept = concept;
  }

}
