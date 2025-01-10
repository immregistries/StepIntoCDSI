package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.BirthDateImmunity;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.MedicalHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Immunity;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class DetermineEvidenceOfImmunity extends LogicStep {

  // ConditionAttributes to be used
  private ConditionAttribute<Date> caDateofBirth = null;
  private ConditionAttribute<String> caCountryofBirth = null;
  private ConditionAttribute<MedicalHistory> caEvidenceOfImmunity = null;
  private ConditionAttribute<List<Immunity>> caImmunityElements = null;

  public DetermineEvidenceOfImmunity(DataModel dataModel) {
    super(LogicStepType.DETERMINE_EVIDENCE_OF_IMMUNITY, dataModel);

    // Table 7-2
    setConditionTableName("Table 7-2 Immunity attributes");
    caDateofBirth = new ConditionAttribute<Date>("Patient", "Date of Birth");
    caCountryofBirth = new ConditionAttribute<String>("Patient", "Country of Birth");
    caEvidenceOfImmunity = new ConditionAttribute<MedicalHistory>("Patient", "Evidence of Immunity");
    caImmunityElements = new ConditionAttribute<List<Immunity>>("Supporting Data", "Immunity Elements");

    // Sets initial and assumed values
    caDateofBirth.setInitialValue(dataModel.getPatient().getDateOfBirth());
    caCountryofBirth.setInitialValue(dataModel.getPatient().getCountryOfBirth());
    caEvidenceOfImmunity.setInitialValue(dataModel.getPatient().getMedicalHistory());
    caImmunityElements.setInitialValue(dataModel.getForecast().getAntigen().getImmunityList());

    // Adds items to conditionAttributesList
    conditionAttributesList.add(caDateofBirth);
    conditionAttributesList.add(caCountryofBirth);
    conditionAttributesList.add(caEvidenceOfImmunity);

    // Adds logic table 7-2 to logicTableList
    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {

    Forecast forecast = new Forecast();
    forecast.setAntigen(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease());
    forecast.setTargetDose(dataModel.getTargetDose());
    dataModel.setForecast(forecast);
    dataModel.getPatientSeries().setForecast(forecast);

    setNextLogicStepType(LogicStepType.DETERMINE_CONTRAINDICATIONS);
    evaluateLogicTables();
    return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);
  }

  private void printStandard(PrintWriter out) {
    out.println(
        "<p>Determine evidence of immunity  assesses the patient’s profile to determine if the patient is already potentially immune to the target disease, negating the need for additional doses.</p>");
    out.println(
        "<p>A patient may be considered immune due to their clinical history or if they were born before a defined date for the given target disease.</p>");
    out.println("<img src=\"Figure 7.2.png\"/>");
    out.println("<p>FIGURE 7 - 2 EVIDENCE OF IMMUNITY PROCESS MODEL</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  // Table 7-3, no actual changes to functional logic but it has to work with the
  // new 7-2 attributes
  private class LT extends LogicTable {
    public LT() {
      super(4, 5, "Table 7-3 Does the patient have evidence of immunity?");

      setLogicCondition(0, new LogicCondition(
          "Does the patient history contain one of the immunity guidelines?") {
        @Override
        protected LogicResult evaluateInternal() {
          if (caEvidenceOfImmunity != null) {
            /*
             * TODO add logic condition
             * for(int i = 0; i <= caEvidenceOfImmunity.get; )
             * OR
             * for (VaccineDoseAdministered vda :
             * caEvidenceOfImmunity.getFinalValue().getImmunizationHistory().
             * getVaccineDoseAdministeredList()) {
             * 
             * YES if the immunity guideline is somewhere in evidence of immunity
             * where to find immunity guidelines?
             * dataModel -> immunityList -> Immunity -> clinicalHistoryList ->
             * clinicalHistory -> immunityGuidelineCode
             * OR dataModel -> forecast -> antigen -> immunityList -> Immunity ->
             * clinicalHistoryList -> clinicalHistory -> immunityGuidelineCode
             * 
             * where to find evidence of immunity/patient history?
             */
            // placeholder for now, above logic should determine YES or NO
          }
          return NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Is the patient's date of birth < the immunity birth date ?") {
        @Override
        protected LogicResult evaluateInternal() {
          Date dob = caDateofBirth.getFinalValue();
          if (dob == null) {
            return NO;
          }
          if (dataModel.getImmunityList().size() == 0) {
            return NO;
          }
          if (dataModel.getImmunityList().get(0).getBirthDateImmunityList().size() == 0) {
            return NO;
          }

          List<BirthDateImmunity> birthDateImmunityList = dataModel.getImmunityList().get(0).getBirthDateImmunityList();
          for (BirthDateImmunity bdi : birthDateImmunityList) {
            if (dob.before(bdi.getImmunityBirthDate())) {
              return YES;
            }
          }

          return NO;
        }
      });

      setLogicCondition(2,
          new LogicCondition("Does the patient have an immunity exclusion condition?") {
            @Override
            protected LogicResult evaluateInternal() {
              if (dataModel.getImmunityList().size() == 0) {
                return NO;
              }

              if (dataModel.getImmunityList().get(0).getBirthDateImmunityList().size() == 0) {
                return NO;
              }

              List<BirthDateImmunity> birthDateImmunityList = dataModel.getImmunityList().get(0)
                  .getBirthDateImmunityList();
              for (BirthDateImmunity bdi : birthDateImmunityList) {
                if (bdi.getExclusionList().size() > 0) {
                  return YES;
                }
              }

              return NO;
            }
          });

      setLogicCondition(3, new LogicCondition(
          "Is the patient's country of birth the same as the immunity country of birth?") {
        @Override
        protected LogicResult evaluateInternal() {
          String patientCountry = caCountryofBirth.getFinalValue().toString();
          if (patientCountry == null) {
            return NO;
          }
          if (dataModel.getImmunityList().size() == 0) {
            return NO;
          }
          if (dataModel.getImmunityList().get(0).getBirthDateImmunityList().size() == 0) {
            return NO;
          }

          List<BirthDateImmunity> birthDateImmunityList = dataModel.getImmunityList().get(0).getBirthDateImmunityList();
          for (BirthDateImmunity bdi : birthDateImmunityList) {
            if (bdi.getCountryOfBirth().equals(patientCountry)) {
              return YES;
            }
          }

          return NO;
        }
      });

      // Outcomes of 7-3 logic
      setLogicResults(0, YES, NO, NO, NO, NO);
      setLogicResults(1, ANY, YES, YES, YES, NO);
      setLogicResults(2, ANY, YES, NO, NO, ANY);
      setLogicResults(3, ANY, ANY, YES, NO, ANY);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The patient has evidence of immunity.");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.IMMUNE);
          log("Forecast reason is \"patient has evidence of immunity\". ");
          dataModel.getForecast().setForecastReason("Patient has Evidence of immunity");
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient does not have evidence of immunity.");
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The patient has evidence of immunity.");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.IMMUNE);
          dataModel.getForecast().setForecastReason("Patient has evidence of immunity");
          log("Forecast reason is \"patient has evidence of immunity\". ");
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient does not have evidence of immunity.");
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient does not have evidence of immunity.");
        }
      });
    }
  }
}
