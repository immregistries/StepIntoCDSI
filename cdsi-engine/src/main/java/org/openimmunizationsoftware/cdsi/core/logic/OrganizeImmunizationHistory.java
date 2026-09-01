package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.Collections;
import java.util.Comparator;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;

public class OrganizeImmunizationHistory extends LogicStep {

  public OrganizeImmunizationHistory(DataModel dataModel) {
    super(LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY, dataModel);
  }

  @Override
  public LogicStep process() {

    for (VaccineDoseAdministered vda : dataModel.getImmunizationHistory()
        .getVaccineDoseAdministeredList()) {
      for (Antigen antigen : vda.getVaccine().getVaccineType().getAntigenList()) {
        AntigenAdministeredRecord aar = new AntigenAdministeredRecord(vda, antigen);
        dataModel.getAntigenAdministeredRecordList().add(aar);
      }
    }

    Collections.sort(dataModel.getAntigenAdministeredRecordList(),
        new Comparator<AntigenAdministeredRecord>() {
          @Override
          public int compare(AntigenAdministeredRecord o1, AntigenAdministeredRecord o2) {
            Antigen a1 = o1.getAntigen();
            Antigen a2 = o2.getAntigen();
            if (a1 == null || a2 == null || a1.getName().equalsIgnoreCase(a2.getName())) {
              return o1.getDateAdministered().compareTo(o2.getDateAdministered());
            }
            return a1.getName().compareTo(a2.getName());
          }
        });

    return LogicStepFactory.createLogicStep(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES,
        dataModel);
  }

}
