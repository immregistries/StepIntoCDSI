package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class DetermineForecastNeed extends LogicStep
{

   private ConditionAttribute<String> caVaccineDoseAdministered = null;
   private ConditionAttribute<String> caAdvereseEvents = null;
   private ConditionAttribute<String> caRelevantMedicalObservation = null;
   private ConditionAttribute<String> caTargetDose = null;
   private ConditionAttribute<Date> caMaximumAgeDate = null;
   private ConditionAttribute<Date> caEndDate = null;
   private ConditionAttribute<Date> caAssessmentDate = null;
   private ConditionAttribute<String> caContraindication = null;
   private ConditionAttribute<String> caImmunity = null;

  public DetermineForecastNeed(DataModel dataModel)
  {
    super(LogicStepType.DETERMINE_FORECAST_NEED, dataModel);
    setConditionTableName("Table ");
    
     caVaccineDoseAdministered = new ConditionAttribute<String>("Immunization history", "Vaccine Dose(s) Administered");
     caAdvereseEvents = new ConditionAttribute<String>("Immunization history" , "Adverse Events");
     caRelevantMedicalObservation = new ConditionAttribute<String>("Medical History" , "Relevant Medical Observation");
     caTargetDose = new ConditionAttribute<String>("Patient series" , "Target Dose(s)");
     caMaximumAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTAGE-1)" , "Maximum Age Date");
     caEndDate = new ConditionAttribute<Date>("Supporting data (Seasonal Recommendation" , "End Date");
     caAssessmentDate = new ConditionAttribute<Date>("Data Entry" , "Assessment Date");
     caContraindication = new ConditionAttribute<String>("Supporting Data" , "Contraindication");
     caImmunity = new ConditionAttribute<String>("Supporting Data" , "Immunity");
     
     
     
    
    caMaximumAgeDate.setAssumedValue(FUTURE);
    caEndDate.setAssumedValue(FUTURE);
    caAssessmentDate.setAssumedValue(null);
    
    
  conditionAttributesList.add(caVaccineDoseAdministered);
  conditionAttributesList.add(caAdvereseEvents);
  conditionAttributesList.add(caRelevantMedicalObservation);
  conditionAttributesList.add(caTargetDose);
  conditionAttributesList.add(caMaximumAgeDate);
  conditionAttributesList.add(caEndDate);
  conditionAttributesList.add(caAssessmentDate);
  conditionAttributesList.add(caContraindication);
  conditionAttributesList.add(caImmunity);
    
    LT logicTable = new LT();
    logicTableList.add(logicTable);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.GENERATE_FORECAST_DATES_AND_RECOMMEND_VACCINES);
    //setNextLogicStepType(LogicStepType.FOR_EACH_PATIENT_SERIES);
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
    out.println("<p>Determine forecast need determines  if there is a need to forecast dates. This involves reviewing patient data, antigen  administered  records,  and  patient  series.  This  is  a  prerequisite  before  a  CDS  engine  can  produce forecast dates and reasons </p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(0, 0, "Table ?-?");

      //      setLogicCondition(0, new LogicCondition("date administered > lot expiration date?") {
      //        @Override
      //        public LogicResult evaluateInternal() {
      //          if (caDateAdministered.getFinalValue() == null || caTriggerAgeDate.getFinalValue() == null) {
      //            return LogicResult.NO;
      //          }
      //          if (caDateAdministered.getFinalValue().before(caTriggerAgeDate.getFinalValue())) {
      //            return LogicResult.YES;
      //          }
      //          return LogicResult.NO;
      //        }
      //      });

      //      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.ANY);

      //      setLogicOutcome(0, new LogicOutcome() {
      //        @Override
      //        public void perform() {
      //          log("No. The target dose cannot be skipped. ");
      //          log("Setting next step: 4.3 Substitute Target Dose");
      //          setNextLogicStep(LogicStep.SUBSTITUTE_TARGET_DOSE_FOR_EVALUATION);
      //        }
      //      });
      //      
    }
  }


}
