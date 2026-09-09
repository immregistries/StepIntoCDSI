package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class VaccineContraindication extends Contraindication {

  private List<VaccineType> contraindicatedVaccineTypeList = new ArrayList<VaccineType>();

  public List<VaccineType> getContraindicatedVaccineTypeList() {
    return contraindicatedVaccineTypeList;
  }
}
