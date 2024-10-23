package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class Antigen {
  private String name = "";
  private VaccineGroup vaccineGroup = null;
  private List<VaccineType> vaccineTypeList = new ArrayList<VaccineType>();
  private List<Immunity> immunityList = new ArrayList<Immunity>();

  public List<Immunity> getImmunityList() {
    return immunityList;
  }

  public void setImmunityList(List<Immunity> immunityList) {
    this.immunityList = immunityList;
  }

  public List<VaccineType> getCvxList() {
    return vaccineTypeList;
  }

  public void setCvxList(List<VaccineType> vaccineTypeList) {
    this.vaccineTypeList = vaccineTypeList;
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

}
