package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class VaccineType {
  private String cvxCode = "";
  private String shortDescription = "";
  private List<Antigen> antigenList = new ArrayList<Antigen>();
  private Map<Antigen, TimePeriod> associationBeginAgeMap = new HashMap<Antigen, TimePeriod>();
  private Map<Antigen, TimePeriod> associationEndAgeMap = new HashMap<Antigen, TimePeriod>();

  public String toString() {
    return shortDescription + " (" + cvxCode + ")";
  }

  public List<Antigen> getAntigenList() {
    return antigenList;
  }

  public void setAntigenList(List<Antigen> antigenList) {
    this.antigenList = antigenList;
  }

  /**
   * The CVX to Antigen Supporting Data's Association Begin/End Age for one of
   * this vaccine type's antigen associations - unvalued (no restriction) for
   * most associations, but valued for a few (e.g. CVX 121 Zoster live, whose
   * Varicella association ends, and Zoster association begins, at 50 years).
   * Note 2a (section 4.2) is the only place these are read.
   */
  public TimePeriod getAssociationBeginAge(Antigen antigen) {
    return associationBeginAgeMap.get(antigen);
  }

  public TimePeriod getAssociationEndAge(Antigen antigen) {
    return associationEndAgeMap.get(antigen);
  }

  public void setAssociationAge(Antigen antigen, TimePeriod beginAge, TimePeriod endAge) {
    associationBeginAgeMap.put(antigen, beginAge);
    associationEndAgeMap.put(antigen, endAge);
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
