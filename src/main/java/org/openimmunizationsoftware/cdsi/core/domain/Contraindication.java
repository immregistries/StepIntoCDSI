package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;


public class Contraindication {
  private String contraindicationTextDescription = "";
  private Age contraindicationBeginAge = null;
  private Age contraindicationEndAge = null;
  private Antigen antigen = null;
  private Concept concept = new Concept();
  private AntigenContraindication antigenContraindication = null;
  private VaccineContraindication vaccineContraindication = null;
  private Schedule schedule = null;

  public Schedule getSchedule() {
    return schedule;
  }

  public void setSchedule(Schedule schedule) {
    this.schedule = schedule;
  }

  public Age getContraindicationBeginAge() {
    return contraindicationBeginAge;
  }

  public void setContraindicationBeginAge(Age contraindicationBeginAge) {
    this.contraindicationBeginAge = contraindicationBeginAge;
  }

  public Age getContraindicationEndAge() {
    return contraindicationEndAge;
  }

  public void setContraindicationEndAge(Age contraindicationEndAge) {
    this.contraindicationEndAge = contraindicationEndAge;
  }

  public AntigenContraindication getAntigenContraindication() {
    return antigenContraindication;
  }

  public void setAntigenContraindication(AntigenContraindication antigenContraindication) {
    this.antigenContraindication = antigenContraindication;
  }

  public VaccineContraindication getVaccineContraindication() {
    return vaccineContraindication;
  }

  public void setVaccineContraindication(VaccineContraindication vaccineContraindication) {
    this.vaccineContraindication = vaccineContraindication;
  }

  @Override
  public String toString() {
    return antigen.getName() + ": " + contraindicationTextDescription;
  }

  private List<VaccineType> cvxList = new ArrayList<VaccineType>();

  public Antigen getAntigen() {
    return antigen;
  }

  public void setAntigen(Antigen antigen) {
    this.antigen = antigen;
  }

  public String getContraindicationTextDescription() {
    return contraindicationTextDescription;
  }

  public void setContraindicationTextDescription(String contraindicationLanguage) {
    this.contraindicationTextDescription = contraindicationLanguage;
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
