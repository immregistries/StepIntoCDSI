package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class VaccineGroupForecast extends Forecast {
  private List<Antigen> antigensNeededList = new ArrayList<Antigen>();
  private VaccineGroupStatus vaccineGroupStatus = null;
  private List<Forecast> forecastList = new ArrayList<Forecast>();
  private VaccineGroup vaccineGroup = null;
  
  public VaccineGroup getVaccineGroup() {
    return vaccineGroup;
  }
  public void setVaccineGroup(VaccineGroup vaccineGroup) {
    this.vaccineGroup = vaccineGroup;
  }
  public List<Forecast> getForecastList() {
    return forecastList;
  }
  public void setForecastList(List<Forecast> forecastList) {
    this.forecastList = forecastList;
  }
  public List<Antigen> getAntigensNeededList() {
    return antigensNeededList;
  }
  public void setAntigensNeededList(List<Antigen> antigensNeededList) {
    this.antigensNeededList = antigensNeededList;
  }
  public VaccineGroupStatus getVaccineGroupStatus() {
    return vaccineGroupStatus;
  }
  public void setVaccineGroupStatus(VaccineGroupStatus vaccineGroupStatus) {
    this.vaccineGroupStatus = vaccineGroupStatus;
  }
}
