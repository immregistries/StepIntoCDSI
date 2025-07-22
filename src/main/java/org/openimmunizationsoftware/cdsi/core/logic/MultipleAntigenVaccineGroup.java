package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.IntervalPriority;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class MultipleAntigenVaccineGroup extends LogicStep {

  // private ConditionAttribute<Date> caDateAdministered = null;
  private VaccineGroupForecast vgf;
  private List<PatientSeries> selectedList;

  public MultipleAntigenVaccineGroup(DataModel dataModel) {
    super(LogicStepType.MULTIPLE_ANTIGEN_VACCINE_GROUP, dataModel);
    setConditionTableName("Table ");

    VaccineGroup vaccineGroup = dataModel.getVaccineGroup();
    vgf = new VaccineGroupForecast();
    vgf.setVaccineGroup(vaccineGroup);
    selectedList = new ArrayList<PatientSeries>();
    for (PatientSeries p : dataModel.getBestPatientSeriesList()) {
      for (Antigen a : dataModel.getVaccineGroup().getAntigenList()) {
        if (p.getTrackedAntigenSeries().getTargetDisease().equals(a)) {
          selectedList.add(p);
        }
      }
    }

    LT logicTable = new LT(vgf, selectedList);
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {

    setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
    evaluateLogicTables();

    if (vgf.getVaccineGroupStatus() == VaccineGroupStatus.NOT_COMPLETE) {
      // MULTIANTVG-1
      MULTIANTVG_1();
      // MULTIANTVG-2
      MULTIANTVG_2();
      // MULTIANTVG-3
      MULTIANTVG_3();
      // MULTIANTVG-4
      MULTIANTVG_4();
      // MULTIANTVG-5
      MULTIANTVG_5();
      // MULTIANTVG-6
      MULTIANTVG_6();
      // MULTIANTVG-7
      MULTIANTVG_7();
      // MULTIANTVG-8
      MULTIANTVG_8();
      // MULTIANTVG-9
      List<VaccineGroup> recommendedVaccines = new ArrayList<VaccineGroup>();
      for (PatientSeries p : selectedList)
        if (p.getForecast() != null && p.getForecast().getVaccineGroupForecast() != null
            && p.getForecast().getVaccineGroupForecast().getVaccineGroup() != null) {
          recommendedVaccines.add(p.getForecast().getVaccineGroupForecast().getVaccineGroup());
        }
      // vgf.setV
      for (PatientSeries p : selectedList) {
        vgf.setAntigen(p.getForecast().getAntigen());
        vgf.getAntigenList().add(p.getForecast().getAntigen());
        dataModel.getVaccineGroupForecastList().add(vgf);
      }
    }

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
    String reasons = "";
    for (PatientSeries p : selectedList)
      reasons = reasons + p.getForecast().getForecastReason();
    vgf.setForecastReason(reasons);
  }

  private void MULTIANTVG_6() {
    Date earliestRecommendedDate = null;
    for (PatientSeries p : selectedList) {
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
    vgf.setUnadjustedPastDueDate(earliestRecommendedDate);
  }

  private void MULTIANTVG_5() {
    Date earliestRecommendedDate = null;
    for (PatientSeries p : selectedList) {
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
    vgf.setUnadjustedRecommendedDate(earliestRecommendedDate);
  }

  private void MULTIANTVG_4() {
    Date latestDate = null;
    TargetDose td = null;
    for (PatientSeries p : selectedList) {
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
    if (vgf.getTargetDose() == null && td != null) {
      vgf.setTargetDose(td);
    }
    vgf.setLatestDate(latestDate);
  }

  private void MULTIANTVG_3() {
    Date earliestRecommendedDate = null;
    TargetDose td = null;
    for (PatientSeries p : selectedList) {
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
    if (vgf.getEarliestDate() != null && earliestRecommendedDate != null && vgf.getEarliestDate().after(earliestRecommendedDate)) {
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
    Date earliestDate = null;
    TargetDose td = null;
    for (PatientSeries p : selectedList) {
      Date ed = p.getForecast().getEarliestDate();
      if (ed != null) {
        if (earliestDate == null) {
          earliestDate = ed;
          if (p.getForecast().getTargetDose() != null)
            td = p.getForecast().getTargetDose();
        } else {
          IntervalPriority intervalPriority = p.getForecast().getInterval() == null ? null
              : p.getForecast().getInterval().getIntervalPriority();
          if (intervalPriority == null) {
            if (ed.after(earliestDate)) {
              earliestDate = ed;
              if (p.getForecast().getTargetDose() != null)
                td = p.getForecast().getTargetDose();
            }
          } else {
            if (ed.before(earliestDate)) {
              earliestDate = ed;
              if (p.getForecast().getTargetDose() != null)
                td = p.getForecast().getTargetDose();
            }
          }
        }
      }
    }
    vgf.setEarliestDate(earliestDate);
    vgf.setTargetDose(td);
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
        "<p>The forecasting  decisions and  rules which need to be applied to a multiple antigen  vaccine group are  listed below</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
    out.println(
        "<h2>Selected Patient Series for " + dataModel.getVaccineGroup().getName() + "</h2>");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <th>Antigen</th>");
    out.println("    <th>Patient Series Status</th>");
    out.println("  </tr>");
    for (PatientSeries patientSeries : selectedList) {
      out.println("  <tr>");
      out.println(
          "    <td>" + patientSeries.getTrackedAntigenSeries().getTargetDisease() + "</td>");
      out.println("    <td>" + patientSeries.getPatientSeriesStatus() + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");

    printLog(out);
  }

  private class LT extends LogicTable {
    public LT(final VaccineGroupForecast vgf, final List<PatientSeries> selectedList) {
      super(6, 6, "Table 9 - 3 WHAT IS THE VACCINE GROUP STATUS OF A MULTIPLE VACCINE GROUP?");

      setLogicCondition(0, new LogicCondition(
          "Is there a patient series forecast contained in the vaccine group forecast with a patient series status of 'Contraindicated'?") {
        @Override
        public LogicResult evaluateInternal() {
          for (PatientSeries p : selectedList) {
            if (p.getPatientSeriesStatus().equals(PatientSeriesStatus.CONTRAINDICATED)) {
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
            if (p.getPatientSeriesStatus().equals(PatientSeriesStatus.AGED_OUT)) {
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
            if (p.getPatientSeriesStatus().equals(PatientSeriesStatus.NOT_RECOMMENDED)) {
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
          for (PatientSeries p : dataModel.getBestPatientSeriesList()) {
            if (p.getPatientSeriesStatus().equals(PatientSeriesStatus.NOT_COMPLETE)) {
              return LogicResult.YES;
            }
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(4, new LogicCondition("Do all patient series forecasts contained in the vaccine group forecast have a patient series status of 'Immune'?") {
        @Override
        public LogicResult evaluateInternal() {
          if (selectedList.size() == 0) {
            return LogicResult.NO;
          }
          for (PatientSeries p : selectedList) {
            if (!p.getPatientSeriesStatus().equals(PatientSeriesStatus.IMMUNE)) {
              return LogicResult.NO;
            }
          }
          return LogicResult.YES;
        }
      });

      setLogicCondition(5, new LogicCondition("Do all patient series forecasts contained in the vaccine group forecast have a patient series status of 'Complete' or 'Immune'?") {
        @Override
        public LogicResult evaluateInternal() {
          if (selectedList.size() == 0) {
            return LogicResult.NO;
          }
          for (PatientSeries p : selectedList) {
            if (!p.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE) && !p.getPatientSeriesStatus().equals(PatientSeriesStatus.IMMUNE)) {
              return LogicResult.NO;
            }
          }
          return LogicResult.YES;
        }
      });

      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.NO);
      setLogicResults(1, LogicResult.ANY, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.NO, LogicResult.NO);
      setLogicResults(2, LogicResult.ANY, LogicResult.ANY, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.NO);
      setLogicResults(3, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.YES, LogicResult.NO, LogicResult.NO);
      setLogicResults(4, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.YES, LogicResult.NO);
      setLogicResults(5, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY, LogicResult.YES);

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
          log("Not Complete");
          vgf.setVaccineGroupStatus(PatientSeriesStatus.NOT_COMPLETE);
          vgf.setPatientSeriesStatus(PatientSeriesStatus.NOT_COMPLETE);
          setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
        }
      });
      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          log("Immune");
          vgf.setVaccineGroupStatus(PatientSeriesStatus.IMMUNE);
          vgf.setPatientSeriesStatus(PatientSeriesStatus.IMMUNE);
          setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
        }
      });
      setLogicOutcome(5, new LogicOutcome() {
        @Override
        public void perform() {
          log("Complete");
          vgf.setVaccineGroupStatus(PatientSeriesStatus.COMPLETE);
          vgf.setPatientSeriesStatus(PatientSeriesStatus.COMPLETE);
          setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
        }
      });

    }
  }

}
