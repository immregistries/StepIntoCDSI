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
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication_TO_BE_REMOVED;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
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

public class DetermineForecastNeed extends LogicStep {

  private ConditionAttribute<String> caVaccineDoseAdministered = null;
  private ConditionAttribute<TargetDose> caTargetDoseStatuses = null;
  private ConditionAttribute<Date> caSeasonalRecommendationEndDate = null;
  private ConditionAttribute<String> caEvidenceOfImmunity = null;
  private ConditionAttribute<String> caContraindicatedPatientSeries = null;
  private ConditionAttribute<Date> caAssessmentDate = null;
  private ConditionAttribute<Date> caMaximumAgeDate = null;
  private ConditionAttribute<Date> caCandidateEarliestDate = null;

  private void findSeasonalRecommendationEndDate() {
    if (dataModel.getTargetDose() == null) {
      return;
    }
    SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    if (referenceSeriesDose.getSeasonalRecommendationList().size() > 0) {
      Date seasonalRecommendationEndDate = referenceSeriesDose.getSeasonalRecommendationList()
          .get(0).getSeasonalRecommendationEndDate();
      caSeasonalRecommendationEndDate.setInitialValue(seasonalRecommendationEndDate);
    } else {
      log("Recommendation End date is not referenced");
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
    super(LogicStepType.DETERMINE_FORECAST_NEED, dataModel);
    setConditionTableName("Table 7-9 : Determine Forecast Need Attributes");

    caVaccineDoseAdministered = new ConditionAttribute<String>("Immunization history", "Vaccine Dose(s) Administered");
    caTargetDoseStatuses = new ConditionAttribute<TargetDose>("Relevant Patient series", "Target Dose Statuses");
    caSeasonalRecommendationEndDate = new ConditionAttribute<Date>("Supporting data (Seasonal Recommendation",
        "Seasonal Recommendation End Date");
    caEvidenceOfImmunity = new ConditionAttribute<String>("Section 7.2 Outcome", "Evidence of Immunity");
    caContraindicatedPatientSeries = new ConditionAttribute<String>("Section 7.3 Outcome",
        "Contraindicated Patient Series");
    caAssessmentDate = new ConditionAttribute<Date>("Runtime Data", "Assessment Date");
    caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)", "Maximum Age Date");
    caCandidateEarliestDate = new ConditionAttribute<Date>("Calculated date (FORECASTDTCAN-1)",
        "Candidate Earliest Date");

    caMaximumAgeDate.setAssumedValue(FUTURE);
    caSeasonalRecommendationEndDate.setAssumedValue(FUTURE);
    caAssessmentDate.setAssumedValue(new Date());
    caCandidateEarliestDate.setAssumedValue(FUTURE);

    // TODO: Add assumed values for caEvidenceOfImmunity and
    // caContraindicatedPatientSeries

    caTargetDoseStatuses.setInitialValue(dataModel.getTargetDose());
    // TODO move DateRule calculations to DateRules file
    caMaximumAgeDate.setInitialValue(CALCDTAGE_1.evaluate(dataModel, this, null));
    caCandidateEarliestDate.setInitialValue(computeEarliestDate());
    findMaximumAgeDate();
    findSeasonalRecommendationEndDate();
    caAssessmentDate.setInitialValue(dataModel.getAssessmentDate());

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
      super(7, 8, "Table 7-10 Should the patient receive another target dose?");
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

      setLogicCondition(2, new LogicCondition(
          "Does the patient have evidence of immunity?") {
        @Override
        protected LogicResult evaluateInternal() {
          // TODO add logic for this logic condition
          //if(dataModel.getPatientSeries().getPatientSeriesStatus().equals(PatientSeriesStatus.IMMUNE)) {
          //  return LogicResult.YES;  
          //}
          return LogicResult.NO;
        }
      });

      setLogicCondition(3,
          new LogicCondition("Is the relevant patient series a contraindicated patient series?") {
            @Override
            protected LogicResult evaluateInternal() {
              // TODO add logic
              if (dataModel.getPatient().getMedicalHistory().getContraindicationSet().isEmpty()) {
                return LogicResult.NO;
              }
              return LogicResult.YES;
            }
          });

      setLogicCondition(4, new LogicCondition("Is the assessment date <= the seasonal recommendation end date?") {
        @Override
        protected LogicResult evaluateInternal() {
          if (caAssessmentDate.getFinalValue().after(caSeasonalRecommendationEndDate.getFinalValue())) {
            return LogicResult.NO;
          }
          return LogicResult.YES;
        }
      });

      setLogicCondition(5, new LogicCondition("Is the assessment date < maximum age date?") {
        @Override
        protected LogicResult evaluateInternal() {
          if (caAssessmentDate.getFinalValue().before(caMaximumAgeDate.getFinalValue())) {
            return LogicResult.YES;
          } else {
            return LogicResult.NO;
          }
        }
      });

      setLogicCondition(6, new LogicCondition("Is the candidate earliest date < the maximum age date?") {
        @Override
        protected LogicResult evaluateInternal() {
          if (caCandidateEarliestDate.getFinalValue().before(caMaximumAgeDate.getFinalValue())) {
            return LogicResult.YES;
          } else {
            return LogicResult.NO;
          }
        }
      });

      setLogicResults(0, YES, NO, NO, ANY, ANY, ANY, ANY, ANY);
      setLogicResults(1, ANY, YES, NO, ANY, ANY, ANY, ANY, ANY);
      setLogicResults(2, NO, ANY, ANY, YES, ANY, ANY, ANY, ANY);
      setLogicResults(3, NO, ANY, ANY, ANY, YES, ANY, ANY, ANY);
      setLogicResults(4, YES, ANY, ANY, ANY, ANY, NO, ANY, ANY);
      setLogicResults(5, YES, ANY, ANY, ANY, ANY, ANY, NO, ANY);
      setLogicResults(6, YES, ANY, ANY, ANY, ANY, ANY, ANY, NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The patient should receive another dose.");
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
          log("Patient Series Status is 'Not Complete'");
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose.");
          setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.COMPLETE);
          log("Patient Series Status is 'Complete'");
          Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
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
          setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_RECOMMENDED);
          log("Patient Series Status is 'Not Recommended'");
          Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
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
          log("No. The patient should not receive another dose.");
          setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.IMMUNE);
          log("Patient Series Status is 'Immune'");
          Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          List<Forecast> forecastList = dataModel.getForecastList();
          for (Forecast forecast : forecastList) {
            if (forecast.getAntigen().equals(tmpAntigen)) {
              forecast.setForecastReason("Patient has evidence of immunity");
            }
          }
          log("Forecast reason is \"Patient has evidence of immunity\"");
        }
      });

      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose.");
          setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.CONTRAINDICATED);
          log("Patient Series Status is 'Contraindicated'");
          Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          List<Forecast> forecastList = dataModel.getForecastList();
          for (Forecast forecast : forecastList) {
            if (forecast.getAntigen().equals(tmpAntigen)) {
              forecast.setForecastReason("Patient has contraindication");
            }
          }
          log("Forecast reason is \"patient has a contraindication.\"");
        }
      });

      setLogicOutcome(5, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another doses.");
          setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.NOT_RECOMMENDED);
          log("Patient Series Status is 'Not Recommended'");
          Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          List<Forecast> forecastList = dataModel.getForecastList();
          for (Forecast forecast : forecastList) {
            if (forecast.getAntigen().equals(tmpAntigen)) {
              forecast.setForecastReason("Past seasonal recommendation end date");
            }
          }
          log("Forecast reason is \"Past seasonal recommendation end date\"");
        }
      });

      setLogicOutcome(6, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose.");
          setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.AGED_OUT);
          log("Patient Series Status is 'Aged Out'");
          Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          List<Forecast> forecastList = dataModel.getForecastList();
          for (Forecast forecast : forecastList) {
            if (forecast.getAntigen().equals(tmpAntigen)) {
              forecast.setForecastReason("Patient has exceeded the maximum age");
            }
          }
          log("Forecast reason is \"Patient has exceeded the maximum age\"");
        }
      });

      setLogicOutcome(7, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The patient should not receive another dose.");
          setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
          dataModel.getPatientSeries().setPatientSeriesStatus(PatientSeriesStatus.AGED_OUT);
          log("Patient Series Status is 'Aged Out'");
          Antigen tmpAntigen = dataModel.getPatientSeries().getTrackedAntigenSeries().getTargetDisease();
          List<Forecast> forecastList = dataModel.getForecastList();
          for (Forecast forecast : forecastList) {
            if (forecast.getAntigen().equals(tmpAntigen)) {
              forecast.setForecastReason("Patient is unable to finish the series prior to the maximum age");
            }
          }
          log("Forecast reason is \"Patient is unable to finish the series prior to the maximum age\"");
        }
      });

    }
  }

  private Date computeEarliestDate() {
    List<Date> list = new ArrayList<Date>();

    SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    Date dob = dataModel.getPatient().getDateOfBirth();

    {
      TimePeriod timePeriod = referenceSeriesDose.getAgeList().get(0).getMinimumAge();
      Date minimumAgeDate = timePeriod.getDateFrom(dob);
      list.add(minimumAgeDate);
    }
    Date latestMinimumIntervalDate = GenerateForecastDatesAndRecommendedVaccines
        .getLatestDate(findMinimumIntervalDates());
    list.add(latestMinimumIntervalDate);
    // list.add(caLatestConflictEndIntervalDate.getFinalValue());// CALCDTLIVE-4 is
    // both used and removed?
    // list.add(caSeasonalRecommendationStartDate.getFinalValue());
    Date earliestDate = GenerateForecastDatesAndRecommendedVaccines.getLatestDate(list);
    return earliestDate;
  }

  private List<Date> findMinimumIntervalDates() {
    List<Date> minimumIntervalList = new ArrayList<Date>();
    SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    Date patientReferenceDoseDate = computePatientReferenceDoseDate();
    if (referenceSeriesDose.getIntervalList() != null) {
      for (Interval minIn : referenceSeriesDose.getIntervalList()) {
        TimePeriod minimalIntervalFromReferenceSeriesDose = minIn.getMinimumInterval();
        if (minimalIntervalFromReferenceSeriesDose == null) {
          continue;
        }
        minimumIntervalList.add(minimalIntervalFromReferenceSeriesDose.getDateFrom(patientReferenceDoseDate));
      }
    }
    return minimumIntervalList;
  }

  private Date computePatientReferenceDoseDate() {
    Date tmpPatientReferenceDoseDate = new Date();
    try {
      AntigenAdministeredRecord previousAAR = dataModel.getPreviousAntigenAdministeredRecord();
      tmpPatientReferenceDoseDate = previousAAR.getDateAdministered();
    } catch (NullPointerException np) {
      np.getCause();
    }
    return tmpPatientReferenceDoseDate;

  }

}
