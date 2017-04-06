package org.openimmunizationsoftware.cdsi.core.domain;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;

public class VaccineDoseAdministered {
  private Date dateAdministered = null;
  private DoseCondition doseCondition = null;
  private Patient patient = null;
  private Vaccine vaccine = null;
  private ImmunizationHistory immunizationHistory = null;
  private TargetDose targetDose = null;
  private Antigen antigenAssigned = null;
  private Map<Antigen, Evaluation> evaluationMap = new HashMap<Antigen, Evaluation>();

  public Map<Antigen, Evaluation> getEvaluationMap() {
    return evaluationMap;
  }

  public Antigen getAntigenAssigned() {
    return antigenAssigned;
  }

  public void setAntigenAssigned(Antigen antigenAssigned) {
    this.antigenAssigned = antigenAssigned;
  }

  public VaccineDoseAdministered() {
    // default
  }

  public VaccineDoseAdministered(VaccineDoseAdministered vdaOriginal) {
    dateAdministered = vdaOriginal.getDateAdministered();
    doseCondition = vdaOriginal.getDoseCondition();
    patient = vdaOriginal.getPatient();
    vaccine = vdaOriginal.getVaccine();
    immunizationHistory = vdaOriginal.getImmunizationHistory();
    targetDose = vdaOriginal.getTargetDose();
  }

  public TargetDose getTargetDose() {
    return targetDose;
  }

  public void setTargetDose(TargetDose targetDose) {
    this.targetDose = targetDose;
  }

  public Date getDateAdministered() {
    return dateAdministered;
  }

  public void setDateAdministered(Date dateAdministered) {
    this.dateAdministered = dateAdministered;
  }

  public DoseCondition getDoseCondition() {
    return doseCondition;
  }

  public void setDoseCondition(DoseCondition doseCondition) {
    this.doseCondition = doseCondition;
  }

  public Patient getPatient() {
    return patient;
  }

  public void setPatient(Patient patient) {
    this.patient = patient;
  }

  public Vaccine getVaccine() {
    return vaccine;
  }

  public void setVaccine(Vaccine vaccine) {
    this.vaccine = vaccine;
  }

  public ImmunizationHistory getImmunizationHistory() {
    return immunizationHistory;
  }

  public void setImmunizationHistory(ImmunizationHistory immunizationHistory) {
    this.immunizationHistory = immunizationHistory;
  }

  @Override
  public String toString() {
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    if (dateAdministered != null && vaccine != null) {
      return vaccine + " given " + sdf.format(dateAdministered);
    } else if (dateAdministered != null) {
      return "given " + sdf.format(dateAdministered);
    } else if (vaccine != null) {
      return vaccine.toString();
    }
    return super.toString();
  }
}
