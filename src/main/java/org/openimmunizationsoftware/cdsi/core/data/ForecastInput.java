package org.openimmunizationsoftware.cdsi.core.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;

/**
 * Plain, transport-agnostic description of a single forecast request: a
 * patient, an assessment date, and the immunization/observation history to
 * evaluate. This is the one input shape the CDSi engine understands -
 * whatever the caller is (an HTTP form, a FITS test case, a FHIR operation),
 * it is adapted into a ForecastInput before GatherNecessaryData runs.
 */
public class ForecastInput {

  private Date patientDateOfBirth;
  private String patientSex;
  private Date assessmentDate;
  private final List<VaccinationInput> vaccinationList = new ArrayList<>();
  private final List<ObservationInput> observationList = new ArrayList<>();

  public Date getPatientDateOfBirth() {
    return patientDateOfBirth;
  }

  public void setPatientDateOfBirth(Date patientDateOfBirth) {
    this.patientDateOfBirth = patientDateOfBirth;
  }

  public String getPatientSex() {
    return patientSex;
  }

  public void setPatientSex(String patientSex) {
    this.patientSex = patientSex;
  }

  public Date getAssessmentDate() {
    return assessmentDate;
  }

  public void setAssessmentDate(Date assessmentDate) {
    this.assessmentDate = assessmentDate;
  }

  public List<VaccinationInput> getVaccinationList() {
    return vaccinationList;
  }

  public VaccinationInput addVaccination() {
    VaccinationInput vaccination = new VaccinationInput();
    vaccinationList.add(vaccination);
    return vaccination;
  }

  public List<ObservationInput> getObservationList() {
    return observationList;
  }

  public ObservationInput addObservation() {
    ObservationInput observation = new ObservationInput();
    observationList.add(observation);
    return observation;
  }

  public static class VaccinationInput {
    private Date dateAdministered;
    private String vaccineCvx;
    private String vaccineMvx;
    private DoseCondition doseCondition;

    public Date getDateAdministered() {
      return dateAdministered;
    }

    public void setDateAdministered(Date dateAdministered) {
      this.dateAdministered = dateAdministered;
    }

    public String getVaccineCvx() {
      return vaccineCvx;
    }

    public void setVaccineCvx(String vaccineCvx) {
      this.vaccineCvx = vaccineCvx;
    }

    public String getVaccineMvx() {
      return vaccineMvx;
    }

    public void setVaccineMvx(String vaccineMvx) {
      this.vaccineMvx = vaccineMvx;
    }

    public DoseCondition getDoseCondition() {
      return doseCondition;
    }

    public void setDoseCondition(DoseCondition doseCondition) {
      this.doseCondition = doseCondition;
    }
  }

  public static class ObservationInput {
    private String observationCode;
    private Date observationDate;

    public String getObservationCode() {
      return observationCode;
    }

    public void setObservationCode(String observationCode) {
      this.observationCode = observationCode;
    }

    public Date getObservationDate() {
      return observationDate;
    }

    public void setObservationDate(Date observationDate) {
      this.observationDate = observationDate;
    }
  }
}
