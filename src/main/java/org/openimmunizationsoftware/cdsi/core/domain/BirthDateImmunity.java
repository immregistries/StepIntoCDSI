package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BirthDateImmunity {
  private Immunity immunity = null;
  private Date immunityBirthDate = null;
  private String immunityCountryOfBirth = "";
  private String immunityExclusionCondition = "";
  private List<Exclusion> exclusionList = new ArrayList<Exclusion>();

  public Immunity getImmunity() {
    return immunity;
  }
  
  public void setImmunity(Immunity immunity) {
    this.immunity = immunity;
  }
  
  public List<Exclusion> getExclusionList() {
    return exclusionList;
  }

  public Date getImmunityBirthDate() {
    return immunityBirthDate;
  }

  public void setImmunityBirthDate(Date immunityBirthDate) {
    this.immunityBirthDate = immunityBirthDate;
  }

  public String getImmunityCountryOfBirth() {
    return immunityCountryOfBirth;
  }

  public void setImmunityCountryOfBirth(String countryOfBirth) {
    this.immunityCountryOfBirth = countryOfBirth;
  }

  public String getImmunityExclusionCondition() {
    return immunityExclusionCondition;
  }

  public void setImmunityExclusionCondition(String immunityExclusionCondition) {
    this.immunityExclusionCondition = immunityExclusionCondition;
  }

}
