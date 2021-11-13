package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class VaccineGroup {
  private String name = "";
  private List<Vaccine> vaccineList = new ArrayList<Vaccine>();
  private VaccineGroupForecast vaccineGroupForecast = null;
  private YesNo administerFullVaccineGroup = null;
  private List<Antigen> antigenList = new ArrayList<Antigen>();

  public List<Antigen> getAntigenList() {
    return antigenList;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public VaccineGroupForecast getVaccineGroupForecast() {
    return vaccineGroupForecast;
  }

  public void setVaccineGroupForecast(VaccineGroupForecast vaccineGroupForecast) {
    this.vaccineGroupForecast = vaccineGroupForecast;
  }

  public YesNo getAdministerFullVaccineGroup() {
    return administerFullVaccineGroup;
  }

  public void setAdministerFullVaccineGroup(YesNo administerFullVaccineGroup) {
    this.administerFullVaccineGroup = administerFullVaccineGroup;
  }

  public List<Vaccine> getVaccineList() {
    return vaccineList;
  }

  public void setVaccineList(List<Vaccine> vaccineList) {
    this.vaccineList = vaccineList;
  }

  @Override
  public String toString() {
    return name;
  }
}
