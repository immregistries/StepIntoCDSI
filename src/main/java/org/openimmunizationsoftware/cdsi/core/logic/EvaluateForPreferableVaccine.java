package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
// import org.openimmunizationsoftware.cdsi.core.logic.EvaluateAllowableInterval.LT;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateForPreferableVaccine extends LogicStep {
  public EvaluateForPreferableVaccine(DataModel dataModel) {
    super(LogicStepType.EVALUATE_FOR_PREFERABLE_VACCINE, dataModel);
    setConditionTableName("Table 6.8");


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
      logicTable.caVolume = new ConditionAttribute<String>(
          "Vaccine dose administered", "Volume");
      logicTable.caTradeName =
          new ConditionAttribute<String>("Vaccine dose administered", "Trade Name ");
      logicTable.caPreferableVaccineElements = new ConditionAttribute<PreferrableVaccine>(
          "Supporting Data", "Preferable Vaccine elements");
      logicTable.caVaccineTypeBeginAgeDate = new ConditionAttribute<Date>(
          "Vaccine Type Begin Age Date", "Calculated date (CALCDTPREF-1)");
      logicTable.caVaccineTypeEndAgeDate = new ConditionAttribute<Date>("Vaccine Type End Age Date",
          "Calculated date (CALCDTPREF-2)");

      
      logicTable.caDateAdministered.setInitialValue(aar.getDateAdministered());
      logicTable.caVolume.setInitialValue(aar.getVolume());
      logicTable.caTradeName.setInitialValue(aar.getTradeName());
      logicTable.caPreferableVaccineElements.setInitialValue(pi);
      logicTable.caVaccineTypeBeginAgeDate.setAssumedValue(PAST);
      logicTable.caVaccineTypeEndAgeDate.setAssumedValue(FUTURE);

      List<ConditionAttribute<?>> caList = new ArrayList<ConditionAttribute<?>>();
      caList.add(logicTable.caDateAdministered);
      caList.add(logicTable.caVolume);
      caList.add(logicTable.caTradeName);
      caList.add(logicTable.caPreferableVaccineElements);
      caList.add(logicTable.caVaccineTypeBeginAgeDate);
      caList.add(logicTable.caVaccineTypeEndAgeDate);

      conditionAttributesAdditionalMap.put("Preferrable Vaccine #" + count, caList);
      
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
    setNextLogicStepType(LogicStepType.EVALUATE_FOR_ALLOWABLE_VACCINE);
    for (LogicTable logicTable : logicTableList) {
      logicTable.evaluate();
      if (((LT) logicTable).getResult() == YesNo.YES) {
        setNextLogicStepType(LogicStepType.SATISFY_TARGET_DOSE);
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
        "<p>Figures 6-17 depicts a patient who received a preferable vaccine while figure 6-18 depicts a patient who did not receive a preferable vaccine.</p>");
    out.println("<img src=\"Figure 6.17.PNG\"/>");
    out.println("<p>FIGURE 6 - 17 PATIENT RECEIVED A PREFERABLE VACCINE</p>");
    out.println("<img src=\"Figure 6.18.PNG\"/>");
    out.println("<p>FIGURE 6 - 18 PATIENT DID NOT RECEIVE A PREFERABLE VACCINE</p>");
    out.println(
        "<p>It should be noted that volume is sparsely populated and tracked differently in most systems. Therefore, volume will not be used to evaluate the validity of a vaccine dose administered. However, it will be provided as an evaluation reason that less than sufficient volume was administered.</p>");
    out.println(
        "<p>The following process model, attribute table, decision table, and business rule table are used to evaluate for a preferable vaccine.</p>");
    out.println("<img src=\"Figure 6.19.PNG\"/>");
    out.println("<p>FIGURE 6 - 19 EVALUATE FOR A PREFERABLE VACCINE PROCESS MODEL</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable {
    private ConditionAttribute<Date> caDateAdministered = null;
    private ConditionAttribute<String> caVolume = null;
    private ConditionAttribute<String> caTradeName = null;
    private ConditionAttribute<PreferrableVaccine> caPreferableVaccineElements = null;
    private ConditionAttribute<Date> caVaccineTypeBeginAgeDate = null;
    private ConditionAttribute<Date> caVaccineTypeEndAgeDate = null;
    
    private YesNo result = null;

    public LT(int count) {
      super(4, 5,
          "Table 6-26 WAS THE VACCINE DOSE ADMINISTERED A PREFERABLE VACCINE FOR THE TARGET DOSE? #" + count);

      setLogicCondition(0, new LogicCondition(
          "Is the vaccine type of the vaccine dose administered the same as the vaccine type of a preferable vaccine for the target dose?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caPreferableVaccineElements.getFinalValue().getSeriesDose()
              .equals(dataModel.getAntigenAdministeredRecord().getVaccineType().getCvxCode())) {
            return LogicResult.YES;
          } else {
            return LogicResult.NO;
          }
        }
      });
      setLogicCondition(1, new LogicCondition(
          "Is the preferable vaccine type begin age date <= date administered < preferable vaccine type end age date?") {
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
        "Is the trade name of the vaccine dose administered the same as the trade name of the preferable vaccine for the target dose?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caTradeName.getFinalValue().equals(caPreferableVaccineElements.getFinalValue().getTradeName())) {
            return LogicResult.YES;
          } else {
            return LogicResult.NO;
          }
        }
      });
      setLogicCondition(3, new LogicCondition(
          "Is the volume of the vaccine dose administered ≥ the volume of the preferable vaccine for the target dose?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caVolume.equals("") || caVolume.getFinalValue().equalsIgnoreCase(caPreferableVaccineElements.getFinalValue().getVolume())) {
            return LogicResult.YES;
          }
          try {
            if (Double.parseDouble(caVolume.getFinalValue()) >= Double
                .parseDouble(caPreferableVaccineElements.getFinalValue().getVolume())) {
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
          log("Yes. The vaccine dose administered was a preferable vaccine for the target dose.");
        }
      });
      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          result = YesNo.YES;
          log("Yes. The vaccine dose administered was a preferable vaccine for the target dose. Evaluation Reason is 'Volume administered is less than recommended volume'.");
        }
      });
      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          result = YesNo.NO;
          log("No. The vaccine dose administered was not a preferable vaccine for the target dose.");
        }
      });
      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          result = YesNo.NO;
          log("No. The vaccine dose administered was not a preferable vaccine for the target dose. It was administered out of the recommended age range for the preferable vaccine.");
        }
      });
      setLogicOutcome(4, new LogicOutcome() {
        @Override
        public void perform() {
          result = YesNo.NO;
          log("No. The vaccine dose administered was not a preferable vaccine for the target dose. The trade name of the vaccine dose administered is not the same as the trade name of the preferable vaccine.");
        }
      });
    }

    public YesNo getResult() {
      return result;
    }
  }

}
