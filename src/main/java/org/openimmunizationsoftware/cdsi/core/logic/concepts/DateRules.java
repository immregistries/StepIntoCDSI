package org.openimmunizationsoftware.cdsi.core.logic.concepts;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.SkipTargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;

public class DateRules
{
  public static DateRule CALCDTSKIP_1 = null;
  public static DateRule CALCDTSKIP_2 = null;
  public static DateRule CALCDTSUB_1 = null;
  public static DateRule CALCDTSUB_2 = null;
  public static DateRule CALCDTAGE_1 = null;
  public static DateRule CALCDTAGE_2 = null;
  public static DateRule CALCDTAGE_3 = null;
  public static DateRule CALCDTAGE_4 = null;
  public static DateRule CALCDTAGE_5 = null;
  public static DateRule CALCDTINT_1 = null;
  public static DateRule CALCDTINT_2 = null;
  public static DateRule CALCDTINT_3 = null;
  public static DateRule CALCDTINT_4 = null;
  public static DateRule CALCDTINT_5 = null;
  public static DateRule CALCDTINT_6 = null;
  public static DateRule CALCDTINT_7 = null;
  public static DateRule CALCDTLIVE_1 = null;
  public static DateRule CALCDTLIVE_2 = null;
  public static DateRule CALCDTLIVE_3 = null;
  public static DateRule CALCDTLIVE_4 = null;
  public static DateRule CALCDTPREF_1 = null;
  public static DateRule CALCDTPREF_2 = null;
  public static DateRule CALCDTALLOW_1 = null;
  public static DateRule CALCDTALLOW_2 = null;

  static {
    CALCDTSKIP_1 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
        if (seriesDose.getSkipTargetDoseList().size() > 0) {
          logicStep.log("  + skip target dose list has entries");
          SkipTargetDose skipTargetDose = seriesDose.getSkipTargetDoseList().get(0);
          logicStep.log("  + trigger age is " + skipTargetDose.getTriggerAge());
          return skipTargetDose.getTriggerAge().getDateFrom(dataModel.getPatient().getDateOfBirth());
        }
        return null;
      }
    };
    CALCDTSKIP_1.setBusinessRuleId("CALCDTSKIP-1");
    CALCDTSKIP_1
        .setBusinessRule("The patient's trigger age date must be calculated as the patient’s date of birth plus the "
            + "skip dose trigger age.");
    CALCDTSKIP_1.setLogicalComponent("Skip Target Dose");
    CALCDTSKIP_1.setFieldName("patient's trigger age date");

    CALCDTSKIP_2 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        TargetDose currentTargetDose = dataModel.getTargetDose();
        TargetDose intervalTargetDose = null;
        logicStep.log("  + looking for last satisfied dose");
        for (TargetDose targetDose : dataModel.getTargetDoseList()) {
          if (currentTargetDose == targetDose) {
            break;
          }
          if (targetDose.getTargetDoseStatus() == TargetDoseStatus.SATISFIED) {
            logicStep.log("  + found satisfied dose on "
                + sdf.format(targetDose.getSatisifiedByVaccineDoseAdministered().getDateAdministered()));
            intervalTargetDose = targetDose;
          }
        }
        if (intervalTargetDose != null) {
          logicStep.log("  + looking at satisfied dose on "
              + sdf.format(intervalTargetDose.getSatisifiedByVaccineDoseAdministered().getDateAdministered()));
          SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
          if (seriesDose.getSkipTargetDoseList().size() > 0) {
            logicStep.log("  + skip target dose list has entries");
            SkipTargetDose skipTargetDose = seriesDose.getSkipTargetDoseList().get(0);
            logicStep.log("  + trigger interval is " + skipTargetDose.getTriggerInterval());
            return skipTargetDose.getTriggerInterval().getDateFrom(
                intervalTargetDose.getSatisifiedByVaccineDoseAdministered().getDateAdministered());
          }
        }
        return null;
      }

    };
    CALCDTSKIP_2.setBusinessRuleId("CALCDTSKIP-2");
    CALCDTSKIP_2
        .setBusinessRule("The patient's trigger interval date must be calculated as the vaccine date administered which "
            + "satisfied the previous target dose plus the skip dose trigger interval.");
    CALCDTSKIP_2.setLogicalComponent("Skip Target Dose");
    CALCDTSKIP_2.setFieldName("patient's trigger interval date");

    CALCDTSUB_1 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTSUB_1.setBusinessRuleId("CALCDTSUB-1");
    CALCDTSUB_1
        .setBusinessRule("The patient's first dose begin age date must be calculated as the patient’s date of birth plus substitute dose first dose begin age.");
    CALCDTSUB_1.setLogicalComponent("Substitute Target Dose");
    CALCDTSUB_1.setFieldName("patient's first dose begin age date");

    CALCDTSUB_2 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTSUB_2.setBusinessRuleId("CALCDTSUB-2");
    CALCDTSUB_2
        .setBusinessRule("The patient's first dose end age date must be calculated as the patient’s date of birth plus substitute dose first dose end age.");
    CALCDTSUB_2.setLogicalComponent("Substitute Target Dose");
    CALCDTSUB_2.setFieldName("patient's first dose end age date");

    CALCDTAGE_1 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTAGE_1.setBusinessRuleId("CALCDTAGE-1");
    CALCDTAGE_1
        .setBusinessRule("The patient's maximum age date must be calculated as the patient’s date of birth plus the maximum age.");
    CALCDTAGE_1.setLogicalComponent("Age");
    CALCDTAGE_1.setFieldName("patient's maximum age date");

    CALCDTAGE_2 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTAGE_2.setBusinessRuleId("CALCDTAGE-2");
    CALCDTAGE_2
        .setBusinessRule("The patient's latest recommended age date must be calculated as the patient’s date of birth plus the latest recommended age.");
    CALCDTAGE_2.setLogicalComponent("Age");
    CALCDTAGE_2.setFieldName("patient's latest recommended age date");

    CALCDTAGE_3 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTAGE_3.setBusinessRuleId("CALCDTAGE-3");
    CALCDTAGE_3
        .setBusinessRule("The patient's earliest recommended age date must be calculated as the patient’s date of birth plus the earliest recommended age.");
    CALCDTAGE_3.setLogicalComponent("Age");
    CALCDTAGE_3.setFieldName("patient's earliest recommended age date");

    CALCDTAGE_4 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTAGE_4.setBusinessRuleId("CALCDTAGE-4");
    CALCDTAGE_4
        .setBusinessRule("The patient's minimum age date must be calculated as the patient’s date of birth plus the minimum age.");
    CALCDTAGE_4.setLogicalComponent("Age");
    CALCDTAGE_4.setFieldName("patient's minimum age date");

    CALCDTAGE_5 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTAGE_5.setBusinessRuleId("CALCDTAGE-5");
    CALCDTAGE_5
        .setBusinessRule("The patient's absolute minimum age date must be calculated as the patient’s date of birth plus the absolute minimum age.");
    CALCDTAGE_5.setLogicalComponent("Age");
    CALCDTAGE_5.setFieldName("patient's absolute minimum age date");

    CALCDTINT_1 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTINT_1.setBusinessRuleId("CALCDTINT-1");
    CALCDTINT_1
        .setBusinessRule("The patient's reference dose date must be calculated as the date administered of the most immediate previous vaccine dose administered which has evaluation status \"Valid\" or \"Not Valid\" if from immediate previous dose administered is \"Y\".");
    CALCDTINT_1.setLogicalComponent("Interval");
    CALCDTINT_1.setFieldName("patient's reference dose date");

    CALCDTINT_2 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTINT_2.setBusinessRuleId("CALCDTINT-2");
    CALCDTINT_2
        .setBusinessRule("The patient's reference dose date must be calculated as the date administered of the vaccine dose administered which satisfies the target dose defined in the interval from target dose number in series if from immediate previous dose administered is \"N\".");
    CALCDTINT_2.setLogicalComponent("Interval");
    CALCDTINT_2.setFieldName("patient's reference dose date");

    CALCDTINT_3 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTINT_3.setBusinessRuleId("CALCDTINT-3");
    CALCDTINT_3
        .setBusinessRule("The patient's absolute minimum interval date must be calculated as the patient's reference dose date plus the absolute minimum interval.");
    CALCDTINT_3.setLogicalComponent("Interval");
    CALCDTINT_3.setFieldName("patient's absolute minimum interval date");

    CALCDTINT_4 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTINT_4.setBusinessRuleId("CALCDTINT-4");
    CALCDTINT_4
        .setBusinessRule("The patient's minimum interval date must be calculated as the patient's reference dose date plus the minimum interval.");
    CALCDTINT_4.setLogicalComponent("Interval");
    CALCDTINT_4.setFieldName("patient's minimum interval date");

    CALCDTINT_5 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTINT_5.setBusinessRuleId("CALCDTINT-5");
    CALCDTINT_5
        .setBusinessRule("The patient's earliest recommended interval date must be calculated as the patient’s reference dose date plus the earliest recommended interval.");
    CALCDTINT_5.setLogicalComponent("Interval");
    CALCDTINT_5.setFieldName("patient's earliest recommended interval date");

    CALCDTINT_6 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTINT_6.setBusinessRuleId("CALCDTINT-6");
    CALCDTINT_6
        .setBusinessRule("The patient's latest recommended interval date must be calculated as the patient’s reference dose date plus the latest recommended interval.");
    CALCDTINT_6.setLogicalComponent("Interval");
    CALCDTINT_6.setFieldName("patient's latest recommended interval date");

    CALCDTINT_7 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTINT_7.setBusinessRuleId("CALCDTINT-7");
    CALCDTINT_7
        .setBusinessRule("The patient's latest minimum interval date must be the latest date of all calculated minimum interval dates for a given target dose.");
    CALCDTINT_7.setLogicalComponent("Interval");
    CALCDTINT_7.setFieldName("patient's latest minimum interval date");

    CALCDTLIVE_1 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTLIVE_1.setBusinessRuleId("CALCDTLIVE-1");
    CALCDTLIVE_1
        .setBusinessRule("The patient's conflict begin interval date must be calculated as the date administered of the conflicting vaccine dose administered plus the live virus conflict begin interval.");
    CALCDTLIVE_1.setLogicalComponent("Live Virus Conflict");
    CALCDTLIVE_1.setFieldName("patient's conflict begin interval date");

    CALCDTLIVE_2 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTLIVE_2.setBusinessRuleId("CALCDTLIVE-2");
    CALCDTLIVE_2
        .setBusinessRule("The patient's conflict end interval date must be calculated as the date administered of the conflicting vaccine dose administered plus the live virus minimum conflict end interval when the conflicting vaccine dose administered has evaluation status \"valid.\"");
    CALCDTLIVE_2.setLogicalComponent("Live Virus Conflict");
    CALCDTLIVE_2.setFieldName("patient's conflict end interval date");

    CALCDTLIVE_3 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTLIVE_3.setBusinessRuleId("CALCDTLIVE-3");
    CALCDTLIVE_3
        .setBusinessRule("The patient's conflict end interval date must be calculated as the date administered of the conflicting vaccine dose administered plus the live virus conflict end interval when the conflicting vaccine dose administered does not have evaluation status \"valid.\"");
    CALCDTLIVE_3.setLogicalComponent("Live Virus Conflict");
    CALCDTLIVE_3.setFieldName("patient's conflict end interval date");

    CALCDTLIVE_4 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTLIVE_4.setBusinessRuleId("CALCDTLIVE-4");
    CALCDTLIVE_4
        .setBusinessRule("The patient's latest conflict end interval date must be the latest date of all calculated conflict end dates for a given target dose.");
    CALCDTLIVE_4.setLogicalComponent("Live Virus Conflict");
    CALCDTLIVE_4.setFieldName("patient's latest conflict end interval date");

    CALCDTPREF_1 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTPREF_1.setBusinessRuleId("CALCDTPREF-1");
    CALCDTPREF_1
        .setBusinessRule("The patient's preferable vaccine type begin age date must be calculated as the patient’s date of birth plus the preferable vaccine type begin age.");
    CALCDTPREF_1.setLogicalComponent("Preferable Vaccine");
    CALCDTPREF_1.setFieldName("patient's preferable vaccine type begin age date");

    CALCDTPREF_2 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTPREF_2.setBusinessRuleId("CALCDTPREF-2");
    CALCDTPREF_2
        .setBusinessRule("The patient's preferable vaccine type end age date must be calculated as the patient’s date of birth plus the preferable vaccine type end age.");
    CALCDTPREF_2.setLogicalComponent("Preferable Vaccine");
    CALCDTPREF_2.setFieldName("patient's preferable vaccine type end age date");

    CALCDTALLOW_1 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTALLOW_1.setBusinessRuleId("CALCDTALLOW-1");
    CALCDTALLOW_1
        .setBusinessRule("The patient's allowable vaccine type begin age date must be calculated as the patient’s date of birth plus the allowable vaccine type begin age.");
    CALCDTALLOW_1.setLogicalComponent("Allowable Vaccine");
    CALCDTALLOW_1.setFieldName("patient's allowable vaccine type begin age date");

    CALCDTALLOW_2 = new DateRule() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep) {
        return null;
      }
    };
    CALCDTALLOW_2.setBusinessRuleId("CALCDTALLOW-2");
    CALCDTALLOW_2
        .setBusinessRule("The patient's allowable vaccine type end age date must be calculated as the patient’s date of birth plus the allowable vaccine type end Age.");
    CALCDTALLOW_2.setLogicalComponent("Allowable Vaccine");
    CALCDTALLOW_2.setFieldName("patient's allowable vaccine type end age date");
  }

}
