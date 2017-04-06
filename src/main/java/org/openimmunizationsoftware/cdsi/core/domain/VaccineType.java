package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class VaccineType {
  private String cvxCode = "";
  private String shortDescription = "";
  private List<Antigen> antigenList = new ArrayList<Antigen>();

  public String toString() {
    return shortDescription + " (" + cvxCode + ")";
  }

  public List<Antigen> getAntigenList() {
    return antigenList;
  }

  public void setAntigenList(List<Antigen> antigenList) {
    this.antigenList = antigenList;
  }

  public String getCvxCode() {
    return cvxCode;
  }

  public void setCvxCode(String cvxCode) {
    this.cvxCode = cvxCode;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof VaccineType) {
      VaccineType vt = (VaccineType) obj;
      return this.getCvxCode().equals(vt.getCvxCode());
    }
    return super.equals(obj);
  }


  @Override
  public int hashCode() {
    return getCvxCode().hashCode();
  }
}
