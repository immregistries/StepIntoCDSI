package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class Immunity {
  private List<ClinicalHistory> clinicalHistoryList = new ArrayList<ClinicalHistory>();
  private List<BirthDateImmunity> birthDateImmunityList = new ArrayList<BirthDateImmunity>();

  public List<ClinicalHistory> getClinicalHistoryList() {
    return clinicalHistoryList;
  }

  public List<BirthDateImmunity> getBirthDateImmunityList() {
    return birthDateImmunityList;
  }
}
