package org.openimmunizationsoftware.cdsi.core.domain;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;

public class VaccineDoseAdministered {
  private int id = 0;
  private Date dateAdministered = null;
  private DoseCondition doseCondition = null;
  private Patient patient = null;
  private Vaccine vaccine = null;
  private ImmunizationHistory immunizationHistory = null;
  private TargetDose targetDose = null;
  private TargetDose evaluatedAgainstTargetDose = null;
  private Antigen antigenAssigned = null;
  private boolean inadvertentAdministration = false;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
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
    evaluatedAgainstTargetDose = vdaOriginal.getEvaluatedAgainstTargetDose();
    inadvertentAdministration = vdaOriginal.isInadvertentAdministration();
  }

  public TargetDose getTargetDose() {
    return targetDose;
  }

  public void setTargetDose(TargetDose targetDose) {
    this.targetDose = targetDose;
  }

  /**
   * Which target dose 6.10 Satisfy Target Dose most recently evaluated this
   * administered dose against, regardless of outcome (SATISFIED, EXTRANEOUS, or
   * NOT_VALID) - unlike {@link #getTargetDose()}, which stays null unless the
   * dose actually satisfied one. Several other steps rely on
   * {@code getTargetDose() == null} / {@code getSatisfiedByVaccineDoseAdministered()
   * == null} as their own signal for "this dose was not satisfied"; this is a
   * separate link so those are never affected by it.
   */
  public TargetDose getEvaluatedAgainstTargetDose() {
    return evaluatedAgainstTargetDose;
  }

  public void setEvaluatedAgainstTargetDose(TargetDose evaluatedAgainstTargetDose) {
    this.evaluatedAgainstTargetDose = evaluatedAgainstTargetDose;
  }

  /**
   * Whether 6.3 Evaluate for Inadvertent Vaccine (Table 6-13) found this
   * specific administered dose's vaccine type on the target dose's own
   * inadvertent-vaccine list, at the time it was evaluated - a fact about
   * this one administration, not a pointer to a shared, mutable TargetDose
   * (whose own evaluation reason can be overwritten by a later administered
   * dose evaluated against the same target dose). FORECASTDTCAN-1 needs
   * exactly this: "the latest of all dates administered of any inadvertent
   * administration being evaluated against a target dose."
   */
  public boolean isInadvertentAdministration() {
    return inadvertentAdministration;
  }

  public void setInadvertentAdministration(boolean inadvertentAdministration) {
    this.inadvertentAdministration = inadvertentAdministration;
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
