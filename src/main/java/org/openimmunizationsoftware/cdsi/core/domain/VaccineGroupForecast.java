package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;

public class VaccineGroupForecast extends Forecast {
  private List<Antigen> antigensNeededList = new ArrayList<Antigen>();
  private VaccineGroupStatus vaccineGroupStatus = VaccineGroupStatus.NOT_COMPLETE;// null;
  private List<Forecast> forecastList = new ArrayList<Forecast>();
  private VaccineGroup vaccineGroup = null;
  private PatientSeriesStatus patientSeriesStatus = PatientSeriesStatus.NOT_COMPLETE;// null;
  private List<Antigen> antigenList = new ArrayList<Antigen>();

  public List<Antigen> getAntigenList() {
    return antigenList;
  }

  public PatientSeriesStatus getPatientSeriesStatus() {
    return patientSeriesStatus;
  }

  public void setPatientSeriesStatus(PatientSeriesStatus patientSeriesStatus) {
    this.patientSeriesStatus = patientSeriesStatus;
  }

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

  public void setVaccineGroupStatus(PatientSeriesStatus patientSeriesStatus) {
    // TODO Auto-generated method stub

    switch (patientSeriesStatus) {
      case COMPLETE:
        setVaccineGroupStatus(VaccineGroupStatus.COMPLETE);
        break;
      case CONTRAINDICATED:
        setVaccineGroupStatus(VaccineGroupStatus.CONTRAINDICATED);
        break;
      case IMMUNE:
        setVaccineGroupStatus(VaccineGroupStatus.IMMUNE);
        break;
      case NOT_COMPLETE:
        setVaccineGroupStatus(VaccineGroupStatus.NOT_COMPLETE);
        break;
      case NOT_RECOMMENDED:
        setVaccineGroupStatus(VaccineGroupStatus.NOT_RECOMMENDED);
        break;
      case AGED_OUT:
        setVaccineGroupStatus(VaccineGroupStatus.AGED_OUT);
        break;
      default:
        break;
    }
  }
}
