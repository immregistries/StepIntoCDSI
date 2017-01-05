package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.List;
import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeasonalRecommendation;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

public class DetermineForecastNeed extends LogicStep {

  private ConditionAttribute<String> caVaccineDoseAdministered = null;
  private ConditionAttribute<String> caAdvereseEvents = null;
  private ConditionAttribute<String> caRelevantMedicalObservation = null;
  private ConditionAttribute<String> caTargetDose = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<Date> caEndDate = null;
  private ConditionAttribute<Date> caAssessmentDate = null;
  private ConditionAttribute<String> caContraindication = null;
  private ConditionAttribute<String> caImmunity = null;

  public DetermineForecastNeed(DataModel dataModel) {
    super(LogicStepType.DETERMINE_FORECAST_NEED, dataModel);
    setConditionTableName("Table ");

    caVaccineDoseAdministered = new ConditionAttribute<String>("Immunization history", "Vaccine Dose(s) Administered");
    caAdvereseEvents = new ConditionAttribute<String>("Immunization history", "Adverse Events");
    caRelevantMedicalObservation = new ConditionAttribute<String>("Medical History", "Relevant Medical Observation");
    caTargetDose = new ConditionAttribute<String>("Patient series", "Target Dose(s)");
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)", "Maximum Age Date");
    caEndDate = new ConditionAttribute<Date>("Supporting data (Seasonal Recommendation", "End Date");
    caAssessmentDate = new ConditionAttribute<Date>("Data Entry", "Assessment Date");
    caContraindication = new ConditionAttribute<String>("Supporting Data", "Contraindication");
    caImmunity = new ConditionAttribute<String>("Supporting Data", "Immunity");

    caMaximumAgeDate.setAssumedValue(FUTURE);
    caEndDate.setAssumedValue(FUTURE);
    caAssessmentDate.setAssumedValue(null);

    conditionAttributesList.add(caVaccineDoseAdministered);
    conditionAttributesList.add(caAdvereseEvents);
    conditionAttributesList.add(caRelevantMedicalObservation);
    conditionAttributesList.add(caTargetDose);
    conditionAttributesList.add(caMaximumAgeDate);
    conditionAttributesList.add(caEndDate);
    conditionAttributesList.add(caAssessmentDate);
    conditionAttributesList.add(caContraindication);
    conditionAttributesList.add(caImmunity);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES);
    // setNextLogicStepType(LogicStepType.FOR_EACH_PATIENT_SERIES);
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
    out.println("<h1> " + getTitle() + "</h1>");
    out.println(
        "<p>Determine forecast need determines  if there is a need to forecast dates. This involves reviewing patient data, antigen  administered  records,  and  patient  series.  This  is  a  prerequisite  before  a  CDS  engine  can  produce forecast dates and reasons </p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    public LT() {
      super(5, 6, "Table 5-5 Should the patient receive another target dose ?");

      setLogicCondition(0, new LogicCondition(
          "Does the patient have at least one target dose with a target dose status of \"not satisfied\"?") {
        @Override
        protected LogicResult evaluateInternal() {
          if (caTargetDose.equals(TargetDoseStatus.NOT_SATISFIED)) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Does the patient have at least one target dose with a target dose status of \"satisfied\"?") {
        @Override
        protected LogicResult evaluateInternal() {
          if (caTargetDose.equals(TargetDoseStatus.SATISFIED)) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(2, new LogicCondition("Is the patient without a contradiction for this patient series ?") {
        @Override
        protected LogicResult evaluateInternal() {
          if (dataModel.getContraindicationList().isEmpty()) {
            return LogicResult.YES;
          }
          return LogicResult.NO;

        }
      });

      setLogicCondition(3, new LogicCondition("Is the assement date < the maximum age date ?") {
        @Override
        protected LogicResult evaluateInternal() {
          if (caAssessmentDate.getAssumedValue().before(caMaximumAgeDate.getAssumedValue())) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(4, new LogicCondition("Is the assement date < seasonal recommendation end date ?") {
        @Override
        protected LogicResult evaluateInternal() {
          List<TargetDose> targetDoseList = dataModel.getTargetDoseList();
          int targetDoseListLength = targetDoseList.size();
          for (int i = 0; i < targetDoseListLength; i++) {
            TargetDose targetDose = targetDoseList.get(i);
            List<SeasonalRecommendation> seasonalRecommendationList = targetDose.getTrackedSeriesDose()
                .getSeasonalRecommendationList();
            int seasonalRecommendationLength = seasonalRecommendationList.size();
            for (int j = 0; j < seasonalRecommendationLength; j++) {
              SeasonalRecommendation seasonalRecommendation = seasonalRecommendationList.get(j);
              if (caAssessmentDate.getAssumedValue()
                  .before(seasonalRecommendation.getSeasonalRecommendationEndDate())) {
                return LogicResult.YES;
              }
            }
          }
          return LogicResult.NO;

        }
      });

      setLogicResults(0, YES, NO, NO, ANY, ANY, ANY);
      setLogicResults(1, ANY, YES, NO, ANY, ANY, ANY);
      setLogicResults(2, YES, ANY, ANY, NO, ANY, ANY);
      setLogicResults(3, YES, ANY, ANY, ANY, NO, ANY);
      setLogicResults(4, YES, ANY, ANY, ANY, ANY, NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The patient should receive another dose.");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose .");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.COMPLETE);
          log("Forecast reason is \"patient series is complete.\"");
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose .");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_RECOMMENDED);
          log("Forecast reason is \"not recommended at this time due to past immunization history.\"");
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose .");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.CONTRAINDICATED);
          log("Forecast reason is \"not recommended at this time due to past immunization history.\"");
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose .");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.AGED_OUT);
          log("Forecast reason is \"patient has execeeded the maximum age.\"");
        }
      });

      setLogicOutcome(5, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose .");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
          log("Forecast reason is \"past seasonal recommendation end date.\"");
        }
      });

    }
  }

}
