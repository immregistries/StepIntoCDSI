package org.immregistries.step.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.immregistries.step.core.data.DataModel;
import org.immregistries.step.core.domain.datatypes.YesNo;
import org.immregistries.step.core.logic.items.ConditionAttribute;
import org.immregistries.step.core.logic.items.LogicCondition;
import org.immregistries.step.core.logic.items.LogicOutcome;
import org.immregistries.step.core.logic.items.LogicResult;
import org.immregistries.step.core.logic.items.LogicTable;
import org.immregistries.step.domain.AntigenAdministeredRecord;
import org.immregistries.step.domain.PreferrableVaccine;

public class EvaluateForPreferableVaccine extends LogicStep {



  public EvaluateForPreferableVaccine(DataModel dataModel) {
    super(LogicStepType.EVALUATE_PREFERABLE_VACCINE_ADMINISTERED, dataModel);
    setConditionTableName("Table ");


    Date birthDate = dataModel.getPatient().getDateOfBirth();

    int i = 0, j = 0, k = 0;
    boolean allTrue = true;
    int count = 0;
    for (PreferrableVaccine pi : dataModel.getTargetDose().getTrackedSeriesDose()
        .getPreferrableVaccineList()) {
      count++;
      LT logicTable = new LT(count);

      logicTable.caDateAdministered =
          new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");

      AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
      logicTable.caPreferableVaccineTradeName = new ConditionAttribute<String>(
          "Supporting data (Preferable Vaccine)", "Preferable Vaccine Trade Name ");
      logicTable.caTradeName =
          new ConditionAttribute<String>("Vaccine dose administered", "Trade Name ");
      logicTable.caVaccineTypeBeginAgeDate = new ConditionAttribute<Date>(
          "Vaccine Type Begin Age Date", "Calculated date (CALCDTPREF-1)");
      logicTable.caVaccineTypeEndAgeDate = new ConditionAttribute<Date>("Vaccine Type End Age Date",
          "Calculated date (CALCDTPREF-2)");
      logicTable.caPreferableVaccineVolume = new ConditionAttribute<String>(
          "Preferable Vaccine Volume ", "Supporting data (Preferable Vaccine)");
      logicTable.caPreferableVaccineType = new ConditionAttribute<String>(
          "Preferable Vaccine Type ", "Supporting data (Preferable Vaccine)");
      logicTable.caVolume = aar.getVolume();
      logicTable.caTradeName.setInitialValue(aar.getAntigen().getName());
      // caPreferableVaccineTradeName.setInitialValue(
      // dataModel.getAntigenSeriesList().get(0).getSeriesDoseList().get(0).getAllowableVaccineList().get(0).getTradeName())
      // ;
      logicTable.caDateAdministered.setInitialValue(aar.getDateAdministered());

      logicTable.caVaccineTypeBeginAgeDate.setAssumedValue(PAST);
      logicTable.caVaccineTypeEndAgeDate.setAssumedValue(FUTURE);
      logicTable.caPreferableVaccineTradeName.setAssumedValue(aar.getAntigen().getName());
      logicTable.caPreferableVaccineVolume.setAssumedValue(aar.getVolume());

      List<ConditionAttribute<?>> caList = new ArrayList<ConditionAttribute<?>>();
      caList.add(logicTable.caDateAdministered);
      caList.add(logicTable.caTradeName);
      caList.add(logicTable.caVaccineTypeBeginAgeDate);
      caList.add(logicTable.caVaccineTypeEndAgeDate);
      caList.add(logicTable.caPreferableVaccineTradeName);
      caList.add(logicTable.caPreferableVaccineVolume);
      caList.add(logicTable.caPreferableVaccineType);

      conditionAttributesAdditionalMap.put("Preferrable Vaccine #" + count, caList);

      if (pi.getTradeName() != null && !pi.getTradeName().equals("")) {
        logicTable.caPreferableVaccineTradeName.setInitialValue(pi.getTradeName());
      }
      if (pi.getVolume() != null && !pi.getVolume().equals("")) {
        logicTable.caPreferableVaccineVolume.setInitialValue(pi.getVolume());
      }
      logicTable.pv = pi.getVolume();
      logicTable.caPreferableVaccineType.setInitialValue(pi.getVaccineType().getCvxCode());
      if (pi.getVaccineTypeBeginAge() != null) {
        logicTable.caVaccineTypeBeginAgeDate
            .setInitialValue(pi.getVaccineTypeBeginAge().getDateFrom(birthDate));
      }
      if (pi.getVaccineTypeEndAge() != null) {
        logicTable.caVaccineTypeEndAgeDate
            .setInitialValue(pi.getVaccineTypeEndAge().getDateFrom(birthDate));
      }

      logicTableList.add(logicTable);
    }


  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
    for (LogicTable logicTable : logicTableList) {
      logicTable.evaluate();
      if (((LT) logicTable).getResult() == YesNo.YES) {
        setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
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
    out.println("<h1> " + getTitle() + "</h1>");
    out.println(
        "<p>Evaluate for preferable vaccine validates the vaccine of a vaccine dose administered against the list of preferable vaccines.</p>");
    out.println(
        "<p>Figures 4-15 depicts a patient who received a preferable vaccine while figure 4-16 depicts a patient who did not receive a preferable vaccine.</p>");
    out.println("<img src=\"Figure 4.15.PNG\"/>");
    out.println("<p>FIGURE 4 - 15 PATIENT RECEIVED A PREFERABLE VACCINE</p>");
    out.println("<img src=\"Figure 4.16.PNG\"/>");
    out.println("<p>FIGURE 4 - 16 PATIENT DID NOT RECEIVE A PREFERABLE VACCINE</p>");
    out.println(
        "<p>It should be noted that volume is sparsely populated and tracked differently in most systems. Therefore, volume will not be used to evaluate the validity of a vaccine dose administered. However, it will be provided as an evaluation reason that less than sufficient volume was administered.</p>");
    out.println(
        "<p>The following process model, attribute table, decision table, and business rule table are used to evaluate for a preferable vaccine.</p>");
    out.println("<img src=\"Figure 4.17.PNG\"/>");
    out.println("<p>FIGURE 4 - 17 EVALUATE FOR A PREFERABLE VACCINE PROCESS MODEL</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    private ConditionAttribute<Date> caDateAdministered = null;
    // private ConditionAttribute<>
    private ConditionAttribute<String> caTradeName = null;
    private ConditionAttribute<Date> caVaccineTypeBeginAgeDate = null;
    private ConditionAttribute<Date> caVaccineTypeEndAgeDate = null;
    private ConditionAttribute<String> caPreferableVaccineTradeName = null;
    private ConditionAttribute<String> caPreferableVaccineVolume = null;
    private ConditionAttribute<String> caPreferableVaccineType = null;
    private String caVolume = null;
    private String pv = null;
    private YesNo result = null;

    public LT(int count) {
      super(4, 5,
          "Table 4-5 Was the supporting data defined preferrable vaccine administered? #" + count);

      setLogicCondition(0, new LogicCondition(
          "Is the vaccine type of the vaccine dose administered the same as the vaccine type of the preferable vaccine? ") {
        @Override
        public LogicResult evaluateInternal() {
          if (caPreferableVaccineType.getFinalValue()
              .equals(dataModel.getAntigenAdministeredRecord().getVaccineType().getCvxCode())) {
            return LogicResult.YES;
          } else {
            return LogicResult.NO;
          }
        }
      });
      setLogicCondition(1, new LogicCondition(
          "Is the Preferable vaccine type begin age date <= date administered < preferable vaccine type end age date? ") {
        @Override
        public LogicResult evaluateInternal() {
          if (caVaccineTypeBeginAgeDate.getFinalValue().before(caDateAdministered.getFinalValue())
              && caDateAdministered.getFinalValue()
                  .before(caVaccineTypeEndAgeDate.getFinalValue())) {
            return LogicResult.YES;
          } else {
            return LogicResult.NO;
          }
        }
      });
      setLogicCondition(2, new LogicCondition(
          "Is the vaccine dose administered trade name the same as the preferable vaccine trade name? ") {
        @Override
        public LogicResult evaluateInternal() {
          if (caTradeName.getFinalValue().equals(caPreferableVaccineTradeName.getFinalValue())) {
            return LogicResult.YES;
          } else {
            return LogicResult.NO;
          }
        }
      });
      setLogicCondition(3, new LogicCondition(
          "Is the Vaccine dose administered volume >= preferable vaccine volume? ") {
        @Override
        public LogicResult evaluateInternal() {
          if (caVolume.equals("") || caVolume.equalsIgnoreCase(caPreferableVaccineVolume.getFinalValue())) {
            return LogicResult.YES;
          }
          try {
            if (Double.parseDouble(caVolume) >= Double
                .parseDouble(caPreferableVaccineVolume.getFinalValue())) {
              return LogicResult.YES;
            } else {
              return LogicResult.NO;
            }
          } catch (NumberFormatException nfe) {
            return LogicResult.NO;
          }
        }
      });
      setLogicResults(0, new LogicResult[] {LogicResult.YES, LogicResult.YES, LogicResult.NO,
          LogicResult.YES, LogicResult.YES});
      setLogicResults(1, new LogicResult[] {LogicResult.YES, LogicResult.YES, LogicResult.ANY,
          LogicResult.NO, LogicResult.YES});
      setLogicResults(2, new LogicResult[] {LogicResult.YES, LogicResult.YES, LogicResult.ANY,
          LogicResult.ANY, LogicResult.NO});
      setLogicResults(3, new LogicResult[] {LogicResult.YES, LogicResult.NO, LogicResult.ANY,
          LogicResult.ANY, LogicResult.ANY});

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          result = YesNo.YES;
          log("Yes. A preferable vaccine was administered.");
          // setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          result = YesNo.YES;
          log("Yes. A preferable vaccine was administered. Evaluation Reason is volume administered is \"less than recommended volume.\"");
          // setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
        }
      });
      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          // setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
          result = YesNo.NO;
          log("No.  This supporting data defined preferable vaccine was not administered.");
        }
      });
      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          // setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
          result = YesNo.NO;
          log("No.  This supporting data defined preferable vaccine was administered out of the preferred age range.");
        }
      });
      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          // setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
          result = YesNo.NO;
          log("No. This supporting data defined preferable vaccine was of the wrong trade name. ");
        }
      });

      // setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.ANY);

      // setLogicOutcome(0, new LogicOutcome() {
      // @Override
      // public void perform() {
      // log("No. The target dose cannot be skipped. ");
      // log("Setting next step: 4.3 Substitute Target Dose");
      // setNextLogicStep(LogicStep.SUBSTITUTE_TARGET_DOSE_FOR_EVALUATION);
      // }
      // });
      //
    }

    public YesNo getResult() { // TODO Auto-generated method stub
      return result;
    }
  }

}
