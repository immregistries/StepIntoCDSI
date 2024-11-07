package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication_TO_BE_REMOVED;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTAGE_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.FORECASTDTCAN_1;

public class DetermineForecastNeed extends LogicStep {

  private ConditionAttribute<String> caVaccineDoseAdministered = null;
  private ConditionAttribute<TargetDose> caTargetDoseStatuses = null;
  private ConditionAttribute<Date> caSeasonalRecommendationEndDate = null;
  private ConditionAttribute<String> caEvidenceOfImmunity = null;
  private ConditionAttribute<String> caContraindicatedPatientSeries = null;
  private ConditionAttribute<Date> caAssessmentDate = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<Date> caCandidateEarliestDate = null;

  private void findEndDate() {
    if (dataModel.getTargetDose() == null) {
      return;
    }
    SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    if (referenceSeriesDose.getSeasonalRecommendationList().size() > 0) {
      Date seasonalRecommendationEndDate = referenceSeriesDose.getSeasonalRecommendationList()
          .get(0).getSeasonalRecommendationEndDate();
          caSeasonalRecommendationEndDate.setInitialValue(seasonalRecommendationEndDate);
    } else {
      //// System.err.println("Recommendation End date is not referenced");
    }
  }

  private void findMaximumAgeDate() {
    if (dataModel.getTargetDose() == null) {
      return;
    }
    SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getMaximumAge();
    if (timePeriod.isValued()) {
      Date dob = dataModel.getPatient().getDateOfBirth();
      caMaximumAgeDate.setInitialValue(timePeriod.getDateFrom(dob));
    }
  }

  public DetermineForecastNeed(DataModel dataModel) {
    // TODO: add Calculated Date (FORECASTDTCAN-1) to table 7-9
    super(LogicStepType.DETERMINE_FORECAST_NEED, dataModel);
    setConditionTableName("Table 7-0 : Determine Forecast Need Attributes");

    caVaccineDoseAdministered =
        new ConditionAttribute<String>("Immunization history", "Vaccine Dose(s) Administered");
    caTargetDoseStatuses = new ConditionAttribute<TargetDose>("Relevant Patient series", "Target Dose Statuses");
    caSeasonalRecommendationEndDate = new ConditionAttribute<Date>("Supporting data (Seasonal Recommendation", "Seasonal Recommendation End Date");
    caEvidenceOfImmunity = new ConditionAttribute<String>("Section 7.2 Outcome", "Evidence of Immunity");
    caContraindicatedPatientSeries = new ConditionAttribute<String>("Section 7.3 Outcome", "Contraindicated Patient Series");
    caAssessmentDate = new ConditionAttribute<Date>("Runtime Data", "Assessment Date"); 
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)", "Maximum Age Date");
    caCandidateEarliestDate = new ConditionAttribute<Date>("Calculated date (FORECASTDTCAN-1)", "Candidate Earliest Date");

    caMaximumAgeDate.setAssumedValue(FUTURE);
    caSeasonalRecommendationEndDate.setAssumedValue(FUTURE);
    caAssessmentDate.setAssumedValue(new Date());
    caCandidateEarliestDate.setAssumedValue(FUTURE);
    
    // TODO: Add assumed values for caEvidenceOfImmunity and caContraindicatedPatientSeries

    caTargetDoseStatuses.setInitialValue(dataModel.getTargetDose());
    findMaximumAgeDate();
    findEndDate();
    caAssessmentDate.setInitialValue(dataModel.getAssessmentDate());
    caMaximumAgeDate.setInitialValue(CALCDTAGE_1.evaluate(dataModel, this, null));
    caCandidateEarliestDate.setInitialValue(FORECASTDTCAN_1.evaluate(dataModel, this, null));

    conditionAttributesList.add(caVaccineDoseAdministered);
    conditionAttributesList.add(caTargetDoseStatuses);
    conditionAttributesList.add(caSeasonalRecommendationEndDate);
    conditionAttributesList.add(caEvidenceOfImmunity);
    conditionAttributesList.add(caContraindicatedPatientSeries);
    conditionAttributesList.add(caAssessmentDate);
    conditionAttributesList.add(caMaximumAgeDate);
    conditionAttributesList.add(caCandidateEarliestDate);

    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES);
    evaluateLogicTables();
    if (getNextLogicStepType() == LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES) {
      if (dataModel.getTargetDose() == null) {
        throw new Exception("Problem! next target dose is null "
            + dataModel.getPatientSeries().getTrackedAntigenSeries().getSeriesName());
      }
    }

    return next();
  }

  @Override
  public void printPre(PrintWriter out) throws Exception {
    printStandard(out);
  }

  @Override
  public void printPost(PrintWriter out) throws Exception {
    printStandard(out);

    if (!dataModel.getPatient().getMedicalHistory().getContraindicationSet().isEmpty()) {
      out.println("<h2>Contraindications for Patient</h2>");
      for (Contraindication_TO_BE_REMOVED contraindication : dataModel.getPatient().getMedicalHistory()
          .getContraindicationSet()) {
        if (contraindication.getAntigen()
            .equals(dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease())) {
          out.println("<li>" + contraindication + "</li>");
        }
      }
    }
  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + getTitle() + "</h1>");
    out.println(
        "<p>Determine forecast need determines  if there is a need to forecast dates. This involves reviewing patient data, antigen  administered  records,  and  patient  series.  This  is  a  prerequisite  before  a  CDS  engine  can  produce forecast dates and reasons </p>");
    out.println(
        "<p>The following process model, attribute table, and decision table are used to determine the need to generate forecast dates.</p>");
    out.println("<img src=\"Figure 7.6.png\"/>");
    out.println("<p>FIGURE 7 - 6 DETERMINE FORECAST NEED PROCESS MODEL</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    public LT() {
      /*  TODO: Add the following conditions to 7-10
       * Does the patient have evidence of immunity?
       * Is the candidate earliest date < the maximum age date?
      */
      super(5, 6, "Table 7-10 Should the patient receive another target dose ?");
      setLogicCondition(0, new LogicCondition(
          "Does the patient have at least one target dose with a target dose status of \"not satisfied\"?") {
        @Override
        protected LogicResult evaluateInternal() {
          List<TargetDose> targetDoseList = dataModel.getTargetDoseList();
          for (TargetDose targetDose : targetDoseList) {
            if (targetDose.getTargetDoseStatus() != null) {
              if (targetDose.getTargetDoseStatus().equals(TargetDoseStatus.NOT_SATISFIED)) {
                return LogicResult.YES;
              }
            }

          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Does the patient have at least one target dose with a target dose status of \"satisfied\"?") {
        @Override
        protected LogicResult evaluateInternal() {
          List<TargetDose> targetDoseList = dataModel.getTargetDoseList();
          for (TargetDose targetDose : targetDoseList) {
            if (targetDose.getTargetDoseStatus() != null) {
              if (targetDose.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {
                return LogicResult.YES;
              }
            }

          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(2,
          new LogicCondition("Is the patient without a contradiction for this patient series ?") {
            @Override
            protected LogicResult evaluateInternal() {
              List<Contraindication_TO_BE_REMOVED> targetContraindictionList = new ArrayList<Contraindication_TO_BE_REMOVED>();
              for (Contraindication_TO_BE_REMOVED contraindication : dataModel.getPatient().getMedicalHistory()
                  .getContraindicationSet()) {
                if (contraindication.getAntigen().equals(
                    dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease())) {
                  targetContraindictionList.add(contraindication);
                }
              }
              if (dataModel.getPatient().getMedicalHistory().getContraindicationSet().isEmpty()) {
                return LogicResult.YES;
              }
              return LogicResult.NO;

            }
          });

      setLogicCondition(3, new LogicCondition("Is the assessment date < the maximum age date ?") {
        @Override
        protected LogicResult evaluateInternal() {
          if (caAssessmentDate.getFinalValue().before(caMaximumAgeDate.getFinalValue())) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(4,
          new LogicCondition("Is the assessment date < seasonal recommendation end date ?") {
            @Override
            protected LogicResult evaluateInternal() {
              if (caAssessmentDate.getFinalValue().before(caSeasonalRecommendationEndDate.getFinalValue())) {
                return LogicResult.YES;
              } else {
                return LogicResult.NO;
              }
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
          log("No. The patient should not receive another dose.");
          setNextLogicStepType(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES);
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.COMPLETE);
          Antigen tmpAntigen =
              dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          List<Forecast> forecastList = dataModel.getForecastList();
          for (Forecast forecast : forecastList) {
            if (forecast.getAntigen().equals(tmpAntigen)) {
              forecast.setForecastReason("Patient series is complete");
            }
          }

          log("Forecast reason is \"patient series is complete.\"");
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose.");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_RECOMMENDED);
          Antigen tmpAntigen =
              dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          List<Forecast> forecastList = dataModel.getForecastList();
          for (Forecast forecast : forecastList) {
            if (forecast.getAntigen().equals(tmpAntigen)) {
              forecast.setForecastReason(
                  "Not recommended at this time due to past immunization history");
            }
          }
          log("Forecast reason is \"not recommended at this time due to past immunization history.\"");
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another doses.");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.CONTRAINDICATED);
          Antigen tmpAntigen =
              dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          List<Forecast> forecastList = dataModel.getForecastList();
          for (Forecast forecast : forecastList) {
            if (forecast.getAntigen().equals(tmpAntigen)) {
              forecast.setForecastReason("Patient has contraindication");
            }
          }
          log("Forecast reason is \"patient has a contraindication.\"");
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another doses.");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.AGED_OUT);
          Antigen tmpAntigen =
              dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          List<Forecast> forecastList = dataModel.getForecastList();
          for (Forecast forecast : forecastList) {
            if (forecast.getAntigen().equals(tmpAntigen)) {
              forecast.setForecastReason("Patient has exceeded the maximum age");
            }
          }
          log("Forecast reason is \"patient has exceeded the maximum age.\"");
        }
      });

      setLogicOutcome(5, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose.");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
          Antigen tmpAntigen =
              dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          List<Forecast> forecastList = dataModel.getForecastList();
          for (Forecast forecast : forecastList) {
            if (forecast.getAntigen().equals(tmpAntigen)) {
              forecast.setForecastReason("Forecast reason is past seasonal date");
            }
          }
          log("Forecast reason is \"past seasonal recommendation end date.\"");

        }
      });

    }
  }

}
