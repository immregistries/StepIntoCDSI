package org.openimmunizationsoftware.cdsi.core.logic.concepts;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipCondition;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;

public class DateRules {
  // public static DateRule CALCDT_1 = null;
  // public static DateRule CALCDT_2 = null;
  // public static DateRule CALCDT_3 = null;
  // public static DateRule CALCDT_4 = null;
  // public static DateRule CALCDT_5 = null;
  // public static DateRule CALCDT_6 = null;

  // public static DateRule<?> CALCDTSKIP_1 = null;
  // public static DateRule<?> CALCDTSKIP_2 = null;
  public static DateRule<ConditionalSkipCondition> CALCDTSKIP_3 = null;
  public static DateRule<ConditionalSkipCondition> CALCDTSKIP_4 = null;
  public static DateRule<ConditionalSkipCondition> CALCDTSKIP_5 = null;
  public static DateRule<Age> CALCDTAGE_1 = null;
  public static DateRule<Age> CALCDTAGE_2 = null;
  public static DateRule<Age> CALCDTAGE_3 = null;
  public static DateRule<Age> CALCDTAGE_4 = null;
  public static DateRule<Age> CALCDTAGE_5 = null;
  public static DateRule<Interval> CALCDTINT_1 = null;
  public static DateRule<Interval> CALCDTINT_2 = null;
  public static DateRule<Interval> CALCDTINT_3 = null;
  public static DateRule<Interval> CALCDTINT_4 = null;
  public static DateRule<Interval> CALCDTINT_5 = null;
  public static DateRule<Interval> CALCDTINT_6 = null;
  public static DateRule<Interval> CALCDTINT_7 = null;
  public static DateRule<Interval> CALCDTINT_8 = null;
  public static DateRule<LiveVirusConflict> CALCDTLIVE_1 = null;
  public static DateRule<LiveVirusConflict> CALCDTLIVE_2 = null;
  public static DateRule<LiveVirusConflict> CALCDTLIVE_3 = null;
  public static DateRule<LiveVirusConflict> CALCDTLIVE_4 = null;
  public static DateRule<PreferrableVaccine> CALCDTPREF_1 = null;
  public static DateRule<PreferrableVaccine> CALCDTPREF_2 = null;
  public static DateRule<AllowableVaccine> CALCDTALLOW_1 = null;
  public static DateRule<AllowableVaccine> CALCDTALLOW_2 = null;
  public static DateRule<Contraindication> CALCDTCI_1 = null;
  public static DateRule<Contraindication> CALCDTCI_2 = null;
  public static DateRule<Contraindication> FORECASTDTCAN_1 = null;
  
  // public static DateRule CALCDTCOND_1 = null;
  // public static DateRule CALCDTCOND_2 = null;

  static {

    CALCDTSKIP_3 = new DateRule<ConditionalSkipCondition>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          ConditionalSkipCondition conditionalSkipCondition) {
        if (conditionalSkipCondition.getBeginAge() == null
            || !conditionalSkipCondition.getBeginAge().isValued()) {
          return null;
        }
        return conditionalSkipCondition.getBeginAge()
            .getDateFrom(dataModel.getPatient().getDateOfBirth());
      }
    };
    CALCDTSKIP_3.setBusinessRuleId("CALCDTSKIP-3");
    CALCDTSKIP_3.setBusinessRule(
        "A patient's conditional skip begin age date must be calculated as the patient's date of birth plus the conditional skip begin age of a conditional skip.");
    CALCDTSKIP_3.setLogicalComponent("Skip Target Dose");

    CALCDTSKIP_4 = new DateRule<ConditionalSkipCondition>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          ConditionalSkipCondition conditionalSkipCondition) {
        if (conditionalSkipCondition.getEndAge() == null
            || !conditionalSkipCondition.getEndAge().isValued()) {
          return null;
        }
        return conditionalSkipCondition.getEndAge()
            .getDateFrom(dataModel.getPatient().getDateOfBirth());
      }
    };
    CALCDTSKIP_4.setBusinessRuleId("CALCDTSKIP-4");
    CALCDTSKIP_4.setBusinessRule(
        "A patient's conditional skip end age date must be calculated as the patient's date of birth plus the conditional skip end age of a conditional skip.");
    CALCDTSKIP_4.setLogicalComponent("Skip Target Dose");

    CALCDTSKIP_5 = new DateRule<ConditionalSkipCondition>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          ConditionalSkipCondition conditionalSkipCondition) {
        if (dataModel.getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose() == null) {
          return null;
        }
        if (conditionalSkipCondition.getInterval() == null
            || !conditionalSkipCondition.getInterval().isValued()) {
          return null;
        }
        return conditionalSkipCondition.getInterval().getDateFrom(dataModel
            .getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose().getDateAdministered());
      }
    };
    CALCDTSKIP_5.setBusinessRuleId("CALCDTSKIP-5");
    CALCDTSKIP_5.setBusinessRule(
        "A patient's conditional skip interval date must be calculated as the vaccine date administered from the immediate previous vaccine dose administered plus the Interval of the conditional skip condition. ");
    CALCDTSKIP_5.setLogicalComponent("Skip Target Dose");

    CALCDTAGE_1 = new DateRule<Age>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Age age) {
        return null;
      }
    };
    CALCDTAGE_1.setBusinessRuleId("CALCDTAGE-1");
    CALCDTAGE_1.setBusinessRule(
        "The patient's maximum age date must be calculated as the patient's date of birth plus the maximum age.");
    CALCDTAGE_1.setLogicalComponent("Age");
    CALCDTAGE_1.setFieldName("patient's maximum age date");

    CALCDTAGE_2 = new DateRule<Age>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Age age) {
        return null;
      }
    };
    CALCDTAGE_2.setBusinessRuleId("CALCDTAGE-2");
    CALCDTAGE_2.setBusinessRule(
        "The patient's latest recommended age date must be calculated as the patient's date of birth plus the latest recommended age.");
    CALCDTAGE_2.setLogicalComponent("Age");
    CALCDTAGE_2.setFieldName("patient's latest recommended age date");

    CALCDTAGE_3 = new DateRule<Age>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Age age) {
        return null;
      }
    };
    CALCDTAGE_3.setBusinessRuleId("CALCDTAGE-3");
    CALCDTAGE_3.setBusinessRule(
        "The patient's earliest recommended age date must be calculated as the patient's date of birth plus the earliest recommended age.");
    CALCDTAGE_3.setLogicalComponent("Age");
    CALCDTAGE_3.setFieldName("patient's earliest recommended age date");

    CALCDTAGE_4 = new DateRule<Age>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Age age) {
        return null;
      }
    };
    CALCDTAGE_4.setBusinessRuleId("CALCDTAGE-4");
    CALCDTAGE_4.setBusinessRule(
        "The patient's minimum age date must be calculated as the patient's date of birth plus the minimum age.");
    CALCDTAGE_4.setLogicalComponent("Age");
    CALCDTAGE_4.setFieldName("patient's minimum age date");

    CALCDTAGE_5 = new DateRule<Age>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Age age) {
        return null;
      }
    };
    CALCDTAGE_5.setBusinessRuleId("CALCDTAGE-5");
    CALCDTAGE_5.setBusinessRule(
        "The patient's absolute minimum age date must be calculated as the patient's date of birth plus the absolute minimum age.");
    CALCDTAGE_5.setLogicalComponent("Age");
    CALCDTAGE_5.setFieldName("patient's absolute minimum age date");

    CALCDTINT_1 = new DateRule<Interval>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Interval interval) {
        return null;
      }
    };
    CALCDTINT_1.setBusinessRuleId("CALCDTINT-1");
    CALCDTINT_1.setBusinessRule(
        "The patient's reference dose date must be calculated as the date administered of the most immediate previous vaccine dose administered which has evaluation status \"Valid\" or \"Not Valid\" if from immediate previous dose administered is \"Y\".");
    CALCDTINT_1.setLogicalComponent("Interval");
    CALCDTINT_1.setFieldName("patient's reference dose date");

    CALCDTINT_2 = new DateRule<Interval>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Interval interval) {
        return null;
      }
    };
    CALCDTINT_2.setBusinessRuleId("CALCDTINT-2");
    CALCDTINT_2.setBusinessRule(
        "The patient's reference dose date must be calculated as the date administered of the vaccine dose administered which satisfies the target dose defined in the interval from target dose number in series if from immediate previous dose administered is \"N\" and from target dose number in series is not \"n/a\".");
    CALCDTINT_2.setLogicalComponent("Interval");
    CALCDTINT_2.setFieldName("patient's reference dose date");

    CALCDTINT_3 = new DateRule<Interval>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Interval interval) {
        if (interval != null) {
          if (interval.getAbsoluteMinimumInterval() != null) {
            SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
            Date dob = dataModel.getAntigenAdministeredRecord().getDateAdministered();
            int absMinIntAmount = interval.getAbsoluteMinimumInterval().getAmount();
            Date absMinIntDate = new Date();
            switch (interval.getAbsoluteMinimumInterval().getType()) {
              case DAY:
                absMinIntDate = DateUtils.addDays(dob, absMinIntAmount);
                break;
              case WEEK:
                absMinIntDate = DateUtils.addWeeks(dob, absMinIntAmount);
                break;
              case MONTH:
                absMinIntDate = DateUtils.addMonths(dob, absMinIntAmount);
                break;
              case YEAR:
                absMinIntDate = DateUtils.addYears(dob, absMinIntAmount);
                break;
              default:
                break;
            }
            return absMinIntDate;
          } else {
            return null;
          }
        } else {
          return null;
        }
      }
    };
    CALCDTINT_3.setBusinessRuleId("CALCDTINT-3");
    CALCDTINT_3.setBusinessRule(
        "The patient's absolute minimum interval date must be calculated as the patient's reference dose date plus the absolute minimum interval.");
    CALCDTINT_3.setLogicalComponent("Interval");
    CALCDTINT_3.setFieldName("patient's absolute minimum interval date");

    CALCDTINT_4 = new DateRule<Interval>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Interval interval) {
        if (interval != null) {
          if (interval.getMinimumInterval() != null) {
            SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
            Date dob = dataModel.getAntigenAdministeredRecord().getDateAdministered();
            int minIntAmount = interval.getMinimumInterval().getAmount();
            Date minIntDate = new Date();
            switch (interval.getMinimumInterval().getType()) {
              case DAY:
                minIntDate = DateUtils.addDays(dob, minIntAmount);
                break;
              case WEEK:
                minIntDate = DateUtils.addWeeks(dob, minIntAmount);
                break;
              case MONTH:
                minIntDate = DateUtils.addMonths(dob, minIntAmount);
                break;
              case YEAR:
                minIntDate = DateUtils.addYears(dob, minIntAmount);
                break;
              default:
                break;
            }
            return minIntDate;
          } else {
            return null;
          }
        } else {
          return null;
        }
      }
    };
    CALCDTINT_4.setBusinessRuleId("CALCDTINT-4");
    CALCDTINT_4.setBusinessRule(
        "The patient's minimum interval date must be calculated as the patient's reference dose date plus the minimum interval.");
    CALCDTINT_4.setLogicalComponent("Interval");
    CALCDTINT_4.setFieldName("patient's minimum interval date");

    CALCDTINT_5 = new DateRule<Interval>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Interval interval) {
        return null;
      }
    };
    CALCDTINT_5.setBusinessRuleId("CALCDTINT-5");
    CALCDTINT_5.setBusinessRule(
        "The patient's earliest recommended interval date must be calculated as the patient's reference dose date plus the earliest recommended interval.");
    CALCDTINT_5.setLogicalComponent("Interval");
    CALCDTINT_5.setFieldName("patient's earliest recommended interval date");

    CALCDTINT_6 = new DateRule<Interval>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Interval interval) {
        return null;
      }
    };
    CALCDTINT_6.setBusinessRuleId("CALCDTINT-6");
    CALCDTINT_6.setBusinessRule(
        "The patient's latest recommended interval date must be calculated as the patient's reference dose date plus the latest recommended interval.");
    CALCDTINT_6.setLogicalComponent("Interval");
    CALCDTINT_6.setFieldName("patient's latest recommended interval date");

    CALCDTINT_7 = new DateRule<Interval>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Interval interval) {
        return null;
      }
    };
    CALCDTINT_7.setBusinessRuleId("CALCDTINT-7");
    CALCDTINT_7.setBusinessRule(
        "The patient's latest minimum interval date must be the latest date of all calculated minimum interval dates for a given target dose.");
    CALCDTINT_7.setLogicalComponent("Interval");
    CALCDTINT_7.setFieldName("patient's latest minimum interval date");

    CALCDTINT_8 = new DateRule<Interval>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep, Interval interval) {
        return null;
      }
    };
    CALCDTINT_8.setBusinessRuleId("CALCDTINT-8");
    CALCDTINT_8.setBusinessRule(
        "A patient's reference dose date must be calculated as the most recent vaccine dose administered which is of the same vaccine type as the supporting data defined from most recent vaccine type if from immediate previous dose administered is \"N\" and from most recent is not \"n/a\".");
    CALCDTINT_8.setLogicalComponent("Interval");
    // CALCDTINT_8.setFieldName("patient's latest minimum interval date");

    CALCDTLIVE_1 = new DateRule<LiveVirusConflict>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          LiveVirusConflict liveVirusConflict) {
        return null;
      }
    };
    CALCDTLIVE_1.setBusinessRuleId("CALCDTLIVE-1");
    CALCDTLIVE_1.setBusinessRule(
        "The patient's conflict begin interval date must be calculated as the date administered of the conflicting vaccine dose administered plus the live virus conflict begin interval.");
    CALCDTLIVE_1.setLogicalComponent("Live Virus Conflict");
    CALCDTLIVE_1.setFieldName("patient's conflict begin interval date");

    CALCDTLIVE_2 = new DateRule<LiveVirusConflict>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          LiveVirusConflict liveVirusConflict) {
        return null;
      }
    };
    CALCDTLIVE_2.setBusinessRuleId("CALCDTLIVE-2");
    CALCDTLIVE_2.setBusinessRule(
        "The patient's conflict end interval date must be calculated as the date administered of the conflicting vaccine dose administered plus the live virus minimum conflict end interval when the conflicting vaccine dose administered has evaluation status \"valid.\"");
    CALCDTLIVE_2.setLogicalComponent("Live Virus Conflict");
    CALCDTLIVE_2.setFieldName("patient's conflict end interval date");

    CALCDTLIVE_3 = new DateRule<LiveVirusConflict>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          LiveVirusConflict liveVirusConflict) {
        return null;
      }
    };
    CALCDTLIVE_3.setBusinessRuleId("CALCDTLIVE-3");
    CALCDTLIVE_3.setBusinessRule(
        "The patient's conflict end interval date must be calculated as the date administered of the conflicting vaccine dose administered plus the live virus conflict end interval when the conflicting vaccine dose administered does not have evaluation status \"valid.\"");
    CALCDTLIVE_3.setLogicalComponent("Live Virus Conflict");
    CALCDTLIVE_3.setFieldName("patient's conflict end interval date");

    CALCDTLIVE_4 = new DateRule<LiveVirusConflict>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          LiveVirusConflict liveVirusConflict) {
        return null;
      }
    };
    CALCDTLIVE_4.setBusinessRuleId("CALCDTLIVE-4");
    CALCDTLIVE_4.setBusinessRule(
        "The patient's latest conflict end interval date must be the latest date of all calculated conflict end dates for a given target dose.");
    CALCDTLIVE_4.setLogicalComponent("Live Virus Conflict");
    CALCDTLIVE_4.setFieldName("patient's latest conflict end interval date");

    CALCDTPREF_1 = new DateRule<PreferrableVaccine>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          PreferrableVaccine preferrableVaccine) {
        return null;
      }
    };
    CALCDTPREF_1.setBusinessRuleId("CALCDTPREF-1");
    CALCDTPREF_1.setBusinessRule(
        "The patient's preferable vaccine type begin age date must be calculated as the patient's date of birth plus the preferable vaccine type begin age.");
    CALCDTPREF_1.setLogicalComponent("Preferable Vaccine");
    CALCDTPREF_1.setFieldName("patient's preferable vaccine type begin age date");

    CALCDTPREF_2 = new DateRule<PreferrableVaccine>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          PreferrableVaccine preferrableVaccine) {
        return null;
      }
    };
    CALCDTPREF_2.setBusinessRuleId("CALCDTPREF-2");
    CALCDTPREF_2.setBusinessRule(
        "The patient's preferable vaccine type end age date must be calculated as the patient's date of birth plus the preferable vaccine type end age.");
    CALCDTPREF_2.setLogicalComponent("Preferable Vaccine");
    CALCDTPREF_2.setFieldName("patient's preferable vaccine type end age date");

    CALCDTALLOW_1 = new DateRule<AllowableVaccine>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          AllowableVaccine allowableVaccine) {
        return null;
      }
    };
    CALCDTALLOW_1.setBusinessRuleId("CALCDTALLOW-1");
    CALCDTALLOW_1.setBusinessRule(
        "The patient's allowable vaccine type begin age date must be calculated as the patient's date of birth plus the allowable vaccine type begin age.");
    CALCDTALLOW_1.setLogicalComponent("Allowable Vaccine");
    CALCDTALLOW_1.setFieldName("patient's allowable vaccine type begin age date");

    CALCDTALLOW_2 = new DateRule<AllowableVaccine>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          AllowableVaccine allowableVaccine) {
        return null;
      }
    };
    CALCDTALLOW_2.setBusinessRuleId("CALCDTALLOW-2");
    CALCDTALLOW_2.setBusinessRule(
        "The patient's allowable vaccine type end age date must be calculated as the patient's date of birth plus the allowable vaccine type end Age.");
    CALCDTALLOW_2.setLogicalComponent("Allowable Vaccine");
    CALCDTALLOW_2.setFieldName("patient's allowable vaccine type end age date");

    CALCDTCI_1 = new DateRule<Contraindication>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          Contraindication contraindication) {
        if (contraindication.getContraindicationBeginAge() == null) {
          return null;
        }
        return contraindication.getContraindicationBeginAge().getDateFrom(dataModel.getPatient().getDateOfBirth());
      }
    };
    CALCDTCI_1.setBusinessRuleId("CALCDTCI_1");
    CALCDTCI_1.setBusinessRule("A patient's contraindication begin age date must be calculated as the patient's date of birth plus the contraindication begin age of a contraindication.");
    CALCDTCI_1.setLogicalComponent("Contraindication begin age date");

    CALCDTCI_2 = new DateRule<Contraindication>() {
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          Contraindication contraindication) {
        if (contraindication.getContraindicationEndAge() == null) {
          return null;
        }
        return contraindication.getContraindicationEndAge().getDateFrom(dataModel.getPatient().getDateOfBirth());
      }
    };
    CALCDTCI_2.setBusinessRuleId("CALCDTCI_2");
    CALCDTCI_2.setBusinessRule("A patient's contraindication end age date must be calculated as the patient's date of birth plus the contraindication end age of a contraindication.");
    CALCDTCI_2.setLogicalComponent("Contraindication begin end date");

    FORECASTDTCAN_1 = new DateRule<Contraindication>() {
      //TODO logic not correct
      @Override
      protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep,
          Contraindication contraindication) {
        if (contraindication.getContraindicationBeginAge() == null) {
          return null;
        }
        return contraindication.getContraindicationBeginAge().getDateFrom(dataModel.getPatient().getDateOfBirth());
      }
    };
    FORECASTDTCAN_1.setBusinessRuleId("FORECASTDTCAN_1");
    FORECASTDTCAN_1.setBusinessRule("");
    FORECASTDTCAN_1.setLogicalComponent("Candidate Earliest Date");

    // CALCDTCOND_1 = new DateRule() {
    // @Override
    // protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep)
    // {
    // return null;
    // }
    // };
    // CALCDTCOND_1.setBusinessRuleId("CALCDTCOND-1");
    // CALCDTCOND_1
    // .setBusinessRule("Retired in version 2.1 - No Longer Used.\n" +
    // "The patient's conditional begin age date must be calculated as the
    // patient's date of birth plus the conditional begin age.");
    // CALCDTCOND_1.setLogicalComponent("n/a");
    //// CALCDTCOND_1.setFieldName("patient's allowable vaccine type end age
    // date");
    //
    // CALCDTCOND_2 = new DateRule() {
    // @Override
    // protected Date evaluateInternal(DataModel dataModel, LogicStep logicStep)
    // {
    // return null;
    // }
    // };
    // CALCDTCOND_2.setBusinessRuleId("CALCDTCOND-2");
    // CALCDTCOND_2
    // .setBusinessRule("Retired in version 2.1 - No Longer Used.\n" +
    // "The patient's conditional end age date must be calculated as the
    // patient's date of birth plus the conditional end age.");
    // CALCDTCOND_2.setLogicalComponent("n/a");
    //// CALCDTCOND_2.setFieldName("patient's allowable vaccine type end age
    // date");
  }

}
