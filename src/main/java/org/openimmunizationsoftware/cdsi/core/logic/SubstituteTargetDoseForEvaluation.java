package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.SubstituteDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class SubstituteTargetDoseForEvaluation extends LogicStep {

  private ConditionAttribute<Date> caDateAdministered =
      new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
  private ConditionAttribute<List<TargetDose>> caTargetDosesWithATargetDoseSatisfied =
      new ConditionAttribute<List<TargetDose>>("Patient Series",
          "Target Doses with a Target Dose \"Satisfied\"");
  private ConditionAttribute<Date>[] caFirstDoseBeginAgeDates;
  private ConditionAttribute<Date>[] caFirstDoseEndAgeDates;
  private ConditionAttribute<Integer>[] caTotalCountOfValidDoses;
  private ConditionAttribute<Integer>[] caNumberOfTargetDosesToSubstitutes;

  public SubstituteTargetDoseForEvaluation(DataModel dataModel) {
    super(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION, dataModel);

    setConditionTableName("Table 4-6 Substitute Target Dose Attributes");

    caDateAdministered =
        new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caTargetDosesWithATargetDoseSatisfied = new ConditionAttribute<List<TargetDose>>(
        "Patient Series", "Target Doses with a Target Dose \"Satisfied\"");

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caTargetDosesWithATargetDoseSatisfied);

    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    List<TargetDose> targetDoseSatisfiedList = new ArrayList<TargetDose>();
    for (TargetDose targetDose : dataModel.getTargetDoseList()) {
      if (targetDose.getTargetDoseStatus() == TargetDoseStatus.SATISFIED) {
        targetDoseSatisfiedList.add(targetDose);
      }
    }
    caTargetDosesWithATargetDoseSatisfied.setInitialValue(targetDoseSatisfiedList);

    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    int substituteCount = seriesDose.getSubstituteDoseList().size();
    caFirstDoseBeginAgeDates = new ConditionAttribute[0];
    caFirstDoseEndAgeDates = new ConditionAttribute[0];
    caTotalCountOfValidDoses = new ConditionAttribute[0];
    caNumberOfTargetDosesToSubstitutes = new ConditionAttribute[0];

    Date dateOfBirth = dataModel.getPatient().getDateOfBirth();
    for (int i = 0; i < substituteCount; i++) {
      SubstituteDose substituteDose = seriesDose.getSubstituteDoseList().get(i);
      String s = " [" + (i + 1) + "]";
      caFirstDoseBeginAgeDates[i] =
          new ConditionAttribute<Date>("Calculated Date", "First Dose Begin Age Date" + s);
      caFirstDoseEndAgeDates[i] =
          new ConditionAttribute<Date>("Calculated Date", "First Dose End Age Date" + s);
      caTotalCountOfValidDoses[i] =
          new ConditionAttribute<Integer>("Supporting Data", "Total Count of Valid Doses" + s);
      caNumberOfTargetDosesToSubstitutes[i] = new ConditionAttribute<Integer>("Supporting Data",
          "Number of Target Doses to substitute" + s);

      caFirstDoseBeginAgeDates[i]
          .setInitialValue(substituteDose.getFirstDoseBeginAge().getDateFrom(dateOfBirth));
      caFirstDoseEndAgeDates[i]
          .setInitialValue(substituteDose.getFirstDoseEndAge().getDateFrom(dateOfBirth));
      caTotalCountOfValidDoses[i].setInitialValue(substituteDose.getTotalCountOfValidDoses());
      caNumberOfTargetDosesToSubstitutes[i]
          .setInitialValue(substituteDose.getNumberOfTargetDosesToSubstitue());

      conditionAttributesList.add(caFirstDoseBeginAgeDates[i]);
      conditionAttributesList.add(caFirstDoseEndAgeDates[i]);
      conditionAttributesList.add(caTotalCountOfValidDoses[i]);
      conditionAttributesList.add(caNumberOfTargetDosesToSubstitutes[i]);

      LT logicTable = new LT(i);
      logicTableList.add(logicTable);
    }

  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_AGE);
    for (LogicTable logicTable : logicTableList) {
      logicTable.evaluate();
      if (((LT) logicTable).isTargetDosesCanBeSubstituted()) {
        setNextLogicStepType(LogicStepType.EVALUATE_VACCINE_DOSE_ADMINISTERED);
        break;
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
  }

  private void printStandard(PrintWriter out) {
    out.println("<h1> " + logicStepType.getDisplay() + "</h1>");
    out.println(
        "<p>Substitute target dose is similar to skip target dose as a means to adjust where the patient is in the patient series. The goal of substitute target dose is to look at previously satisfied target doses within the patient series to determine how many future target doses `if any` can be substituted and not recommended.</p>");
    out.println(
        "<p>When a target dose does specify substitute target dose attributes, it will contain a set of substitution possibilities. If a substitution is found, the remaining substitute target dose sets can be ignored. If all of the sets are examined and no substitution is found, then the current target dose should be used for evaluation.</p>");

    out.println("<h2>Relationship to ACIP recommendations:</h2>");
    out.println(
        "<p>At present, substitute target dose is only used for children who have partially completed their DTaP series and have turned seven years old. Once the child is seven years old, the number of Tdap/Td doses recommended is based on the number of DTaP vaccine doses administered the child received prior to age seven. See MMWR 2006; 55 (No. RR-3); Appendix D.</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    private int position;
    private boolean targetDosesCanBeSubstituted = false;

    public boolean isTargetDosesCanBeSubstituted() {
      return targetDosesCanBeSubstituted;
    }

    public LT(int p) {
      super(2, 3, "Table 4-7 Can Target Doses Be Substituted [" + (p + 1) + "]");
      this.position = p;

      setLogicCondition(0, new LogicCondition(
          "First dose begin age date <= date administered of first satisfied target dose in patient series < first dose end age date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caTargetDosesWithATargetDoseSatisfied.getFinalValue().size() == 0) {
            log("There is no first target dose");
            return LogicResult.NO;
          }
          TargetDose firstTargetDose = caTargetDosesWithATargetDoseSatisfied.getFinalValue().get(0);
          Date adminDate =
              firstTargetDose.getSatisfiedByVaccineDoseAdministered().getDateAdministered();
          if (caFirstDoseBeginAgeDates[position].getFinalValue().after(adminDate)) {
            log("First dose given before begin range");
            return LogicResult.NO;
          }
          if (adminDate.before(caFirstDoseEndAgeDates[position].getFinalValue())) {
            log("First dose given before end range");
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicCondition(0, new LogicCondition(
          "Total count of satisfied target doses in patient series = substitute dose total count of valid doses?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caTargetDosesWithATargetDoseSatisfied.getFinalValue()
              .size() == caNumberOfTargetDosesToSubstitutes[position].getFinalValue()) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicResults(0, LogicResult.YES, LogicResult.YES, LogicResult.NO);
      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.ANY);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("(1) Yes. Target doses can be substituted. ");
          log("(2) The new target dose is calculated as the current target dose plus the number of target doses to substitute. ");
          int subCount = caNumberOfTargetDosesToSubstitutes[position].getFinalValue();
          log(" + will substitute " + subCount + " dose" + (subCount == 1 ? "" : "s"));
          log("(3) Each target dose which is substituted must have the target dose status \"substituted.\" ");
          TargetDose targetDose = dataModel.getTargetDose();
          TargetDose targetDoseNext = dataModel.findNextTargetDose(dataModel.getTargetDose());
          for (int i = 0; i < caNumberOfTargetDosesToSubstitutes[position].getFinalValue(); i++) {
            log(" + setting dose " + targetDose.getTrackedSeriesDose().getDoseNumber()
                + " target dose status of \"substituted\"");
            targetDose.setTargetDoseStatus(TargetDoseStatus.SUBSTITUTED);
            targetDose = targetDoseNext;
            targetDoseNext = dataModel.findNextTargetDose(targetDose);
          }
          dataModel.setTargetDose(targetDoseNext);
          setNextLogicStepType(LogicStepType.EVALUATE_DOSE_ADMININISTERED_CONDITION);
          log("Setting next step: 4.1 Dose Administered Condition");
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. Target doses cannot be substituted.");
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. Target doses cannot be substituted.");
        }
      });
    }
  }

}
