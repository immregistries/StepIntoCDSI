package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.IntervalPriority;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogLevel;

public class MultipleAntigenVaccineGroup extends LogicStep {

  // private ConditionAttribute<Date> caDateAdministered = null;
  private VaccineGroupForecast vgf;
  private List<PatientSeries> selectedList;

  public List<PatientSeries> getSelectedList() {
    return selectedList;
  }

  public MultipleAntigenVaccineGroup(DataModel dataModel) {
    super(LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, dataModel);
    setConditionTableName("Table ");

    VaccineGroup vaccineGroup = dataModel.getVaccineGroup();
    log(LogLevel.CONTROL, "CONTROL: Processing multiple antigen vaccine group: " + vaccineGroup.getName());
    vgf = new VaccineGroupForecast();
    vgf.setVaccineGroup(vaccineGroup);
    selectedList = new ArrayList<PatientSeries>();
    for (PatientSeries p : dataModel.getBestPatientSeriesList()) {
      for (Antigen a : dataModel.getVaccineGroup().getAntigenList()) {
        if (p.getTrackedAntigenSeries().getTargetDisease().equals(a)) {
          selectedList.add(p);
          log(LogLevel.TRACE, "TRACE: Added patient series for antigen " + a.getName() +
              " with status " + p.getPatientSeriesStatus());
        }
      }
    }
    log(LogLevel.STATE,
        "STATE: Selected " + selectedList.size() + " patient series for vaccine group " + vaccineGroup.getName());

    LT logicTable = new LT(vgf, selectedList);
    logicTable.setLogicStepSink(this.getLogicStepSink());
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {

    setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
    evaluateLogicTables();

    log(LogLevel.STATE, "STATE: Vaccine group status determined as: " + vgf.getVaccineGroupStatus());

    // Table 9-2's FORECASTVG-1..9 and FORECASTDN-2 have no vaccine-group-status
    // precondition. FORECASTVG-8 still filters contained forecasts by their
    // own Not Complete status inside MULTIANTVG_8().
    MULTIANTVG_1();
    MULTIANTVG_2();
    MULTIANTVG_3();
    MULTIANTVG_4();
    MULTIANTVG_5();
    MULTIANTVG_6();
    MULTIANTVG_7();
    MULTIANTVG_8();
    MULTIANTVG_9();
    FORECASTDN_2();

    List<Forecast> containedForecasts = new ArrayList<Forecast>();
    for (PatientSeries p : selectedList) {
      if (p.getForecast() != null) {
        containedForecasts.add(p.getForecast());
        if (p.getForecast().getAntigen() != null) {
          vgf.getAntigenList().add(p.getForecast().getAntigen());
          log(LogLevel.TRACE,
              "TRACE: Added antigen " + p.getForecast().getAntigen().getName() + " to vaccine group forecast");
        }
      }
    }
    vgf.setForecastList(containedForecasts);
    if (!vgf.getAntigenList().isEmpty()) {
      vgf.setAntigen(vgf.getAntigenList().get(0));
      dataModel.getVaccineGroupForecastList().add(vgf);
      log(LogLevel.STATE,
          "STATE: Added combined vaccine group forecast with " + vgf.getAntigenList().size() + " antigens");
    }
    log(LogLevel.STATE, "STATE: Selected List size: " + selectedList.size());
    log(LogLevel.STATE, "STATE: Vaccine group forecast list size: " + dataModel.getVaccineGroupForecastList().size());

    return next();
  }

  private void MULTIANTVG_8() {
    List<Antigen> antigensNeededList = new ArrayList<Antigen>();
    for (PatientSeries p : selectedList) {
      if (p.getPatientSeriesStatus() == PatientSeriesStatus.NOT_COMPLETE)
        antigensNeededList.add(p.getTrackedAntigenSeries().getTargetDisease());
    }
    vgf.setAntigensNeededList(antigensNeededList);
  }

  private void MULTIANTVG_7() {
    Set<String> reasons = new LinkedHashSet<String>();
    for (PatientSeries p : selectedList) {
      if (p.getForecast() == null) {
        continue;
      }
      String reason = p.getForecast().getForecastReason();
      if (reason != null && !reason.isEmpty()) {
        reasons.add(reason);
      }
    }
    vgf.setForecastReason(String.join("; ", reasons));
  }

  private void MULTIANTVG_9() {
    Set<VaccineType> recommendedVaccines = new LinkedHashSet<VaccineType>();
    for (PatientSeries p : selectedList) {
      if (p.getForecast() == null || p.getForecast().getRecommendedVaccineList() == null) {
        continue;
      }
      recommendedVaccines.addAll(p.getForecast().getRecommendedVaccineList());
    }
    vgf.getRecommendedVaccineList().addAll(recommendedVaccines);
  }

  private void FORECASTDN_2() {
    if (dataModel.getVaccineGroup() == null) {
      return;
    }
    YesNo administerFull = dataModel.getVaccineGroup().getAdministerFullVaccineGroup();
    Integer result = null;
    for (PatientSeries p : selectedList) {
      if (p.getForecast() == null || p.getForecast().getDoseNumber() == null) {
        continue;
      }
      int doseNumber = p.getForecast().getDoseNumber().intValue();
      if (result == null) {
        result = doseNumber;
        continue;
      }
      if (YesNo.YES.equals(administerFull)) {
        if (doseNumber < result) {
          result = doseNumber;
        }
      } else if (YesNo.NO.equals(administerFull)) {
        if (doseNumber > result) {
          result = doseNumber;
        }
      }
    }
    vgf.setDoseNumber(result);
  }

  private void MULTIANTVG_6() {
    Date earliestRecommendedDate = null;
    for (PatientSeries p : selectedList) {
      if (p.getForecast() != null) {
        Date erd = p.getForecast().getUnadjustedPastDueDate();
        if (erd != null) {
          if (earliestRecommendedDate == null) {
            earliestRecommendedDate = erd;
          } else {
            if (erd.before(earliestRecommendedDate)) {
              earliestRecommendedDate = erd;
            }
          }
        }
      }
    }
    vgf.setUnadjustedPastDueDate(earliestRecommendedDate);
  }

  private void MULTIANTVG_5() {
    Date earliestRecommendedDate = null;
    for (PatientSeries p : selectedList) {
      if (p.getForecast() != null) {
        Date erd = p.getForecast().getUnadjustedRecommendedDate();
        if (erd != null) {
          if (earliestRecommendedDate == null) {
            earliestRecommendedDate = erd;
          } else {
            if (erd.before(earliestRecommendedDate)) {
              earliestRecommendedDate = erd;
            }
          }
        }
      }
    }
    vgf.setUnadjustedRecommendedDate(earliestRecommendedDate);
  }

  private void MULTIANTVG_4() {
    Date latestDate = null;
    TargetDose td = null;
    for (PatientSeries p : selectedList) {
      if (p.getForecast() != null) {
        if (p.getForecast().getTargetDose() != null)
          td = p.getForecast().getTargetDose();
        Date erd = p.getForecast().getLatestDate();
        if (erd != null) {
          if (latestDate == null) {
            latestDate = erd;
          } else {
            if (erd.before(latestDate)) {
              latestDate = erd;
            }
          }
        }
      }
    }
    if (vgf.getTargetDose() == null && td != null) {
      vgf.setTargetDose(td);
    }
    vgf.setLatestDate(latestDate);
  }

  private void MULTIANTVG_3() {
    Date earliestRecommendedDate = null;
    TargetDose td = null;
    for (PatientSeries p : selectedList) {
      if (p.getForecast() != null) {
        Date erd = p.getForecast().getAdjustedPastDueDate();
        if (p.getForecast().getTargetDose() != null)
          td = p.getForecast().getTargetDose();
        if (erd != null) {
          if (earliestRecommendedDate == null) {
            earliestRecommendedDate = erd;
          } else {
            if (erd.before(earliestRecommendedDate)) {
              earliestRecommendedDate = erd;
            }
          }
        }
      }
    }
    if (vgf.getEarliestDate() != null && earliestRecommendedDate != null
        && vgf.getEarliestDate().after(earliestRecommendedDate)) {
      earliestRecommendedDate = vgf.getEarliestDate();
    }
    if (vgf.getTargetDose() == null && td != null) {
      vgf.setTargetDose(td);
    }

    vgf.setAdjustedPastDueDate(earliestRecommendedDate);
  }

  private void MULTIANTVG_2() {
    Date earliestRecommendedDate = null;
    TargetDose td = null;
    for (PatientSeries p : selectedList) {
      if (p.getForecast() != null) {
        Date erd = p.getForecast().getAdjustedRecommendedDate();
        if (erd != null) {
          if (earliestRecommendedDate == null) {
            earliestRecommendedDate = erd;
            if (p.getForecast().getTargetDose() != null)
              td = p.getForecast().getTargetDose();
          } else {
            if (erd.before(earliestRecommendedDate)) {
              earliestRecommendedDate = erd;
              if (p.getForecast().getTargetDose() != null)
                td = p.getForecast().getTargetDose();
            }
          }
        }
      }
    }
    if (vgf.getEarliestDate() != null) {
      if (earliestRecommendedDate != null) {
        if (vgf.getEarliestDate().after(earliestRecommendedDate)) {
          earliestRecommendedDate = vgf.getEarliestDate();
        }
      }
    }
    if (vgf.getTargetDose() == null && td != null) {
      vgf.setTargetDose(td);
    }
    vgf.setAdjustedRecommendedDate(earliestRecommendedDate);
  }

  private void MULTIANTVG_1() {
    Date earliestContainedDate = null;
    Date latestContainedDate = null;
    TargetDose earliestTargetDose = null;
    TargetDose latestTargetDose = null;
    boolean hasPriorityPatientSeriesForecast = false;
    for (PatientSeries p : selectedList) {
      if (p.getForecast() != null) {
        Date ed = p.getForecast().getEarliestDate();
        if (ed != null) {
          if (earliestContainedDate == null || ed.before(earliestContainedDate)) {
            earliestContainedDate = ed;
            earliestTargetDose = p.getForecast().getTargetDose();
          }
          if (latestContainedDate == null || ed.after(latestContainedDate)) {
            latestContainedDate = ed;
            latestTargetDose = p.getForecast().getTargetDose();
          }
        }
        if (isPriorityPatientSeriesForecast(p)) {
          hasPriorityPatientSeriesForecast = true;
        }
      }
    }
    Date earliestDate = latestContainedDate;
    TargetDose td = latestTargetDose;
    if (hasPriorityPatientSeriesForecast) {
      earliestDate = earliestContainedDate;
      td = earliestTargetDose;
      Date latestDateAdministered = latestDateAdministeredInVaccineGroup();
      if (latestDateAdministered != null && (earliestDate == null || latestDateAdministered.after(earliestDate))) {
        earliestDate = latestDateAdministered;
      }
    }
    vgf.setEarliestDate(earliestDate);
    vgf.setTargetDose(td);
  }

  private boolean isPriorityPatientSeriesForecast(PatientSeries patientSeries) {
    if (patientSeries.getForecast() == null || patientSeries.getForecast().getTargetDose() == null
        || patientSeries.getForecast().getTargetDose().getTrackedSeriesDose() == null) {
      return false;
    }
    List<org.openimmunizationsoftware.cdsi.core.domain.Interval> intervalList =
        patientSeries.getForecast().getTargetDose().getTrackedSeriesDose().getIntervalList();
    if (intervalList.isEmpty()) {
      return false;
    }
    for (org.openimmunizationsoftware.cdsi.core.domain.Interval interval : intervalList) {
      if (interval.getIntervalPriority() != IntervalPriority.OVERRIDE) {
        return false;
      }
    }
    return true;
  }

  private Date latestDateAdministeredInVaccineGroup() {
    if (dataModel.getImmunizationHistory() == null) {
      return null;
    }
    Date latestDateAdministered = null;
    for (VaccineDoseAdministered vaccineDoseAdministered :
        dataModel.getImmunizationHistory().getVaccineDoseAdministeredList()) {
      if (!belongsToVaccineGroup(vaccineDoseAdministered)) {
        continue;
      }
      Date dateAdministered = vaccineDoseAdministered.getDateAdministered();
      if (dateAdministered != null
          && (latestDateAdministered == null || dateAdministered.after(latestDateAdministered))) {
        latestDateAdministered = dateAdministered;
      }
    }
    return latestDateAdministered;
  }

  /**
   * Table 9-5 MULTIANTVG-1: a dose belongs to the current vaccine group. Schedule
   * Supporting Data never populates {@code VaccineGroup.vaccineList} (groups are
   * named and mapped to antigens only), so catalog identity matching is dead in
   * production. Prefer that catalog when a test or caller did fill it; otherwise
   * a dose belongs if its CVX-to-antigen associations overlap the group's
   * antigens (DT CVX 28 belongs to DTaP/Tdap/Td because it contains Diphtheria
   * and Tetanus).
   */
  private boolean belongsToVaccineGroup(VaccineDoseAdministered vaccineDoseAdministered) {
    if (vaccineDoseAdministered == null || vaccineDoseAdministered.getVaccine() == null) {
      return false;
    }
    VaccineGroup vaccineGroup = dataModel.getVaccineGroup();
    if (vaccineGroup == null) {
      return false;
    }
    Vaccine administeredVaccine = vaccineDoseAdministered.getVaccine();
    for (Vaccine vaccine : vaccineGroup.getVaccineList()) {
      if (vaccine == administeredVaccine || (vaccine.getVaccineType() != null
          && vaccine.getVaccineType().equals(administeredVaccine.getVaccineType()))) {
        return true;
      }
    }
    return vaccineTypeSharesAnAntigenWithTheVaccineGroup(administeredVaccine.getVaccineType(), vaccineGroup);
  }

  private static boolean vaccineTypeSharesAnAntigenWithTheVaccineGroup(VaccineType vaccineType,
      VaccineGroup vaccineGroup) {
    if (vaccineType == null || vaccineType.getAntigenList().isEmpty()
        || vaccineGroup.getAntigenList().isEmpty()) {
      return false;
    }
    for (Antigen administeredAntigen : vaccineType.getAntigenList()) {
      if (administeredAntigen == null) {
        continue;
      }
      for (Antigen groupAntigen : vaccineGroup.getAntigenList()) {
        if (administeredAntigen.equals(groupAntigen)) {
          return true;
        }
      }
    }
    return false;
  }

  private class LT extends LogicTable {
    public LT(final VaccineGroupForecast vgf, final List<PatientSeries> selectedList) {
      super(6, 6, "Table 9-4 What is the Vaccine Group Status of a Vaccine Group Forecast for a Multiple Antigen Vaccine Group?");

      setLogicCondition(0, new LogicCondition(
          "Is there a patient series forecast contained in the vaccine group forecast with a patient series status of 'Contraindicated'?") {
        @Override
        public LogicResult evaluateInternal() {
          for (PatientSeries p : selectedList) {
            if (p.getPatientSeriesStatus() != null
                && p.getPatientSeriesStatus().equals(PatientSeriesStatus.CONTRAINDICATED)) {
              return LogicResult.YES;
            }
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Is there a patient series forecast contained in the vaccine group forecast with a patient series status of 'Aged Out'?") {
        @Override
        public LogicResult evaluateInternal() {
          for (PatientSeries p : selectedList) {
            if (p.getPatientSeriesStatus() != null
                && p.getPatientSeriesStatus().equals(PatientSeriesStatus.AGED_OUT)) {
              return LogicResult.YES;
            }
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(2, new LogicCondition(
          "Is there a patient series forecast contained in the vaccine group forecast with a patient series status of 'Not Recommended'?") {
        @Override
        public LogicResult evaluateInternal() {
          for (PatientSeries p : selectedList) {
            if (p.getPatientSeriesStatus() != null
                && p.getPatientSeriesStatus().equals(PatientSeriesStatus.NOT_RECOMMENDED)) {
              return LogicResult.YES;
            }
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(3, new LogicCondition(
          "Is there a patient series forecast contained in the vaccine group forecast with a patient series status of 'Not Complete'?") {
        @Override
        public LogicResult evaluateInternal() {
          for (PatientSeries p : selectedList) {
            if (p.getPatientSeriesStatus() != null
                && p.getPatientSeriesStatus().equals(PatientSeriesStatus.NOT_COMPLETE)) {
              return LogicResult.YES;
            }
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(4, new LogicCondition(
          "Do all patient series forecasts contained in the vaccine group forecast have a patient series status of 'Immune'?") {
        @Override
        public LogicResult evaluateInternal() {
          if (selectedList.size() == 0) {
            return LogicResult.NO;
          }
          for (PatientSeries p : selectedList) {
            if (p.getPatientSeriesStatus() != null
                && !p.getPatientSeriesStatus().equals(PatientSeriesStatus.IMMUNE)) {
              return LogicResult.NO;
            }
          }
          return LogicResult.YES;
        }
      });

      setLogicCondition(5, new LogicCondition(
          "Do all patient series forecasts contained in the vaccine group forecast have a patient series status of 'Complete' or 'Immune'?") {
        @Override
        public LogicResult evaluateInternal() {
          if (selectedList.size() == 0) {
            return LogicResult.NO;
          }
          for (PatientSeries p : selectedList) {
            if (p.getPatientSeriesStatus() != null
                && !p.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)
                && !p.getPatientSeriesStatus().equals(PatientSeriesStatus.IMMUNE)) {
              return LogicResult.NO;
            }
          }
          return LogicResult.YES;
        }
      });

      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.NO,
          LogicResult.NO);
      setLogicResults(1, LogicResult.ANY, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.NO,
          LogicResult.NO);
      setLogicResults(2, LogicResult.ANY, LogicResult.ANY, LogicResult.YES, LogicResult.NO, LogicResult.NO,
          LogicResult.NO);
      setLogicResults(3, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.YES, LogicResult.NO,
          LogicResult.NO);
      setLogicResults(4, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.YES,
          LogicResult.NO);
      setLogicResults(5, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY,
          LogicResult.YES);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Contraindicated");
          vgf.setVaccineGroupStatus(PatientSeriesStatus.CONTRAINDICATED);
          vgf.setPatientSeriesStatus(PatientSeriesStatus.CONTRAINDICATED);
          setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
        }
      });
      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Aged Out");
          vgf.setVaccineGroupStatus(PatientSeriesStatus.AGED_OUT);
          vgf.setPatientSeriesStatus(PatientSeriesStatus.AGED_OUT);
          setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
        }
      });
      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Not Recommended");
          vgf.setVaccineGroupStatus(PatientSeriesStatus.NOT_RECOMMENDED);
          vgf.setPatientSeriesStatus(PatientSeriesStatus.NOT_RECOMMENDED);
          setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
        }
      });
      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          log(LogLevel.STATE, "STATE: Vaccine group status set to NOT_COMPLETE");
          vgf.setVaccineGroupStatus(PatientSeriesStatus.NOT_COMPLETE);
          vgf.setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
          setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
        }
      });
      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          log(LogLevel.STATE, "STATE: Vaccine group status set to IMMUNE");
          vgf.setVaccineGroupStatus(PatientSeriesStatus.IMMUNE);
          vgf.setPatientSeriesStatus(PatientSeriesStatus.IMMUNE);
          setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
        }
      });
      setLogicOutcome(5, new LogicOutcome() {
        @Override
        public void perform() {
          log(LogLevel.STATE, "STATE: Vaccine group status set to COMPLETE");
          vgf.setVaccineGroupStatus(PatientSeriesStatus.COMPLETE);
          vgf.setPatientSeriesStatus(PatientSeriesStatus.COMPLETE);
          setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
        }
      });

    }
  }

}
