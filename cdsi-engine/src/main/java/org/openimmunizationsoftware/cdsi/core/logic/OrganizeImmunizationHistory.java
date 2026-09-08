package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class OrganizeImmunizationHistory extends LogicStep {

  public OrganizeImmunizationHistory(DataModel dataModel) {
    super(LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY, dataModel);
  }

  @Override
  public LogicStep process() {

    for (VaccineDoseAdministered vda : dataModel.getImmunizationHistory()
        .getVaccineDoseAdministeredList()) {
      VaccineType vaccineType = vda.getVaccine().getVaccineType();
      for (Antigen antigen : vaccineType.getAntigenList()) {
        if (isAssociatedAtAdministration(vaccineType, antigen, vda.getDateAdministered())) {
          AntigenAdministeredRecord aar = new AntigenAdministeredRecord(vda, antigen);
          dataModel.getAntigenAdministeredRecordList().add(aar);
        }
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

  /**
   * Note 2a: the CVX to Antigen Supporting Data's Association Begin/End Age
   * select the antigen by the patient's age at administration - most
   * associations carry no such restriction (an unvalued TimePeriod for both),
   * but CVX 121 Zoster live's does: administered below 50 years associates
   * with Varicella, at or above 50 years with Zoster. Begin age is inclusive,
   * end age exclusive, matching every other age-window convention in the
   * specification (e.g. Table 6-15).
   */
  private boolean isAssociatedAtAdministration(VaccineType vaccineType, Antigen antigen,
      Date dateAdministered) {
    Date dateOfBirth = dataModel.getPatient().getDateOfBirth();
    TimePeriod beginAge = vaccineType.getAssociationBeginAge(antigen);
    if (beginAge != null && beginAge.isValued()
        && dateAdministered.before(beginAge.getDateFrom(dateOfBirth))) {
      return false;
    }
    TimePeriod endAge = vaccineType.getAssociationEndAge(antigen);
    if (endAge != null && endAge.isValued()
        && !dateAdministered.before(endAge.getDateFrom(dateOfBirth))) {
      return false;
    }
    return true;
  }

}
