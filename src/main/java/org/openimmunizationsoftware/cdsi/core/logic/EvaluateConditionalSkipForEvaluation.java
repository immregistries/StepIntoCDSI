package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTSKIP_3;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTSKIP_4;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTSKIP_5;
import static org.openimmunizationsoftware.cdsi.core.logic.items.BusinessRuleTable.CONDSKIP_1;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkip;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipCondition;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipConditionType;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipSet;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionalSkipElements;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateConditionalSkipForEvaluation extends LogicStep {

  // Creating null attributes for use later
  protected ConditionAttribute<Date> caDateAdministered = null;
  protected ConditionAttribute<Date> caExpirationDate = null;
  protected ConditionAttribute<Date> caAssessmentDate = null;
  protected ConditionAttribute<Integer> caAdministeredDoseCount = null;
  protected boolean isForecast;
  protected boolean isValidating;

  // Constructor 1
  protected EvaluateConditionalSkipForEvaluation(LogicStepType logicStepType, DataModel dataModel) {
    super(logicStepType, dataModel);
  }

  // Constructor 2
  // Naming the condition table, using setupInternal function
  public EvaluateConditionalSkipForEvaluation(DataModel dataModel) {
    super(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION, dataModel);
    setConditionTableName("Table 6.4 Conditional Skip Attributes");
    setupInternal(dataModel, LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE, LogicStepType.FOR_EACH_PATIENT_SERIES);
  }

  // Defining setupInternal function; appears to change the isForecast variable depending on if noSkip == EVALUATE_FOR_INADVERTENT_VACCINE
  // If constructor 2 is called, noSkip will equal EVALUATE_FOR_INADVERTENT_VACCINE
  // isForecast is meant to identify whether or not this is for EvaluateConditionalSkipForEvaluation or EvaluateConditionalSkipForForecast
  protected void setupInternal(DataModel dataModel, final LogicStepType noSkip,
      final LogicStepType skip) {
    if (noSkip.equals(LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE)) {
      isForecast = false;
      isValidating = false;
    } else if(noSkip.equals(LogicStepType.VALIDATE_RECOMMENDATIONS)) {
      isValidating = true;
    } else {
      isForecast = true;
    }

    // Defining variables initialized earlier
    caDateAdministered =
        new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caAdministeredDoseCount =
        new ConditionAttribute<Integer>("Patient Immunization History", "Administered Dose Count");
    
    caExpirationDate = new ConditionAttribute<Date>("Vaccine", "Expiration Date");
    caAssessmentDate = new ConditionAttribute<Date>("Runtime data", "Assessment Date");

    caAssessmentDate.setAssumedValue(new Date());

    // this list is just for printing?
    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caAssessmentDate);
    conditionAttributesList.add(caAdministeredDoseCount);

    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    caExpirationDate.setInitialValue(aar.getLotExpirationDate());
    caAssessmentDate.setInitialValue(dataModel.getAssessmentDate());
    caAdministeredDoseCount.setInitialValue(dataModel.getAntigenAdministeredRecordList().size());

    // This appears to be the core logic of the function?
    /*
     * TODO: 
      I'm sure there is a better place to list this but before the for loop starts, we should eliminate Conditional Skip instances without a context of Evaluation or Both
     */
    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    if (seriesDose.getConditionalSkip() != null) {
      LT611 logicTable611 = new LT611(noSkip, skip);
      // logicTableList.add(logicTable611);
      log("Conditional skip has been defined, now looking at the details.");
      ConditionalSkip conditionalSkip = seriesDose.getConditionalSkip();
      logicTable611.setSetLogicType(conditionalSkip.getSetLogic());

      // 2. First for loop box; For Each Conditional Skip Set
      for (ConditionalSkipSet conditionalSkipSet : conditionalSkip.getConditionalSkipSetList()) {
        LT610 logicTable610 = new LT610();
        logicTable610.setConditionLogicType(conditionalSkipSet.getConditionLogic());

        // 3. Second for loop box; For Each Condition in a Set
        for (ConditionalSkipCondition condition : conditionalSkipSet.getConditionList()) {
          
          // 4. Evaluate condition; uses Business Rule Table 4-5 and Decision Tables 4-6, 4-7, and 4-8
          // Defining the condition as a class that corresponds to each Decision Table
          LTInnerSet lt = null;
          if (condition.getConditionType() == ConditionalSkipConditionType.AGE) {
            lt = new LT66();
          } else if (condition.getConditionType() == ConditionalSkipConditionType.INTERVAL) {
            lt = new LT68();
          } else if (condition
              .getConditionType() == ConditionalSkipConditionType.VACCINE_COUNT_BY_AGE
              || condition
                  .getConditionType() == ConditionalSkipConditionType.VACCINE_COUNT_BY_DATE) {
            lt = new LT69();
          }
          if (lt != null) {
            logicTableList.add(lt);
            lt.caConditionalSkipElements = new ConditionAttribute<ConditionalSkipElements>("Supporting Data (Conditional Skip)", "Conditional Skip Elements");
            lt.caStartDate = new ConditionAttribute<Date>(
              "Supporting Data (Conditional Skip)", "Start Date");
            lt.caEndDate = new ConditionAttribute<Date>(
              "Supporting Data (Conditional Skip)", "End Date");
            lt.caConditionalSkipBeginAgeDate = new ConditionAttribute<Date>(
                "Calculated date (CALCDTSKIP-3)", "Conditional Skip Begin Age Date");
            lt.caConditionalSkipEndAgeDate = new ConditionAttribute<Date>(
                "Calculated date (CALCDTSKIP-4)", "Conditional Skip End Age Date");
            lt.caConditionalSkipIntervalDate = new ConditionAttribute<Date>(
                "Calculated date (CALCDTSKIP-5)", "Conditional Skip Interval Date");
            lt.caNumberofConditionalDosesAdministered = new ConditionAttribute<Integer>(
                "Supporting Data (CONDSKIP-1)", "Number of Conditional Doses Administered");
            lt.caConditionalSkipReferenceDate = new ConditionAttribute<Date>(
                "Supporting Data (CONDSKIP-2)", "Conditional Skip Reference Date");

            // caList is also just for printing?
            List<ConditionAttribute<?>> caList = new ArrayList<ConditionAttribute<?>>();
            caList.add(lt.caStartDate);
            caList.add(lt.caEndDate);
            caList.add(lt.caConditionalSkipElements);
            caList.add(lt.caConditionalSkipBeginAgeDate);
            caList.add(lt.caConditionalSkipEndAgeDate);
            caList.add(lt.caConditionalSkipIntervalDate);
            caList.add(lt.caConditionalSkipReferenceDate);
            conditionAttributesAdditionalMap.put("Table 6 - 4 Conditional Skip Attributes "
                + conditionalSkipSet.getSetId() + "." + condition.getConditionId(), caList);

            // setting assumed values
            lt.caStartDate.setAssumedValue(PAST);
            lt.caEndDate.setAssumedValue(FUTURE);
            lt.caConditionalSkipBeginAgeDate.setAssumedValue(PAST);
            lt.caConditionalSkipEndAgeDate.setAssumedValue(FUTURE);

            // setting initial values
            lt.caStartDate.setInitialValue(condition.getStartDate());
            lt.caEndDate.setInitialValue(condition.getEndDate());
            lt.caConditionalSkipBeginAgeDate
                .setInitialValue(CALCDTSKIP_3.evaluate(dataModel, this, condition));
            lt.caConditionalSkipEndAgeDate
                .setInitialValue(CALCDTSKIP_4.evaluate(dataModel, this, condition));
            lt.caConditionalSkipIntervalDate
                .setInitialValue(CALCDTSKIP_5.evaluate(dataModel, this, condition));

            
            if (condition.getDoseType() != null){
              lt.caConditionalSkipElements.setInitialValue(new ConditionalSkipElements(condition.getDoseType().name(), condition.getDoseCountLogic(), 0));
            }
            else {
              lt.caConditionalSkipElements.setInitialValue(new ConditionalSkipElements(null, condition.getDoseCountLogic(), 0));
            }

            // this may need to change in the future to fit changes to AssessmentDate
            if (isForecast) {
              lt.caConditionalSkipReferenceDate.setInitialValue(caAssessmentDate.getFinalValue());
            }
            else if (isValidating){
              lt.caConditionalSkipReferenceDate.setInitialValue(PAST);
            }
            else {
              lt.caConditionalSkipReferenceDate.setInitialValue(caDateAdministered.getFinalValue());
            }
            if (condition.getEndDate() != null) {
              lt.caNumberofConditionalDosesAdministered
                  .setInitialValue(CONDSKIP_1.evaluate(dataModel, condition));
            }

          }
          logicTable610.addInnerSet(lt);
        }
        logicTableList.add(logicTable610);
        logicTable611.addInnerSet(logicTable610);
      }
      logicTableList.add(logicTable611);

    } else {
      log("No conditional skips are defined. ");
    }
    log("Looking to set Trigger values");
  }

  // Overriding the methods of parent functions and redefining them
  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_FOR_INADVERTENT_VACCINE);
    evaluateLogicTables();
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
        "<p>Evaluate Conditional Skip addresses times when a target dose can be skipped. A dose should be considered necessary unless it is determined that it can be skipped. The most common scenarios for skipping a dose are:</p>");
    out.println("<ul>");
    out.println(
        "<li>Catch-up doses where the patient is current with their administrations and does not need to catch-up</lui>");
    out.println(
        "<li>The patient is behind schedule and the total number of doses needed to satisfy the patient series can be reduced</lui>");
    out.println(
        "<li>The previously administered dose(s) negates the need for the current target dose</lui>");
    out.println("</ul>");

    out.println(
        "<p>In cases where a target dose does not specify Conditional Skip attributes, the target dose cannot be skipped.</p>");
    out.println(
        "<p>A dose may be skipped based on whether or not one or more conditions evaluates to true. Conditions are classified as one of a number of types, each with one or more parameters in the Supporting Data. Conditions are contained within sets. Each set contains one or more conditions to be evaluated. Within a set, one or more conditions must be met for the set to be met. In the case where a set contains multiple conditions, whether all conditions or just one condition must be met is specified by the Condition Logic (e.g., AND vs. OR). Similarly, a dose may contain multiple sets. In the case where a dose contains multiple sets, whether all sets or just one set must be met is specified by the Set Logic.</p>");
    out.println(
        "<p>Finally, in an effort to reduce page size and eliminate duplicate logic which could result in typographical and consistency errors, this section of logic is defined here once, but used in both Evaluation and Forecasting. The forecasting chapter refers the reader back to this section for appropriate logic.</p>");

    out.println("<img src=\"Figure 6.3.png\"/>"); // make sure you have the right image source
    out.println("<p>FIGURE 6 - 3 CONDITIONAL SKIP PROCESS MODEL</p>");
    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  // Defining condition attributes
  protected class LTInnerSet extends LogicTable {
    protected ConditionAttribute<ConditionalSkipElements> caConditionalSkipElements = null; //???
    protected ConditionAttribute<Date> caConditionalSkipBeginAgeDate = null;
    protected ConditionAttribute<Date> caConditionalSkipEndAgeDate = null;
    protected ConditionAttribute<Date> caConditionalSkipIntervalDate = null;
    protected ConditionAttribute<Date> caStartDate = null;
    protected ConditionAttribute<Date> caEndDate = null;
    protected ConditionAttribute<Date> caConditionalSkipReferenceDate = null;
    protected ConditionAttribute<Integer> caNumberofConditionalDosesAdministered = null;

    protected boolean met = false;

    public boolean isMet() {
      return met;
    }

    public LTInnerSet(int conditionCount, int outcomeCount, String label) {
      super(conditionCount, outcomeCount, label);
    }
  }

  // Logic Tables are defined here.
  /*
   * TODO:
   * Change name and code of the logic tables to that of their 4.5 equivalents 
   * Add new logic table 6-7
   */

  // Logic Table 4-6 -> 6-6
  // 2.1: "Is the Conditional Skip Reference Date ≥ Conditional Skip Begin Age Date" 
  // 4.5: "Is the Conditional Skip End Age Date > Is the Conditional Skip Reference Date ≥ Conditional Skip Begin Age Date?"
  protected class LT66 extends LTInnerSet {
    public LT66() {
      super(1, 2, "Table 6-6 CONDITIONAL Type of Age - Is the Condition Met?");

      setLogicCondition(0, new LogicCondition(
          "Is the Conditional Skip End Age Date > Is the Conditional Skip Reference Date ≥ Conditional Skip Begin Age Date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caConditionalSkipEndAgeDate.getFinalValue() == null 
              || caConditionalSkipBeginAgeDate.getFinalValue() == null
              || caExpirationDate.getFinalValue() == null) { 
              // if anything is null, answer is NO
            return LogicResult.NO;
          }
          if (caConditionalSkipBeginAgeDate.getFinalValue()
              .before(caExpirationDate.getFinalValue()) 
              && caExpirationDate.getFinalValue()
              .before(caConditionalSkipEndAgeDate.getFinalValue())) { 
            // So is expiration date the skip reference date? This code assumes it is for now, but it should be inquired about.
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicResults(0, new LogicResult[] {LogicResult.YES, LogicResult.NO});

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The condition is met.");
          met = true;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The condition is not met");
          met = false;
        }
      });

    }
  }

  // New Logic Table 6-7 (CONDITIONAL TYPE OF COMPLETED SERIES - IS THE CONDITION MET?) goes here
  // It requires Chapter 5 to work I think
  /*
  protected class LT67 extends LTInnerSet{
    public LT67() {
      super(1,2,"Table 6 - 7 CONDITIONAL Type of Completed Series - is the condition met?");

      setLogicCondition(0, new LogicCondition(
        "Does the Conditional Group Series Group identify a Series Group with at least one relevant patient series with a patient series status of 'Complete'?") {
          @Override
          public LogicResult evaluateInternal(){
            if() {
            return LogicResult.YES;
            }
            return LogicResult.NO;
          }
      });
    }
  }*/

  // Logic Table 4-7 -> 6-8
  // Add a new logic check: Has at least one dose been administered to the patient? 
  // A positive result now requires both conditions to be true. 
  protected class LT68 extends LTInnerSet {
    public LT68() {
      super(2, 4, "Table 6 - 8 CONDITIONAL Type of Interval - Is the Condition Met?");

      setLogicCondition(0, new LogicCondition(
        "Has at least one dose been administered to the patient?"){
          @Override
          public LogicResult evaluateInternal() {
            if (caConditionalSkipIntervalDate.getFinalValue() == null
            || caExpirationDate.getFinalValue() == null) {
              return LogicResult.NO;
            }
            if (caAdministeredDoseCount.getFinalValue() >= 1){
              return LogicResult.YES;
            }
            return LogicResult.NO;
          }
        });

      setLogicCondition(1, new LogicCondition(
        "Is the Conditional Skip Reference Date ≥ Conditional Skip Interval Date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caConditionalSkipIntervalDate.getFinalValue()
              .before(caExpirationDate.getFinalValue())
              ) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      // setLogicResults(0, new LogicResult[] {LogicResult.YES, LogicResult.NO}); I don't know if I'll need to restore this code later
      // Do I have to type out all 4 possibilities? Or can it be truncated?
      setLogicResults(0, LogicResult.YES, LogicResult.YES);
      setLogicResults(1, LogicResult.NO, LogicResult.YES);
      setLogicResults(2, LogicResult.NO, LogicResult.NO);
      setLogicResults(3, LogicResult.YES, LogicResult.NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The condition is met.");
          met = true;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The condition is not met");
          met = false;
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The condition is not met");
          met = false;
        }
      });

      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The condition is not met");
          met = false;
        }
      });

    }
  }

  // Logic Table 4-8 -> 6-9
  /*
   * TODO:
   * Fix the LogicResult to work with the new Conditional Skip Elements
   */
  protected class LT69 extends LTInnerSet {
    public LT69() {
      super(1, 2,
          "Table 6 - 9 CONDITIONAL Type of Vaccine Count By Age or Date - Is the Condition Met?");

      setLogicCondition(0, new LogicCondition(
          "Comparing the Number of Conditional Doses Administered with the Conditional Skip Dose Count") {
        @Override
        public LogicResult evaluateInternal() {
          if (caConditionalSkipElements.getFinalValue().getDoseCountLogic().equalsIgnoreCase("greater than")) {
            if (caConditionalSkipElements.getFinalValue().getDoseCount() < caAdministeredDoseCount
                .getFinalValue()) {
              return LogicResult.YES;
            }
            return LogicResult.NO;
          }
          if (caConditionalSkipElements.getFinalValue().getDoseCountLogic().equalsIgnoreCase("equal")) {
            if (caConditionalSkipElements.getFinalValue().getDoseCount() == caAdministeredDoseCount
                .getFinalValue()) {
              return LogicResult.YES;
            }
            return LogicResult.NO;
          }
          if (caConditionalSkipElements.getFinalValue().getDoseCountLogic().equalsIgnoreCase("less than")) {
            if (caConditionalSkipElements.getFinalValue().getDoseCount() > caAdministeredDoseCount
                .getFinalValue()) {
              return LogicResult.YES;
            }
            return LogicResult.NO;
          }
          return LogicResult.ANY;
        }
      });

      setLogicResults(0, new LogicResult[] {LogicResult.YES, LogicResult.NO});

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes, condition is met.");
          met = true;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No, condition is not met.");
          met = false;
        }
      });

    }
  }

  // Logic Table 4-9 -> 6-10
  // Extends LogicTable and not LTInnerSet
  protected class LT610 extends LogicTable {
    protected String conditionLogicType = "";

    public void setConditionLogicType(String conditionLogicType) {
      this.conditionLogicType = conditionLogicType;
    }


    private List<LTInnerSet> innerSetList =
        new ArrayList<EvaluateConditionalSkipForEvaluation.LTInnerSet>();
    private boolean met = false;

    public boolean isMet() {
      return met;
    }

    public void addInnerSet(LTInnerSet innerSet) {
      innerSetList.add(innerSet);
    }

    public LT610() {
      super(1, 2, "Table 6 - 10 Is the Conditional Skip Set Met?");

      setLogicCondition(0, new LogicCondition("How many conditions were met?") {
        @Override
        public LogicResult evaluateInternal() {
          if (conditionLogicType.equals(ConditionalSkipSet.CONDITION_LOGIC_AND)) {
            for (LTInnerSet innerSet : innerSetList) {
              if (!innerSet.isMet()) {
                return LogicResult.NO;
              }
            }
            return LogicResult.YES;
          } else if (conditionLogicType.equals(ConditionalSkipSet.CONDITION_LOGIC_OR)) {
            for (LTInnerSet innerSet : innerSetList) {
              if (innerSet.isMet()) {
                return LogicResult.YES;
              }
            }
            return LogicResult.NO;
          } else {
            return LogicResult.ANY;
          }
        }
      });

      setLogicResults(0, new LogicResult[] {LogicResult.YES, LogicResult.NO});

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes, the set is met. ");
          met = true;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No, the set is not met. ");
          met = false;
        }
      });

    }
  }

  // Logic Table 4-10 -> 6-11
  protected class LT611 extends LogicTable {
    private String setLogicType = "";

    public void setSetLogicType(String setLogicType) {
      this.setLogicType = setLogicType;
    }

    private List<LT610> innerSetList = new ArrayList<EvaluateConditionalSkipForEvaluation.LT610>();

    public void addInnerSet(LT610 innerSet) {
      innerSetList.add(innerSet);
    }

    public LT611(final LogicStepType noSkip, final LogicStepType skip) {
      super(1, 2, "Table 6 - 11 Can The Target Dose Be Skipped?");
      setLogicCondition(0, new LogicCondition("How many sets were met?") {
        @Override
        public LogicResult evaluateInternal() {
          if (setLogicType.equals(ConditionalSkip.SET_LOGIC_AND)) {
            for (LT610 innerSet : innerSetList) {
              if (!innerSet.isMet()) {
                return LogicResult.NO;
              }
            }
            return LogicResult.YES;
          } else if (setLogicType.equals(ConditionalSkip.SET_LOGIC_OR)) {
            for (LT610 innerSet : innerSetList) {
              if (innerSet.isMet()) {
                return LogicResult.YES;
              }
            }
            return LogicResult.NO;
          } else {
            return LogicResult.ANY;
          }
        }
      });

      setLogicResults(0, new LogicResult[] {LogicResult.YES, LogicResult.NO});

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The target dose can be skipped.");
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SKIPPED);
          log("The target dose status is \"skipped\"");
          log("Setting next step: Evaluate Immunization History");
          TargetDose targetDoseNext = dataModel.findNextTargetDose(dataModel.getTargetDose());
          dataModel.setTargetDose(targetDoseNext);
          setNextLogicStepType(skip);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The target dose cannot be skipped. ");
          log("Setting next step: Evaluate For Inadvertent Vaccine");
          setNextLogicStepType(noSkip);
        }
      });

    }
  }
}
