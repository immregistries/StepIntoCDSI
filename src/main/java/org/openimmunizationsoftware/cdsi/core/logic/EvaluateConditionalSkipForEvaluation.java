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
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateConditionalSkipForEvaluation extends LogicStep {

  protected ConditionAttribute<Date> caDateAdministered = null;
  protected ConditionAttribute<Date> caExpirationDate = null;
  protected ConditionAttribute<Date> caAssessmentDate = null;
  protected ConditionAttribute<Integer> caAdministeredDoseCount = null;
  protected boolean isForecast;

  protected EvaluateConditionalSkipForEvaluation(LogicStepType logicStepType, DataModel dataModel) {
    super(logicStepType, dataModel);
  }

  public EvaluateConditionalSkipForEvaluation(DataModel dataModel) {
    super(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION, dataModel);
    setConditionTableName("Table 4.4 Skip Target Dose Attributes");
    setupInternal(dataModel, LogicStepType.EVALUATE_AGE, LogicStepType.FOR_EACH_PATIENT_SERIES);
  }

  protected void setupInternal(DataModel dataModel, final LogicStepType noSkip, final LogicStepType skip) {
	if(noSkip.equals(LogicStepType.EVALUATE_AGE)){
		isForecast=false;
	} else {
		isForecast=true;
	}
	
    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
    caAssessmentDate = new ConditionAttribute<Date>("Processing data", "Assessment Date");
    caAdministeredDoseCount = new ConditionAttribute<Integer>("Patient Immunization History","Administered Dose Count");
    caExpirationDate = new ConditionAttribute<Date>("Vaccine", "Expiration Date");
   
    caAssessmentDate.setAssumedValue(new Date());

    conditionAttributesList.add(caDateAdministered);
    conditionAttributesList.add(caAssessmentDate);
    conditionAttributesList.add(caAdministeredDoseCount);

    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    caExpirationDate.setInitialValue(aar.getLotExpirationDate());
    caAssessmentDate.setInitialValue(dataModel.getAssessmentDate());
    caAdministeredDoseCount.setInitialValue(dataModel.getAntigenAdministeredRecordList().size());
    

    SeriesDose seriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
    if (seriesDose.getConditionalSkip() != null) {
      LT410 logicTable410 = new LT410(noSkip, skip);
      logicTableList.add(logicTable410);
      log("Conditional skip has been defined, now looking at the details.");
      ConditionalSkip conditionalSkip = seriesDose.getConditionalSkip();
      logicTable410.setSetLogicType(conditionalSkip.getSetLogic());
      for (ConditionalSkipSet conditionalSkipSet : conditionalSkip.getConditionalSkipSetList()) {
        LT49 logicTable49 = new LT49();
        logicTableList.add(logicTable49);
        logicTable410.addInnerSet(logicTable49);
        logicTable49.setConditionLogicType(conditionalSkipSet.getConditionLogic());
        for (ConditionalSkipCondition condition : conditionalSkipSet.getConditionList()) {

          LTInnerSet lt = null;
          if (condition.getConditionType() == ConditionalSkipConditionType.AGE) {
            lt = new LT46();
          } else if (condition.getConditionType() == ConditionalSkipConditionType.INTERVAL) {
            lt = new LT47();
          } else if (condition.getConditionType() == ConditionalSkipConditionType.VACCINE_COUNT_BY_AGE
              || condition.getConditionType() == ConditionalSkipConditionType.VACCINE_COUNT_BY_DATE) {
            lt = new LT48();
          }
          if (lt != null) {
            logicTableList.add(lt);

            lt.caConditionalSkipBeginAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTSKIP-3)",
                "Conditional Skip Begin Age Date");
            lt.caConditionalSkipEndAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTSKIP-4)",
                "Conditional Skip End Age Date");
            lt.caConditionalSkipIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTSKIP-5)",
                "Conditional Skip Interval Date");
            lt.caConditionalSkipStartDate = new ConditionAttribute<Date>("Supporting Data (Conditional Skip)",
                "Conditional Skip Start Date");
            lt.caConditionalSkipEndDate = new ConditionAttribute<Date>("Supporting Data (Conditional Skip)",
                "Conditional SKip End Date");
            lt.caConditionalSkipDoseType = new ConditionAttribute<String>("Supporting Data (Conditional Skip)",
                "Conditional Skip Dose Type");
            lt.caConditionalSkipDoseCountLogic = new ConditionAttribute<String>("Supporting Data (Conditional Skip)",
                "Conditional Skip Doese Count Logic");
            lt.caConditionalSkipDoseCount = new ConditionAttribute<Integer>("Supporting Data (Conditional Skip)",
                "Conditional Skip Dose Count");
            lt.caNumberofConditionalDosesAdministered = new ConditionAttribute<Integer>("Supporting Data (CONDSKIP-1)",
                    "Number of Conditional Doses Administered");
            lt.caConditionalSkipReferenceDate = new ConditionAttribute<Date>("Supporting Data (CONDSKIP-2)",
                    "Conditional Skip Reference Date");

            List<ConditionAttribute<?>> caList = new ArrayList<ConditionAttribute<?>>();
            caList.add(lt.caConditionalSkipBeginAgeDate);
            caList.add(lt.caConditionalSkipEndAgeDate);
            caList.add(lt.caConditionalSkipIntervalDate);
            caList.add(lt.caConditionalSkipStartDate);
            caList.add(lt.caConditionalSkipEndDate);
            caList.add(lt.caConditionalSkipDoseType);
            caList.add(lt.caConditionalSkipDoseCountLogic);
            caList.add(lt.caConditionalSkipDoseCount);
            caList.add(lt.caConditionalSkipReferenceDate);
            conditionAttributesAdditionalMap.put("Table 4 - 4 Conditional Skip Attributes "
                + conditionalSkipSet.getSetId() + "." + condition.getConditionId(), caList);

            lt.caConditionalSkipBeginAgeDate.setInitialValue(CALCDTSKIP_3.evaluate(dataModel, this, condition));
            lt.caConditionalSkipEndAgeDate.setInitialValue(CALCDTSKIP_4.evaluate(dataModel, this, condition));
            lt.caConditionalSkipIntervalDate.setInitialValue(CALCDTSKIP_5.evaluate(dataModel, this, condition));
            lt.caConditionalSkipStartDate.setInitialValue(condition.getStartDate());
            lt.caConditionalSkipEndDate.setInitialValue(condition.getEndDate());
            if (condition.getDoseType() != null) {
              lt.caConditionalSkipDoseType.setInitialValue(condition.getDoseType().name());
            }
            if(isForecast){
            	lt.caConditionalSkipReferenceDate.setInitialValue(caAssessmentDate.getFinalValue());
            } else {
            	lt.caConditionalSkipReferenceDate.setInitialValue(caDateAdministered.getFinalValue());
            }
            lt.caConditionalSkipDoseCountLogic.setInitialValue(condition.getDoseCountLogic());
            lt.caConditionalSkipDoseCount.setInitialValue(0);
            if(condition.getEndDate()!=null){
            	lt.caNumberofConditionalDosesAdministered.setInitialValue(CONDSKIP_1.evaluate(dataModel, condition));
            }
            
          }
        }
      }
    } else {
      log("No conditional skips are defined. ");
    }
    log("Looking to set Trigger values");
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_AGE);
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
        "<p>Skip target dose addresses times when a target dose can be skipped. In most settings, this occurs when a patient is behind schedule and the total number of doses needed to satisfy patient series can be reduced. In cases where a target dose does not specify skip target dose attributes, the target dose cannot be skipped.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  protected class LTInnerSet extends LogicTable {

    protected ConditionAttribute<Date> caConditionalSkipBeginAgeDate = null;
    protected ConditionAttribute<Date> caConditionalSkipEndAgeDate = null;
    protected ConditionAttribute<Date> caConditionalSkipIntervalDate = null;
    protected ConditionAttribute<Date> caConditionalSkipStartDate = null;
    protected ConditionAttribute<Date> caConditionalSkipEndDate = null;
    protected ConditionAttribute<String> caConditionalSkipDoseType = null;
    protected ConditionAttribute<String> caConditionalSkipDoseCountLogic = null;
    protected ConditionAttribute<Integer> caConditionalSkipDoseCount = null;
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

  protected class LT46 extends LTInnerSet {
    public LT46() {
      super(1, 2, "Table 4-6 CONDITIONAL Type of Age – Is the Condition Met?");

      setLogicCondition(0,
          new LogicCondition("Is the Conditional Skip Reference Date ≥ Conditional Skip Begin Age Date?") {
            @Override
            public LogicResult evaluateInternal() {
              if (caConditionalSkipBeginAgeDate.getFinalValue() == null || caExpirationDate.getFinalValue() == null) {
                return LogicResult.NO;
              }
              if (caConditionalSkipBeginAgeDate.getFinalValue().before(caExpirationDate.getFinalValue())) {
                return LogicResult.YES;
              }
              return LogicResult.NO;
            }
          });

      setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.NO });

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

  protected class LT47 extends LTInnerSet {
    public LT47() {
      super(1, 2, "Table 4 - 7 CONDITIONAL Type of Interval – Is the Condition Met?");

      setLogicCondition(0,
          new LogicCondition("Is the Conditional Skip Reference Date ≥ Conditional Skip Interval Date?") {
            @Override
            public LogicResult evaluateInternal() {
                if (caConditionalSkipIntervalDate.getFinalValue() == null || caExpirationDate.getFinalValue() == null) {
                  return LogicResult.NO;
                }
                if (caConditionalSkipIntervalDate.getFinalValue().before(caExpirationDate.getFinalValue())) {
                  return LogicResult.YES;
                }
                return LogicResult.NO;
              }
          });

      setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.NO });

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

  protected class LT48 extends LTInnerSet {
    public LT48() {
      super(1, 2, "Table 4 - 8 CONDITIONAL Type of Vaccine Count By Age or Date – Is the Condition Met?");

      setLogicCondition(0, new LogicCondition(
          "Comparing the Number of Conditional Doses Administered with the Conditional Skip Dose Count") {
        @Override
        public LogicResult evaluateInternal() {
          if (caConditionalSkipDoseCountLogic.getFinalValue().equalsIgnoreCase("greater than")){
        	  if(caConditionalSkipDoseCount.getFinalValue() < caAdministeredDoseCount.getFinalValue()){
        		  return LogicResult.YES;
        	  }
        	  return LogicResult.NO;
          }
          if (caConditionalSkipDoseCountLogic.getFinalValue().equalsIgnoreCase("equal")){
        	  if(caConditionalSkipDoseCount.getFinalValue() == caAdministeredDoseCount.getFinalValue()){
        		  return LogicResult.YES;
        	  }
        	  return LogicResult.NO;
          }
          if (caConditionalSkipDoseCountLogic.getFinalValue().equalsIgnoreCase("less than")){
        	  if(caConditionalSkipDoseCount.getFinalValue() > caAdministeredDoseCount.getFinalValue()){
        		  return LogicResult.YES;
        	  }
        	  return LogicResult.NO;
          }
          return LogicResult.ANY;
        }
      });

      setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.NO });

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

  protected class LT49 extends LogicTable {
    protected String conditionLogicType = "";

    public void setConditionLogicType(String conditionLogicType) {
      this.conditionLogicType = conditionLogicType;
    }


    private List<LTInnerSet> innerSetList = new ArrayList<EvaluateConditionalSkipForEvaluation.LTInnerSet>();
    private boolean met = false;

    public boolean isMet() {
      return met;
    }

    public void addInnerSet(LTInnerSet innerSet) {
      innerSetList.add(innerSet);
    }

    public LT49() {
      super(1, 2, "Table 4 - 9 Is the Conditional Skip Set Met?");

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

      setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.NO });

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

  protected class LT410 extends LogicTable {
    private String setLogicType = "";

    public void setSetLogicType(String setLogicType) {
      this.setLogicType = setLogicType;
    }

    private List<LT49> innerSetList = new ArrayList<EvaluateConditionalSkipForEvaluation.LT49>();

    public void addInnerSet(LT49 innerSet) {
      innerSetList.add(innerSet);
    }

    public LT410(final LogicStepType noSkip, final LogicStepType skip) {
      super(1, 2, "Table 4 - 10 Can The Target Dose Be Skipped?");
      setLogicCondition(0, new LogicCondition("How many sets were met?") {
        @Override
        public LogicResult evaluateInternal() {
          if (setLogicType.equals(ConditionalSkip.SET_LOGIC_AND)) {
            for (LT49 innerSet : innerSetList) {
              if (!innerSet.isMet()) {
                return LogicResult.NO;
              }
            }
            return LogicResult.YES;
          } else if (setLogicType.equals(ConditionalSkip.SET_LOGIC_OR)) {
            for (LT49 innerSet : innerSetList) {
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

      setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.NO });

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The target dose can be skipped.");
          dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SKIPPED);
          log("The taget dose status is \"skipped\"");
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
          log("Setting next step: 4.3 Evaluate Age");
          setNextLogicStepType(noSkip);
        }
      });

    }
  }
}
