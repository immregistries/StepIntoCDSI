package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class AntigenSeries {
  private String seriesName = "";
  private List<SeriesDose> seriesDoseList = new ArrayList<SeriesDose>();
  private SelectPatientSeries selectPatientSeries = null;
  private Antigen antigen = null;
  private VaccineGroup vaccineGroup = null;
  private String seriesType = "";
  private Indication indication = null;
  private Schedule schedule = null;
  private AdministrativeGuidance administrativeGuidance = null;

  public AdministrativeGuidance getAdministrativeGuidance() {
    return administrativeGuidance;
  }

  public void setAdministrativeGuidance(AdministrativeGuidance administrativeGuidance) {
    this.administrativeGuidance = administrativeGuidance;
  }

  public Indication getIndication() {
    return indication;
  }

  public void setIndication(Indication indication) {
    this.indication = indication;
  }

  public Schedule getSchedule() {
    return schedule;
  }

  public void setSchedule(Schedule schedule) {
    this.schedule = schedule;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AntigenSeries) {
      AntigenSeries as = (AntigenSeries) obj;
      return as.getSeriesName().equals(this.getSeriesName());
    }
    return super.equals(obj);
  }

  public Antigen getAntigen() {
    return antigen;
  }

  public void setAntigen(Antigen targetDisease) {
    this.antigen = targetDisease;
  }

  public VaccineGroup getVaccineGroup() {
    return vaccineGroup;
  }

  public void setVaccineGroup(VaccineGroup vaccineGroup) {
    this.vaccineGroup = vaccineGroup;
  }

  public String getSeriesName() {
    return seriesName;
  }

  public void setSeriesName(String seriesName) {
    this.seriesName = seriesName;
  }

  public List<SeriesDose> getSeriesDoseList() {
    return seriesDoseList;
  }

  public SelectPatientSeries getSelectPatientSeries() {
    return selectPatientSeries;
  }

  public void setSelectPatientSeries(SelectPatientSeries selectBestPatientSeries) {
    this.selectPatientSeries = selectBestPatientSeries;
  }

  public String getSeriesType() {
    return seriesType;
  }

  public void setSeriesType(String seriesType) {
    this.seriesType = seriesType;
  }


}
