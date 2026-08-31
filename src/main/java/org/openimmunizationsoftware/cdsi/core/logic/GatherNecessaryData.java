package org.openimmunizationsoftware.cdsi.core.logic;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.ForecastInput;
import org.openimmunizationsoftware.cdsi.core.domain.ImmunizationHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Observation;
import org.openimmunizationsoftware.cdsi.core.domain.ObservationCode;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientObservation;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;

public class GatherNecessaryData extends LogicStep {

  public GatherNecessaryData(DataModel dataModel) {
    super(LogicStepType.GATHER_NECESSARY_DATA, dataModel);
  }

  @Override
  public LogicStep process() throws Exception {

    ForecastInput input = dataModel.getForecastInput();
    if (input == null) {
      throw new IllegalStateException(
          "No ForecastInput has been set on the data model; the caller must adapt its input (web request, FITS test case, FHIR operation, etc.) into a ForecastInput before running the engine");
    }

    Patient patient = new Patient();
    dataModel.setPatient(patient);
    patient.setDateOfBirth(input.getPatientDateOfBirth());
    patient.setGender(input.getPatientSex());
    dataModel.setAssessmentDate(input.getAssessmentDate());

    ImmunizationHistory immunizationHistory = new ImmunizationHistory();
    dataModel.setImmunizationHistory(immunizationHistory);
    patient.getMedicalHistory().setImmunizationHistory(immunizationHistory);
    immunizationHistory.setMedicalHistory(patient.getMedicalHistory());

    int id = 1;
    for (ForecastInput.VaccinationInput v : input.getVaccinationList()) {
      VaccineDoseAdministered vda = new VaccineDoseAdministered();
      vda.setId(id++);
      vda.setPatient(patient);
      vda.setImmunizationHistory(immunizationHistory);
      immunizationHistory.getVaccineDoseAdministeredList().add(vda);
      patient.getReceivesList().add(vda);
      vda.setDateAdministered(v.getDateAdministered());
      if (v.getDoseCondition() != null) {
        vda.setDoseCondition(v.getDoseCondition());
      }
      String cvxCode = v.getVaccineCvx();
      VaccineType cvx = dataModel.getCvxMap().get(cvxCode);
      if (cvx == null) {
        throw new IllegalArgumentException("Unrecognized cvx code '" + cvxCode + "'");
      }
      Vaccine vaccine = new Vaccine();
      vaccine.setVaccineType(cvx);
      vaccine.setManufacturer(v.getVaccineMvx());
      vda.setVaccine(vaccine);
    }

    for (ForecastInput.ObservationInput o : input.getObservationList()) {
      String observationCodeValue = o.getObservationCode();
      if (observationCodeValue == null || observationCodeValue.equals("")) {
        continue;
      }
      Observation observation = dataModel.getObservationMap().get(observationCodeValue);
      if (observation == null) {
        throw new IllegalArgumentException("Unrecognized observation code '" + observationCodeValue + "'");
      }

      PatientObservation patientObservation = new PatientObservation();
      ObservationCode observationCode = new ObservationCode();
      observationCode.setCode(observation.getObservationCode());
      observationCode.setText(observation.getObservationTitle());
      patientObservation.setObservationCode(observationCode);
      patientObservation.setObservationDate(o.getObservationDate());

      patient.getMedicalHistory().getPatientObservationList().add(patientObservation);
    }

    return LogicStepFactory.createLogicStep(LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY, dataModel);
  }

}
