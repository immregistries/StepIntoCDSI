package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;

public class AntigenAdministeredRecord
{
  private Antigen antigen = null;
  private Date dateAdministered = null;
  private VaccineType vaccineType = null;
  private String manufacturer = "";
  private String tradeName = "";
  private String amount = "";
  private Date lotExpirationDate = null;
  private DoseCondition doseCondition = null;
  private Evaluation evaluation = new Evaluation();
  public Evaluation getEvaluation() {
    
    return evaluation;
  }

  public AntigenAdministeredRecord() {
    // default
  }

  public AntigenAdministeredRecord(VaccineDoseAdministered vda, Antigen antigen) {
    this.antigen = antigen;
    this.dateAdministered = vda.getDateAdministered();
    this.vaccineType = vda.getVaccine().getVaccineType();
    this.manufacturer = vda.getVaccine().getManufacturer();
    this.tradeName = vda.getVaccine().getTradeName();
    this.amount = vda.getVaccine().getVolume();
    this.lotExpirationDate = vda.getVaccine().getLotExpirationDate();
    this.doseCondition = vda.getDoseCondition();
  }

  public Antigen getAntigen() {
    return antigen;
  }

  public void setAntigen(Antigen antigen) {
    this.antigen = antigen;
  }

  public Date getDateAdministered() {
    return dateAdministered;
  }

  public void setDateAdministered(Date dateAdministered) {
    this.dateAdministered = dateAdministered;
  }

  public VaccineType getVaccineType() {
    return vaccineType;
  }

  public void setVaccineType(VaccineType vaccineType) {
    this.vaccineType = vaccineType;
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

  public String getAmount() {
    return amount;
  }
  
  public String getVolume()
  {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public Date getLotExpirationDate() {
    return lotExpirationDate;
  }

  public void setLotExpirationDate(Date lotExpirationDate) {
    this.lotExpirationDate = lotExpirationDate;
  }

  public DoseCondition getDoseCondition() {
    return doseCondition;
  }

  public void setDoseCondition(DoseCondition doseCondition) {
    this.doseCondition = doseCondition;
  }
}
