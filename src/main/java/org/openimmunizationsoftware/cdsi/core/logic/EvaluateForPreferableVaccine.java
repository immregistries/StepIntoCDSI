package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTPREF_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTPREF_2;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateForPreferableVaccine extends LogicStep
{

  private ConditionAttribute<Date> caDateAdministered = null;
  //private ConditionAttribute<>
  private ConditionAttribute<String> caTradeName = null;
  private ConditionAttribute<Date> caVaccineTypeBeginAgeDate = null;
  private ConditionAttribute<Date> caVaccineTypeEndAgeDate = null;
  private ConditionAttribute<String> caPreferableVaccineTradeName = null;
  private ConditionAttribute<String> caPreferableVaccineVolume = null;

  public EvaluateForPreferableVaccine(DataModel dataModel) {
    super(LogicStepType.EVALUATE_PREFERABLE_VACCINE_ADMINISTERED, dataModel);
    setConditionTableName("Table ");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
//     _____________>Logic Problem : Check logic specification
    
    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caPreferableVaccineTradeName = new ConditionAttribute<String>("Supporting data (Allowable Vaccine)", "Vaccine Type") ;
    caTradeName = new ConditionAttribute<String>("Vaccine dose administered", "Vaccine Type") ;
    caVaccineTypeBeginAgeDate = new ConditionAttribute<Date>("Allowable Vaccine Type Begin Age Date", "Calculated date (CALCDTALLOW-1)");
    
    caTradeName.setInitialValue(aar.getAntigen().getName());
    caPreferableVaccineTradeName.setInitialValue( dataModel.getAntigenSeriesList().get(0).getSeriesDoseList().get(0).getAllowableVaccineList().get(0).getTradeName()) ;
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    
    conditionAttributesList.add(caDateAdministered);
  	conditionAttributesList.add(caTradeName);
  	conditionAttributesList.add(caVaccineTypeBeginAgeDate);
  	conditionAttributesList.add(caVaccineTypeEndAgeDate);
  	conditionAttributesList.add(caPreferableVaccineTradeName);
  	conditionAttributesList.add(caPreferableVaccineVolume);
    
    int i=0, j=0, k=0;
    while (i<dataModel.getAntigenSeriesList().size()){
    	while (j<dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().size()){
    		while (k<dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getAllowableVaccineList().size()){
    		    caPreferableVaccineTradeName.setInitialValue( dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getAllowableVaccineList().get(k).getTradeName()) ;
    		  //  caVaccineTypeBeginAgeDate.setInitialValue(dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getAllowableVaccineList().get(k).getVaccineTypeBeginAge());
    		  //  caVaccineTypeEndAgeDate.setInitialValue(dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getAllowableVaccineList().get(k).getVaccineTypeEndAge());

    		    LT logicTable = new LT();
    		    logicTableList.add(logicTable);
    		    k++;
    		}
    		j++;
    	}
    	i++;
    }
    
    //    // set assumed values
//    caVaccineTypeBeginAgeDate.setAssumedValue(PAST);
//    caVaccineTypeEndAgeDate.setAssumedValue(FUTURE);
//    caPreferableVaccineTradeName.setAssumedValue(aar.getTradeName());
//    caPreferableVaccineVolume.setAssumedValue(aar.getVolume());
//
//    // set actual values
//    caDateAdministered.setInitialValue(aar.getDateAdministered());
//    caTradeName.setInitialValue(aar.getTradeName());
//    caVaccineTypeBeginAgeDate.setInitialValue(CALCDTPREF_1.evaluate(dataModel, this, null));
//    caVaccineTypeEndAgeDate.setInitialValue(CALCDTPREF_2.evaluate(dataModel, this, null));
//    //caPreferableVaccineTradeName.setInitialValue(initialValue);
//
//    conditionAttributesList.add(caDateAdministered);
//    conditionAttributesList.add(caTradeName);
//    conditionAttributesList.add(caVaccineTypeBeginAgeDate);
//    conditionAttributesList.add(caVaccineTypeEndAgeDate);
//    conditionAttributesList.add(caPreferableVaccineTradeName);
//    conditionAttributesList.add(caPreferableVaccineVolume);


  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
    //setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
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
    out.println("<p>Evaluate  for  preferable  vaccine  validates  the  vaccine  of  a  vaccine  dose  administered  against  the  list  of preferable vaccines.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(0, 0, "Table 4-5 Was the supporting data defined preferrable vaccine administered?");

      setLogicCondition(0, new LogicCondition(
          "Is the vaccine type of the vaccine dose administered the same as the vaccine type of the preferable vaccine? ") {
        @Override
        public LogicResult evaluateInternal() {
          if (caTradeName.equals(caPreferableVaccineTradeName)) {
            return LogicResult.YES;
          }
          else{
          return LogicResult.NO;
          }
        }
      });

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
