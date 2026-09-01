package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BirthDateImmunity {
  private Date immunityBirthDate = null;
  private String countryOfBirth = "";
  private List<Exclusion> exclusionList = new ArrayList<Exclusion>();

  public List<Exclusion> getExclusionList() {
    return exclusionList;
  }

  public Date getImmunityBirthDate() {
    return immunityBirthDate;
  }

  public void setImmunityBirthDate(Date immunityBirthDate) {
    this.immunityBirthDate = immunityBirthDate;
  }

  public String getCountryOfBirth() {
    return countryOfBirth;
  }

  public void setCountryOfBirth(String countryOfBirth) {
    this.countryOfBirth = countryOfBirth;
  }

}
