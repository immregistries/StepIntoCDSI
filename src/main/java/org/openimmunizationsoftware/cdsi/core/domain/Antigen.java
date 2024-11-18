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

  // TODO get rid of this method
  public String getCvxForForecast() {
    if (name.equals("Cholera")) {
      return "26";
    }
    if (name.equals("Diphtheria")) {
      return "20";
    }
    if (name.equals("HepA")) {
      return "85";
    }
    if (name.equals("HepB")) {
      return "45";
    }
    if (name.equals("Hib")) {
      return "17";
    }
    if (name.equals("HPV")) {
      return "137";
    }
    if (name.equals("Influenza")) {
      return "88";
    }
    if (name.equals("Japanese Encephalitis")) {
      return "129";
    }
    if (name.equals("Measles")) {
      return "05";
    }
    if (name.equals("Meningococcal")) {
      return "108";
    }
    if (name.equals("Meningococcal B")) {
      return "164";
    }
    if (name.equals("Mumps")) {
      return "07";
    }
    if (name.equals("Pertussis")) {
      return "11";
    }
    if (name.equals("Pneumococcal")) {
      return "109";
    }
    if (name.equals("Polio")) {
      return "89";
    }
    if (name.equals("Rabies")) {
      return "90";
    }
    if (name.equals("Rotavirus")) {
      return "122";
    }
    if (name.equals("Rubella")) {
      return "06";
    }
    if (name.equals("Tetanus")) {
      return "112";
    }
    if (name.equals("Typhoid")) {
      return "91";
    }
    if (name.equals("Varicella")) {
      return "21";
    }
    if (name.equals("Yellow Fever")) {
      return "37";
    }
    if (name.equals("Zoster")) {
      return "188";
    }
    return "XX";
  }

}
