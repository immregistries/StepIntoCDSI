package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class Vaccine
{
  private Date lotExpirationDate = null;
  private String manufacturer = "";
  private String tradeName = "";
  private VaccineType vaccineType = null;
  private TimePeriod vaccineTypeBeginAge = null;
  private TimePeriod vaccineTypeEndAge = null;
  private String volume = "";
  private List<Antigen> antigenList = new ArrayList<Antigen>();
  private List<SeriesDose> preferableVaccineForSeries = new ArrayList<SeriesDose>();
  private List<SeriesDose> allowableVaccineForSeries = new ArrayList<SeriesDose>();

  public Antigen getAntigen() {
    if (antigenList.size() > 0) {
      return antigenList.get(0);
    }
    return null;
  }

  public void setAntigen(Antigen antigen) {
    antigenList.add(antigen);
  }

  public Date getLotExpirationDate() {
    return lotExpirationDate;
  }

  public void setLotExpirationDate(Date lotExpirationDate) {
    this.lotExpirationDate = lotExpirationDate;
  }

  public String getManufacturer() {
    return manufacturer;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public String getTradeName() {
    return tradeName;
  }

  public void setTradeName(String tradeName) {
    this.tradeName = tradeName;
  }

  public VaccineType getVaccineType() {
    return vaccineType;
  }

  public void setVaccineType(VaccineType vaccineType) {
    this.vaccineType = vaccineType;
  }

  public TimePeriod getVaccineTypeBeginAge() {
    return vaccineTypeBeginAge;
  }

  public void setVaccineTypeBeginAge(TimePeriod vaccineTypeBeginAge) {
    this.vaccineTypeBeginAge = vaccineTypeBeginAge;
  }

  public TimePeriod getVaccineTypeEndAge() {
    return vaccineTypeEndAge;
  }

  public void setVaccineTypeEndAge(TimePeriod vaccineTypeEndAge) {
    this.vaccineTypeEndAge = vaccineTypeEndAge;
  }

  public String getVolume() {
    return volume;
  }

  public void setVolume(String volume) {
    this.volume = volume;
  }

  public List<Antigen> getAntigenList() {
    return antigenList;
  }

  public void setAntigenList(List<Antigen> antigenList) {
    this.antigenList = antigenList;
  }

  public List<SeriesDose> getPreferableVaccineForSeries() {
    return preferableVaccineForSeries;
  }

  public void setPreferableVaccineForSeries(List<SeriesDose> preferableVaccineForSeries) {
    this.preferableVaccineForSeries = preferableVaccineForSeries;
  }

  public List<SeriesDose> getAllowableVaccineForSeries() {
    return allowableVaccineForSeries;
  }

  public void setAllowableVaccineForSeries(List<SeriesDose> allowableVaccineForSeries) {
    this.allowableVaccineForSeries = allowableVaccineForSeries;
  }
}
