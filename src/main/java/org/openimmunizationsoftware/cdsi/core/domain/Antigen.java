package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class Antigen {
  private String name = "";
  private VaccineGroup vaccineGroup = null;
  private List<VaccineType> cvxList = new ArrayList<VaccineType>();
  private List<Immunity> immunityList = new ArrayList<Immunity>();
  private List<Contraindication> contraindicationList = new ArrayList<Contraindication>();
  private List<AntigenSeries> antigenSeriesList = new ArrayList<AntigenSeries>();
  private PatientSeries patientSeries = null;
  private SeriesDose seriesDose = null;
  private List<Vaccine> vaccineList = new ArrayList<Vaccine>();
  private List<Forecast> forecastList = new ArrayList<Forecast>();
  private List<Evaluation> evaluationList = new ArrayList<Evaluation>();

  public List<Evaluation> getEvaluationList() {
    return evaluationList;
  }

  public List<Forecast> getForecastList() {
    return forecastList;
  }

  public List<Vaccine> getVaccineList() {
    return vaccineList;
  }

  public List<AntigenSeries> getAntigenSeriesList() {
    return antigenSeriesList;
  }

  public void setAntigenSeriesList(List<AntigenSeries> antigenSeriesList) {
    this.antigenSeriesList = antigenSeriesList;
  }

  public List<Contraindication> getContraindicationList() {
    return contraindicationList;
  }

  public List<Immunity> getImmunityList() {
    return immunityList;
  }

  public void setImmunityList(List<Immunity> immunityList) {
    this.immunityList = immunityList;
  }

  public List<VaccineType> getCvxList() {
    return cvxList;
  }

  public void setCvxList(List<VaccineType> cvxList) {
    this.cvxList = cvxList;
  }

  public VaccineGroup getVaccineGroup() {
    return vaccineGroup;
  }

  public void setVaccineGroup(VaccineGroup vaccineGroup) {
    this.vaccineGroup = vaccineGroup;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Antigen) {
      Antigen other = (Antigen) obj;
      return other.getName().equals(this.getName());
    }
    return super.equals(obj);
  }

  public PatientSeries getPatientSeries() {
    return patientSeries;
  }

  public void setPatientSeries(PatientSeries patientSeries) {
    this.patientSeries = patientSeries;
  }

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

}
